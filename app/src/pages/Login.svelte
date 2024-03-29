<script lang="ts">
    import {
        Button,
        Input,
        Label,
        Select,
        Spinner,
        type SelectOptionType,
    } from "flowbite-svelte";
    import { trpc } from "../main";
    import { authStore } from "../stores/auth";
    import { eventStore } from "../stores/event";
    import { navigate } from "svelte-routing";

    export let toast: (title: string, text: string, color?: string) => void;

    let auth = $authStore;
    let event = $eventStore;

    let email = "";
    let username = "";
    let password = "";
    let verifyPassword = "";
    let role: "ADMIN" | "FTA" | "FTAA" | "CSA" = "FTA";

    let loading = false;
    let view: null | "login" | "create" = null;

    async function createUser(evt: Event) {
        evt.preventDefault();
        loading = true;

        if (password !== verifyPassword) {
            toast("Error", "Passwords do not match");
            return;
        }

        try {
            const res = await trpc.user.createAccount.query({
                email,
                username,
                password,
                role,
            });

            authStore.set({
                token: res.token,
                eventToken: "",
                user: {
                    username,
                    email,
                    role,
                    id: res.id,
                },
            });

            toast("Success", "Account created successfully", "green-500");
        } catch (err: any) {
            toast("Error Creating Account", err.message);
            console.error(err);
        }

        loading = false;
    }

    async function login(evt: Event) {
        evt.preventDefault();
        loading = true;

        try {
            const res = await trpc.user.login.query({ email, password });

            authStore.set({
                token: res.token,
                eventToken: "",
                user: {
                    username: res.username,
                    email: res.email,
                    role: res.role,
                    id: res.id,
                },
            });

            toast("Success", "Logged in successfully", "green-500");
        } catch (err: any) {
            toast("Error Logging In", err.message);
            console.error(err);
        }

        loading = false;
    }

    function logout() {
        authStore.set({ token: "", eventToken: "", user: undefined });
    }

    authStore.subscribe((value) => {
        console.log(value);
        auth = value;
        updateEventList();
    });

    let eventCode = "";
    let eventPin = "";

    async function createEvent() {
        loading = true;

        try {
            const res = await trpc.event.create.query({
                code: eventCode,
                pin: eventPin,
            });
            console.log(res);
            authStore.set({ ...auth, eventToken: res.token });
            eventStore.set({
                code: eventCode,
                pin: eventPin,
                teams: res.teams,
            });
            toast("Success", "Event created successfully", "green-500");
            updateEventList();
        } catch (err: any) {
            toast("Error Creating Event", err.message);
            console.error(err);
        }

        loading = false;
    }

    let eventList: SelectOptionType<string>[] = [];

    function updateEventList() {
        trpc.event.getAll.query().then((res) => {
            eventList = res.map((e) => ({ value: e.code, name: e.code }));
            eventList.unshift({ value: "none", name: "None" });
            eventList = eventList;
            console.log(eventList);
        });
    }

    if (auth.user?.role === "ADMIN") updateEventList();

    async function adminSelectEvent() {
        if (event.code === "none") {
            authStore.set({ ...auth, eventToken: "" });
            eventStore.set({ code: "", pin: "", teams: [] });
            return;
        }
        try {
            const res = await trpc.event.get.query({ code: event.code });
            authStore.set({ ...auth, eventToken: res.token });
            eventStore.set({
                code: event.code,
                pin: res.pin,
                teams: res.teams,
            });
            eventCode = event.code;
            eventPin = res.pin;
        } catch (err: any) {
            toast("Error", err.message);
            console.error(err);
        }
    }

    async function joinEvent(evt: Event) {
        evt.preventDefault();
        loading = true;

        try {
            const res = await trpc.event.join.query({
                code: eventCode,
                pin: eventPin,
            });
            authStore.set({ ...auth, eventToken: res.token });
            eventStore.set({
                code: eventCode,
                pin: eventPin,
                teams: (res.teams as any) || [],
            });
            toast("Success", "Event joined successfully", "green-500");
            setTimeout(() => navigate("/app/"), 50);
        } catch (err: any) {
            toast("Error Joining Event", err.message);
            console.error(err);
        }

        loading = false;
    }
</script>

{#if loading}
    <div class="fixed w-full h-full z-50 justify-center translate-y-1/2">
        <Spinner color="white" class="my-auto" />
    </div>
{/if}

<div
    class="container mx-auto flex flex-col justify-center p-4 h-full space-y-6"
>
    <h1 class="text-3xl">Welcome to FTA Buddy</h1>
    {#if !auth || !auth.token}
        <!-- Create Account -->
        {#if view === "create"}
            <h2 class="text-xl">Create Account</h2>
            <form
                class="flex flex-col space-y-2 mt-2 text-left"
                on:submit={createUser}
            >
                <div>
                    <Label for="username">Username</Label>
                    <Input
                        id="username"
                        bind:value={username}
                        placeholder="John"
                        bind:disabled={loading}
                    />
                </div>

                <div>
                    <Label for="email">Email</Label>
                    <Input
                        id="email"
                        bind:value={email}
                        placeholder="me@example.com"
                        bind:disabled={loading}
                        type="email"
                    />
                </div>

                <div>
                    <Label for="password">Password</Label>
                    <Input
                        id="password"
                        bind:value={password}
                        type="password"
                        bind:disabled={loading}
                    />
                </div>

                <div>
                    <Label for="verify-password">Verify Password</Label>
                    <Input
                        id="verify-password"
                        bind:value={verifyPassword}
                        type="password"
                        bind:disabled={loading}
                    />
                </div>

                <div>
                    <Label for="role">Role</Label>
                    <Select
                        id="role"
                        bind:value={role}
                        items={["FTA", "FTAA", "CSA", "RI"].map((v) => ({
                            name: v,
                            value: v,
                        }))}
                        bind:disabled={loading}
                    />
                </div>

                <Button type="submit" bind:disabled={loading}
                    >Create Account</Button
                >
            </form>
            <Button
                on:click={() => (view = "login")}
                bind:disabled={loading}
                outline>Login</Button
            >

            <!-- Login -->
        {:else if view === "login"}
            <h2 class="text-xl">Login</h2>
            <form
                class="flex flex-col space-y-2 mt-2 text-left"
                on:submit={login}
            >
                <div>
                    <Label for="email">Email</Label>
                    <Input
                        id="email"
                        bind:value={email}
                        placeholder="me@example.com"
                        bind:disabled={loading}
                        type="email"
                    />
                </div>

                <div>
                    <Label for="password">Password</Label>
                    <Input
                        id="password"
                        bind:value={password}
                        type="password"
                        bind:disabled={loading}
                    />
                </div>
                <Button type="submit" bind:disabled={loading}>Login</Button>
            </form>
            <Button
                on:click={() => (view = "create")}
                bind:disabled={loading}
                outline>Create Account</Button
            >

            <!-- Login Prompt -->
        {:else}
            <h2 class="text-xl">Login or Create Account</h2>
            <Button on:click={() => (view = "create")} bind:disabled={loading}
                >Create Account</Button
            >
            <Button on:click={() => (view = "login")} bind:disabled={loading}
                >Login</Button
            >
        {/if}

        <!-- Logged In -->
    {:else}
        <h2 class="text-lg">Logged in as {auth.user?.username}</h2>
        <Button on:click={logout}>Logout</Button>

        <!-- Event selector for admins -->
        {#if auth.user?.role === "ADMIN"}
            <div
                class="flex flex-col border-t border-neutral-500 pt-10 space-y-4"
            >
                <Select
                    bind:value={event.code}
                    items={eventList}
                    placeholder="Select Event"
                    on:change={adminSelectEvent}
                />
                <form class="flex flex-col space-y-2 text-left">
                    <div class="col-span-2">
                        <Label for="event-code">Event Code</Label>
                        <Input
                            id="event-code"
                            bind:value={eventCode}
                            placeholder="2024miket"
                        />
                    </div>
                    <div class="col-span-2">
                        <Label for="event-pin">Event Pin</Label>
                        <Input
                            id="event-pin"
                            bind:value={eventPin}
                            placeholder="1234"
                        />
                    </div>
                    <Button on:click={createEvent}>Create Event</Button>
                </form>
                <div class="pt-10 border-t border-neutral-500">
                    <Button href="/" on:click={() => navigate("/")}
                        >Go to App</Button
                    >
                </div>
            </div>

            <!-- Currently have an event selected -->
        {:else if auth.eventToken}
            <div
                class="flex flex-col border-t border-neutral-500 pt-10 space-y-2"
            >
                <h3 class="text-lg">Event: {event.code}</h3>
                <Button href="/" on:click={() => navigate("/")}
                    >Go to App</Button
                >
                <Button
                    outline
                    on:click={() => (
                        eventStore.set({ code: "", pin: "", teams: [] }),
                        authStore.set({ ...auth, eventToken: "" })
                    )}>Leave Event</Button
                >
            </div>

            <!-- No event selected -->
        {:else}
            <div class="flex flex-col border-t border-neutral-500 pt-10">
                <h3 class="text-lg">Create/Join Event</h3>
                <form
                    class="grid md:grid-cols-2 gap-2 text-left"
                    on:submit={joinEvent}
                >
                    <div class="col-span-2">
                        <Label for="event-code">Event Code</Label>
                        <Input
                            id="event-code"
                            bind:value={eventCode}
                            placeholder="2024miket"
                        />
                    </div>
                    <div class="col-span-2">
                        <Label for="event-pin">Event Pin</Label>
                        <Input
                            id="event-pin"
                            bind:value={eventPin}
                            placeholder="1234"
                        />
                    </div>
                    <Button on:click={createEvent} outline>Create Event</Button>
                    <Button type="submit">Join Event</Button>
                </form>
            </div>
        {/if}
    {/if}
</div>
