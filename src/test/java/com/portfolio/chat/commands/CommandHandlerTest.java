package com.portfolio.chat.commands;

import com.portfolio.chat.core.MessageSender;
import com.portfolio.chat.domain.ChatRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du CommandHandler")
class CommandHandlerTest {

    private CommandHandler commandHandler;
    @Mock
    private ChatRoom chatRoom;
    @Mock
    private MessageSender messageSender;
    @Mock
    private Runnable quitAction;

    @BeforeEach
    void setUp() {
        commandHandler = new CommandHandler();

        // On enregistre les commandes réelles avec des mocks pour leurs dépendances
        // Cela reflète exactement ce qui se passe dans SocketClientHandler
        commandHandler.register(new ListCommand(chatRoom, messageSender));
        commandHandler.register(new QuitCommand(quitAction));
    }
    @Test
    @DisplayName("Devrait reconnaître et exécuter la commande /quit")
    void testHandleQuitCommand() {
        String input = "/quit";

        boolean handled = commandHandler.handle(input);

        // Vérification du retour
        assertTrue(handled, "La commande /quit devrait être reconnue");

        // On vérifie que le callback de déconnexion a été appelé
        verify(quitAction, times(1)).run();
    }

    @Test
    @DisplayName("Devrait reconnaître et exécuter la commande /list")
    void testHandleListCommand() {
        String input = "/list";
        when(chatRoom.getOnlineUsers()).thenReturn(List.of("Alice", "Bob"));

        boolean handled = commandHandler.handle(input);

        assertTrue(handled, "La commande /list devrait être reconnue");
        verify(messageSender).sendMessage(contains("Online Users (2)"));
    }

    @Test
    @DisplayName("Devrait ignorer les messages normaux (ne commençant pas par /)")
    void testHandleNormalMessage() {
        String input = "Bonjour à tous";

        boolean handled = commandHandler.handle(input);

        // Un message normal n'est pas une commande, le handler doit retourner false
        assertFalse(handled, "Un message normal ne devrait pas être traité par le CommandHandler");

        // On vérifie qu'aucune action n'a été déclenchée
        verifyNoInteractions(chatRoom, messageSender, quitAction);
    }

    @Test
    @DisplayName("Devrait renvoyer false pour une commande inconnue")
    void testUnknownCommand() {
        String input = "/invalidCommand";

        // Dans la nouvelle implémentation, si la commande n'est pas enregistrée,
        // handle retourne false (via .orElse(false))
        boolean handled = commandHandler.handle(input);

        assertFalse(handled, "Une commande non enregistrée doit retourner false");
        verifyNoInteractions(chatRoom, messageSender, quitAction);
    }

    @Test
    @DisplayName("Devrait être insensible à la casse pour les commandes")
    void testCaseInsensitivity() {
        String input = "/QUIT";

        boolean handled = commandHandler.handle(input);

        assertTrue(handled);
        verify(quitAction, times(1)).run();
    }

    @Test
    @DisplayName("Devrait gérer correctement les arguments (même si ignorés)")
    void testHandleWithArguments() {
        // On teste que /quit avec des arguments fonctionne toujours
        String input = "/quit force";

        boolean handled = commandHandler.handle(input);

        assertTrue(handled);
        verify(quitAction).run();
    }
}