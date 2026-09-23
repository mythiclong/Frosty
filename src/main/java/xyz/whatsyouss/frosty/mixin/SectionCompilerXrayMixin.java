package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerXrayMixin {
    /**
     * 直接修改命名方法 getOrBeginLayer 的入参，而不是注入编译器生成的
     * lambda$compile$N 合成方法——后者的方法名对编译器版本敏感，MC 更新即崩。
     */
    @ModifyVariable(
            method = "getOrBeginLayer",
            at = @At("HEAD"),
            argsOnly = true
    )
    private ChunkSectionLayer frosty$xrayLayer(ChunkSectionLayer original) {
        return Xray.useTranslucentLayer() ? ChunkSectionLayer.TRANSLUCENT : original;
    }
}
