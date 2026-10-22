import { ForbiddenException } from '@nestjs/common';
import { CampusConnectGateway } from './websocket.gateway';

describe('CampusConnectGateway', () => {
  it('does not join a conversation room for a non-participant', async () => {
    const wsService = {
      assertConversationParticipant: jest.fn().mockRejectedValue(
        new ForbiddenException('Not a participant of this conversation'),
      ),
    };
    const gateway = new CampusConnectGateway(null, null, wsService as any, null);
    const socket = { userId: 'user-1', join: jest.fn() };

    await expect(
      gateway.handleJoinConversation(socket as any, { conversationId: 'conversation-1' }),
    ).rejects.toThrow(ForbiddenException);
    expect(wsService.assertConversationParticipant).toHaveBeenCalledWith('conversation-1', 'user-1');
    expect(socket.join).not.toHaveBeenCalled();
  });
});