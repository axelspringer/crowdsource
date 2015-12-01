package de.asideas.crowdsource.service;

import de.asideas.crowdsource.config.mail.MailTemplateConfig;
import de.asideas.crowdsource.domain.model.ProjectEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.domain.shared.ProjectStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MailTemplateConfig.class, MailTest.Config.class})
public class MailTest {

    private static final String ADMIN_EMAIL = "some.admin@email.com";

    @Autowired
    private UserNotificationService userNotificationService;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private AsyncTaskExecutor taskExecutorSmtp;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(userNotificationService, "applicationUrl", "https://crowd.asideas.de");

        reset(javaMailSender, taskExecutorSmtp);

        // Emulate async execution synchronously to immedately be able to verify invocations of javaMailSender
        doAnswer(invocation -> {
            final Runnable runnable = (Runnable) invocation.getArguments()[0];
            runnable.run();
            return null;
        }).when(taskExecutorSmtp).submit(isA(Runnable.class));
    }

    @Test
    public void testSendActivationMail() {
        UserEntity user = new UserEntity("some.one@email.com");
        user.setActivationToken("activationTok3n");

        userNotificationService.sendActivationMail(user);

        SimpleMailMessage mail = getMessageFromMailSender();
        assertThat(mail.getFrom(), is(UserNotificationService.FROM_ADDRESS));
        assertThat(mail.getTo(), arrayContaining(user.getEmail()));
        assertThat(mail.getSubject(), is(UserNotificationService.ACTIVATION_SUBJECT));
        assertThat(replaceLineBreaksIfWindows(mail.getText()), is(
                "Hallo Some One,\n\n" +
                        "Du hast Dich gerade auf der CrowdSource Platform angemeldet.\n" +
                        "Um Deine Registrierung abzuschließen, öffne bitte diesen Link und setze Dein Passwort:\n\n" +
                        "https://crowd.asideas.de#/signup/some.one@email.com/activation/activationTok3n\n\n" +
                        "Mit freundlichen Grüßen\nDein CrowdSource Team"));
    }

    @Test
    public void testSendPasswordRecoveryMail() {
        UserEntity user = new UserEntity("some.one@email.com");
        user.setActivationToken("activationTok3n");

        userNotificationService.sendPasswordRecoveryMail(user);

        SimpleMailMessage mail = getMessageFromMailSender();
        assertThat(mail.getFrom(), is(UserNotificationService.FROM_ADDRESS));
        assertThat(mail.getTo(), arrayContaining(user.getEmail()));
        assertThat(mail.getSubject(), is(UserNotificationService.PASSWORD_FORGOTTEN_SUBJECT));
        assertThat(replaceLineBreaksIfWindows(mail.getText()), is(
                "Hallo Some One,\n\n" +
                        "Du hast soeben ein neues Passwort für Dein Konto bei der CrowdSource Plattform angefordert.\n\n" +
                        "Bitte öffne diesen Link:\n\n" +
                        "https://crowd.asideas.de#/login/password-recovery/some.one@email.com/activation/activationTok3n\n\n" +
                        "und setze Dein neues Passwort.\n\n" +
                        "Mit freundlichen Grüßen\n" +
                        "Dein CrowdSource Team"));
    }

    @Test
    public void testSendUserNotificationMailForPublished() {
        UserEntity user = new UserEntity("some.one@email.com");

        userNotificationService.notifyCreatorOnProjectStatusUpdate(project("proj3ctId", ProjectStatus.PUBLISHED, user, "My Super Project"));

        SimpleMailMessage mail = getMessageFromMailSender();
        assertThat(mail.getFrom(), is(UserNotificationService.FROM_ADDRESS));
        assertThat(mail.getTo(), arrayContaining(user.getEmail()));
        assertThat(mail.getSubject(), is(UserNotificationService.PROJECT_PUBLISHED_SUBJECT));
        assertThat(replaceLineBreaksIfWindows(mail.getText()), is(
                "Hallo Some One,\n\n" +
                        "Dein Projekt wurde erfolgreich freigegeben!\n" +
                        "Weitere Informationen hinsichtlich des Prozesses kannst Du der FAQ entnehmen.\n\n" +
                        "Zu Deinem Projekt:\n\n" +
                        "https://crowd.asideas.de#/project/proj3ctId\n\n" +
                        "Mit freundlichen Grüßen\n" +
                        "Dein CrowdSource Team"));
    }

    @Test
    public void testSendUserNotificationMailForRejected() {
        UserEntity user = new UserEntity("some.one@email.com");

        userNotificationService.notifyCreatorOnProjectStatusUpdate(project("proj3ctId", ProjectStatus.REJECTED, user, "My Super Project"));

        SimpleMailMessage mail = getMessageFromMailSender();
        assertThat(mail.getFrom(), is(UserNotificationService.FROM_ADDRESS));
        assertThat(mail.getTo(), arrayContaining(user.getEmail()));
        assertThat(mail.getSubject(), is(UserNotificationService.PROJECT_REJECTED_SUBJECT));
        assertThat(replaceLineBreaksIfWindows(mail.getText()), is(
                "Hallo Some One,\n\n" +
                        "Dein Projekt wurde leider abgelehnt.\n" +
                        "Das CrowdSource Team wird in Kürze mit Dir in Kontakt treten, um die nächsten Schritte zu besprechen.\n\n" +
                        "Zu Deinem Projekt:\n\n" +
                        "https://crowd.asideas.de#/project/proj3ctId\n\n" +
                        "Mit freundlichen Grüßen\nDein CrowdSource Team"));
    }

    @Test
    public void testSendUserNotificationMailForFallback() {
        UserEntity user = new UserEntity("some.one@email.com");

        userNotificationService.notifyCreatorOnProjectStatusUpdate(project("proj3ctId", ProjectStatus.PROPOSED, user, "My Super Project"));

        SimpleMailMessage mail = getMessageFromMailSender();
        assertThat(mail.getFrom(), is(UserNotificationService.FROM_ADDRESS));
        assertThat(mail.getTo(), arrayContaining(user.getEmail()));
        assertThat(mail.getSubject(), is("Der Zustand des Projekts My Super Project hat sich geändert!"));
        assertThat(mail.getText(), is("Das Projekt My Super Project wurde in den Zustand PROPOSED versetzt."));
    }

    @Test
    public void testNotifyAdminOnProjectCreation() {
        UserEntity user = new UserEntity("some.one@email.com");

        userNotificationService.notifyAdminOnProjectCreation(project("proj3ctId", ProjectStatus.PUBLISHED, user, "My Super Project"), ADMIN_EMAIL);

        SimpleMailMessage mail = getMessageFromMailSender();
        assertThat(mail.getFrom(), is(UserNotificationService.FROM_ADDRESS));
        assertThat(mail.getTo(), arrayContaining(ADMIN_EMAIL));
        assertThat(mail.getSubject(), is(UserNotificationService.NEW_PROJECT_SUBJECT));
        assertThat(replaceLineBreaksIfWindows(mail.getText()), is(
                "Hallo Admin,\n\n" +
                        "es liegt ein neues Projekt zur Freigabe vor:\n\n" +
                        "https://crowd.asideas.de#/project/proj3ctId\n\n" +
                        "Mit freundlichen Grüßen\n" +
                        "Dein CrowdSource Team"));
    }


    private SimpleMailMessage getMessageFromMailSender() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        return messageCaptor.getValue();
    }

    private ProjectEntity project(String id, ProjectStatus status, UserEntity user, String title) {
        final ProjectEntity project = new ProjectEntity();
        project.setId(id);
        project.setCreator(user);
        project.setTitle(title);
        project.setStatus(status);
        return project;
    }

    private String replaceLineBreaksIfWindows(String message) {

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return message.replace("\r\n", "\n");
        }
        return message;
    }

    @Configuration
    static class Config {

        @Bean
        public UserNotificationService userNotificationService() {
            return new UserNotificationService();
        }

        @Bean
        public JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }

        @Bean
        public AsyncTaskExecutor taskExecutorSmtp() {
            return mock(AsyncTaskExecutor.class);
        }

    }
}
