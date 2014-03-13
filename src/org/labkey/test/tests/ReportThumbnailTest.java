/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;

import java.io.File;

import static org.junit.Assert.*;

@Category({DailyB.class})
public class ReportThumbnailTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "ReportThumbnailTest";
    private static final String PROJECT_NAME_ROUNDTRIP = "ReportThumbnailTest_Roundtrip";
    private static final File TEST_STUDY = new File(getSampledataPath(), "study/LabkeyDemoStudyWithCharts.folder.zip");
    private static final File TEST_THUMBNAIL = new File(getSampledataPath(), "Microarray/test1.jpg");
    private static final File TEST_ICON = new File(getSampledataPath(), "icemr/piggy.JPG");
    private static final String BOX_PLOT = "Example Box Plot";
    private static final String SCATTER_PLOT = "Example Scatter Plot";
    private static final String R_PARTICIPANT_VIEWS = "R Participant Views: Physical Exam";
    private static final String R_REGRESSION_BP_ALL = "R Regression: Blood Pressure: All";
    private static final String R_REGRESSION_BP_MEANS = "R Regression: Blood Pressure: Means";

    private String THUMBNAIL_DATA;
    private String ICON_DATA;

    private final RReportHelper _rReportHelper = new RReportHelper(this);

    //
    // expected values for roundtrip test
    //
    private String ICON_CUSTOM_DATA; // custom value same across all views
    private String ICON_PLOT_NONE_DATA; // stock icons differ per report type
    private String ICON_R_NONE_DATA;

    private String THUMBNAIL_R_AUTO_DATA; // autogenerated thumbnails specific to the report being viewed
    private String THUMBNAIL_CUSTOM_DATA; // custom thumbnail same across all views
    private String THUMBNAIL_R_NONE_DATA; // stock thumbnails differ per report type
    private String _currentProject = PROJECT_NAME;

    @Override
    protected String getProjectName()
    {
        return _currentProject;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(PROJECT_NAME, afterTest);
        // besides the default project, the test also creates a project to verify
        // roundtrip (import/export) of thumbs
        deleteProject(PROJECT_NAME_ROUNDTRIP, afterTest);
    }


    @Override
    protected void doTestSteps() throws Exception
    {
        doSetup();
        doVerifySteps();
    }

    protected void doVerifySteps() throws Exception
    {
        testGenericChartThumbnails();
        testCustomIcon();
        testRThumbnails();
        testThumbnailRoundtrip();
    }

    private void testGenericChartThumbnails() throws Exception
    {
        goToDataViews();
        setThumbnailSRC(BOX_PLOT);
        toggleThumbnailType(BOX_PLOT, true);
        assertNewThumbnail(BOX_PLOT);
        assignCustomThumbnail(BOX_PLOT, TEST_THUMBNAIL);
        assertNewThumbnail(BOX_PLOT);

        goToDataViews();
        setThumbnailSRC(SCATTER_PLOT);
        toggleThumbnailType(SCATTER_PLOT, true);
        assertNewThumbnail(SCATTER_PLOT);
        assignCustomThumbnail(SCATTER_PLOT, TEST_THUMBNAIL);
        assertNewThumbnail(SCATTER_PLOT);
        THUMBNAIL_CUSTOM_DATA = THUMBNAIL_DATA;
    }

    private void testRThumbnails() throws Exception
    {
        goToDataViews();
        setThumbnailSRC(R_REGRESSION_BP_ALL);
        THUMBNAIL_R_NONE_DATA = THUMBNAIL_DATA;

        goToDataViews();
        generateRThumbnail(R_PARTICIPANT_VIEWS);

        goToDataViews();
        setThumbnailSRC(R_PARTICIPANT_VIEWS);
        THUMBNAIL_R_AUTO_DATA = THUMBNAIL_DATA;

        goToDataViews();
        assignCustomThumbnail(R_REGRESSION_BP_MEANS, TEST_THUMBNAIL);
        verifyThumbnail(R_REGRESSION_BP_MEANS, THUMBNAIL_CUSTOM_DATA);

        // setup icons
        goToDataViews();
        setIconSRC(R_PARTICIPANT_VIEWS);
        ICON_R_NONE_DATA = ICON_DATA;

        goToDataViews();
        assignCustomIcon(R_REGRESSION_BP_ALL, TEST_ICON);
        verifyIcon(R_REGRESSION_BP_ALL, ICON_CUSTOM_DATA);

        goToDataViews();
        assignCustomIcon(R_REGRESSION_BP_MEANS, TEST_ICON);
        verifyIcon(R_REGRESSION_BP_MEANS, ICON_CUSTOM_DATA);
    }

    private void testCustomIcon() throws Exception
    {
        goToDataViews();
        setIconSRC(BOX_PLOT);
        assignCustomIcon(BOX_PLOT, TEST_ICON);
        assertNewIcon(BOX_PLOT);
        ICON_CUSTOM_DATA = ICON_DATA;

        goToDataViews();
        setIconSRC(SCATTER_PLOT);
        ICON_PLOT_NONE_DATA = ICON_DATA;
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doSetup()
    {
        _rReportHelper.ensureRConfig();
        _containerHelper.createProject(PROJECT_NAME, "Study");
        importStudyFromZip(TEST_STUDY);
        goToDataViews();
    }

    protected void goToDataViews()
    {
        clickFolder(_currentProject);
        waitAndClickAndWait(Locator.linkWithText("Clinical and Assay Data"));
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT); // Lots of stuff on this page. Can take a while to load.
    }

    @LogMethod
    protected void toggleThumbnailType(String chart, boolean custom)
    {
        clickAndWait(Locator.linkWithText(chart));
        waitForElement(Locator.css("svg"));
        clickButton("Edit");
        waitForElement(Locator.css("svg"));
        waitForTextToDisappear("loading data...");
        clickButton("Save", 0);
        if(!custom)
        {
            waitAndClick(Locator.xpath("//input[@type='button' and ../label[text()='None']]"));
        }
        else
        {
            waitAndClick(Locator.xpath("//input[@type='button' and ../label[text()='Auto-generate']]"));
        }

        _ext4Helper.clickWindowButton("Save", "Save", 0, 0);
        // Timing is tight, don't step through this bit.
        _extHelper.waitForExtDialog("Saved");
    }

    @LogMethod
    protected void generateRThumbnail(String report)
    {
        waitForElement(Locator.linkWithText(report));
        clickAndWait(Locator.linkWithText(report));
        _rReportHelper.clickSourceTab();
        prepForPageLoad();
        _rReportHelper.saveReport(null);
        newWaitForPageToLoad();
    }


    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void setThumbnailSRC(String chart)
    {
        waitForElement(Locator.linkWithText(chart));
        mouseOver(Locator.linkWithText(chart));
        Locator.XPathLocator thumbnail = Locator.xpath("//div[@class='thumbnail']/img").notHidden();
        waitForElement(thumbnail);
        try
        {
            THUMBNAIL_DATA = WebTestHelper.getHttpGetResponseBody(getAttribute(thumbnail, "src"));
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void assertNewThumbnail(String chart)
    {
        verifyThumbnail(chart, null);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyThumbnail(String chart, String expected)
    {
        goToDataViews();
        waitForElement(Locator.xpath("//a[text()='"+chart+"']"));
        mouseOver(Locator.xpath("//a[text()='"+chart+"']"));
        Locator.XPathLocator thumbnail = Locator.xpath("//div[@class='thumbnail']/img").notHidden();
        waitForElement(thumbnail);
        String thumbnailData;
        try
        {
            thumbnailData = WebTestHelper.getHttpGetResponseBody(getAttribute(thumbnail, "src"));
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }

        if (null == expected)
            assertFalse("Thumbnail was was still default", THUMBNAIL_DATA.equals(thumbnailData));
        else
            assertTrue("Thumbnail wasn't persisted correctly", expected.equals(thumbnailData));

        THUMBNAIL_DATA = thumbnailData;
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void assignCustomThumbnail(String chart, File thumbnail)
    {
        goToDataViews();
        waitAndClick(Locator.xpath("//img[@title='Edit']"));
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.name("viewName"));
        _ext4Helper.clickExt4Tab("Images");
        waitForElement(Locator.id("customThumbnail"));
        setFormElement(Locator.xpath("//input[@id='customThumbnail-button-fileInputEl']"), thumbnail);
        _ext4Helper.clickWindowButton(chart, "Save", 0, 0);
        waitForTextToDisappear("Saving...");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void setIconSRC(String chart)
    {
        waitForElement(Locator.linkWithText(chart));
        waitAndClick(Locator.xpath("//img[@title='Edit']"));
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.name("viewName"));
        _ext4Helper.clickExt4Tab("Images");
        Locator iconLocator = Locator.xpath("//div[@class=\"icon\"]/img").notHidden();
        waitForElement(iconLocator);
        try
        {
            ICON_DATA = WebTestHelper.getHttpGetResponseBody(getAttribute(iconLocator, "src"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void assertNewIcon(String chart)
    {
        verifyIcon(chart, null);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyIcon(String chart, String expected)
    {
        goToDataViews();
        waitForElement(Locator.xpath("//a[text()='"+chart+"']"));
        waitAndClick(Locator.xpath("//img[@title='Edit']"));
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.name("viewName"));
        _ext4Helper.clickExt4Tab("Images");
        Locator iconLocator = Locator.xpath("//div[@class=\"icon\"]/img").notHidden();
        waitForElement(iconLocator);
        String iconData;
        try
        {
            iconData = WebTestHelper.getHttpGetResponseBody(getAttribute(iconLocator, "src"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        if (null == expected)
            assertFalse("Icon was still default", ICON_DATA.equals(iconData));
        else
            assertTrue("Unexpected icon", expected.equals(iconData));

        ICON_DATA = iconData;
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void assignCustomIcon(String chart, File icon)
    {
        goToDataViews();
        waitAndClick(Locator.xpath("//img[@title='Edit']"));
        DataViewsTest.clickCustomizeView(chart, this);
        waitForElement(Locator.name("viewName"));
        _ext4Helper.clickExt4Tab("Images");
        waitForElement(Locator.id("customIcon"));
        setFormElement(Locator.xpath("//input[@id='customIcon-button-fileInputEl']"), icon);
        _ext4Helper.clickWindowButton(chart, "Save", 0, 0);
        waitForTextToDisappear("Saving...");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected File getExportFolderZip()
    {
        goToProjectHome();
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Export"));
        clickButton("Export", 0);

        waitFor(new BaseWebDriverTest.Checker() {
            @Override
            public boolean check()
            {
                File[] exportFiles = getDownloadDir().listFiles();
                return (exportFiles != null && exportFiles.length > 0);
            }
        }, "failed to export study", WAIT_FOR_JAVASCRIPT);

        File[] exportFiles = getDownloadDir().listFiles();
        return exportFiles[0];
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void importFolder(File importZip)
    {
        _containerHelper.createProject(PROJECT_NAME_ROUNDTRIP, "Study");
        _currentProject = PROJECT_NAME_ROUNDTRIP;
        importStudyFromZip(importZip);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void testThumbnailRoundtrip()
    {
        // export
        File exportZip = getExportFolderZip();
        importFolder(exportZip);

        // BOX_PLOT has a custom icon and custom thumbnail
        verifyIcon(BOX_PLOT, ICON_CUSTOM_DATA);
        verifyThumbnail(BOX_PLOT, THUMBNAIL_CUSTOM_DATA);

        // SCATTER_PLOT has no icon and custom thumbnail
        verifyIcon(SCATTER_PLOT, ICON_PLOT_NONE_DATA);
        verifyThumbnail(SCATTER_PLOT, THUMBNAIL_CUSTOM_DATA);

        // R_PARTICIPANT_VIEWS has no icon, auto thumbnail
        verifyIcon(R_PARTICIPANT_VIEWS, ICON_R_NONE_DATA);
        verifyThumbnail(R_PARTICIPANT_VIEWS, THUMBNAIL_R_AUTO_DATA);

        // R_REGRESSION_BP_ALL has custom icon, no thumbnail
        verifyIcon(R_REGRESSION_BP_ALL, ICON_CUSTOM_DATA);
        verifyThumbnail(R_REGRESSION_BP_ALL, THUMBNAIL_R_NONE_DATA);

        // R_REGRESSION_BP_MEANS has custom icon, custom thumbnail
        verifyIcon(R_REGRESSION_BP_MEANS, ICON_CUSTOM_DATA);
        verifyThumbnail(R_REGRESSION_BP_MEANS, THUMBNAIL_CUSTOM_DATA);
    }
}
