package com.portfolio.chat.domain;

import com.portfolio.chat.core.MessageSender;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Collection;

public class ChatRoom {
    // Thread-safe et performant
    private final ConcurrentHashMap<String, MessageSender> users = new ConcurrentHashMap<>();

    public void join(String name, MessageSender sender) {
        users.put(name, sender);
        broadcast("[SYSTEM] " + name + " a rejoint le salon.");
    }

    public void leave(String name) {
        if (users.remove(name) != null) {
            broadcast("[SYSTEM] " + name + " a quitté le salon.");
        }
    }

    public void broadcast(String message) {
        users.values().forEach(u -> u.sendMessage(message));
    }

    public Collection<String> getOnlineUsers() {
        return users.keySet();
    }
}