package com.pitso.ui;

import com.pitso.ui.api.LibraryApiClient;
import com.pitso.ui.view.MainFrame;
import com.pitso.ui.view.Ui;

import javax.swing.*;

public final class LibraryDesktopApplication {
    private static final String DEFAULT_API = "https://library-intelligence-api.vercel.app";

    private LibraryDesktopApplication() {}

    public static void main(String[] args) {
        String baseUrl = resolveBaseUrl(args);
        SwingUtilities.invokeLater(() -> {
            Ui.installLookAndFeel();
            LibraryApiClient api = new LibraryApiClient(baseUrl);
            MainFrame frame = new MainFrame(api);
            frame.setVisible(true);
        });
    }

    private static String resolveBaseUrl(String[] args) {
        String systemProperty = System.getProperty("library.api.url");
        if (systemProperty != null && !systemProperty.isBlank()) return systemProperty;

        String env = System.getenv("LIBRARY_API_URL");
        if (env != null && !env.isBlank()) return env;

        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) return args[0];
        return DEFAULT_API;
    }
}
