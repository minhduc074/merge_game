import { Transform } from "class-transformer";
import { IsOptional, IsString, Length, Matches } from "class-validator";

const cleanText = ({ value }: { value: unknown }): unknown =>
  typeof value === "string" ? value.replace(/[\u0000-\u001f\u007f]/g, "").trim() : value;

export class BootstrapPlayerDto {
  @IsString()
  @Matches(/^[A-Za-z0-9_-]{8,80}$/)
  installId!: string;

  @IsOptional()
  @Transform(cleanText)
  @IsString()
  @Length(2, 24)
  nickname?: string;
}

export class UpdateNicknameDto {
  @Transform(cleanText)
  @IsString()
  @Length(2, 24)
  nickname!: string;
}
