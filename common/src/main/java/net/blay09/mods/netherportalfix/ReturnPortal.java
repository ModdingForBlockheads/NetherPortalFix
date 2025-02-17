package net.blay09.mods.netherportalfix;

import net.minecraft.BlockUtil;

import java.util.UUID;

public class ReturnPortal {
    private final UUID uid;
    private final BlockUtil.FoundRectangle rectangle;

    public ReturnPortal(UUID uid, BlockUtil.FoundRectangle rectangle) {
        this.uid = uid;
        this.rectangle = rectangle;
    }

    public UUID getUid() {
        return uid;
    }

    public BlockUtil.FoundRectangle getRectangle() {
        return rectangle;
    }
}
