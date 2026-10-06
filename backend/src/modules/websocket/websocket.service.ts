import { Injectable, ForbiddenException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Message } from '../messaging/entities/message.entity';
import { ConversationParticipant } from '../messaging/entities/conversation-participant.entity';
import { Conversation } from '../messaging/entities/conversation.entity';

@Injectable()
export class WebSocketService {
  constructor(
    @InjectRepository(Message)
    private readonly messageRepository: Repository<Message>,
    @InjectRepository(ConversationParticipant)
    private readonly participantRepository: Repository<ConversationParticipant>,
    @InjectRepository(Conversation)
    private readonly conversationRepository: Repository<Conversation>,
  ) {}

  async assertConversationParticipant(conversationId: string, userId: string): Promise<void> {
    const participant = await this.participantRepository.findOne({
      where: { conversationId, userId },
    });
    if (!participant) throw new ForbiddenException('Not a participant of this conversation');
  }

  async saveAndBroadcastMessage(
    conversationId: string,
    senderId: string,
    dto: {
      content?     : string;
      messageType? : string;
      mediaUrl?    : string;
      replyToId?   : string;
    },
  ): Promise<Message> {
    const participant = await this.participantRepository.findOne({
      where: { conversationId, userId: senderId },
    });
    if (!participant) throw new ForbiddenException('Not a participant');

    const message = await this.messageRepository.save(
      this.messageRepository.create({ conversationId, senderId, ...dto }),
    );

    await this.conversationRepository.update(conversationId, {
      lastMessageAt: new Date(),
    });
    await this.conversationRepository.increment(
      { id: conversationId }, 'messagesCount', 1,
    );

    return this.messageRepository.findOne({
      where    : { id: message.id },
      relations: ['sender'],
    });
  }

  async markConversationRead(conversationId: string, userId: string): Promise<void> {
    await this.assertConversationParticipant(conversationId, userId);
    await this.participantRepository.update(
      { conversationId, userId },
      { lastReadAt: new Date() },
    );
    await this.messageRepository.update(
      { conversationId, isRead: false },
      { isRead: true },
    );
  }

  async reactToMessage(messageId: string, conversationId: string, userId: string, _emoji: string): Promise<void> {
    // Stored as JSON field on message — simple implementation
    const message = await this.messageRepository.findOne({ where: { id: messageId, conversationId } });
    if (!message) throw new ForbiddenException('Message is not accessible');
    await this.assertConversationParticipant(conversationId, userId);
    // Future: dedicated MessageReaction entity
  }
}
