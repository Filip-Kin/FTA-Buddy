import { z } from "zod";
import { publicProcedure, router } from "../trpc";
import { getEvent } from "../util/get-event";
import { observable } from "@trpc/server/observable";
import { CycleData, FMSEnums, FMSLevelMap } from "../../shared/types";
import { db } from "../db/db";
import { cycleLogs } from "../db/schema";
import { InferSelectModel, and, eq } from "drizzle-orm";
import { randomUUID } from "crypto";

export const cycleRouter = router({
    postCycleTime: publicProcedure.input(z.object({
        eventToken: z.string(),
        type: z.enum(['lastCycleTime', 'prestart', 'start', 'end', 'refsDone', 'scoresPosted']),
        lastCycleTime: z.string().optional(),
        matchNumber: z.number(),
        playNumber: z.number(),
        level: z.enum(['None', 'Practice', 'Qualification', 'Playoff'])
    })).mutation(async ({ input }) => {
        const event = await getEvent(input.eventToken);

        if (input.type === 'lastCycleTime' && input.lastCycleTime) {
            let cycle: any = await db.query.cycleLogs.findFirst({ where: and(eq(cycleLogs.event, event.code), eq(cycleLogs.match_number, input.matchNumber - 1), eq(cycleLogs.play_number, 1), eq(cycleLogs.level, input.level)) });

            event.monitorFrame.lastCycleTime = input.lastCycleTime;

            void db.update(cycleLogs).set({ calculated_cycle_time: input.lastCycleTime }).where(eq(cycleLogs.id, cycle.id)).execute();

        } else {
            let cycle: any = await db.query.cycleLogs.findFirst({ where: and(eq(cycleLogs.event, event.code), eq(cycleLogs.match_number, input.matchNumber), eq(cycleLogs.play_number, input.playNumber), eq(cycleLogs.level, input.level)) });

            if (!cycle) {
                cycle = await db.insert(cycleLogs).values({
                    id: randomUUID(),
                    event: event.code,
                    match_number: input.matchNumber,
                    play_number: input.playNumber,
                    level: input.level
                }).returning();
            }


            switch (input.type) {
                case 'prestart':
                    event.lastPrestartDone = new Date();
                    void db.update(cycleLogs).set({ prestart_time: event.lastPrestartDone }).where(eq(cycleLogs.id, cycle.id)).execute();
                    break;
                case 'start':
                    event.lastMatchStart = new Date();
                    void db.update(cycleLogs).set({ start_time: event.lastMatchStart }).where(eq(cycleLogs.id, cycle.id)).execute();
                    break;
                case 'end':
                    event.lastMatchEnd = new Date();
                    void db.update(cycleLogs).set({ end_time: event.lastMatchEnd }).where(eq(cycleLogs.id, cycle.id)).execute();
                    break;
                case 'refsDone':
                    event.lastMatchRefDone = new Date();
                    void db.update(cycleLogs).set({ ref_done_time: event.lastMatchRefDone }).where(eq(cycleLogs.id, cycle.id)).execute();
                    break;
                case 'scoresPosted':
                    event.lastMatchScoresPosted = new Date();
                    void db.update(cycleLogs).set({ scores_posted_time: event.lastMatchScoresPosted }).where(eq(cycleLogs.id, cycle.id)).execute();
                    break;
            };
        }

        event.cycleEmitter.emit('update');
    }),

    subscription: publicProcedure.input(z.object({
        eventToken: z.string()
    })).subscription(async ({ input }) => {
        const event = await getEvent(input.eventToken);

        return observable<CycleData>((emitter) => {
            const listener = () => {
                emitter.next({
                    eventCode: event.code,
                    startTime: event.lastMatchStart,
                    refEndTime: event.lastMatchRefDone,
                    scoresPostedTime: event.lastMatchScoresPosted,
                    prestartTime: event.lastPrestartDone,
                    endTime: event.lastMatchEnd,
                    matchNumber: event.monitorFrame.match,
                    previousCycleTime: event.monitorFrame.lastCycleTime
                });
            };

            event.cycleEmitter.on('update', listener);

            return () => {
                event.cycleEmitter.off('update', listener);
            };
        });
    })
});