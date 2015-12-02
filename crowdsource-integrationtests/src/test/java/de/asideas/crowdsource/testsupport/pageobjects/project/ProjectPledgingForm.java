package de.asideas.crowdsource.testsupport.pageobjects.project;

import de.asideas.crowdsource.testsupport.selenium.ElementUtils;
import de.asideas.crowdsource.testsupport.selenium.SeleniumWait;
import de.asideas.crowdsource.testsupport.selenium.WebDriverProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class ProjectPledgingForm {

    private static final String SELECTOR_BUDGET = ".pledging-form .finance__section .budget";
    private static final String SELECTOR_PLEDGED_AMOUNT = ".pledging-form .pledged-amount";
    private static final String AMOUNT_INPUT_FIELD = "input.finance__input[type=\"number\"]";

    @FindBy(css = ".pledging-form .notification")
    private WebElement notificationBox;

    @FindBy(css = ".pledging-form .general-error")
    private WebElement errorMessageBox;

    @FindBy(css = ".pledging-form .range-slider")
    private WebElement slider;

    @FindBy(css = ".pledging-form .range-slider-handle")
    private WebElement sliderHandle;

    @FindBy(css = SELECTOR_PLEDGED_AMOUNT)
    private WebElement pledgedAmountLabel;

    @FindBy(css = ".pledging-form .pledge-goal")
    private WebElement pledgeGoalLabel;

    @FindBy(css = SELECTOR_BUDGET)
    private WebElement budgetLabel;

    @FindBy(css = ".pledging-form .finance__btn")
    private WebElement pledgingButton;

    @FindBy(css = AMOUNT_INPUT_FIELD)
    private WebElement amountInputField;

    @FindBy(css = ".pledging-form .finance__btn")
    private WebElement submitButton;

    @Autowired
    private WebDriverProvider webDriverProvider;

    @Autowired
    private SeleniumWait wait;

    public String getNotificationMessage() {
        return notificationBox.getText();
    }

    public String getErrorMessage() {
        return errorMessageBox.getText();
    }

    public int getUserBudget() {
        makeSureDataLoaded();
        return ElementUtils.parseCurrency(updatedWebElement(webDriverProvider.provideDriver(), SELECTOR_BUDGET));
    }

    public int getPledgedAmount() {
        makeSureDataLoaded();
        return ElementUtils.parseCurrency(updatedWebElement(webDriverProvider.provideDriver(), SELECTOR_PLEDGED_AMOUNT));
    }

    public int getPledgeGoalAmount() {
        return ElementUtils.parseCurrency(pledgeGoalLabel);
    }

    public WebElement getPledgingButton() {
        return pledgingButton;
    }

    public boolean isSliderEnabled() {
        return !slider.getAttribute("class").contains("disabled");
    }

    public WebElement getAmountInputField() {
        return amountInputField;
    }

    public void setAmountInputValue(int value) {
        amountInputField.clear();
        amountInputField.sendKeys(Integer.toString(value));
    }

    public int getAmountFromInputField() {
        wait.until(d -> Integer.valueOf(updatedWebElement(d, AMOUNT_INPUT_FIELD).getAttribute("value")), 5, 200, Collections.singletonList(NumberFormatException.class));
        return Integer.parseInt(amountInputField.getAttribute("value"));
    }

    public void moveSliderBy(int value) {
        new Actions(webDriverProvider.provideDriver())
                .dragAndDropBy(sliderHandle, value, 0)
                .perform();
        final long waitStart = System.currentTimeMillis();
        wait.until(d-> System.currentTimeMillis() - waitStart > 1000L); // Wait for slider adjustment
    }

    public void submitForm() {
        submitButton.click();
    }

    public void waitUntilANotificationOrEerrorMessageIsDisplayed() {
        wait.until(driver -> getNotificationMessage().length() > 0 || getErrorMessage().length() > 0);
    }

    private WebElement updatedWebElement(WebDriver d, String cssSelector) {
        return d.findElement(By.cssSelector(cssSelector));
    }
    private void makeSureDataLoaded(){
        PageFactory.initElements(webDriverProvider.provideDriver(), this);
        try{
            wait.until( d -> {
                try{
                    ElementUtils.parseCurrency(updatedWebElement(webDriverProvider.provideDriver(), SELECTOR_BUDGET));
                }catch (Exception e){
                    return false;
                }
                return true;
            } );
        }catch (TimeoutException e){
            // we catch it and hope that the element has been updated and initialized anyway.
        }
        // Just to be really sure, as build server fails but locally it works
        final long wStart = System.currentTimeMillis();
        wait.until(d-> System.currentTimeMillis() - wStart > 1000L);
        PageFactory.initElements(webDriverProvider.provideDriver(), this);
    }
}
