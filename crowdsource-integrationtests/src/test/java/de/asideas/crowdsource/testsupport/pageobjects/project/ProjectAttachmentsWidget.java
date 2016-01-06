package de.asideas.crowdsource.testsupport.pageobjects.project;

import de.asideas.crowdsource.testsupport.selenium.SeleniumWait;
import de.asideas.crowdsource.testsupport.selenium.WebDriverProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;

@Component
public class ProjectAttachmentsWidget {

    private static final String CLASS_ATTACHMENTS_TABLE = ".attachments__list";
    @Autowired
    private SeleniumWait wait;

    @Autowired
    private WebDriverProvider webDriverProvider;

    @FindBy(css = "input[name=attachment]")
    private WebElement fileChooser;

    @FindBy(css = ".file-info")
    private WebElement fileInfoUpload;

    @FindBy(css = ".button.upload")
    private WebElement uploadButton;

    @FindBy(css = ".upload-messages__success")
    private WebElement uploadSuccessSpan;

    @FindBy(css = CLASS_ATTACHMENTS_TABLE)
    private WebElement attachmentsTable;

    public void selectFile(String filepath) {
        fileChooser.sendKeys(filepath);
        wait.until(d -> visibilityOf(fileInfoUpload));
    }

    public String getFileInfoUploadContent() {
        return fileInfoUpload.getText();
    }

    public void clickUploadButton() {
        this.uploadButton.click();
        wait.until(visibilityOf(uploadSuccessSpan));
    }

    public boolean uploadSuccessMessageVisible() {
        return uploadSuccessSpan.isDisplayed();
    }

    public boolean fileChooserVisibile() {
        return this.fileChooser.isDisplayed();
    }

    public boolean attachmentsTableExists() {
        return !webDriverProvider.provideDriver().findElements(By.id(CLASS_ATTACHMENTS_TABLE)).isEmpty();
    }

    public String attachmentsTable_FilenameCellOfRow(int row) {
        List<WebElement> rowsCells = cellsOfAttachmentsTablesRow(row);
        return rowsCells.get(0).getText();
    }

    public String attachmentsTable_FilesizeCellOfRow(int row) {
        List<WebElement> rowsCells = cellsOfAttachmentsTablesRow(row);
        return rowsCells.get(1).getText();
    }

    public boolean attachmentsTable_deleteButtonVisibleInRow(int row) {
        List<WebElement> webElements = attachmentsTable_ActionButtonsOfRow(row);
        WebElement button = actionButtonByClass(webElements, "delete-attachment");
        return button.isDisplayed();
    }

    public boolean attachmentsTable_copyLinkButtonVisibleInRow(int row) {
        List<WebElement> webElements = attachmentsTable_ActionButtonsOfRow(row);
        WebElement button = actionButtonByClass(webElements, "copy-attachment");
        return button.isDisplayed();
    }

    public boolean attachmentsTable_copyMarkdownButtonVisibleInRow(int row) {
        List<WebElement> webElements = attachmentsTable_ActionButtonsOfRow(row);
        WebElement button = actionButtonByClass(webElements, "copy-attachment-md");
        return button.isDisplayed();
    }

    public void clickDeleteButtonInRow(int row) {
        List<WebElement> webElements = attachmentsTable_ActionButtonsOfRow(row);
        WebElement button = actionButtonByClass(webElements, "delete-attachment");
        button.click();
        wait.until(invisibilityOfElementLocated(By.className(".attachments__list")));
    }

    private List<WebElement> attachmentsTable_ActionButtonsOfRow(int row) {
        List<WebElement> rowsCells = cellsOfAttachmentsTablesRow(row);
        return rowsCells.get(2).findElements(By.tagName("a"));
    }

    private List<WebElement> cellsOfAttachmentsTablesRow(int row) {
        List<WebElement> rows = attachmentsTable.findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
        return rows.get(row).findElements(By.tagName("td"));
    }

    private WebElement actionButtonByClass(List<WebElement> webElements, String btnClass) {
        return webElements.stream().filter(el -> el.getAttribute("class").contains(btnClass))
                .findFirst().get();
    }
}
