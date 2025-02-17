package net.blay09.mods.netherportalfix.mixin;

import net.blay09.mods.netherportalfix.NetherPortalFix;
import net.blay09.mods.netherportalfix.ReturnPortal;
import net.blay09.mods.netherportalfix.ReturnPortalManager;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(method = "getExitPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/border/WorldBorder;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    public void getExitPortal(ServerLevel level, BlockPos pos, boolean isToNether, WorldBorder worldBorder, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> callbackInfo) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        BlockPos fromPos = player.blockPosition();
        final ResourceKey<Level> fromDim = player.level().dimension();
        final ResourceKey<Level> toDim = level.dimension();
        final ResourceKey<Level> OVERWORLD = Level.OVERWORLD;
        final ResourceKey<Level> THE_NETHER = Level.NETHER;
        boolean isPlayerCurrentlyInPortal = ((EntityAccessor) player).getIsInsidePortal();
        boolean isTeleportBetweenNetherAndOverworld = (fromDim == OVERWORLD && toDim == THE_NETHER)
                || (fromDim == THE_NETHER && toDim == OVERWORLD);
        if (isPlayerCurrentlyInPortal && isTeleportBetweenNetherAndOverworld) {
            ReturnPortal returnPortal = ReturnPortalManager.findReturnPortal(player, fromDim, fromPos);
            if (returnPortal == null) {
                NetherPortalFix.logger.info("No return portal found");
                return;
            }

            final var foundRectangle = ReturnPortalManager.findRectangleFromReturnPortal(level, returnPortal);
            if (foundRectangle == null) {
                NetherPortalFix.logger.info("Return portal is no longer valid");
                return;
            }

            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }

            Level toLevel = server.getLevel(toDim);
            if (toLevel == null) {
                return;
            }

            NetherPortalFix.logger.debug("Return portal found, redirecting! :)");
            callbackInfo.setReturnValue(Optional.of(foundRectangle));
        }
    }

}
