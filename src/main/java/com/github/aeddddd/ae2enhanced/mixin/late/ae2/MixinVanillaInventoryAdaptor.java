package com.github.aeddddd.ae2enhanced.mixin.late.ae2;

import appeng.util.InventoryAdaptor;
import com.github.aeddddd.ae2enhanced.util.inv.VanillaAdaptorCache;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 缓存原版 {@link InventoryAdaptor#getAdaptor(TileEntity, EnumFacing)} 的结果.
 *
 * <p>仅在 ae2fc 未安装时加载（见 LateMixinLoader）：ae2fc 安装时接口热路径的
 * getAdaptor 会被 ae2fc 重定向到 FluidConvertingInventoryAdaptor.wrap，
 * 该路径已由 MixinFluidConvertingInventoryAdaptor + FluidAdaptorCache 覆盖。</p>
 *
 * <p>缓存语义与失效校验见 {@link VanillaAdaptorCache}。</p>
 */
@Mixin(value = InventoryAdaptor.class, remap = false)
public abstract class MixinVanillaInventoryAdaptor {

    @Inject(method = "getAdaptor(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)Lappeng/util/InventoryAdaptor;",
            at = @At("HEAD"), cancellable = true)
    private static void ae2e$cachedGetAdaptor(TileEntity te, EnumFacing face,
            CallbackInfoReturnable<InventoryAdaptor> cir) {
        if (te == null) {
            return;
        }
        InventoryAdaptor cached = VanillaAdaptorCache.get(te, face);
        if (cached != null) {
            cir.setReturnValue(cached == VanillaAdaptorCache.NONE ? null : cached);
        }
    }

    @Inject(method = "getAdaptor(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)Lappeng/util/InventoryAdaptor;",
            at = @At("RETURN"))
    private static void ae2e$cacheGetAdaptorResult(TileEntity te, EnumFacing face,
            CallbackInfoReturnable<InventoryAdaptor> cir) {
        if (te != null) {
            VanillaAdaptorCache.put(te, face, cir.getReturnValue());
        }
    }
}
