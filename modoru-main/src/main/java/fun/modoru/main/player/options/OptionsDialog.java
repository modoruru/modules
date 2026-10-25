package fun.modoru.main.player.options;

import fun.modoru.main.util.ArrayUtil;
import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.user.Particle;

public final class OptionsDialog {

    private static final Option<?>[] OPTIONS = ArrayUtil.create(
            Option.elements(
                    (container, index) -> {
                        if(index == 0) container.set(UserDefinition.particle, null);
                        else container.set(UserDefinition.particle, Particle.values()[index - 1]);
                    },
                    container -> {
                        Particle particle = container.get(UserDefinition.particle);
                        if(particle == null) return 0;

                        return particle.ordinal() + 1;
                    },
                    particle -> "modoru.main.options.option.particle." + switch (particle) {
                        case CHERRY -> "cherry";
                        case PALE_OAK -> "pale";
                        case null -> "disable";
                    },
                    ArrayUtil.create(null, Particle.CHERRY, Particle.PALE_OAK)
            )
    );


}
