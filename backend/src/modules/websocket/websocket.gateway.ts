import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  OnGatewayConnection,
  OnGatewayDisconnect,
  OnGatewayInit,
  ConnectedSocket,
  MessageBody,
  WsException,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { Logger, Injectable } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { WebSocketService } from './websocket.service';
import { User } from '../users/entities/user.entity';

interface AuthenticatedSocket extends Socket {
  userId: string;
  user  : User;
}

@Injectable()
@WebSocketGateway({
  cors       : { origin: '*', credentials: true },
  namespace  : '/ws',
  transports : ['websocket', 'polling'],
})
export class CampusConnectGateway
  implements OnGatewayInit, OnGatewayConnection, OnGatewayDisconnect
{
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(CampusConnectGateway.name);

  // Track online users: userId → Set<socketId>
  private readonly onlineUsers = new Map<string, Set<string>>();

  constructor(
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
    private readonly wsService: WebSocketService,
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
  ) {}

  afterInit(server: Server) {
    this.logger.log('WebSocket gateway initialised');

    // JWT auth middleware — validates token before connection is accepted
    server.use(async (socket: AuthenticatedSocket, next) => {
      try {
        const token =
          socket.handshake.auth?.token ||
          socket.handshake.headers?.authorization?.replace('Bearer ', '');

        if (!token) throw new WsException('No auth token provided');

        const payload = this.jwtService.verify(token, {
          secret: this.configService.get<string>('jwt.accessSecret'),
        });

        const user = await this.userRepository.findOne({
          where: { id: payload.sub, isActive: true },
        });
        if (!user) throw new WsException('User not found');

        socket.userId = user.id;
        socket.user   = user;
        next();
      } catch {
        next(new Error('Unauthorised'));
      }
    });
  }

  async handleConnection(socket: AuthenticatedSocket) {
    const userId = socket.userId;
    if (!userId) { socket.disconnect(); return; }

    // Register online presence
    if (!this.onlineUsers.has(userId)) {
      this.onlineUsers.set(userId, new Set());
    }
    this.onlineUsers.get(userId).add(socket.id);

    // Auto-join personal notification room
    socket.join(`user:${userId}`);

    // Update last seen
    await this.userRepository.update(userId, { lastSeenAt: new Date() });

    // Broadcast online status to followers
    this.broadcastOnlineStatus(userId, true);

    this.logger.debug(`Client connected: ${userId} [${socket.id}]`);
  }

  async handleDisconnect(socket: AuthenticatedSocket) {
    const userId = socket.userId;
    if (!userId) return;

    const sockets = this.onlineUsers.get(userId);
    if (sockets) {
      sockets.delete(socket.id);
      if (sockets.size === 0) {
        this.onlineUsers.delete(userId);
        // Broadcast offline status only when all sockets disconnect
        this.broadcastOnlineStatus(userId, false);
        await this.userRepository.update(userId, { lastSeenAt: new Date() });
      }
    }

    this.logger.debug(`Client disconnected: ${userId} [${socket.id}]`);
  }

  // ── Conversation rooms ─────────────────────────────────────────────────

  @SubscribeMessage('join:conversation')
  async handleJoinConversation(
    @ConnectedSocket() socket: AuthenticatedSocket,
    @MessageBody() data: { conversationId: string },
  ) {
    await this.wsService.assertConversationParticipant(data.conversationId, socket.userId);
    socket.join(`conv:${data.conversationId}`);
    return { event: 'joined', data: { conversationId: data.conversationId } };
  }

  @SubscribeMessage('leave:conversation')
  handleLeaveConversation(
    @ConnectedSocket() socket: AuthenticatedSocket,
    @MessageBody() data: { conversationId: string },
  ) {
    socket.leave(`conv:${data.conversationId}`);
  }

  // ── Messaging ──────────────────────────────────────────────────────────

  @SubscribeMessage('message:send')
  async handleSendMessage(
    @ConnectedSocket() socket: AuthenticatedSocket,
    @MessageBody() data: {
      conversationId: string;
      content?       : string;
      messageType?   : string;
      mediaUrl?      : string;
      replyToId?     : string;
    },
  ) {
    try {
      const message = await this.wsService.saveAndBroadcastMessage(
        data.conversationId,
        socket.userId,
        data,
      );
      // Emit to everyone in the conversation room
      this.server.to(`conv:${data.conversationId}`).emit('message:new', message);
      return { success: true, messageId: message.id };
    } catch (error) {
      throw new WsException((error as Error).message);
    }
  }

  // ── Typing indicators ──────────────────────────────────────────────────

  @SubscribeMessage('typing:start')
  async handleTypingStart(
    @ConnectedSocket() socket: AuthenticatedSocket,
    @MessageBody() data: { conversationId: string },
  ) {
    await this.wsService.assertConversationParticipant(data.conversationId, socket.userId);
    socket.to(`conv:${data.conversationId}`).emit('typing:started', {
      userId         : socket.userId,
      username       : socket.user.username,
      conversationId : data.conversationId,
    });
  }

  @SubscribeMessage('typing:stop')
  async handleTypingStop(
    @ConnectedSocket() socket: AuthenticatedSocket,
    @MessageBody() data: { conversationId: string },
  ) {
    await this.wsService.assertConversationParticipant(data.conversationId, socket.userId);
    socket.to(`conv:${data.conversationId}`).emit('typing:stopped', {
      userId         : socket.userId,
      conversationId : data.conversationId,
    });
  }

  // ── Read receipts ──────────────────────────────────────────────────────

  @SubscribeMessage('message:read')
  async handleMessageRead(
    @ConnectedSocket() socket: AuthenticatedSocket,
    @MessageBody() data: { conversationId: string },
  ) {
    await this.wsService.markConversationRead(data.conversationId, socket.userId);
    socket.to(`conv:${data.conversationId}`).emit('message:read', {
      userId         : socket.userId,
      conversationId : data.conversationId,
      readAt         : new Date().toISOString(),
    });
  }

  // ── Message reactions ──────────────────────────────────────────────────

  @SubscribeMessage('message:react')
  async handleMessageReact(
    @ConnectedSocket() socket: AuthenticatedSocket,
    @MessageBody() data: { messageId: string; conversationId: string; emoji: string },
  ) {
    await this.wsService.assertConversationParticipant(data.conversationId, socket.userId);
    await this.wsService.reactToMessage(data.messageId, data.conversationId, socket.userId, data.emoji);
    this.server.to(`conv:${data.conversationId}`).emit('message:reaction', {
      messageId     : data.messageId,
      userId        : socket.userId,
      emoji         : data.emoji,
    });
  }

  // ── Public API (called from other services) ────────────────────────────

  /** Push a real-time notification to a specific user. */
  emitToUser(userId: string, event: string, data: any) {
    this.server.to(`user:${userId}`).emit(event, data);
  }

  /** Push to everyone in a conversation. */
  emitToConversation(conversationId: string, event: string, data: any) {
    this.server.to(`conv:${conversationId}`).emit(event, data);
  }

  isUserOnline(userId: string): boolean {
    return (this.onlineUsers.get(userId)?.size ?? 0) > 0;
  }

  getOnlineUserIds(): string[] {
    return Array.from(this.onlineUsers.keys());
  }

  private broadcastOnlineStatus(userId: string, isOnline: boolean) {
    this.server.emit('user:status', { userId, isOnline, timestamp: new Date().toISOString() });
  }
}
