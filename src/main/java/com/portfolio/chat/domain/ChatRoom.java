package com.portfolio.chat.domain;

import com.portfolio.chat.core.ChatObserver;
import com.portfolio.chat.core.MessageSender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Collection;

public class ChatRoom {
    // Thread-safe et performant
    private final ConcurrentHashMap<String, MessageSender> users = new ConcurrentHashMap<>();
    private final List<ChatObserver> observers = new ArrayList<>();

    public void addObserver(ChatObserver observer) {
        observers.add(observer);
    }

    public void join(String name, MessageSender sender) {
        users.put(name, sender);
        observers.forEach(o -> o.onUserJoined(name)); // Notification
        broadcast("SYSTEM", name + " has joined room.");
    }

    public void leave(String name) {
        if (users.remove(name) != null) {
            // 1. Notifier l'événement métier "Départ"
            observers.forEach(o -> o.onUserLeft(name));

            // 2. Notifier l'événement "Message" pour le chat
            broadcast("SYSTEM", name + " has left room.");
        }
    }

    /**
     * Envoie un message à tous les utilisateurs et notifie les observateurs.
     * @param sender Le nom de l'expéditeur (ex: "Alice" ou "SYSTEM")
     * @param message Le contenu brut du message
     */
    public void broadcast(String sender, String message) {
        // 1. Formatage du message pour les clients
        String formattedMessage = sender.equals("SYSTEM") ? "[SYSTEM] " + message : sender + " : " + message;

        // 2. Envoi réseau à tous les utilisateurs connectés
        users.values().forEach(user -> user.sendMessage(formattedMessage));

        // 3. Notification "propre" aux observateurs (ex: pour archivage en BDD ou logs)
        observers.forEach(observer -> observer.onMessageSent(sender, message));
    }

    public Collection<String> getOnlineUsers() {
        return users.keySet();
    }
}