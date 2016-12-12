package de.asideas.crowdsource.controller;

import de.asideas.crowdsource.presentation.user.User;
import de.asideas.crowdsource.presentation.user.UserActivation;
import de.asideas.crowdsource.presentation.user.UserRegistration;
import de.asideas.crowdsource.security.Roles;
import de.asideas.crowdsource.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;

@RestController
@RequestMapping(value = "/user")
public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void registerUser(@RequestBody @Valid UserRegistration userRegistration) {

        userService.assignActivationTokenForRegistration(userRegistration);
    }

    // FIXME, email should also in request body..
    @RequestMapping(value = "/{email}/activation", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void activateUser(
            @PathVariable String email,
            @RequestBody @Valid UserActivation userActivation) {

        userService.activateUser(email.toLowerCase(), userActivation);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{email}/password-recovery", method = RequestMethod.GET)
    public void recoverPassword(@PathVariable String email) {

        userService.recoverPassword(email.toLowerCase());
    }

    @Secured(Roles.ROLE_USER)
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/current", method = RequestMethod.GET)
    public User getCurrentUser(Principal principal) {

        return userService.getUserByEmail(principal.getName());
    }
}
