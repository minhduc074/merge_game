import { BadRequestException, ConflictException, Injectable, NotFoundException } from "@nestjs/common";
import type { Player } from "../../generated/prisma/client";
import { sha256Hex } from "../../common/crypto";
import { PrismaService } from "../../prisma/prisma.service";
import type { PutCloudSaveDto } from "./dto";

function metadata(row: {
  revision: number;
  checksum: string;
  clientUpdatedAt: bigint;
  updatedAt: Date;
}) {
  return {
    revision: row.revision,
    checksum: row.checksum,
    clientUpdatedAt: Number(row.clientUpdatedAt),
    updatedAt: row.updatedAt,
  };
}

@Injectable()
export class CloudSaveService {
  constructor(private readonly prisma: PrismaService) {}

  async getMine(player: Player) {
    const row = await this.prisma.cloudSave.findUnique({ where: { playerId: player.id } });
    if (!row) throw new NotFoundException("cloud save not found");
    return {
      ...metadata(row),
      saveJson: row.saveJson,
    };
  }

  async putMine(player: Player, dto: PutCloudSaveDto) {
    const checksum = sha256Hex(dto.saveJson);
    if (checksum !== dto.checksum.toLowerCase()) {
      throw new BadRequestException("checksum does not match saveJson");
    }
    let parsed: unknown;
    try {
      parsed = JSON.parse(dto.saveJson);
    } catch {
      throw new BadRequestException("saveJson must be valid JSON");
    }
    if (typeof parsed !== "object" || parsed == null || Array.isArray(parsed)) {
      throw new BadRequestException("saveJson must contain a JSON object");
    }

    const existing = await this.prisma.cloudSave.findUnique({ where: { playerId: player.id } });
    const expectedBase = existing?.revision ?? 0;
    if (dto.baseRevision !== expectedBase) {
      throw new ConflictException({
        message: "cloud save revision conflict",
        cloud: existing ? metadata(existing) : null,
        expectedBaseRevision: expectedBase,
      });
    }

    const row = existing
      ? await this.prisma.cloudSave.update({
          where: { playerId: player.id },
          data: {
            revision: existing.revision + 1,
            saveJson: parsed as never,
            checksum,
            clientUpdatedAt: BigInt(dto.clientUpdatedAt),
          },
        })
      : await this.prisma.cloudSave.create({
          data: {
            playerId: player.id,
            revision: 1,
            saveJson: parsed as never,
            checksum,
            clientUpdatedAt: BigInt(dto.clientUpdatedAt),
          },
        });

    return { ok: true, ...metadata(row) };
  }
}
