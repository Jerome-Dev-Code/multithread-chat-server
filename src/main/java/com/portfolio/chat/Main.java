package com.portfolio.chat;

import com.portfolio.chat.domain.ChatRoom;
import com.portfolio.chat.infra.network.SocketServer;
import com.portfolio.chat.infra.web.AdminStatusServer;
import com.portfolio.chat.core.ChatObserver;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    // On expose les serveurs pour permettre un arrêt externe (notamment pour les tests)
    static SocketServer chatServer;
    static AdminStatusServer adminServer;
    public static void main(String[] args) {
        Properties config = loadFullConfiguration();

        // LOGIQUE HYBRIDE : On cherche dans System.getProperty d'abord (utile pour les tests ou Docker)
        // Sinon on prend la valeur du fichier properties.
        int chatPort = Integer.parseInt(System.getProperty("chat.port", config.getProperty("chat.port")));
        int adminPort = Integer.parseInt(System.getProperty("admin.port", config.getProperty("admin.port")));
        boolean isVerbose = Boolean.parseBoolean(System.getProperty("log.verbose", config.getProperty("log.verbose")));

        System.out.println("=== " + config.getProperty("app.name") + " v" + config.getProperty("app.version") + " ===");
        System.out.println("=== Profil : " + config.getProperty("app.active.profile").toUpperCase() + " ===");
        System.out.println("=== Ports : Chat=" + (chatPort == 0 ? "AUTO" : chatPort) +
                ", Admin=" + (adminPort == 0 ? "AUTO" : adminPort) + " ===");

        ChatRoom chatRoom = new ChatRoom();
        if (isVerbose) setupDebugLogging(chatRoom);

        adminServer = new AdminStatusServer(chatRoom);
        chatServer = new SocketServer(chatPort, chatRoom);

        try {
            adminServer.start(adminPort);

            Runtime.getRuntime().addShutdownHook(new Thread(Main::stopServers));

            chatServer.start();
        } catch (IOException e) {
            System.err.println("Erreur fatale : " + e.getMessage());
            System.exit(1);
        }
    }

    // Méthode utilitaire pour l'arrêt propre
    static void stopServers() {
        if (chatServer != null) chatServer.stop();
        if (adminServer != null) adminServer.stop();
        System.out.println("=== Serveurs arrêtés proprement ===");
    }

    private static Properties loadFullConfiguration() {
        Properties props = new Properties();
        try {
            loadFromClasspath(props, "application.properties");
            String activeProfile = System.getProperty("app.profile", props.getProperty("app.active.profile", "dev"));
            loadFromClasspath(props, "application-" + activeProfile + ".properties");
            props.setProperty("app.active.profile", activeProfile);
        } catch (IOException e) {
            throw new RuntimeException("Erreur de config", e);
        }
        return props;
    }

    private static void loadFromClasspath(Properties props, String fileName) throws IOException {
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is != null) props.load(is);
        }
    }

    private static void setupDebugLogging(ChatRoom chatRoom) {
        chatRoom.addObserver(new ChatObserver() {
            @Override
            public void onUserJoined(String n) { System.out.println("[DEBUG] + " + n); }
            @Override
            public void onUserLeft(String n) { System.out.println("[DEBUG] - " + n); }
            @Override
            public void onMessageSent(String s, String m) { System.out.println("[DEBUG] " + s + ": " + m); }
        });
    }
}