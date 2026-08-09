package modoru.main.player.options;

import modoru.main.data.DataFields;
import modoru.main.data.user.cosmetics.Particle;
import modoru.main.util.ArrayUtil;

@SuppressWarnings("UnstableApiUsage")
public final class OptionsDialog {

    private static final Option<?>[] OPTIONS = ArrayUtil.create(
            Option.elements(
                    (container, index) -> {
                        if(index == 0) container.set(DataFields.CURRENT_PARTICLE, null);
                        else container.set(DataFields.CURRENT_PARTICLE, Particle.values()[index - 1]);
                    },
                    container -> {
                        Particle particle = container.get(DataFields.CURRENT_PARTICLE);
                        if(particle == null) return 0;

                        return particle.ordinal() + 1;
                    },
                    particle -> "modoru.main.options.option.particle." + switch (particle) {
                        case CHERRY -> "cherry";
                        case PALE -> "pale";
                        case null -> "disable";
                    },
                    ArrayUtil.create(null, Particle.CHERRY, Particle.PALE)
            )
    );


}
