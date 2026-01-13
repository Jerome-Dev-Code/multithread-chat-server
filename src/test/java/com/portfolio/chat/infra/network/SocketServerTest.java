package com.portfolio.chat.infra.network;

import com.portfolio.chat.domain.ChatRoom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du SocketServer (Intégration)")
class SocketServerTest {

    @Mock
    private ChatRoom chatRoom;

    private SocketServer socketServer;

    @AfterEach
    void tearDown() {
        if (socketServer != null) {
            socketServer.stop();
        }
    }

    @Test
    @DisplayName("Le serveur doit accepter une connexion client sur un port donné")
    void testServerAcceptsConnection() throws Exception {
        // Utilisation du port 0 pour laisser l'OS choisir un port libre
        int testPort = 5555;
        socketServer = new SocketServer(testPort, chatRoom);

        // On lance le serveur dans un thread séparé car start() est bloquant
        CompletableFuture.runAsync(() -> socketServer.start());

        // On laisse un court instant au serveur pour s'initialiser
        TimeUnit.MILLISECONDS.sleep(200);

        // On tente de se connecter avec un vrai socket client
        try (Socket clientSocket = new Socket("localhost", testPort)) {
            assertTrue(clientSocket.isConnected(), "Client should be connected to server");
        }
    }

    @Test
    @DisplayName("Le serveur ne doit pas lever d'exception lors d'un arrêt propre")
    void testServerStop() {
        socketServer = new SocketServer(5556, chatRoom);

        // Lancement asynchrone
        CompletableFuture.runAsync(() -> socketServer.start());

        // On vérifie que l'arrêt ne provoque pas de crash
        assertDoesNotThrow(() -> {
            TimeUnit.MILLISECONDS.sleep(100);
            socketServer.stop();
        });
    }
}