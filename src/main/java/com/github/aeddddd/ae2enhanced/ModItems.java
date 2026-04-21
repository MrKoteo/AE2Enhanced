package com.github.aeddddd.ae2enhanced;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = AE2Enhanced.MOD_ID)
public class ModItems {

    public static void init() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // 升级卡片等物品将在后续里程碑中添加
    }
}
