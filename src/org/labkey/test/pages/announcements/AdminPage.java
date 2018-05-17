package org.labkey.test.pages.announcements;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Checkbox.Checkbox;

/**
 * User: tgaluhn
 * Date: 4/30/2018
 *
 * Page object for the messsage board admin (customization) page
 */
public class AdminPage extends LabKeyPage<AdminPage.ElementCache>
{
    public AdminPage(WebDriver driver)
    {
        super(driver);
    }

    public static AdminPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static AdminPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("announcements", containerPath, "customize"));
        return new AdminPage(driver.getDriver());
    }

    public LabKeyPage save()
    {
        clickAndWait(elementCache().saveButton);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage cancel()
    {
        clickAndWait(elementCache().cancelButton);
        return new LabKeyPage(getDriver());
    }

    public AdminPage setModeratorReviewAll()
    {
        elementCache().moderatorReviewAll.click();
        return new AdminPage(getDriver());
    }

    public AdminPage setModeratorReviewInitial()
    {
        elementCache().moderatorReviewInitial.click();
        return new AdminPage(getDriver());
    }

    public AdminPage setModeratorReviewNone()
    {
        elementCache().moderatorReviewNone.click();
        return new AdminPage(getDriver());
    }

    protected AdminPage.ElementCache newElementCache()
    {
        return new AdminPage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected WebElement boardName = Locator.input("boardName").findWhenNeeded(this);
        protected WebElement conversationName = Locator.input("conversationName").findWhenNeeded(this);

        protected WebElement sortingInitial = Locator.radioButtonByName("sortOrderIndex").withAttribute("value", "0").findWhenNeeded(this);
        protected WebElement sortingRecent = Locator.radioButtonByName("sortOrderIndex").withAttribute("value", "1").findWhenNeeded(this);

        protected WebElement securityOff = Locator.radioButtonByName("secure").withAttribute("value", "0").findWhenNeeded(this);
        protected WebElement securityOn = Locator.radioButtonByName("secure").withAttribute("value", "1").findWhenNeeded(this);

        private Locator.XPathLocator moderatorReview = Locator.radioButtonByName("moderatorReview");
        protected WebElement moderatorReviewAll = moderatorReview.withAttribute("value", "All").findWhenNeeded(this);
        protected WebElement moderatorReviewInitial = moderatorReview.withAttribute("value", "InitialPost").findWhenNeeded(this);
        protected WebElement moderatorReviewNone = moderatorReview.withAttribute("value", "None").findWhenNeeded(this);

        Checkbox canEditTitle = Checkbox(Locator.checkboxByName("titleEditable")).findWhenNeeded(this);
        Checkbox includeMemberList = Checkbox(Locator.checkboxByName("memberList")).findWhenNeeded(this);
        Checkbox includeStatus = Checkbox(Locator.checkboxByName("status")).findWhenNeeded(this);
        Checkbox includeExpires = Checkbox(Locator.checkboxByName("expires")).findWhenNeeded(this);
        Checkbox includeAssignedTo = Checkbox(Locator.checkboxByName("assignedTo")).findWhenNeeded(this);
        OptionSelect<OptionSelect.SelectOption> defaultAssignedToSelect = OptionSelect.OptionSelect(Locator.tagWithName("select", "defaultAssignedTo")).findWhenNeeded(this);
        Checkbox includeGroups = Checkbox(Locator.checkboxByName("includeGroups")).findWhenNeeded(this);

        protected WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);

    }

}