package org.opentosca.artifacttemplates;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class OpenToscaIASpringApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(OpenToscaIASpringApplication.class, args);
    }
}
