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
        // Si ce n'est pas une commande, on laisse tomber tout de suite
        if (!input.startsWith("/")) {
            return false;
        }

        // C'est une tentative de commande
        var cmd = commands.get(input.split(" ")[0].toLowerCase());

        if (cmd != null) {
            cmd.accept(username, room);
        } else {
            // Optionnel : On pourrait envoyer un message d'erreur privé ici
            System.out.println("[DEBUG] Commande inconnue tentée par " + username);
        }

        // On retourne true systématiquement car le message commençait par /
        // et ne doit donc pas être traité comme un message de chat standard.
        return true;
    }
}
