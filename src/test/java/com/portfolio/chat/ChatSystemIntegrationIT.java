package com.portfolio.chat;

import com.portfolio.chat.domain.ChatRoom;
import com.portfolio.chat.infra.network.SocketServer;
import com.portfolio.chat.infra.web.AdminStatusServer;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@DisplayName("Tests d'Intégration - Système Complet (CI-Ready)")
class ChatSystemIntegrationIT {

    private static SocketServer chatServer;
    private static AdminStatusServer adminServer;
    private static int chatPort = 5000;
    private static int adminPort = 8080;

    @BeforeAll
    static void setup() throws IOException {
        var chatRoom = new ChatRoom();
        adminServer = new AdminStatusServer(chatRoom);
        adminServer.start(adminPort);

        chatServer = new SocketServer(chatPort, chatRoom);
        Thread t = new Thread(() -> {
            try { chatServer.start(); } catch (IOException ignored) {}
        });
        t.setDaemon(true);
        t.start();

        // Pause plus longue pour la CI
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
    }

    @AfterAll
    static void tearDown() {
        chatServer.stop();
        adminServer.stop();
    }

    @Test
    @DisplayName("Flux complet : Connexion, Commandes, Broadcast et Admin API")
    void fullFlowIntegrationTest() throws Exception {
        String host = "127.0.0.1";

        try (
                Socket aliceSocket = new Socket(host, chatPort);
                PrintWriter aliceOut = new PrintWriter(aliceSocket.getOutputStream(), true);
                BufferedReader aliceIn = new BufferedReader(new InputStreamReader(aliceSocket.getInputStream()));

                Socket bobSocket = new Socket(host, chatPort);
                PrintWriter bobOut = new PrintWriter(bobSocket.getOutputStream(), true);
                BufferedReader bobIn = new BufferedReader(new InputStreamReader(bobSocket.getInputStream()))
        ) {
            // --- 1. LOGIN ---
            assertEquals("Enter nickname :", aliceIn.readLine());
            aliceOut.println("Alice");
            assertEquals("Enter nickname :", bobIn.readLine());
            bobOut.println("Bob");

            // Laisser le temps au serveur de processer les entrées
            Thread.sleep(300);

            // --- 2. TEST COMMANDE /list ---
            aliceOut.println("/list");

            boolean listReceived = false;
            // On attend max 2 secondes pour la réponse
            long timeout = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < timeout) {
                if (aliceIn.ready()) {
                    String line = aliceIn.readLine();
                    if (line.contains("Online Users (2)")) {
                        listReceived = true;
                        break;
                    }
                }
                Thread.sleep(50);
            }
            assertTrue(listReceived, "Alice devrait avoir reçu la liste des utilisateurs");

            // --- 3. TEST BROADCAST ---
            aliceOut.println("Hello Bob!");

            // Lecture bloquante pour Bob (plus fiable que ready() sur CI)
            String receivedByBob = bobIn.readLine();
            assertTrue(receivedByBob.contains("Alice: Hello Bob!"), "Bob n'a pas reçu le message d'Alice");

            // --- 4. VÉRIFICATION ADMIN API (HTTP) ---
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + host + ":" + adminPort + "/status"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertAll("Vérification Admin Dashboard",
                    () -> assertEquals(200, response.statusCode()),
                    () -> assertTrue(response.body().contains("Utilisateurs en ligne : 2"), "Compteur faux"),
                    () -> assertTrue(response.body().contains("Alice"), "Alice absente du dashboard"),
                    () -> assertTrue(response.body().contains("Bob"), "Bob absent du dashboard")
            );

            // --- 5. DÉCONNEXION PROPRE ---
            aliceOut.println("/quit");
            bobOut.println("/quit");

            // Crucial : laisser le temps aux sockets de se fermer côté serveur
            // AVANT de sortir du bloc try-with-resources du test
            Thread.sleep(500);
        }
    }
}