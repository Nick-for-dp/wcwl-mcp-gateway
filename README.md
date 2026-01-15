# WCWL MCP Gateway

MCP（Model Context Protocol）工具网关服务，提供统一的工具注册、发现和执行能力。

## 项目简介

WCWL MCP Gateway 是一个基于 Spring Boot 3.2 构建的工具网关平台，主要功能包括：

- **工具注册** - 支持静态注册（@Component）和动态注册（API）
- **工具发现** - 提供工具清单接口，返回所有可用工具及其参数定义
- **工具执行** - 统一的工具调用入口，支持参数校验和结果返回
- **权限控制** - 基于 JWT 的认证和基于角色的授权（RBAC）
- **可观测性** - 集成 Spring Actuator，提供健康检查和指标监控

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | LTS 版本 |
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Security | 6.x | 安全认证 |
| JWT (jjwt) | 0.11.5 | Token 认证 |
| Lombok | - | 代码简化 |
| MyBatis | 3.0.3 | ORM 框架（预留） |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 构建运行

```bash
# 克隆项目
git clone <repository-url>
cd wcwl-mcp-gateway

# 编译
mvn clean compile

# 运行（开发环境）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 打包
mvn clean package -DskipTests

# 运行 JAR
java -jar target/wcwl-mcp-gateway-1.0.0.jar
```

### 默认配置

- 服务端口：`8080`
- JWT 过期时间：`24 小时`

## API 接口

### 认证说明

除 `/actuator/**` 外，所有 `/mcp/**` 接口需要 JWT 认证：

```http
Authorization: Bearer <your-jwt-token>
```

### 预置测试用户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | ADMIN, USER |
| user | user123 | USER, WAREHOUSE_VIEWER |

### 接口列表

#### 1. 获取工具清单

```http
GET /mcp/manifest
Authorization: Bearer <token>
```

响应示例：
```json
{
  "tools": [
    {
      "name": "get_warehouse_inventory",
      "description": "查询仓库库存",
      "inputSchema": {
        "type": "object",
        "properties": {
          "sku": {
            "type": "string",
            "description": "商品SKU编码"
          }
        },
        "required": ["sku"]
      }
    }
  ]
}
```

#### 2. 执行工具

```http
POST /mcp/tools/{toolName}
Authorization: Bearer <token>
Content-Type: application/json

{
  "sku": "SKU001"
}
```

响应示例：
```json
{
  "result": {
    "sku": "SKU001",
    "totalQuantity": 350,
    "warehouses": [
      {
        "warehouseId": "WH001",
        "warehouseName": "北京仓",
        "quantity": 150,
        "availableQuantity": 120
      }
    ]
  }
}
```

#### 3. 动态注册工具（仅 ADMIN）

第三方用户可以通过 API 注册自己的服务为 MCP 工具，无需修改代码：

```http
POST /admin/tools/register
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "query_trade_data",
  "description": "查询贸易数据",
  "endpoint": "http://api.example.com/trade/query",
  "method": "POST",
  "params": [
    {"name": "startDate", "type": "string", "required": true, "description": "开始日期"},
    {"name": "endDate", "type": "string", "required": true, "description": "结束日期"},
    {"name": "tradeType", "type": "string", "required": false, "description": "贸易类型"}
  ],
  "headers": {
    "X-API-Key": "your-api-key"
  },
  "timeout": 30000,
  "requiredRoles": ["TRADE_VIEWER"]
}
```

参数说明：

| 字段 | 必填 | 说明 |
|------|------|------|
| name | 是 | 工具名称，小写字母+数字+下划线 |
| description | 否 | 工具描述 |
| endpoint | 是 | 第三方服务 URL |
| method | 否 | HTTP 方法，默认 POST |
| params | 否 | 参数定义列表 |
| headers | 否 | 自定义请求头 |
| timeout | 否 | 超时时间（毫秒），默认 30000 |
| requiredRoles | 否 | 所需角色，为空则无限制 |

params 中每个参数的定义：

| 字段 | 必填 | 说明 |
|------|------|------|
| name | 是 | 参数名 |
| type | 否 | 类型：string/integer/number/boolean，默认 string |
| required | 否 | 是否必填，默认 false |
| description | 否 | 参数描述 |

#### 4. 注销工具（仅 ADMIN）

```http
DELETE /admin/tools/{toolName}
Authorization: Bearer <admin-token>
```

#### 5. 查看已注册工具列表（仅 ADMIN）

```http
GET /admin/tools
Authorization: Bearer <admin-token>
```

#### 6. 健康检查

```http
GET /actuator/health
```

## 项目结构

```
src/main/java/com/wcwl/mcpgateway/
├── common/                          # 通用组件
│   ├── constant/                    # 常量定义
│   │   └── ErrorCodes.java          # 错误码常量
│   ├── error/                       # 异常处理
│   │   └── GlobalExceptionHandler.java
│   ├── exception/                   # 自定义异常
│   │   └── McpToolException.java
│   └── logging/                     # 日志工具
│       └── McpToolLogger.java
├── config/                          # 配置类
│   ├── filter/                      # 过滤器
│   │   └── JwtAuthFilter.java       # JWT 认证过滤器
│   ├── ObservabilityConfig.java     # 可观测性配置
│   ├── SecurityConfig.java          # 安全配置
│   └── WebConfig.java               # Web 配置
├── controller/                      # 控制器
│   ├── admin/                       # 管理接口
│   │   └── ToolAdminController.java
│   ├── ManifestController.java      # 工具清单
│   └── McpToolController.java       # 工具执行
├── dto/                             # 数据传输对象
│   ├── request/                     # 请求 DTO
│   │   └── ToolRegisterRequest.java
│   └── response/                    # 响应 DTO
│       ├── McpErrorResponse.java
│       ├── McpManifestResponse.java
│       └── McpSuccessResponse.java
├── model/                           # 数据模型
│   └── mcp/
│       ├── JsonObjectSchema.java    # Schema 构建工具
│       └── McpTool.java             # 工具接口定义
├── service/                         # 服务层
│   ├── auth/                        # 认证服务
│   │   ├── JwtTokenService.java     # JWT 服务
│   │   └── UserService.java         # 用户服务
│   └── tool/                        # 工具服务
│       ├── ToolRegistry.java        # 工具注册中心实现
│       └── ToolRegistryService.java # 工具注册中心接口
├── tools/                           # 工具实现
│   ├── BaseTool.java                # 工具基类
│   └── builtin/                     # 内置工具
│       └── SampleWarehouseTool.java # 示例工具
└── WcwlMcpGatewayApplication.java   # 启动类
```

## 开发指南

### 创建新工具

1. 继承 `BaseTool` 类
2. 添加 `@Component` 注解
3. 实现必要方法

```java
@Component
public class MyTool extends BaseTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getName() {
        return "my_tool";
    }

    @Override
    public String getDescription() {
        return "我的工具描述";
    }

    @Override
    public JsonNode getInputSchema() {
        return new JsonObjectSchema()
                .addStringProperty("param1", "参数1描述")
                .addIntegerProperty("param2", "参数2描述")
                .setRequired(List.of("param1"))
                .toJsonNode();
    }

    @Override
    public Set<String> getRequiredRoles() {
        // 返回空集合表示无角色限制
        // 返回 Set.of("ADMIN") 表示需要 ADMIN 角色
        return Set.of();
    }

    @Override
    protected JsonNode doExecute(Map<String, Object> args, Authentication auth) {
        String param1 = (String) args.get("param1");
        Integer param2 = (Integer) args.get("param2");
        
        // 业务逻辑...
        
        return MAPPER.valueToTree(Map.of(
                "result", "处理结果"
        ));
    }
}
```

### 错误处理

使用 `McpToolException` 抛出业务异常：

```java
throw new McpToolException(ErrorCodes.INVALID_PARAM, "参数不能为空", 400);
```

常用错误码（定义在 `ErrorCodes` 类）：

| 错误码 | 说明 |
|--------|------|
| `unauthorized` | 未认证 |
| `forbidden` | 权限不足 |
| `tool_not_found` | 工具不存在 |
| `invalid_param` | 参数无效 |
| `tool_execution_error` | 执行错误 |

## 配置说明

### application.yml

```yaml
server:
  port: 8080

jwt:
  secret: your-secret-key-at-least-32-characters
  expiration: 86400000  # 24小时（毫秒）

logging:
  level:
    com.wcwl.mcpgateway: DEBUG
```

### 环境配置

| Profile | 说明 |
|---------|------|
| dev | 开发环境，详细日志 |
| prod | 生产环境，从环境变量读取敏感配置 |

生产环境需设置环境变量：
```bash
export JWT_SECRET=your-production-secret-key
```

## 安全说明

- JWT 密钥在生产环境必须使用强密钥（至少 32 字符）
- 默认用户仅用于开发测试，生产环境需接入数据库
- `/admin/**` 接口仅 ADMIN 角色可访问
- 动态注册工具存在安全风险，生产环境建议限制可加载的类

## 待完善功能

- [ ] 登录接口（获取 JWT Token）
- [ ] 用户管理（数据库存储）
- [ ] 工具版本管理
- [ ] API 限流
- [ ] Swagger/OpenAPI 文档
- [ ] 工具执行日志持久化

## 许可证

MIT License
