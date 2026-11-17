// Sayfa baslarken calisir: reklam verisini temizler, sizan reklami atlar, reklam alanlarini gizler.
(() => {
  if (window.__YNA_OFF || window.__YNA_LOADED) return;
  window.__YNA_LOADED = true;

  const AD_KEYS = ["adPlacements", "playerAds", "adSlots", "adBreakHeartbeatParams"];
  const AD_ITEM_KEYS = [
    "adSlotRenderer",
    "promotedSparklesWebRenderer",
    "displayAdRenderer",
    "bannerPromoRenderer",
    "inFeedAdLayoutRenderer",
    "compactPromotedVideoRenderer",
    "promotedVideoRenderer",
  ];
  const QUICK = /adPlacements|playerAds|adSlots|adSlotRenderer|promotedSparkles|displayAdRenderer|bannerPromoRenderer|inFeedAdLayoutRenderer|compactPromotedVideoRenderer|promotedVideoRenderer/;

  let removed = 0;

  function clean(o, depth = 0) {
    if (!o || typeof o !== "object" || depth > 40) return o;
    if (Array.isArray(o)) {
      for (let i = o.length - 1; i >= 0; i--) {
        const it = o[i];
        if (it && typeof it === "object") {
          const inner = (it.richItemRenderer && it.richItemRenderer.content) || it;
          if (AD_ITEM_KEYS.some((k) => k in inner)) { o.splice(i, 1); removed++; continue; }
          clean(it, depth + 1);
        }
      }
      return o;
    }
    for (const k of AD_KEYS) if (k in o) { delete o[k]; removed++; }
    for (const k in o) {
      const v = o[k];
      if (v && typeof v === "object") clean(v, depth + 1);
    }
    return o;
  }

  function run(o) {
    removed = 0;
    clean(o);
    if (removed) {
      try { if (window.YNA) window.YNA.blocked(removed); } catch (e) {}
    }
    return o;
  }

  const origParse = JSON.parse;
  const parseClean = (t) => { try { return run(origParse(t)); } catch (e) { return null; } };

  JSON.parse = function (text, ...rest) {
    const r = origParse.call(this, text, ...rest);
    return typeof text === "string" && QUICK.test(text) ? run(r) : r;
  };

  const origJson = Response.prototype.json;
  Response.prototype.json = function () {
    return origJson.call(this).then(run);
  };
  const origText = Response.prototype.text;
  Response.prototype.text = function () {
    return origText.call(this).then((t) => {
      if (!QUICK.test(t)) return t;
      const o = parseClean(t);
      return o ? JSON.stringify(o) : t;
    });
  };

  const xhrResp = Object.getOwnPropertyDescriptor(XMLHttpRequest.prototype, "responseText");
  if (xhrResp && xhrResp.get) {
    Object.defineProperty(XMLHttpRequest.prototype, "responseText", {
      configurable: true,
      get() {
        const t = xhrResp.get.call(this);
        if (typeof t !== "string" || !QUICK.test(t)) return t;
        const o = parseClean(t);
        return o ? JSON.stringify(o) : t;
      },
    });
  }

  for (const name of ["ytInitialPlayerResponse", "ytInitialData"]) {
    let val;
    Object.defineProperty(window, name, {
      configurable: true,
      get: () => val,
      set: (v) => { val = run(v); },
    });
  }

  // ---- Gorunum: reklam alanlarini gizle ----
  const css = `
    #player-ads, #masthead-ad, .ytp-ad-overlay-container, .ytp-ad-image-overlay, .ytp-ad-module,
    ytm-promoted-sparkles-web-renderer, ytm-promoted-video-renderer, ytm-companion-slot,
    ytm-companion-ad-renderer, ytm-ad-slot-renderer, ytm-in-feed-ad-layout-renderer,
    ytm-display-ad-renderer, ytm-statement-banner-renderer, ytm-mealbar-promo-renderer,
    ytm-brand-video-singleton-renderer, ytm-search-pyv-renderer, ytm-banner-promo-renderer,
    ytm-rich-item-renderer:has(ytm-ad-slot-renderer),
    ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer, ytd-banner-promo-renderer,
    ytd-promoted-sparkles-web-renderer, ytd-promoted-video-renderer, ytd-display-ad-renderer,
    ytd-enforcement-message-view-model, ytm-enforcement-message-view-model {
      display: none !important;
    }`;
  const style = document.createElement("style");
  style.textContent = css;
  (document.head || document.documentElement).appendChild(style);

  // ---- Yedek: sizan reklami atla / yeniden yukle, uyari duvarini kaldir ----
  let lastTime = 0, lastId = "", adSince = 0;
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

    if (video && isFinite(video.duration) && video.duration > 0) {
      video.muted = true;
      video.playbackRate = 16;
      video.currentTime = video.duration;
    }
    const skip = document.querySelector(".ytp-skip-ad-button, .ytp-ad-skip-button, .ytp-ad-skip-button-modern");
    if (skip) skip.click();

    if (Date.now() - adSince > 1500 && (reloads[id] || 0) < 2 && typeof player.loadVideoById === "function") {
      reloads[id] = (reloads[id] || 0) + 1;
      adSince = Date.now();
      player.loadVideoById(id, lastTime);
    }
  }

  function removeEnforcement() {
    const msg = document.querySelector("ytd-enforcement-message-view-model, ytm-enforcement-message-view-model");
    if (!msg) return;
    document.querySelectorAll("tp-yt-iron-overlay-backdrop").forEach((el) => el.remove());
    msg.remove();
    document.documentElement.style.overflow = "";
    if (document.body) document.body.style.overflow = "";
    const v = document.querySelector("video");
    if (v && v.paused) v.play().catch(() => {});
  }

  function tick() { skipAd(); removeEnforcement(); }

  new MutationObserver(tick).observe(document, {
    subtree: true, childList: true, attributes: true, attributeFilter: ["class"],
  });
  setInterval(tick, 300);
})();
