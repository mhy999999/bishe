package com.bishe;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
            try {
                jdbcTemplate.execute("ALTER TABLE chain_transaction ADD COLUMN chain_id BIGINT COMMENT '链ID'");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE chain_transaction ADD COLUMN from_address VARCHAR(42) COMMENT '发起地址'");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE chain_transaction ADD COLUMN error_message TEXT COMMENT '错误信息'");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE chain_transaction MODIFY block_height BIGINT NULL");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE chain_transaction MODIFY contract_addr VARCHAR(42) NULL");
            } catch (Exception ignored) {
            }
        };
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> filesCharsetFilter() {
        FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                String uri = request.getRequestURI();
                if (uri != null && uri.startsWith("/files/")) {
                    response.setCharacterEncoding("UTF-8");
                    HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response) {
                        @Override
                        public void setContentType(String type) {
                            if (type != null) {
                                String lower = type.toLowerCase();
                                if (lower.startsWith("text/") && !lower.contains("charset=")) {
                                    super.setContentType(type + ";charset=UTF-8");
                                    return;
                                }
                            }
                            super.setContentType(type);
                        }

                        @Override
                        public void setHeader(String name, String value) {
                            if ("content-type".equalsIgnoreCase(name) && value != null) {
                                String lower = value.toLowerCase();
                                if (lower.startsWith("text/") && !lower.contains("charset=")) {
                                    super.setHeader(name, value + ";charset=UTF-8");
                                    return;
                                }
                            }
                            super.setHeader(name, value);
                        }
                    };
                    filterChain.doFilter(request, wrapper);
                    return;
                }
                filterChain.doFilter(request, response);
            }
        });
        bean.addUrlPatterns("/files/*");
        bean.setOrder(0);
        return bean;
    }
}
