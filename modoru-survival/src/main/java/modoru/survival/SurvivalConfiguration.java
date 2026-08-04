package modoru.survival;

import su.hitori.api.configuration.Field;
import su.hitori.api.configuration.SectionScheme;

public final class SurvivalConfiguration extends SectionScheme {

    public final Miscellaneous miscellaneous = new Miscellaneous();

    public static final class Miscellaneous extends SectionScheme {
        public final Field<Float> wardenSwiftSneakDropChance = Field.create(0.4f);
    }

}
