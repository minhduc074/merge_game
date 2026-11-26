import { Controller, Get, Header } from "@nestjs/common";
import { ADMIN_PAGE } from "./admin-page";

@Controller()
export class AdminPageController {
  @Get("admin")
  @Header("Content-Type", "text/html; charset=utf-8")
  adminPage(): string {
    return ADMIN_PAGE;
  }
}
