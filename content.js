// Yedek katman: sızan reklamı atla / videoyu yeniden yükle + algılama duvarını kaldır.
let lastTime = 0;
let lastId = "";
let adSince = 0;
const reloads = {};

const videoId = () => new URLSearchParams(location.search).get("v") || location.pathname.split("/").pop();

function skipAd() {
  const player = document.getElementById("movie_player") || document.querySelector(".html5-video-player");
  if (!player) return;
  const video = player.querySelector("video");

  const id = videoId();
  if (id !== lastId) { lastId = id; lastTime = 0; adSince = 0; }

  if (!player.classList.contains("ad-showing")) {
    adSince = 0;
    if (video && !video.paused && video.currentTime > 0) lastTime = video.currentTime;
    return;
  }

  if (!adSince) adSince = Date.now();

  // 1) hızlı yol: sessize al, hızlandır, sona sar
  if (video && isFinite(video.duration) && video.duration > 0) {
    video.muted = true;
    video.playbackRate = 16;
    video.currentTime = video.duration;
  }

  // 2) atla butonu
  document
    .querySelector(".ytp-skip-ad-button, .ytp-ad-skip-button, .ytp-ad-skip-button-modern")
    ?.click();

  // 3) hâlâ reklam varsa asıl videoyu kaldığı yerden yeniden yükle
  if (Date.now() - adSince > 1500 && (reloads[id] || 0) < 2 && typeof player.loadVideoById === "function") {
    reloads[id] = (reloads[id] || 0) + 1;
    adSince = Date.now();
    player.loadVideoById(id, lastTime);
  }
}

function removeEnforcement() {
  const msg = document.querySelector("ytd-enforcement-message-view-model");
  if (!msg) return;
  document
    .querySelectorAll("tp-yt-paper-dialog:has(ytd-enforcement-message-view-model), tp-yt-iron-overlay-backdrop")
    .forEach((el) => el.remove());
  msg.remove();
  document.documentElement.style.overflow = "";
  document.body && (document.body.style.overflow = "");
  const v = document.querySelector("video");
  if (v && v.paused) v.play().catch(() => {});
}

// Shorts: reklam kartına gelirse sonrakine geç
function skipShortsAd() {
  const active = document.querySelector("ytd-reel-video-renderer[is-active]");
  if (active && active.querySelector("ytd-ad-slot-renderer, .ytReelAdMetapanelViewModelHost")) {
    document.querySelector("#navigation-button-down button")?.click();
  }
}

// Açık/kapalı durumunu MAIN world'e ve CSS'e yansıt; sayacı arka plana ilet.
let enabled = true;
try {
  chrome.runtime.sendMessage({ type: "reset" });
  chrome.storage.local.get("enabled").then((r) => {
    enabled = r.enabled !== false;
    document.documentElement.dataset.yna = enabled ? "on" : "off";
  });
  window.addEventListener("yna-blocked", (e) =>
    chrome.runtime.sendMessage({ type: "blocked", n: e.detail })
  );
} catch {}

function tick() {
  if (!enabled) return;
  skipAd();
  removeEnforcement();
  skipShortsAd();
}

new MutationObserver(tick).observe(document, {
  subtree: true,
  childList: true,
  attributes: true,
  attributeFilter: ["class"],
});
setInterval(tick, 300);
