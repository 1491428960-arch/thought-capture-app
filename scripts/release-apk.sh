#!/bin/bash
# 构建 Release APK、创建 GitHub Release、更新 ideas 仓库 version.json
# 用法：bash scripts/release-apk.sh <version> <changelog>
# 例如：bash scripts/release-apk.sh "1.2" "修复收件箱状态同步；新增远程自更新"

set -e

VERSION="${1:?需要 version 参数，如 1.2}"
CHANGELOG="${2:?需要 changelog 参数}"
VERSION_CODE=$(date +%Y%m%d%H)
APK_NAME="app-release.apk"
RELEASE_TAG="v${VERSION_CODE}"
IDEAS_DIR="$HOME/ideas"
REPO="1491428960-arch/thought-capture-app"

echo "=== 1. 构建 APK ==="
./gradlew assembleRelease
APK_PATH="app/build/outputs/apk/release/${APK_NAME}"
if [ ! -f "$APK_PATH" ]; then
    echo "APK 构建失败：$APK_PATH 不存在"
    exit 1
fi
echo "APK: $APK_PATH"

echo "=== 2. 创建 GitHub Release ==="
gh release create "$RELEASE_TAG" "$APK_PATH" \
    --repo "$REPO" \
    --title "v${VERSION} (build ${VERSION_CODE})" \
    --notes "${CHANGELOG}"

APK_URL="https://github.com/${REPO}/releases/download/${RELEASE_TAG}/${APK_NAME}"
echo "Release URL: $APK_URL"

echo "=== 3. 更新 ideas 仓库 version.json ==="
cd "$IDEAS_DIR"
git pull

cat > version.json << VEOF
{
  "version": "${VERSION}",
  "version_code": ${VERSION_CODE},
  "apk_url": "${APK_URL}",
  "changelog": "${CHANGELOG}",
  "min_version_code": 0
}
VEOF

git add version.json
git commit -m "release: v${VERSION} build ${VERSION_CODE} — ${CHANGELOG}"
git push

echo "=== 完成 ==="
echo "version_code: $VERSION_CODE"
echo "APK: $APK_URL"
echo "App 下次 sync 后将提示用户更新。"
