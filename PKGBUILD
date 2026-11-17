pkgname=youtubenoad
_repo=YotubeNoAd
pkgver=1.5
pkgrel=1
pkgdesc="Basit YouTube reklam engelleyici tarayici eklentisi (Chromium ve Firefox)"
arch=('any')
url="https://github.com/rextrorexc/${_repo}"
license=('MIT')
depends=('bash' 'python')
optdepends=('firefox: Firefox icin gecici kurulum'
            'chromium: Chromium icin otomatik kurulum'
            'brave-bin: Brave icin otomatik kurulum'
            'vivaldi: Vivaldi icin otomatik kurulum')
install="${pkgname}.install"
source=("${pkgname}-${pkgver}.tar.gz::${url}/archive/refs/tags/v${pkgver}.tar.gz")
sha256sums=('2592e090d8bc4a84840a73d6c0102ea3d551d0c2e703feb2e9f899afc4e8a052')

package() {
  cd "${_repo}-${pkgver}"
  local dest="${pkgdir}/usr/share/${pkgname}"

  install -d "${dest}/tools"
  install -m644 manifest.json background.js content.js inject.js \
                popup.html popup.js rules.json style.css "${dest}/"
  install -m755 install.sh "${dest}/install.sh"
  install -m644 tools/firefox_install.py "${dest}/tools/firefox_install.py"

  install -d "${pkgdir}/usr/bin"
  ln -s "/usr/share/${pkgname}/install.sh" "${pkgdir}/usr/bin/youtubenoad-install"

  install -Dm644 README.md "${pkgdir}/usr/share/doc/${pkgname}/README.md"
  install -Dm644 LICENSE "${pkgdir}/usr/share/licenses/${pkgname}/LICENSE"
}
