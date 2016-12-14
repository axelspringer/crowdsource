package de.asideas.crowdsource;

import de.asideas.crowdsource.config.SchedulerConfig;
import de.asideas.crowdsource.config.SecurityConfig;
import de.asideas.crowdsource.config.WebConfig;
import de.asideas.crowdsource.config.mail.MailSenderConfig;
import de.asideas.crowdsource.config.mail.MailTemplateConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({SecurityConfig.class, MailSenderConfig.class, MailTemplateConfig.class, SchedulerConfig.class, WebConfig.class})
@ComponentScan(excludeFilters = @ComponentScan.Filter(Configuration.class), basePackageClasses = CrowdSourceAutoConfiguration.class)
@EnableAutoConfiguration
public class CrowdSourceAutoConfiguration {
}
