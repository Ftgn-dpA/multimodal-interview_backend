# MySQL 数据库设置指南

## 1. 安装 MySQL

### Windows
1. 下载 MySQL Community Server: https://dev.mysql.com/downloads/mysql/
2. 安装时设置 root 密码为 `root`
3. 或者安装后修改密码

### macOS
```bash
brew install mysql
brew services start mysql
```

### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install mysql-server
sudo mysql_secure_installation
```

## 2. 创建数据库

1. 登录 MySQL:
```bash
mysql -u root -p
```

2. 执行初始化脚本:
```sql
source database-init.sql
```

或者手动创建:
```sql
CREATE DATABASE interview_simulator CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 3. 配置应用

如果使用不同的用户名/密码，请修改 `application.yml`:

```yaml
spring:
  datasource:
    username: your_username
    password: your_password
```

## 4. 启动应用

```bash
mvn spring-boot:run
```

应用启动后会自动创建表结构。

## 5. 验证

- 注册新用户
- 登录
- 数据应该持久化保存 