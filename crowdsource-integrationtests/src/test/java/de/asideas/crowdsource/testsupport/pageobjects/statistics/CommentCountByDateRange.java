package de.asideas.crowdsource.testsupport.pageobjects.statistics;

import de.asideas.crowdsource.testsupport.selenium.SeleniumWait;
import de.asideas.crowdsource.testsupport.selenium.WebDriverProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommentCountByDateRange implements StatisticsContainer {

    @Autowired
    private SeleniumWait wait;

    @Autowired
    private WebDriverProvider webDriverProvider;

    @FindBy(css = ".statistics-startdate")
    private WebElement startDate;

    @FindBy(css = ".statistics-enddate")
    private WebElement endDate;

    @FindBy(css = ".statistic-result")
    private WebElement resultContainer;

    @Override
    public void waitForPageload() {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".statistic-result")));
        PageFactory.initElements(webDriverProvider.provideDriver(), this);
    }

    public boolean startDateSelectionDisplayed(){
        return startDate.isDisplayed();
    }

    public boolean endDateSelectionDisplayed(){
        return endDate.isDisplayed();
    }

    public boolean resultContainerDisplayed(){
        return resultContainer.isDisplayed();
    }
}
