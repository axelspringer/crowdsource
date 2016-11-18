package de.asideas.crowdsource;

import de.asideas.crowdsource.config.SchedulerConfig;
import de.asideas.crowdsource.config.SecurityConfig;
import de.asideas.crowdsource.config.WebConfig;
import de.asideas.crowdsource.config.mail.MailSenderConfig;
import de.asideas.crowdsource.config.mail.MailTemplateConfig;
import org.springframework.context.annotation.*;

@Configuration
@Import({SecurityConfig.class, MailSenderConfig.class, MailTemplateConfig.class, SchedulerConfig.class, WebConfig.class})
@ComponentScan(excludeFilters = @ComponentScan.Filter(Configuration.class))
@PropertySources({
    @PropertySource(value = "build.properties", ignoreResourceNotFound = true),
    @PropertySource(value = "build.core.properties")
})
public class CrowdSource {


}
