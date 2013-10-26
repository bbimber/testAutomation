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

import org.jetbrains.annotations.Nullable;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;

@Category({DailyA.class, Assays.class})
public class AffymetrixAssayTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "AffymetrixAssayVerifyProject";
    private static final String PIPELINE_ROOT = "/sampledata/Affymetrix";
    private static final String ASSAY_NAME = "Affy Test Assay";
    private static final String EXTRA_RUN_DATA_FIELD_NAME = "ExtraField";
    private static final String EXTRA_RUN_DATA_FIELD_LABEL = "Extra Field";
    private static final String SAMPLE_SET_NAME = "AffyTestSampleSet";
    private static final String EXCEL_FILE_NAME = "test_affymetrix_run.xlsx";
    public static final String CEL_FILE_NAME = "sample_file_1.CEL";

    @Nullable
    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/microarray";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        doCreateSteps();
        doVerifySteps();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        log("Create Project");

        _containerHelper.createProject(getProjectName(), "Assay");

        log("Setup the pipeline");
        setPipelineRoot(getLabKeyRoot() + PIPELINE_ROOT);
        assertTextPresent("The pipeline root was set to");
        enableModule("Microarray", true);

        log("Create Affymetrix Assay");
        goToManageAssays();
        clickButton("New Assay Design");
        checkCheckbox(new Locator.XPathLocator("//input[@id='providerName_Affymetrix']"));
        clickButton("Next");
        Locator nameLocator = Locator.xpath("//input[@id='AssayDesignerName']");
        waitForElement(nameLocator);
        setFormElement(nameLocator, ASSAY_NAME);
        _listHelper.addField("Data Fields", 3, EXTRA_RUN_DATA_FIELD_NAME, EXTRA_RUN_DATA_FIELD_LABEL, ListHelper.ListColumnType.Integer);
        clickButton("Save & Close");

        PortalHelper portalHelper = new PortalHelper(this);

        log("Create Sample Set");
        String sampleData = "hyb_name\n";

        for (int i = 1; i <= 96; i++)
        {
            sampleData += "Sample" + i + "\n";
        }

        goToProjectHome();
        portalHelper.addWebPart("Sample Sets");
        clickButton("Import Sample Set");
        setFormElement(Locator.name("name"), SAMPLE_SET_NAME);
        setFormElement(Locator.name("data"), sampleData);
        clickButton("Submit");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void doVerifySteps()
    {
        importRun();
        verifyResults();
    }

    @LogMethod
    private void importRun()
    {
        goToModule("Pipeline");
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile(EXCEL_FILE_NAME, "Use " + ASSAY_NAME);
        clickButton("Save and Finish");
    }

    @LogMethod
    private void verifyResults()
    {
        assertTextPresent(EXCEL_FILE_NAME);
        click(Locator.linkContainingText(EXCEL_FILE_NAME));
        waitForText(EXTRA_RUN_DATA_FIELD_LABEL);
        pushLocation();
        click(Locator.linkContainingText("Sample1"));
        waitForElement(Locator.linkWithText(EXCEL_FILE_NAME));
        popLocation();
        click(Locator.linkContainingText(CEL_FILE_NAME));
        waitForElement(Locator.linkWithText(EXCEL_FILE_NAME));
    }
}
