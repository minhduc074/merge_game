import { Injectable, OnModuleDestroy, OnModuleInit } from "@nestjs/common";
import { PrismaPg } from "@prisma/adapter-pg";
import { attachDatabasePool } from "@vercel/functions";
import { Pool } from "pg";
import { PrismaClient } from "../generated/prisma/client";

@Injectable()
export class PrismaService extends PrismaClient implements OnModuleInit, OnModuleDestroy {
  constructor() {
    const connectionString =
      process.env.DATABASE_URL_POOLED ??
      process.env.POSTGRES_PRISMA_URL ??
      process.env.POSTGRES_URL ??
      process.env.DATABASE_URL ??
      process.env.PRISMA_DATABASE_URL ??
      process.env.POSTGRES_URL_NON_POOLING;
    if (!connectionString) {
      throw new Error(
        "DATABASE_URL_POOLED (or POSTGRES_PRISMA_URL / POSTGRES_URL / DATABASE_URL / PRISMA_DATABASE_URL) is required",
      );
    }
    const pool = new Pool({
      connectionString,
      max: 5,
      idleTimeoutMillis: 5_000,
      connectionTimeoutMillis: 5_000,
    });
    pool.on("error", (error) => {
      console.warn("Idle Postgres client error", error.message);
    });
    attachDatabasePool(pool);
    super({ adapter: new PrismaPg(pool) });
  }

  async onModuleInit(): Promise<void> {
    await this.$connect();
  }

  async onModuleDestroy(): Promise<void> {
    await this.$disconnect();
  }
}
