package gmbh.tantive.jsonplaceholder_crud.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full application on a random port against one Cassandra container. All integration tests share this
 * configuration, so Spring caches a single context and starts the container only once per test run.
 * The context also shares one {@code AuthRateLimiter}: tests that send many auth requests should use
 * their own client address.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cassandra.keyspace-name=jsonplaceholder",
        "spring.cassandra.schema-action=none",
        "security.jwt.secret=" + IntegrationTest.JWT_SECRET,
        "jsonplaceholder.import.enabled=false",
        "jsonplaceholder.relations.backfill.enabled=false" })
@AutoConfigureMockMvc
@Import(CassandraTestcontainerConfiguration.class)
public @interface IntegrationTest {

    /** Base64 of "test-only-signing-key-with-32-bytes". */
    String JWT_SECRET = "dGVzdC1vbmx5LXNpZ25pbmcta2V5LXdpdGgtMzItYnl0ZXM=";
}
