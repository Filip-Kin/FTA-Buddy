import 'dotenv/config';
import { TRPCError } from "@trpc/server";
import { eq } from "drizzle-orm";
import { z } from "zod";
import { db } from "../db/db";
import { events } from "../db/schema";
import { adminProcedure, eventProcedure, protectedProcedure, publicProcedure, router } from "../trpc";
import { generateToken } from "./user";

export const eventRouter = router({
    join: publicProcedure.input(z.object({
        code: z.string(),
        pin: z.string()
    })).query(async ({ input }) => {
        const event = await db.query.events.findFirst({ where: eq(events.code, input.code.trim().toLowerCase()) });

        if (!event) throw new TRPCError({ code: 'NOT_FOUND', message: 'Event not found' });

        if (event.pin !== input.pin) throw new TRPCError({ code: 'UNAUTHORIZED', message: 'Incorrect pin' });

        return event;
    }),

    get: adminProcedure.input(z.object({
        code: z.string()
    })).query(async ({ input }) => {
        const event = await db.query.events.findFirst({ where: eq(events.code, input.code.trim().toLowerCase()) });

        if (!event) throw new TRPCError({ code: 'NOT_FOUND', message: 'Event not found' });

        return event;
    }),

    create: protectedProcedure.input(z.object({
        code: z.string().startsWith('202').min(6),
        pin: z.string().min(4)
    })).query(async ({ input }) => {
        const token = generateToken();
        const teams: ({number: string, name: string})[] = [];

        if (await db.query.events.findFirst({ where: eq(events.code, input.code) })) {
            throw new TRPCError({ code: 'CONFLICT', message: 'Event already exists' });
        }

        const teamsData = await fetch(`https://www.thebluealliance.com/api/v3/event/${input.code}/teams/simple`, {
            headers: {
                'X-TBA-Auth-Key': process.env.TBA_API_KEY ?? ''
            }
        }).then(res => res.json());

        if (teamsData) {
            for (let team of teamsData) {
                teams.push({number: team.team_number, name: team.nickname});
            }
        }

        await db.insert(events).values({
            code: input.code,
            pin: input.pin,
            token,
            teams
        });

        return { code: input.code, token, teams };
    }),

    getAll: adminProcedure.query(async () => {
        return (await db.query.events.findMany()).sort((a, b) => b.created_at.getTime() - a.created_at.getTime());
    })
});
