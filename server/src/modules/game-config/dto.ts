import { Type } from "class-transformer";
import { IsInt, IsObject, IsOptional, IsString, Length, Min } from "class-validator";

export class ConfigPayloadDto {
  @IsObject()
  payload!: Record<string, unknown>;

  @IsOptional()
  @IsString()
  @Length(0, 300)
  note?: string;
}

export class CreateConfigDto extends ConfigPayloadDto {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  basedOnVersion?: number;
}

export class ValidateConfigDto {
  @IsObject()
  payload!: Record<string, unknown>;
}
