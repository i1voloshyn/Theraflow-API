package dev.ivanvoloshyn.theraflowapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TheraflowApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TheraflowApiApplication.class, args);
    }

}
