package com.hercey.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Env {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)(?::-(.*?))?}");
    private static final Properties APP_PROPERTIES = new Properties();
    private static final Map<String, String> DOTENV = new HashMap<>();

    static {

        loadDotEnv();

        try {

            InputStream inputStream = Env.class.getClassLoader().getResourceAsStream("app.properties");
            if (inputStream == null) {
                throw new IllegalStateException("app.properties not found on the classpath");
            }
            APP_PROPERTIES.load(inputStream);

        } catch (IOException e) {
            throw new RuntimeException("Application properties loading failed: " + e.getMessage());
        }

        for (String key : APP_PROPERTIES.stringPropertyNames()) {
            APP_PROPERTIES.setProperty(key, resolve(APP_PROPERTIES.getProperty(key)));
        }

    }

    private static void loadDotEnv() {

        Path dir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();

        for (int depth = 0; depth < 5 && dir != null; depth++) {
            Path candidate = dir.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                parseDotEnv(candidate);
                return;
            }
            dir = dir.getParent();
        }

    }

    private static void parseDotEnv(Path file) {

        try {

            for (String line : Files.readAllLines(file)) {

                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();

                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }

                DOTENV.put(key, value);

            }

        } catch (IOException e) {
            System.err.println("WARNING: could not read " + file + ": " + e.getMessage());
        }

    }

    public static String lookup(String key) {

        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }

        value = System.getenv(key);
        if (value != null) {
            return value;
        }

        return DOTENV.get(key);

    }

    public static String resolve(String value) {

        if (value == null) {
            return null;
        }

        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {

            String key = matcher.group(1);
            String fallback = matcher.group(2);
            String replacement = lookup(key);

            if (replacement == null) {
                if (fallback != null) {
                    replacement = fallback;
                } else {
                    replacement = matcher.group(0);
                    System.err.println("WARNING: unresolved configuration variable ${" + key + "}");
                }
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));

        }

        matcher.appendTail(result);
        return result.toString();

    }

    public static String get(String key) {
        return APP_PROPERTIES.getProperty(key);
    }

    public static Properties getAppProperties() {
        return APP_PROPERTIES;
    }

}
