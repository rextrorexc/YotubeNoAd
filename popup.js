const box = document.getElementById("on");

chrome.storage.local.get("enabled").then(({ enabled }) => {
  box.checked = enabled !== false;
});

box.addEventListener("change", async () => {
  await chrome.storage.local.set({ enabled: box.checked });
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (tab?.url && /youtube\.com/.test(tab.url)) chrome.tabs.reload(tab.id);
});
