package xyz.whatsyouss.frosty.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class FrostyMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("frosty");
    private static final String SUPPORTED_SODIUM_VERSION_26_2 = "0.9.1+mc26.2";
    private static final String SUPPORTED_SODIUM_VERSION_26_1 = "0.6.6+mc1.21.5";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.endsWith(".SodiumBlockRendererXrayMixin")) {
            return true;
        }

        return FabricLoader.getInstance().getModContainer("sodium")
                .map(container -> {
                    String version = container.getMetadata().getVersion().getFriendlyString();
                    if (SUPPORTED_SODIUM_VERSION_26_2.equals(version) ||
                        SUPPORTED_SODIUM_VERSION_26_1.equals(version) ||
                        version.startsWith("0.6.") || version.startsWith("0.9.")) {
                        LOGGER.info("Xray Sodium renderer hook enabled for version {}", version);
                        return true;
                    }
                    LOGGER.warn("Xray's Sodium renderer hook supports {} or {}, but found {}; disabling the hook",
                            SUPPORTED_SODIUM_VERSION_26_2, SUPPORTED_SODIUM_VERSION_26_1, version);
                    return false;
                })
                .orElseGet(() -> {
                    LOGGER.info("Sodium not found, Xray will use vanilla renderer only");
                    return false;
                });
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}
