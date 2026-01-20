package com.portfolio.chat.core;

public interface Command {
    /**
     * @param args Les arguments fournis après le nom de la commande
     */
    void execute(String[] args);

    String getCommandName(); // ex: "/list"
    String getDescription(); // Pour une éventuelle commande /help
}
