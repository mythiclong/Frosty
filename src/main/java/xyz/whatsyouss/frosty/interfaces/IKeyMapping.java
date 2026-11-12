package xyz.whatsyouss.frosty.interfaces;

import net.minecraft.client.KeyMapping;

public interface IKeyMapping {

    boolean frosty$isActuallyDown();

    void frosty$resetPressedState();

    void frosty$simulatePress(boolean pressed);

    public default void setDown(boolean down)
    {
        asVanilla().setDown(down);
    }

    public default KeyMapping asVanilla()
    {
        return (KeyMapping)this;
    }

    public static IKeyMapping get(KeyMapping kb)
    {
        return (IKeyMapping)kb;
    }
}