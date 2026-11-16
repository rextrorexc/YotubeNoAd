"""Firefox'a eklentiyi geçici olarak yükler (Marionette üzerinden) ve YouTube'u açar.
Kullanım: python3 -I firefox_install.py <eklenti_klasörü>"""
import json, socket, sys, time

ADDON = sys.argv[1]


def connect():
    for _ in range(60):
        try:
            s = socket.create_connection(("127.0.0.1", 2828), timeout=60)
            s.recv(4096)  # karşılama mesajı
            return s
        except OSError:
            time.sleep(1)
    sys.exit("Firefox'a bağlanılamadı")


def send(s, i, name, params):
    body = json.dumps([0, i, name, params])
    s.sendall(f"{len(body)}:{body}".encode())
    buf = b""
    while b":" not in buf:
        buf += s.recv(4096)
    n, rest = buf.split(b":", 1)
    n = int(n)
    while len(rest) < n:
        rest += s.recv(4096)
    return json.loads(rest[:n])


s = connect()
send(s, 1, "WebDriver:NewSession", {})
res = send(s, 2, "Addon:Install", {"path": ADDON, "temporary": True})
if res[2]:
    sys.exit(f"Yükleme hatası: {res[2]}")
print("Firefox: eklenti yüklendi")
s.settimeout(3)
try:
    send(s, 3, "WebDriver:Navigate", {"url": "https://www.youtube.com"})
except OSError:
    pass  # sayfa yüklenirken yanıt gecikebilir, sorun değil
