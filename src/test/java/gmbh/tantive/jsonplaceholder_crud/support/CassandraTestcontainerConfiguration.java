package gmbh.tantive.jsonplaceholder_crud.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.cassandra.CassandraContainer;

@TestConfiguration(proxyBeanMethods = false)
public class CassandraTestcontainerConfiguration {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    CassandraContainer cassandraContainer() {
        // Without explicit heap sizes Cassandra sizes its heap from the host RAM.
        return new CassandraContainer("cassandra:5.0")
                .withInitScript("schema.cql")
                .withEnv("MAX_HEAP_SIZE", "512M")
                .withEnv("HEAP_NEWSIZE", "128M");
    }
}
