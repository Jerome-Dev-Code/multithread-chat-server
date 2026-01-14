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
    @DisplayName("Le handler doit appeler broadcast avec l'expéditeur et le contenu séparés")
    void testUserMessageBroadcast() throws IOException {
        // Simulation : L'utilisateur choisit "Alice", envoie "Salut !", puis quitte
        String simulatedInput = "Alice\nSalut !\n/quit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
        when(mockSocket.getInputStream()).thenReturn(inputStream);

        SocketClientHandler handler = new SocketClientHandler(mockSocket, mockChatRoom);
        handler.run();

        // 1. Vérifie que l'utilisateur a rejoint
        verify(mockChatRoom).join(eq("Alice"), eq(handler));

        // 2. Vérifie l'appel à broadcast avec la nouvelle signature (sender, message)
        // C'est ici que la modification est importante
        verify(mockChatRoom).broadcast(eq("Alice"), eq("Salut !"));

        // 3. Vérifie le départ
        verify(mockChatRoom).leave(eq("Alice"));
    }

    @Test
    @DisplayName("Le handler ne doit pas broadcaster la commande /quit")
    void testQuitCommandHandling() throws IOException {
        String simulatedInput = "Alice\n/quit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
        when(mockSocket.getInputStream()).thenReturn(inputStream);

        SocketClientHandler handler = new SocketClientHandler(mockSocket, mockChatRoom);
        handler.run();

        // On vérifie que broadcast n'a jamais été appelé avec "/quit"
        verify(mockChatRoom, never()).broadcast(anyString(), eq("/quit"));
        verify(mockChatRoom).leave("Alice");
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

        // 2. On déclenche l'initialisation des flux manuellement
        // Cela va lier le PrintWriter à notre outputStream mocké dans le @BeforeEach
        // Grâce à setupStreams(), nous pouvons tester l'envoi de messages sans déclencher la boucle bloquante run().
        // Cela permet un test unitaire rapide et isolé.
        handler.setupStreams();

        // 3. Action
        String testMessage = "Test message";
        handler.sendMessage(testMessage);

        // 4. Vérification
        // On vérifie que le message est bien présent dans le flux de sortie capturé
        String capturedOutput = outputStream.toString();
        assertTrue(capturedOutput.contains(testMessage),"Le flux de sortie devrait contenir le message envoyé");
    }
}