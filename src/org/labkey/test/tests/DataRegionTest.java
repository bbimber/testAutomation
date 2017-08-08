/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

package org.labkey.test.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.util.Pair;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Data;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ListHelper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@Category({DailyA.class, Data.class})
public class DataRegionTest extends BaseWebDriverTest
{
    {setIsBootstrapWhitelisted(true);}
    private static final String FIRST_LINK = "First Page";
    private static final String PREV_LINK = "Previous Page";
    private static final String NEXT_LINK = "Next Page";
    private static final String LAST_LINK = "Last Page";

    private static final String LIST_NAME = "WebColors" + INJECT_CHARS_1;
    private static final ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.Integer;
    private static final String LIST_KEY_NAME = "Key";

    private static final ListHelper.ListColumn NAME_COLUMN =
            new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Color Name");
    private static final ListHelper.ListColumn HEX_COLUMN =
            new ListHelper.ListColumn("Hex", "Hex", ListHelper.ListColumnType.String, "Hexadecimal");

    private static final String LIST_DATA;
    private static final int TOTAL_ROWS;

    private static final List<Pair<String, String>> QWP_TAB_SIGNALS =
            Arrays.asList(new Pair<>("Show default view for query", "testQueryOnly"),
                    new Pair<>("Filter by Tag equals blue", "testFilterArray"),
                    new Pair<>("Sort by Tag", "testSort"),
                    new Pair<>("Hide buttons", "testHideButtons"),
                    new Pair<>("Hide Edit and Details columns", "testHideColumns"),
                    new Pair<>("Set Paging to 3 with config", "testPagingConfig"),
                    new Pair<>("Set Paging to 2 with API", "testSetPaging"),
                    new Pair<>("Parameterized Queries", "testParameterizedQueries"),
                    new Pair<>("Regression #25337", "test25337"),
                    new Pair<>("Change Page Offset", "testPageOffset"),
                    new Pair<>("Keep Removable Filters", "testRemovableFilters"),
                    new Pair<>("Show All Rows", "testShowAllTotalRows")
                    );

    private static final Pair<String, String> QWP_SCHEMA_LISTING = new Pair<>("List out all queries in schema", "testSchemaOnly");

    static
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("aqua", "#00FFFF");
        map.put("black", "#000000");
        map.put("blue", "#0000FF");
        map.put("fuchsia", "#FF00FF");
        map.put("green", "#008000");
        map.put("grey", "#808080");
        map.put("lime", "#00FF00");
        map.put("maroon", "#800000");
        map.put("navy", "#000080");
        map.put("olive", "#808000");
        map.put("purple", "#800080");
        map.put("red", "#FF0000");
        map.put("silver", "#C0C0C0");
        map.put("teal", "#008080");
        map.put("white", "#FFFFFF");
        map.put("yellow", "#FFFF00");

        StringBuilder sb = new StringBuilder();
        sb.append("Key\tName\tHex\n");
        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            sb.append(i).append("\t");
            sb.append(entry.getKey()).append("\t");
            sb.append(entry.getValue()).append("\n");
            i++;
        }

        LIST_DATA = sb.toString();
        TOTAL_ROWS = map.size();
    }

    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected String getProjectName()
    {
        return "DataRegionProject";
    }

    @Test
    public void testSteps() throws Exception
    {
        createList();

        enableComplianceIfInstalled();
        clickAndWait(Locator.linkWithText(LIST_NAME));
        URL url = getURL();
        dataRegionTest(url, INJECT_CHARS_1);
        dataRegionTest(url, INJECT_CHARS_2);
        exportLoggingTest();

        testQWPDemoPage();
    }

    private void testQWPDemoPage()
    {
        log("Begin testing QWPDemo page");
        beginAt("/query/" + getProjectName() + "/QWPDemo.view");

        log("Drop and reload QWPDemo test data");
        clickButton("Drop schema and clear test data");
        waitForElement(Locator.button("Populate test data"));
        clickButton("Populate test data");
        sleep(1000);

        log("Testing " + QWP_SCHEMA_LISTING.first);
        click(Locator.linkWithText(QWP_SCHEMA_LISTING.first));

        if (pageHasAlert(3000, QWP_SCHEMA_LISTING.second)) {
            dismissAllAlerts();
            fail(QWP_SCHEMA_LISTING.first + " failed");
        }
        waitForElement(Locator.css("span.labkey-wp-title-text").withText(QWP_SCHEMA_LISTING.first));

        QWP_TAB_SIGNALS.stream().forEach(this::testQWPTab);

        log("Drop QWPDemo test data");
        beginAt("/query/" + getProjectName() + "/QWPDemo.view");
        clickButton("Drop schema and clear test data"); // drop domain, needed for clean up project
    }

    // check every 500ms for specified wait amount for either alert or successSignal
    private boolean pageHasAlert(long wait, String successSignal)
    {
        long t= System.currentTimeMillis();
        long end = t + wait;
        while (System.currentTimeMillis() < end)
        {
            if (null != getAlertIfPresent())
                return true;
            if (isElementPresent(Locators.pageSignal(successSignal)))
                return false;
            sleep(500);
        }
        return false;
    }

    private void testQWPTab(Pair<String, String> titleSignalPair)
    {
        log("Testing " + titleSignalPair.first);
        if (IS_BOOTSTRAP_LAYOUT && !titleSignalPair.first.equals("Change Page Offset")) // need to fix this one for the new UI
        {
            click(Locator.linkWithText(titleSignalPair.first));
            if (pageHasAlert(3000, titleSignalPair.second))
            {
                dismissAllAlerts();
                fail(titleSignalPair.first + " failed");
            }
            waitForElement(Locator.css(".labkey-data-region"));
        }
    }

    private void exportLoggingTest()
    {
        DataRegionTable list = new DataRegionTable(INJECT_CHARS_2, getDriver());
        DataRegionExportHelper exportHelper = new DataRegionExportHelper(list);
        exportHelper.exportText();
        goToAdminConsole().clickAuditLog();
        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.name("view"), "Query export events"));

        DataRegionTable auditTable =  new DataRegionTable("query", getDriver());
        String[][] columnAndValues = new String[][] {{"Created By", getDisplayName()},
                {"Project", getProjectName()}, {"Container", getProjectName()}, {"SchemaName", "lists"},
                {"QueryName", LIST_NAME}, {"Comment", "Exported to TSV"}};
        for (String[] columnAndValue : columnAndValues)
        {
            log("Checking column: "+ columnAndValue[0]);
            assertEquals(columnAndValue[1], auditTable.getDataAsText(0, columnAndValue[0]));
        }

        if (IS_BOOTSTRAP_LAYOUT)
        {
            list = new DataRegionTable("query", getDriver());
            list.detailsLink(list.getRowIndex("Project", getProjectName())).click();
        }
        else
        {
            clickAndWait(Locator.linkContainingText("details"));
        }
        assertTextPresent(LIST_NAME);
    }

    private void createList()
    {
        _containerHelper.createProject(getProjectName(), null);

        log("Define list");
        _listHelper.createList(getProjectName(), LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, NAME_COLUMN, HEX_COLUMN);

        log("Upload data");
        _listHelper.uploadData(LIST_DATA);
    }

    private void dataRegionTest(URL url, String dataRegionName) throws MalformedURLException
    {
        log("** Beginning test for dataRegionName: " + dataRegionName);

        // Issue 11392: DataRegion name escaping in button menus.  Append evil dataRegionName parameter.
        String encodedName = EscapeUtil.encode(dataRegionName);
        url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "&dataRegionName=" + encodedName);
        beginAt(url.getFile());

        DataRegionTable table = new DataRegionTable(dataRegionName, getDriver());
        assertEquals(TOTAL_ROWS, table.getDataRowCount());
        assertEquals("aqua", table.getDataAsText(0, "Name"));
        assertEquals("#FFFF00", table.getDataAsText(15, "Hex"));

        if (IS_BOOTSTRAP_LAYOUT)
        {
            assertElementNotPresent(Locator.xpath("//button[ ./i[@class='fa fa-chevron-right']]"));
            assertElementNotPresent(Locator.xpath("//button[ ./i[@class='fa fa-chevron-left']]"));
        }
        else
        {
            assertElementPresent(Locator.lkButton("Paging"));
            assertElementNotPresent(Locator.linkWithTitle(PREV_LINK));
            assertElementNotPresent(Locator.linkWithTitle(NEXT_LINK));
        }

        log("Test 3 per page");
        table.setMaxRows(3);
        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
        {
            table.getPagingWidget().viewPagingOptions();
            assertElementPresent(Locator.linkContainingText("3 per page"));
            assertElementPresent(Locator.linkContainingText("20 per page"));
            assertElementPresent(Locator.linkContainingText("40 per page"));
            assertElementPresent(Locator.linkContainingText("100 per page"));
            assertElementPresent(Locator.linkContainingText("250 per page"));
        }
        else
        {
            WebElement menuItem = _ext4Helper.openMenu(Locator.lkButton("Paging"), "3 per page");
            assertNotNull("'Paging' menu selection doesn't match URL parameter", Locator.xpath("preceding-sibling::div").withClass("fa-check-square-o").findElementOrNull(menuItem));
            assertElementPresent(Locator.linkWithText("3 per page"));
            assertElementPresent(Locator.linkWithText("40 per page"));
            assertElementPresent(Locator.linkWithText("100 per page"));
            assertElementPresent(Locator.linkWithText("250 per page"));
            assertElementPresent(Locator.linkWithText("1000 per page"));
            assertElementPresent(Locator.linkWithText("Show Selected"));
            assertElementPresent(Locator.linkWithText("Show Unselected"));
            assertElementPresent(Locator.linkWithText("Show All"));
        }
        table.assertPaginationText(1, 3, 16);
        assertEquals(3, table.getDataRowCount());

        log("Test 5 per page");
        table.setMaxRows(5);
        table.assertPaginationText(1, 5, 16);
        assertEquals(5, table.getDataRowCount());
        assertEquals("aqua", table.getDataAsText(0, "Name"));
        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
        {
            assertEquals(table.getPagingWidget().menuOptionEnabled("Show first", "Show first"), false);
            assertEquals(table.getPagingWidget().menuOptionEnabled("Show last", "Show last"), true);
            assertEquals(table.getPagingWidget().pagingButtonEnabled(true), false);
            assertEquals(table.getPagingWidget().pagingButtonEnabled(false), true);
        }
        else
        {
            assertElementNotPresent(Locator.linkWithTitle(FIRST_LINK));
            assertElementNotPresent(Locator.linkWithTitle(PREV_LINK));
            assertElementPresent(Locator.linkWithTitle(NEXT_LINK));
            assertElementPresent(Locator.linkWithTitle(LAST_LINK));
        }

        log("Next Page");
        table.pageNext();
        table.assertPaginationText(6, 10, 16);
        assertEquals(5, table.getDataRowCount());
        assertEquals("grey", table.getDataAsText(0, "Name"));
        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
        {
            assertEquals(table.getPagingWidget().menuOptionEnabled("Show first", "Show first"), true);
            assertEquals(table.getPagingWidget().menuOptionEnabled("Show last", "Show last"), true);
            assertEquals(table.getPagingWidget().pagingButtonEnabled(true), true);
            assertEquals(table.getPagingWidget().pagingButtonEnabled(false), true);
        }
        else
        {
            assertElementNotPresent(Locator.linkWithTitle(FIRST_LINK));
            assertElementPresent(Locator.linkWithTitle(PREV_LINK));
            assertElementPresent(Locator.linkWithTitle(NEXT_LINK));
            assertElementPresent(Locator.linkWithTitle(LAST_LINK));
        }


        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
        {
            log("Last Page");
            table.pageLast();
            table.assertPaginationText(16, 16, 16);
            assertEquals(1, table.getDataRowCount());
            assertEquals("yellow", table.getDataAsText(0, "Name"));
            assertEquals(table.getPagingWidget().menuOptionEnabled("Show first", "Show first"), true);
            assertEquals(table.getPagingWidget().menuOptionEnabled("Show last", "Show last"), false);
            assertEquals(table.getPagingWidget().pagingButtonEnabled(true), true);
            assertEquals(table.getPagingWidget().pagingButtonEnabled(false), false);

            log("Previous Page");
            table.pagePrev();
            table.assertPaginationText(11, 15, 16);
            assertEquals(5, table.getDataRowCount());
            assertEquals("purple", table.getDataAsText(0, "Name"));
            assertEquals(table.getPagingWidget().menuOptionEnabled("Show first", "Show first"), true);
            assertEquals(table.getPagingWidget().menuOptionEnabled("Show last", "Show last"), true);
            assertEquals(table.getPagingWidget().pagingButtonEnabled(true), true);
            assertEquals(table.getPagingWidget().pagingButtonEnabled(false), true);
        }
        else
        {
            log("Last Page");
            table.pageLast();
            table.assertPaginationText(16, 16, 16);
            assertEquals(1, table.getDataRowCount());
            assertEquals("yellow", table.getDataAsText(0, "Name"));
            assertElementPresent(Locator.linkWithTitle(FIRST_LINK));
            assertElementPresent(Locator.linkWithTitle(PREV_LINK));
            assertElementNotPresent(Locator.linkWithTitle(NEXT_LINK));
            assertElementNotPresent(Locator.linkWithTitle(LAST_LINK));

            log("Previous Page");
            table.pagePrev();
            table.assertPaginationText(11, 15, 16);
            assertEquals(5, table.getDataRowCount());
            assertEquals("purple", table.getDataAsText(0, 3));
            assertElementPresent(Locator.linkWithTitle(FIRST_LINK));
            assertElementPresent(Locator.linkWithTitle(PREV_LINK));
            assertElementPresent(Locator.linkWithTitle(NEXT_LINK));
            assertElementNotPresent(Locator.linkWithTitle(LAST_LINK));
        }


        log("Setting a filter should go back to first page");
        table.setFilter(NAME_COLUMN.getName(), "Does Not Equal", "aqua");
        table.assertPaginationText(1, 5, 15);
        assertEquals("black", table.getDataAsText(0, "Name"));

        log("Show Selected");
        table.checkAllOnPage();
        if (IS_BOOTSTRAP_LAYOUT)
        {
            waitForElement(Locator.tagWithAttribute("div", "data-msgpart", "selection"));
            WebElement msgDiv = Locator.tagWithAttribute("div", "data-msgpart", "selection").findElement(getDriver());
            assertEquals(msgDiv.getText().contains("Selected 5 of 15 rows."), true);

            table.showSelected();
            assertEquals(5, table.getDataRowCount());
            assertElementPresent(Locator.xpath("//div[contains(@class,'labkey-pagination')]"));

            table.showAll();
            assertEquals(15, table.getDataRowCount());
            assertElementPresent(Locator.xpath("//div[contains(@class,'labkey-pagination')]"));

        }
        else
        {
            waitForElements(Locator.css(".labkey-dataregion-msg"), 3);
            boolean found = false;
            for (WebElement msg : Locator.css(".labkey-dataregion-msg").findElements(getDriver()))
            {
                if (msg.getText().contains("Selected 5 of 15 rows."))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
                fail("Didn't find 'Selected 5 of 15 rows.' message");
            clickButton("Paging", 0);
            clickAndWait(Locator.linkWithText("Show Selected"));
            assertEquals(5, table.getDataRowCount());
            assertElementNotPresent(Locator.linkWithTitle(FIRST_LINK));
            assertElementNotPresent(Locator.linkWithTitle(PREV_LINK));
            assertElementNotPresent(Locator.linkWithTitle(NEXT_LINK));
            assertElementNotPresent(Locator.linkWithTitle(LAST_LINK));


            log("Show All");
            clickButton("Paging", 0);
            clickAndWait(Locator.linkWithText("Show All"));
            assertEquals(15, table.getDataRowCount());
            assertElementNotPresent(Locator.linkWithTitle(FIRST_LINK));
            assertElementNotPresent(Locator.linkWithTitle(PREV_LINK));
            assertElementNotPresent(Locator.linkWithTitle(NEXT_LINK));
            assertElementNotPresent(Locator.linkWithTitle(LAST_LINK));

            log("Test 1000 per page");
            clickButton("Paging", 0);
            clickAndWait(Locator.linkWithText("1000 per page"));
            assertElementNotPresent(Locator.linkWithTitle(FIRST_LINK));
            assertElementNotPresent(Locator.linkWithTitle(PREV_LINK));
            assertElementNotPresent(Locator.linkWithTitle(NEXT_LINK));
            assertElementNotPresent(Locator.linkWithTitle(LAST_LINK));
        }
    }

    private void enableComplianceIfInstalled()
    {
        // Make sure it works with Compliance on (which enables Elec Sign control)
        // Have to do what enableModule does in order to check if it's installed
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));

        try
        {
            scrollIntoView(Locator.checkboxByTitle("Compliance"));
            checkCheckbox(Locator.checkboxByTitle("Compliance"));
            clickButton("Update Folder");
        }
        catch (NoSuchElementException missingModule)
        {
            log("Compliance module not found; ignoring");
        }
        goToProjectHome();
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
