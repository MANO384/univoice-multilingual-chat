import React, { useState, useEffect, useRef } from 'react';
import { api } from './services/api';
import { wsService } from './services/websocket';
import AuthScreen from './components/AuthScreen';
import {
  Send, Globe, LogOut, Search, MessageSquare, RefreshCw,
  ChevronUp, UserPlus, Users, X, Check, CheckCheck, AlertCircle, Pencil, Trash2, Lock, Info
} from 'lucide-react';
import { CONTACT_LANG_OPTIONS } from './constants/languages';

const SENDER_COLORS = [
  'text-emerald-400',
  'text-sky-400',
  'text-amber-400',
  'text-purple-400',
  'text-rose-400',
  'text-teal-300'
];

function getSenderColor(uuid = '') {
  let hash = 0;
  for (let i = 0; i < uuid.length; i++) {
    hash = uuid.charCodeAt(i) + ((hash << 5) - hash);
  }
  return SENDER_COLORS[Math.abs(hash) % SENDER_COLORS.length];
}

export default function App() {
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = sessionStorage.getItem('univoice_user');
    return saved ? JSON.parse(saved) : null;
  });

  // Global Translation ON/OFF Toggle State
  const [globalTranslateOn, setGlobalTranslateOn] = useState(() => {
    const saved = sessionStorage.getItem('univoice_global_translate');
    return saved === null ? true : saved === 'true';
  });

  const toggleGlobalTranslate = () => {
    setGlobalTranslateOn((prev) => {
      const next = !prev;
      sessionStorage.setItem('univoice_global_translate', String(next));
      return next;
    });
  };

  // Profile Modal State
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [profileForm, setProfileForm] = useState({
    displayName: currentUser?.displayName || '',
    motherTongue: currentUser?.motherTongue || 'ta',
    defaultLanguage: currentUser?.preferredLanguage || currentUser?.defaultLanguage || 'ta'
  });
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileMessage, setProfileMessage] = useState('');

  // Contacts & Groups
  const [contacts, setContacts] = useState([]);
  const [groups, setGroups] = useState([]);
  // activeTarget: { type: 'DIRECT', data: contact } | { type: 'GROUP', data: group } | null
  const [activeTarget, setActiveTarget] = useState(null);

  const [contactOnline, setContactOnline] = useState(false);
  const [langPref, setLangPref] = useState('DEFAULT');

  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [editingMsg, setEditingMsg] = useState(null);
  const [isConnected, setIsConnected] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [unreadMap, setUnreadMap] = useState({});

  // Add Contact Modal
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [newContactPhone, setNewContactPhone] = useState('');
  const [newContactName, setNewContactName] = useState('');
  const [lookupError, setLookupError] = useState('');
  const [lookupLoading, setLookupLoading] = useState(false);

  // Create Group Modal
  const [isCreateGroupOpen, setIsCreateGroupOpen] = useState(false);
  const [groupName, setGroupName] = useState('');
  const [groupDesc, setGroupDesc] = useState('');
  const [selectedMemberUuids, setSelectedMemberUuids] = useState([]);
  const [groupCreating, setGroupCreating] = useState(false);
  const [groupError, setGroupError] = useState('');

  // Group Info Modal
  const [isGroupInfoOpen, setIsGroupInfoOpen] = useState(false);

  // Pagination
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);

  const messagesEndRef = useRef(null);
  const chatContainerRef = useRef(null);
  const activeTargetRef = useRef(activeTarget);
  const shouldScrollToBottomRef = useRef(true);

  useEffect(() => {
    activeTargetRef.current = activeTarget;
  }, [activeTarget]);

  useEffect(() => {
    if (shouldScrollToBottomRef.current) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  const openProfileModal = async () => {
    setProfileMessage('');
    setIsProfileOpen(true);
    try {
      const res = await api.get('/users/me');
      const me = res.data;
      setProfileForm({
        displayName: me.displayName,
        motherTongue: me.motherTongue,
        defaultLanguage: me.defaultLanguage
      });
      const updatedUser = { ...currentUser, ...me, preferredLanguage: me.defaultLanguage };
      setCurrentUser(updatedUser);
      sessionStorage.setItem('univoice_user', JSON.stringify(updatedUser));
    } catch (err) {
      setProfileForm({
        displayName: currentUser?.displayName || '',
        motherTongue: currentUser?.motherTongue || 'ta',
        defaultLanguage: currentUser?.preferredLanguage || currentUser?.defaultLanguage || 'ta'
      });
    }
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setProfileSaving(true);
    setProfileMessage('');
    try {
      const res = await api.put('/users/me', profileForm);
      const updated = res.data;
      const mergedUser = {
        ...currentUser,
        displayName: updated.displayName,
        motherTongue: updated.motherTongue,
        defaultLanguage: updated.defaultLanguage,
        preferredLanguage: updated.defaultLanguage
      };
      setCurrentUser(mergedUser);
      sessionStorage.setItem('univoice_user', JSON.stringify(mergedUser));
      setProfileMessage('Profile updated successfully!');
    } catch (err) {
      setProfileMessage(err.response?.data?.message || 'Failed to update profile');
    } finally {
      setProfileSaving(false);
    }
  };

  const moveContactToTop = (contactUuid) => {
    setContacts((prev) => {
      const idx = prev.findIndex((c) => c.userUuid === contactUuid);
      if (idx <= 0) return prev;
      const updated = [...prev];
      const [moved] = updated.splice(idx, 1);
      return [moved, ...updated];
    });
  };

  const moveGroupToTop = (groupUuid) => {
    setGroups((prev) => {
      const idx = prev.findIndex((g) => g.groupUuid === groupUuid);
      if (idx <= 0) return prev;
      const updated = [...prev];
      const [moved] = updated.splice(idx, 1);
      return [moved, ...updated];
    });
  };

  const fetchContacts = async () => {
    try {
      const res = await api.get('/users/contacts');
      const fetched = res.data || [];
      setContacts((prev) => {
        const existingUuids = new Set(prev.map((c) => c.userUuid));
        const newUsers = fetched.filter((c) => !existingUuids.has(c.userUuid));
        const refreshedExisting = prev
          .map((oldC) => fetched.find((f) => f.userUuid === oldC.userUuid))
          .filter(Boolean);
        return [...refreshedExisting, ...newUsers];
      });
    } catch (err) {
      console.error('Failed to load contacts:', err);
    }
  };

  const fetchGroups = async () => {
    try {
      const res = await api.get('/groups');
      setGroups(res.data || []);
    } catch (err) {
      console.error('Failed to load groups:', err);
    }
  };

  const fetchInitialUnreads = async () => {
    try {
      const res = await api.get('/chats/unread-counts');
      if (res.data) setUnreadMap(res.data);
    } catch (err) {}
  };

  useEffect(() => {
    if (!currentUser) return;

    fetchContacts();
    fetchGroups();
    fetchInitialUnreads();
    const interval = setInterval(() => {
      fetchContacts();
      fetchGroups();
    }, 8000);

    wsService.connect(
      // 1-on-1 message callback
      (incomingMsg) => {
        const current = activeTargetRef.current;
        const partnerUuid =
          incomingMsg.senderUuid === currentUser.userUuid
            ? incomingMsg.receiverUuid
            : incomingMsg.senderUuid;

        fetchContacts();
        moveContactToTop(partnerUuid);

        if (incomingMsg.receiverUuid === currentUser.userUuid && incomingMsg.messageUuid) {
          wsService.sendDelivered(incomingMsg.messageUuid);
        }

        const isForOpenChat =
          current?.type === 'DIRECT' &&
          ((incomingMsg.senderUuid === current.data.userUuid &&
            incomingMsg.receiverUuid === currentUser.userUuid) ||
            (incomingMsg.senderUuid === currentUser.userUuid &&
              incomingMsg.receiverUuid === current.data.userUuid));

        if (isForOpenChat) {
          shouldScrollToBottomRef.current = true;
          if (incomingMsg.senderUuid === current.data.userUuid) {
            wsService.sendRead(current.data.userUuid);
          }
          setMessages((prev) => {
            const index = prev.findIndex(
              (m) =>
                m.messageUuid === incomingMsg.messageUuid ||
                (incomingMsg.clientMsgId && m.clientMsgId === incomingMsg.clientMsgId)
            );
            if (index !== -1) {
              const updated = [...prev];
              updated[index] = incomingMsg;
              return updated;
            }
            return [...prev, incomingMsg];
          });
        } else if (incomingMsg.senderUuid !== currentUser.userUuid) {
          setUnreadMap((prev) => ({
            ...prev,
            [incomingMsg.senderUuid]: (prev[incomingMsg.senderUuid] || 0) + 1
          }));
        }
      },
      // Connection status callback
      (connectedStatus) => {
        setIsConnected(connectedStatus);
      },
      // 1-on-1 receipts callback
      (receiptUpdate) => {
        if (!receiptUpdate?.messageUuids) return;
        const idSet = new Set(receiptUpdate.messageUuids);
        setMessages((prev) =>
          prev.map((m) => (idSet.has(m.messageUuid) ? { ...m, status: receiptUpdate.status } : m))
        );
      },
      // Group message & translation callback
      (incomingGroupMsg) => {
        const current = activeTargetRef.current;
        fetchGroups();
        moveGroupToTop(incomingGroupMsg.groupUuid);

        const isForOpenGroup =
          current?.type === 'GROUP' && current.data.groupUuid === incomingGroupMsg.groupUuid;

        if (isForOpenGroup) {
          shouldScrollToBottomRef.current = true;
          setMessages((prev) => {
            const index = prev.findIndex(
              (m) =>
                m.messageUuid === incomingGroupMsg.messageUuid ||
                (incomingGroupMsg.clientMsgId && m.clientMsgId === incomingGroupMsg.clientMsgId)
            );
            if (index !== -1) {
              const updated = [...prev];
              updated[index] = incomingGroupMsg;
              return updated;
            }
            return [...prev, incomingGroupMsg];
          });
        } else if (incomingGroupMsg.senderUuid !== currentUser.userUuid) {
          const badgeKey = 'group:' + incomingGroupMsg.groupUuid;
          setUnreadMap((prev) => ({
            ...prev,
            [badgeKey]: (prev[badgeKey] || 0) + 1
          }));
        }
      }
    );

    return () => {
      clearInterval(interval);
      wsService.disconnect();
    };
  }, [currentUser]);

  // Load conversation when switching between 1-on-1 contact or group
  useEffect(() => {
    if (!activeTarget) return;

    setEditingMsg(null);
    setInputMessage('');
    setPage(0);
    setHasMore(false);
    shouldScrollToBottomRef.current = true;

    if (activeTarget.type === 'DIRECT') {
      const contact = activeTarget.data;
      setUnreadMap((prev) => {
        const copy = { ...prev };
        delete copy[contact.userUuid];
        return copy;
      });

      const loadDirectContext = async () => {
        try {
          const [histRes, prefRes, presRes] = await Promise.allSettled([
            api.get(`/chats/conversation/${contact.userUuid}?page=0&size=20`),
            api.get(`/users/preferences/${contact.userUuid}`),
            api.get(`/chats/presence/${contact.userUuid}`)
          ]);

          if (histRes.status === 'fulfilled') {
            setMessages(histRes.value.data.messages || []);
            setHasMore(Boolean(histRes.value.data.hasNext));
            wsService.sendRead(contact.userUuid);
          } else {
            setMessages([]);
          }

          if (prefRes.status === 'fulfilled') {
            setLangPref(prefRes.value.data.targetLanguage || 'DEFAULT');
          } else {
            setLangPref('DEFAULT');
          }

          if (presRes.status === 'fulfilled') {
            setContactOnline(Boolean(presRes.value.data.online));
          }
        } catch (err) {
          setMessages([]);
        }
      };

      loadDirectContext();
      const presenceTimer = setInterval(async () => {
        try {
          const res = await api.get(`/chats/presence/${contact.userUuid}`);
          setContactOnline(Boolean(res.data.online));
        } catch (e) {}
      }, 5000);

      return () => clearInterval(presenceTimer);
    } else if (activeTarget.type === 'GROUP') {
      const group = activeTarget.data;
      const badgeKey = 'group:' + group.groupUuid;
      setUnreadMap((prev) => {
        const copy = { ...prev };
        delete copy[badgeKey];
        return copy;
      });
      setLangPref(group.myTargetLanguage || currentUser.preferredLanguage || 'en');

      const loadGroupContext = async () => {
        try {
          const [histRes, detailRes] = await Promise.allSettled([
            api.get(`/groups/${group.groupUuid}/messages?page=0&size=20`),
            api.get(`/groups/${group.groupUuid}`)
          ]);

          if (histRes.status === 'fulfilled') {
            setMessages(histRes.value.data.messages || []);
            setHasMore(Boolean(histRes.value.data.hasNext));
          } else {
            setMessages([]);
          }

          if (detailRes.status === 'fulfilled') {
            const freshGroup = detailRes.value.data;
            setLangPref(freshGroup.myTargetLanguage || 'en');
            setActiveTarget((prev) =>
              prev?.type === 'GROUP' && prev.data.groupUuid === freshGroup.groupUuid
                ? { type: 'GROUP', data: freshGroup }
                : prev
            );
          }
        } catch (err) {
          setMessages([]);
        }
      };

      loadGroupContext();
    }
  }, [activeTarget?.type, activeTarget?.type === 'DIRECT' ? activeTarget.data.userUuid : activeTarget?.data?.groupUuid]);

  const handleLanguagePreferenceChange = async (newLang) => {
    setLangPref(newLang);
    if (!activeTarget) return;

    if (activeTarget.type === 'DIRECT') {
      try {
        const isNone = newLang === 'NONE';
        const targetLang =
          newLang === 'DEFAULT' ? currentUser.preferredLanguage || currentUser.motherTongue : newLang;
        await api.put(`/users/preferences/${activeTarget.data.userUuid}`, {
          targetLanguage: targetLang,
          autoTranslateEnabled: !isNone
        });
      } catch (err) {
        console.error('Failed to update contact translation preference:', err);
      }
    } else if (activeTarget.type === 'GROUP') {
      try {
        const targetLang =
          newLang === 'DEFAULT' ? currentUser.preferredLanguage || currentUser.motherTongue : newLang;
        const res = await api.put(`/groups/${activeTarget.data.groupUuid}/language`, {
          targetLanguage: targetLang
        });
        const updatedGroup = res.data;
        setGroups((prev) =>
          prev.map((g) => (g.groupUuid === updatedGroup.groupUuid ? updatedGroup : g))
        );
        setActiveTarget({ type: 'GROUP', data: updatedGroup });

        // Reload group messages so existing history displays in the newly chosen language
        const histRes = await api.get(`/groups/${updatedGroup.groupUuid}/messages?page=0&size=20`);
        setMessages(histRes.data.messages || []);
      } catch (err) {
        console.error('Failed to update group language preference:', err);
      }
    }
  };

  const loadOlderMessages = async () => {
    if (!activeTarget || !hasMore || loadingOlder) return;
    setLoadingOlder(true);
    shouldScrollToBottomRef.current = false;

    const nextPage = page + 1;
    const container = chatContainerRef.current;
    const previousScrollHeight = container ? container.scrollHeight : 0;

    try {
      const url =
        activeTarget.type === 'DIRECT'
          ? `/chats/conversation/${activeTarget.data.userUuid}?page=${nextPage}&size=20`
          : `/groups/${activeTarget.data.groupUuid}/messages?page=${nextPage}&size=20`;

      const res = await api.get(url);
      const olderMessages = res.data.messages || [];

      setMessages((prev) => {
        const existingIds = new Set(prev.map((m) => m.messageUuid));
        const uniqueOlder = olderMessages.filter((m) => !existingIds.has(m.messageUuid));
        return [...uniqueOlder, ...prev];
      });

      setPage(nextPage);
      setHasMore(Boolean(res.data.hasNext));

      requestAnimationFrame(() => {
        if (container) {
          container.scrollTop = container.scrollHeight - previousScrollHeight;
        }
      });
    } catch (err) {
      console.error('Failed to load older messages:', err);
    } finally {
      setLoadingOlder(false);
    }
  };

  const handleScroll = (e) => {
    if (e.target.scrollTop === 0 && hasMore && !loadingOlder) {
      loadOlderMessages();
    }
  };

  const handleAddContactByPhone = async (e) => {
    e.preventDefault();
    if (!newContactPhone.trim()) return;

    setLookupError('');
    setLookupLoading(true);
    try {
      const res = await api.post('/users/contacts/search-add', {
        phoneNumber: newContactPhone.trim(),
        name: newContactName.trim()
      });
      const addedFriend = res.data;

      setContacts((prev) => {
        const filtered = prev.filter((c) => c.userUuid !== addedFriend.userUuid);
        return [addedFriend, ...filtered];
      });

      setActiveTarget({ type: 'DIRECT', data: addedFriend });
      setIsAddModalOpen(false);
      setNewContactPhone('');
      setNewContactName('');
    } catch (err) {
      setLookupError(err.response?.data?.message || 'Nobody exists like that');
    } finally {
      setLookupLoading(false);
    }
  };

  const toggleMemberSelection = (uuid) => {
    setSelectedMemberUuids((prev) =>
      prev.includes(uuid) ? prev.filter((id) => id !== uuid) : [...prev, uuid]
    );
  };

  const handleCreateGroup = async (e) => {
    e.preventDefault();
    if (!groupName.trim() || selectedMemberUuids.length === 0) return;

    setGroupError('');
    setGroupCreating(true);
    try {
      const membersPayload = selectedMemberUuids.map((uuid) => {
        const contact = contacts.find((c) => c.userUuid === uuid);
        return {
          userUuid: uuid,
          displayName: contact?.displayName || contact?.phoneNumber || 'Member',
          targetLanguage: contact?.preferredLanguage || contact?.motherTongue || 'en'
        };
      });

      const res = await api.post('/groups', {
        name: groupName.trim(),
        description: groupDesc.trim() || null,
        creatorDisplayName: currentUser.displayName,
        creatorLanguage: currentUser.preferredLanguage || currentUser.motherTongue || 'en',
        members: membersPayload
      });

      const createdGroup = res.data;
      setGroups((prev) => [createdGroup, ...prev]);
      setActiveTarget({ type: 'GROUP', data: createdGroup });
      setIsCreateGroupOpen(false);
      setGroupName('');
      setGroupDesc('');
      setSelectedMemberUuids([]);
    } catch (err) {
      setGroupError(err.response?.data?.message || 'Failed to create group');
    } finally {
      setGroupCreating(false);
    }
  };

  const handleSendMessage = (e) => {
    e.preventDefault();
    if (!inputMessage.trim() || !activeTarget) return;

    const textToSend = inputMessage.trim();
    setInputMessage('');

    if (activeTarget.type === 'DIRECT') {
      const contact = activeTarget.data;
      if (editingMsg) {
        wsService.editMessage(editingMsg.messageUuid, textToSend);
        setMessages((prev) =>
          prev.map((m) =>
            m.messageUuid === editingMsg.messageUuid
              ? { ...m, originalText: textToSend, translatedText: null, edited: true }
              : m
          )
        );
        setEditingMsg(null);
        return;
      }

      moveContactToTop(contact.userUuid);
      shouldScrollToBottomRef.current = true;

      const clientMsgId = 'msg-' + Date.now();
      const sentOk = wsService.sendMessage(contact.userUuid, textToSend, clientMsgId);

      setMessages((prev) => [
        ...prev,
        {
          messageUuid: 'temp-' + Date.now(),
          clientMsgId,
          senderUuid: currentUser.userUuid,
          receiverUuid: contact.userUuid,
          originalText: textToSend,
          status: sentOk ? 'SENT' : 'FAILED',
          createdAt: new Date().toISOString()
        }
      ]);
    } else if (activeTarget.type === 'GROUP') {
      const group = activeTarget.data;
      if (editingMsg) {
        wsService.editGroupMessage(group.groupUuid, editingMsg.messageUuid, textToSend);
        setMessages((prev) =>
          prev.map((m) =>
            m.messageUuid === editingMsg.messageUuid
              ? { ...m, originalText: textToSend, translatedText: null, edited: true }
              : m
          )
        );
        setEditingMsg(null);
        return;
      }

      moveGroupToTop(group.groupUuid);
      shouldScrollToBottomRef.current = true;

      const clientMsgId = 'gmsg-' + Date.now();
      wsService.sendGroupMessage(group.groupUuid, textToSend, clientMsgId);

      setMessages((prev) => [
        ...prev,
        {
          messageUuid: 'temp-' + Date.now(),
          clientMsgId,
          groupUuid: group.groupUuid,
          senderUuid: currentUser.userUuid,
          senderName: currentUser.displayName,
          originalText: textToSend,
          createdAt: new Date().toISOString()
        }
      ]);
    }
  };

  const renderStatusTicks = (status) => {
    if (status === 'FAILED') {
      return (
        <span className="inline-flex items-center gap-0.5 text-rose-400 font-medium" title="Not delivered">
          <AlertCircle size={12} /> Not delivered
        </span>
      );
    }
    if (status === 'READ') {
      return <CheckCheck size={14} className="text-sky-400 inline" title="Read" />;
    }
    if (status === 'DELIVERED') {
      return <CheckCheck size={14} className="text-white/60 inline" title="Delivered" />;
    }
    return <Check size={14} className="text-white/60 inline" title="Sent" />;
  };

  const handleLogout = async () => {
    try {
      const refreshToken = sessionStorage.getItem('univoice_refresh_token');
      if (refreshToken) await api.post('/auth/logout', { refreshToken });
    } catch (err) {} finally {
      sessionStorage.clear();
      setCurrentUser(null);
      setActiveTarget(null);
      setMessages([]);
      wsService.disconnect();
    }
  };

  if (!currentUser) {
    return <AuthScreen onAuthSuccess={(user) => setCurrentUser(user)} />;
  }

  const normalizedQuery = searchQuery.replace(/\s+/g, '').toLowerCase();
  const filteredGroups = groups.filter((g) =>
    (g.name || '').toLowerCase().includes(normalizedQuery)
  );
  const filteredContacts = contacts.filter((c) => {
    const nameMatch = (c.displayName || '').toLowerCase().includes(normalizedQuery);
    const phoneMatch = (c.phoneNumber || '').replace(/\s+/g, '').toLowerCase().includes(normalizedQuery);
    return nameMatch || phoneMatch;
  });

  const allLanguages = CONTACT_LANG_OPTIONS.filter(
    (opt) => opt.code !== 'DEFAULT' && opt.code !== 'NONE'
  );

  const groupLanguageOptions = [
    { code: 'DEFAULT', label: 'My Default Language' },
    { code: 'NONE', label: 'Unchanged (No Translation)' },
    ...allLanguages
  ];

  return (
    <div className="flex h-screen bg-[#0c1317] text-gray-100 overflow-hidden font-sans">
      {/* SIDEBAR */}
      <div className="w-80 sm:w-96 bg-[#111b21] border-r border-[#222d34] flex flex-col">
        {/* Clickable Profile Header */}
        <div className="p-3 bg-[#202c33] flex justify-between items-center border-b border-[#222d34]">
          <div
            onClick={openProfileModal}
            className="flex items-center gap-2.5 cursor-pointer hover:opacity-85 transition min-w-0"
            title="View & Edit Profile"
          >
            <div className="w-9 h-9 rounded-full bg-[#00a884] flex items-center justify-center font-bold text-white shadow-md shrink-0">
              {(currentUser.displayName || 'U').slice(0, 2).toUpperCase()}
            </div>
            <div className="truncate">
              <h2 className="text-sm font-semibold text-white leading-tight truncate">{currentUser.displayName}</h2>
              <span className="text-[10px] text-[#8696a0] uppercase tracking-wider">
                Native: {currentUser.motherTongue} • Edit Profile
              </span>
            </div>
          </div>

          <div className="flex items-center gap-1.5 shrink-0">
            {/* Global Translation ON/OFF Switch */}
            <button
              type="button"
              onClick={toggleGlobalTranslate}
              className="flex items-center gap-1.5 px-2 py-1 rounded-full bg-[#111b21] border border-[#2a3942] hover:border-[#00a884] transition"
              title={
                globalTranslateOn
                  ? 'Translation ON (Click for Normal Chat)'
                  : 'Normal Chat Mode (Click to enable Translation)'
              }
            >
              <span className="text-[10px] font-medium text-gray-300">
                {globalTranslateOn ? 'Trans' : 'Off'}
              </span>
              <span
                className={`relative inline-flex h-4 w-7 items-center rounded-full transition-colors ${
                  globalTranslateOn ? 'bg-[#00a884]' : 'bg-gray-600'
                }`}
              >
                <span
                  className={`inline-block h-3 w-3 transform rounded-full bg-white transition-transform ${
                    globalTranslateOn ? 'translate-x-3.5' : 'translate-x-0.5'
                  }`}
                />
              </span>
            </button>

            <span
              className={`w-2 h-2 rounded-full transition-colors ${
                isConnected ? 'bg-emerald-500 shadow-md shadow-emerald-500/50' : 'bg-rose-500 animate-pulse'
              }`}
              title={isConnected ? 'Connected' : 'Disconnected'}
            />
            <button
              onClick={() => {
                setGroupError('');
                setSelectedMemberUuids([]);
                setIsCreateGroupOpen(true);
              }}
              className="p-1.5 bg-[#2a3942] hover:bg-[#00a884] rounded-lg text-white transition"
              title="Create New Group"
            >
              <Users size={15} />
            </button>
            <button
              onClick={() => {
                setLookupError('');
                setNewContactPhone(searchQuery);
                setIsAddModalOpen(true);
              }}
              className="p-1.5 bg-[#00a884] hover:bg-emerald-600 rounded-lg text-white transition"
              title="Add New Contact by Phone"
            >
              <UserPlus size={15} />
            </button>
            <button
              onClick={() => {
                fetchContacts();
                fetchGroups();
              }}
              className="p-1.5 hover:bg-[#111b21] rounded-lg text-[#8696a0] hover:text-white transition"
              title="Refresh Chats"
            >
              <RefreshCw size={15} />
            </button>
            <button
              onClick={handleLogout}
              className="p-1.5 hover:bg-[#111b21] rounded-lg text-[#8696a0] hover:text-white transition"
              title="Log Out"
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>

        {/* Search Input */}
        <div className="p-2 bg-[#111b21] border-b border-[#222d34]">
          <div className="relative">
            <Search size={14} className="absolute left-3 top-2.5 text-[#8696a0]" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search groups, contacts, or phone..."
              className="w-full bg-[#202c33] rounded-lg pl-9 pr-3 py-1.5 text-xs text-white placeholder-[#8696a0] focus:outline-none focus:ring-1 focus:ring-[#00a884]"
            />
          </div>
        </div>

        {/* Groups + Contacts List */}
        <div className="flex-1 overflow-y-auto">
          {/* Groups Section */}
          {filteredGroups.length > 0 && (
            <div>
              <div className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-[#00a884] bg-[#0c1317]/60">
                Groups ({filteredGroups.length})
              </div>
              {filteredGroups.map((group) => {
                const isSelected =
                  activeTarget?.type === 'GROUP' && activeTarget.data.groupUuid === group.groupUuid;
                const unreadCount = unreadMap['group:' + group.groupUuid] || 0;
                return (
                  <div
                    key={group.groupUuid}
                    onClick={() => setActiveTarget({ type: 'GROUP', data: group })}
                    className={`flex items-center p-3 border-b border-[#222d34]/40 cursor-pointer transition ${
                      isSelected ? 'bg-[#2a3942]' : 'hover:bg-[#202c33]'
                    }`}
                  >
                    <div className="w-10 h-10 rounded-full bg-sky-500/20 border border-sky-400/40 flex items-center justify-center font-bold text-sky-300 mr-3 shrink-0">
                      <Users size={18} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-center">
                        <h3 className="font-medium text-sm truncate text-gray-100">{group.name}</h3>
                        <div className="flex items-center gap-1.5">
                          {unreadCount > 0 && (
                            <span className="bg-[#00a884] text-[#111b21] text-[10px] font-bold px-1.5 py-0.5 rounded-full">
                              {unreadCount}
                            </span>
                          )}
                          <span className="text-[10px] text-sky-300 uppercase bg-[#202c33] px-1.5 py-0.5 rounded">
                            {group.myTargetLanguage}
                          </span>
                        </div>
                      </div>
                      <p className="text-xs text-[#8696a0] truncate mt-0.5">
                        {group.memberCount} members
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Direct Contacts Section */}
          {filteredGroups.length > 0 && filteredContacts.length > 0 && (
            <div className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-[#8696a0] bg-[#0c1317]/60">
              Direct Chats ({filteredContacts.length})
            </div>
          )}

          {filteredContacts.length === 0 && filteredGroups.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-64 text-[#8696a0] px-6 text-center">
              <UserPlus size={36} className="mb-3 opacity-40 text-[#00a884]" />
              <p className="text-sm font-medium text-gray-300">No chats yet</p>
              <p className="text-xs mt-1 mb-4">Add a friend by phone number or create a multilingual group.</p>
              <button
                onClick={() => {
                  setLookupError('');
                  setNewContactPhone(searchQuery);
                  setIsAddModalOpen(true);
                }}
                className="px-3 py-1.5 bg-[#00a884] hover:bg-emerald-600 text-white font-medium rounded-lg text-xs transition"
              >
                + Add new contact
              </button>
            </div>
          ) : (
            filteredContacts.map((contact) => {
              const isSelected =
                activeTarget?.type === 'DIRECT' && activeTarget.data.userUuid === contact.userUuid;
              const unreadCount = unreadMap[contact.userUuid] || 0;
              return (
                <div
                  key={contact.userUuid}
                  onClick={() => setActiveTarget({ type: 'DIRECT', data: contact })}
                  className={`flex items-center p-3 border-b border-[#222d34]/40 cursor-pointer transition ${
                    isSelected ? 'bg-[#2a3942]' : 'hover:bg-[#202c33]'
                  }`}
                >
                  <div className="w-10 h-10 rounded-full bg-[#00a884]/30 border border-[#00a884]/40 flex items-center justify-center font-bold text-white mr-3 shrink-0">
                    {(contact.displayName || contact.phoneNumber).slice(0, 2).toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-center">
                      <h3 className="font-medium text-sm truncate text-gray-100">{contact.displayName}</h3>
                      <div className="flex items-center gap-1.5">
                        {unreadCount > 0 && (
                          <span className="bg-[#00a884] text-[#111b21] text-[10px] font-bold px-1.5 py-0.5 rounded-full">
                            {unreadCount}
                          </span>
                        )}
                        <span className="text-[10px] text-[#8696a0] uppercase bg-[#202c33] px-1.5 py-0.5 rounded">
                          {contact.motherTongue}
                        </span>
                      </div>
                    </div>
                    <p className="text-xs text-[#8696a0] truncate mt-0.5">{contact.phoneNumber}</p>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* CHAT AREA */}
      <div className="flex-1 flex flex-col bg-[#0b141a]">
        {activeTarget ? (
          <>
            {/* Chat Header */}
            <div className="p-3 bg-[#202c33] border-b border-[#222d34] flex items-center justify-between gap-2">
              {activeTarget.type === 'DIRECT' ? (
                <div className="flex items-center gap-3 min-w-0">
                  <div className="relative">
                    <div className="w-10 h-10 rounded-full bg-[#00a884]/30 flex items-center justify-center font-bold text-white">
                      {(activeTarget.data.displayName || 'U').slice(0, 2).toUpperCase()}
                    </div>
                    <span
                      className={`absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full border-2 border-[#202c33] ${
                        contactOnline ? 'bg-emerald-400' : 'bg-gray-500'
                      }`}
                    />
                  </div>
                  <div className="truncate">
                    <h3 className="font-semibold text-sm text-white truncate">{activeTarget.data.displayName}</h3>
                    <p className="text-[11px] text-[#8696a0]">
                      {contactOnline ? (
                        <span className="text-emerald-400 font-medium">Online</span>
                      ) : (
                        <span>Offline</span>
                      )}{' '}
                      • {activeTarget.data.phoneNumber}
                    </p>
                  </div>
                </div>
              ) : (
                <div
                  onClick={() => setIsGroupInfoOpen(true)}
                  className="flex items-center gap-3 min-w-0 cursor-pointer hover:opacity-85 transition"
                  title="View Group Members"
                >
                  <div className="w-10 h-10 rounded-full bg-sky-500/20 border border-sky-400/40 flex items-center justify-center text-sky-300">
                    <Users size={18} />
                  </div>
                  <div className="truncate">
                    <div className="flex items-center gap-1.5">
                      <h3 className="font-semibold text-sm text-white truncate">{activeTarget.data.name}</h3>
                      <Info size={13} className="text-[#8696a0] shrink-0" />
                    </div>
                    <p className="text-[11px] text-[#8696a0] truncate">
                      {activeTarget.data.members?.map((m) => m.displayName).join(', ') ||
                        `${activeTarget.data.memberCount} members`}
                    </p>
                  </div>
                </div>
              )}

              {/* Per-Contact or Per-Group Incoming Translation Language Preference */}
              <div className="flex items-center gap-1.5 bg-[#111b21] px-2.5 py-1.5 rounded-lg border border-[#2a3942]">
                <Globe size={14} className="text-[#00a884] shrink-0" />
                <select
                  value={langPref}
                  onChange={(e) => handleLanguagePreferenceChange(e.target.value)}
                  className="bg-transparent text-xs text-gray-200 focus:outline-none cursor-pointer"
                  title="Choose how incoming messages are translated for you"
                >
                  {(activeTarget.type === 'DIRECT' ? CONTACT_LANG_OPTIONS : groupLanguageOptions).map((opt) => (
                    <option key={opt.code} value={opt.code} className="bg-[#111b21] text-white">
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Messages Scroll Area */}
            <div ref={chatContainerRef} onScroll={handleScroll} className="flex-1 p-4 overflow-y-auto space-y-3">
              {hasMore && (
                <div className="flex justify-center pb-2">
                  <button
                    onClick={loadOlderMessages}
                    disabled={loadingOlder}
                    className="flex items-center gap-1.5 text-xs bg-[#202c33] hover:bg-[#2a3942] text-[#00a884] px-3 py-1.5 rounded-full shadow transition"
                  >
                    <ChevronUp size={14} />
                    {loadingOlder ? 'Loading history...' : 'Load older messages'}
                  </button>
                </div>
              )}

              {messages.map((msg, idx) => {
                const isMe = msg.senderUuid === currentUser.userUuid;
                const isGroup = activeTarget.type === 'GROUP';
                return (
                  <div key={msg.messageUuid || idx} className={`group flex ${isMe ? 'justify-end' : 'justify-start'}`}>
                    <div
                      className={`relative max-w-[70%] rounded-2xl px-4 py-2.5 shadow-md ${
                        isMe ? 'bg-[#005c4b] text-white rounded-br-xs' : 'bg-[#202c33] text-gray-100 rounded-bl-xs'
                      }`}
                    >
                      {isMe && !msg.deleted && !String(msg.messageUuid).startsWith('temp-') && (
                        <div className="hidden group-hover:flex items-center gap-1 absolute -top-3 right-2 bg-[#111b21] border border-[#2a3942] rounded-md px-1.5 py-0.5 shadow-lg">
                          <button
                            onClick={() => {
                              setEditingMsg(msg);
                              setInputMessage(msg.originalText);
                            }}
                            className="p-1 text-gray-300 hover:text-[#00a884]"
                            title="Edit message"
                          >
                            <Pencil size={12} />
                          </button>
                          <button
                            onClick={() =>
                              isGroup
                                ? wsService.deleteGroupMessage(activeTarget.data.groupUuid, msg.messageUuid)
                                : wsService.deleteMessage(msg.messageUuid)
                            }
                            className="p-1 text-gray-300 hover:text-rose-400"
                            title="Delete message"
                          >
                            <Trash2 size={12} />
                          </button>
                        </div>
                      )}

                      {/* Group Sender Name Badge */}
                      {isGroup && !isMe && (
                        <div className={`text-[11px] font-semibold mb-1 ${getSenderColor(msg.senderUuid)}`}>
                          {msg.senderName || 'Member'}
                          {msg.senderLanguage && (
                            <span className="ml-1.5 text-[9px] uppercase text-white/40 font-normal">
                              ({msg.senderLanguage})
                            </span>
                          )}
                        </div>
                      )}

                      <p dir="auto" className={`text-sm leading-relaxed ${msg.deleted ? 'italic text-white/60' : ''}`}>
                        {msg.originalText}
                      </p>

                      {globalTranslateOn && !isMe && !msg.deleted && msg.translatedText && msg.translatedText !== msg.originalText && (
                        <div className="mt-2 pt-2 border-t border-white/10 flex items-start gap-1.5 text-xs text-emerald-200">
                          <Globe size={13} className="shrink-0 mt-0.5 text-emerald-400" />
                          <span dir="auto" className="font-medium">{msg.translatedText}</span>
                        </div>
                      )}

                      <div className="flex items-center justify-end gap-1.5 text-[10px] text-white/50 mt-1">
                        {msg.edited && !msg.deleted && <span className="italic">edited</span>}
                        <span>
                          {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                        {isMe && !isGroup && renderStatusTicks(msg.status)}
                      </div>
                    </div>
                  </div>
                );
              })}
              <div ref={messagesEndRef} />
            </div>

            {/* Edit Banner */}
            {editingMsg && (
              <div className="px-4 py-2 bg-[#182229] border-t border-[#222d34] flex justify-between items-center text-xs">
                <span className="text-[#00a884] font-medium">Editing message...</span>
                <button
                  onClick={() => {
                    setEditingMsg(null);
                    setInputMessage('');
                  }}
                  className="text-[#8696a0] hover:text-white"
                >
                  Cancel
                </button>
              </div>
            )}

            {/* Input Bar */}
            <form onSubmit={handleSendMessage} className="p-3 bg-[#202c33] flex items-center gap-2">
              <input
                type="text"
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                placeholder={
                  editingMsg
                    ? 'Update your message...'
                    : activeTarget.type === 'DIRECT'
                    ? `Message ${activeTarget.data.displayName}...`
                    : `Message ${activeTarget.data.name}...`
                }
                className="flex-1 bg-[#2a3942] text-white rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-[#00a884] placeholder-[#8696a0]"
              />
              <button
                type="submit"
                disabled={!inputMessage.trim()}
                className="p-2.5 bg-[#00a884] hover:bg-emerald-600 disabled:opacity-40 text-white rounded-lg transition shadow-md"
              >
                <Send size={18} />
              </button>
            </form>
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-[#8696a0] select-none px-6 text-center">
            <MessageSquare size={48} className="mb-3 opacity-30 text-[#00a884]" />
            <h3 className="text-base font-semibold text-gray-300">Welcome to UniVoice</h3>
            <p className="text-xs mt-1 text-[#8696a0] max-w-sm">
              Select a contact or create a multilingual group where every member reads messages in their own language.
            </p>
          </div>
        )}
      </div>

      {/* CREATE GROUP MODAL (Select from Saved Contacts) */}
      {isCreateGroupOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-[#111b21] border border-[#222d34] rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-base font-semibold text-white flex items-center gap-2">
                <Users size={18} className="text-[#00a884]" /> Create Multilingual Group
              </h3>
              <button onClick={() => setIsCreateGroupOpen(false)} className="text-[#8696a0] hover:text-white">
                <X size={18} />
              </button>
            </div>

            {groupError && (
              <div className="mb-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/40 text-rose-300 text-xs">
                {groupError}
              </div>
            )}

            <form onSubmit={handleCreateGroup} className="space-y-4">
              <div>
                <label className="text-xs text-[#8696a0] block mb-1">Group Name *</label>
                <input
                  type="text"
                  required
                  value={groupName}
                  onChange={(e) => setGroupName(e.target.value)}
                  placeholder="e.g. Global Project Team"
                  className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                />
              </div>

              <div>
                <label className="text-xs text-[#8696a0] block mb-1">Description (Optional)</label>
                <input
                  type="text"
                  value={groupDesc}
                  onChange={(e) => setGroupDesc(e.target.value)}
                  placeholder="What is this group about?"
                  className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                />
              </div>

              <div>
                <label className="text-xs text-[#8696a0] block mb-1.5">
                  Select Members from Your Contacts * ({selectedMemberUuids.length} selected)
                </label>
                {contacts.length === 0 ? (
                  <p className="text-xs text-amber-300 bg-amber-500/10 border border-amber-500/30 rounded-lg p-3">
                    Add contacts first using the + person icon before creating a group.
                  </p>
                ) : (
                  <div className="max-h-48 overflow-y-auto space-y-1.5 bg-[#0c1317] p-2 rounded-lg border border-[#222d34]">
                    {contacts.map((c) => {
                      const checked = selectedMemberUuids.includes(c.userUuid);
                      return (
                        <div
                          key={c.userUuid}
                          onClick={() => toggleMemberSelection(c.userUuid)}
                          className={`flex items-center justify-between p-2 rounded-lg cursor-pointer transition ${
                            checked ? 'bg-[#00a884]/20 border border-[#00a884]/50' : 'hover:bg-[#202c33]'
                          }`}
                        >
                          <div className="flex items-center gap-2.5 min-w-0">
                            <input
                              type="checkbox"
                              checked={checked}
                              onChange={() => {}}
                              className="accent-[#00a884] cursor-pointer"
                            />
                            <div className="truncate">
                              <p className="text-xs font-medium text-white truncate">{c.displayName}</p>
                              <p className="text-[10px] text-[#8696a0]">{c.phoneNumber}</p>
                            </div>
                          </div>
                          <span className="text-[10px] uppercase bg-[#202c33] text-[#8696a0] px-1.5 py-0.5 rounded">
                            {c.motherTongue}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              <button
                type="submit"
                disabled={groupCreating || !groupName.trim() || selectedMemberUuids.length === 0}
                className="w-full bg-[#00a884] hover:bg-emerald-600 disabled:opacity-40 text-white font-semibold py-2.5 rounded-lg text-sm transition"
              >
                {groupCreating ? 'Creating Group...' : 'Create Group'}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* GROUP INFO / MEMBERS MODAL */}
      {isGroupInfoOpen && activeTarget?.type === 'GROUP' && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-[#111b21] border border-[#222d34] rounded-2xl w-full max-w-sm p-6 shadow-2xl">
            <div className="flex justify-between items-center mb-3">
              <h3 className="text-base font-semibold text-white">{activeTarget.data.name}</h3>
              <button onClick={() => setIsGroupInfoOpen(false)} className="text-[#8696a0] hover:text-white">
                <X size={18} />
              </button>
            </div>
            {activeTarget.data.description && (
              <p className="text-xs text-[#8696a0] mb-4">{activeTarget.data.description}</p>
            )}

            <div className="text-xs text-[#00a884] font-semibold uppercase tracking-wider mb-2">
              Members ({activeTarget.data.members?.length || activeTarget.data.memberCount})
            </div>
            <div className="max-h-56 overflow-y-auto space-y-2">
              {(activeTarget.data.members || []).map((m) => (
                <div
                  key={m.userUuid}
                  className="flex items-center justify-between bg-[#202c33] px-3 py-2 rounded-lg"
                >
                  <div>
                    <p className="text-xs font-medium text-white">
                      {m.displayName} {m.userUuid === currentUser.userUuid && '(You)'}
                    </p>
                    <p className="text-[10px] text-[#8696a0]">{m.role}</p>
                  </div>
                  <span className="text-[10px] uppercase bg-[#111b21] text-emerald-300 px-2 py-0.5 rounded border border-[#2a3942]">
                    Reads: {m.targetLanguage}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ADD NEW CONTACT MODAL */}
      {isAddModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-[#111b21] border border-[#222d34] rounded-2xl w-full max-w-sm p-6 shadow-2xl">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-base font-semibold text-white">Find & Add Contact</h3>
              <button onClick={() => setIsAddModalOpen(false)} className="text-[#8696a0] hover:text-white">
                <X size={18} />
              </button>
            </div>

            {lookupError && (
              <div className="mb-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/40 text-rose-300 text-xs font-medium">
                {lookupError}
              </div>
            )}

            <form onSubmit={handleAddContactByPhone} className="space-y-4">
              <div>
                <label className="text-xs text-[#8696a0] block mb-1">Phone Number *</label>
                <input
                  type="text"
                  required
                  value={newContactPhone}
                  onChange={(e) => setNewContactPhone(e.target.value)}
                  placeholder="e.g. +919876543210 or +819012345678"
                  className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                />
              </div>
              <div>
                <label className="text-xs text-[#8696a0] block mb-1">Nickname (Optional)</label>
                <input
                  type="text"
                  value={newContactName}
                  onChange={(e) => setNewContactName(e.target.value)}
                  placeholder="Leave blank to use registered name"
                  className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                />
              </div>
              <button
                type="submit"
                disabled={lookupLoading}
                className="w-full bg-[#00a884] hover:bg-emerald-600 disabled:opacity-50 text-white font-medium py-2 rounded-lg text-sm transition"
              >
                {lookupLoading ? 'Searching...' : 'Search & Start Chat'}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* MY PROFILE MODAL */}
      {isProfileOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-[#111b21] border border-[#222d34] rounded-2xl w-full max-w-sm p-6 shadow-2xl">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-base font-semibold text-white">My Profile</h3>
              <button onClick={() => setIsProfileOpen(false)} className="text-[#8696a0] hover:text-white">
                <X size={18} />
              </button>
            </div>

            {profileMessage && (
              <div className="mb-4 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/40 text-emerald-300 text-xs">
                {profileMessage}
              </div>
            )}

            <form onSubmit={handleSaveProfile} className="space-y-4">
              <div>
                <label className="text-xs text-[#8696a0] flex items-center justify-between mb-1">
                  <span>Registered Phone Number</span>
                  <span className="inline-flex items-center gap-1 text-[10px] text-amber-400">
                    <Lock size={10} /> Cannot be changed
                  </span>
                </label>
                <input
                  type="text"
                  disabled
                  value={currentUser.phoneNumber || 'Verified Account'}
                  className="w-full bg-[#182229] border border-[#222d34] rounded-lg px-3 py-2 text-sm text-[#8696a0] cursor-not-allowed select-none"
                />
              </div>

              <div>
                <label className="text-xs text-[#8696a0] block mb-1">Display Name</label>
                <input
                  type="text"
                  required
                  value={profileForm.displayName}
                  onChange={(e) => setProfileForm({ ...profileForm, displayName: e.target.value })}
                  className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                />
              </div>

              <div>
                <label className="text-xs text-[#8696a0] block mb-1">Native Tongue (Typing Language)</label>
                <select
                  value={profileForm.motherTongue}
                  onChange={(e) => setProfileForm({ ...profileForm, motherTongue: e.target.value })}
                  className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                >
                  {allLanguages.map((l) => (
                    <option key={l.code} value={l.code}>{l.label}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs text-[#8696a0] block mb-1">Default Incoming Translation Language</label>
                <select
                  value={profileForm.defaultLanguage}
                  onChange={(e) => setProfileForm({ ...profileForm, defaultLanguage: e.target.value })}
                  className="w-full bg-[#202c33] border border-[#2a3942] rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-[#00a884]"
                >
                  {allLanguages.map((l) => (
                    <option key={l.code} value={l.code}>{l.label}</option>
                  ))}
                </select>
              </div>

              <button
                type="submit"
                disabled={profileSaving}
                className="w-full bg-[#00a884] hover:bg-emerald-600 disabled:opacity-50 text-white font-semibold py-2.5 rounded-lg text-sm transition"
              >
                {profileSaving ? 'Saving...' : 'Save Changes'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}