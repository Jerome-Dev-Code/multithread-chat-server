package com.portfolio.chat.core;

/**
 * Interface permettant de suivre les événements du chat (Logging, Monitoring, etc.)
 * Respecte le principe OCP : on peut ajouter des logs sans modifier la ChatRoom.
 */
public interface ChatObserver {
    void onUserJoined(String username);
    void onUserLeft(String username);
    void onMessageSent(String sender, String message);
}