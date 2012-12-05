/*
 * Copyright (c) 2012 LabKey Corporation
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
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
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

    @LogMethod
    public void selectComboBoxItem(Locator.XPathLocator parentLocator, String selection)
    {
        Locator l = Locator.xpath(parentLocator.getPath()+"//div[contains(@class,'arrow')]");
        _test.waitForElement(l);
        _test.click(l);
        if(_test.getBrowser().startsWith(BaseWebDriverTest.IE_BROWSER))
        {
            _test.sleep(500);
            _test.clickAt(Locator.xpath("//div/div/div[text()='" + selection + "']"), "1,1");
            _test.mouseDownAt(Locator.xpath("/html/body"), 1,1);
        }
        else
        {
            // wait for the dropdown to open
            Locator listItem = Locator.css("li.x4-boundlist-item");
            _test.waitForElement(listItem);

            // select the list item
            _test.click(listItem.withText(selection));

            // menu should disappear
            _test._shortWait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("li.x4-boundlist-item")));
        }
    }

    public void selectComboBoxItem(String label, String selection)
    {
       Ext4FieldRefWD.getForLabel(_test, label).setValue(selection);
    }

    @LogMethod
    public void selectComboBoxItemById(String labelId, String selection)
    {
        Locator.XPathLocator loc = Locator.xpath("//tbody[./tr/td/label[@id='" + labelId + "-labelEl']]");
        selectComboBoxItem(loc, selection);
    }

    @LogMethod
    public void selectRadioButton(String label, String selection)
    {
        Locator l = Locator.xpath("//div[div/label[text()='" + label + "']]//label[text()='" + selection + "']");
        if (!_test.isElementPresent(l))
        {
            // try Ext 4.1.0 version
            l = Locator.xpath("//div[./table//label[text()='" + label + "']]//label[text()='" + selection + "']");
        }
        _test.click(l);
    }

    public void selectRadioButtonById(String labelId)
    {
        Locator l = Locator.xpath("//label[@id='" + labelId + "']");
        _test.click(l);
    }

    public void clickExt4Tab(String tabname)
    {
        _test.log("Selecting Ext tab " + tabname);
        Locator l = Locator.xpath("//span[contains(@class, 'x4-tab') and text() = '" + tabname + "']");
        _test.click(l);
    }

    public void checkCheckbox(String label)
    {
        if (!isChecked(label))
        {
            Locator l = Locator.xpath("//table[contains(@class, 'x4-form-item')][.//label[text()='" + label + "']]//input[contains(@class,'x4-form-checkbox')]");
            _test.click(l);
        }
    }

    public void uncheckCheckbox(String label)
    {
        if (isChecked(label))
        {
            Locator l = Locator.xpath("//table[contains(@class, 'x4-form-cb-checked')][.//label[text()='" + label + "']]//input[contains(@class,'x4-form-checkbox')]");
            _test.click(l);
        }
    }

    public boolean isChecked(String label)
    {
        _test.assertElementPresent(Locator.xpath("//table[contains(@class, 'x4-form-item')][.//label[text()='" + label + "']]//input"));
        Locator l = Locator.xpath("//table[contains(@class, 'x4-form-cb-checked')][.//label[text()='" + label + "']]//input");
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
    @LogMethod
    public void checkGridRowCheckbox(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        if (!isChecked(rowLoc))
            _test.mouseDown(rowLoc.append("//div[contains(@class, 'x4-grid-row-checker')]"));
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
    @LogMethod
    public void uncheckGridRowCheckbox(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        if (isChecked(rowLoc))
            _test.mouseDown(rowLoc.append("//div[contains(@class, 'x4-grid-row-checker')]"));
    }

    /**
     * Click the text of an Ext4 grid row
     * Currently used only for time chart measure picker
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    @LogMethod
    public void clickGridRowText(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        _test.mouseDown(rowLoc.append("//div[contains(@class, 'x4-grid-cell')][string() = '"+cellText+"']"));
    }

    /**
     * Click the text of an Participant filter panel grid row
     * Currently used only for participant filter panel
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    public void clickParticipantFilterGridRowText(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        _test.click(rowLoc.append("//span[contains(@class, 'lk-filter-panel-label')][string() = '"+cellText+"']"));
    }

    /**
     * Determines if the specified row has a checked checkbox
     * @param rowLoc Locator provided by {@link #getGridRow(String, int)}
     * @return true if the specified row has a checked checkbox
     */
    private boolean isChecked(Locator.XPathLocator rowLoc)
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
    public boolean isChecked(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        _test.assertElementPresent(rowLoc);
        return _test.isElementPresent(rowLoc.append("[contains(@class, 'x4-grid-row-selected')]"));
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
        List<String> ids = new ArrayList<String>();
        for (String id : unfilteredIds)
        {
            if (Locator.id(id).findElements(_test._driver).size() > 0)
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
            List<Type> ret = new ArrayList<Type>(ids.size());
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
        _test.click(Locator.xpath("//span[contains(@class, 'x4-tab-inner') and contains( text(), '" + tabText + "')]"));
    }

    public void waitForMaskToDisappear()
    {
        waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForMaskToDisappear(int wait)
    {
        _test.waitForElementToDisappear(getExtMask(), wait);
    }

    public void waitForMask()
    {
        waitForMask(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForMask(int wait)
    {
        _test.waitForElement(getExtMask(), wait);
    }

    private Locator getExtMask()
    {
        return Locator.xpath("//div["+Locator.NOT_HIDDEN+" and contains(@class, 'x4-mask')]");
    }

    /**
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     * @return XPathLocator for the desired row
     */
    private Locator.XPathLocator getGridRow(String cellText, int index)
    {
        return Locator.xpath("(//tr[contains(@class, 'x4-grid-row')][td[string() = '" + cellText + "']]["+Locator.NOT_HIDDEN+"])[" + (index + 1) + "]");
    }

    public static Locator.XPathLocator invalidField()
    {
        return Locator.xpath("//input[contains(@class, 'x4-form-field') and contains(@class, 'x4-form-invalid-field')]");
    }

    public static void clickExt4MenuButton(BaseWebDriverTest test, boolean wait, Locator menu, boolean onlyOpen, String ... subMenuLabels)
    {
        test.click(menu);
        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = ext4MenuItem(subMenuLabels[i]);
            test.waitForElement(parentLocator, 1000);
            test.mouseOver(parentLocator);
        }
        Locator itemLocator = ext4MenuItem(subMenuLabels[subMenuLabels.length - 1]);
        test.waitForElement(itemLocator, 1000);
        if (onlyOpen)
            return;
        if (wait)
            test.clickAndWait(itemLocator);
        else
            test.click(itemLocator);
    }

    public void clickExt4MenuItem(String text)
    {
        _test.click(ext4MenuItem(text));
    }

    public static Locator.XPathLocator ext4MenuItem(String text)
    {
        return Locator.xpath("//span[contains(@class, 'x4-menu-item-text') and text() = '" + text + "']");
    }

    public static Locator.XPathLocator ext4Window(String title)
    {
        return Locator.xpath("//div[contains(@class, 'x4-window-header')]//span[text() = '" + title + "']");
    }
}
