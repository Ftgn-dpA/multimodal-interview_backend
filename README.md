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