package de.asideas.crowdsource.testsupport.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import de.asideas.crowdsource.testsupport.CrowdSourceTestConfig;
import de.asideas.crowdsource.testsupport.pageobjects.statistics.*;
import de.asideas.crowdsource.testsupport.selenium.SeleniumWait;
import de.asideas.crowdsource.testsupport.selenium.WebDriverProvider;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@ContextConfiguration(classes = CrowdSourceTestConfig.class)
public class StatisticsSteps {

    @Autowired
    private SeleniumWait wait;

    @Autowired
    private WebDriverProvider webDriverProvider;

    @Autowired
    private StatisticsPage statisticsPage;

    private WebDriver webDriver;

    private String currentStatisticType;
    private StatisticsContainer currentStatisticsContainer;

    @Before
    public void init() {
        webDriver = webDriverProvider.provideDriver();
    }

    @After
    public void after() {
        WebDriverProvider.closeWebDriver();
    }


    @And("^she directly opens the statistics page.*$")
    public void she_Directly_Opens_The_Statistics_Page() throws Throwable {
        statisticsPage.open();
    }

    @And("^she selects the statistic '(.+)'$")
    public void she_Selects_The_Statistic_New_Users_And_Projects(String statisticType) throws Throwable {

        this.currentStatisticType = statisticType;
        currentStatisticsContainer = statisticsPage.selectStatisticType(statisticType);

    }

    @Then("^all expected UI elements are displayed for the selected statistic.*$")
    public void all_Expected_UI_Elements_Are_Displayed_For_The_Selected_Statistic() throws Throwable {
        switch (currentStatisticType) {
            case "Anzahl Neuregistrieung / Neu eingereichte Ideen":
                verifyStatistic_CountNewRegistrationsAndNewIdeas((CountUserRegistrationsAndNewProjects) currentStatisticsContainer);
                break;
            case "Projekte je Projektstatus":
                verifyStatistic_projectsByStatus((ProjectsByStatus) currentStatisticsContainer);
                break;
            case "Kommentare je Projekt":
                verifyStatistic_CountCommentOfProject((CommentCountByProject) currentStatisticsContainer);
                break;
            default:
                throw new IllegalArgumentException("Statistic type not supported: " + currentStatisticType);
        }
    }

    private void verifyStatistic_projectsByStatus(ProjectsByStatus currentStatisticsContainer) {
        assertThat(currentStatisticsContainer.resultContainerDisplayed(), is(true));

    }

    private void verifyStatistic_CountNewRegistrationsAndNewIdeas(CountUserRegistrationsAndNewProjects currentStatisticsContainer) {
        assertThat(currentStatisticsContainer.startDateSelectionDisplayed(), is(true));
        assertThat(currentStatisticsContainer.endDateSelectionDisplayed(), is(true));
        assertThat(currentStatisticsContainer.resultContainerDisplayed(), is(true));
    }

    private void verifyStatistic_CountCommentOfProject(CommentCountByProject commentCountByProject) {
        assertThat(commentCountByProject.projectCountDisplayed(), is(true));
        assertThat(commentCountByProject.resultContainerDisplayed(), is(true));
    }
}
