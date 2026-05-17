"""
发布脚本：构建 APK → 计算 MD5 → 上传 OSS → 更新 version.json → 提交 GitHub

用法：
  python scripts/deploy.py                    # 只生成 version.json（手动上传 APK）
  python scripts/deploy.py --upload           # 需要 aliyun-oss 配置
  python scripts/deploy.py --github-release   # 发布到 GitHub Release

前置：
  pip install oss2   # 上传到阿里云 OSS 时需要
"""

import json
import hashlib
import re
import os
import subprocess
import argparse
import sys
import tempfile
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent.parent
GRADLE_FILE = PROJECT_DIR / "app" / "build.gradle.kts"
APK_OUTPUT_DIR = PROJECT_DIR / "app" / "build" / "outputs" / "apk" / "release"
APK_FILENAME = "记账助手.apk"
VERSION_JSON = PROJECT_DIR / "version.json"
RELEASE_DIR = PROJECT_DIR / "app" / "build" / "intermediates" / "apk" / "release"


def parse_version():
    """从 build.gradle.kts 解析 versionCode 和 versionName"""
    text = GRADLE_FILE.read_text(encoding="utf-8")
    vc = re.search(r'versionCode\s*=\s*(\d+)', text)
    vn = re.search(r'versionName\s*=\s*"(.*?)"', text)
    if not vc or not vn:
        print("❌ 无法从 build.gradle.kts 解析版本号")
        sys.exit(1)
    return int(vc.group(1)), vn.group(1)


def find_apk():
    """找 release APK"""
    candidates = [
        PROJECT_DIR / f"app/build/outputs/apk/release/{APK_FILENAME}",
        PROJECT_DIR / f"app/build/intermediates/apk/release/{APK_FILENAME}",
    ]
    for p in candidates:
        if p.exists():
            return p
    # glob fallback
    matches = sorted(PROJECT_DIR.rglob(APK_FILENAME))
    if matches:
        return matches[0]
    print(f"❌ 找不到 {APK_FILENAME}，请先构建 release APK")
    print("   运行: ./gradlew assembleRelease")
    sys.exit(1)


def compute_md5(path: Path) -> str:
    """计算文件 MD5"""
    h = hashlib.md5()
    with open(path, "rb") as f:
        while True:
            buf = f.read(8192)
            if not buf:
                break
            h.update(buf)
    return h.hexdigest().lower()


def build_apk():
    """调用 Gradle 构建 release APK"""
    print("🔨 构建 release APK...")
    rc = subprocess.run(
        ["./gradlew", "assembleRelease", "--quiet"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
    )
    if rc.returncode != 0:
        print("❌ 构建失败:")
        print(rc.stderr)
        sys.exit(1)
    print("✅ 构建成功")


def upload_to_oss(apk_path: Path, version_name: str):
    """上传到阿里云 OSS（需要环境变量配置）"""
    try:
        import oss2
    except ImportError:
        print("⚠️  未安装 oss2，跳过 OSS 上传")
        return None

    key = os.environ.get("OSS_KEY")
    secret = os.environ.get("OSS_SECRET")
    endpoint = os.environ.get("OSS_ENDPOINT", "oss-cn-hangzhou.aliyuncs.com")
    bucket_name = os.environ.get("OSS_BUCKET", "billtracker")

    if not key or not secret:
        print("⚠️  未设置 OSS_KEY / OSS_SECRET，跳过 OSS 上传")
        return None

    auth = oss2.Auth(key, secret)
    bucket = oss2.Bucket(auth, endpoint, bucket_name)
    object_name = f"apks/记账助手-v{version_name}.apk"

    print(f"☁️  上传到 OSS: {object_name}")
    bucket.put_object_from_file(object_name, str(apk_path))

    # CDN 加速 URL（使用阿里云 CDN）
    cdn_domain = os.environ.get("OSS_CDN_DOMAIN", f"https://{bucket_name}.oss-cn-hangzhou.aliyuncs.com")
    url = f"{cdn_domain}/{object_name}"
    print(f"✅ OSS 上传完成: {url}")
    return url


def create_github_release(version_name: str, apk_path: Path):
    """通过 gh CLI 创建 GitHub Release"""
    tag = f"v{version_name}"

    # 检查 gh 是否可用
    rc = subprocess.run(["gh", "--version"], capture_output=True, text=True)
    if rc.returncode != 0:
        print("⚠️  未安装 gh CLI，跳过 GitHub Release")
        return None

    # 检查是否已有该 tag
    rc = subprocess.run(
        ["gh", "release", "view", tag, "--json", "tagName"],
        capture_output=True, text=True,
    )
    if rc.returncode == 0:
        print(f"⚠️  GitHub Release {tag} 已存在，跳过")
        # 但仍返回 release 的下载 URL
        return f"https://github.com/2350471581/-/releases/download/{tag}/{APK_FILENAME}"

    print(f"📦 创建 GitHub Release: {tag}")
    rc = subprocess.run(
        ["gh", "release", "create", tag, str(apk_path),
         "--title", f"v{version_name}",
         "--generate-notes"],
        cwd=PROJECT_DIR, capture_output=True, text=True,
    )
    if rc.returncode != 0:
        print(f"⚠️  GitHub Release 创建失败: {rc.stderr}")
        return None

    url = f"https://github.com/2350471581/-/releases/download/{tag}/{APK_FILENAME}"
    print(f"✅ Release 创建完成: {url}")
    return url


def update_version_json(
    version_code: int,
    version_name: str,
    md5: str,
    oss_url: str | None,
    github_url: str | None,
):
    """生成 version.json（多源结构）"""
    sources = []

    priority = 0
    if oss_url:
        sources.append({
            "url": oss_url,
            "priority": priority,
            "label": "主站 CDN",
        })
        priority += 1

    # jsDelivr 加速（从 GitHub Releases 加速）
    jsd_url = f"https://cdn.jsdelivr.net/gh/2350471581/-@v{version_name}/{APK_FILENAME}"
    sources.append({
        "url": jsd_url,
        "priority": priority,
        "label": "jsDelivr 镜像",
    })
    priority += 1

    # GitHub 直链（URL 可预测，始终包含）
    gh_url = github_url or f"https://github.com/2350471581/-/releases/download/v{version_name}/{APK_FILENAME}"
    sources.append({
        "url": gh_url,
        "priority": priority,
        "label": "GitHub 直连",
    })
    priority += 1

    # ghproxy 备用
    sources.append({
        "url": f"https://ghproxy.com/{gh_url}",
        "priority": priority,
        "label": "ghproxy 备用",
    })

    data = {
        "versionCode": version_code,
        "versionName": version_name,
        "releaseNotes": "",  # 发布后手动编辑
        "md5": md5,
        "sources": sources,
    }

    VERSION_JSON.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"📝 version.json 已生成: {VERSION_JSON}")


def commit_and_push():
    """提交 version.json 并推送"""
    cmds = [
        ["git", "add", "version.json"],
        ["git", "commit", "-m", "chore: update version.json"],
        ["git", "push"],
    ]
    for cmd in cmds:
        rc = subprocess.run(cmd, cwd=PROJECT_DIR, capture_output=True, text=True)
        if rc.returncode != 0 and cmd[1] != "commit":  # commit 可能 "nothing to commit"
            print(f"⚠️  {' '.join(cmd)} 失败: {rc.stderr}")


def main():
    parser = argparse.ArgumentParser(description="BillTracker 发布脚本")
    parser.add_argument("--build", action="store_true", help="先执行 Gradle 构建")
    parser.add_argument("--upload", action="store_true", help="上传到阿里云 OSS")
    parser.add_argument("--github-release", action="store_true", help="发布到 GitHub Release")
    parser.add_argument("--push", action="store_true", help="提交 version.json 到 GitHub")
    args = parser.parse_args()

    # 1. 解析版本
    version_code, version_name = parse_version()
    print(f"📋 版本: {version_code} ({version_name})")

    # 2. 构建
    if args.build:
        build_apk()

    # 3. 找 APK
    apk = find_apk()
    print(f"📦 APK: {apk} ({apk.stat().st_size / 1024 / 1024:.1f} MB)")

    # 4. MD5
    md5 = compute_md5(apk)
    print(f"🔐 MD5: {md5}")

    # 5. 上传
    oss_url = upload_to_oss(apk, version_name) if args.upload else None
    gh_url = create_github_release(version_name, apk) if args.github_release else None

    # 6. 生成 version.json
    update_version_json(version_code, version_name, md5, oss_url, gh_url)

    # 7. 提交
    if args.push:
        commit_and_push()
        print("✅ 已推送至 GitHub")

    print("\n🎉 完成! 记得编辑 version.json 补充 releaseNotes")


if __name__ == "__main__":
    main()
