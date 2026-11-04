package fun.modoru.main.chat;

import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public record Placeholder<E>(String name, Function<E, @Nullable Object> resolver) {

    @SafeVarargs
    public static <E> void resolve(StringBuilder content, E object, Placeholder<E>... placeholders) {
        resolve(content, '%', '%', object, placeholders);
    }

    @SafeVarargs
    public static <E> void resolve(StringBuilder content, @Nullable Character placeholderStartChar, @Nullable Character placeholderEndChar, E object, Placeholder<E>... placeholders) {
        for (Placeholder<E> placeholder : placeholders) {
            StringBuilder boxedName = new StringBuilder();
            if(placeholderStartChar != null) boxedName.append(placeholderStartChar);
            boxedName.append(placeholder.name());
            if(placeholderEndChar != null) boxedName.append(placeholderEndChar);

            replace(content, boxedName.toString(), () -> placeholder.resolver.apply(object));
        }
    }

    public static String sequenceReplaces(String original, String... placeholdersAndReplacements) {
        if(placeholdersAndReplacements.length % 2 != 0) throw new IllegalArgumentException();
        StringBuilder builder = new StringBuilder(original);

        int count = placeholdersAndReplacements.length / 2;
        for (int i = 0; i < count; i++) {
            String placeholder = placeholdersAndReplacements[i * 2];
            String replacement = placeholdersAndReplacements[i * 2 + 1];
            int placeholderLength = placeholder.length();
            int replacementLength = replacement.length();

            int index;
            int indexThreshold = 0;
            while ((index = builder.indexOf(placeholder, indexThreshold)) != -1) {
                builder.replace(index, index + placeholderLength, replacement);
                indexThreshold = index + replacementLength;
            }
        }

        return builder.toString();
    }

    public static void replace(StringBuilder content, String substring, Supplier<@Nullable Object> supplier) {
        final int substringLength = substring.length();

        String formatted = null;
        int formattedLength = -1;

        int index;
        int indexThreshold = 0;
        while ((index = content.indexOf(substring, indexThreshold)) != -1) {
            if(formatted == null) {
                Object object = supplier.get();
                if(object == null) formatted = "null";
                else formatted = object.toString();
                formattedLength = formatted.length();
            }

            indexThreshold = index + formattedLength;
            content.replace(index, index + substringLength, formatted);
        }
    }

}
