package de.asideas.crowdsource.controller;

import de.asideas.crowdsource.presentation.user.UserMetrics;
import de.asideas.crowdsource.security.Roles;
import de.asideas.crowdsource.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/metrics")
public class UserMetricsController {

    @Autowired
    private UserService userService;

    @Secured({Roles.ROLE_USER})
    @RequestMapping(method = RequestMethod.GET)
    public UserMetrics getUserMetrics() {

        return userService.getUserMetrics();
    }
}
