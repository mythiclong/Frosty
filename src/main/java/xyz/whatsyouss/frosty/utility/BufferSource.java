package xyz.whatsyouss.frosty.utility;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.rendertype.RenderType;

import static xyz.whatsyouss.frosty.Frosty.mc;

/**
 * Small adapter that gives batched Xray ESP drawing the same lifecycle on
 * 26.1.2 as the staged buffer implementation used by 26.2.
 */
public final class BufferSource implements AutoCloseable {
    private final MultiBufferSource.BufferSource delegate;
    private final boolean reusable;
    private boolean closed;

    public BufferSource() {
        this(false);
    }

    private BufferSource(boolean reusable) {
        this.reusable = reusable;
        RenderBuffers renderBuffers = mc.renderBuffers();
        this.delegate = renderBuffers.bufferSource();
    }

    public static BufferSource reusable() {
        return new BufferSource(true);
    }

    public VertexConsumer getBuffer(RenderType renderType) {
        if (closed) {
            throw new IllegalStateException("BufferSource is closed");
        }
        return delegate.getBuffer(renderType);
    }

    public void uploadAndDraw() {
        if (closed) {
            return;
        }
        delegate.endBatch();
        if (!reusable) {
            close();
        }
    }

    @Override
    public void close() {
        closed = true;
    }
}
