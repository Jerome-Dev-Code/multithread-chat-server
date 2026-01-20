package com.portfolio.chat.commands;

import com.portfolio.chat.core.Command;

public class QuitCommand implements Command {
    private final Runnable onQuit;
    public QuitCommand(Runnable onQuit) {
        this.onQuit = onQuit;
    }
    @Override
    public void execute(String[] args) {
        onQuit.run();
    }
    @Override public String getCommandName() { return "/quit"; }
    @Override public String getDescription() { return "Disconnects from the server"; }
}
