import { Module } from "@nestjs/common";
import { AdminController } from "./admin.controller";
import { AdminPageController } from "./admin-page.controller";
import { AdminService } from "./admin.service";

@Module({
  controllers: [AdminController, AdminPageController],
  providers: [AdminService],
})
export class AdminModule {}
