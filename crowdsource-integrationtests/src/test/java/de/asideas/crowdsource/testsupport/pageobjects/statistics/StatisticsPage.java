package de.asideas.crowdsource.testsupport.pageobjects.statistics;

import de.asideas.crowdsource.testsupport.selenium.SeleniumWait;
import de.asideas.crowdsource.testsupport.selenium.WebDriverProvider;
import de.asideas.crowdsource.testsupport.util.UrlProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StatisticsPage {

    public static final String[] STATISTICS_OPTIONS = new String[]{
            "Anzahl Neuregistrieung / Neu eingereichte Ideen",
            "Projekte je Projektstatus"
    };

    @FindBy(css = ".statistics-form-type-select-dropdown")
    private WebElement metricsSelector;

    @Autowired
    private SeleniumWait wait;

    @Autowired
    private UrlProvider urlProvider;

    @Autowired
    private WebDriverProvider webDriverProvider;

    @Autowired
    private CountUserRegistrationsAndNewProjects countUserRegistrationsAndNewProjects;

    @Autowired
    private ProjectsByStatus projectsByStatus;

    @Autowired
    private CommentCountByProject commentCountByProject;

    public void open(){
        webDriverProvider.provideDriver().get(urlProvider.applicationUrl() + "/#/statistics");
        waitForPageLoad();
        PageFactory.initElements(webDriverProvider.provideDriver(), this);
    }

    public void waitForPageLoad() {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".statistics-form-type-select-dropdown")));
    }


    public StatisticsContainer selectStatisticType(String statisticType) {
        new Select(metricsSelector).selectByVisibleText(statisticType);

        switch(statisticType){
            case "Anzahl Neuregistrieung / Neu eingereichte Ideen":
                countUserRegistrationsAndNewProjects.waitForPageload();
                return countUserRegistrationsAndNewProjects;
            case "Projekte je Projektstatus":
                projectsByStatus.waitForPageload();
                return projectsByStatus;
            case "Kommentare je Projekt":
                commentCountByProject.waitForPageload();
                return commentCountByProject;
            default:
                throw new IllegalArgumentException("Requested statistics type not supported: " + statisticType);
        }
    }
}
