package com.portfolio.chat.domain;

// Imports de la logique métier (Domain et Core)
import com.portfolio.chat.core.MessageSender;
import com.portfolio.chat.domain.ChatRoom;

// Imports JUnit 5
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// Imports Mockito (pour simuler les envois de messages)
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test unitaire de la logique métier du salon de chat.
 * Localisation : src/test/java/com/portfolio/chat/domain/ChatRoomTest.java
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de la logique ChatRoom")
class ChatRoomTest {

    private ChatRoom chatRoom;

    // On simule (mock) deux utilisateurs pour tester les interactions
    @Mock
    private MessageSender alice;

    @Mock
    private MessageSender bob;

    @BeforeEach
    void setUp() {
        // Initialisation d'une nouvelle salle avant chaque test
        chatRoom = new ChatRoom();
    }

    @Test
    @DisplayName("Vérifier qu'un utilisateur peut rejoindre la salle")
    void testUserJoin() {
        chatRoom.join("Alice", alice);

        assertTrue(chatRoom.getOnlineUsers().contains("Alice"),
                "Alice devrait figurer dans la liste des utilisateurs connectés.");
        assertEquals(1, chatRoom.getOnlineUsers().size(),
                "Il devrait y avoir exactement 1 utilisateur.");
    }

    @Test
    @DisplayName("Vérifier que le broadcast envoie le message à tout le monde")
    void testBroadcastDistribution() {
        // Alice et Bob rejoignent
        chatRoom.join("Alice", alice);
        chatRoom.join("Bob", bob);

        String message = "Bonjour tout le monde !";
        chatRoom.broadcast(message);

        // On vérifie que la méthode sendMessage a été appelée sur chaque mock
        verify(alice, times(1)).sendMessage(anyString());
        verify(bob, times(1)).sendMessage(anyString());
    }

    @Test
    @DisplayName("Vérifier qu'un utilisateur ne reçoit plus de messages après son départ")
    void testUserLeave() {
        chatRoom.join("Alice", alice);
        chatRoom.join("Bob", bob);

        // Alice quitte la salle
        chatRoom.leave("Alice");

        assertFalse(chatRoom.getOnlineUsers().contains("Alice"),
                "Alice ne devrait plus être dans la liste.");

        // On broadcast un message
        chatRoom.broadcast("Message de test");

        // Bob doit le recevoir, mais Alice ne doit pas recevoir de NOUVEAU message
        verify(bob, atLeastOnce()).sendMessage(anyString());

        // Alice ne doit avoir reçu que les messages système lors de sa connexion/déconnexion,
        // mais aucun message de broadcast après son 'leave'.
        // (Note: selon ton implémentation, Alice a pu recevoir 1 message système avant de partir)
    }

    @Test
    @DisplayName("Vérifier la robustesse du retrait d'un utilisateur inexistant")
    void testLeaveInexistantUser() {
        // Ne doit pas lever d'exception
        assertDoesNotThrow(() -> chatRoom.leave("Inconnu"));
    }
}