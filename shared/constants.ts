import { MonitorFrame } from "./types";

export const DEFAULT_MONITOR: MonitorFrame = {
    type: 'monitorUpdate',
    field: 0,
    match: 0,
    time: 'unk',
    blue1: {
        number: 9999,
        ds: 0,
        radio: 0,
        rio: 0,
        code: 0,
        bwu: 0,
        battery: 0,
        ping: 0,
        packets: 0,
    },
    blue2: {
        number: 9998,
        ds: 2,
        radio: 0,
        rio: 0,
        code: 0,
        bwu: 0,
        battery: 0,
        ping: 0,
        packets: 0,
    },
    blue3: {
        number: 9997,
        ds: 4,
        radio: 0,
        rio: 0,
        code: 0,
        bwu: 0,
        battery: 0,
        ping: 0,
        packets: 0,
    },
    red1: {
        number: 9996,
        ds: 1,
        radio: 1,
        rio: 0,
        code: 0,
        bwu: 0,
        battery: 0,
        ping: 0,
        packets: 0,
    },
    red2: {
        number: 9995,
        ds: 1,
        radio: 1,
        rio: 1,
        code: 0,
        bwu: 0,
        battery: 0,
        ping: 0,
        packets: 0,
    },
    red3: {
        number: 9994,
        ds: 1,
        radio: 1,
        rio: 1,
        code: 1,
        bwu: 0.5,
        battery: 12.5,
        ping: 10,
        packets: 12,
    }
};
