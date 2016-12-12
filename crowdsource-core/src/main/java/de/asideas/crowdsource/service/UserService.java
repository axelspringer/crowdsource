package de.asideas.crowdsource.service;

import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.domain.exception.NotAuthorizedException;
import de.asideas.crowdsource.domain.exception.ResourceNotFoundException;
import de.asideas.crowdsource.domain.model.FinancingRoundEntity;
import de.asideas.crowdsource.domain.model.UserEntity;
import de.asideas.crowdsource.domain.service.user.UserNotificationService;
import de.asideas.crowdsource.presentation.user.User;
import de.asideas.crowdsource.presentation.user.UserActivation;
import de.asideas.crowdsource.presentation.user.UserMetrics;
import de.asideas.crowdsource.presentation.user.UserRegistration;
import de.asideas.crowdsource.repository.FinancingRoundRepository;
import de.asideas.crowdsource.repository.UserRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    public static final int ACTIVATION_TOKEN_LENGTH = 32;

    private UserRepository userRepository;
    private UserNotificationService userNotificationService;
    private PasswordEncoder passwordEncoder;
    private FinancingRoundRepository financingRoundRepository;

    @Autowired
    public UserService(UserRepository userRepository, UserNotificationService userNotificationService, PasswordEncoder passwordEncoder, FinancingRoundRepository financingRoundRepository) {
        this.userRepository = userRepository;
        this.userNotificationService = userNotificationService;
        this.passwordEncoder = passwordEncoder;
        this.financingRoundRepository = financingRoundRepository;
    }

    @Transactional
    public User getUserByEmail(String email) {

        UserEntity userEntity = userRepository.findByEmail(email.toLowerCase());
        if (userEntity == null) {
            throw new NotAuthorizedException("No user found with email " + email.toLowerCase());
        }

        FinancingRoundEntity currentFinancingRound = financingRoundRepository.findActive(DateTime.now());
        if (currentFinancingRound == null) {
            // if there is no active financing round, the budget of the user should be 0
            // but we have no scheduler that resets the budget of every user to 0 when the financing round ends
            userEntity.setBudget(BigDecimal.ZERO);
        }

        return new User(userEntity);
    }

    @Transactional
    public void assignActivationTokenForRegistration(UserRegistration userRegistration) {

        UserEntity userEntity = userRepository.findByEmail(userRegistration.getEmail());

        if (userEntity == null) {
            userEntity = new UserEntity(userRegistration.getEmail(), userRegistration.getFirstname(), userRegistration.getLastname());
        }

        userEntity.setActivationToken(generateActivationToken());
        userNotificationService.sendActivationMail(userEntity);
        userRepository.save(userEntity);
    }

    @Transactional
    public UserMetrics getUserMetrics() {
        final List<UserEntity> all = userRepository.findAll();
        return new UserMetrics(all);
    }

    @Transactional
    public User findByEmail(String email) {
        final UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) {
            return null;
        }
        return new User(userEntity);
    }

    @Transactional
    public void activateUser(String email, UserActivation userActivation) {

        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) {
            LOG.debug("userentity with id {} does not exist and can therefore not be activated.", email);
            throw new ResourceNotFoundException();
        }

        // The activation token may be set to non blank when using password recovery.
        // In this case, the user is still activated but has a token set.
        if (isBlank(userEntity.getActivationToken()) && userEntity.isActivated()) {
            LOG.debug("user {} is already activated", userEntity);
            throw InvalidRequestException.userAlreadyActivated();
        }

        if (isBlank(userEntity.getActivationToken())
                || !userEntity.getActivationToken().equals(userActivation.getActivationToken())) {
            LOG.debug("token mismatch on activation request for user with email: {} (was {}, expected: {})",
                    email, userActivation.getActivationToken(), userEntity.getActivationToken());

            throw InvalidRequestException.activationTokenInvalid();
        }

        userEntity.activate(passwordEncoder.encode(userActivation.getPassword()));
        userRepository.save(userEntity);

    }

    @Transactional
    public void recoverPassword(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null) {
            LOG.debug("userentity with id {} does not exist and a password can therefore not be recovered.", email);
            throw new ResourceNotFoundException();
        }

        userEntity.setActivationToken(generateActivationToken());
        userNotificationService.sendPasswordRecoveryMail(userEntity);
        userRepository.save(userEntity);
    }

    private String generateActivationToken() {
        return RandomStringUtils.randomAlphanumeric(ACTIVATION_TOKEN_LENGTH);
    }
}
