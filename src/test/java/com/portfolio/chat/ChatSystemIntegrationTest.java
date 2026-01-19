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
        try(
            // 1. Connexion d'Alice (Client TCP)
            Socket aliceSocket = new Socket("localhost", chatPort);
            PrintWriter aliceOut = new PrintWriter(aliceSocket.getOutputStream(), true);
            BufferedReader aliceIn = new BufferedReader(new InputStreamReader(aliceSocket.getInputStream()));

            var bobSocket = new Socket("localhost", chatPort);
            var bobOut = new PrintWriter(bobSocket.getOutputStream(), true);
            var bobIn = new BufferedReader(new InputStreamReader(bobSocket.getInputStream()))
            ){
            aliceOut.println("Alice"); // Envoi du pseudo
            aliceIn.readLine();        // Lecture du message de bienvenue
            // Note : Alice reçoit aussi "SYSTEM: User Alice joined", il faut le lire ou l'ignorer
            aliceIn.readLine();

            bobOut.println("Bob");
            bobIn.readLine();
            bobIn.readLine();

            // 3. Alice envoie un message
            aliceIn.readLine();
            aliceOut.println("Hello Bob!");

            // 4. Utilisation d'une attente explicite ou lecture propre
            // On peut ajouter un petit délai de sécurité si nécessaire
            Thread.sleep(50);
            String receivedByBob = bobIn.readLine();

            assertNotNull(receivedByBob, "Bob should have received a message");
            assertTrue(receivedByBob.contains("Alice") && receivedByBob.contains("Hello Bob!"),
                    "Expected message from Alice but got: " + receivedByBob);

            // 5. Vérification via l'API Admin (Client HTTP)
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + adminPort + "/status"))
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

            //  7. Nettoyage
            aliceOut.println("/quit");
            bobOut.println("/quit");

            aliceSocket.shutdownOutput();
            bobSocket.shutdownOutput();

            // PAUSE TECHNIQUE : Laisser le temps aux threads de mettre à jour la ChatRoom
            Thread.sleep(200);
        }
    }
    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}