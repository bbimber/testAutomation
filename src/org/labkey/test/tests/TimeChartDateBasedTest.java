/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

/**
 * User: cnathe
 * Date: 11/5/12
 */
@Category({DailyB.class, Reports.class})
public class TimeChartDateBasedTest extends TimeChartTest
{
    private static final String REPORT_NAME_1 = "TimeChartTest Report";
    private static final String REPORT_NAME_2 = "TimeChartTest 2Report";
    private static final String REPORT_NAME_3 = "TimeChartTest Multi-Measure Report";
    private static final String X_AXIS_LABEL = "New X-Axis Label";
    private static final String X_AXIS_LABEL_MANUAL = "New X-Axis Label Manual";
    private static final String Y_AXIS_LABEL = "New Y-Axis Label";
    private static final String CHART_TITLE = "New Chart Title";
    private static final String PER_GROUP = "One Chart Per Group";
    private static final String REPORT_DESCRIPTION = "This is a report generated by the TimeChartDateBasedTest";

    @Override
    @LogMethod protected void doCreateSteps()
    {
        configureStudy();
        windowMaximize();
    }

    @Override
    public void doVerifySteps()
    {
        axisRangeTest();

        createChartTest();

        stdDevRegressionTest();

        visualizationTest();

        generateChartPerParticipantTest();

        saveTest();

        timeChartPermissionsTest();

        pointClickFunctionTest();

        multiMeasureTimeChartTest();

        createParticipantGroups();

        participantGroupTimeChartTest();

        multiAxisTimeChartTest();

        aggregateTimeChartUITest();

        filteredTimeChartRegressionTest();
    }

    private static final String SVG_AXIS_X =              "Created with Rapha\u00ebl 2.1.0\nHIV Test Results, Lab Results: 249320107\n0\n50\n100\n150\n200\nDays Since Start Date\nViral Load Quantified (copies/ml)\n50000.0\n100000.0\n150000.0\n200000.0\n250000.0\n300000.0\n350000.0\n400000.0\n450000.0\n500000.0\n550000.0\n600000.0\n650000.0\nCD4+ (cells/mm3)\n200.0\n300.0\n400.0\n500.0\n600.0\n700.0\n800.0\n900.0\n1000.0\n1100.0\n1200.0\n1300.0\n249320107 CD4+(cells/mm3)\n249320107 Viral LoadQuantified (copies/ml)";
    private static final String SVG_AXIS_X_LEFT =         "Created with Rapha\u00ebl 2.1.0\nHIV Test Results, Lab Results: 249320107\n0\n50\n100\n150\n200\nDays Since Start Date\nViral Load Quantified (copies/ml)\n200000.0\n210000.0\n220000.0\n230000.0\n240000.0\n250000.0\n260000.0\n270000.0\nCD4+ (cells/mm3)\n200.0\n300.0\n400.0\n500.0\n600.0\n700.0\n800.0\n900.0\n1000.0\n1100.0\n1200.0\n1300.0\n249320107 CD4+(cells/mm3)\n249320107 Viral LoadQuantified (copies/ml)";
    private static final String SVG_AXIS_X_LEFT_RIGHT =   "Created with Rapha\u00ebl 2.1.0\nHIV Test Results, Lab Results: 249320107\n0\n50\n100\n150\n200\nDays Since Start Date\nViral Load Quantified (copies/ml)\n200000.0\n210000.0\n220000.0\n230000.0\n240000.0\n250000.0\n260000.0\n270000.0\nCD4+ (cells/mm3)\n250.0\n300.0\n350.0\n400.0\n450.0\n500.0\n550.0\n600.0\n249320107 CD4+(cells/mm3)\n249320107 Viral LoadQuantified (copies/ml)";
    private static final String AXIS_TIME_CHART = "Axis Time Chart";
    @LogMethod private void axisRangeTest()
    {
        clickFolder(getFolderName());
        goToManageViews();
        _extHelper.clickMenuButton("Create", "Time Chart");
        clickChooseInitialMeasure();
        _ext4Helper.clickGridRowText("Viral Load Quantified (copies/ml)", 0);
        clickButton("Select", 0);
        waitForElement(Locator.css("svg text").withText("HIV Test Results"));

        enterMeasuresPanel();
        addMeasure();
        _ext4Helper.clickGridRowText("CD4+ (cells/mm3)", 0);
        clickButton("Select", 0);
        waitForElement(Locator.tag("tr").withClass("x4-grid-row-selected").containing("CD4+ (cells/mm3)"));
        _ext4Helper.selectComboBoxItem("Draw y-axis on:", "Right");
        applyChanges();
        waitForElement(Locator.css("svg").withText("Viral Load Quantified (copies/ml)"), WAIT_FOR_JAVASCRIPT, false);

        goToGroupingTab();
        setNumberOfCharts("One Chart Per Participant");
        applyChanges();
        waitForElement(Locator.css("svg").withText("HIV Test Results, Lab Results: 249318596"), WAIT_FOR_JAVASCRIPT, false);

        goToAxisTab("Days Since Start Date");
        click(Locator.id("xaxis_range_automatic_per_chart-inputEl"));
        applyChanges();
        waitForElement(Locator.css("svg").withText("HIV Test Results, Lab Results: 249320107"), WAIT_FOR_JAVASCRIPT, false);
        assertSVG(SVG_AXIS_X, 1);

        goToAxisTab("Viral Load Quantified (copies/ml)");
        click(Locator.id("leftaxis_range_automatic_per_chart-inputEl"));
        applyChanges();
        waitForElement(Locator.css("svg").withText("HIV Test Results, Lab Results: 249320107"), WAIT_FOR_JAVASCRIPT, false);
        assertSVG(SVG_AXIS_X_LEFT, 1);

        goToAxisTab("CD4+ (cells/mm3)");
        click(Locator.id("rightaxis_range_automatic_per_chart-inputEl"));
        applyChanges();
        waitForElement(Locator.css("svg").withText("HIV Test Results, Lab Results: 249320107"), WAIT_FOR_JAVASCRIPT, false);
        assertSVG(SVG_AXIS_X_LEFT_RIGHT, 1);

        openSaveMenu();
        setFormElement(Locator.name("reportName"), AXIS_TIME_CHART);
        saveReport(true);
    }

    @LogMethod public void createChartTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Views");
        portalHelper.addWebPart("Datasets");
        portalHelper.addWebPart("Specimens");
        goToManageViews();
        _extHelper.clickMenuButton("Create", "Time Chart");
        clickChooseInitialMeasure();
        waitForText("NAbAssay", WAIT_FOR_JAVASCRIPT);

        log("Test measure search.");
        _extHelper.setExtFormElementByType(ADD_MEASURE_DIALOG, "text", "nab");
        sleep(500);
        // Count search results (11 in study's NAb assay)
        Assert.assertEquals("", 11, getXpathCount(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_DIALOG) + "//div[contains(@class, 'x4-grid-view')]/table/tbody/tr")));

        log("Check for appropriate message for measure with no data.");
        _ext4Helper.clickGridRowText("Cutoff Percentage (3)", 0);
        clickButton("Select", 0);
        waitForText("No data found for the following measures/dimensions: RunCutoff3", WAIT_FOR_JAVASCRIPT);
    }

    // Regression test for "11764: Time Chart Wizard raises QueryParseException on 'StdDev' measure"
    @LogMethod public void stdDevRegressionTest()
    {
        log("StdDev regression check");
        enterMeasuresPanel();
        clickButton("Remove Measure", 0);
        applyChanges();
        waitForText("No measure selected.", WAIT_FOR_JAVASCRIPT);
        enterMeasuresPanel();
        addMeasure();
        _ext4Helper.clickGridRowText("StdDev", 0);
        clickButton("Select", 0);
        waitForText("StdDev from LuminexAssay");
        applyChanges();
        waitForElement(Locator.css("svg").withText("Days Since Start Date"), WAIT_FOR_JAVASCRIPT, false);
        waitForText("StdDev", WAIT_FOR_JAVASCRIPT); // left-axis label
        waitForText("LuminexAssay", WAIT_FOR_JAVASCRIPT); // main title
    }

    @LogMethod public void visualizationTest()
    {
        log("Check visualization");
        enterMeasuresPanel();
        clickButton("Remove Measure", 0);
        applyChanges();
        waitForText("No measure selected.", WAIT_FOR_JAVASCRIPT);
        enterMeasuresPanel();
        addMeasure();
        waitForElement(Locator.name("filterSearch"));
        sleep(1000);// TODO: why doesnt the wait above work?
        _extHelper.setExtFormElementByType(ADD_MEASURE_DIALOG, "text", "viral");
        waitForElementToDisappear(Locator.xpath(_extHelper.getExtDialogXPath(ADD_MEASURE_DIALOG) + "//div[contains(@class, 'x4-grid-view')]/table/tbody/tr").index(2), WAIT_FOR_JAVASCRIPT);
        _ext4Helper.clickGridRowText("Viral Load Quantified (copies/ml)", 0);
        clickButton("Select", 0);
        waitForText("Viral Load Quantified (copies/ml) from HIV Test Results");
        applyChanges();
        waitForText("Days Since Start Date", WAIT_FOR_JAVASCRIPT); // x-axis label
        waitForText("Viral Load Quantified (copies/ml)", WAIT_FOR_JAVASCRIPT); // left-axis label
        waitForText("HIV Test Results", WAIT_FOR_JAVASCRIPT); // main title
        assertTextNotPresent("No data found");

        clickButton("View Data", 0);
        waitForElement(Locator.paginationText(33));
        _ext4Helper.checkGridRowCheckbox("249325717");
        waitForElement(Locator.paginationText(38));
        _ext4Helper.uncheckGridRowCheckbox("249320127");
        waitForElement(Locator.paginationText(31));

        // verify column headers for date based plotting option
        assertTextPresent("Participant ID");
        assertTextPresent("Visit Date");
        assertTextPresent("Visit");
        assertTextPresent("Viral Load Quantified");
        assertTextPresent("Start Date");
        assertTextPresent("Days");
        assertTextNotPresent("sequencenum");

        log("Test X-Axis");
        clickButton("View Chart(s)", 0);
        _ext4Helper.waitForMaskToDisappear();
        goToAxisTab("Days Since Start Date");
        _ext4Helper.selectComboBoxItem("Draw x-axis as:", "Weeks");
        applyChanges();
        waitForText("Weeks Since Start Date", WAIT_FOR_JAVASCRIPT);
        goToAxisTab("Weeks Since Start Date");
        setAxisValue(Axis.X, null, null, null, X_AXIS_LABEL, null, null, new String[]{X_AXIS_LABEL}, null);

        goToAxisTab(X_AXIS_LABEL);
        _ext4Helper.selectComboBoxItem("Draw x-axis as:", "Days");
        Assert.assertEquals(X_AXIS_LABEL, getFormElement(Locator.name("x-axis-label-textfield"))); // Label shouldn't change automatically once it has been set manually

        // set manual x-axis range
        goToAxisTab(X_AXIS_LABEL);
        setAxisValue(Axis.X, "xaxis_range_manual", "15", "40", X_AXIS_LABEL_MANUAL, null, null, new String[] {X_AXIS_LABEL_MANUAL,"15","20","25","30","35","40"}, null);

        log("Test Y-Axis");
        goToAxisTab("Viral Load Quantified (copies/ml)");
        setAxisValue(Axis.LEFT, "leftaxis_range_manual", "200000", "400000", Y_AXIS_LABEL, null, null, new String[] {Y_AXIS_LABEL}, new String[] {"500000","200000"});
        goToAxisTab(Y_AXIS_LABEL);
        setAxisValue(Axis.LEFT, "leftaxis_range_manual", "10000", "1000000", null, "leftaxis_scale", "Log", new String[] {"100000"}, null );
    }

    @LogMethod public void generateChartPerParticipantTest()
    {
        goToGroupingTab();
        setParticipantSelection(PARTICIPANTS);
        setNumberOfCharts(ONE_CHART_PER_PARTICIPANT);
        _extHelper.uncheckCheckbox("Show Mean"); // select show mean
        _extHelper.checkCheckbox("Show Individual Lines"); // de-select show individual lines
        applyChanges();
        waitForText("HIV Test Results: 249318596");
        assertTextPresentInThisOrder("HIV Test Results: 249318596", "HIV Test Results: 249320107", "HIV Test Results: 249320489");

        goToAxisTab("HIV Test Results: 249318596");
        setChartTitle(CHART_TITLE);
        applyChanges();
        waitForText(CHART_TITLE);
        assertTextPresent(CHART_TITLE, 6); // 5 for individual chart titles + 1 for chart title in thumbnail preview on save dialog

        // re-select participant
        _ext4Helper.checkGridRowCheckbox("249320127");
        waitForText(CHART_TITLE + ": 249320127", WAIT_FOR_JAVASCRIPT);
        assertTextPresent(CHART_TITLE, 7); // 6 for individual chart titles + 1 for chart title in thumbnail preview on save dialog
    }

    @LogMethod public void saveTest()
    {
        openSaveMenu();
        assertTextPresent("Report Name");
        _extHelper.setExtFormElementByLabel("Report Name:", REPORT_NAME_1);
        _extHelper.setExtFormElementByLabel("Report Description:", REPORT_DESCRIPTION);
        saveReport(true);
        waitForText(CHART_TITLE);
        assertTextPresent(CHART_TITLE, 6); // once for each individual chart title (note: save dialog thumbnail preview hasn't been rendered yet)

        clickButton("Save As", 0);
        waitForText("Report Name");
        _extHelper.setExtFormElementByLabel("Report Name:", REPORT_NAME_2);
        _extHelper.setExtFormElementByLabel("Report Description:", "This is another report generated by the TimeChartTest");
        _ext4Helper.selectRadioButton("Viewable By:", "Only me");
        saveReport(true);
        waitForText(CHART_TITLE);
        assertTextPresent(CHART_TITLE, 6); // once for each individual chart title (note: save dialog thumbnail preview hasn't been rendered yet)

        log("Verify saved report");
        goToManageViews();
        waitForText(REPORT_NAME_1, WAIT_FOR_JAVASCRIPT);
        assertTextPresent(REPORT_NAME_2);
        click(Locator.tagWithText("div", REPORT_NAME_1));
        assertTextPresent(REPORT_DESCRIPTION);
        clickAndWait(Locator.xpath("//a[text()='edit' and contains(@href, '"+REPORT_NAME_1.replace(" ", "%20")+"')]"));
        waitForText(X_AXIS_LABEL_MANUAL, WAIT_FOR_JAVASCRIPT);
        assertTextPresent(CHART_TITLE, 6); // once for each individual chart title (note: save dialog thumbnail preview hasn't been rendered yet)
        pushLocation();
        pushLocation();
    }

    @LogMethod public void timeChartPermissionsTest()
    {
        log("Check Time Chart Permissions");
        createUser(USER1, null);
        clickProject(getProjectName());
        clickFolder(getFolderName());
        enterPermissionsUI();
        setUserPermissions(USER1, "Reader");
        _securityHelper.setSiteGroupPermissions("Guests", "Reader");
        clickButton("Save and Finish");
        impersonate(USER1);
        popLocation(); // Saved chart
        waitForText(CHART_TITLE);
        assertElementNotPresent(Locator.ext4Button("Edit"));
        assertElementNotPresent(Locator.ext4Button("Save"));
        assertElementPresent(Locator.ext4Button("Save As"));
        clickFolder(getFolderName());
        assertTextNotPresent(REPORT_NAME_2);
        stopImpersonating();
        signOut();
        popLocation(); // Saved chart
        waitForText(CHART_TITLE);
        assertElementNotPresent(Locator.ext4Button("Save"));
        assertElementNotPresent(Locator.ext4Button("Save As"));
        simpleSignIn();
    }

    @LogMethod public void pointClickFunctionTest()
    {
        log("Check Time Chart Point Click Function (Developer Only)");
        clickProject(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        waitForText(REPORT_NAME_1, WAIT_FOR_JAVASCRIPT);
        click(Locator.tagWithText("div", REPORT_NAME_1));
        clickAndWait(Locator.xpath("//a[text()='edit' and contains(@href, '"+REPORT_NAME_1.replace(" ", "%20")+"')]"));
        waitForText(X_AXIS_LABEL_MANUAL, WAIT_FOR_JAVASCRIPT);
        // change to the data points are visible again
        goToAxisTab(Y_AXIS_LABEL);
        setAxisValue(Axis.LEFT, "leftaxis_range_automatic", null, null, null, "leftaxis_scale", "Linear", null, null);
        goToAxisTab(X_AXIS_LABEL_MANUAL);
        setAxisValue(Axis.X, "xaxis_range_automatic", null, null, null, null, null, null, null);
        waitForText("249318596,\n Days", 20, WAIT_FOR_JAVASCRIPT); // 10 in first ptid chart and 10 in save dialog thumbnail preview
        // open the developer panel and verify that it is disabled by default
        assertElementPresent(Locator.ext4Button("Developer"));
        goToDeveloperTab();
        assertElementPresent(Locator.ext4Button("Enable"));
        assertElementNotPresent(Locator.ext4Button("Disable"));
        // enable the feature and verify that you can switch tabs
        clickButton("Enable", 0);
        _ext4Helper.clickTabContainingText("Help");
        assertTextPresentInThisOrder("Your code should define a single function", "data:", "columnMap:", "measureInfo:", "clickEvent:");
        _ext4Helper.clickTabContainingText("Source");
        String fn = _extHelper.getCodeMirrorValue("point-click-fn-textarea");
        if (fn != null)
            Assert.assertTrue("Default point click function not inserted in to editor", fn.startsWith("function (data, columnMap, measureInfo, clickEvent) {"));
        // apply the default point click function
        applyChanges();
        click(Locator.css("svg a circle"));
        _extHelper.waitForExtDialog("Data Point Information");
        waitAndClick(Locator.ext4Button("OK"));
        // open developer panel and test JS function validation
        goToDeveloperTab();
        _extHelper.setCodeMirrorValue("point-click-fn-textarea", "");
        waitAndClick(Locator.ext4Button("OK"));
        assertTextPresent("Error: the value provided does not begin with a function declaration.");
        _extHelper.setCodeMirrorValue("point-click-fn-textarea", "function(){");
        waitAndClick(Locator.ext4Button("OK"));
        assertTextPresent("Error parsing the function:");
        clickButton("Disable", 0);
        _extHelper.waitForExtDialog("Confirmation...");
        _ext4Helper.clickWindowButton("Confirmation...", "Yes", 0, 0);
        clickButton("Enable", 0);
        // test use-case to navigate to participang page on click
        String function = getFileContents(TEST_DATA_API_PATH + "/timeChartPointClickTestFn.js");
        _extHelper.setCodeMirrorValue("point-click-fn-textarea", function);
        applyChanges();
        openSaveMenu();
        saveReport(false);

        pushLocation(); // for impersonation test
        pushLocation(); // for impersonation test

        goToManageViews();
        waitForText(REPORT_NAME_1, WAIT_FOR_JAVASCRIPT);
        click(Locator.tagWithText("div", REPORT_NAME_1));
        clickAndWait(Locator.xpath("//a[text()='edit' and contains(@href, '"+REPORT_NAME_1.replace(" ", "%20")+"')]"));
        waitForText(X_AXIS_LABEL_MANUAL, WAIT_FOR_JAVASCRIPT);
        clickAndWait(Locator.css("svg a circle"), WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Participant - 249318596");

        // verify that only developers can see the button to add point click function
        createUser(USER2, null);
        clickProject(getProjectName());
        clickFolder(getFolderName());
        enterPermissionsUI();
        setUserPermissions(USER2, "Editor");
        // USER2 is not yet a developer, so shouldn't have permissions to this feature
        impersonate(USER2);
        popLocation();
        waitForText(X_AXIS_LABEL_MANUAL, WAIT_FOR_JAVASCRIPT);
        assertElementNotPresent(Locator.ext4Button("Developer"));
        stopImpersonating();
        // give USER2 developer perms and try again
        createSiteDeveloper(USER2);
        impersonate(USER2);
        popLocation();
        waitForText(X_AXIS_LABEL_MANUAL, WAIT_FOR_JAVASCRIPT);
        assertElementPresent(Locator.ext4Button("Developer"));
        stopImpersonating();
    }

    @LogMethod public void multiMeasureTimeChartTest()
    {
        log("Create multi-measure time chart.");
        clickProject(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        _extHelper.clickMenuButton("Create", "Time Chart");
        clickChooseInitialMeasure();
        _ext4Helper.clickGridRowText("CD4+ (cells/mm3)", 0);
        clickButton("Select", 0);
        waitForElement(Locator.css("svg").withText("Days Since Start Date"), WAIT_FOR_JAVASCRIPT, false);
        enterMeasuresPanel();
        addMeasure();
        _ext4Helper.clickGridRowText("Lymphs (cells/mm3)", 0);
        clickButton("Select", 0);
        waitForText("Lymphs (cells/mm3) from Lab Results");
        applyChanges();
        waitForElement(Locator.css("svg").withText("249318596 Lymphs (cells/mm3)"), WAIT_FOR_JAVASCRIPT, false);
        goToGroupingTab();
        setParticipantSelection(PARTICIPANTS);
        setNumberOfCharts(ONE_CHART_PER_MEASURE);
        applyChanges();
        waitForText("CD4+ (cells/mm3), Lymphs (cells/mm3)"); // y-axis default label
        goToAxisTab("Lab Results: CD4+ (cells/mm3)");
        setChartTitle(CHART_TITLE);
        applyChanges();
        waitForText(CHART_TITLE);
        assertTextPresent(CHART_TITLE, 3); // 2 for individual chart titles + 1 for chart title in thumbnail preview on save dialog

        openSaveMenu();
        _extHelper.setExtFormElementByLabel("Report Name:", REPORT_NAME_3);
        saveReport(true);
        waitForText(CHART_TITLE);
        assertTextPresent(CHART_TITLE, 2); // once for each individual chart title (note: save dialog thumbnail preview hasn't been rendered yet)

        clickFolder(getFolderName());
        goToManageViews();
        waitAndClick(Locator.tagWithText("div", REPORT_NAME_3));
        clickAndWait(Locator.linkWithText("edit"));
        waitForText(CHART_TITLE);
        assertTextPresent("Days Since Start Date", 2); // X-Axis labels for each measure
        assertTextPresent(CHART_TITLE+": Lymphs (cells/mm3)", 1); // Title
        assertTextPresent(CHART_TITLE+": CD4+ (cells/mm3)", 1); // Title
    }

    // This SVG text might change (due to shared axis ranges) if different groups are selected
    private static final String SVG_PARTICIPANTGROUP_SOME = "Created with Rapha\u00ebl 2.1.0\nNew Chart Title: Some Participants\n0\n50\n100\n150\n200\n250\n300\n350\nDays Since Start Date\nCD4+ (cells/mm3), Lymphs (cells/mm3)\n200.0\n400.0\n600.0\n800.0\n1000.0\n1200.0\n1400.0\n1600.0\n1800.0\n2000.0\n2200.0\n249318596 Lymphs(cells/mm3)\n249320107 Lymphs(cells/mm3)\n249318596 CD4+(cells/mm3)\n249320107 CD4+(cells/mm3)";
    private static final String SVG_PARTICIPANTGROUP_SOME_MODIFIED = "Created with Rapha\u00ebl 2.1.0\nNew Chart Title: Some Participants\n50\n100\n150\n200\n250\n300\n350\nDays Since Start Date\nCD4+ (cells/mm3), Lymphs (cells/mm3)\n200.0\n400.0\n600.0\n800.0\n1000.0\n1200.0\n1400.0\n1600.0\n1800.0\n249318596 Lymphs(cells/mm3)\n249318596 CD4+(cells/mm3)";
    private static final String SVG_PARTICIPANTGROUP_OTHER = "Created with Rapha\u00ebl 2.1.0\nNew Chart Title: Other Participants\n0\n50\n100\n150\n200\n250\n300\n350\nDays Since Start Date\nCD4+ (cells/mm3), Lymphs (cells/mm3)\n200.0\n400.0\n600.0\n800.0\n1000.0\n1200.0\n1400.0\n1600.0\n1800.0\n2000.0\n2200.0\n249320127 Lymphs(cells/mm3)\n249320489 Lymphs(cells/mm3)\n249320127 CD4+(cells/mm3)\n249320489 CD4+(cells/mm3)";
    private static final String SVG_PARTICIPANTGROUP_YET_MORE = "Created with Rapha\u00ebl 2.1.0\nNew Chart Title: Yet More Participants\n0\n50\n100\n150\n200\n250\n300\n350\nDays Since Start Date\nCD4+ (cells/mm3), Lymphs (cells/mm3)\n200.0\n400.0\n600.0\n800.0\n1000.0\n1200.0\n1400.0\n1600.0\n1800.0\n2000.0\n2200.0\n249320489 Lymphs(cells/mm3)\n249320897 Lymphs(cells/mm3)\n249325717 Lymphs(cells/mm3)\n249320489 CD4+(cells/mm3)\n249320897 CD4+(cells/mm3)\n249325717 CD4+(cells/mm3)";
    private static final String SVG_PARTICIPANTGROUP_1 = "Created with Rapha\u00ebl 2.1.0\nNew Chart Title: Group 1: Accute HIV-1\n0\n50\n100\n150\n200\n250\n300\n350\nDays Since Start Date\nCD4+ (cells/mm3), Lymphs (cells/mm3)\n200.0\n400.0\n600.0\n800.0\n1000.0\n1200.0\n1400.0\n1600.0\n1800.0\n2000.0\n2200.0\n249318596 Lymphs(cells/mm3)\n249320107 Lymphs(cells/mm3)\n249320489 Lymphs(cells/mm3)\n249318596 CD4+(cells/mm3)\n249320107 CD4+(cells/mm3)\n249320489 CD4+(cells/mm3)";
    private static final String SVG_PARTICIPANTGROUP_2 = "Created with Rapha\u00ebl 2.1.0\nNew Chart Title: Group 2: HIV-1 Negative\n0\n50\n100\n150\n200\n250\n300\n350\nDays Since Start Date\nCD4+ (cells/mm3), Lymphs (cells/mm3)\n200.0\n400.0\n600.0\n800.0\n1000.0\n1200.0\n1400.0\n1600.0\n1800.0\n2000.0\n2200.0\n249320127 Lymphs(cells/mm3)\n249320897 Lymphs(cells/mm3)\n249325717 Lymphs(cells/mm3)\n249320127 CD4+(cells/mm3)\n249320897 CD4+(cells/mm3)\n249325717 CD4+(cells/mm3)";

    @LogMethod public void participantGroupTimeChartTest()
    {
        log("Test charting with participant groups");

        clickFolder(getFolderName());
        goToManageViews();
        waitAndClick(Locator.tagWithText("div", REPORT_NAME_3));
        clickAndWait(Locator.linkWithText("edit"));
        waitForText(CHART_TITLE);

        // kbl : TODO, filter panel behavior has changed and it's still not certain what the proper AND / OR behavior for categories is
        // until the final details are worked out, just ignore the number of occurances of text, and fix them later
        //
        assertTextPresent("Days Since Start Date");//, 2); // X-Axis labels for each measure
        assertTextPresent(CHART_TITLE+": Lymphs (cells/mm3)");//, 1); // Title
        assertTextPresent(CHART_TITLE+": CD4+ (cells/mm3)");//, 1); // Title

        goToGroupingTab();
        setParticipantSelection(PARTICIPANTS_GROUPS);
        setNumberOfCharts(PER_GROUP);
        _extHelper.uncheckCheckbox("Show Mean"); // select show mean
        _extHelper.checkCheckbox("Show Individual Lines"); // de-select show individual lines

        applyChanges();
        waitForText(GROUP1_NAME);
        assertElementPresent(Locator.linkWithText("Manage Groups"));
        _ext4Helper.checkGridRowCheckbox(GROUP3_NAME);

        log("Verify one line per measure per participant. All groups.");
        waitForCharts(5);
        assertSVG(SVG_PARTICIPANTGROUP_1, 0);
        assertSVG(SVG_PARTICIPANTGROUP_2, 1);
        assertSVG(SVG_PARTICIPANTGROUP_SOME, 2);
        assertSVG(SVG_PARTICIPANTGROUP_OTHER, 3);
        assertSVG(SVG_PARTICIPANTGROUP_YET_MORE, 4);

        log("Verify one line per measure per participant. 2/3 groups.");
        // uncheck group 2 (leaving group 1 and 3 checked)
        _ext4Helper.uncheckGridRowCheckbox(GROUP2_NAME);
        waitForCharts(4);
        assertSVG(SVG_PARTICIPANTGROUP_1, 0);
        assertSVG(SVG_PARTICIPANTGROUP_2, 1);
        assertSVG(SVG_PARTICIPANTGROUP_SOME, 2);
        assertSVG(SVG_PARTICIPANTGROUP_YET_MORE, 3);

        openSaveMenu();
        saveReport(false);

        log("Verify report after modifying participant groups.");
        clickFolder(getFolderName());
        modifyParticipantGroups();
        clickFolder(getFolderName());
        goToManageViews();
        waitAndClick(Locator.tagWithText("div", REPORT_NAME_3));
        clickAndWait(Locator.linkWithText("edit"));
        waitForText("One or more of the participant groups originally saved with this chart are not currently visible", WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent(GROUP3_NAME);

        waitForText(CHART_TITLE);
        assertTextPresent(CHART_TITLE);//, 1); // One chart per group.

        _ext4Helper.clickParticipantFilterCategory("Cohorts");
        waitForCharts(1);
        assertSVG(SVG_PARTICIPANTGROUP_SOME_MODIFIED);

        log("Verify one line per measure per participant.");
        // re-select group 2
        _ext4Helper.checkGridRowCheckbox(GROUP2_NAME);
        waitForCharts(2);
        assertElementPresent(Locator.css("svg").containing(GROUP1_NAME));
        assertElementPresent(Locator.css("svg").containing(GROUP2_NAME));

        // uncheck group 1
        _ext4Helper.clickParticipantFilterCategory(GROUP1_NAME);
        waitForCharts(1);
        assertElementNotPresent(Locator.css("svg").containing(GROUP1_NAME));
        assertElementPresent(Locator.css("svg").containing(GROUP2_NAME));

        // reselect cohorts
        _ext4Helper.clickParticipantFilterCategory("Cohorts");
        waitForCharts(3);
        assertElementPresent(Locator.css("svg").containing(GROUP2_NAME));
        assertElementPresent(Locator.css("svg").containing("Group 1: Accute HIV-1"));
        assertElementPresent(Locator.css("svg").containing("Group 2: HIV-1 Negative"));

        openSaveMenu();
        saveReport(false);

        //Now impersonate another user, make sure only 2 groups show up and warning was given.
        pushLocation();
        impersonate(USER1);
        popLocation(); // Saved Chart with groups.

        waitForText(CHART_TITLE);
        waitForText("Group 1: Accute HIV-1");
        _ext4Helper.uncheckGridRowCheckbox("Group 1: Accute HIV-1");
        _ext4Helper.uncheckGridRowCheckbox("Group 2: HIV-1 Negative");

        waitForText("Please select at least one group");
        assertElementNotPresent(Locator.css("svg"));
        waitForText("One or more of the participant groups originally saved with this chart are not currently visible.", WAIT_FOR_JAVASCRIPT);
        assertTextPresent(GROUP1_NAME);
        assertTextNotPresent(GROUP2_NAME, GROUP3_NAME);
        stopImpersonating();
    }

    private static final String SVG_MULTI_MANUAL_1 = "Created with Rapha\u00ebl 2.1.0\nNew Chart Title: Other Participants\n0\n50\n100\n150\n200\n250\nDays Since Start Date\nCD4+ (cells/mm3), Lymphs (cells/mm3)\n200.0\n400.0\n600.0\n800.0\n1000.0\n1200.0\n1400.0\n1600.0\n1800.0\n2000.0\n2200.0\nHemogoblins\n12.5\n13\n13.5\n14\n14.5\n15\n15.5\n16\n249320127 Hemoglobin\n249320489 Hemoglobin\n249320127 Lymphs(cells/mm3)\n249320489 Lymphs(cells/mm3)\n249320127 CD4+(cells/mm3)\n249320489 CD4+(cells/mm3)";
    private static final String SVG_MULTI_MANUAL_2 = "Created with Rapha\u00ebl 2.1.0\nNew Chart Title: Other Participants\n0\n50\n100\n150\n200\n250\nDays Since Start Date\nCD4+ (cells/mm3), Lymphs (cells/mm3)\n200.0\n400.0\n600.0\n800.0\n1000.0\n1200.0\n1400.0\n1600.0\n1800.0\n2000.0\n2200.0\nHemogoblins\n20\n249320127 Hemoglobin\n249320489 Hemoglobin\n249320127 Lymphs(cells/mm3)\n249320489 Lymphs(cells/mm3)\n249320127 CD4+(cells/mm3)\n249320489 CD4+(cells/mm3)";
    @LogMethod public void multiAxisTimeChartTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        waitAndClick(Locator.tagWithText("div", REPORT_NAME_3));
        clickAndWait(Locator.linkWithText("edit"));
        waitForText(CHART_TITLE);

        waitForText("Group 1: Accute HIV-1");
        _ext4Helper.uncheckGridRowCheckbox("Group 1: Accute HIV-1");
        _ext4Helper.uncheckGridRowCheckbox("Group 2: HIV-1 Negative");
        waitForCharts(1);

        assertTextPresent("Days Since Start Date", 1); // X-Axis label for one selected group.
        enterMeasuresPanel();
        addMeasure();
        _ext4Helper.clickGridRowText("Hemoglobin", 0);
        clickButton("Select", 0);
        waitForText("Hemoglobin from Lab Results");
        applyChanges();
        waitForElement(Locator.css("svg").withText(GROUP2_PTIDS[0]+" Hemoglobin"), WAIT_FOR_JAVASCRIPT, false);

        enterMeasuresPanel();
        _ext4Helper.selectComboBoxItem("Draw y-axis on:", "Right");
        applyChanges();
        waitForElement(Locator.css("svg").withText(GROUP2_PTIDS[0]+" Hemoglobin"), WAIT_FOR_JAVASCRIPT, false);

        goToAxisTab("Hemoglobin");
        setAxisValue(Axis.RIGHT, "rightaxis_range_manual", "12", "16", "Hemogoblins", null, null, null, null);
        assertSVG(SVG_MULTI_MANUAL_1, 0);

        goToAxisTab("Hemogoblins");
        setAxisValue(Axis.RIGHT, "rightaxis_range_automatic", null, null, null, "rightaxis_scale", "Log", null, null);
        assertSVG(SVG_MULTI_MANUAL_2, 0);

        openSaveMenu();
        saveReport(false);
    }

    //depends on:  participantGroupTimeChartTest
    @LogMethod public void aggregateTimeChartUITest()
    {
        goToNewTimeChart();

        //choose measure
        _ext4Helper.clickGridRowText("CD4+ (cells/mm3)", 0);
        clickButton("Select", 0);
        waitForElement(Locator.css("svg").withText("Days Since Start Date"), WAIT_FOR_JAVASCRIPT, false);

        //set to aggregate
        goToGroupingTab();
        setParticipantSelection(PARTICIPANTS_GROUPS);
        setNumberOfCharts(PER_GROUP);
        _extHelper.uncheckCheckbox("Show Mean"); // select show mean
        _extHelper.checkCheckbox("Show Individual Lines"); // de-select show individual lines
        applyChanges();

        waitForText("Lab Results: " + GROUP1_NAME);
        waitForCharts(4);

        goToGroupingTab();
        _extHelper.checkCheckbox("Show Mean"); // select show mean
        _extHelper.uncheckCheckbox("Show Individual Lines"); // de-select show individual lines
        applyChanges();
        waitForCharts(4);

        // uncheck all groups
        _ext4Helper.uncheckGridRowCheckbox(GROUP1_NAME);
        _ext4Helper.uncheckGridRowCheckbox(GROUP2_NAME);
        _ext4Helper.uncheckGridRowCheckbox("Group 1: Accute HIV-1");
        _ext4Helper.uncheckGridRowCheckbox("Group 2: HIV-1 Negative");

        waitForElement(Locator.tag("td").withText("Please select at least one group from the filter panel on the right."));
        sleep(5500);// wait for tool tip to disappear, as it is covering what we want to click. it has a timeout of 5 sec

        // re-select group 1 and 2
        _ext4Helper.checkGridRowCheckbox(GROUP1_NAME);
        _ext4Helper.checkGridRowCheckbox(GROUP2_NAME);
        waitForCharts(2);

        // Count data points in charts by checking the title attribute of the points ('*' here due to xpath namespace limitations in svgs)
        int elCount = getElementCount(Locator.tag("div").append("//*[name()='svg']/*[name()='a']").withAttributeContaining("*", GROUP1_NAME + ",\n Days"));
        Assert.assertTrue(elCount == 10 || elCount == 20); // 10 in chart and 10 in thumbnail (chrome seems to count the thumbnail, but firefox does not)
        assertElementPresent(Locator.tag("div").append("//*[name()='svg']/*[name()='a']").withAttributeContaining("*", GROUP2_NAME + ",\n Days"), 12);

        goToGroupingTab();
        _extHelper.uncheckCheckbox("Show Mean"); // select show mean
        _extHelper.checkCheckbox("Show Individual Lines"); // de-select show individual lines
        setNumberOfCharts(ONE_CHART_PER_MEASURE);
        applyChanges();
        waitForCharts(1);
        waitForText("Lab Results: CD4");

        openSaveMenu();
        setFormElement(Locator.name("reportName"), "Aggregate");
        setFormElement(Locator.name("reportDescription"), REPORT_DESCRIPTION);
        saveReport(true);
        _ext4Helper.uncheckGridRowCheckbox("Group 1: Accute HIV-1"); // TODO : Remove workaround for bad chart loading
        _ext4Helper.uncheckGridRowCheckbox("Group 2: HIV-1 Negative");
        waitForCharts(1);
        waitForText("Lab Results: CD4");
    }

    /**
     * regression for 15246 : Filtering on a column in the grid before creating time chart causes error
     */
    @LogMethod public void filteredTimeChartRegressionTest()
    {
        log("Test time chart from a filtered grid");

        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Physical Exam"));

        String ptid = "249318596";
        setFacetedFilter("Dataset", "ParticipantId", ptid);
        assertTextPresent(ptid);

        _extHelper.clickMenuButton("Charts", "Create Time Chart");
        clickChooseInitialMeasure();
        waitForText("Physical Exam", WAIT_FOR_JAVASCRIPT);

        _ext4Helper.clickGridRowText("Pulse", 0);
        clickButton("Select", 0);
        waitForText("Days Since Start Date", WAIT_FOR_JAVASCRIPT);

        openSaveMenu();
        setFormElement(Locator.name("reportName"), "Filtered Time Chart");
        setFormElement(Locator.name("reportDescription"), REPORT_DESCRIPTION);
        saveReport(true);
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsers(afterTest, USER1, USER2);
        super.doCleanup(afterTest);
    }
}
