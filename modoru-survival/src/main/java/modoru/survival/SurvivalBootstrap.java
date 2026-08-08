package modoru.survival;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import org.jspecify.annotations.NonNull;
import su.hitori.api.module.ModuleBootstrap;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public final class SurvivalBootstrap implements ModuleBootstrap {

    @SuppressWarnings({"UnstableApiUsage", "removal"})
    @Override
    public void bootstrap(@NonNull BootstrapContext context) {
        try {
            Field FeatureFlags_DEFAULT_FLAGS = net.minecraft.world.flag.FeatureFlags.class.getDeclaredField("DEFAULT_FLAGS");
            Field FeatureFlags_VANILLA_SET = net.minecraft.world.flag.FeatureFlags.class.getDeclaredField("VANILLA_SET");

            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);

            net.minecraft.world.flag.FeatureFlagSet set = net.minecraft.world.flag.FeatureFlagSet.of(
                    net.minecraft.world.flag.FeatureFlags.VANILLA,
                    net.minecraft.world.flag.FeatureFlags.TRADE_REBALANCE,
                    net.minecraft.world.flag.FeatureFlags.MINECART_IMPROVEMENTS
            );

            long offset = unsafe.staticFieldOffset(FeatureFlags_DEFAULT_FLAGS);
            Object base = unsafe.staticFieldBase(FeatureFlags_DEFAULT_FLAGS);
            unsafe.putObject(base, offset, set);

            offset = unsafe.staticFieldOffset(FeatureFlags_VANILLA_SET);
            base = unsafe.staticFieldBase(FeatureFlags_VANILLA_SET);
            unsafe.putObject(base, offset, set);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
