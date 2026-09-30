package com.ggar.stvr.packages.streamlink.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Message resolver for internationalized strings in the Streamlink package.
 */
public final class StreamlinkMessages {

    private static final String BUNDLE_BASE_NAME = "i18n.streamlink-messages";

    private StreamlinkMessages() {}

    /**
     * Resolves and formats a message by key using the specified locale.
     *
     * @param key message key in properties bundle
     * @param locale target locale (defaults to Locale.ENGLISH if null)
     * @param args optional arguments for MessageFormat
     * @return formatted message string
     */
    public static String get(String key, Locale locale, Object... args) {
        Locale targetLocale = locale != null ? locale : Locale.ENGLISH;
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, targetLocale);
            if (bundle.containsKey(key)) {
                String pattern = bundle.getString(key);
                if (args != null && args.length > 0) {
                    return MessageFormat.format(pattern, args);
                }
                return pattern;
            }
        } catch (Exception ignored) {
            // Fallback to key if bundle lookup fails
        }
        return key;
    }

    /**
     * Resolves and formats a message using English as default.
     *
     * @param key message key
     * @param args optional arguments
     * @return formatted message string
     */
    public static String get(String key, Object... args) {
        return get(key, Locale.ENGLISH, args);
    }
}
