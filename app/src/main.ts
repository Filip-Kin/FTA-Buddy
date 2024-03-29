import "./app.pcss";
import App from "./App.svelte";
import { createTRPCClient, httpBatchLink } from '@trpc/client';
import type { AppRouter } from '../../src/index';
import { authStore } from "./stores/auth";
import { get } from "svelte/store";
import { settingsStore } from "./stores/settings";

let token = get(authStore).token;
let eventToken = get(authStore).eventToken;

export let server = (get(settingsStore).forceCloud) ? 'https://ftabuddy.com' : window.location.origin;

export let trpc = createTRPCClient<AppRouter>({
    links: [
        httpBatchLink({
            url: server + '/trpc',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Event-Token': eventToken
            }
        }),
    ],
});

authStore.subscribe((value) => {
    token = value.token;
    eventToken = value.eventToken;
    trpc = createTRPCClient<AppRouter>({
        links: [
            httpBatchLink({
                url: server + '/trpc',
                headers: {
                    'Authorization': `Bearer ${get(authStore).token}`,
                    'Event-Token': eventToken
                }
            }),
        ],
    });
});

const app = new App({
    target: document.getElementById("app"),
});

export default app;

if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register("/serviceworker.js");
}
