// MAIN world: reklam verisini oynatıcı/sayfa görmeden temizler.
(() => {
  const AD_KEYS = ["adPlacements", "playerAds", "adSlots", "adBreakHeartbeatParams"];
  const AD_ITEM_KEYS = [
    "adSlotRenderer",
    "promotedSparklesWebRenderer",
    "displayAdRenderer",
    "bannerPromoRenderer",
    "inFeedAdLayoutRenderer",
  ];
  const QUICK = /adPlacements|playerAds|adSlots|adSlotRenderer|promotedSparkles|displayAdRenderer|bannerPromoRenderer|inFeedAdLayoutRenderer/;

  let removed = 0;

  function clean(o, depth = 0) {
    if (!o || typeof o !== "object" || depth > 40) return o;
    if (Array.isArray(o)) {
      for (let i = o.length - 1; i >= 0; i--) {
        const it = o[i];
        if (it && typeof it === "object") {
          const inner = it.richItemRenderer?.content || it;
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

  const isOff = () => document.documentElement?.dataset.yna === "off";

  // Dış giriş noktası: kapalıysa dokunma, temizlenen varsa sayaca bildir.
  function run(o) {
    if (isOff()) return o;
    removed = 0;
    clean(o);
    if (removed) window.dispatchEvent(new CustomEvent("yna-blocked", { detail: removed }));
    return o;
  }

  const origParse = JSON.parse;
  const parseClean = (t) => { try { return run(origParse(t)); } catch { return null; } };

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
      if (isOff() || !QUICK.test(t)) return t;
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
        if (typeof t !== "string" || isOff() || !QUICK.test(t)) return t;
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
})();
