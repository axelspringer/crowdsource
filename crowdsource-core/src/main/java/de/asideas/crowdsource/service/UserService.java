package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.exception.NotAuthorizedException;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.presentation.user.UserMetrics;
import de.asideas.crowdsource.repository.UserRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    public static final int ACTIVATION_TOKEN_LENGTH = 32;

    private UserRepository userRepository;
    private UserNotificationService userNotificationService;

    @Autowired
    public UserService(UserRepository userRepository, UserNotificationService userNotificationService) {
        this.userRepository = userRepository;
        this.userNotificationService = userNotificationService;
    }

    @Transactional
    public UserEntity getUserByEmail(String email) {

        UserEntity userEntity = userRepository.findByEmail(email.toLowerCase());
        if (userEntity == null) {
            throw new NotAuthorizedException("No user found with email " + email.toLowerCase());
        }
        return userEntity;
    }

    @Transactional
    public void assignActivationTokenForRegistration(UserEntity userEntity) {

        userEntity.setActivationToken(generateActivationToken());
        userNotificationService.sendActivationMail(userEntity);
        saveUser(userEntity);
    }

    @Transactional
    public void assignActivationTokenForPasswordRecovery(UserEntity userEntity) {

        userEntity.setActivationToken(generateActivationToken());
        userNotificationService.sendPasswordRecoveryMail(userEntity);
        saveUser(userEntity);
    }

    @Transactional
    public UserMetrics getUserMetrics() {
        final List<UserEntity> all = userRepository.findAll();
        return new UserMetrics(all);
    }

    private String generateActivationToken() {
        return RandomStringUtils.randomAlphanumeric(ACTIVATION_TOKEN_LENGTH);
    }

    private void saveUser(UserEntity userEntity) {
        userRepository.save(userEntity);
        LOG.debug("User saved: {}", userEntity);
    }
}
