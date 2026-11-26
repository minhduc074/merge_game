import { BadRequestException, Injectable, NotFoundException } from "@nestjs/common";
import { ConfigStatus } from "../../generated/prisma/enums";
import { PrismaService } from "../../prisma/prisma.service";
import { DEFAULT_GAME_CONFIG, validateGameConfigPayload } from "./config-schema";

@Injectable()
export class GameConfigService {
  constructor(private readonly prisma: PrismaService) {}

  async current() {
    const row = await this.prisma.gameConfig.findFirst({
      where: { status: ConfigStatus.PUBLISHED },
      orderBy: { version: "desc" },
    });
    if (!row) {
      return {
        version: 0,
        status: "bundled-default",
        payload: DEFAULT_GAME_CONFIG,
        publishedAt: null,
      };
    }
    return {
      version: row.version,
      status: row.status.toLowerCase(),
      payload: row.payload,
      publishedAt: row.publishedAt,
    };
  }

  list() {
    return this.prisma.gameConfig.findMany({
      orderBy: { version: "desc" },
      take: 100,
      select: {
        id: true,
        version: true,
        status: true,
        note: true,
        createdBy: true,
        createdAt: true,
        publishedAt: true,
      },
    });
  }

  async getPayload(id: string) {
    const row = await this.prisma.gameConfig.findUnique({ where: { id } });
    if (!row) throw new NotFoundException("config not found");
    return row;
  }

  async createDraft(actor: string, payload: Record<string, unknown> | undefined, note?: string, basedOnVersion?: number) {
    let basePayload = payload;
    if (!basePayload && basedOnVersion !== undefined) {
      const base = await this.prisma.gameConfig.findUnique({ where: { version: basedOnVersion } });
      if (!base) throw new NotFoundException("basedOnVersion not found");
      basePayload = base.payload as Record<string, unknown>;
    }
    if (!basePayload) {
      basePayload = (await this.current()).payload as Record<string, unknown>;
    }
    const max = await this.prisma.gameConfig.aggregate({ _max: { version: true } });
    const version = (max._max.version ?? 0) + 1;
    const row = await this.prisma.gameConfig.create({
      data: {
        version,
        status: ConfigStatus.DRAFT,
        payload: basePayload as never,
        note,
        createdBy: actor,
      },
    });
    await this.audit(actor, "config.createDraft", "gameConfig", row.id, { version });
    return row;
  }

  async updateDraft(actor: string, id: string, payload: Record<string, unknown>, note?: string) {
    const row = await this.getPayload(id);
    if (row.status !== ConfigStatus.DRAFT) {
      throw new BadRequestException("only draft configs can be edited");
    }
    const updated = await this.prisma.gameConfig.update({
      where: { id },
      data: { payload: payload as never, note },
    });
    await this.audit(actor, "config.updateDraft", "gameConfig", id, { version: row.version });
    return updated;
  }

  validatePayload(payload: unknown) {
    const errors = validateGameConfigPayload(payload);
    return { valid: errors.length === 0, errors };
  }

  async validateId(id: string) {
    const row = await this.getPayload(id);
    return this.validatePayload(row.payload);
  }

  async publish(actor: string, id: string) {
    const row = await this.getPayload(id);
    const validation = this.validatePayload(row.payload);
    if (!validation.valid) {
      throw new BadRequestException({ message: "config validation failed", errors: validation.errors });
    }
    const updated = await this.prisma.$transaction(async (tx) => {
      await tx.gameConfig.updateMany({
        where: { status: ConfigStatus.PUBLISHED },
        data: { status: ConfigStatus.ARCHIVED },
      });
      return tx.gameConfig.update({
        where: { id },
        data: { status: ConfigStatus.PUBLISHED, publishedAt: new Date() },
      });
    });
    await this.audit(actor, "config.publish", "gameConfig", id, { version: row.version });
    return updated;
  }

  async rollback(actor: string, id: string, note?: string) {
    const source = await this.getPayload(id);
    const validation = this.validatePayload(source.payload);
    if (!validation.valid) {
      throw new BadRequestException({ message: "source config is invalid under current schema", errors: validation.errors });
    }
    const max = await this.prisma.gameConfig.aggregate({ _max: { version: true } });
    const version = (max._max.version ?? 0) + 1;
    const updated = await this.prisma.$transaction(async (tx) => {
      await tx.gameConfig.updateMany({
        where: { status: ConfigStatus.PUBLISHED },
        data: { status: ConfigStatus.ARCHIVED },
      });
      return tx.gameConfig.create({
        data: {
          version,
          status: ConfigStatus.PUBLISHED,
          payload: source.payload as never,
          note: note ?? `Rollback to v${source.version}`,
          createdBy: actor,
          publishedAt: new Date(),
        },
      });
    });
    await this.audit(actor, "config.rollback", "gameConfig", updated.id, {
      sourceVersion: source.version,
      newVersion: version,
    });
    return updated;
  }

  private async audit(actor: string, action: string, targetType: string, targetId: string, details: Record<string, unknown>) {
    await this.prisma.adminAuditLog.create({
      data: { actor, action, targetType, targetId, details: details as never },
    });
  }
}
