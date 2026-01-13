package com.portfolio.chat.commands;

import com.portfolio.chat.domain.ChatRoom;
import java.util.Map;
import java.util.function.BiConsumer;

public class CommandHandler {
    private final Map<String, BiConsumer<String, ChatRoom>> commands;

    public CommandHandler() {
        commands = Map.of(
                "/quit", (name, room) -> room.leave(name),
                "/list", (name, room) -> {
                    String list = "Connectés: " + String.join(", ", room.getOnlineUsers());
                    // Logique pour envoyer au demandeur uniquement...
                }
        );
    }

    public boolean handle(String input, String username, ChatRoom room) {
        if (input.startsWith("/")) {
            var cmd = commands.get(input.split(" ")[0].toLowerCase());
            if (cmd != null) {
                cmd.accept(username, room);
                return true;
            }
        }
        return false;
    }
}
