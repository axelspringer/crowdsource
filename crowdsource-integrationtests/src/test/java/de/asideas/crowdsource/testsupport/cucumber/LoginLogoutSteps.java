package de.asideas.crowdsource.testsupport.cucumber;

import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.asideas.crowdsource.testsupport.CrowdSourceTestConfig;
import de.asideas.crowdsource.testsupport.pageobjects.LoginForm;
import de.asideas.crowdsource.testsupport.pageobjects.LogoutPage;
import de.asideas.crowdsource.testsupport.pageobjects.NavigationBar;
import de.asideas.crowdsource.testsupport.pageobjects.project.ProjectListPage;
import de.asideas.crowdsource.testsupport.selenium.WebDriverProvider;
import de.asideas.crowdsource.testsupport.util.UrlProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static de.asideas.crowdsource.testsupport.util.CrowdSourceClient.DEFAULT_ADMIN_EMAIL;
import static de.asideas.crowdsource.testsupport.util.CrowdSourceClient.DEFAULT_ADMIN_PASS;
import static de.asideas.crowdsource.testsupport.util.CrowdSourceClient.DEFAULT_USER_EMAIL;
import static de.asideas.crowdsource.testsupport.util.CrowdSourceClient.DEFAULT_USER_PASS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ContextConfiguration(classes = CrowdSourceTestConfig.class)
public class LoginLogoutSteps {

    @Autowired
    private WebDriverProvider webDriverProvider;

    @Autowired
    private UrlProvider urlProvider;

    @Autowired
    private NavigationBar navigationBar;

    @Autowired
    private ProjectListPage projectListPage;

    @Autowired
    private LoginForm loginForm;

    @Autowired
    private LogoutPage logoutPage;

    private WebDriver webDriver;

    @Before
    public void init() {
        webDriver = webDriverProvider.provideDriver();
    }

    @Given("^a user is logged in")
    public void a_user_is_logged_in() throws Throwable {
        the_index_page_is_visited();
        he_clicks_on_the_Login_link_in_the_navigation_bar();
        he_enters_valid_credentials();
        he_is_redirected_to_the_index_page();
    }

    @Given("^an admin is logged in$")
    public void an_admin_is_logged_in() throws Throwable {
        the_index_page_is_visited();
        he_clicks_on_the_Login_link_in_the_navigation_bar();
        login(DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASS);
        he_is_redirected_to_the_index_page();
    }

    @When("^he clicks on the Login link in the navigation bar$")
    public void he_clicks_on_the_Login_link_in_the_navigation_bar() throws Throwable {
        PageFactory.initElements(webDriver, navigationBar);
        navigationBar.clickLogin();
    }

    @Then("^he is redirected to the login page$")
    public void he_is_redirected_to_the_login_page() throws Throwable {

        PageFactory.initElements(webDriver, loginForm);
        loginForm.waitForPageLoad();
    }

    @When("^he enters valid credentials$")
    public void he_enters_valid_credentials() throws Throwable {
        login(DEFAULT_USER_EMAIL, DEFAULT_USER_PASS);
    }

    private void login(String email, String password) {
        PageFactory.initElements(webDriver, loginForm);
        final String email_ = email.substring(0, email.indexOf("@"));
        loginForm.login(email_, password);
    }

    @When("^he enters invalid credentials$")
    public void he_enters_invalid_credentials() throws Throwable {

        PageFactory.initElements(webDriver, loginForm);
        loginForm.login("foooooooaaaahhhh", DEFAULT_USER_PASS);
    }

    @Then("^he is redirected to the index page$")
    public void he_is_redirected_to_the_index_page() throws Throwable {

        PageFactory.initElements(webDriver, projectListPage);
        projectListPage.waitForPageLoad();
    }

    @When("^he reloads the page$")
    public void he_reloads_the_page() throws Throwable {

        webDriver.get(webDriver.getCurrentUrl());
    }

    @Given("^the index page is visited.*$")
    public void the_index_page_is_visited() throws Throwable {

        webDriver.get(urlProvider.applicationUrl());
        projectListPage.waitForPageLoad();
    }

    @Given("^the CROWD link is clicked$")
    public void the_CROWD_link_is_clicled() throws Throwable {
        webDriverProvider.provideDriver().findElement(By.className("site-logo")).click();
    }

    @Then("^the error \"([^\"]*)\" is displayed$")
    public void the_error_is_displayed(String errorText) throws Throwable {
        assertThat(loginForm.getErrorText(), is(errorText));
    }

    @Then("^the \"([^\"]*)\" button is visible$")
    public void the_button_is_visible(String buttonName) throws Throwable {

        // will throw an exception if element does not exist
        webDriver.findElement(By.className(buttonName));
    }


    @And("^the \"([^\"]*)\" button is not visible$")
    public void the_button_is_not_visible(String buttonName) throws Throwable {

        boolean notFound = false;
        try {
            the_button_is_visible(buttonName);
        } catch (NoSuchElementException e) {
            notFound = true;
        }
        assertThat("button should not be visible", notFound, is(true));
    }

    @When("^he clicks on the Logout button$")
    public void he_clicks_on_the_Logout_button() throws Throwable {
        PageFactory.initElements(webDriver, navigationBar);
        navigationBar.clickLogout();
    }

    @Then("^he is redirected to the logout page$")
    public void he_is_redirected_to_the_logout_page() throws Throwable {
        PageFactory.initElements(webDriver, logoutPage);
        logoutPage.waitForPageLoad();
    }

    @When("^he clicks on the relogin-link$")
    public void he_clicks_on_the_relogin_link() throws Throwable {
        logoutPage.clickRelogin();
    }

    @And("^the text \"([^\"]*)\" is displayed$")
    public void the_text_is_displayed(String text) throws Throwable {
        String pageSource = webDriver.getPageSource();
        assertThat(pageSource, containsString(text));
    }

    @And("^he clicks on the password recovery link$")
    public void he_clicks_on_the_password_recovery_link() throws Throwable {
        PageFactory.initElements(webDriver, loginForm);
        loginForm.waitForPageLoad();
        loginForm.clickPasswordRecoveryLink();
    }
}
