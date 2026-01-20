package com.portfolio.chat.commands;

import com.portfolio.chat.core.Command;
import com.portfolio.chat.domain.ChatRoom;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class CommandHandler {
    private final Map<String, Command> commands = new HashMap<>();

    public void register(Command command) {
        commands.put(command.getCommandName().toLowerCase(), command);
    }

    public boolean handle(String input) {
        if (input == null || !input.startsWith("/")) return false;

        var parts = input.split(" ");
        var commandName = parts[0].toLowerCase();

        return Optional.ofNullable(commands.get(commandName))
                .map(cmd -> {
                    // Extraction des arguments (tout sauf le premier mot)
                    var args = new String[parts.length - 1];
                    System.arraycopy(parts, 1, args, 0, parts.length - 1);
                    cmd.execute(args);
                    return true;
                })
                .orElse(false);
    }
}
