/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PasswordUtil;
import com.thoughtworks.selenium.SeleniumException;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Apr 6, 2009
 * Time: 4:39:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProgrammaticQCTest extends AbstractAssayTest
{
    protected final static String TEST_PROGRAMMATIC_QC_PRJ = "Programmatic QC Test";
    protected final static String TEST_ASSAY = "QC Assay";

    private final ListHelper.ListColumn _listCol1 = new ListHelper.ListColumn("Date", "Date", ListHelper.ListColumnType.DateTime, "date");
    private final ListHelper.ListColumn _listCol2 = new ListHelper.ListColumn("Container", "Container", ListHelper.ListColumnType.String, "container path");
    private final ListHelper.ListColumn _listCol3 = new ListHelper.ListColumn("AssayId", "AssayId", ListHelper.ListColumnType.String, "assay id");
    private final ListHelper.ListColumn _listCol4 = new ListHelper.ListColumn("AssayName", "AssayName", ListHelper.ListColumnType.String, "assay name");
    private final ListHelper.ListColumn _listCol5 = new ListHelper.ListColumn("User", "User", ListHelper.ListColumnType.String, "user");
    private final ListHelper.ListColumn _listCol6 = new ListHelper.ListColumn("Comments", "Comments", ListHelper.ListColumnType.String, "run comments");

    protected static final String TEST_ASSAY_DATA_PROP_NAME = "testAssayDataProp";
    public static final int TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT = 4;
    protected static final String[] TEST_ASSAY_DATA_PROP_TYPES = { "Boolean", "Integer", "DateTime" };

    protected static final String TEST_RUN1_DATA1 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "20\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "s1\ta\t1\ttrue\t20\t2000-01-01\n" +
            "s2\tb\t2\ttrue\t19\t2000-02-02\n" +
            "s3\tb\t3\ttrue\t18\t2000-03-03\n" +
            "s4\td\t4\tfalse\t17\t2000-04-04\n" +
            "s5\te\t5\tfalse\t16\t2000-05-05\n" +
            "s5\te\t5\tfalse\t16\t2000-05-05\n" +
            "s6\tf\t6\tfalse\t15\t2000-06-06";
    protected static final String TEST_RUN1_DATA2 = "specimenID\tparticipantID\tvisitID\t" + TEST_ASSAY_DATA_PROP_NAME + "4\t" + TEST_ASSAY_DATA_PROP_NAME + "5\t" + TEST_ASSAY_DATA_PROP_NAME + "6\n" +
            "s1\ta\t1\ttrue\t20\t2000-01-01\n" +
            "s2\tb\t2\ttrue\t19\t2000-02-02\n" +
            "s3\tc\t3\ttrue\t18\t2000-03-03\n" +
            "s4\td\t4\tfalse\t17\t2000-04-04\n" +
            "s5\te\t5\tfalse\t16\t2000-05-05\n" +
            "s6\tf\t6\tfalse\t15\t2000-06-06";

    protected void doTestSteps() throws Exception
    {
        prepare();

        createProject(TEST_PROGRAMMATIC_QC_PRJ);
        setupPipeline(TEST_PROGRAMMATIC_QC_PRJ);

        defineAssay();
        uploadRuns();
    }

    protected void doCleanup() throws Exception
    {
        try {
            deleteEngine();
            deleteProject(TEST_PROGRAMMATIC_QC_PRJ);
        }
        catch (Throwable t) {}
    }

    protected void prepare()
    {
        ensureAdminMode();

        clickLinkWithText("Admin Console");
        clickLinkWithText("views and scripting");
        log("setup a java engine");

        if (!isEngineConfigured())
        {
            // add a new r engine configuration
            String id = ExtHelper.getExtElementId(this, "btn_addEngine");
            click(Locator.id(id));

            id = ExtHelper.getExtElementId(this, "add_externalEngine");
            click(Locator.id(id));

            id = ExtHelper.getExtElementId(this, "btn_submit");
            waitForElement(Locator.id(id), 10000);

            id = ExtHelper.getExtElementId(this, "editEngine_exePath");

            String javaHome = System.getProperty("java.home");
            File javaExe = new File(javaHome + "/bin/java.exe");
            if (!javaExe.exists())
            {
                javaExe = new File(javaHome + "/bin/java");
                if (!javaExe.exists())
                    fail("unable to setup the java engine");
            }
            setFormElement(Locator.id(id), javaExe.getAbsolutePath());

            id = ExtHelper.getExtElementId(this, "editEngine_name");
            setFormElement(Locator.id(id), "Java");

            id = ExtHelper.getExtElementId(this, "editEngine_languageName");
            setFormElement(Locator.id(id), "java");

            id = ExtHelper.getExtElementId(this, "editEngine_extensions");
            setFormElement(Locator.id(id), "jar");

            id = ExtHelper.getExtElementId(this, "editEngine_exeCommand");
            setFormElement(Locator.id(id), "-jar ${scriptFile} \"${runInfo}\" \"" + PasswordUtil.getUsername() + "\" \"" + PasswordUtil.getPassword() + "\" \"" + WebTestHelper.getBaseURL() + "\"");

            id = ExtHelper.getExtElementId(this, "btn_submit");
            click(Locator.id(id));

            // wait until the dialog has been dismissed
            int cnt = 3;
            while (isElementPresent(Locator.id(id)) && cnt > 0)
            {
                sleep(1000);
                cnt--;
            }

            if (!isEngineConfigured())
                fail("unable to setup the java engine");
        }

        // ensure the .netrc file exists
        try {
            File netrcFile = new File(System.getProperty("user.home") + "/" + "_netrc");

            if (!netrcFile.exists())
                netrcFile = new File(System.getProperty("user.home") + "/" + ".netrc");

            if (!netrcFile.exists())
            {
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(netrcFile)));
                try {
                    pw.append("machine localhost:8080");
                    pw.append('\n');
                    pw.append("login " + PasswordUtil.getUsername());
                    pw.append('\n');
                    pw.append("password " + PasswordUtil.getPassword());
                    pw.append('\n');
                }
                finally
                {
                    pw.close();
                }
            }
        }
        catch (IOException ioe)
        {
            log("failed trying to create a .netrc file " + ioe.getMessage());
        }
    }

    protected boolean isEngineConfigured()
    {
        // need to allow time for the server to return the engine list and the ext grid to render
        Locator engine = Locator.xpath("//div[@id='enginesGrid']//td//div[.='jar']");
        int time = 0;
        while (!isElementPresent(engine) && time < 5000)
        {
            sleep(100);
            time += 100;
        }
        return isElementPresent(engine);
    }

    protected void deleteEngine()
    {
        ensureAdminMode();

        clickLinkWithText("Admin Console");
        clickLinkWithText("views and scripting");

        if (isEngineConfigured())
        {
            Locator engine = Locator.xpath("//div[@id='enginesGrid']//td//div[.='jar']");
            selenium.mouseDown(engine.toString());

            String id = ExtHelper.getExtElementId(this, "btn_deleteEngine");
            click(Locator.id(id));

            ExtHelper.waitForExtDialog(this, 5000);

            String btnId = selenium.getEval("this.browserbot.getCurrentWindow().Ext.MessageBox.getDialog().buttons[1].getId();");
            click(Locator.id(btnId));
        }
    }

    private void defineAssay()
    {
        log("Defining a test assay at the project level");

        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        addWebPart("Assay List");

        clickLinkWithText("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "General");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_GWT);

        selenium.type("//input[@id='AssayDesignerName']", TEST_ASSAY);

        File qcScript = new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/validator.jar");
        if (qcScript.exists())
            selenium.type("//input[@id='AssayDesignerQCScript']", qcScript.getAbsolutePath());
        else
            fail("unable to locate the QC script");

        for (int i = TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT; i < TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT + TEST_ASSAY_DATA_PROP_TYPES.length; i++)
        {
            addField("Data Fields", i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_NAME + i, TEST_ASSAY_DATA_PROP_TYPES[i - TEST_ASSAY_DATA_PREDEFINED_PROP_COUNT]);
        }
        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

        // create the list for the qc log
        ListHelper.createList(this, TEST_PROGRAMMATIC_QC_PRJ, "QC Log", ListHelper.ListColumnType.AutoInteger, "Key", _listCol1, _listCol2,
                _listCol3, _listCol4, _listCol5, _listCol6);
    }

    private void uploadRuns()
    {
        log("uploading runs");
        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        clickLinkWithText("Assay List");
        clickLinkWithText(TEST_ASSAY);

        clickNavButton("Import Data");
        clickNavButton("Next");

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA1);
        clickNavButton("Save and Finish");

        assertTextPresent("A duplicate PTID was discovered : b");
        assertTextPresent("A duplicate PTID was discovered : e");

        selenium.click("//input[@value='textAreaDataProvider']");
        selenium.type("TextAreaDataCollector.textArea", TEST_RUN1_DATA2);
        clickNavButton("Save and Finish");

        // verify the log entry
        clickLinkWithText(TEST_PROGRAMMATIC_QC_PRJ);
        clickLinkWithText("QC Log");

        assertTextPresent("Programmatic QC was run and 2 errors were found");
        assertTextPresent("Programmatic QC was run and 0 errors were found");
    }

    public String getAssociatedModuleDirectory()
    {
        return "query";
    }
}
