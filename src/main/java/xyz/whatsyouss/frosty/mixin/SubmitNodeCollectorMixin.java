package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.render.Nametags;

@Mixin(SubmitNodeCollector.class)
public class SubmitNodeCollectorMixin {

    @WrapOperation(
            method = "submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void wrapLabelScale(PoseStack matrices, float x, float y, float z,
                                Operation<Void> original, PoseStack matrices2,
                                @Nullable Vec3 nameTagAttachment, int offset, Component name,
                                boolean seeThrough, int lightCoords, CameraRenderState camera) {
        Nametags nametags = ModuleManager.nametags;
        if (!nametags.isEnabled()) {
            original.call(matrices, x, y, z);
            return;
        }

        float scale = (float) (0.025F * nametags.scale.getInput());

        // Calculate distance from camera using the pose matrix
        Matrix4f pose = matrices.last().pose();
        Vector4f origin = new Vector4f(0, 0, 0, 1).mul(pose);
        double distanceSq = origin.x * origin.x + origin.y * origin.y + origin.z * origin.z;
        double distance = Math.sqrt(distanceSq);

        if (distance > 10) {
            scale *= distance / 10;
        }

        original.call(matrices, scale, -scale, scale);
    }
}
