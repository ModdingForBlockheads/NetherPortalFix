package net.blay09.mods.netherportalfix;

import net.blay09.mods.balm.api.Balm;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalForcer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ReturnPortalManager {

    private static final int MAX_PORTAL_DISTANCE_SQ = 16;
    private static final String RETURN_PORTAL_LIST = "ReturnPortalList";
    private static final String RETURN_PORTAL_UID = "UID";
    private static final String FROM_DIM = "FromDim";
    private static final String FROM_POS = "FromPos";
    private static final String TO_MIN_CORNER = "ToMinCorner";
    private static final String TO_AXIS_1_SIZE = "ToAxis1Size";
    private static final String TO_AXIS_2_SIZE = "ToAxis2Size";

    public static BlockUtil.FoundRectangle findPortalAt(Player player, ResourceKey<Level> dim, BlockPos pos) {
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            ServerLevel fromWorld = server.getLevel(dim);
            if (fromWorld != null) {
                PortalForcer portalForcer = fromWorld.getPortalForcer();
                return portalForcer.findPortalAround(pos, false, fromWorld.getWorldBorder()).orElse(null);
            }
        }

        return null;
    }

    public static BlockUtil.FoundRectangle findRectangleFromReturnPortal(ServerLevel level, ReturnPortal returnPortal) {
        PortalForcer portalForcer = level.getPortalForcer();
        return portalForcer.findPortalAround(returnPortal.getRectangle().minCorner, false, level.getWorldBorder()).orElse(null);
    }

    public static ListTag getPlayerPortalList(Player player) {
        CompoundTag data = Balm.getHooks().getPersistentData(player);
        ListTag list = data.getList(RETURN_PORTAL_LIST, Tag.TAG_COMPOUND);
        data.put(RETURN_PORTAL_LIST, list);
        return list;
    }

    @Nullable
    public static ReturnPortal findReturnPortal(ServerPlayer player, ResourceKey<Level> fromDim, BlockPos fromPos) {
        ListTag portalList = getPlayerPortalList(player);
        for (Tag entry : portalList) {
            CompoundTag portal = (CompoundTag) entry;
            ResourceKey<Level> entryFromDim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(portal.getString(FROM_DIM)));
            if (entryFromDim == fromDim) {
                BlockPos portalTrigger = BlockPos.of(portal.getLong(FROM_POS));
                if (portalTrigger.distSqr(fromPos) <= MAX_PORTAL_DISTANCE_SQ) {
                    UUID uid = portal.hasUUID(RETURN_PORTAL_UID) ? portal.getUUID(RETURN_PORTAL_UID) : UUID.randomUUID();
                    BlockPos minCorner = BlockPos.of(portal.getLong(TO_MIN_CORNER));
                    int axis1Size = portal.getInt(TO_AXIS_1_SIZE);
                    int axis2Size = portal.getInt(TO_AXIS_2_SIZE);
                    return new ReturnPortal(uid, new BlockUtil.FoundRectangle(minCorner, axis1Size, axis2Size));
                }
            }
        }

        return null;
    }

    public static void storeReturnPortal(ServerPlayer player, ResourceKey<Level> fromDim, BlockPos fromPos, BlockUtil.FoundRectangle toPortal) {
        ListTag portalList = getPlayerPortalList(player);
        ReturnPortal returnPortal = findReturnPortal(player, fromDim, fromPos);
        if (returnPortal != null) {
            removeReturnPortal(player, returnPortal);
        }

        CompoundTag portalCompound = new CompoundTag();
        portalCompound.putUUID(RETURN_PORTAL_UID, UUID.randomUUID());
        portalCompound.putString(FROM_DIM, String.valueOf(fromDim.location()));
        portalCompound.putLong(FROM_POS, fromPos.asLong());
        portalCompound.putLong(TO_MIN_CORNER, toPortal.minCorner.asLong());
        portalCompound.putInt(TO_AXIS_1_SIZE, toPortal.axis1Size);
        portalCompound.putInt(TO_AXIS_2_SIZE, toPortal.axis2Size);
        portalList.add(portalCompound);
    }

    public static void removeReturnPortal(ServerPlayer player, ReturnPortal portal) {
        // This doesn't check if it's the right toDim, but it's probably so rare for positions to actually overlap that I don't care
        ListTag portalList = getPlayerPortalList(player);
        for (int i = 0; i < portalList.size(); i++) {
            CompoundTag entry = (CompoundTag) portalList.get(i);
            if (entry.hasUUID(RETURN_PORTAL_UID) && entry.getUUID(RETURN_PORTAL_UID).equals(portal.getUid())) {
                portalList.remove(i);
                break;
            }
        }
    }
}
