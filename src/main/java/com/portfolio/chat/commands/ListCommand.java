package com.portfolio.chat.commands;

import com.portfolio.chat.core.Command;
import com.portfolio.chat.core.MessageSender;
import com.portfolio.chat.domain.ChatRoom;

public class ListCommand implements Command {
    private final ChatRoom chatRoom;
    private final MessageSender sender;
    public ListCommand(ChatRoom chatRoom, MessageSender sender) {
        this.chatRoom = chatRoom;
        this.sender = sender;
    }
    @Override
    public void execute(String[] args) {
        var users = chatRoom.getOnlineUsers();
        sender.sendMessage("--- Online Users (" + users.size() + ") ---");
        users.forEach(u -> sender.sendMessage("- " + u));
    }
    @Override public String getCommandName() { return "/list"; }
    @Override public String getDescription() { return "Shows the list of connected users"; }
}
