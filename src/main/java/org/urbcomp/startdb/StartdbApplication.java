package org.urbcomp.startdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.urbcomp.startdb")
public class StartdbApplication {
    public static void main(String[] args) {
        SpringApplication.run(StartdbApplication.class, args);
    }
}
