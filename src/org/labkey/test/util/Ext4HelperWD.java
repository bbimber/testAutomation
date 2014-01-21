/*
 * Copyright (c) 2012-2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ext4cmp.Ext4CmpRefWD;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: klum
 * Date: Jan 3, 2012
 * Time: 3:34:16 PM
 */
public class Ext4HelperWD extends AbstractHelperWD
{
    public Ext4HelperWD(BaseWebDriverTest test)
    {
        super(test);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(Locator.XPathLocator comboBox, @LoggedParam String selection)
    {
        selectComboBoxItem(comboBox, false, selection);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(Locator.XPathLocator comboBox, boolean containsText, @LoggedParam String... selections)
    {
        openComboList(comboBox);

        for (String selection : selections)
        {
            selectItemFromOpenComboList(selection, containsText);
        }

        closeComboList(comboBox);
    }

    private void openComboList(Locator.XPathLocator comboBox)
    {
        Locator arrowTrigger = comboBox.append("//div[contains(@class,'arrow')]");
        _test.click(arrowTrigger);

        if (!_test.waitForElement(comboBox.withDescendant(Locator.tag("td").withClass("x4-pickerfield-open")), 1000, false))
            _test.click(arrowTrigger); // try again if combo-box doesn't open

        _test.waitForElement(Locator.css(".x4-boundlist-item"));
    }

    private void selectItemFromOpenComboList(String itemText, boolean containsText)
    {
        Locator.XPathLocator listItem = Locator.xpath("//*[contains(@class, 'x4-boundlist-item')]").notHidden();
        if (containsText)
            listItem = listItem.containing(itemText);
        else
            listItem = listItem.withText(itemText);

        WebElement element = listItem.waitForElement(_test.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        boolean elementAlreadySelected = element.getAttribute("class").contains("selected");
        if (!isOpenComboBoxMultiSelect() || !elementAlreadySelected)
        {
            _test.scrollIntoView(element); // Workaround: Auto-scrolling in chrome isn't working well
            _test.click(listItem);
        }
    }

    private void closeComboList(Locator.XPathLocator comboBox)
    {
        Locator arrowTrigger = comboBox.append("//div[contains(@class,'arrow')]");

        // close combo manually if it is a multi-select combo-box
        if (isOpenComboBoxMultiSelect())
            _test.click(arrowTrigger);

        // menu should disappear
        _test.shortWait().until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".x4-boundlist-item")));
    }

    private boolean isOpenComboBoxMultiSelect()
    {
        return _test.isElementPresent(Locator.xpath("//*[contains(@class, 'x4-boundlist-item')]").notHidden().append("/span").withClass("x4-combo-checker"));
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(@LoggedParam String label, @LoggedParam String selection)
    {
        selectComboBoxItem(Ext4HelperWD.Locators.formItemWithLabel(label), false, selection);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(@LoggedParam String label, @LoggedParam String selection, boolean containsText)
    {
        selectComboBoxItem(Ext4HelperWD.Locators.formItemWithLabel(label), containsText, selection);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItemById(@LoggedParam String labelId, @LoggedParam String selection)
    {
        Locator.XPathLocator loc = Locator.xpath("//tbody[./tr/td/label[@id='" + labelId + "-labelEl']]");
        selectComboBoxItem(loc, selection);
    }

    @LogMethod(quiet = true)
    public void selectRadioButton(@LoggedParam String label, @LoggedParam String selection)
    {
        Locator l = Locator.xpath("//div[div/label[text()='" + label + "']]//label[text()='" + selection + "']");
        if (!_test.isElementPresent(l))
        {
            // try Ext 4.1.0 version
            l = Locator.xpath("//div[./table//label[text()='" + label + "']]//label[text()='" + selection + "']");
        }
        _test.click(l);
    }

    @LogMethod(quiet = true)
    public void selectRadioButtonById(@LoggedParam String labelId)
    {
        Locator l = Locator.xpath("//label[@id='" + labelId + "']");
        _test.click(l);
    }

    public void selectRadioButton(String selection)
    {
        Locator l = Locators.radiobutton(_test, selection);
        _test.click(l);
    }

    @LogMethod(quiet = true)
    public void waitForComponentNotDirty(@LoggedParam final String componentId)
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return !(Boolean)_test.executeScript("return Ext4.getCmp('" + componentId + "').isDirty();");
            }
        }, "Page still marked as dirty", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod(quiet = true)
    public void clickExt4Tab(@LoggedParam String tabname)
    {
        Locator l = Locators.tab(tabname);
        _test.click(l);
    }

    public void clickWindowButton(String windowTitle, String buttonText, int wait, int index)
    {
        _test.log("Clicking Ext4 button with text: " + buttonText + " inside window with title: " + windowTitle);
        Locator loc = ext4WindowButton(windowTitle, buttonText);
        _test.waitForElement(loc, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.clickAndWait(loc, wait);
    }

    @LogMethod(quiet = true)
    public void checkCheckbox(@LoggedParam String label)
    {
        if (!isChecked(label))
        {
            Locator l = Locators.checkbox(_test, label);
            _test.click(l);
        }
    }

    @LogMethod(quiet = true)
    public void uncheckCheckbox(@LoggedParam String label)
    {
        if (isChecked(label))
        {
            Locator l = Locators.checkbox(_test, label);
            _test.click(l);
        }
    }

    public boolean isChecked(String label)
    {
        Locator.XPathLocator checkbox = Locators.checkbox(_test, label);
        return isChecked(checkbox);
    }

    public boolean isChecked(Locator.XPathLocator checkbox)
    {
        _test.assertElementPresent(checkbox);
        Locator l = checkbox.append("[./ancestor-or-self::*[contains(@class, 'checked')]]");
        return _test.isElementPresent(l);
    }

    /**
     * Check the checkbox for an Ext4 grid row
     * Currently used only for participant filter panel.
     * @param cellText Exact text from any cell in the desired row
     */
    public void checkGridRowCheckbox(String cellText)
    {
        checkGridRowCheckbox(cellText, 0);
    }

    /**
     * Check the checkbox for an Ext4 grid row
     * Currently used only for participant filter panel
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    @LogMethod(quiet = true)
    public void checkGridRowCheckbox(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        if (!isGridRowChecked(rowLoc))
            _test.click(rowLoc.append("//div[contains(@class, 'x4-grid-row-checker')]"));
    }

    /**
     * Uncheck the checkbox for an Ext4 grid row
     * Currently used only for participant filter panel.
     * @param cellText Exact text from any cell in the desired row
     */
    public void uncheckGridRowCheckbox(String cellText)
    {
        uncheckGridRowCheckbox(cellText, 0);
    }

    /**
     * Uncheck the checkbox for an Ext4 grid row
     * Currently used only for participant filter panel
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    @LogMethod(quiet = true)
    public void uncheckGridRowCheckbox(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        if (isGridRowChecked(rowLoc))
            _test.click(rowLoc.append("//div[contains(@class, 'x4-grid-row-checker')]"));
    }

    /**
     * Click the text of an Ext4 grid row
     * Currently used only for time chart measure picker
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    @LogMethod(quiet = true)
    public void clickGridRowText(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        _test.waitForElement(rowLoc);
        _test.click(rowLoc.append("//div[contains(@class, 'x4-grid-cell')][normalize-space() = '"+cellText+"']"));
    }

    /**
     * Click the text of an Participant filter panel grid row
     * Currently used only for participant filter panel
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    public void clickParticipantFilterGridRowText(String cellText, int index)
    {
        _test.waitForElementToDisappear(Locator.tag("div").withClass("x4-tip").notHidden()); // tooltip breaks test in Chrome
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        _test.waitForElement(rowLoc);
        _test.click(rowLoc.append("//span[contains(@class, 'lk-filter-panel-label')][normalize-space() = '"+cellText+"']"));
    }

    /**
     * Click the text of an Participant filter panel category grouping header
     * Currently used only for participant filter panel
     * @param categoryLabel Exact text from any category label (i.e. Cohorts, Group 1)
     */
    public void clickParticipantFilterCategory(String categoryLabel)
    {
        Locator.XPathLocator loc = Locator.xpath("//div[contains(@class, 'category-label') and text()='" + categoryLabel + "']/../../td/div[contains(@class, 'category-header')]");
        _test.click(loc);
    }

    /**
     * Deselect the "All" Participant filter panel category checkbox
     */
    public void deselectAllParticipantFilter()
    {
        checkGridRowCheckbox("All");
        uncheckGridRowCheckbox("All");
    }

    /**
     * Select the "All" Participant filter panel category checkbox
     */
    public void selectAllParticipantFilter()
    {
        checkGridRowCheckbox("All");
    }

    /**
     * Determines if the specified row has a checked checkbox
     * @param rowLoc Locator provided by {@link #getGridRow(String, int)}
     * @return true if the specified row has a checked checkbox
     */
    private boolean isGridRowChecked(Locator.XPathLocator rowLoc)
    {
        _test.assertElementPresent(rowLoc);
        return _test.isElementPresent(rowLoc.append("[contains(@class, 'x4-grid-row-selected')]"));
    }

    /**
     * Determines if the specified row has a checked checkbox
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     * @return true if the specified row has a checked checkbox
     */
    public boolean isGridRowChecked(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        return isGridRowChecked(rowLoc);
    }

    public <Type extends Ext4CmpRefWD> List<Type> componentQuery(String componentSelector, Class<Type> clazz)
    {
        return componentQuery(componentSelector, null, clazz);
    }

    public <Type extends Ext4CmpRefWD> List<Type> componentQuery(String componentSelector, String parentId, Class<Type> clazz)
    {
        componentSelector = componentSelector.replaceAll("'", "\"");  //escape single quotes
        String script =
                "ext4ComponentQuery = function (selector, parentId) {\n" +
                "    var res = null;\n" +
                "    if (parentId)\n" +
                "        res = Ext4.getCmp(parentId).query(selector);\n" +
                "    else\n" +
                "        res = Ext4.ComponentQuery.query(selector);\n" +

                "    return null == res ? null : Ext4.Array.pluck(res, \"id\");\n" +
                "};" +
                "return ext4ComponentQuery(arguments[0], arguments[1]);";

        List<String> unfilteredIds = (List<String>)_test.executeScript(script, componentSelector, parentId);
        List<String> ids = new ArrayList<>();
        for (String id : unfilteredIds)
        {
            if (Locator.id(id).findElements(_test.getDriver()).size() > 0)
                ids.add(id); // ignore uninitialized ext components
        }
        return _test._ext4Helper.componentsFromIds(ids, clazz);
    }

    public <Type extends Ext4CmpRefWD> Type queryOne(String componentSelector, Class<Type> clazz)
    {
        List<Type> cmpRefs = componentQuery(componentSelector, clazz);
        if (null == cmpRefs || cmpRefs.size() == 0)
            return null;

        return cmpRefs.get(0);
    }

    public <Type extends Ext4CmpRefWD> List<Type> componentsFromIds(List<String> ids, Class<Type> clazz)
    {
        if (null == ids || ids.isEmpty())
            return null;

        try
        {
            List<Type> ret = new ArrayList<>(ids.size());
            for (String id : ids)
            {
                Constructor<Type> constructor = clazz.getConstructor(String.class, BaseWebDriverTest.class);
                ret.add(constructor.newInstance(id, _test));
            }
            return ret;
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public BaseWebDriverTest.Checker getExt4SelectorChecker(final String selector)
    {
        return new BaseWebDriverTest.Checker(){
            public boolean check()
            {
                return queryOne(selector, Ext4CmpRefWD.class) != null;
            }
        };
    }

    public void clickTabContainingText(String tabText)
    {
        _test.click(Locators.tab(tabText));
    }

    public void waitForMaskToDisappear()
    {
        waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForMaskToDisappear(int wait)
    {
        _test.waitForElementToDisappear(Locators.mask(), wait);
    }

    public void waitForMask()
    {
        waitForMask(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForMask(int wait)
    {
        _test.waitForElement(Locators.mask(), wait);
    }

    /**
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     * @return XPathLocator for the desired row
     */
    private Locator.XPathLocator getGridRow(String cellText, int index)
    {
        return Locator.xpath("(//tr[contains(@class, 'x4-grid-row')][(td|td/table/tbody/tr/td)[string() = '" + cellText + "']]["+Locator.NOT_HIDDEN+"])[" + (index + 1) + "]");
    }

    public static Locator.XPathLocator invalidField()
    {
        return Locator.xpath("//input[contains(@class, 'x4-form-field') and contains(@class, 'x4-form-invalid-field')]");
    }

    @LogMethod(quiet = true)
    public void clickExt4MenuButton(boolean wait, Locator menu, boolean onlyOpen, @LoggedParam String ... subMenuLabels)
    {
        _test.waitAndClick(menu);
        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = ext4MenuItem(subMenuLabels[i]);
            _test.waitForElement(parentLocator, 1000);
            _test.mouseOver(parentLocator);
        }
        Locator itemLocator = ext4MenuItem(subMenuLabels[subMenuLabels.length - 1]);
        if (onlyOpen)
        {
            _test.waitForElement(itemLocator, 1000);
            return;
        }
        if (wait)
            _test.waitAndClickAndWait(itemLocator);
        else
            _test.waitAndClick(itemLocator);
    }

    public void clickExt4MenuItem(String text)
    {
        _test.click(ext4MenuItem(text));
    }

    public static Locator.XPathLocator ext4MenuItem(String text)
    {
        // For now, menu lookup is no longer Ext 4 specific due to Ext 4 menus not escpaing properly
        return Locator.xpath("//span[contains(@class, 'menu-item-text') and text() = '" + text + "']");
    }

    public static Locator.XPathLocator ext4Window(String title)
    {
        return Locators.window(title);
    }

    public static Locator.XPathLocator ext4WindowButton(String windowTitle, String buttonText)
    {
        // TOD0: Check not hidden
        String windowPath = "//div[contains(@class, 'x4-window') and ./div/div/div/div/div/span[contains(@class, 'x4-window-header-text')" +
                " and contains(string(), '" + windowTitle + "')]]";
        return Locator.xpath(windowPath + Locator.ext4Button(buttonText).toXpath());
    }

    public static class Locators
    {
        public static Locator.XPathLocator checkbox(BaseWebDriverTest test, String label)
        {
            Locator.XPathLocator l = Locator.xpath("//input[contains(@class,'x4-form-checkbox')][../label[text()='" + label + "']]");
            if (!test.isElementPresent(l))
                l = Locator.xpath("//input[contains(@class,'x4-form-checkbox')][../../td/label[text()='" + label + "']]");
            return l;
        }

        public static Locator.XPathLocator radiobutton(BaseWebDriverTest test, String label)
        {
            Locator.XPathLocator l = Locator.xpath("//input[contains(@class,'x4-form-radio')][../label[contains(text(), '" + label + "')]]");
            if (!test.isElementPresent(l))
                l = Locator.xpath("//input[contains(@class,'x4-form-radio')][../../td/label[text()='" + label + "']]");
            return l;
        }

        public static Locator.XPathLocator window(String title)
        {
            return Locator.xpath("//div").withClass("x4-window").notHidden().withDescendant(Locator.xpath("//span").withClass("x4-window-header-text").withText(title));
        }

        public static Locator.XPathLocator formItemWithLabel(String label)
        {
            return Locator.tag("*").withClass("x4-form-item").withDescendant(Locator.tag("label").withText(label)).notHidden();
        }

        public static Locator.XPathLocator formItemWithLabelContaining(String label)
        {
            return Locator.tag("*").withClass("x4-form-item").withDescendant(Locator.tag("label").containing(label)).notHidden();
        }

        public static Locator.XPathLocator mask()
        {
            return Locator.xpath("//div["+Locator.NOT_HIDDEN+" and contains(@class, 'x4-mask')]");
        }

        public static Locator.XPathLocator folderManagementTreeNode(String nodeText)
        {
            return Locator.xpath("//tr").withClass("x4-grid-row").append("/td/div").withText(nodeText);
        }

        public static Locator.XPathLocator tab(String tabName)
        {
            return Locator.xpath("//a[contains(@class, 'x4-tab') and contains( normalize-space(), '" + tabName + "')]");
        }
    }

    public static Locator.XPathLocator ext4Tab(String label)
    {
        return Locator.tagWithText("span", label).withClass("x4-tab-inner").notHidden();
    }

    public void clickExtTab(String tabname)
    {
        _test.waitAndClick(Locator.xpath("//span[contains(@class, 'x4-tab-inner') and text() = '" + tabname + "']"));
    }
}
