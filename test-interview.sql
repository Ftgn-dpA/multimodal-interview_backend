-- 测试面试记录和报告创建
-- 1. 检查是否有用户数据
SELECT * FROM users LIMIT 5;

-- 2. 检查面试记录表
SELECT * FROM interview_records LIMIT 5;

-- 3. 检查面试报告表
SELECT * FROM interview_reports LIMIT 5;

-- 4. 检查关联关系
SELECT 
    ir.id as record_id,
    ir.position,
    ir.status,
    ir.overall_score,
    irp.id as report_id,
    irp.overall_score as report_score
FROM interview_records ir
LEFT JOIN interview_reports irp ON ir.id = irp.interview_record_id
ORDER BY ir.created_at DESC
LIMIT 10; 