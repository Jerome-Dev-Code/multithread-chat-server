package com.portfolio.chat.domain;

import com.portfolio.chat.core.ChatObserver;
import com.portfolio.chat.core.MessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de ChatRoom (Version Observateur & Signature Broadcast)")
class ChatRoomTest {

    private ChatRoom chatRoom;

    @Mock
    private MessageSender alice;

    @Mock
    private MessageSender bob;

    @Mock
    private ChatObserver chatObserver;

    @BeforeEach
    void setUp() {
        chatRoom = new ChatRoom();
        chatRoom.addObserver(chatObserver);
    }

    @Test
    @DisplayName("Vérifier la notification de l'observateur lors d'un join")
    void testJoinNotification() {
        chatRoom.join("Alice", alice);

        // Vérifie que l'observateur est prévenu
        verify(chatObserver, times(1)).onUserJoined("Alice");
    }

    @Test
    @DisplayName("Vérifier le broadcast complet (utilisateurs + observateur)")
    void testBroadcastLogic() {
        chatRoom.join("Alice", alice);
        chatRoom.join("Bob", bob);

        // Reset des mocks pour ignorer les messages système de bienvenue
        clearInvocations(alice, bob, chatObserver);

        String sender = "Alice";
        String content = "Bonjour à tous !";

        chatRoom.broadcast(sender, content);

        // 1. Vérifie que les destinataires reçoivent le message formaté
        String expectedFormatted = "Alice : Bonjour à tous !";
        verify(alice, times(1)).sendMessage(expectedFormatted);
        verify(bob, times(1)).sendMessage(expectedFormatted);

        // 2. Vérifie que l'observateur reçoit les données brutes
        verify(chatObserver, times(1)).onMessageSent(sender, content);
    }

    @Test
    @DisplayName("Vérifier que les messages SYSTEM sont correctement gérés")
    void testSystemBroadcast() {
        chatRoom.join("Alice", alice);

        String systemContent = "Maintenance dans 5 minutes";
        chatRoom.broadcast("SYSTEM", systemContent);

        // Vérifie le formatage spécifique au système
        verify(alice).sendMessage("[SYSTEM] " + systemContent);

        // L'observateur reçoit toujours les données pures
        verify(chatObserver).onMessageSent("SYSTEM", systemContent);
    }

    @Test
    @DisplayName("Vérifier la notification lors d'un leave")
    void testLeaveNotification() {
        chatRoom.join("Alice", alice);

        // On nettoie les interactions liées au "join" pour y voir clair
        clearInvocations(chatObserver);

        chatRoom.leave("Alice");

        // Vérifie que l'observateur est prévenu du départ
        verify(chatObserver, times(1)).onUserLeft("Alice");

        // Vérifie qu'un message SYSTEM de départ a été broadcasté
        verify(chatObserver).onMessageSent(eq("SYSTEM"), contains("Alice has left room."));
    }

    @Test
    @DisplayName("Vérifier la robustesse avec plusieurs observateurs")
    void testMultipleObservers() {
        ChatObserver secondaryObserver = mock(ChatObserver.class);
        chatRoom.addObserver(secondaryObserver);

        chatRoom.join("Bob", bob);

        verify(chatObserver).onUserJoined("Bob");
        verify(secondaryObserver).onUserJoined("Bob");
    }
}