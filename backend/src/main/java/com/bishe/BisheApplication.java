package com.bishe;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@MapperScan("com.bishe.mapper")
public class BisheApplication {

    public static void main(String[] args) {
        SpringApplication.run(BisheApplication.class, args);
    }

    @Bean
    public CommandLineRunner schemaFixRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE sys_user MODIFY COLUMN password VARCHAR(128)");
            } catch (Exception ignored) {
            }
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
        };
    }
}
