# 后端详细说明文档

## 📁 文件结构说明

### 核心文件
- **InterviewSimulatorApplication.java**: Spring Boot启动类
- **application.yml**: 应用配置文件（数据库、JWT、端口等）

### 控制器层 (controller/)
- **AuthController.java**: 用户认证控制器
  - `/api/auth/register`: 用户注册
  - `/api/auth/login`: 用户登录
  - `/api/auth/validate`: JWT验证
- **InterviewController.java**: 面试控制器
  - `/api/interview/start`: 开始面试
  - `/api/interview/question`: 获取问题
  - `/api/interview/answer`: 提交答案
  - `/api/interview/score`: 获取评分
- **GlobalExceptionHandler.java**: 全局异常处理

### 服务层 (service/)
- **UserService.java**: 用户业务逻辑
- **LargeModelService.java**: AI模型服务（模拟）
- **CustomUserDetailsService.java**: Spring Security用户服务

### 数据模型 (model/)
- **User.java**: 用户实体
- **InterviewRecord.java**: 面试记录实体
- **AuthRequest.java**: 认证请求DTO
- **JwtResponse.java**: JWT响应DTO
- **QuestionResponse.java**: 问题响应DTO
- **AnswerRequest.java**: 答案请求DTO
- **ScoreResponse.java**: 评分响应DTO
- **ScoreFeedback.java**: 评分反馈DTO

### 数据访问层 (repository/)
- **UserRepository.java**: 用户数据访问
- **InterviewRecordRepository.java**: 面试记录数据访问

### 配置层 (config/)
- **SecurityConfig.java**: 安全配置
- **JwtAuthenticationFilter.java**: JWT认证过滤器

### 工具类 (util/)
- **JwtUtil.java**: JWT工具类

## 🚀 启动步骤

1. 创建MySQL数据库
2. 配置application.yml
3. 运行 `mvn spring-boot:run`
4. 访问 http://localhost:8080 

# Avatar 虚拟人服务集成说明

本项目已集成基于讯飞 SparkOS 平台的虚拟人推流服务，后端采用 Java Spring Boot 实现，严格参照官方 Python demo 逻辑。

## 目录结构

- `controller/AvatarController.java`：RESTful API 控制器
- `service/AvatarService.java`：业务逻辑层
- `ws/AvatarWebSocketClient.java`：WebSocket 客户端，负责与讯飞平台通信
- `util/AuthUtil.java`：鉴权工具类
- `config/AvatarConfig.java`：参数配置

## 配置

在 `src/main/resources/application.yml` 添加如下配置：

```yaml
avatar:
  app_id: 你的appid
  api_key: 你的apikey
  api_secret: 你的apisecret
```

## 依赖

Maven 添加依赖：

```xml
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.3</version>
</dependency>
```

## 接口说明

### 1. 启动虚拟人推流
- `POST /api/avatar/start`
- 功能：启动推流，控制台打印原始返回
- 返回：推流结束/失败信息

## 启动方式

1. 配置好 `application.yml`，填写讯飞平台参数和音频路径。
2. 启动 Spring Boot 服务。
3. 通过 Postman 或前端调用 `POST /api/avatar/start`。

## 备注
- 代码严格参照 Python demo 的参数、流程、帧控制、鉴权、推流逻辑。
- 可根据需要扩展 send/stop 等接口。
- 如需支持音频/文本交互、会话管理等，可进一步扩展。 