#!/usr/bin/env bash
# Dağıtım paketleri üretir: dist/yotubenoad.zip (Chromium) ve dist/yotubenoad.xpi (Firefox).
set -eu
cd "$(dirname "$0")"
FILES=(manifest.json background.js content.js inject.js popup.html popup.js rules.json style.css)
mkdir -p dist
rm -f dist/yotubenoad.zip dist/yotubenoad.xpi
python3 -I - "${FILES[@]}" <<'PY'
import sys, zipfile
for out in ("dist/yotubenoad.zip", "dist/yotubenoad.xpi"):
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sys.argv[1:]:
            z.write(f)
PY
echo "dist/yotubenoad.zip ve dist/yotubenoad.xpi hazır"
