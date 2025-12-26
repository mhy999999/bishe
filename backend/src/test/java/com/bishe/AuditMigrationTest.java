package com.bishe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class AuditMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void createAuditTables() {
        // 1. 创建统一审核表
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_audit (" +
                "audit_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "business_type VARCHAR(50) NOT NULL COMMENT '业务类型: QUALITY, MAINTENANCE, SALES'," +
                "business_id VARCHAR(100) NOT NULL COMMENT '业务ID'," +
                "apply_user VARCHAR(50) COMMENT '申请人'," +
                "apply_time DATETIME COMMENT '申请时间'," +
                "status INT DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回'," +
                "auditor VARCHAR(50) COMMENT '审核人'," +
                "audit_time DATETIME COMMENT '审核时间'," +
                "audit_opinion VARCHAR(500) COMMENT '审核意见'" +
                ")");
        System.out.println("Created sys_audit table");

        // 2. 确保销售记录表存在 (SalesRecord) - 之前没看到 SalesRecord，这里创建一下
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sales_record (" +
                "sales_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "battery_id VARCHAR(100) NOT NULL," +
                "buyer_name VARCHAR(100)," +
                "sales_price DECIMAL(10,2)," +
                "sales_date DATETIME," +
                "sales_person VARCHAR(50)," +
                "status INT DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回'" +
                ")");
        System.out.println("Created sales_record table");
        
        // 3. 为 maintenance_record 添加 status 字段
        try {
            jdbcTemplate.execute("ALTER TABLE maintenance_record ADD COLUMN status INT DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回'");
            System.out.println("Added status to maintenance_record");
        } catch (Exception e) {
             System.out.println("status column might already exist in maintenance_record");
        }
    }
}
