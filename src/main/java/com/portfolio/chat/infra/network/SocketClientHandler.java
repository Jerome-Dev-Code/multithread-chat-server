package com.portfolio.chat.infra.network;

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

            String input;
            while (connected && !socket.isClosed()) {
                // On vérifie si une donnée est disponible avant de lire pour éviter le blocage fatal
                if (socket.getInputStream().available() > 0 || in.ready()) {
                    input = in.readLine();
                    if (input == null){
                        break;
                    }

                    if (input.equalsIgnoreCase("/quit")){
                        break;
                    }else if (input.equalsIgnoreCase("/list")) {
                        // On récupère la liste des utilisateurs depuis la ChatRoom
                        List<String> users = (List<String>) chatRoom.getOnlineUsers();
                        if (users.isEmpty()) {
                            sendMessage("La salle est vide.");
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("--- Utilisateurs en ligne (").append(users.size()).append(") ---\n");
                            for (String user : users) {
                                sb.append("- ").append(user).append("\n");
                            }
                            sendMessage(sb.toString());
                        }
                    }else {
                        chatRoom.broadcast(username, input);
                    }
                } else {
                    // Petite pause pour éviter de saturer le CPU (Busy spinning)
                    Thread.sleep(10);
                }
            }
        }catch (SocketException | EOFException e) {
            //System.err.println("Client Error : " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            if (connected) {
                System.err.println("IO Error with " + username + ": " + e.getMessage());
            }
            //System.err.println("Client Error : " + e.getMessage());
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

    @Override
    public String getUsername() { return username; }

    @Override
    public synchronized void disconnect() {
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
        } catch (IOException ignored) {

        }
    }
}