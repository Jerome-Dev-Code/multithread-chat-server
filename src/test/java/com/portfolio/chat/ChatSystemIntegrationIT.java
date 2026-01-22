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
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@DisplayName("Tests d'Intégration - Système Complet (CI-Ready)")
class ChatSystemIntegrationIT {

    private static SocketServer chatServer;
    private static AdminStatusServer adminServer;
    private static int chatPort;
    private static int adminPort;

    @BeforeAll
    static void setup() throws IOException {
        var chatRoom = new ChatRoom();
        // 1. On trouve des ports libres AVANT de créer les serveurs
        chatPort = findFreePort();
        adminPort = findFreePort();

        // 2. On instancie les serveurs avec ces ports réservés
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

            String receivedByBob = null;
            long msgTimeout = System.currentTimeMillis() + 3000; // 3 secondes de marge pour la CI

            while (System.currentTimeMillis() < msgTimeout) {
                if (bobIn.ready()) {
                    receivedByBob = bobIn.readLine();
                    if (receivedByBob != null && receivedByBob.contains("Alice: Hello Bob!")) {
                        break;
                    }
                }
                Thread.sleep(100);
            }
            assertNotNull(receivedByBob, "Bob n'a absolument rien reçu");
            assertTrue(receivedByBob.contains("Alice: Hello Bob!"), "Le message reçu par Bob est incorrect : " + receivedByBob);

            // --- 4. VÉRIFICATION ADMIN API (HTTP) ---
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            // 5. Vérification via l'API Admin (Client HTTP)
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + host + ":" + adminPort + "/status"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            System.out.println("Admin API Response: " + body); // Pour le debug en cas d'échec

            // Assertions adaptées au format "Admin Dashboard"
            assertAll("Vérification du Dashboard Admin",
                () -> assertEquals(200, response.statusCode(), "Le statut HTTP doit être 200"),

                // Vérification du titre
                () -> assertTrue(body.contains("--- Chat Admin Dashboard ---"), "Titre du dashboard manquant"),

                // Vérification du compteur (on cherche le libellé exact + le chiffre)
                () -> assertTrue(body.contains("Utilisateurs en ligne : 2"),
                        "Le compteur devrait afficher 2. Reçu : " + body),

                // Vérification de la liste des utilisateurs
                () -> assertTrue(body.contains("Alice"), "Alice doit être dans la liste"),
                () -> assertTrue(body.contains("Bob"), "Bob doit être dans la liste")
            );

            // 5. DÉCONNEXION PROPRE (Le fameux délai de 50ms)
            aliceOut.println("/quit");
            bobOut.println("/quit");

            // Crucial : laisser le temps aux sockets de se fermer côté serveur
            // AVANT de sortir du bloc try-with-resources du test
            Thread.sleep(500);
        }
    }
    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}