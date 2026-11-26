import { Type } from "class-transformer";
import { IsBoolean, IsInt, IsObject, IsOptional, IsString, Length, Max, Min } from "class-validator";

export class SubmitScoreDto {
  @Type(() => Number)
  @IsInt()
  @Min(0)
  @Max(100_000_000)
  score!: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  configVersion?: number;

  @IsOptional()
  @IsString()
  @Length(1, 40)
  appVersion?: string;

  @IsOptional()
  @IsBoolean()
  debug?: boolean;

  @IsOptional()
  @IsObject()
  clientMeta?: Record<string, unknown>;
}
