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
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

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
        this.mockChatRoom = mock(ChatRoom.class);
        this.mockSocket = mock(Socket.class);
        // On prépare un flux de sortie pour capturer ce que le serveur envoie au client
        this.outputStream = new ByteArrayOutputStream();
        lenient().when(this.mockSocket.getOutputStream()).thenReturn(this.outputStream);
    }

    @Test
    @DisplayName("Le handler doit appeler broadcast avec l'expéditeur et le contenu séparés")
    void testUserMessageBroadcast() throws IOException {
        // Simulation : L'utilisateur choisit "Alice", envoie "Salut !", puis quitte
        String simulatedInput = "Alice\nSalut !\n/quit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
        when(this.mockSocket.getInputStream()).thenReturn(inputStream);

        SocketClientHandler handler = new SocketClientHandler(this.mockSocket, this.mockChatRoom);
        handler.run();

        // 1. Vérifie que l'utilisateur a rejoint
        verify(this.mockChatRoom).join(eq("Alice"), eq(handler));

        // 2. Vérifie l'appel à broadcast avec la nouvelle signature (sender, message)
        // C'est ici que la modification est importante
        verify(this.mockChatRoom).broadcast(eq("Alice"), eq("Salut !"));

        // 3. Vérifie le départ
        verify(this.mockChatRoom).leave(eq("Alice"));
    }
    @Test
    @DisplayName("La méthode sendMessage doit écrire correctement sur le flux de sortie")
    void testSendMessage() throws IOException {
        SocketClientHandler handler = new SocketClientHandler(this.mockSocket, this.mockChatRoom);

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
        String capturedOutput = this.outputStream.toString();
        assertTrue(capturedOutput.contains(testMessage),"Le flux de sortie devrait contenir le message envoyé");
    }
    @Test
    @DisplayName("La commande /list doit retourner la liste formatée des utilisateurs")
    void shouldReturnUserListWhenListCommandIsReceived() throws IOException {
        // GIVEN: Deux utilisateurs en ligne
        List<String> fakeUsers = Arrays.asList("Alice", "Bob");
        when(this.mockChatRoom.getOnlineUsers()).thenReturn(fakeUsers);

        // Simulation de l'entrée utilisateur : "Alice\n/list\n/quit\n"
        // (On simule le pseudo, puis la commande, puis la sortie)
        String inputData = "Alice\n/list\n/quit\n";
        InputStream inputStream = new ByteArrayInputStream(inputData.getBytes());
        when(this.mockSocket.getInputStream()).thenReturn(inputStream);
        SocketClientHandler handler = new SocketClientHandler(this.mockSocket, this.mockChatRoom);
        // WHEN
        handler.run();
        // THEN
        String output = this.outputStream.toString();
        // On vérifie que la réponse contient les noms des utilisateurs
        assertTrue(output.contains("Alice"), "La réponse doit contenir Alice");
        assertTrue(output.contains("Bob"), "La réponse doit contenir Bob");
        assertTrue(output.contains("2"), "La réponse doit afficher le compte total");

        // On vérifie que la méthode du domaine a bien été appelée
        verify(this.mockChatRoom, atLeastOnce()).getOnlineUsers();
    }
    @Test
    @DisplayName("La commande /list doit gérer le cas d'une liste vide")
    void shouldHandleEmptyUserList() throws IOException {
        // GIVEN: Aucun utilisateur
        when(this.mockChatRoom.getOnlineUsers()).thenReturn(Arrays.asList());

        String inputData = "Alice\n/list\n/quit\n";
        when(this.mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(inputData.getBytes()));
        SocketClientHandler handler = new SocketClientHandler(this.mockSocket, this.mockChatRoom);
        // WHEN
        handler.run();
        // THEN
        assertTrue(this.outputStream.toString().contains("0") || this.outputStream.toString().contains("vide"));
    }
    @Test
    @DisplayName("Le handler ne doit pas broadcaster la commande /quit")
    void testQuitCommandHandling() throws IOException {
        String simulatedInput = "Alice\n/quit\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(simulatedInput.getBytes());
        when(this.mockSocket.getInputStream()).thenReturn(inputStream);

        SocketClientHandler handler = new SocketClientHandler(this.mockSocket, this.mockChatRoom);
        handler.run();

        // On vérifie que broadcast n'a jamais été appelé avec "/quit"
        verify(this.mockChatRoom, never()).broadcast(anyString(), eq("/quit"));
        verify(this.mockChatRoom).leave("Alice");
    }
    @Test
    @DisplayName("Le handler doit fermer le socket en cas d'erreur de lecture")
    void testSocketCloseOnException() throws IOException {
        // On simule une erreur lors de la lecture du flux
        when(this.mockSocket.getInputStream()).thenThrow(new IOException("Simulated network error"));

        SocketClientHandler handler = new SocketClientHandler(this.mockSocket, this.mockChatRoom);
        handler.run();

        // Vérification que le socket est bien fermé même en cas d'erreur
        verify(this.mockSocket).close();
    }
}