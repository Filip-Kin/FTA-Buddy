<script lang="ts">
    import { type MonitorFrame, type Station } from "./../../../shared/types";
    import { Table, TableBody, TableHead, TableHeadCell } from "flowbite-svelte";
    import MonitorRow from "../components/MonitorRow.svelte";
    import TeamModal from "../components/TeamModal.svelte";

    export let monitorFrame: MonitorFrame;

    const FieldStates = {
        0: "Unknown",
        1: "Match Running Teleop",
        2: "Match Transistioning",
        3: "Match Running Auto",
        4: "Match Ready",
        5: "Prestart Completed",
        6: "Prestart Initiated",
        7: "Ready to Prestart",
        8: "Match Aborted",
        9: "Match Over",
        10: "Ready for Post Result",
        11: "Match Not Ready",
    };

    let modalOpen = false;
    let modalStation: Station = "blue1";

    function detailView(evt: Event) {
        let target = evt.target as HTMLElement;
        let station = target.parentElement?.id.replace("-row", "");
        modalStation = (station as Station) || "blue1";
        modalOpen = true;
    }

    const stations: Station[] = ["blue1", "blue2", "blue3", "red1", "red2", "red3"];
</script>

{#if monitorFrame}
    <TeamModal bind:modalOpen bind:modalStation bind:monitorFrame />
{/if}

<div class="w-full mx-auto container md:max-w-xl md:p-2">
    {#key monitorFrame}
        <div class="flex w-full mb-1">
            {#if monitorFrame}
                <div class="w-24">M: {monitorFrame.match}</div>
                <div class="grow">{FieldStates[monitorFrame.field]}</div>
                <div class="w-32">{monitorFrame.time}</div>
            {/if}
        </div>
        <Table class="w-full mx-auto">
            <TableHead class="dark:bg-neutral-500 dark:text-white">
                <TableHeadCell class="w-20">Team</TableHeadCell>
                <TableHeadCell class="w-20">DS</TableHeadCell>
                <TableHeadCell class="w-10">Radio</TableHeadCell>
                <TableHeadCell class="w-10">Rio</TableHeadCell>
                <TableHeadCell>Bat</TableHeadCell>
                <TableHeadCell>Net</TableHeadCell>
            </TableHead>
            <TableBody>
                {#if monitorFrame}
                    {#each stations as station}
                        <MonitorRow {station} {monitorFrame} {detailView} />
                    {/each}
                {/if}
            </TableBody>
        </Table>
    {/key}
    {#if !monitorFrame}
        <p>Requires Chrome Extension to be setup on field network</p>
    {/if}
</div>