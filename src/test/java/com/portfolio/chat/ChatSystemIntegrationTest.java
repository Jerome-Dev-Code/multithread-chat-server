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

@DisplayName("Tests d'Intégration - Système Complet")
class ChatSystemIntegrationTest {

    private static SocketServer chatServer;
    private static AdminStatusServer adminServer;
    private static int chatPort;
    private static int adminPort;
    private static ChatRoom chatRoom;

    @BeforeAll
    static void setup() throws IOException {
        chatRoom = new ChatRoom();
        // 1. On trouve des ports libres AVANT de créer les serveurs
        chatPort = findFreePort();
        adminPort = findFreePort();

        // 2. On instancie les serveurs avec ces ports réservés
        adminServer = new AdminStatusServer(chatRoom);
        adminServer.start(adminPort);

        chatServer = new SocketServer(chatPort, chatRoom);

        // Lancement du serveur de chat dans un thread séparé
        Thread serverThread = new Thread(() -> {
            try {
                chatServer.start();
            } catch (IOException ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // On laisse un court instant pour le démarrage
        try {
            Thread.sleep(500);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    @AfterAll
    static void tearDown() {
        chatServer.stop();
        adminServer.stop();
    }

    @Test
    @DisplayName("Scénario complet : Connexion TCP et vérification via API HTTP")
    void fullFlowIntegrationTest() throws Exception {
        // 1. Connexion d'Alice (Client TCP)
        Socket aliceSocket = new Socket("localhost", chatPort);
        PrintWriter aliceOut = new PrintWriter(aliceSocket.getOutputStream(), true);
        BufferedReader aliceIn = new BufferedReader(new InputStreamReader(aliceSocket.getInputStream()));

        aliceOut.println("Alice"); // Envoi du pseudo
        aliceIn.readLine();        // Lecture du message de bienvenue

        // 2. Connexion de Bob (Client TCP)
        Socket bobSocket = new Socket("localhost", chatPort);
        PrintWriter bobOut = new PrintWriter(bobSocket.getOutputStream(), true);
        BufferedReader bobIn = new BufferedReader(new InputStreamReader(bobSocket.getInputStream()));

        bobOut.println("Bob");
        bobIn.readLine();

        // 3. Alice envoie un message
        aliceOut.println("Hello Bob!");
        String receivedByBob = bobIn.readLine();
        assertTrue(receivedByBob.contains("Alice: Hello Bob!"));

        // 4. Vérification via l'API Admin (Client HTTP)
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + adminPort + "/status"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 5. Assertions sur l'état global
        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"userCount\":2"));
        assertTrue(body.contains("Alice"));
        assertTrue(body.contains("Bob"));

        // Nettoyage
        aliceSocket.close();
        bobSocket.close();
    }
    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}