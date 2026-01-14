package com.portfolio.chat.infra.web;

import com.portfolio.chat.core.MessageSender;
import com.portfolio.chat.domain.ChatRoom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tests de l'API Admin (HTTP + Observer)")
class AdminStatusServerTest {

    private ChatRoom chatRoom;
    private AdminStatusServer adminServer;
    private int realPort = 8081;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() throws IOException {
        chatRoom = new ChatRoom();
        adminServer = new AdminStatusServer(chatRoom);
        realPort = adminServer.start(0);
    }

    @AfterEach
    void tearDown() {
        if (adminServer != null) {
            adminServer.stop();
        }
    }

    @Test
    @DisplayName("L'API doit afficher le nombre correct de messages via l'Observer")
    void testMessageCounterViaObserver() throws IOException, InterruptedException {
        // 1. On simule l'envoi de messages dans la ChatRoom
        chatRoom.broadcast("Alice", "Premier message");
        chatRoom.broadcast("Bob", "Deuxième message");

        // 2. On interroge l'API Admin via HttpClient (Java 11)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + realPort + "/status"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // 3. Vérifications
        assertTrue(response.statusCode() == 200);
        assertTrue(response.body().contains("Total messages échangés depuis démarrage : 2"),
                "Le compteur de l'API Admin devrait être à 2");
    }

    @Test
    @DisplayName("L'API doit lister les utilisateurs connectés")
    void testUserListInStatus() throws IOException, InterruptedException {
        MessageSender mockSender = org.mockito.Mockito.mock(MessageSender.class);
        // On simule une connexion (on mock le sender car on ne teste pas le réseau ici)
        chatRoom.join("Alice", mockSender);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + realPort + "/status"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertTrue(response.body().contains("Alice"), "Alice devrait figurer dans le dashboard admin");
        assertTrue(response.body().contains("Utilisateurs en ligne : 1"));
    }
}