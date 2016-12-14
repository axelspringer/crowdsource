package de.asideas.crowdsource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.subethamail.wiser.Wiser;

@SpringBootApplication
public class CrowdSourceExample {

    @Value("${de.asideas.crowdsource.mail.port:1025}")
    private Integer mailServerPort;

    public static void main(String[] args) {

        SpringApplication.run(CrowdSourceExample.class, args);
    }

    @Bean
    public Wiser mailServer() {

        Wiser wiser = new Wiser(mailServerPort);
        wiser.start();
        return wiser;
    }
}
