import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { TypeOrmModule } from '@nestjs/typeorm';
import { CampusConnectGateway } from './websocket.gateway';
import { WebSocketService } from './websocket.service';
import { Message } from '../messaging/entities/message.entity';
import { ConversationParticipant } from '../messaging/entities/conversation-participant.entity';
import { Conversation } from '../messaging/entities/conversation.entity';
import { User } from '../users/entities/user.entity';

@Module({
  imports: [
    JwtModule.register({}),
    TypeOrmModule.forFeature([Message, ConversationParticipant, Conversation, User]),
  ],
  providers: [CampusConnectGateway, WebSocketService],
  exports  : [CampusConnectGateway],
})
export class WebSocketModule {}
