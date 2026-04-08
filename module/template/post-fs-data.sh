MODDIR=${0%/*}

# Ensure the persistent data directory exists (outside module dir so it survives updates).
# Matches DATA_DIR in example.cpp and ModuleInfo.DATA_DIR in the WebUI.
DATADIR="/data/adb/@MODULE_ID@"
mkdir -p "$DATADIR"
chmod 700 "$DATADIR"

# Symlink system CJK font into webroot so wasmJs (Skia/Canvas) can fetch it.
# Compose for Web cannot access system fonts from the WASM sandbox.
CJK_FONT="/system/fonts/NotoSansCJK-Regular.ttc"
WEBROOT_FONT="${MODDIR}/webroot/NotoSansCJK-Regular.ttc"
[ -f "$CJK_FONT" ] && [ ! -e "$WEBROOT_FONT" ] && ln -s "$CJK_FONT" "$WEBROOT_FONT"
