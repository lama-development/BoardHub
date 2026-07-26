package it.uniupo.boardhub.eventservice.support;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

public final class MigratedTestDatabase {

    private MigratedTestDatabase() {
    }

    // Crea un database H2 isolato applicando le stesse migrazioni dell'ambiente reale.
    public static JdbcTemplate create() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:boardhub_"
                + UUID.randomUUID().toString().replace("-", "")
                + ";DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .schemas("game_schema")
                .defaultSchema("game_schema")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .cleanDisabled(true)
                .load()
                .migrate();
        return new JdbcTemplate(dataSource);
    }
}
