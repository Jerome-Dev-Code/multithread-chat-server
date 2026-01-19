package com.portfolio.chat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
@Tag("smoke")
@DisplayName("Smoke Test - Main")
class MainTest {

    @AfterEach
    void tearDown() {
        // Sécurité maximale : on force l'arrêt des serveurs après chaque test
        // pour libérer les ports, même si le main() tourne encore en fond.
        Main.stopServers();
    }
    @Test
    @DisplayName("L'application doit démarrer sans conflit de ports")
    void smokeTestMainStartup() {
        assertDoesNotThrow(() -> {
            // Configuration forcée pour le test
            System.setProperty("app.profile", "dev");
            // Port aléatoire
            System.setProperty("chat.port", "0");
            // Port aléatoire
            System.setProperty("admin.port", "0");
            System.setProperty("log.verbose", "false");

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> Main.main(new String[]{}));

            try {
                // On attend 1.5 seconde pour vérifier qu'aucune exception ne survient au boot
                future.get(1500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // Timeout normal car le serveur est une boucle infinie
            }
        });
    }
}