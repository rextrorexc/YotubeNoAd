# YotubeNoAd

Basit YouTube reklam engelleyici. Reklam verisini oynatıcıya ulaşmadan siler, reklam isteklerini ağ seviyesinde keser.

## Arch Linux (AUR)

```bash
yay -S youtubenoad
youtubenoad-install
```

İkinci komut eklentiyi bulunan tarayıcılara ekler. Tek tarayıcı için: `youtubenoad-install firefox` veya `youtubenoad-install chromium`.

## Kaynaktan kurulum (Linux)

```bash
git clone https://github.com/rextroanticheat/YotubeNoAd
cd YotubeNoAd
./install.sh
```

Bulunan tüm tarayıcılara kurar. Tek tarayıcı için: `./install.sh firefox` veya `./install.sh chromium`.

## Elle kurulum

| Tarayıcı | Adımlar |
|---|---|
| Chrome, Edge, Brave, Opera, Vivaldi, Chromium | Eklentiler sayfasını aç, **Geliştirici modu**'nu aç, **Paketlenmemiş öğe yükle** ile bu klasörü seç |
| Firefox (128+) | `about:debugging#/runtime/this-firefox` > **Geçici Eklenti Yükle** > `manifest.json` |
| Safari | Doğrudan desteklenmiyor. macOS'ta `xcrun safari-web-extension-converter` ile Xcode projesine çevrilmesi gerekir |

Firefox'ta kalıcı kurulum için eklentinin Mozilla tarafından imzalanması gerekir.

## Paketleme

```bash
./build.sh
```

`dist/` klasöründe Chromium için `.zip`, Firefox için `.xpi` üretir.

## Dosyalar

- `inject.js`: reklam verisini sayfa bağlamında temizler
- `content.js`: sızan reklamı atlar, reklam engelleyici uyarısını kaldırır
- `rules.json`: reklam isteklerini engelleyen ağ kuralları
- `style.css`: reklam alanlarını gizler
- `background.js`, `popup.*`: rozet sayacı ve açma/kapama düğmesi
