package de.asideas.crowdsource;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CrowdSourceAutoConfiguration.class, AbstractIT.Config.class})
public abstract class AbstractIT {

    @Configuration
    public static class Config {

    }
}
