package jp.warau.bakari;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import java.util.List;

@Slf4j
class DbInitializer {
	private static boolean initialized = false;

	@SuppressWarnings("unused")
	@Autowired
	void initializeDb(ConnectionFactory connectionFactory) {
		log.info("running initializeDb");
		if (!initialized) {
			log.info("DB not initialized, initializing");
			ResourceLoader resourceLoader = new DefaultResourceLoader();
			Resource[] scripts = new Resource[] {
					resourceLoader.getResource("classpath:schema-postgresql-test.sql")
			};
			new ResourceDatabasePopulator(scripts).populate(connectionFactory).block();
			initialized = true;
			log.info("DB initialized, exiting");
		}
	}
}

@Slf4j
@Testcontainers
@ActiveProfiles("test")
@Import(DbInitializer.class)
@SpringBootTest
class BakariApplicationTests {


	//DB is populated already by DbInitializer, relation exists
//	@BeforeEach
//	void populateTestData(@Value("classpath:schema-postgresql-test.sql") Resource testDataSql, @Autowired ConnectionFactory connectionFactory) {
//		ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator();
//		resourceDatabasePopulator.addScript(testDataSql);
//		resourceDatabasePopulator.populate(connectionFactory).block();
//	}

	@SuppressWarnings("")
	@Autowired
	private DatabaseClient client;

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
			//.withInitScript("schema-postgresql-test.sql");

	@Test
	void contextLoads() {

	}

	//@Sql(scripts = "classpath:schema-postgresql-test.sql")
	@Test
	void queryGoes() {
        log.info("db name: {}", postgres.getDatabaseName());
		String res = client.sql("SELECT 3 AS nums")
				.map(row -> row.get("nums", String.class))
				.first()
				.block();
		log.info("r2dbc random query: {}", res);
		List<String> res2 = client.sql("select * from information_schema.tables")
				.map(row -> row.get("table_name", String.class))
				.all()
				.collectList()
				.block();

        assert res2 != null;
        res2.forEach((it) -> log.info("list res: {}", it));
	}
}