package com.wcwl.mcpgateway.service.tool;

import com.wcwl.mcpgateway.model.mcp.McpTool;
import com.wcwl.mcpgateway.model.mcp.ToolMetadata;
import com.wcwl.mcpgateway.model.mcp.ToolStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * MCP 工具注册中心实现类
 * 
 * <p>这是 {@link ToolRegistryService} 接口的默认实现，使用内存存储工具。
 * 它就像一个"工具仓库"，其他组件可以从这里获取需要的工具。</p>
 * 
 * <h3>什么是 @Service？</h3>
 * <p>@Service 是 Spring 的注解，标记这个类是一个服务组件。
 * Spring 会自动创建这个类的实例（单例），并管理它的生命周期。
 * 其他类可以通过依赖注入来使用它。</p>
 * 
 * <h3>为什么使用 ConcurrentHashMap？</h3>
 * <p>ConcurrentHashMap 是线程安全的 Map 实现。在多线程环境下
 * （如 Web 应用同时处理多个请求），它可以安全地进行读写操作，
 * 不会出现数据不一致的问题。</p>
 * 
 * <h3>工具是如何被注册的？</h3>
 * <ol>
 *   <li>Spring 启动时，扫描所有带 @Component 注解的 McpTool 实现类</li>
 *   <li>Spring 创建这些工具的实例，并注入到 ToolRegistry 的构造函数</li>
 *   <li>构造函数遍历所有工具，调用 register() 方法注册到 toolMap</li>
 * </ol>
 * 
 * @see ToolRegistryService 服务接口定义
 * @see McpTool 工具接口
 */
@Service
public class ToolRegistry implements ToolRegistryService {

    /**
     * 日志记录器
     * 
     * <p>使用 SLF4J（Simple Logging Facade for Java）进行日志记录。
     * 它是一个日志门面，可以与多种日志实现（如 Logback、Log4j）配合使用。</p>
     */
    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    /**
     * 工具存储 Map
     * 
     * <p>key: 工具名称（如 "get_warehouse_inventory"）</p>
     * <p>value: 工具实例</p>
     * 
     * <p>使用 ConcurrentHashMap 保证线程安全，支持并发读写。</p>
     */
    private final ConcurrentMap<String, McpTool> toolMap = new ConcurrentHashMap<>();

    /**
     * 构造函数 - 自动注册所有 Spring 管理的 McpTool
     * 
     * <p>这是<b>构造函数注入</b>的典型用法。Spring 会自动查找所有
     * 实现了 McpTool 接口且被 @Component 标记的类，创建实例后
     * 作为 List 传入这个构造函数。</p>
     * 
     * <h3>为什么用构造函数注入？</h3>
     * <ul>
     *   <li>依赖明确 - 一眼就能看出这个类依赖什么</li>
     *   <li>不可变性 - 依赖在构造时确定，之后不会改变</li>
     *   <li>便于测试 - 可以轻松传入 Mock 对象进行单元测试</li>
     * </ul>
     * 
     * @param tools Spring 自动注入的所有 McpTool 实现
     */
    public ToolRegistry(List<McpTool> tools) {
        // 遍历所有工具并注册
        // forEach 是 Java 8 引入的函数式编程方法
        // this::register 是方法引用，等价于 tool -> this.register(tool)
        tools.forEach(this::register);
        log.info("ToolRegistry initialized with {} tools", toolMap.size());
    }

    /**
     * 注册工具
     * 
     * <p>将工具添加到注册中心。如果同名工具已存在，会记录警告日志
     * 但不会覆盖原有工具（使用 putIfAbsent）。</p>
     * 
     * @param tool 要注册的工具实例
     */
    @Override
    public void register(McpTool tool) {
        String name = tool.getName();
        // putIfAbsent: 如果 key 不存在则放入，返回 null
        //              如果 key 已存在则不放入，返回已存在的值
        McpTool existing = toolMap.putIfAbsent(name, tool);
        if (existing != null) {
            log.warn("Tool already registered: {}", name);
        } else {
            log.info("Tool registered: {}", name);
        }
    }

    /**
     * 根据名称获取工具
     * 
     * @param name 工具名称
     * @return 工具实例，如果不存在返回 null
     */
    @Override
    public McpTool getTool(String name) {
        return toolMap.get(name);
    }

    /**
     * 获取所有已注册的工具
     * 
     * <p>返回不可修改的集合视图，防止外部代码修改内部状态。</p>
     * 
     * @return 所有工具的集合（不可修改）
     */
    @Override
    public Collection<McpTool> getAllTools() {
        return Collections.unmodifiableCollection(toolMap.values());
    }

    /**
     * 检查工具是否存在
     * 
     * @param name 工具名称
     * @return true 如果工具存在，否则 false
     */
    @Override
    public boolean hasTool(String name) {
        return toolMap.containsKey(name);
    }

    /**
     * 注销工具
     * 
     * <p>从注册中心移除指定工具。通常用于动态卸载工具。</p>
     * 
     * @param name 要注销的工具名称
     */
    @Override
    public void unregister(String name) {
        McpTool removed = toolMap.remove(name);
        if (removed != null) {
            log.info("Tool unregistered: {}", name);
        }
    }

    /**
     * 获取所有已发布的工具
     * 
     * <p>只返回状态为 PUBLISHED 的工具，供普通用户使用。</p>
     */
    @Override
    public Collection<McpTool> getPublishedTools() {
        return toolMap.values().stream()
                .filter(tool -> tool.getMetadata().getStatus() == ToolStatus.PUBLISHED)
                .toList();
    }

    /**
     * 更新工具状态
     */
    @Override
    public boolean updateToolStatus(String name, ToolStatus status, String operator) {
        McpTool tool = toolMap.get(name);
        if (tool == null) {
            return false;
        }

        ToolMetadata metadata = tool.getMetadata();
        metadata.setStatus(status);
        metadata.setUpdatedBy(operator);
        metadata.setUpdatedAt(LocalDateTime.now());
        tool.setMetadata(metadata);

        log.info("Tool status updated: name={}, status={}, operator={}", name, status, operator);
        return true;
    }
}
