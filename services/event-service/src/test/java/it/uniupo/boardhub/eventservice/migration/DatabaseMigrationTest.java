package it.uniupo.boardhub.eventservice.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationTest {

    @Test
    void creaLoSchemaSuUnDatabaseVuoto() {
        DataSource dataSource = createDataSource();

        Flyway flyway = createFlyway(dataSource);
        flyway.migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        assertThat(countRows(jdbcTemplate, "game_schema.game_events")).isZero();
        assertThat(countRows(jdbcTemplate, "game_schema.game_sessions")).isZero();
        assertThat(countRows(jdbcTemplate, "game_schema.game_grid_cells")).isZero();
        assertThat(countRows(jdbcTemplate, "game_schema.game_grid_walls")).isZero();
        assertThat(countRows(jdbcTemplate, "game_schema.game_grid_traps")).isZero();
        assertThat(countRows(jdbcTemplate, "game_schema.game_tables")).isZero();
        assertThat(countRows(jdbcTemplate, "game_schema.session_join_requests")).isZero();
        assertThat(countRows(jdbcTemplate, "game_schema.session_participants")).isZero();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }

    @Test
    void registraLaBaselineSenzaCancellareUnoSchemaEsistente() {
        DataSource dataSource = createDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE SCHEMA game_schema");
        jdbcTemplate.execute("CREATE TABLE game_schema.existing_data (id INTEGER PRIMARY KEY)");
        jdbcTemplate.update("INSERT INTO game_schema.existing_data (id) VALUES (1)");

        Flyway flyway = createFlyway(dataSource);
        flyway.migrate();

        assertThat(countRows(jdbcTemplate, "game_schema.existing_data")).isEqualTo(1);
        assertThat(countRows(jdbcTemplate, "game_schema.game_sessions")).isZero();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }

    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:flyway_"
                + UUID.randomUUID().toString().replace("-", "")
                + ";DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private Flyway createFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .schemas("game_schema")
                .defaultSchema("game_schema")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .cleanDisabled(true)
                .load();
    }

    private int countRows(JdbcTemplate jdbcTemplate, String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
