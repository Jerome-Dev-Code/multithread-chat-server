package com.portfolio.chat.infra.network;

import com.portfolio.chat.domain.ChatRoom;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketServer {
    private final int port;
    private final ChatRoom chatRoom;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;

    public SocketServer(int port, ChatRoom chatRoom) {
        this.port = port;
        this.chatRoom = chatRoom;
        // Pool de threads pour gérer les clients en parallèle
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() throws IOException{
        this.serverSocket = new ServerSocket(port);
        System.out.println("[TCP] Tchat server started on port " + port);

        try {
            while (!this.serverSocket.isClosed()) {
                var client = this.serverSocket.accept();
                threadPool.execute(new SocketClientHandler(client, chatRoom));
            }
        } catch (IOException e) {
            // Si le socket est fermé volontairement (via stop()), on ne relance pas l'exception
            // Sinon, c'est une erreur critique qu'on remonte au main
            if (serverSocket != null && !serverSocket.isClosed()) {
                throw new IOException("SERVER_ACCEPT_ERROR: Failure while accepting client connection", e);
            }
        }
        //stop();
    }

    public void stop() {
        try {
            if (this.serverSocket != null) serverSocket.close();
            threadPool.shutdown();
            if (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (IOException | InterruptedException e) {
            // On remonte une RuntimeException car stop() est souvent appelé dans des contextes
            // où on ne peut pas gérer une exception vérifiée (ex: Shutdown Hook)
            throw new RuntimeException("SERVER_STOP_ERROR: Failed to shutdown server cleanly", e);
        }
    }
}