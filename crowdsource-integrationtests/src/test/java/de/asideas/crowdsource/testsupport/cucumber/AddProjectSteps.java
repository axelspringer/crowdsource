package de.asideas.crowdsource.testsupport.cucumber;

import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.asideas.crowdsource.testsupport.pageobjects.NavigationBar;
import de.asideas.crowdsource.testsupport.pageobjects.project.AddProjectConfirmationView;
import de.asideas.crowdsource.testsupport.pageobjects.project.ProjectAddAndModificationForm;
import de.asideas.crowdsource.testsupport.pageobjects.project.ProjectDetailPage;
import de.asideas.crowdsource.testsupport.pageobjects.project.ProjectStatusWidget;
import de.asideas.crowdsource.testsupport.pageobjects.project.ProjectsPage;
import de.asideas.crowdsource.testsupport.selenium.ElementUtils;
import de.asideas.crowdsource.testsupport.selenium.WebDriverProvider;
import de.asideas.crowdsource.testsupport.util.CrowdSourceClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AddProjectSteps {

    @Autowired
    private WebDriverProvider webDriverProvider;

    @Autowired
    private NavigationBar navigationBar;

    @Autowired
    private ProjectAddAndModificationForm addProjectForm;

    @Autowired
    private ProjectDetailPage projectDetailPage;

    @Autowired
    private AddProjectConfirmationView addProjectConfirmationView;

    @Autowired
    private ProjectsPage projectsPage;

    @Autowired
    private CrowdSourceClient crowdSourceClient;

    private WebDriver webDriver;

    private String randomProjectTitlePrefix;

    private String randomProjectShortDescriptionPrefix;

    @Before
    public void init() {
        webDriver = webDriverProvider.provideDriver();
    }


    @When("^he clicks on the New Project link in the navigation bar$")
    public void he_clicks_on_the_New_Project_link_in_the_navigation_bar() throws Throwable {
        PageFactory.initElements(webDriver, navigationBar);
        navigationBar.clickNewProject();
    }

    @Then("^he is redirected to the project creation page$")
    public void he_is_redirected_to_the_project_creation_page() throws Throwable {
        PageFactory.initElements(webDriver, addProjectForm);
        addProjectForm.waitForPageLoadNewProject();
    }

    @When("^he submits the form with valid project data$")
    public void he_submits_the_form_with_valid_project_data() throws Throwable {
        PageFactory.initElements(webDriver, addProjectForm);

        randomProjectTitlePrefix = "Title" + RandomStringUtils.randomAlphanumeric(4);
        randomProjectShortDescriptionPrefix = "Short description " + RandomStringUtils.randomAlphanumeric(16);

        // makes exactly 60 characters, to cause the text to be abbreviated on purpose
        addProjectForm.setTitle(randomProjectTitlePrefix + " Mmmmm mmmmm mmmmm mmm mmmm, mmmmmmmmm");

        // makes exactly 140 characters, to cause the text to be abbreviated on purpose
        addProjectForm.setShortDescription(randomProjectShortDescriptionPrefix +
                " Mmmmm mmmmm mmmmm mmm mmmm, mmmmmmmmmm mmmmmmmmmm mmmmm, mmm mmmm mmmmmm mmmmmm mmmmmm mmmmmmmm mm mmmmmm");

        addProjectForm.setPledgeGoal("25000");
        addProjectForm.setDescription("Loooong description\nwith newlines");
        addProjectForm.submit();
    }

    @Then("^the project creation success page is shown$")
    public void the_project_creation_success_page_is_shown() throws Throwable {
        PageFactory.initElements(webDriver, addProjectConfirmationView);
        addProjectConfirmationView.waitForPageLoad();
    }

    @When("^he clicks the project details link$")
    public void he_clicks_the_project_details_link() throws Throwable {
        PageFactory.initElements(webDriver, addProjectConfirmationView);
        addProjectConfirmationView.clickLinkToProject();
    }

    @Then("^the project overview page shows the new project$")
    public void the_project_overview_page_shows_the_new_project() throws Throwable {
        PageFactory.initElements(webDriver, projectsPage);
        projectsPage.waitForPageLoad();


        assertTrue(projectsPage.containsProject(randomProjectTitlePrefix, randomProjectShortDescriptionPrefix));
    }

    @Then("^the project details page shows the new project$")
    public void the_project_details_page_shows_the_new_project() throws Throwable {
        projectDetailPage.waitForDetailsToBeLoaded();

        assertThat(projectDetailPage.getTitle(), startsWith(randomProjectTitlePrefix));
        assertThat(projectDetailPage.getShortDescription(), startsWith(randomProjectShortDescriptionPrefix));
        assertThat(projectDetailPage.getDescription(), is("Loooong description\nwith newlines"));

        ProjectStatusWidget projectStatusWidget = projectDetailPage.getProjectStatusWidget();
        assertThat(projectStatusWidget.getProgressBarValue(), is("0px"));
        assertThat(projectStatusWidget.getPledgedAmount(), is("0"));
        assertThat(projectStatusWidget.getPledgeGoal(), is("25.000"));
        assertThat(projectStatusWidget.getBackers(), is("0"));
        assertThat(projectStatusWidget.getUserName(), is("Crowdsource"));
    }

    @And("^the tooltip for currency conversion is not visible$")
    public void the_tooltip_for_currency_conversion_is_not_visible() throws Throwable {
        assertFalse(addProjectForm.currencyConversionTooltipVisible());
    }

    @When("^he hovers the currency element$")
    public void he_hovers_the_currency_element() throws Throwable {
        addProjectForm.hoverCurrency();
    }

    @Then("^the tooltip for currency conversion is visible$")
    public void the_tooltip_for_currency_conversion_is_visible() throws Throwable {
        assertTrue(addProjectForm.currencyConversionTooltipVisible());
    }

    @When("^an admin publishs the project$")
    public void an_admin_publishs_the_project() throws Throwable {

        final CrowdSourceClient.AuthToken authToken = crowdSourceClient.authorizeWithAdminUser();
        crowdSourceClient.listProjects(authToken).forEach(project -> {
            if (project.getTitle().startsWith(randomProjectTitlePrefix)) {
                crowdSourceClient.publish(project, authToken);
            }
        });
    }

    @When("^an admin defers the project$")
    public void an_admin_defers_the_project() throws Throwable {

        final CrowdSourceClient.AuthToken authToken = crowdSourceClient.authorizeWithAdminUser();
        crowdSourceClient.listProjects(authToken).forEach(project -> {
            if (project.getTitle().startsWith(randomProjectTitlePrefix)) {
                crowdSourceClient.defer(project, authToken);
            }
        });
    }

    @And("^the project is marked \"([^\"]*)\"$")
    public void the_project_is_marked(String state) throws Throwable {
        final WebElement projectElement = projectsPage.findProject(randomProjectTitlePrefix, randomProjectShortDescriptionPrefix);
        assertNotNull(projectElement);
        assertTrue(ElementUtils.hasClass(projectElement, "project-" + state));
    }
}
