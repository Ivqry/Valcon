package net.ivqrydev.valcon.block.custom;

import com.mojang.serialization.MapCodec;
import net.ivqrydev.valcon.block.entity.SoulForgeBlockEntity;
import net.ivqrydev.valcon.item.ModItems;
import net.ivqrydev.valcon.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Unbreakable;
import org.jetbrains.annotations.Nullable;

public class SoulForgeBlock extends BaseEntityBlock {
    public static final MapCodec<SoulForgeBlock> CODEC = simpleCodec(SoulForgeBlock::new);

    public SoulForgeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /* BLOCK ENTITY */

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SoulForgeBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof SoulForgeBlockEntity soulForgeBlockEntity) {
                soulForgeBlockEntity.drops();
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof SoulForgeBlockEntity soulForgeBlockEntity) {
            ItemStack stored = soulForgeBlockEntity.inventory.getStackInSlot(0);

            // Attempt to insert a durability item
            if (stored.isEmpty() && !stack.isEmpty()) {
                if (stack.isDamageableItem()) {
                    soulForgeBlockEntity.inventory.insertItem(0, stack.copy(), false);
                    stack.shrink(1);
                    level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 2f);
                } else {
                    player.displayClientMessage(Component.literal("This item is unworthy."), true);
                }

                // Attempt to extract item
            } else if (stack.isEmpty()) {
                ItemStack extracted = soulForgeBlockEntity.inventory.extractItem(0, 1, false);
                if (!extracted.isEmpty()) {
                    player.setItemInHand(hand, extracted);
                    soulForgeBlockEntity.clearContents();
                    level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 1f);
                }
            } else if (!stored.isEmpty() && !stack.isEmpty()) {
                // Replace this with your modded ingot item instance
                ItemStack ingotStack = stack;
                if (ingotStack.getItem() == ModItems.SOUL_STEEL_INGOT.get()) {
                    if (stored.isDamageableItem()) {
                        stored.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
                        soulForgeBlockEntity.inventory.setStackInSlot(0, stored);
                        ingotStack.shrink(1);
                        level.playSound(player, pos, ModSounds.SOUL_FORGE_USE.get(), SoundSource.BLOCKS, 1f, 1f);
                        player.displayClientMessage(Component.literal("Forge successful! Item made imperishable."), true);
                        for (int i = 0; i < 20; i++) {
                            double offsetX = level.random.nextGaussian() * 0.1;
                            double offsetY = level.random.nextDouble() * 0.1;
                            double offsetZ = level.random.nextGaussian() * 0.1;
                            double x = pos.getX() + 0.5;
                            double y = pos.getY() + 1;
                            double z = pos.getZ() + 0.5;
                            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, offsetX, offsetY, offsetZ);
                        }
                    } else {
                        player.displayClientMessage(Component.literal("This item is unworthy"), true);
                    }
                }
            }
        }
        return ItemInteractionResult.SUCCESS;
    }
}