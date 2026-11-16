// Rozet sayacı + açma/kapama (ağ kurallarını da aç/kapat).
const counts = {};

function badge(tabId) {
  const n = counts[tabId] || 0;
  chrome.action.setBadgeBackgroundColor({ color: "#d33" });
  chrome.action.setBadgeText({ tabId, text: n ? String(n) : "" });
}

chrome.runtime.onMessage.addListener((msg, sender) => {
  const tabId = sender.tab?.id;
  if (tabId == null) return;
  if (msg.type === "reset" && sender.frameId === 0) counts[tabId] = 0;
  if (msg.type === "blocked") counts[tabId] = (counts[tabId] || 0) + msg.n;
  badge(tabId);
});

chrome.tabs.onRemoved.addListener((id) => delete counts[id]);

function applyEnabled(enabled) {
  chrome.declarativeNetRequest.updateEnabledRulesets(
    enabled ? { enableRulesetIds: ["ads"] } : { disableRulesetIds: ["ads"] }
  );
}

chrome.storage.onChanged.addListener((ch) => {
  if (ch.enabled) applyEnabled(ch.enabled.newValue !== false);
});
chrome.runtime.onStartup.addListener(async () => {
  const { enabled } = await chrome.storage.local.get("enabled");
  applyEnabled(enabled !== false);
});
