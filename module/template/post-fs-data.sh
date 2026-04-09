MODDIR=${0%/*}

# Ensure the persistent data directory exists (outside module dir so it survives updates).
# Matches DATA_DIR in example.cpp and ModuleInfo.DATA_DIR in the WebUI.
DATADIR="/data/adb/@MODULE_ID@"
mkdir -p "$DATADIR"
chmod 700 "$DATADIR"

# CJK font (NotoSansSC WOFF2 subset) is now bundled via Compose Resources
# (composeResources/font/) and loaded at runtime with Res.readBytes(),
# so no runtime font symlink is needed.
