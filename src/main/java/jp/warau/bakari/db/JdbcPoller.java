package jp.warau.bakari.db;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JdbcPoller {

    private final JdbcClient jdbcClient;

    public JdbcPoller(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public String fetchViolently() {
        String thing = jdbcClient.sql("SELECT thing1 FROM FUN_STUFF WHERE ID = 1")
                .query(String.class)
                .optional()
                .orElse("999");

        return thing;
    }
}
