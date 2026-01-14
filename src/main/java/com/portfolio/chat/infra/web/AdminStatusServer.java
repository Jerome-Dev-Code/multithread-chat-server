package com.portfolio.chat.infra.web;

import com.portfolio.chat.core.ChatObserver;
import com.portfolio.chat.domain.ChatRoom;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminStatusServer implements ChatObserver {
    private final ChatRoom chatRoom;
    private final AtomicInteger totalMessagesServed = new AtomicInteger(0);
    private HttpServer server;

    public AdminStatusServer(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
        // Le serveur s'enregistre lui-même comme observateur
        this.chatRoom.addObserver(this);
    }

    public int start(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        this.server.createContext("/status", exchange -> {
            var users = chatRoom.getOnlineUsers();
            var responseString = String.format(
                    "--- Chat Admin Dashboard ---\n" +
                            "Utilisateurs en ligne : %d\n" +
                            "Total messages échangés depuis démarrage : %d\n" +
                            "Liste : %s",
                    users.size(),
                    totalMessagesServed.get(),
                    String.join(", ", users)
            );
            // 1. Convertir d'abord en tableau d'octets UTF-8
            byte[] responseBytes = responseString.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // 2. Envoyer la taille réelle en OCTETS (pas en nombre de caractères)
            exchange.sendResponseHeaders(200, responseBytes.length);

            // 3. Écrire et fermer proprement
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
                // Force l'envoi avant la fermeture
                os.flush();
            }
        });

        this.server.start();
        System.out.println("[WEB] API Admin prête sur http://localhost:" + port + "/status");
        return this.server.getAddress().getPort();

    }

    /**
     * Arrête proprement le serveur HTTP.
     */
    public void stop() {
        if (this.server != null) {
            // Le paramètre '0' indique d'arrêter le serveur immédiatement
            // sans attendre que les échanges en cours se terminent.
            this.server.stop(0);
            System.out.println("[WEB] Serveur API Admin arrêté.");
        }
    }

    // --- Implémentation des méthodes de l'interface ChatObserver ---

    @Override
    public void onUserJoined(String username) {
        System.out.println("[MONITORING] Nouvel utilisateur : " + username);
    }

    @Override
    public void onUserLeft(String username) {
        System.out.println("[MONITORING] Départ de : " + username);
    }

    @Override
    public void onMessageSent(String sender, String message) {
        // On incrémente le compteur global à chaque message broadcasté
        totalMessagesServed.incrementAndGet();
    }
}