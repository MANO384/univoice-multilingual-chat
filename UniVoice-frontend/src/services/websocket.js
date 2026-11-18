import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8082/ws-chat';
class WebSocketService {
  constructor() {
    this.client = null;
  }

  connect(onMessageReceived, onStatusChange, onReceiptReceived, onGroupMessageReceived) {
    const token = sessionStorage.getItem('univoice_access_token');
    if (!token) return;

    if (this.client && this.client.active) {
      this.client.deactivate();
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        if (onStatusChange) onStatusChange(true);

        // 1-on-1 incoming messages & translation updates
        this.client.subscribe('/user/queue/messages', (frame) => {
          if (frame.body && onMessageReceived) {
            onMessageReceived(JSON.parse(frame.body));
          }
        });

        // 1-on-1 delivery & read receipts
        this.client.subscribe('/user/queue/receipts', (frame) => {
          if (frame.body && onReceiptReceived) {
            onReceiptReceived(JSON.parse(frame.body));
          }
        });

        // Group messages & personalized per-member translations
        this.client.subscribe('/user/queue/group-messages', (frame) => {
          if (frame.body && onGroupMessageReceived) {
            onGroupMessageReceived(JSON.parse(frame.body));
          }
        });
      },
      onDisconnect: () => {
        if (onStatusChange) onStatusChange(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
        if (onStatusChange) onStatusChange(false);
      }
    });

    this.client.activate();
  }

  // --- 1-on-1 Methods ---
  sendMessage(receiverUuid, content, clientMsgId) {
    if (!this.client || !this.client.connected) return false;
    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ receiverUuid, content, clientMsgId })
    });
    return true;
  }

  sendDelivered(messageUuid) {
    if (!this.client || !this.client.connected) return;
    this.client.publish({
      destination: '/app/chat.delivered',
      body: JSON.stringify({ messageUuid })
    });
  }

  sendRead(contactUuid) {
    if (!this.client || !this.client.connected) return;
    this.client.publish({
      destination: '/app/chat.read',
      body: JSON.stringify({ contactUuid })
    });
  }

  editMessage(messageUuid, newContent) {
    if (!this.client || !this.client.connected) return;
    this.client.publish({
      destination: '/app/chat.edit',
      body: JSON.stringify({ messageUuid, newContent })
    });
  }

  deleteMessage(messageUuid) {
    if (!this.client || !this.client.connected) return;
    this.client.publish({
      destination: '/app/chat.delete',
      body: JSON.stringify({ messageUuid })
    });
  }

  // --- Group Chat Methods ---
  sendGroupMessage(groupUuid, content, clientMsgId) {
    if (!this.client || !this.client.connected) return false;
    this.client.publish({
      destination: '/app/group.send',
      body: JSON.stringify({ groupUuid, content, clientMsgId })
    });
    return true;
  }

  editGroupMessage(groupUuid, messageUuid, newContent) {
    if (!this.client || !this.client.connected) return;
    this.client.publish({
      destination: '/app/group.edit',
      body: JSON.stringify({ groupUuid, messageUuid, newContent })
    });
  }

  deleteGroupMessage(groupUuid, messageUuid) {
    if (!this.client || !this.client.connected) return;
    this.client.publish({
      destination: '/app/group.delete',
      body: JSON.stringify({ groupUuid, messageUuid })
    });
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }
}

export const wsService = new WebSocketService();