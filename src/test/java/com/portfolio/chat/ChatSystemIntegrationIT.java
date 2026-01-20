package com.portfolio.chat;

import com.portfolio.chat.domain.ChatRoom;
import com.portfolio.chat.infra.network.SocketServer;
import com.portfolio.chat.infra.web.AdminStatusServer;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@DisplayName("Tests d'Intégration - Système Complet")
class ChatSystemIntegrationIT {

    private static SocketServer chatServer;
    private static AdminStatusServer adminServer;
    private static int chatPort;
    private static int adminPort;

    @BeforeAll
    static void setup() throws IOException {
        var chatRoom = new ChatRoom();
        chatPort = findFreePort();
        adminPort = findFreePort();

        adminServer = new AdminStatusServer(chatRoom);
        adminServer.start(adminPort);

        chatServer = new SocketServer(chatPort, chatRoom);
        Thread serverThread = new Thread(() -> {
            try { chatServer.start(); } catch (IOException ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();

        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
    }

    @AfterAll
    static void tearDown() {
        chatServer.stop();
        adminServer.stop();
    }

    @Test
    @DisplayName("Scénario complet : Login, Commandes, Messaging et Admin API")
    void fullFlowIntegrationTest() throws Exception {
        try (
                Socket aliceSocket = new Socket("localhost", chatPort);
                PrintWriter aliceOut = new PrintWriter(aliceSocket.getOutputStream(), true);
                BufferedReader aliceIn = new BufferedReader(new InputStreamReader(aliceSocket.getInputStream()));

                Socket bobSocket = new Socket("localhost", chatPort);
                PrintWriter bobOut = new PrintWriter(bobSocket.getOutputStream(), true);
                BufferedReader bobIn = new BufferedReader(new InputStreamReader(bobSocket.getInputStream()))
        ) {
            // 1. PHASE DE CONNEXION (Synchronisée avec SocketClientHandler)
            assertEquals("Enter nickname :", aliceIn.readLine());
            aliceOut.println("Alice");

            assertEquals("Enter nickname :", bobIn.readLine());
            bobOut.println("Bob");

            // On vide les messages système de bienvenue ("Alice joined", etc.)
            Thread.sleep(100);
            while(aliceIn.ready()) aliceIn.readLine();
            while(bobIn.ready()) bobIn.readLine();

            // 2. TEST DU COMMAND PATTERN (/list)
            aliceOut.println("/list");
            Thread.sleep(50); // Petit délai réseau simulé

            boolean foundListHeader = false;
            String line;
            // On cherche le header défini dans ListCommand
            while (aliceIn.ready() && (line = aliceIn.readLine()) != null) {
                if (line.contains("Online Users (2)")) foundListHeader = true;
            }
            assertTrue(foundListHeader, "Alice devrait pouvoir exécuter /list et voir 2 utilisateurs");

            // 3. MESSAGERIE CLASSIQUE
            aliceOut.println("Hello Bob!");
            Thread.sleep(100);

            String receivedByBob = bobIn.readLine();
            assertNotNull(receivedByBob);
            assertTrue(receivedByBob.contains("Alice: Hello Bob!"));

            // 4. VÉRIFICATION API ADMIN (HTTP)
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + adminPort + "/status"))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertAll("Vérification Admin",
                    () -> assertEquals(200, response.statusCode()),
                    () -> assertTrue(response.body().contains("Utilisateurs en ligne : 2")),
                    () -> assertTrue(response.body().contains("Alice"))
            );

            // 5. DÉCONNEXION PROPRE (Le fameux délai de 50ms)
            aliceOut.println("/quit");
            bobOut.println("/quit");

            // Délai pour laisser le flag 'connected' passer à false côté serveur
            Thread.sleep(100);
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}