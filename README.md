# 后端详细说明文档

## 📁 文件结构说明

### 核心文件
- **InterviewSimulatorApplication.java**: Spring Boot启动类
- **application.yml**: 应用配置文件（数据库、JWT、虚拟人配置等）

### 控制器层 (controller/)
- **AuthController.java**: 用户认证控制器
  - `/api/auth/register`: 用户注册
  - `/api/auth/login`: 用户登录
  - `/api/auth/validate`: JWT验证
- **InterviewController.java**: 面试控制器
  - `/api/interview/start`: 开始面试
  - `/api/interview/end`: 结束面试
  - `/api/interview/types`: 获取面试类型
  - `/api/interview/history`: 获取历史记录
- **AvatarController.java**: 虚拟人控制器
  - `/api/avatar/start`: 启动虚拟人
  - `/api/avatar/send`: 发送消息（大模型交互）
  - `/api/avatar/stop`: 关闭虚拟人
- **GlobalExceptionHandler.java**: 全局异常处理
- **PythonController.java**:音频视频模态分析脚本控制器
  - `/api/python/analyze`: 得到分析结果

### 服务层 (service/)
- **UserService.java**: 用户业务逻辑
- **LargeModelService.java**: AI模型服务
- **AvatarService.java**: 虚拟人服务（核心）
- **AiResponseService.java**: AI回复缓存和保存服务（新增）
- **CustomUserDetailsService.java**: Spring Security用户服务
- **PythonScriptService.java**: 运行模态分析脚本

### 数据模型 (model/)
- **User.java**: 用户实体
- **InterviewRecord.java**: 面试记录实体
- **AiResponse.java**: AI回复实体（新增）
- **InterviewType.java**: 面试类型枚举
- **AuthRequest.java**: 认证请求DTO
- **JwtResponse.java**: JWT响应DTO
- **QuestionResponse.java**: 问题响应DTO
- **AnswerRequest.java**: 答案请求DTO
- **ScoreResponse.java**: 评分响应DTO
- **ScoreFeedback.java**: 评分反馈DTO

### 数据访问层 (repository/)
- **UserRepository.java**: 用户数据访问
- **InterviewRecordRepository.java**: 面试记录数据访问
- **AiResponseRepository.java**: AI回复数据访问（新增）

### 配置层 (config/)
- **SecurityConfig.java**: 安全配置
- **AvatarConfig.java**: 虚拟人配置
- **JwtAuthenticationFilter.java**: JWT认证过滤器

### WebSocket (ws/)
- **AvatarWebSocketClient.java**: 虚拟人WebSocket客户端

### 工具类 (util/)
- **JwtUtil.java**: JWT工具类
- **AuthUtil.java**: 讯飞认证工具类

## 🚀 启动步骤

1. 创建MySQL数据库
2. 配置application.yml（包括虚拟人配置）
3. 运行 `mvn spring-boot:run`
4. 访问 http://localhost:8080 

## 🔧 虚拟人服务配置

### 讯飞平台配置
在 `src/main/resources/application.yml` 中配置：

```yaml
avatar:
  app_id: 你的appid
  api_key: 你的apikey
  api_secret: 你的apisecret
  avatar_id: 你的avatarId
  vcn: 你的vcn
  scene_id: 你的scene_id

  # python脚本配置
python:
  interpreter: D:/miniconda3/envs/ship/python.exe，改成你的python运行环境
  env: python3.9+torch+ffmpeg
```

### 依赖配置
Maven 依赖：

```xml
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.3</version>
</dependency>
```

## 📡 API接口说明

### 虚拟人相关接口

#### 1. 启动虚拟人
- **接口**: `POST /api/avatar/start`
- **功能**: 启动虚拟人会话，建立WebSocket连接
- **返回**: 
  ```json
  {
    "api_url": "https://rtc-api.xf-yun.com/v1/rtc/play/",
    "session": "session_id",
    "stream_url": "webrtc://...",
    "status": "ok"
  }
  ```

#### 2. 发送消息
- **接口**: `POST /api/avatar/send`
- **参数**: 
  - `sessionId`: 会话ID
  - `text`: 消息内容
- **功能**: 发送消息给虚拟人，触发大模型交互
- **返回**: 
  ```json
  {
    "status": "ok",
    "msg": "消息已发送"
  }
  ```

#### 3. 关闭虚拟人
- **接口**: `POST /api/avatar/stop`
- **参数**: `sessionId`: 会话ID
- **功能**: 关闭虚拟人会话，释放资源
- **返回**: 
  ```json
  {
    "status": "ok",
    "msg": "avatar会话已关闭"
  }
  ```

## 🤖 AI回复保存功能

### 核心特性
- **流式分片合并**: 自动合并同一轮AI回复的多个流式片段
- **JSON数组存储**: 每轮完整AI回复作为JSON数组的一个元素
- **批量保存**: 面试结束时一次性保存所有轮次的AI回复
- **内存缓存**: 面试过程中AI回复缓存在内存，避免频繁数据库操作

### 工作流程
1. **分片接收**: WebSocket接收AI回复的流式分片
2. **分片缓存**: 按`request_id`分组缓存分片
3. **片段合并**: 当`status=2`时合并该轮所有分片为完整回复
4. **完整回复缓存**: 将完整回复添加到面试的回复数组
5. **批量保存**: 面试结束时将回复数组保存为JSON格式

### 数据库存储格式
```json
[
  "第1轮完整AI回复内容",
  "第2轮完整AI回复内容", 
  "第3轮完整AI回复内容"
]
```

### 查询示例
```sql
-- 查询第1轮AI回复
SELECT JSON_EXTRACT(ai_response, '$[0]') FROM ai_responses WHERE interview_record_id = 123;

-- 查询所有轮次
SELECT JSON_EXTRACT(ai_response, '$[*]') FROM ai_responses WHERE interview_record_id = 123;

-- 查询回复轮数
SELECT JSON_LENGTH(ai_response) FROM ai_responses WHERE interview_record_id = 123;
```

## 🔄 虚拟人工作流程

1. **启动阶段**: 
   - 创建WebSocket连接到讯飞平台
   - 发送start协议，配置虚拟人参数
   - 等待stream_url返回

2. **交互阶段**:
   - 接收用户消息
   - 发送text_interact协议
   - 虚拟人基于大模型生成回复
   - **AI回复自动缓存和合并**（新增）

3. **关闭阶段**:
   - 发送stop协议
   - 关闭WebSocket连接
   - 清理会话资源
   - **批量保存AI回复到数据库**（新增）

## 🛠️ 技术特点

- **多会话管理**: 支持多个用户同时使用虚拟人
- **自动资源管理**: 自动清理过期会话
- **错误处理**: 完善的异常处理和重连机制
- **协议兼容**: 严格遵循讯飞官方协议规范
- **流式处理**: 支持AI回复的流式分片合并
- **JSON存储**: 使用MySQL JSON类型存储AI回复数组
- **双缓存机制**: 分片缓存 + 完整回复缓存，提高性能 