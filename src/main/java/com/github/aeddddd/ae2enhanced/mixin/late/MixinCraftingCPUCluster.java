package com.github.aeddddd.ae2enhanced.mixin.late;

import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.cache.CraftingGridCache;
import appeng.api.networking.energy.IEnergyGrid;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyController;
import com.github.aeddddd.ae2enhanced.tile.TileAssemblyMeInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = CraftingCPUCluster.class, remap = false, priority = 1000)
public class MixinCraftingCPUCluster {

    // ---- 反射缓存 ----
    private static Field tasksField;
    private static Field waitingForField;
    private static Field remOpsField;
    private static Field remItemCountField;
    private static Method postCraftingStatusChange;
    private static Method postChange;
    private static Field taskProgressValueField;
    private static Method waitingForAddMethod;
    private static boolean reflectionReady = false;

    private static void initReflection() throws Exception {
        if (reflectionReady) return;
        tasksField = CraftingCPUCluster.class.getDeclaredField("tasks");
        tasksField.setAccessible(true);
        waitingForField = CraftingCPUCluster.class.getDeclaredField("waitingFor");
        waitingForField.setAccessible(true);
        remOpsField = CraftingCPUCluster.class.getDeclaredField("remainingOperations");
        remOpsField.setAccessible(true);
        remItemCountField = CraftingCPUCluster.class.getDeclaredField("remainingItemCount");
        remItemCountField.setAccessible(true);
        postCraftingStatusChange = CraftingCPUCluster.class.getDeclaredMethod("postCraftingStatusChange", IAEItemStack.class);
        postCraftingStatusChange.setAccessible(true);
        postChange = CraftingCPUCluster.class.getDeclaredMethod("postChange", IAEItemStack.class, appeng.api.networking.security.IActionSource.class);
        postChange.setAccessible(true);
        Class<?> taskProgressClass = Class.forName("appeng.me.cluster.implementations.CraftingCPUCluster$TaskProgress");
        taskProgressValueField = taskProgressClass.getDeclaredField("value");
        taskProgressValueField.setAccessible(true);
        reflectionReady = true;
    }

    private static Method getWaitingForAdd(Object waitingFor) throws NoSuchMethodException {
        if (waitingForAddMethod == null) {
            waitingForAddMethod = waitingFor.getClass().getMethod("add", IAEItemStack.class);
        }
        return waitingForAddMethod;
    }

    @Inject(method = "executeCrafting", at = @At("HEAD"))
    private void batchProcessVirtualTasks(IEnergyGrid energy, CraftingGridCache cache, CallbackInfo ci) {
        try {
            initReflection();
            CraftingCPUCluster cpu = (CraftingCPUCluster) (Object) this;

            @SuppressWarnings("unchecked")
            Map<ICraftingPatternDetails, Object> tasks = (Map<ICraftingPatternDetails, Object>) tasksField.get(cpu);
            if (tasks.isEmpty()) return;

            Object waitingFor = waitingForField.get(cpu);
            Method waitingForAdd = getWaitingForAdd(waitingFor);
            int remainingOps = remOpsField.getInt(cpu);
            long remainingItemCount = remItemCountField.getLong(cpu);

            // 收集需要移除的 keys（避免在遍历中修改 map）
            List<ICraftingPatternDetails> toRemove = new ArrayList<>();

            for (Map.Entry<ICraftingPatternDetails, Object> entry : new ArrayList<>(tasks.entrySet())) {
                ICraftingPatternDetails details = entry.getKey();
                Object progress = entry.getValue();

                long remaining = taskProgressValueField.getLong(progress);
                if (remaining <= 0) continue;

                List<ICraftingMedium> mediums = cache.getMediums(details);
                if (mediums == null || mediums.isEmpty()) continue;

                for (ICraftingMedium medium : mediums) {
                    if (!(medium instanceof TileAssemblyMeInterface)) continue;

                    TileAssemblyController controller = ((TileAssemblyMeInterface) medium).getController();
                    if (controller == null || !controller.isVirtualPattern(details)) continue;

                    appeng.api.networking.security.IActionSource source = cpu.getActionSource();
                    controller.setCurrentActionSource(source);
                    try {
                        boolean success = controller.executeBatch(details, remaining);
                        if (success) {
                            toRemove.add(details);
                            remainingOps -= remaining;
                            remainingItemCount -= remaining;

                            for (IAEItemStack outputTemplate : details.getCondensedOutputs()) {
                                if (outputTemplate == null || outputTemplate.getStackSize() <= 0) continue;
                                IAEItemStack expected = outputTemplate.copy();
                                expected.setStackSize(outputTemplate.getStackSize() * remaining);

                                postChange.invoke(cpu, expected.copy(), source);
                                waitingForAdd.invoke(waitingFor, expected.copy());
                                postCraftingStatusChange.invoke(cpu, expected.copy());
                            }

                            System.out.println("[AE2E] BATCH: removed task remaining=" + remaining);
                        }
                    } finally {
                        controller.setCurrentActionSource(null);
                    }
                    break;
                }
            }

            // 直接移除已 batch 处理的 tasks entry，阻止 executeCrafting 后续遍历到它们
            for (ICraftingPatternDetails key : toRemove) {
                tasks.remove(key);
            }

            if (remainingOps != remOpsField.getInt(cpu)) {
                remOpsField.setInt(cpu, remainingOps);
            }
            if (remainingItemCount != remItemCountField.getLong(cpu)) {
                remItemCountField.setLong(cpu, remainingItemCount);
            }
        } catch (Exception e) {
            System.err.println("[AE2E] batchProcessVirtualTasks error: " + e);
            e.printStackTrace();
        }
    }
}
