package de.asideas.crowdsource;

import de.asideas.crowdsource.testsupport.selenium.SeleniumWait;
import de.asideas.crowdsource.testsupport.selenium.WebDriverProvider;
import de.asideas.crowdsource.testsupport.util.UrlProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockPropertySource;

public class IndexIT extends AbstractIT {

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
    public void testIndexPage() {
        webDriver.get(urlProvider.applicationUrl() + "/");
        wait.until(driver -> "CrowdSource - Projekte".equals(driver.getTitle()));
    }

}
