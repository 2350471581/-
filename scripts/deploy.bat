@echo off
chcp 65001 >nul
cd /d "%~dp0.."

echo ========================================
echo  BillTracker 发布助手
echo ========================================
echo.

:: 1. 构建
echo [1/4] 构建 Release APK...
call ./gradlew assembleRelease --quiet
if %ERRORLEVEL% neq 0 (
    echo ❌ 构建失败
    pause
    exit /b 1
)
echo ✅ 构建成功
echo.

:: 2. 找 APK
echo [2/4] 查找 APK 文件...
set "APK_PATH="
for /r "app\build\outputs\apk\release" %%f in (*.apk) do set "APK_PATH=%%f"
if not defined APK_PATH (
    for /r "app\build\intermediates\apk\release" %%f in (*.apk) do set "APK_PATH=%%f"
)
if not defined APK_PATH (
    echo ❌ 找不到 APK 文件
    pause
    exit /b 1
)
echo ✅ APK: %APK_PATH%
echo.

:: 3. 计算 MD5
echo [3/4] 计算 MD5...
set "MD5="
for /f "skip=1 tokens=*" %%i in ('certutil -hashfile "%APK_PATH%" MD5') do (
    if not defined MD5 set "MD5=%%i"
)
set "MD5=%MD5: =%"
echo ✅ MD5: %MD5%
echo.

:: 4. 生成 version.json
echo [4/4] 生成 version.json...

:: 解析版本号
for /f "tokens=*" %%a in ('findstr "versionName" app\build.gradle.kts') do set "LINE=%%a"
for /f "tokens=2 delims=^"" %%v in ('findstr "versionName" app\build.gradle.kts') do set "VERSION_NAME=%%v"
for /f "tokens=*" %%c in ('findstr "versionCode" app\build.gradle.kts') do set "VCODE_LINE=%%c"

:: 提取 versionCode
set "VERSION_CODE="
for /f "tokens=*" %%a in ('findstr /r "versionCode *= *[0-9]" app\build.gradle.kts') do (
    for /f "tokens=3 delims= " %%b in ("%%a") do set "VERSION_CODE=%%b"
)

if not defined VERSION_CODE (
    echo ⚠️  无法解析版本号，使用默认值 1
    set VERSION_CODE=1
)
if not defined VERSION_NAME set VERSION_NAME=1.0

echo 📋 版本: %VERSION_CODE% (%VERSION_NAME%)

(
echo {
echo   "versionCode": %VERSION_CODE%,
echo   "versionName": "%VERSION_NAME%",
echo   "releaseNotes": "",
echo   "md5": "%MD5%",
echo   "sources": [
echo     {
echo       "url": "https://github.com/2350471581/-/releases/download/v%VERSION_NAME%/记账助手.apk",
echo       "priority": 0,
echo       "label": "GitHub 直连"
echo     },
echo     {
echo       "url": "https://cdn.jsdelivr.net/gh/2350471581/-@v%VERSION_NAME%/记账助手.apk",
echo       "priority": 1,
echo       "label": "jsDelivr 镜像"
echo     },
echo     {
echo       "url": "https://ghproxy.com/https://github.com/2350471581/-/releases/download/v%VERSION_NAME%/记账助手.apk",
echo       "priority": 2,
echo       "label": "ghproxy 备用"
echo     }
echo   ]
echo }
) > version.json

echo ✅ version.json 已生成
echo.
echo ========================================
echo  🎉 完成!
echo.
echo  下一步:
echo   1. 手动上传 APK 到 GitHub Release
echo      gh release create v%VERSION_NAME% "%APK_PATH%" --title "v%VERSION_NAME%"
echo.
echo   2. 编辑 version.json 补充 releaseNotes
echo.
echo   3. 提交 version.json 并推送:
echo      git add version.json
echo      git commit -m "chore: update version.json for v%VERSION_NAME%"
echo      git push
echo ========================================
echo.

pause
