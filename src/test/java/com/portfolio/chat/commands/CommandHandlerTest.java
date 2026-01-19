package com.portfolio.chat.commands;

import com.portfolio.chat.domain.ChatRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du CommandHandler")
class CommandHandlerTest {

    private CommandHandler commandHandler;

    @Mock
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        commandHandler = new CommandHandler();
    }

    @Test
    @DisplayName("Devrait reconnaître et exécuter la commande /quit")
    void testHandleQuitCommand() {
        String input = "/quit";
        String user = "Alice";

        boolean handled = commandHandler.handle(input, user, chatRoom);

        // On vérifie que la commande a été traitée
        assertTrue(handled, "La commande /quit devrait être reconnue");

        // On vérifie que la logique métier correspondante a été appelée
        verify(chatRoom, times(1)).leave(user);
    }

    @Test
    @DisplayName("Devrait ignorer les messages normaux (ne commençant pas par /)")
    void testHandleNormalMessage() {
        String input = "Bonjour à tous";
        String user = "Alice";

        boolean handled = commandHandler.handle(input, user, chatRoom);

        // Un message normal n'est pas une commande
        assertFalse(handled, "Un message normal ne devrait pas être traité par le CommandHandler");

        // On vérifie qu'aucune action n'a été effectuée sur la chatRoom
        verifyNoInteractions(chatRoom);
    }

    @Test
    @DisplayName("Devrait renvoyer true pour une commande inconnue commençant par /")
    void testUnknownCommand() {
        String input = "/invalidCommand";

        // Dans notre implémentation, si ça commence par /, le handler "consomme" l'input
        // mais ne trouve rien à exécuter.
        boolean handled = commandHandler.handle(input, "Alice", chatRoom);

        assertTrue(handled, "Une chaîne commençant par / doit être interceptée comme une tentative de commande");
        verifyNoInteractions(chatRoom);
    }

    @Test
    @DisplayName("Devrait être insensible à la casse pour les commandes")
    void testCaseInsensitivity() {
        String input = "/QUIT";

        boolean handled = commandHandler.handle(input, "Alice", chatRoom);

        assertTrue(handled);
        verify(chatRoom, times(1)).leave("Alice");
    }
}
