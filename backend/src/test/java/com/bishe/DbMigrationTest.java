package com.bishe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class DbMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void addQualityColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE quality_inspection ADD COLUMN status INT DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已驳回'");
            System.out.println("Added status column");
        } catch (Exception e) {
            System.out.println("status column might already exist: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE quality_inspection ADD COLUMN audit_opinion VARCHAR(255) COMMENT '审核意见'");
            System.out.println("Added audit_opinion column");
        } catch (Exception e) {
            System.out.println("audit_opinion column might already exist: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE quality_inspection ADD COLUMN auditor VARCHAR(50) COMMENT '审核人'");
            System.out.println("Added auditor column");
        } catch (Exception e) {
            System.out.println("auditor column might already exist: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE quality_inspection ADD COLUMN audit_time DATETIME COMMENT '审核时间'");
            System.out.println("Added audit_time column");
        } catch (Exception e) {
            System.out.println("audit_time column might already exist: " + e.getMessage());
        }
    }
}
