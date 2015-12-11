package de.asideas.crowdsource.testsupport.cucumber;

import cucumber.api.PendingException;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.asideas.crowdsource.domain.presentation.project.Project;
import de.asideas.crowdsource.testsupport.CrowdSourceTestConfig;
import de.asideas.crowdsource.testsupport.pageobjects.project.ProjectAddAndModificationForm;
import de.asideas.crowdsource.testsupport.pageobjects.project.ProjectDetailPage;
import de.asideas.crowdsource.testsupport.pageobjects.project.ProjectsPage;
import de.asideas.crowdsource.testsupport.selenium.SeleniumWait;
import de.asideas.crowdsource.testsupport.selenium.WebDriverProvider;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@ContextConfiguration(classes = CrowdSourceTestConfig.class)
public class ProjectEditSteps {

    @Autowired
    private ProjectsPage projectsPage;

    @Autowired
    private WebDriverProvider webDriverProvider;

    @Autowired
    private ProjectDetailPage projectDetailPage;

    @Autowired
    private ProjectDetailSteps projectDetailSteps;

    @Autowired
    private SeleniumWait seleniumWait;

    @Autowired
    private ProjectAddAndModificationForm projectAddAndModificationForm;

    private WebDriver webDriver;

    @Before
    public void init() {
        webDriver = webDriverProvider.provideDriver();
    }

    @After
    public void after() {
        WebDriverProvider.closeWebDriver();
    }

    private Project editedProject;


    @Then("^the project edit button is (indeed|not) existing and (indeed|not) enabled$")
    public void the_edit_button_is_visible_and_enabled(String existing, String enabled){
        boolean shouldExist = "indeed".equals(existing);
        boolean btnEnabled = "indeed".equals(enabled);

        PageFactory.initElements(webDriver, projectDetailPage);
        try {
            assertThat(projectDetailPage.getEditProjectButton().isDisplayed(), is(shouldExist));
            assertThat(projectDetailPage.getEditProjectButton().isEnabled(), is(btnEnabled));
        } catch (NoSuchElementException e) {
            if(shouldExist){
                throw e;
            }
        }
    }
    @When("^the user clicks the edit button$")
    public void the_user_clicks_the_edit_button(){
        projectDetailPage.clickEditButton();
    }

    @Then("^he is located at the project edit page$")
    public void he_is_located_at_the_project_edit_page(){
        PageFactory.initElements(webDriver, projectAddAndModificationForm);
        projectAddAndModificationForm.waitForPageLoadEditProject();
        assertThat(webDriver.getCurrentUrl(), is(projectAddAndModificationForm.editUrl(projectDetailSteps.getCreatedProject().getId())));
    }


    @And("^the form input fields are initialized with the project's data.*$")
    public void the_Form_Input_Fields_Are_Initialized_With_The_Project_SData() throws Throwable {
        Project project = projectDetailSteps.getCreatedProject();

        assertThat(project.getDescription(), is(projectAddAndModificationForm.getDescription()));
        assertThat(project.getPledgeGoal() + "", is(projectAddAndModificationForm.getPledgeGoal()));
        assertThat(project.getShortDescription(), is(projectAddAndModificationForm.getShortDescription()));
        assertThat(project.getTitle(), is(projectAddAndModificationForm.getTitle()));

    }

    @And("^he directly opens the project edit view$")
    public void he_Directly_Opens_The_Project_Edit_View() throws Throwable {
        projectAddAndModificationForm.openInEditMode(projectDetailSteps.getCreatedProject().getId());
    }

    @When("^he adapts the project details and submits the form$")
    public void he_Adapts_The_Project_Details_And_Submits_The_Form() throws Throwable {
        Project oldProject = projectDetailSteps.getCreatedProject();
        editedProject = new Project();
        editedProject.setTitle(oldProject.getTitle() + "_EDITED");
        editedProject.setDescription(oldProject.getDescription() + "_EDITED");
        editedProject.setShortDescription(oldProject.getShortDescription() + "_EDITED");
        editedProject.setPledgeGoal(1337);

        projectAddAndModificationForm.setTitle(editedProject.getTitle());
        projectAddAndModificationForm.setDescription(editedProject.getDescription());
        projectAddAndModificationForm.setShortDescription(editedProject.getShortDescription());
        projectAddAndModificationForm.setPledgeGoal(editedProject.getPledgeGoal() + "");

        projectAddAndModificationForm.submit();
    }

    @Then("^he is redirected to the project detail page containing updated project data$")
    public void he_Is_Redirected_To_The_Project_Detail_Page() throws Throwable {
        projectDetailPage.waitForDetailsToBeLoaded();

        assertThat(projectDetailPage.getTitle(), is(editedProject.getTitle()));
        assertThat(projectDetailPage.getShortDescription(), is(editedProject.getShortDescription()));
        assertThat(projectDetailPage.getDescription(), is(editedProject.getDescription()));
        assertThat(projectDetailPage.getProjectStatusWidget().getPledgeGoal(), is("1.337"));
    }
}
