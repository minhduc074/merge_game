import { Injectable } from "@nestjs/common";
import type { Player } from "../../generated/prisma/client";
import { PrismaService } from "../../prisma/prisma.service";
import type { AnalyticsBatchDto } from "./dto";

@Injectable()
export class AnalyticsService {
  constructor(private readonly prisma: PrismaService) {}

  async ingest(player: Player, dto: AnalyticsBatchDto) {
    const now = new Date();
    await this.prisma.analyticsEvent.createMany({
      data: dto.events.map((event) => ({
        playerId: player.id,
        sessionId: event.sessionId,
        eventName: event.eventName,
        payload: event.payload as never,
        appVersion: event.appVersion,
        configVersion: event.configVersion,
        clientAt: event.clientAt == null ? null : BigInt(event.clientAt),
        receivedAt: now,
      })),
    });
    return { accepted: dto.events.length };
  }
}
