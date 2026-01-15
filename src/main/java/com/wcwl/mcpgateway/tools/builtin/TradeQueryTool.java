package com.wcwl.mcpgateway.tools.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wcwl.mcpgateway.model.mcp.JsonObjectSchema;
import com.wcwl.mcpgateway.tools.BaseTool;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 贸易数据查询工具
 * 
 * <p>提供贸易数据的查询功能，支持按日期范围和贸易类型筛选。</p>
 */
@Component
public class TradeQueryTool extends BaseTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 工具名称（用于 API 调用）
     * 调用方式：POST /mcp/tools/query_trade_data
     */
    @Override
    public String getName() {
        return "query_trade_data";
    }

    @Override
    public String getDescription() {
        return "查询贸易数据，支持按日期范围和贸易类型筛选";
    }

    /**
     * 定义输入参数
     * 
     * 示例请求：
     * {
     *   "startDate": "2024-01-01",
     *   "endDate": "2024-01-31",
     *   "tradeType": "export"
     * }
     */
    @Override
    public JsonNode getInputSchema() {
        return new JsonObjectSchema()
                .addStringProperty("startDate", "开始日期，格式：yyyy-MM-dd")
                .addStringProperty("endDate", "结束日期，格式：yyyy-MM-dd")
                .addStringProperty("tradeType", "贸易类型：export(出口)/import(进口)/all(全部)")
                .setRequired(List.of("startDate", "endDate"))
                .toJsonNode();
    }

    @Override
    public Set<String> getRequiredRoles() {
        // 如需限制权限，可以返回 Set.of("TRADE_VIEWER")
        return Set.of();
    }

    /**
     * 执行查询逻辑
     */
    @Override
    protected JsonNode doExecute(Map<String, Object> args, Authentication auth) {
        // 1. 获取参数
        String startDate = (String) args.get("startDate");
        String endDate = (String) args.get("endDate");
        String tradeType = (String) args.getOrDefault("tradeType", "all");

        // 2. 执行业务逻辑（这里用 Mock 数据，实际应查询数据库）
        List<Map<String, Object>> tradeRecords = List.of(
                Map.of(
                        "tradeId", "TR20240101001",
                        "date", "2024-01-15",
                        "type", "export",
                        "product", "电子产品",
                        "amount", 150000.00,
                        "currency", "USD",
                        "destination", "美国"
                ),
                Map.of(
                        "tradeId", "TR20240101002",
                        "date", "2024-01-20",
                        "type", "import",
                        "product", "原材料",
                        "amount", 80000.00,
                        "currency", "USD",
                        "source", "德国"
                )
        );

        // 3. 返回结果
        return MAPPER.valueToTree(Map.of(
                "query", Map.of(
                        "startDate", startDate,
                        "endDate", endDate,
                        "tradeType", tradeType
                ),
                "totalCount", tradeRecords.size(),
                "totalAmount", 230000.00,
                "records", tradeRecords
        ));
    }
}
