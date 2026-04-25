package com.github.aeddddd.ae2enhanced.tile;

import com.github.aeddddd.ae2enhanced.crafting.BlackHoleCraftingHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * 微型奇点的 TileEntity。
 * 300 秒（6000 ticks）后自动坍缩消失。
 * 期间对 3×3×3 范围内的生物执行击杀，对物品执行黑洞合成。
 */
public class TileMicroSingularity extends TileEntity implements ITickable {

    private int lifeTicks = 6000;

    @Override
    public void update() {
        if (world.isRemote) {
            // 客户端：生成紫色粒子
            if (world.rand.nextInt(4) == 0) {
                double ox = pos.getX() + 0.5;
                double oy = pos.getY() + 0.5;
                double oz = pos.getZ() + 0.5;
                world.spawnParticle(EnumParticleTypes.PORTAL,
                        ox + (world.rand.nextDouble() - 0.5) * 0.8,
                        oy + (world.rand.nextDouble() - 0.5) * 0.8,
                        oz + (world.rand.nextDouble() - 0.5) * 0.8,
                        (world.rand.nextDouble() - 0.5) * 0.2,
                        (world.rand.nextDouble() - 0.5) * 0.2,
                        (world.rand.nextDouble() - 0.5) * 0.2);
            }
            return;
        }

        // 事件视界：生物击杀
        BlockPos origin = pos;
        AxisAlignedBB horizon = new AxisAlignedBB(
                origin.getX() - 1, origin.getY() - 1, origin.getZ() - 1,
                origin.getX() + 2, origin.getY() + 2, origin.getZ() + 2
        );
        for (EntityLivingBase entity : world.getEntitiesWithinAABB(EntityLivingBase.class, horizon)) {
            if (entity.isEntityAlive()) {
                entity.setDead();
            }
        }

        // 黑洞合成（系统 B）
        BlackHoleCraftingHelper.tryCraft(world, pos);

        // 倒计时
        if (--lifeTicks <= 0) {
            collapse();
        }
    }

    private void collapse() {
        world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.BLOCKS, 2.0f, 0.5f);
        world.setBlockToAir(pos);
    }
}
