package com.portfolio.chat.infra.network;

import com.portfolio.chat.domain.ChatRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du SocketClientHandler")
class SocketClientHandlerTest {

    @Mock
    private Socket mockSocket;
    @Mock
    private ChatRoom mockChatRoom;
    private ByteArrayOutputStream outputStream;
    private SocketClientHandler handler;

    @BeforeEach
    void setUp() throws IOException {
        this.outputStream = new ByteArrayOutputStream();
        // Initialisation du comportement par défaut pour les flux
        lenient().when(this.mockSocket.getOutputStream()).thenReturn(this.outputStream);

        // Initialisation de l'objet à tester
        handler = new SocketClientHandler(this.mockSocket, this.mockChatRoom);
    }
    @Test
    @DisplayName("Le handler doit broadcaster les messages normaux et déléguer les commandes")
    void testUserMessageBroadcast() throws IOException {
        // Alice entre son nom, envoie un message, puis quitte
        String simulatedInput = "Alice\nSalut !\n/quit\n";
        when(this.mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(simulatedInput.getBytes()));

        handler.run();

        // 1. Vérifie la jonction
        verify(this.mockChatRoom).join(eq("Alice"), eq(handler));

        // 2. Vérifie que le message normal est broadcasté
        verify(this.mockChatRoom).broadcast(eq("Alice"), eq("Salut !"));

        // 3. Vérifie que le /quit a bien provoqué le départ (via QuitCommand)
        verify(this.mockChatRoom).leave(eq("Alice"));
    }
    @Test
    @DisplayName("La commande /list doit être traitée par la ListCommand via le handler")
    void shouldReturnUserListWhenListCommandIsReceived() throws IOException {
        // GIVEN: Liste d'utilisateurs
        when(this.mockChatRoom.getOnlineUsers()).thenReturn(Arrays.asList("Alice", "Bob"));
        String inputData = "Alice\n/list\n/quit\n";
        when(this.mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(inputData.getBytes()));

        // WHEN
        handler.run();

        // THEN
        String output = this.outputStream.toString();
        // On vérifie le formatage défini dans ListCommand
        assertTrue(output.contains("Online Users (2)"), "Le header de liste est manquant");
        assertTrue(output.contains("- Alice"), "Alice devrait figurer dans la liste");
        assertTrue(output.contains("- Bob"), "Bob devrait figurer dans la liste");
    }

    @Test
    @DisplayName("Le handler ne doit jamais broadcaster une commande")
    void testNoBroadcastForCommands() throws IOException {
        String simulatedInput = "Alice\n/list\n/quit\n";
        when(this.mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(simulatedInput.getBytes()));

        handler.run();

        // On vérifie que broadcast n'a JAMAIS été appelé avec une commande
        verify(this.mockChatRoom, never()).broadcast(anyString(), contains("/"));
    }

    @Test
    @DisplayName("Le handler doit fermer proprement le socket après une erreur fatale")
    void testSocketCloseOnException() throws IOException {
        // Simulation d'une rupture de flux au milieu du run
        when(this.mockSocket.getInputStream()).thenThrow(new IOException("Fatal crash"));

        assertThrows(RuntimeException.class, () -> handler.run());

        // Vérifie que disconnect() a bien été appelé dans le 'finally'
        verify(this.mockSocket, atLeastOnce()).close();
    }

    @Test
    @DisplayName("Vérifie que sendMessage écrit physiquement sur le flux réseau")
    void testSendMessage() throws IOException {
        // On prépare les flux manuellement pour ce test granulaire
        handler.setupStreams();

        handler.sendMessage("Hello World");

        // On vérifie que le contenu a bien été écrit dans notre ByteArrayOutputStream
        assertTrue(this.outputStream.toString().contains("Hello World"));
    }
}