const cloudCheckbox = document.getElementById('cloud');
const urlInput = document.getElementById('url');
const urlContainer = document.getElementById('url-container');
const eventInput = document.getElementById('event');
const eventContainer = document.getElementById('event-container');
const enabled = document.getElementById('enabled');

chrome.storage.local.get(['url', 'cloud', 'event', 'changed', 'enabled'], item => {
    console.log(item);

    if (item.url == undefined || item.cloud == undefined || item.event == undefined || item.changed == undefined || item.enabled == undefined) {
        item = {
            url: item.url || 'ws://localhost:3001/ws/',
            cloud: item.cloud ?? true,
            event: item.event || '2024event',
            changed: item.changed || new Date().getTime(),
            enabled: item.enabled ?? false
        };
        chrome.storage.local.set(item);
    }
    cloudCheckbox.checked = item.cloud;
    urlInput.value = item.url;
    eventInput.value = item.event;
    enabled.checked = item.enabled;

    urlContainer.style.display = item.cloud ? 'none' : 'block';

    if (item.changed + (1000 * 60 * 60 * 24 * 4) < new Date().getTime()) {
        enabled.checked = false;
        chrome.storage.local.set({ enabled: false });
    }
});

function handleUpdate() {
    if (eventInput.value == '') {
        eventInput.value = '2024event';
    }
    if (urlInput.value == '') {
        urlInput.value = 'ws://localhost:3001/ws/';
    }

    console.log({ url: urlInput.value, cloud: cloudCheckbox.checked, event: eventInput.value, changed: new Date().getTime(), enabled: enabled.checked })
    chrome.storage.local.set({ url: urlInput.value, cloud: cloudCheckbox.checked, event: eventInput.value, changed: new Date().getTime(), enabled: enabled.checked });

    urlContainer.style.display = item.cloud ? 'none' : 'block';
}

cloudCheckbox.addEventListener('input', handleUpdate);
urlInput.addEventListener('input', handleUpdate);
eventInput.addEventListener('input', handleUpdate);
enabled.addEventListener('input', handleUpdate);
