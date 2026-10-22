import { ApiProperty } from '@nestjs/swagger';
import { IsUUID } from 'class-validator';

export class RevokeSessionDto {
  @ApiProperty({ description: 'Session ID to revoke' })
  @IsUUID()
  sessionId: string;
}
