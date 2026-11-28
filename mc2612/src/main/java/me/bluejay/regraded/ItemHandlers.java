package me.bluejay.regraded;

import me.bluejay.regraded.core.Points;
import me.bluejay.regraded.regrade.Regrade;
import me.bluejay.regraded.regrade.RegradeQueue;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;

/**
 * Server-side right-click handlers for tools that modify the world.
 * Client-only tools (hub, add/delete node) are handled in ReGradedClient.
 */
public final class ItemHandlers {
    private static final double RAY_RANGE = 100.0;

    private ItemHandlers() {}

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            var stack = player.getItemInHand(hand);
            ServerLevel level = sp.level();

            if (stack.is(ModItems.REGRADE_TOOL)) {
                return regradeBlock(sp, level);
            }
            if (stack.is(ModItems.REGRADE_CHUNK)) {
                return regradeChunk(sp, level);
            }
            if (stack.is(ModItems.REMOVE_TOOL)) {
                return removeChiseled(sp, level);
            }
            if (stack.is(ModItems.REMOVE_CHUNK)) {
                return removeChunk(sp, level);
            }
            return InteractionResult.PASS;
        });
    }

    private static InteractionResult regradeBlock(ServerPlayer player, ServerLevel world) {
        if (Points.size() == 0) {
            player.sendSystemMessage(Component.literal("No points loaded."));
            return InteractionResult.SUCCESS;
        }
        HitResult hit = player.pick(RAY_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("No block in look direction."));
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        RegradeQueue.enqueueColumn(world, pos.getX(), pos.getZ(), player);
        player.sendSystemMessage(Component.literal(
                "Queued column at " + pos.getX() + "," + pos.getZ() + " (" + RegradeQueue.size() + " in queue)."));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult regradeChunk(ServerPlayer player, ServerLevel world) {
        if (Points.size() == 0) {
            player.sendSystemMessage(Component.literal("No points loaded."));
            return InteractionResult.SUCCESS;
        }
        HitResult hit = player.pick(RAY_RANGE, 1.0f, false);
        BlockPos pos = hit.getType() == HitResult.Type.BLOCK
                ? ((BlockHitResult) hit).getBlockPos()
                : player.blockPosition();
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        int removed = Regrade.removeChunk(world, chunk);
        RegradeQueue.enqueueChunk(world, chunk, player);
        player.sendSystemMessage(Component.literal(
                "Removed " + removed + " chiseled blocks, queued chunk " + chunk.x() + "," + chunk.z()
                        + " (" + RegradeQueue.size() + " columns)."));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult removeChiseled(ServerPlayer player, ServerLevel world) {
        HitResult hit = player.pick(RAY_RANGE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("No block in look direction."));
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        int cleared = clearColumn(world, pos);
        player.sendSystemMessage(Component.literal(
                "Cleared chiseled column at " + pos.getX() + "," + pos.getZ()
                        + " (" + cleared + " blocks)."));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult removeChunk(ServerPlayer player, ServerLevel world) {
        HitResult hit = player.pick(RAY_RANGE, 1.0f, false);
        BlockPos pos = hit.getType() == HitResult.Type.BLOCK
                ? ((BlockHitResult) hit).getBlockPos()
                : player.blockPosition();
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        int removed = Regrade.removeChunk(world, chunk);
        player.sendSystemMessage(Component.literal(
                "Removed " + removed + " chiseled blocks from chunk " + chunk.x() + "," + chunk.z() + "."));
        return InteractionResult.SUCCESS;
    }

    /**
     * Remove the chiseled (multi-state) property from the hit block and the
     * column above it. Setting the block to air drops the C&B block entity
     * that holds the chiseled state.
     */
    private static int clearColumn(ServerLevel world, BlockPos origin) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int cleared = 0;
        int x = origin.getX();
        int z = origin.getZ();
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos pos = new BlockPos(x, origin.getY() + dy, z);
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) continue;
            world.setBlock(pos, air, 3);
            cleared++;
        }
        return cleared;
    }
}
