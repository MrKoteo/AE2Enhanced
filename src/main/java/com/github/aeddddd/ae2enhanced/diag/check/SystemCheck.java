package com.github.aeddddd.ae2enhanced.diag.check;

import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * 系统级只读健康检查.
 *
 * <p>实现必须满足：</p>
 * <ul>
 *   <li><b>只读</b>：不得修改任何世界/网格/注册表状态</li>
 *   <li><b>异常自包含</b>：单系统内部异常应捕获并转为 ERROR 结果，不得抛出导致整轮检查中断</li>
 *   <li><b>服务端线程执行</b>：由命令处理器调度，可直接访问世界数据</li>
 * </ul>
 */
public interface SystemCheck {

    /** 命令行标识（小写），如 "storage"。 */
    String name();

    /** 报告中的显示名（本地化键，如 chat.ae2enhanced.check.storage.name）。 */
    String displayName();

    void run(MinecraftServer server, List<CheckResult> out);
}
