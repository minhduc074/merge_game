import { Body, Controller, Get, Param, Post, Put, Req, UseGuards } from "@nestjs/common";
import type { Request } from "express";
import { AdminAuthGuard, type AdminRequest } from "../../common/admin-auth.guard";
import { ConfigPayloadDto, CreateConfigDto, ValidateConfigDto } from "./dto";
import { GameConfigService } from "./game-config.service";

function actorOf(req: Request): string {
  return (req as AdminRequest).adminUser ?? "unknown";
}

@Controller("api/admin/configs")
@UseGuards(AdminAuthGuard)
export class AdminConfigController {
  constructor(private readonly configs: GameConfigService) {}

  @Get()
  list() {
    return this.configs.list();
  }

  @Get(":id")
  get(@Param("id") id: string) {
    return this.configs.getPayload(id);
  }

  @Post()
  create(@Req() req: Request, @Body() dto: CreateConfigDto) {
    return this.configs.createDraft(actorOf(req), dto.payload, dto.note, dto.basedOnVersion);
  }

  @Post("copy-default")
  createDefaultCopy(@Req() req: Request) {
    return this.configs.createDraft(actorOf(req), undefined, "Copy active config");
  }

  @Put(":id")
  update(@Req() req: Request, @Param("id") id: string, @Body() dto: ConfigPayloadDto) {
    return this.configs.updateDraft(actorOf(req), id, dto.payload, dto.note);
  }

  @Post("validate")
  validate(@Body() dto: ValidateConfigDto) {
    return this.configs.validatePayload(dto.payload);
  }

  @Post(":id/validate")
  validateId(@Param("id") id: string) {
    return this.configs.validateId(id);
  }

  @Post(":id/publish")
  publish(@Req() req: Request, @Param("id") id: string) {
    return this.configs.publish(actorOf(req), id);
  }

  @Post(":id/rollback")
  rollback(@Req() req: Request, @Param("id") id: string, @Body() body: { note?: string }) {
    return this.configs.rollback(actorOf(req), id, body?.note);
  }
}
