package de.asideas.crowdsource;

import de.asideas.crowdsource.testsupport.selenium.ElementUtils;
import de.asideas.crowdsource.testsupport.selenium.SeleniumWait;
import de.asideas.crowdsource.testsupport.selenium.WebDriverProvider;
import de.asideas.crowdsource.testsupport.util.UrlProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

public class TrackingIT extends AbstractIT {

    public static final String DEFAULT_TEST_PROPERTY_SOURCE = "testPropsSource";

    private MockPropertySource testPropertySource;

    @Autowired
    private UrlProvider urlProvider;

    @Autowired
    private WebDriverProvider webDriverProvider;

    @Autowired
    private SeleniumWait wait;

    private WebDriver webDriver;

    @Autowired
    ConfigurableEnvironment env;

    @Before
    public void initDriver() {
        webDriver = webDriverProvider.provideDriver();

        testPropertySource = new MockPropertySource(DEFAULT_TEST_PROPERTY_SOURCE);
        // Overlay properties, set by tests.
        if (env.getPropertySources().contains(DEFAULT_TEST_PROPERTY_SOURCE)) {
            env.getPropertySources().replace(DEFAULT_TEST_PROPERTY_SOURCE, testPropertySource);
        } else {
            env.getPropertySources().addFirst(testPropertySource);
        }
    }

    @After
    public void closeDriver() {
        WebDriverProvider.closeWebDriver();
    }

    @Test
    public void trackingJsIsOmittedFromRenderingIfNoPropertiesProvided() {
        loadIndexPage();

        ElementUtils.expectAndGetFirst(webDriver, false, By.id("piwikTracking"));
    }

    @Test
    public void trackingJsIsRenderedIfPropertiesProvided() {
        String expectedTrackingUrl = "http://trackingrulz.crowdsource.de";
        String expectedSiteId = "1234567890987654321";
        testPropertySource.withProperty("de.asideas.crowdsource.tracking.piwik.trackurl", expectedTrackingUrl)
                .withProperty("de.asideas.crowdsource.tracking.piwik.siteid", expectedSiteId);

        loadIndexPage();

        WebElement trackingJsElement = ElementUtils.expectAndGetFirst(webDriver, true, By.id("piwikTracking"));

        assertThat(trackingJsElement.getTagName(), is("script"));
        String trackingJs = trackingJsElement.getAttribute("innerHTML");

        assertThat(trackingJs, containsString(expectedTrackingUrl));
        assertThat(trackingJs, containsString(expectedSiteId));
    }

    private void loadIndexPage() {
        webDriver.get(urlProvider.applicationUrl() + "/");
        wait.until(driver -> "CrowdSource - Projekte".equals(driver.getTitle()));
    }
}
