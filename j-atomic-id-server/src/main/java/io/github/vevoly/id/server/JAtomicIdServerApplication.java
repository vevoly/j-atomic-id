package io.github.vevoly.id.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JAtomicIdServerApplication {
    public static void main(String[] args) {
        System.setProperty("chronicle.analytics.disable", "true");
        SpringApplication.run(JAtomicIdServerApplication.class, args);
    }
}
