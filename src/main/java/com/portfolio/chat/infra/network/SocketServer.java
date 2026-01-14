package com.portfolio.chat.infra.network;

import com.portfolio.chat.domain.ChatRoom;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private final int port;
    private final ChatRoom chatRoom;
    private final ExecutorService threadPool;
    private boolean running = true;

    public SocketServer(int port, ChatRoom chatRoom) {
        this.port = port;
        this.chatRoom = chatRoom;
        // Pool de threads pour gérer les clients en parallèle
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() throws IOException{
        var serverSocket = new ServerSocket(port);
        System.out.println("[TCP] Tchat server started on port " + port);

        while (running) {
            var clientSocket = serverSocket.accept();
            // Chaque client est géré dans son propre thread
            threadPool.execute(new SocketClientHandler(clientSocket, chatRoom));
        }
        stop();
    }

    public void stop() {
        running = false;
        threadPool.shutdown();
    }
}