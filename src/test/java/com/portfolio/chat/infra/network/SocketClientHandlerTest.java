package com.portfolio.chat.infra.network;

import com.portfolio.chat.domain.ChatRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du SocketClientHandler")
class SocketClientHandlerTest {

    @Mock
    private Socket mockSocket;

    @Mock
    private ChatRoom mockChatRoom;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() throws IOException {
        // On prépare un flux de sortie pour capturer ce que le serveur envoie au client
        outputStream = new ByteArrayOutputStream();
        lenient().when(mockSocket.getOutputStream()).thenReturn(outputStream);
    }

    @Test
    @DisplayName("Le handler doit demander un pseudo et rejoindre la ChatRoom")
    void testUserJoinSequence() throws IOException {
        // Simulation de l'entrée utilisateur : "Alice" suivi d'un message, puis fermeture
        String simulatedInput = "Alice\nHello!\n/quit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
        when(mockSocket.getInputStream()).thenReturn(inputStream);

        SocketClientHandler handler = new SocketClientHandler(mockSocket, mockChatRoom);

        // On exécute le run() (dans le thread actuel pour le test)
        handler.run();

        // Vérifications
        // 1. A-t-il rejoint la salle avec le bon pseudo ?
        verify(mockChatRoom).join(eq("Alice"), eq(handler));

        // 2. A-t-il diffusé le message "Hello!" ?
        verify(mockChatRoom).broadcast(contains("Alice : Hello!"));

        // 3. A-t-il quitté la salle à la fin ?
        verify(mockChatRoom).leave(eq("Alice"));
    }

    @Test
    @DisplayName("Le handler doit fermer le socket en cas d'erreur de lecture")
    void testSocketCloseOnException() throws IOException {
        // On simule une erreur lors de la lecture du flux
        when(mockSocket.getInputStream()).thenThrow(new IOException("Simulated network error"));

        SocketClientHandler handler = new SocketClientHandler(mockSocket, mockChatRoom);
        handler.run();

        // Vérification que le socket est bien fermé même en cas d'erreur
        verify(mockSocket).close();
    }

    @Test
    @DisplayName("La méthode sendMessage doit écrire correctement sur le flux de sortie")
    void testSendMessage() throws IOException {
        SocketClientHandler handler = new SocketClientHandler(mockSocket, mockChatRoom);

        // On simule manuellement l'initialisation du flux de sortie (normalement fait dans run)
        // Pour ce test précis, on peut injecter le flux via une petite modification ou simuler le run partiellement
        // Ici, on vérifie si sendMessage écrit dans l'outputStream

        String testMessage = "Message from server";

        // Note: Dans ton code actuel, 'out' est initialisé dans run().
        // Pour tester sendMessage isolément, il faudrait que run() ait été appelé.

        // Simulation rapide du cycle de vie pour initialiser le PrintWriter 'out'
        String input = "Alice\n/quit\n";
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(input.getBytes()));

        handler.run();
        handler.sendMessage(testMessage);

        assertTrue(outputStream.toString().contains(testMessage));
    }
}