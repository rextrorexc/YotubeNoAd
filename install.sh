#!/usr/bin/env bash
# YotubeNoAd tek komutla kurulum (Linux).
#   ./install.sh            -> bulunan tüm tarayıcılara kur
#   ./install.sh firefox    -> sadece Firefox
#   ./install.sh chromium   -> sadece Chromium tabanlılar
set -u

SRC="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
DEST="$HOME/.local/share/YotubeNoAd"
FILES=(manifest.json background.js content.js inject.js popup.html popup.js rules.json style.css)
WANT="${1:-all}"

# Eklentiyi kalıcı bir klasöre kopyala (proje klasörü taşınsa da bozulmasın)
mkdir -p "$DEST"
for f in "${FILES[@]}"; do cp "$SRC/$f" "$DEST/$f"; done
echo "Eklenti dosyaları: $DEST"

# Yol panoya kopyalansın (elle kurulum gerekirse yapıştırmak için)
if command -v wl-copy >/dev/null; then printf %s "$DEST" | wl-copy
elif command -v xclip >/dev/null; then printf %s "$DEST" | xclip -selection clipboard; fi

found=0

# ---- Chromium tabanlılar --------------------------------------------------
if [[ $WANT == all || $WANT == chromium ]]; then
  for b in chromium chromium-browser brave brave-browser vivaldi vivaldi-stable opera \
           google-chrome-stable google-chrome microsoft-edge-stable microsoft-edge; do
    command -v "$b" >/dev/null || continue
    found=1
    echo "-> $b"
    case $b in
      google-chrome*|microsoft-edge*)
        # Chrome/Edge 137+ --load-extension bayrağını kaldırdı: elle yükleme sayfası aç
        "$b" chrome://extensions >/dev/null 2>&1 &
        echo "   Geliştirici modunu aç > 'Paketlenmemiş öğe yükle' > klasör yolu panoda: $DEST"
        ;;
      *)
        "$b" --load-extension="$DEST" https://www.youtube.com >/dev/null 2>&1 &
        ;;
    esac
  done
fi

# ---- Firefox ----------------------------------------------------------------
if [[ $WANT == all || $WANT == firefox ]]; then
  for b in firefox firefox-developer-edition librewolf zen-browser; do
    command -v "$b" >/dev/null || continue
    found=1
    echo "-> $b (geçici yükleme, tarayıcı kapanınca silinir)"
    PROFILE="$HOME/.local/share/YotubeNoAd-firefox-profile"
    mkdir -p "$PROFILE"
    "$b" --no-remote --marionette --profile "$PROFILE" >/dev/null 2>&1 &
    python3 -I "$SRC/tools/firefox_install.py" "$DEST"
    break
  done
fi

((found)) || echo "Desteklenen tarayıcı bulunamadı. README.md'deki elle kurulumu deneyin."
