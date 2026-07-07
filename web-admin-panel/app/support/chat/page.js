"use client";

import { useEffect, useState, useRef } from "react";
import AdminShell from "@/components/AdminShell";
import { useAuth } from "@/context/AuthContext";
import { Loader2, Send, User, MessageSquare, Bot } from "lucide-react";

export default function SupportChatPage() {
  const { user } = useAuth();
  const [activeChats, setActiveChats] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [messages, setMessages] = useState([]);
  const [loadingChats, setLoadingChats] = useState(true);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [newMessage, setNewMessage] = useState("");
  const [sending, setSending] = useState(false);
  const messageEndRef = useRef(null);

  useEffect(() => {
    fetchActiveChats();
    const interval = setInterval(fetchActiveChats, 10000); // refresh every 10 seconds
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (selectedUser) {
      fetchChatHistory(selectedUser.userId);
    }
  }, [selectedUser]);

  useEffect(() => {
    // Scroll to bottom when messages load
    messageEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const fetchActiveChats = async () => {
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const token = localStorage.getItem("jwt_auth_token") || "";

      const res = await fetch(`${baseUrl}/chat/active-chats`, {
        headers: { "Authorization": `Bearer ${token}` }
      });
      if (!res.ok) throw new Error("Failed to load active chats.");
      const data = await res.json();
      setActiveChats(data.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingChats(false);
    }
  };

  const fetchChatHistory = async (userId) => {
    setLoadingHistory(true);
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const token = localStorage.getItem("jwt_auth_token") || "";

      const res = await fetch(`${baseUrl}/chat/history/${userId}`, {
        headers: { "Authorization": `Bearer ${token}` }
      });
      if (!res.ok) throw new Error("Failed to fetch chat history.");
      const data = await res.json();
      setMessages(data.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingHistory(false);
    }
  };

  const handleSend = async (e) => {
    e.preventDefault();
    if (!newMessage.trim() || !selectedUser || sending) return;

    setSending(true);
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const token = localStorage.getItem("jwt_auth_token") || "";

      const res = await fetch(`${baseUrl}/chat/send`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({
          receiverId: selectedUser.userId,
          message: newMessage.trim()
        })
      });

      if (res.ok) {
        setNewMessage("");
        fetchChatHistory(selectedUser.userId);
      }
    } catch (err) {
      console.error("Failed to send message", err);
    } finally {
      setSending(false);
    }
  };

  return (
    <AdminShell className="p-6 h-screen flex flex-col">
      <header className="mb-4">
        <h1 className="text-2xl font-black text-slate-800 uppercase tracking-wide">Live Support Console</h1>
        <p className="text-slate-500 text-sm">Real-time support chat with Yajmans and Gurujis. Automated AI Bot resolves standard FAQs.</p>
      </header>

      <div className="flex-1 bg-white border border-slate-200 rounded-[35px] shadow-sm overflow-hidden flex min-h-0">
        
        {/* Left Side: Active Chats List */}
        <div className="w-1/3 border-r border-slate-100 flex flex-col min-h-0">
          <div className="p-4 border-b border-slate-100">
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">Active Threads</h3>
          </div>

          <div className="flex-1 overflow-y-auto p-2 space-y-1">
            {loadingChats ? (
              <div className="py-8 text-center text-slate-400">
                <Loader2 className="animate-spin mx-auto mb-2 text-blue-600" />
                <p className="text-xs italic">Loading threads...</p>
              </div>
            ) : activeChats.length === 0 ? (
              <div className="py-12 text-center text-slate-400 text-xs">
                No active chat requests.
              </div>
            ) : (
              activeChats.map((c) => (
                <button
                  key={c.userId}
                  onClick={() => setSelectedUser(c)}
                  className={`w-full p-4 rounded-2xl flex items-start gap-3 transition-colors text-left ${
                    selectedUser?.userId === c.userId ? "bg-blue-50 text-blue-900" : "hover:bg-slate-50 text-slate-700"
                  }`}
                >
                  <div className="bg-blue-100 p-2.5 rounded-xl text-blue-600">
                    <User size={16} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-bold truncate">{c.userName || "Yajman Member"}</p>
                    <p className="text-[10px] text-slate-400 truncate mt-1">{c.lastMessage}</p>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>

        {/* Right Side: Chat Window */}
        <div className="flex-1 flex flex-col min-h-0 bg-slate-50">
          {selectedUser ? (
            <>
              {/* Chat Header */}
              <div className="p-4 border-b bg-white border-slate-100 flex justify-between items-center">
                <div>
                  <h4 className="text-xs font-bold text-slate-800">{selectedUser.userName}</h4>
                  <p className="text-[9px] text-slate-400 font-mono mt-0.5">UID: {selectedUser.userId}</p>
                </div>
              </div>

              {/* Chat Thread */}
              <div className="flex-1 overflow-y-auto p-4 space-y-3 min-h-0">
                {loadingHistory ? (
                  <div className="py-12 text-center text-slate-400">
                    <Loader2 className="animate-spin mx-auto text-blue-600" />
                  </div>
                ) : (
                  messages.map((m) => {
                    const isMe = m.senderId !== selectedUser.userId;
                    const isBot = m.senderId === "AI_Chatbot";
                    return (
                      <div key={m.id} className={`flex ${isMe ? "justify-end" : "justify-start"}`}>
                        <div className={`max-w-[70%] p-4 rounded-3xl text-xs shadow-sm flex items-start gap-2 ${
                          isBot ? "bg-amber-100/60 border border-amber-200/50 text-slate-800" :
                          isMe ? "bg-blue-600 text-white rounded-tr-none" : "bg-white text-slate-800 rounded-tl-none border"
                        }`}>
                          {isBot && <Bot size={16} className="text-amber-600 shrink-0 mt-0.5" />}
                          <div>
                            <p className="font-bold text-[9px] mb-1 opacity-70">{m.senderName}</p>
                            <p className="leading-relaxed">{m.message}</p>
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
                <div ref={messageEndRef} />
              </div>

              {/* Chat Input */}
              <form onSubmit={handleSend} className="p-4 bg-white border-t border-slate-100 flex gap-2">
                <input 
                  type="text"
                  className="flex-1 border border-slate-200 bg-slate-50 p-4 rounded-2xl text-xs outline-none focus:border-blue-500 text-slate-800"
                  placeholder="Type support reply..."
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                  required
                />
                <button 
                  type="submit"
                  disabled={sending || !newMessage.trim()}
                  className="bg-blue-600 hover:bg-blue-700 text-white p-4 rounded-2xl font-bold uppercase tracking-wider transition-colors shadow-lg shadow-blue-600/10 flex items-center justify-center shrink-0"
                >
                  {sending ? <Loader2 className="animate-spin" size={16} /> : <Send size={16} />}
                </button>
              </form>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-slate-400">
              <MessageSquare size={36} className="text-slate-300 mb-2" />
              <p className="text-xs">Select a chat thread from the menu to start support chat.</p>
            </div>
          )}
        </div>

      </div>
    </AdminShell>
  );
}
