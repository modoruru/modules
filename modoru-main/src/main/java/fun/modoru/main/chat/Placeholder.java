package fun.modoru.main.chat;

import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public record Placeholder<E>(String name, Function<E, Object> resolver) {

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

    private static void replace(StringBuilder content, String substring, Supplier<Object> supplier) {
        final int substringLength = substring.length();

        String formatted = null;
        int formattedLength = -1;

        int index;
        int indexThreshold = 0;
        while ((index = content.indexOf(substring, indexThreshold)) != -1) {
            if(formatted == null) {
                formatted = supplier.get().toString();
                formattedLength = formatted.length();
            }

            indexThreshold = index + formattedLength;
            content.replace(index, index + substringLength, formatted);
        }
    }

}
