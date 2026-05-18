#!/usr/bin/env python3
"""
发布脚本：构建 APK → 蓝奏云上传 → 更新 version.json → Git 推送 → GitHub Release

用法:
    python scripts/publish.py
    python scripts/publish.py --skip-lanzou   # 跳过蓝奏云上传
    python scripts/publish.py --no-push       # 不推送 git

前置条件:
    1. 同级目录下存在 publish_config.json（已 gitignore），格式见下方模板
    2. 已安装 Python requests 库 (pip install requests)
    3. 当前目录为项目根目录

publish_config.json 模板:
{
    "lanzou": {
        "username": "your_lanzou_email",
        "password": "your_lanzou_password"
    },
    "github": {
        "token": "ghp_xxxxxxxxxxxxxxxxxxxx"
    }
}
"""

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

import requests

# ── 路径 ──
PROJECT_DIR = Path(__file__).resolve().parent.parent
VERSION_JSON = PROJECT_DIR / "version.json"
APK_RELEASE_DIR = PROJECT_DIR / "app" / "build" / "outputs" / "apk" / "release"
CONFIG_PATH = PROJECT_DIR / "scripts" / "publish_config.json"
LANZOU_FOLDER_NAME = "记账助手"
GITHUB_REPO = "2350471581/-"


def log(msg: str):
    print(f"[*] {msg}", flush=True)


def err(msg: str):
    print(f"[!] {msg}", file=sys.stderr, flush=True)


def ok(msg: str):
    print(f"[OK] {msg}", flush=True)


# ── 读取配置 ──
def load_config() -> dict:
    if not CONFIG_PATH.exists():
        err(f"配置文件不存在: {CONFIG_PATH}")
        err("请创建该文件，格式见脚本注释。")
        sys.exit(1)
    with open(CONFIG_PATH, encoding="utf-8") as f:
        return json.load(f)


# ── 构建 APK ──
def build_apk() -> Path:
    log("正在构建 Release APK ...")
    if sys.platform == "win32":
        gradlew = str(PROJECT_DIR / "gradlew.bat")
    else:
        gradlew = "./gradlew"
    result = subprocess.run(
        [gradlew, "assembleRelease", "--quiet"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        err(f"构建失败:\n{result.stderr}")
        sys.exit(1)
    ok("构建完成")

    apks = list(APK_RELEASE_DIR.glob("*.apk"))
    if not apks:
        err(f"未找到 APK 文件: {APK_RELEASE_DIR}")
        sys.exit(1)
    return apks[0]


# ── 计算 MD5 ──
def calc_md5(filepath: Path) -> str:
    h = hashlib.md5()
    with open(filepath, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


# ── 蓝奏云 API ──
class LanzouCloud:
    BASE = "https://pc.woozooo.com"
    ACCOUNT = f"{BASE}/account.php"
    DISK = f"{BASE}/mydisk.php"
    FILE = f"{BASE}/file.php"

    def __init__(self, username: str, password: str):
        self.sess = requests.Session()
        self.sess.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Referer": self.BASE,
        })
        self.username = username
        self.password = password
        self._folder_id = -1  # 根目录

    def login(self):
        log("正在登录蓝奏云 ...")
        data = {"action": "login", "task": "login", "setSession": "1",
                "user": self.username, "passwd": self.password}
        resp = self.sess.post(self.ACCOUNT, data=data)
        try:
            j = resp.json()
            if j.get("zt") != 1:
                err(f"蓝奏云登录失败: {j.get('info', '未知错误')}")
                return False
            ok("蓝奏云登录成功")
            return True
        except json.JSONDecodeError:
            err(f"蓝奏云登录返回异常: {resp.text[:200]}")
            return False

    def _get_folders(self) -> list:
        """获取根目录下所有文件夹"""
        data = {"item": "files", "action": "index", "task": "4", "folder_id": "-1"}
        resp = self.sess.post(self.DISK, data=data)
        try:
            j = resp.json()
            return j.get("text", []) or []
        except Exception:
            return []

    def _create_folder(self, name: str) -> int:
        """创建文件夹，返回 folder_id"""
        data = {"task": "create_folder", "parent_id": "-1", "folder_name": name}
        resp = self.sess.post(self.FILE, data=data)
        try:
            j = resp.json()
            if j.get("zt") == 1:
                # 从返回中提取 folder_id
                text = j.get("info", "")
                m = re.search(r"folder_id=(\d+)", text)
                if m:
                    return int(m.group(1))
            return -1
        except Exception:
            return -1

    def ensure_folder(self) -> int:
        """确保「记账助手」文件夹存在，返回 folder_id"""
        # 先查现有文件夹
        folders = self._get_folders()
        for f in folders:
            if isinstance(f, dict) and f.get("name") == LANZOU_FOLDER_NAME:
                self._folder_id = int(f.get("id", -1))
                ok(f"找到蓝奏云文件夹「{LANZOU_FOLDER_NAME}」(id={self._folder_id})")
                return self._folder_id

        # 不存在则创建
        log(f"蓝奏云文件夹「{LANZOU_FOLDER_NAME}」不存在，正在创建 ...")
        fid = self._create_folder(LANZOU_FOLDER_NAME)
        if fid > 0:
            self._folder_id = fid
            ok(f"已创建蓝奏云文件夹「{LANZOU_FOLDER_NAME}」(id={fid})")
            return fid
        else:
            err("创建蓝奏云文件夹失败，将上传到根目录")
            self._folder_id = -1
            return -1

    def upload(self, filepath: Path) -> dict | None:
        """上传文件到蓝奏云，返回 {url, pwd}"""
        log(f"正在上传 {filepath.name} 到蓝奏云 ...")
        file_size = filepath.stat().st_size
        if file_size > 100 * 1024 * 1024:
            err("文件超过 100MB，蓝奏云无法上传")
            return None

        # 部分版本的上传 endpoint 不同，尝试主站上传
        with open(filepath, "rb") as f:
            files = {
                "task": (None, "upload"),
                "id": (None, str(self._folder_id)),
                "name": (None, filepath.stem),
                "upload_file": (filepath.name, f, "application/vnd.android.package-archive"),
            }
            resp = self.sess.post(self.FILE, files=files)

        try:
            j = resp.json()
            if j.get("zt") == 1:
                # 获取 file_id
                text = j.get("info", "")
                m = re.search(r"file_id=(\d+)", text)
                file_id = m.group(1) if m else None
                if file_id:
                    ok("蓝奏云上传成功")
                    return self._get_share_info(file_id)
            err(f"蓝奏云上传失败: {j.get('info', resp.text[:200])}")
            return None
        except Exception as e:
            err(f"蓝奏云上传异常: {e}")
            return None

    def _get_share_info(self, file_id: str) -> dict | None:
        """获取分享链接和密码"""
        # 先设置一个默认密码
        pwd = "bill"
        data = {"task": "set_passwd", "file_id": file_id, "passwd": pwd}
        self.sess.post(self.FILE, data=data)
        time.sleep(0.5)

        # 获取分享链接
        data = {"task": "share_file", "file_id": file_id}
        resp = self.sess.post(self.FILE, data=data)
        try:
            j = resp.json()
            if j.get("zt") == 1:
                url = j.get("info", "")
                if url.startswith("http"):
                    ok(f"蓝奏云分享链接: {url}")
                    return {"url": url, "password": pwd}
        except Exception:
            pass
        err("获取蓝奏云分享信息失败")
        return None


# ── 更新 version.json ──
def update_version_json(
    version_name: str,
    new_md5: str,
    lanzou_url: str = "",
    lanzou_pwd: str = "",
) -> dict:
    log("正在更新 version.json ...")
    with open(VERSION_JSON, encoding="utf-8") as f:
        data = json.load(f)

    # 递增 versionCode
    data["versionCode"] = data.get("versionCode", 0) + 1
    # 用户可手动修改 versionName，这里默认使用原值或 +1
    data["versionName"] = version_name
    data["md5"] = new_md5
    if lanzou_url:
        data["lanzouUrl"] = lanzou_url
    if lanzou_pwd:
        data["lanzouPassword"] = lanzou_pwd

    # 更新 sources 中的版本号 URL
    vname = version_name
    for src in data.get("sources", []):
        if "2350471581" in src["url"]:
            src["url"] = re.sub(r"@v[\d.]+/", f"@v{vname}/", src["url"])
            src["url"] = re.sub(r"/v[\d.]+/", f"/v{vname}/", src["url"])

    with open(VERSION_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    ok(f"version.json 已更新 (v{version_name}, md5={new_md5[:8]}...)")
    return data


# ── Git 操作 ──
def git_commit_and_push(version_name: str):
    log("正在提交 Git ...")
    subprocess.run(["git", "add", "version.json"], cwd=PROJECT_DIR, check=True)
    subprocess.run(["git", "add", "app/build.gradle.kts"], cwd=PROJECT_DIR, check=False)  # 可能未改
    msg = f"release: v{version_name}"
    result = subprocess.run(
        ["git", "commit", "-m", msg],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
    )
    if result.returncode == 0:
        ok("Git commit 成功")
    else:
        if "nothing to commit" in result.stderr or "nothing to commit" in result.stdout:
            log("无变更需要提交")
        else:
            err(f"Git commit 失败: {result.stderr}")
            return False

    log("正在推送 Git ...")
    push = subprocess.run(["git", "push"], cwd=PROJECT_DIR, capture_output=True, text=True)
    if push.returncode == 0:
        ok("Git push 成功")
        return True
    else:
        err(f"Git push 失败: {push.stderr}")
        return False


# ── GitHub Release ──
def create_github_release(token: str, version_name: str, apk_path: Path, version_data: dict):
    log("正在创建 GitHub Release ...")
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
    }
    tag = f"v{version_name}"
    body = version_data.get("releaseNotes", "")
    payload = {
        "tag_name": tag,
        "name": f"v{version_name}",
        "body": body,
        "generate_release_notes": False,
    }
    url = f"https://api.github.com/repos/{GITHUB_REPO}/releases"
    resp = requests.post(url, headers=headers, json=payload)
    if resp.status_code not in (201, 200):
        # 可能 tag 已存在，尝试获取已有 release
        err(f"创建 Release 失败 ({resp.status_code})，尝试更新已有 Release ...")
        get_url = f"https://api.github.com/repos/{GITHUB_REPO}/releases/tags/{tag}"
        get_resp = requests.get(get_url, headers=headers)
        if get_resp.status_code != 200:
            err(f"获取已有 Release 失败: {get_resp.text[:200]}")
            return False
        release = get_resp.json()
        upload_url = release["upload_url"].replace("{?name,label}", "")
    else:
        release = resp.json()
        upload_url = release["upload_url"].replace("{?name,label}", "")
        ok("GitHub Release 已创建")

    # 上传 APK 作为 Release Asset
    log("正在上传 APK 到 GitHub Release ...")
    asset_name = f"记账助手-v{version_name}.apk"
    with open(apk_path, "rb") as f:
        asset_headers = {**headers, "Content-Type": "application/vnd.android.package-archive"}
        asset_resp = requests.post(
            upload_url + f"?name={asset_name}",
            headers=asset_headers,
            data=f,
        )
    if asset_resp.status_code in (201, 200):
        ok("APK 已上传到 GitHub Release")
        return True
    else:
        err(f"APK 上传到 GitHub 失败: {asset_resp.text[:200]}")
        return False


# ── 主流程 ──
def main():
    parser = argparse.ArgumentParser(description="发布记账助手")
    parser.add_argument("--skip-lanzou", action="store_true", help="跳过蓝奏云上传")
    parser.add_argument("--no-push", action="store_true", help="不推送 Git")
    parser.add_argument("--version", default=None, help="版本号 (如 1.4)")
    args = parser.parse_args()

    config = load_config()

    # 1. 构建
    apk_path = build_apk()
    md5 = calc_md5(apk_path)
    log(f"APK: {apk_path}, MD5: {md5}")

    # 2. 确定版本号
    with open(VERSION_JSON, encoding="utf-8") as f:
        version_data = json.load(f)
    version_name = args.version or version_data.get("versionName", "1.0")

    # 3. 蓝奏云上传
    lanzou_url = ""
    lanzou_pwd = ""
    if not args.skip_lanzou:
        lz = LanzouCloud(config["lanzou"]["username"], config["lanzou"]["password"])
        if lz.login():
            lz.ensure_folder()
            share = lz.upload(apk_path)
            if share:
                lanzou_url = share["url"]
                lanzou_pwd = share["password"]
        else:
            err("蓝奏云登录失败，跳过上传")

    # 4. 更新 version.json
    version_data = update_version_json(version_name, md5, lanzou_url, lanzou_pwd)

    # 5. Git
    if not args.no_push:
        pushed = git_commit_and_push(version_name)
        if pushed and not args.skip_lanzou:
            # 6. GitHub Release
            if "github" in config:
                create_github_release(
                    config["github"]["token"], version_name, apk_path, version_data
                )

    ok("发布流程完成！")


if __name__ == "__main__":
    main()
