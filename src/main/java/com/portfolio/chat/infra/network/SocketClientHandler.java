package com.portfolio.chat.infra.network;

import com.portfolio.chat.core.MessageSender;
import com.portfolio.chat.domain.ChatRoom;
import java.io.*;
import java.net.Socket;

public class SocketClientHandler implements Runnable, MessageSender {
    private final Socket socket;
    private final ChatRoom chatRoom;
    private PrintWriter out;
    private String username;

    public SocketClientHandler(Socket socket, ChatRoom chatRoom) {
        this.socket = socket;
        this.chatRoom = chatRoom;
    }

    @Override
    public void run() {
        try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            this.out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Enter nickname :");
            this.username = in.readLine();
            chatRoom.join(username, this);

            String input;
            while ((input = in.readLine()) != null) {
                if (input.equalsIgnoreCase("/quit")) break;
                chatRoom.broadcast(username + " : " + input);
            }
        } catch (IOException e) {
            System.err.println("Client Error : " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    @Override
    public void sendMessage(String message) { if (out != null) out.println(message); }

    @Override
    public String getUsername() { return username; }

    @Override
    public void disconnect() {
        chatRoom.leave(username);
        try { socket.close(); } catch (IOException ignored) {}
    }
}