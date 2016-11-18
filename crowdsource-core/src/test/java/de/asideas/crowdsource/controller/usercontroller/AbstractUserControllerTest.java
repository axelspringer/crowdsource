package de.asideas.crowdsource.controller.usercontroller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.asideas.crowdsource.controller.ControllerExceptionAdvice;
import de.asideas.crowdsource.controller.UserController;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.repository.FinancingRoundRepository;
import de.asideas.crowdsource.repository.UserRepository;
import de.asideas.crowdsource.service.UserService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.expression.Expression;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AbstractUserControllerTest.Config.class)
public abstract class AbstractUserControllerTest {

    protected static final String NEW_USER_MAIL_ADDRESS = "new@crowd.source.de";
    protected static final String EXISTING_BUT_NOT_YET_ACTIVATED_USER_MAIL_ADDRESS = "existing.not.yet.activated@crowd.source.de";
    protected static final String FIRSTNAME = "firstname";
    protected static final String LASTNAME = "lastname";
    protected static final String ACTIVATED_USER_MAIL_ADDRESS = "existing.and.activated@crowd.source.de";
    protected static final String INVALID_USER_MAIL_ADDRESS = "test@test.de";
    protected static final String ENCODED_PASSWORD = "3nc0d3d";

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UserNotificationService userNotificationService;

    @Autowired
    protected FinancingRoundRepository financingRoundRepository;

    @Resource
    protected WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;
    protected ObjectMapper mapper = new ObjectMapper();
    protected UserEntity existingButNotYetActivatedUser;
    protected UserEntity activatedUser;

    @Before
    public void setup() {

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        reset(userNotificationService);
        reset(userRepository);
        reset(financingRoundRepository);

        when(userRepository.findByEmail(eq(NEW_USER_MAIL_ADDRESS))).thenReturn(null);

        existingButNotYetActivatedUser = new UserEntity(EXISTING_BUT_NOT_YET_ACTIVATED_USER_MAIL_ADDRESS, FIRSTNAME, LASTNAME);
        existingButNotYetActivatedUser.setId(0L);
        existingButNotYetActivatedUser.setActivationToken("activationToken");
        when(userRepository.findByEmail(eq(EXISTING_BUT_NOT_YET_ACTIVATED_USER_MAIL_ADDRESS))).thenReturn(existingButNotYetActivatedUser);

        activatedUser = new UserEntity(ACTIVATED_USER_MAIL_ADDRESS, FIRSTNAME, LASTNAME);
        activatedUser.setActivated(true);
        activatedUser.setActivationToken("");
        activatedUser.setBudget(BigDecimal.valueOf(500));
        when(userRepository.findByEmail(eq(ACTIVATED_USER_MAIL_ADDRESS))).thenReturn(activatedUser);

        reset(passwordEncoder);
        when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);
    }

    @Configuration
    @EnableWebMvc
    static class Config {

        @Bean
        public ControllerExceptionAdvice controllerExceptionAdvice() {
            return new ControllerExceptionAdvice();
        }

        @Bean
        public UserController userController() {
            return new UserController();
        }

        @Bean
        public UserNotificationService userActivationService() {
            return mock(UserNotificationService.class);
        }

        @Bean
        public FinancingRoundRepository financingRoundRepository() {
            return mock(FinancingRoundRepository.class);
        }

        @Bean
        public UserService userService(UserRepository userRepository, UserNotificationService userNotificationService) {
            return new UserService(userRepository, userNotificationService);
        }

        @Bean
        public JavaMailSender javaMailSender() {
            // this is here to make the app context to boot up (transitive dependency of UserActivationService)
            return mock(JavaMailSender.class);
        }

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws IOException {
            PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
            propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(Boolean.FALSE);
            propertySourcesPlaceholderConfigurer.setLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:*.properties"));
            return propertySourcesPlaceholderConfigurer;
        }

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return mock(PasswordEncoder.class);
        }

        @Bean
        public Expression activationEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression newProjectEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression passwordForgottenEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression projectPublishedEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression projectRejectedEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression projectDeferredEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression projectModifiedEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public Expression projectCommentedEmailTemplate() {
            return mock(Expression.class);
        }

        @Bean
        public AsyncTaskExecutor taskExecutorSmtp() {
            return mock(AsyncTaskExecutor.class);
        }
    }
}