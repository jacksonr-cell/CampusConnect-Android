import { ApiProperty } from '@nestjs/swagger';
import { IsString, Length } from 'class-validator';

export class VerifyMfaSetupDto {
  @ApiProperty({ description: '6-digit TOTP code from authenticator app', example: '123456' })
  @IsString()
  @Length(6, 6)
  code: string;
}

export class VerifyMfaLoginDto {
  @ApiProperty({ description: 'Temporary MFA session token issued after password check' })
  @IsString()
  mfaToken: string;

  @ApiProperty({ description: '6-digit TOTP code, or an 8-character backup code', example: '123456' })
  @IsString()
  code: string;
}

export class DisableMfaDto {
  @ApiProperty({ description: 'Current password, required to disable MFA' })
  @IsString()
  password: string;
}
