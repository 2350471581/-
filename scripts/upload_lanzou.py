#!/usr/bin/env python3
"""
Upload APK to Lanzou Cloud using pure requests with cookies.
"""
import json
import re
import sys
import time
import hashlib
from pathlib import Path

import requests

BASE = "https://pc.woozooo.com"
ACCOUNT = f"{BASE}/account.php"
MYDISK = f"{BASE}/mydisk.php"
DOUPLOAD = f"{BASE}/doupload.php"
FILE = f"{BASE}/file.php"
LANZOU_FOLDER = "记账助手"

PROJECT_DIR = Path(__file__).resolve().parent.parent
APK_PATH = PROJECT_DIR / "app" / "build" / "outputs" / "apk" / "debug" / "记账助手.apk"
COOKIE_PATH = PROJECT_DIR / "lanzou_cookie.json"


def log(msg): print(f"[*] {msg}", flush=True)
def ok(msg): print(f"[OK] {msg}", flush=True)
def err(msg): print(f"[!] {msg}", file=sys.stderr, flush=True)


def init_session(cookie_dict):
    """Create a requests session with cookies and establish PHP session."""
    sess = requests.Session()
    sess.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Referer": BASE,
    })

    # Set cookies with proper domain/path
    for k, v in cookie_dict.items():
        sess.cookies.set(k, v, domain="pc.woozooo.com", path="/")

    # Visit mydisk.php to establish PHP session (gets PHPSESSID from server)
    log("Establishing PHP session...")
    resp = sess.get(MYDISK)
    if resp.status_code != 200:
        err(f"Failed to access mydisk: HTTP {resp.status_code}")
        return None

    # Check if logged in
    if "退出" not in resp.text:
        err("Not logged in - cookies may be expired")
        return None

    ok("Session established, logged in as: " +
       (re.search(r'(\d+\*+\d+)', resp.text).group(1) if re.search(r'(\d+\*+\d+)', resp.text) else "unknown"))
    return sess


def doupload_api(sess, task, **extra):
    """Call doupload.php API."""
    data = {"task": task}
    data.update(extra)
    resp = sess.post(DOUPLOAD, data=data)
    try:
        return resp.json()
    except:
        err(f"doupload task={task} returned non-JSON: {resp.text[:200]}")
        return None


def get_folders(sess):
    """Get all folders via doupload API."""
    result = doupload_api(sess, 47, folder_id=-1)
    if not result or result.get("zt") != 1:
        err(f"get_folders failed: {result}")
        return []
    return result.get("text", [])


def get_files(sess, folder_id=-1):
    """Get file list in a folder."""
    result = doupload_api(sess, 5, folder_id=folder_id, pg=1)
    if not result or result.get("zt") != 1:
        return []
    text = result.get("text", [])
    if isinstance(text, dict):
        text = list(text.values())
    return text


def create_folder(sess, name):
    """Create a folder, return folder_id."""
    data = {"task": "create_folder", "parent_id": "-1", "folder_name": name}
    resp = sess.post(FILE, data=data)
    try:
        j = resp.json()
        if j.get("zt") == 1:
            m = re.search(r"folder_id=(\d+)", j.get("info", ""))
            if m:
                return int(m.group(1))
    except:
        pass
    return -1


def ensure_folder(sess):
    """Ensure target folder exists."""
    folders = get_folders(sess)
    for f in folders:
        if isinstance(f, dict) and f.get("name") == LANZOU_FOLDER:
            fid = int(f.get("id", -1))
            ok(f"Found folder '{LANZOU_FOLDER}' (id={fid})")
            return fid

    log(f"Creating folder '{LANZOU_FOLDER}'...")
    fid = create_folder(sess, LANZOU_FOLDER)
    if fid > 0:
        ok(f"Created folder '{LANZOU_FOLDER}' (id={fid})")
        return fid

    # Try via doupload
    result = doupload_api(sess, 2, folder_name=LANZOU_FOLDER, folder_id="-1")
    if result and result.get("zt") == 1:
        folders = get_folders(sess)
        for f in folders:
            if isinstance(f, dict) and f.get("name") == LANZOU_FOLDER:
                ok(f"Created folder via doupload (id={f.get('id')})")
                return int(f.get("id", -1))

    err("Failed to create folder, using root")
    return -1


def upload_file(sess, filepath, folder_id):
    """Upload via file.php with multipart form (browser style)."""
    filename = filepath.name
    name_stem = filepath.stem
    log(f"Uploading {filename} ({filepath.stat().st_size / 1024 / 1024:.1f} MB)...")

    with open(filepath, "rb") as f:
        files = {
            "task": (None, "upload"),
            "id": (None, str(folder_id)),
            "name": (None, name_stem),
            "upload_file": (filename, f, "application/vnd.android.package-archive"),
        }
        resp = sess.post(FILE, files=files)

    log(f"Upload response: HTTP {resp.status_code}, len={len(resp.content)}")
    text = resp.text[:500]
    log(f"Body: {text[:200]}")

    try:
        j = resp.json()
        if j.get("zt") == 1:
            m = re.search(r"file_id=(\d+)", j.get("info", ""))
            file_id = m.group(1) if m else None
            if file_id:
                ok("Upload success (file.php)!")
                return get_share_info(sess, file_id)
        err(f"Upload failed: {j.get('info', text[:200])}")
        return None
    except json.JSONDecodeError:
        # HTML response - check for success indicators
        if "file_id" in text:
            m = re.search(r"file_id=(\d+)", text)
            if m:
                ok("Upload success (detected in HTML)!")
                return get_share_info(sess, m.group(1))
        err(f"Upload non-JSON response")
        return None


def upload_file_v2(sess, filepath, folder_id):
    """Upload via doupload.php/fileup.php using multipart encoder."""
    filename = filepath.name
    log("Trying fileup.php upload...")

    try:
        from requests_toolbelt import MultipartEncoder

        with open(filepath, "rb") as f:
            post_data = {
                "task": "1",
                "vie": "2",
                "ve": "2",
                "id": "WU_FILE_0",
                "folder_id_bb_n": str(folder_id),
                "name": filename,
                "upload_file": (filename, f, "application/octet-stream")
            }
            encoder = MultipartEncoder(post_data)
            headers = {
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                "Referer": f"{BASE}/mydisk.php",
                "Content-Type": encoder.content_type,
            }
            resp = sess.post("https://pc.woozooo.com/fileup.php",
                           data=encoder, headers=headers, timeout=600)

        log(f"fileup.php: HTTP {resp.status_code}, len={len(resp.content)}")
        try:
            j = resp.json()
            if j.get("zt") == 1:
                file_id = int(j["text"][0]["id"])
                ok("Upload success (fileup.php)!")
                return get_share_info(sess, file_id)
            err(f"fileup.php failed: {j}")
        except:
            err(f"fileup.php response: {resp.text[:200]}")

        # Also try with uid
        uid = sess.cookies.get("ylogin", "5184736")
        with open(filepath, "rb") as f:
            post_data["folder_id_bb_n"] = str(folder_id)
            encoder = MultipartEncoder(post_data)
            headers["Content-Type"] = encoder.content_type
            resp = sess.post(f"https://pc.woozooo.com/fileup.php?uid={uid}",
                           data=encoder, headers=headers, timeout=600)
        log(f"fileup.php?uid=: HTTP {resp.status_code}, len={len(resp.content)}")
        log(f"Body: {resp.text[:200]}")
    except ImportError:
        err("requests_toolbelt not installed")
    return None


def get_share_info(sess, file_id):
    """Set password and get share URL."""
    pwd = "bill"
    data = {"task": "set_passwd", "file_id": file_id, "passwd": pwd}
    sess.post(FILE, data=data)
    time.sleep(0.5)

    data = {"task": "share_file", "file_id": file_id}
    resp = sess.post(FILE, data=data)
    try:
        j = resp.json()
        if j.get("zt") == 1:
            url = j.get("info", "")
            if url.startswith("http"):
                ok(f"Share URL: {url}  password: {pwd}")
                return {"url": url, "password": pwd}
    except:
        pass

    # Try via doupload
    data = {"task": 22, "file_id": file_id}
    resp = sess.post(DOUPLOAD, data=data)
    try:
        j = resp.json()
        if j.get("zt") == 1:
            ok(f"Share URL (doupload): {j.get('info', '')}")
            return {"url": j.get("info", ""), "password": pwd}
    except:
        pass

    err("Failed to get share info")
    return None


def update_version_json(lanzou_url, lanzou_pwd, md5):
    vf = PROJECT_DIR / "version.json"
    with open(vf, encoding="utf-8") as f:
        data = json.load(f)
    data["versionCode"] = 9
    data["versionName"] = "0.8.5"
    data["md5"] = md5
    data["lanzouUrl"] = lanzou_url
    data["lanzouPassword"] = lanzou_pwd
    for src in data.get("sources", []):
        if "2350471581" in src["url"]:
            src["url"] = re.sub(r"/v[\d.]+/", "/v0.8.5/", src["url"])
    with open(vf, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    ok(f"version.json updated (v0.8.5, md5={md5[:8]}...)")


def main():
    if not APK_PATH.exists():
        err(f"APK not found: {APK_PATH}")
        sys.exit(1)

    # Load cookies
    if not COOKIE_PATH.exists():
        err(f"Cookie file not found: {COOKIE_PATH}")
        sys.exit(1)

    with open(COOKIE_PATH) as f:
        cookies = json.load(f)
    log(f"Loaded {len(cookies)} cookies")

    # Initialize session
    sess = init_session(cookies)
    if not sess:
        err("Session init failed - need fresh cookies")
        err("Visit https://pc.woozooo.com/mydisk.php in your browser,")
        err("then F12 -> Console -> document.cookie")
        sys.exit(1)

    # Ensure folder
    folder_id = ensure_folder(sess)
    log(f"Using folder_id={folder_id}")

    # Calculate MD5
    md5 = hashlib.md5()
    with open(APK_PATH, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            md5.update(chunk)
    md5 = md5.hexdigest()
    log(f"MD5: {md5[:8]}...")

    # Upload - try file.php first
    share = upload_file(sess, APK_PATH, folder_id)
    if not share:
        log("file.php failed, trying fileup.php...")
        share = upload_file_v2(sess, APK_PATH, folder_id)

    if not share:
        err("All upload methods failed!")
        sys.exit(1)

    # Update version.json
    update_version_json(share["url"], share["password"], md5)
    ok("All done!")


if __name__ == "__main__":
    main()
