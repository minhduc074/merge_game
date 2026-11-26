import { Module } from "@nestjs/common";
import { CloudSaveController } from "./cloud-save.controller";
import { CloudSaveService } from "./cloud-save.service";

@Module({
  controllers: [CloudSaveController],
  providers: [CloudSaveService],
})
export class CloudSaveModule {}
