package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.renderer.entity.MobRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(MobRenderer.class)
public abstract class MobRendererMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Mob;D)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;crosshairPickEntity:Lnet/minecraft/world/entity/Entity;", opcode = Opcodes.GETFIELD, ordinal = 0), cancellable = true)
    private void onHasLabel(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.nametags.isEnabled())
            cir.setReturnValue(true);
    }
}
