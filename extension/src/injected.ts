console.log('Injection loaded');

let url = document.getElementById('fta-buddy')?.dataset.host;
let cloud = document.getElementById('fta-buddy')?.dataset.cloud;
let eventCode = document.getElementById('fta-buddy')?.dataset.event;
let version = document.getElementById('fta-buddy')?.dataset.version;

function read(station: string) {
    const radioStats = document.getElementById(station + 'BWU')?.title;

    const radioElm = document.getElementById(station + 'radio');
    const rioElm = document.getElementById(station + 'robot');
    const enabledElm = document.getElementById(station + 'enabled');

    const obj = {
        number: document.getElementById(station + 'Number')?.innerText,
        ds: identifyStatusDS(station),
        radio: radioElm ? identifyStatus(radioElm) : 0,
        rio: rioElm ? identifyStatus(rioElm) : 0,
        code: (document.getElementById(station + 'Row')?.classList.contains('notReadyYellow')) ? 0 : 1,
        bwu: parseFloat(document.getElementById(station + 'BWU')?.innerText ?? '0'),
        battery: parseFloat(document.getElementById(station + 'Battery')?.innerText ?? '0'),
        ping: parseInt(document.getElementById(station + 'AvgTrip')?.innerText ?? '0'),
        packets: parseInt(document.getElementById(station + 'MissedPackets')?.innerText ?? '0'),
        versionmm: document.getElementById(station + 'versionmm')?.style.display === 'none' ? 0 : 1,
        signal: (radioStats) ? parseInt(radioStats.split('Signal: ')[1].split(' (')[0]) : null,
        noise: (radioStats) ? parseInt(radioStats.split('Noise: ')[1].split(' (')[0]) : null,
        SNR: (radioStats) ? parseInt(radioStats.split('SNR: ')[1].split(' ')[0]) : null,
        TX: (radioStats) ? parseInt(radioStats.split('TX Rate: ')[1].split(' ')[0]) : null,
        TXMCS: (radioStats) ? parseInt(radioStats.split('TX MCS: ')[1].split(' ')[0]) : null,
        RX: (radioStats) ? parseInt(radioStats.split('RX Rate: ')[1].split(' ')[0]) : null,
        RXMCS: (radioStats) ? parseInt(radioStats.split('RX MCS: ')[1].split(' ')[0]) : null,
        MAC: (radioStats) ? document.getElementById(station + 'Number')?.title.split('MAC: ')[1] : null,
        enabled: (radioStats && enabledElm) ? identifyEnableStatus(enabledElm) : null,
    };

    // If the station is bypassed
    if (obj.ds === 5) {
        obj.code = 0;
    }

    return obj
}

function identifyStatusDS(station: string) {
    if (document.getElementById(station + 'Row')?.classList.contains('bypassed')) return 5;
    if (document.getElementById(station + 'enabled')?.classList.contains('fieldMonitor-blackDiamondE')) return 6;
    if (document.getElementById(station + 'enabled')?.classList.contains('fieldMonitor-blackDiamondA')) return 7;
    let elm = document.getElementById(station + 'ds')
    return elm ? identifyStatus(elm) : 0;
}

function identifyStatus(elm: HTMLElement) {
    if (elm.classList.contains('fieldMonitor-redSquare')) return 0;
    if (elm.classList.contains('fieldMonitor-redSquareB')) return 0;
    if (elm.classList.contains('fieldMonitor-greenCircle')) return 1;
    if (elm.classList.contains('fieldMonitor-greenCircleX')) return 2;
    if (elm.classList.contains('fieldMonitor-yellowCircleM')) return 3;
    if (elm.classList.contains('fieldMonitor-yellowCircleW')) return 4;
}

function identifyEnableStatus(elm: HTMLElement) {
    if (elm.classList.contains('fieldMonitor-redSquare')) return 0;
    if (elm.classList.contains('fieldMonitor-redSquareA')) return 8;
    if (elm.classList.contains('fieldMonitor-redSquareT')) return 9;
    if (elm.classList.contains('fieldMonitor-greenCircleA')) return 10;
    if (elm.classList.contains('fieldMonitor-greenCircleT')) return 11;
    if (elm.classList.contains('fieldMonitor-blackDiamondE')) return 6;
    if (elm.classList.contains('fieldMonitor-blackDiamondA')) return 7;
}

function identifyFieldStatus(elm: HTMLElement) {
    if (elm.innerText === 'UNKNOWN') return 0;
    if (elm.innerText === 'MATCH RUNNING (TELEOP)') return 1;
    if (elm.innerText === 'MATCH TRANSITIONING') return 2;
    if (elm.innerText === 'MATCH RUNNING (AUTO)') return 3;
    if (elm.innerText === 'MATCH READY') return 4;
    if (elm.innerText === 'MATCH NOT READY') return 11;
    if (elm.innerText === 'PRE-START COMPLETED') return 5;
    if (elm.innerText === 'PRE-START INITIATED') return 6;
    if (elm.innerText === 'READY TO PRE-START') return 7;
    if (elm.innerText === 'MATCH ABORTED') return 8;
    if (elm.innerText === 'MATCH OVER') return 9;
    if (elm.innerText === 'READY FOR POST-RESULT') return 10;
}

function sendUpdate() {
    let elm = document.getElementById('matchStateTop');
    let data = {
        frameTime: (new Date()).getTime(),
        type: 'monitorUpdate',
        version: version,
        field: elm ? identifyFieldStatus(elm) : 0,
        match: document.getElementById('MatchNumber')?.innerText.substring(3),
        time: document.getElementById('aheadbehind')?.innerText,
        blue1: read('blue1'),
        blue2: read('blue2'),
        blue3: read('blue3'),
        red1: read('red1'),
        red2: read('red2'),
        red3: read('red3')
    };

    if (ws.readyState === 1) ws.send(JSON.stringify(data));
}

let ws: WebSocket;

function connectToWS() {
    if (ws) ws.close();

    console.log('Trying to connect to ' + ((cloud == "true") ? 'cloud' : url));
    ws = new WebSocket((cloud == "true") ? `wss://ftabuddy.com/ws/` : url ?? 'ws://localhost:8080');
    ws.onopen = () => {
        ws.send(`server-${eventCode}`);
        console.log('Connected to server');
        setTimeout(sendUpdate, 100);
    }
    ws.onclose = () => {
        console.log('Disconnected from server, reconnecting in 5 seconds.');
        setTimeout(connectToWS, 5000);
    }
    ws.onerror = (err) => {
        console.error(err);
    }
}

connectToWS();
const elmToObserve = document.getElementById('monitorData');
if (elmToObserve) new MutationObserver(sendUpdate).observe(elmToObserve, { attributes: true, characterData: true, childList: true, subtree: true });