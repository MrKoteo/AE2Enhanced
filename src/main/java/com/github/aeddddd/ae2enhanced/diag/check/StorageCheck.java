package com.github.aeddddd.ae2enhanced.diag.check;

import com.github.aeddddd.ae2enhanced.tile.TileHyperdimensionalController;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 超维度仓储中枢健康检查.
 *
 * <p>检查项：</p>
 * <ul>
 *   <li>已加载控制器：结构成型 / 安全模式 / 网络激活 / 网络供电</li>
 *   <li>磁盘存档：文件头完整性（Magic + 版本号），旧格式识别</li>
 *   <li>磁盘与内存对照：存档存在但控制器未加载 → INFO（区块未加载属正常）</li>
 * </ul>
 */
public final class StorageCheck implements SystemCheck {

    /** 与 HyperdimensionalStorageFile 文件头格式保持一致（Magic "AE2E" + version + flags + entryCount） */
    private static final int HEADER_MAGIC = 0x41453245; // "AE2E"
    private static final int HEADER_CURRENT_VERSION = 1;
    private static final int HEADER_BYTES = 16;

    @Override
    public String name() {
        return "storage";
    }

    @Override
    public String displayName() {
        return "超维度仓储中枢";
    }

    @Override
    public void run(MinecraftServer server, List<CheckResult> out) {
        try {
            Set<UUID> loadedNexusIds = checkLoadedControllers(server, out);
            checkDiskStorage(server, loadedNexusIds, out);
        } catch (Exception e) {
            out.add(CheckResult.error("仓储检查执行异常: " + e));
        }
    }

    private Set<UUID> checkLoadedControllers(MinecraftServer server, List<CheckResult> out) {
        Set<UUID> nexusIds = new HashSet<>();
        int total = 0;
        int formed = 0;
        for (WorldServer world : server.worlds) {
            // 与 CommandAE2Enhanced 现有遍历模式一致：扫 loadedTileEntityList
            for (TileEntity te : world.loadedTileEntityList) {
                if (!(te instanceof TileHyperdimensionalController)) {
                    continue;
                }
                TileHyperdimensionalController controller = (TileHyperdimensionalController) te;
                total++;
                UUID nexusId = controller.getNexusId();
                if (nexusId != null) {
                    nexusIds.add(nexusId);
                }
                String where = te.getPos() + " @dim" + world.provider.getDimension();
                if (!controller.isFormed()) {
                    out.add(CheckResult.error("控制器未成型: " + where));
                    continue;
                }
                formed++;
                if (controller.isSafeMode()) {
                    out.add(CheckResult.error("控制器处于安全模式(存储分区损坏): " + where));
                }
                if (!controller.isNetworkActive()) {
                    out.add(CheckResult.warn("控制器网络未激活: " + where));
                }
                if (!controller.isNetworkPowered()) {
                    out.add(CheckResult.warn("控制器网络供电不足: " + where));
                }
            }
        }
        out.add(CheckResult.ok("已加载控制器: " + total + " 个(成型 " + formed + " 个)"));
        return nexusIds;
    }

    private void checkDiskStorage(MinecraftServer server, Set<UUID> loadedNexusIds, List<CheckResult> out) {
        WorldServer overworld = server.getWorld(0);
        if (overworld == null) {
            out.add(CheckResult.warn("主世界不可用,跳过磁盘存档检查"));
            return;
        }
        File storageDir = new File(overworld.getSaveHandler().getWorldDirectory(), "ae2enhanced/storage");
        if (!storageDir.exists()) {
            out.add(CheckResult.ok("磁盘存档目录不存在(尚无仓储数据)"));
            return;
        }
        File[] entries = storageDir.listFiles((dir, name) -> {
            if (name.startsWith("smartpattern_")) return false;
            File f = new File(dir, name);
            return f.isDirectory() || name.endsWith(".dat");
        });
        if (entries == null || entries.length == 0) {
            out.add(CheckResult.ok("磁盘存档目录为空"));
            return;
        }
        int healthy = 0;
        int unloaded = 0;
        for (File entry : entries) {
            String id = entry.isDirectory()
                    ? entry.getName()
                    : entry.getName().substring(0, entry.getName().length() - 4);
            String headerError = validateHeader(entry);
            if (headerError != null) {
                out.add(CheckResult.error("存档 " + id + " 文件头损坏: " + headerError));
                continue;
            }
            healthy++;
            boolean loaded;
            try {
                loaded = loadedNexusIds.contains(UUID.fromString(id));
            } catch (IllegalArgumentException e) {
                out.add(CheckResult.warn("存档 " + id + " 不是合法 UUID 命名"));
                continue;
            }
            if (!loaded) {
                unloaded++;
            }
        }
        out.add(CheckResult.ok("磁盘存档: " + entries.length + " 个(健康 " + healthy
                + " 个,当前未加载 " + unloaded + " 个)"));
    }

    /** 校验分区文件头；合法返回 null，否则返回错误描述。 */
    private static String validateHeader(File entry) {
        File target;
        if (entry.isDirectory()) {
            target = new File(entry, "items.bin");
            if (!target.exists()) {
                return null; // 目录存在但无 item 分区（可能仅存流体等）,不判错
            }
        } else {
            target = entry; // 旧格式 <uuid>.dat
        }
        if (target.length() < HEADER_BYTES) {
            return "文件长度不足 " + HEADER_BYTES + " 字节";
        }
        try (DataInputStream in = new DataInputStream(new FileInputStream(target))) {
            int magic = in.readInt();
            if (magic != HEADER_MAGIC) {
                return "Magic 不匹配(0x" + Integer.toHexString(magic) + ")";
            }
            int version = in.readInt();
            if (version < 0 || version > HEADER_CURRENT_VERSION) {
                return "版本号非法: " + version;
            }
            return null;
        } catch (Exception e) {
            return "读取失败: " + e;
        }
    }
}
