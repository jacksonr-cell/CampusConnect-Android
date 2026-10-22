import {
  Injectable, ForbiddenException, BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import { Conversation, ConversationType } from './entities/conversation.entity';
import { ConversationParticipant } from './entities/conversation-participant.entity';
import { Message } from './entities/message.entity';
import { SendMessageDto } from './dto/send-message.dto';
import { CreateGroupDto } from './dto/create-group.dto';
import { PaginationDto, paginate, PaginatedResult } from '../../common/dto/pagination.dto';
import { CampusConnectGateway } from '../websocket/websocket.gateway';

/**
 * Conversation enriched with its most recent message — the entity itself
 * only stores `lastMessageAt` (a timestamp, for sorting) with no
 * denormalized message text, so a conversation list previously had no way
 * to show a "Hey, is the room still available?" preview line under each
 * item. Computed here rather than stored, so it's always accurate.
 */
export interface EnrichedConversation extends Conversation {
  lastMessage: Message | null;
}

@Injectable()
export class MessagingService {
  constructor(
    @InjectRepository(Conversation)
    private readonly conversationRepository: Repository<Conversation>,
    @InjectRepository(ConversationParticipant)
    private readonly participantRepository: Repository<ConversationParticipant>,
    @InjectRepository(Message)
    private readonly messageRepository: Repository<Message>,
    private readonly gateway: CampusConnectGateway,
  ) {}

  async getConversations(userId: string, pagination: PaginationDto): Promise<PaginatedResult<EnrichedConversation>> {
    const { page = 1, limit = 20 } = pagination;
    const [conversations, total] = await this.conversationRepository
      .createQueryBuilder('c')
      .innerJoin('c.participants', 'me', 'me.userId = :userId', { userId })
      .leftJoinAndSelect('c.participants', 'allP')
      .leftJoinAndSelect('allP.user', 'u')
      .orderBy('c.lastMessageAt', 'DESC')
      .skip((page - 1) * limit)
      .take(limit)
      .getManyAndCount();

    const enriched = await this.attachLastMessages(conversations);
    return paginate(enriched, total, page, limit);
  }

  async getOrCreateDirect(userId1: string, userId2: string): Promise<Conversation> {
    if (userId1 === userId2) throw new BadRequestException('Cannot message yourself');

    const existing = await this.conversationRepository
      .createQueryBuilder('c')
      .innerJoin('c.participants', 'p1', 'p1.userId = :u1', { u1: userId1 })
      .innerJoin('c.participants', 'p2', 'p2.userId = :u2', { u2: userId2 })
      .where('c.type = :type', { type: ConversationType.DIRECT })
      .getOne();

    if (existing) return existing;

    const conversation = await this.conversationRepository.save(
      this.conversationRepository.create({ type: ConversationType.DIRECT }),
    );
    await this.participantRepository.save([
      this.participantRepository.create({ conversationId: conversation.id, userId: userId1, isAdmin: true }),
      this.participantRepository.create({ conversationId: conversation.id, userId: userId2 }),
    ]);
    return conversation;
  }

  async createGroup(creatorId: string, dto: CreateGroupDto): Promise<Conversation> {
    const conversation = await this.conversationRepository.save(
      this.conversationRepository.create({
        type: ConversationType.GROUP,
        name: dto.name,
        avatarUrl: dto.avatarUrl,
      }),
    );

    const allParticipants = [creatorId, ...dto.participantIds.filter((id) => id !== creatorId)];
    await this.participantRepository.save(
      allParticipants.map((userId) =>
        this.participantRepository.create({
          conversationId: conversation.id,
          userId,
          isAdmin: userId === creatorId,
        }),
      ),
    );

    return conversation;
  }

  async getMessages(conversationId: string, userId: string, pagination: PaginationDto): Promise<PaginatedResult<Message>> {
    const participant = await this.participantRepository.findOne({ where: { conversationId, userId } });
    if (!participant) throw new ForbiddenException('Not a participant of this conversation');

    const { page = 1, limit = 50 } = pagination;
    const [messages, total] = await this.messageRepository.findAndCount({
      where: { conversationId, isDeleted: false },
      relations: ['sender'],
      order: { createdAt: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return paginate(messages, total, page, limit);
  }

  async sendMessage(conversationId: string, senderId: string, dto: SendMessageDto): Promise<Message> {
    const participant = await this.participantRepository.findOne({ where: { conversationId, userId: senderId } });
    if (!participant) throw new ForbiddenException('Not a participant of this conversation');

    if (!dto.content && !dto.mediaUrl) {
      throw new BadRequestException('Message must have content or media');
    }

    const message = await this.messageRepository.save(
      this.messageRepository.create({ conversationId, senderId, ...dto }),
    );

    await this.conversationRepository.update(conversationId, { lastMessageAt: new Date() });
    await this.conversationRepository.increment({ id: conversationId }, 'messagesCount', 1);

    const fullMessage = await this.messageRepository.findOne({
      where: { id: message.id },
      relations: ['sender'],
    });

    // Push to every OTHER connected participant in real time. This is what
    // makes REST-based sends (the path this app's Retrofit client actually
    // uses) behave identically to the gateway's own socket-triggered send —
    // previously only the latter broadcast anything, so a REST send would
    // silently sit un-delivered until the recipient manually refreshed.
    this.gateway.emitToConversation(conversationId, 'message:new', fullMessage);

    return fullMessage;
  }

  async markAsRead(conversationId: string, userId: string): Promise<void> {
    const participant = await this.participantRepository.findOne({ where: { conversationId, userId } });
    if (!participant) throw new ForbiddenException('Not a participant');
    await this.participantRepository.update({ conversationId, userId }, { lastReadAt: new Date() });
    await this.messageRepository.update(
      { conversationId, isRead: false },
      { isRead: true },
    );

    this.gateway.emitToConversation(conversationId, 'message:read', {
      userId,
      conversationId,
      readAt: new Date().toISOString(),
    });
  }

  /**
   * Two total queries regardless of list size (not N+1): fetch every
   * non-deleted message across all listed conversations in one call, then
   * pick the most recent per conversation in memory. Bounded by page size
   * (≤ ~50 conversations per page in practice), so this stays cheap.
   */
  private async attachLastMessages(conversations: Conversation[]): Promise<EnrichedConversation[]> {
    if (conversations.length === 0) return [];
    const conversationIds = conversations.map((c) => c.id);

    const recentMessages = await this.messageRepository.find({
      where: { conversationId: In(conversationIds), isDeleted: false },
      relations: ['sender'],
      order: { createdAt: 'DESC' },
    });

    const lastMessageMap = new Map<string, Message>();
    for (const message of recentMessages) {
      if (!lastMessageMap.has(message.conversationId)) {
        lastMessageMap.set(message.conversationId, message);
      }
    }

    return conversations.map((conversation) => ({
      ...conversation,
      lastMessage: lastMessageMap.get(conversation.id) ?? null,
    })) as EnrichedConversation[];
  }
}
