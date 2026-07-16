package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlay(PoseStack poseStack, MultiBufferSource bufferSource, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (ModuleManager.noOverlay.isEnabled() && ModuleManager.noOverlay.fire.isToggled())
            ci.cancel();
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void renderUnderwaterOverlayHook(Minecraft minecraft, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (ModuleManager.noOverlay.isEnabled() && ModuleManager.noOverlay.water.isToggled())
            ci.cancel();
    }

    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void renderInWallOverlayHook(TextureAtlasSprite sprite, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (ModuleManager.noOverlay.isEnabled() && ModuleManager.noOverlay.inWall.isToggled())
            ci.cancel();
    }
}