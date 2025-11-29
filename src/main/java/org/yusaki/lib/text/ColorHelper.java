package org.yusaki.lib.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility responsible for normalizing colour codes across legacy ampersand formats and MiniMessage tags.
 *
 * <p>Inputs may freely mix legacy formatting codes (e.g. {@code &a}, {@code &l}, {@code &x&0&0&0&0&0&0})
 * with MiniMessage tags (e.g. {@code <bold>}, {@code <rainbow>}). The helper converts legacy tokens into
 * MiniMessage equivalents before parsing so mixed strings are handled consistently. Callers may retrieve
 * either an Adventure {@link Component} or a legacy serialised string using section sign codes.</p>
 */
public final class ColorHelper {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.standard())
            .build();

    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    private static final Map<Character, String> LEGACY_TO_MINI;

    static {
        LEGACY_TO_MINI = new HashMap<>();
        LEGACY_TO_MINI.put('0', "<black>");
        LEGACY_TO_MINI.put('1', "<dark_blue>");
        LEGACY_TO_MINI.put('2', "<dark_green>");
        LEGACY_TO_MINI.put('3', "<dark_aqua>");
        LEGACY_TO_MINI.put('4', "<dark_red>");
        LEGACY_TO_MINI.put('5', "<dark_purple>");
        LEGACY_TO_MINI.put('6', "<gold>");
        LEGACY_TO_MINI.put('7', "<gray>");
        LEGACY_TO_MINI.put('8', "<dark_gray>");
        LEGACY_TO_MINI.put('9', "<blue>");
        LEGACY_TO_MINI.put('a', "<green>");
        LEGACY_TO_MINI.put('b', "<aqua>");
        LEGACY_TO_MINI.put('c', "<red>");
        LEGACY_TO_MINI.put('d', "<light_purple>");
        LEGACY_TO_MINI.put('e', "<yellow>");
        LEGACY_TO_MINI.put('f', "<white>");
        LEGACY_TO_MINI.put('k', "<obfuscated>");
        LEGACY_TO_MINI.put('l', "<bold>");
        LEGACY_TO_MINI.put('m', "<strikethrough>");
        LEGACY_TO_MINI.put('n', "<underlined>");
        LEGACY_TO_MINI.put('o', "<italic>");
        LEGACY_TO_MINI.put('r', "<reset>");
    }

    private ColorHelper() {
    }

    /**
     * Normalise the supplied text into an Adventure component.
     *
     * @param input text containing legacy codes and/or MiniMessage tags
     * @return parsed component (never {@code null})
     */
    public static Component toComponent(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        String preprocessed = convertLegacyCodes(input);
        try {
            return MINI_MESSAGE.deserialize(preprocessed);
        } catch (Exception ex) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
        }
    }

    /**
     * Normalise the supplied text and serialise it back into a legacy section-string.
     *
     * @param input text containing legacy codes and/or MiniMessage tags
     * @return legacy serialised representation (never {@code null})
     */
    public static String toLegacy(String input) {
        return toLegacy(toComponent(input));
    }

    /**
     * Serialise the supplied component into a legacy section-string.
     *
     * @param component component to serialise
     * @return legacy serialised representation (never {@code null})
     */
    public static String toLegacy(Component component) {
        if (component == null) {
            return "";
        }
        return LEGACY_SECTION.serialize(component);
    }

    /**
     * Serialise the supplied component into plain text, stripping formatting.
     *
     * @param component component to serialise
     * @return plain text representation (never {@code null})
     */
    public static String toPlain(Component component) {
        if (component == null) {
            return "";
        }
        return PLAIN_SERIALIZER.serialize(component);
    }

    /**
     * Normalise the supplied text and serialise it into plain text.
     *
     * @param input text containing legacy codes and/or MiniMessage tags
     * @return plain text representation (never {@code null})
     */
    public static String toPlain(String input) {
        return toPlain(toComponent(input));
    }

    private static String convertLegacyCodes(String text) {
        StringBuilder output = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current != '&' || i + 1 >= text.length()) {
                output.append(current);
                continue;
            }

            char next = Character.toLowerCase(text.charAt(i + 1));
            if (next == 'x' && hasHexSequence(text, i)) {
                output.append(toMiniHex(text, i));
                i += 13; // &x&0&0&0&0&0&0 -> advance past entire sequence
                continue;
            }

            String replacement = LEGACY_TO_MINI.get(next);
            if (replacement != null) {
                output.append(replacement);
                i++; // skip colour code character
            } else {
                output.append(current);
            }
        }

        return output.toString();
    }

    private static boolean hasHexSequence(String text, int index) {
        if (index + 13 >= text.length()) {
            return false;
        }

        for (int offset = 2; offset <= 12; offset += 2) {
            char ampersand = text.charAt(index + offset);
            char digit = text.charAt(index + offset + 1);
            if (ampersand != '&' || !isHexDigit(digit)) {
                return false;
            }
        }
        return true;
    }

    private static String toMiniHex(String text, int index) {
        StringBuilder hex = new StringBuilder(6);
        for (int offset = 3; offset <= 13; offset += 2) {
            hex.append(Character.toLowerCase(text.charAt(index + offset))); // append digit
        }
        return "<#" + hex + ">";
    }

    private static boolean isHexDigit(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f');
    }
}
