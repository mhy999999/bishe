package com.bishe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.service.ISalesRecordService;
import com.bishe.service.ISysAuditService;

@SpringBootTest
public class AuditMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ISalesRecordService salesRecordService;
    @Autowired
    private ISysAuditService sysAuditService;

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
                "status INT DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回'," +
                "audit_opinion VARCHAR(500) COMMENT '审核意见'," +
                "material_desc VARCHAR(500) COMMENT '材料说明'," +
                "material_url TEXT COMMENT '材料文件URL(可为JSON数组)'" +
                ")");
        System.out.println("Created sales_record table");
        try {
            jdbcTemplate.execute("ALTER TABLE sales_record ADD COLUMN audit_opinion VARCHAR(500) COMMENT '审核意见'");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE sales_record ADD COLUMN material_desc VARCHAR(500) COMMENT '材料说明'");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE sales_record ADD COLUMN material_url TEXT COMMENT '材料文件URL(可为JSON数组)'");
        } catch (Exception ignored) {
        }
        salesRecordService.page(new Page<>(1, 10));
        
        // 3. 确保 maintenance_record 表存在并补齐字段
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS maintenance_record (" +
                "record_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "battery_id VARCHAR(100) NOT NULL," +
                "station_id BIGINT," +
                "fault_type VARCHAR(50) NOT NULL," +
                "description VARCHAR(500)," +
                "solution VARCHAR(500)," +
                "replace_parts VARCHAR(200)," +
                "maintainer VARCHAR(50) NOT NULL," +
                "create_time DATETIME," +
                "tx_hash VARCHAR(66)," +
                "status INT DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回, 3-已完成'," +
                "audit_opinion VARCHAR(500) COMMENT '审核意见'," +
                "issue_material_desc VARCHAR(500) COMMENT '故障材料说明'," +
                "issue_material_url TEXT COMMENT '故障材料文件URL(可为JSON数组)'," +
                "completion_material_desc VARCHAR(500) COMMENT '完工材料说明'," +
                "completion_material_url TEXT COMMENT '完工材料文件URL(可为JSON数组)'," +
                "complete_time DATETIME" +
                ")");
        try {
            jdbcTemplate.execute("ALTER TABLE maintenance_record ADD COLUMN status INT DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回, 3-已完成'");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE maintenance_record ADD COLUMN audit_opinion VARCHAR(500) COMMENT '审核意见'");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE maintenance_record ADD COLUMN auditor VARCHAR(50) COMMENT '审核人'");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE maintenance_record ADD COLUMN audit_time DATETIME COMMENT '审核时间'");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE maintenance_record ADD COLUMN issue_material_desc VARCHAR(500) COMMENT '故障材料说明'");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE maintenance_record ADD COLUMN issue_material_url TEXT COMMENT '故障材料文件URL(可为JSON数组)'");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE maintenance_record ADD COLUMN completion_material_desc VARCHAR(500) COMMENT '完工材料说明'");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE maintenance_record ADD COLUMN completion_material_url TEXT COMMENT '完工材料文件URL(可为JSON数组)'");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE maintenance_record ADD COLUMN complete_time DATETIME");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void maintenanceAuditShouldSyncAuditorFields() {
        String batteryId = "TEST-MAINT-" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO maintenance_record (battery_id, station_id, fault_type, description, maintainer, create_time, status, issue_material_desc, issue_material_url) " +
                        "VALUES (?, 1, 'TEST', 'TEST', 'tester', NOW(), 0, 'TEST', '[]')",
                batteryId
        );
        Long recordId = jdbcTemplate.queryForObject(
                "SELECT record_id FROM maintenance_record WHERE battery_id = ? ORDER BY record_id DESC LIMIT 1",
                Long.class,
                batteryId
        );
        Assertions.assertNotNull(recordId);

        jdbcTemplate.update(
                "INSERT INTO sys_audit (business_type, business_id, apply_user, apply_time, status) VALUES ('MAINTENANCE', ?, 'tester', NOW(), 0)",
                recordId.toString()
        );
        Long auditId = jdbcTemplate.queryForObject(
                "SELECT audit_id FROM sys_audit WHERE business_type='MAINTENANCE' AND business_id=? ORDER BY audit_id DESC LIMIT 1",
                Long.class,
                recordId.toString()
        );
        Assertions.assertNotNull(auditId);

        sysAuditService.doAudit(auditId, 1, "ok", "auditor1");

        java.util.Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, audit_opinion, auditor, audit_time FROM maintenance_record WHERE record_id=?",
                recordId
        );
        Assertions.assertEquals(1, ((Number) row.get("status")).intValue());
        Assertions.assertEquals("ok", row.get("audit_opinion"));
        Assertions.assertEquals("auditor1", row.get("auditor"));
        Assertions.assertNotNull(row.get("audit_time"));
    }
}
