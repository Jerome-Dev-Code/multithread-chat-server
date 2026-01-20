package com.portfolio.chat.infra.network;

import com.portfolio.chat.commands.CommandHandler;
import com.portfolio.chat.commands.ListCommand;
import com.portfolio.chat.commands.QuitCommand;
import com.portfolio.chat.core.MessageSender;
import com.portfolio.chat.domain.ChatRoom;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class SocketClientHandler implements Runnable, MessageSender {
    private final Socket socket;
    private final ChatRoom chatRoom;
    private PrintWriter out;
    private String username = "Unknown";

    // Utilisation de volatile pour garantir la visibilité entre les threads
    private volatile boolean connected = true;

    public SocketClientHandler(Socket socket, ChatRoom chatRoom) {
        this.socket = socket;
        this.chatRoom = chatRoom;
    }

    // Cette méthode sera appelée au début de run()
    // mais peut aussi être appelée manuellement dans un test.
    public void setupStreams() throws IOException {
        if (this.out == null) {
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }
    }

    @Override
    public void run() {
        try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            this.setupStreams();

            out.println("Enter nickname :");
            // Lecture du pseudo avec vérification de fermeture immédiate
            String inputName = in.readLine();
            if (inputName == null || inputName.isBlank()) {
                connected = false;
                return;
            }
            this.username = inputName.trim();
            chatRoom.join(username, this);

            var invoker = new CommandHandler();
            invoker.register(new ListCommand(chatRoom, this));
            invoker.register(new QuitCommand(this::disconnect));

            String input;
            while (connected && !socket.isClosed()) {
                // On vérifie si une donnée est disponible avant de lire pour éviter le blocage fatal
                if (socket.getInputStream().available() > 0 || in.ready()) {
                    input = in.readLine();
                    if (input == null) {
                        break;
                    }
                    if (!invoker.handle(input)) {
                        chatRoom.broadcast(username, input);
                    }
                } else {
                    // Petite pause pour éviter de saturer le CPU (Busy spinning)
                    Thread.sleep(10);
                }
            }
        } catch (SocketException e) {
            if (connected && !socket.isClosed()) {
                throw new RuntimeException("NETWORK_ERROR: " + e.getMessage(), e);
            }
        } catch (EOFException e) {
            // Remonte la fin de flux inattendue
            throw new RuntimeException("STREAM_CLOSED_ERROR: Unexpected end of stream for " + username, e);
        } catch (IOException e) {
            if (connected) {
                // Log seulement si on n'était pas en train de fermer volontairement
                System.err.println("IO Error: " + e.getMessage());
            }
        } catch (InterruptedException e) {
            // Remonte l'interruption du thread
            Thread.currentThread().interrupt();
            throw new RuntimeException("THREAD_INTERRUPTED: " + e.getMessage(), e);
        } finally {
            disconnect();

        }
    }

    @Override
    public void sendMessage(String message) {
        if (connected && out != null){
            out.println(message);
        }
    }
    private synchronized void disconnect(){
        if (!connected) {
            return;
        }

        connected = false;

        if (username != null && !username.equals("Unknown")) {
            chatRoom.leave(username);
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {

        }
    }
}