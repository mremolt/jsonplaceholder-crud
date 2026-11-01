package gmbh.tantive.jsonplaceholder_crud.importer;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JsonPlaceholderHttpClientConfiguration {

    @Bean
    HttpClient jsonPlaceholderHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }
}