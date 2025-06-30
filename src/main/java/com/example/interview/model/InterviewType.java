package com.example.interview.model;

public enum InterviewType {
    // 人工智能领域
    AI_ENGINEER("AI工程师", "人工智能", "专注于机器学习、深度学习、自然语言处理等技术"),
    AI_RESEARCHER("AI研究员", "人工智能", "专注于前沿AI算法研究和创新"),
    
    // 大数据领域
    DATA_ENGINEER("数据工程师", "大数据", "专注于数据处理、ETL、数据仓库等技术"),
    DATA_SCIENTIST("数据科学家", "大数据", "专注于数据分析、统计建模、商业智能等"),
    
    // 物联网领域
    IOT_ENGINEER("物联网工程师", "物联网", "专注于传感器、嵌入式系统、IoT平台开发"),
    IOT_ARCHITECT("IoT架构师", "物联网", "专注于IoT系统架构设计和优化"),
    
    // 智能系统领域
    SYSTEM_ENGINEER("系统工程师", "智能系统", "专注于系统设计、性能优化、架构规划"),
    DEVOPS_ENGINEER("DevOps工程师", "智能系统", "专注于自动化部署、监控、运维"),
    
    // 产品管理
    PRODUCT_MANAGER("产品经理", "产品管理", "专注于产品规划、需求分析、用户体验"),
    TECHNICAL_PRODUCT_MANAGER("技术产品经理", "产品管理", "专注于技术产品规划和团队协作");

    private final String position;
    private final String category;
    private final String description;

    InterviewType(String position, String category, String description) {
        this.position = position;
        this.category = category;
        this.description = description;
    }

    public String getPosition() {
        return position;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }
} 