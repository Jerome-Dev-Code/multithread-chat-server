package com.portfolio.chat.core;

public interface MessageSender {
    void sendMessage(String message);
    String getUsername();
    void disconnect();
}