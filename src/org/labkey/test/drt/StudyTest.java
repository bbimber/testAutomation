/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

package org.labkey.test.drt;

import com.thoughtworks.selenium.SeleniumException;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.util.ExtHelper;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User: brittp
 * Date: Mar 9, 2006
 * Time: 1:54:57 PM
 */
public class StudyTest extends BaseSeleniumWebTest
{
    protected final String VISIT_MAP = getSampleDataPath() + "v068_visit_map.txt";

    private final String CRF_SCHEMAS = getSampleDataPath() + "schema.tsv";
    private final String SPECIMEN_ARCHIVE_A = getSampleDataPath() + "sample_a.specimens";
    protected final String ARCHIVE_TEMP_DIR = getSampleDataPath() + "drt_temp";
    protected static final int MAX_WAIT_SECONDS = 4*60;

    protected String getSampleDataPath()
    {
        return "/sampledata/study/";
    }

    protected String getProjectName()
    {
        return "StudyVerifyProject";
    }

    protected String getStudyLabel()
    {
        return "Study 001";
    }

    protected String getFolderName()
    {
        return "My Study";
    }

    protected String getPipelinePath()
    {
        return getLabKeyRoot() + getSampleDataPath();
    }

    public String getAssociatedModuleDirectory()
    {
        return "study";
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doCleanup() throws Exception
    {
        try { deleteProject(getProjectName()); } catch (Throwable e) {}

        // Delete log files from specimen imports
        File tempDir = new File(getLabKeyRoot() + ARCHIVE_TEMP_DIR);
        File[] tempFiles = tempDir.listFiles();
        if (tempFiles != null)
        {
            for (File tempFile : tempFiles)
            {
                tempFile.delete();
            }
        }
    }

    protected void doCreateSteps()
    {
        createStudy();
        waitForInitialUpload();
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        afterCreateStudy();
    }

    protected void doTestSteps()
    {
        doCreateSteps();

        verifyDemographics();
        verifyVisitMapPage();
        verifyManageDatasetsPage();
        verifyHiddenVisits();
        verifyCohorts();

        // test custom assays
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");
        setFormElement("typeName", "verifyAssay");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_GWT);

        checkRadioButton("additionalKey", 1);

        clickNavButton("Import Schema", 0);
        waitForElement(Locator.xpath("//textarea[@id='schemaImportBox']"), WAIT_FOR_GWT);

        setFormElement("schemaImportBox", "Property\tLabel\tRangeURI\tNotNull\tDescription\n" +
                "SampleId\tSample Id\txsd:string\ttrue\tstring\n" +
                "DateField\tDateField\txsd:dateTime\tfalse\tThis is a date field\n" +
                "NumberField\tNumberField\txsd:double\ttrue\tThis is a number\n" +
                "TextField\tTextField\txsd:string\tfalse\tThis is a text field");

        clickNavButton("Import", 0);
        waitForElement(Locator.xpath("//input[@id='ff_label3']"), WAIT_FOR_GWT);

        click(Locator.xpath("//span[@id='button_dataField']"));
        selenium.select("list_dataField", "label=Sample Id");

        clickNavButton("Save");
        waitForElement(Locator.navButton("View Dataset Data"), WAIT_FOR_GWT);
        clickNavButton("View Dataset Data");
        clickNavButton("Import Data");

        String tsv = "participantid\tsequencenum\tvisitdate\tSampleId\tDateField\tNumberField\tTextField\treplace\n" +
                "1234\t1\t1/1/2006\t1234_A\t2/1/2006\t1.2\ttext\t\n" +
                "1234\t1\t1/1/2006\t1234_B\t2/1/2006\t1.2\ttext\t\n";
        String errorRow = "\tbadvisitd\t1/1/2006\t\ttext\t";
        setFormElement("tsv", tsv + "\n" + errorRow);
        clickNavButton("Import Data");
        assertTextPresent("Row 3 does not contain required field participantid.");
        assertTextPresent("Row 3 data type error for field SequenceNum.");
        assertTextPresent("Row 3 does not contain required field SampleId.");
        assertTextPresent("Row 3 data type error for field DateField.");
        assertTextPresent("Row 3 does not contain required field NumberField.");

        setFormElement("tsv", tsv);
        clickNavButton("Import Data", longWaitForPage);
        assertTextPresent("1234");
        assertTextPresent("2006-02-01");
        assertTextPresent("1.2");

        // configure QC state management before our second upload
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage QC States");
        setFormElement("newLabel", "unknown QC");
        setFormElement("newDescription", "Unknown data is neither clean nor dirty.");
        clickCheckboxById("dirty_public");
        clickCheckbox("newPublicData");
        clickNavButton("Save");
        selectOptionByText("defaultDirectEntryQCState", "unknown QC");
        selectOptionByText("showPrivateDataByDefault", "Public data");
        clickNavButton("Save");

        // return to dataset import page
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("verifyAssay");
        assertTextPresent("QC State");
        assertTextNotPresent("1234_B");
        clickMenuButton("QC State", "QCState:All data");
        assertTextPresent("unknown QC");
        assertTextPresent("1234_B");

        //Import same data again
        clickNavButton("Import Data");
        setFormElement("tsv", tsv);
        clickNavButton("Import Data");
        assertTextPresent("Duplicates were found");
        //Now explicitly replace
        tsv = "participantid\tsequencenum\tvisitdate\tSampleId\tDateField\tNumberField\tTextField\treplace\n" +
                "1234\t1\t1/1/2006\t1234_A\t2/1/2006\t5000\tnew text\tTRUE\n" +
                "1234\t1\t1/1/2006\t1234_B\t2/1/2006\t5000\tnew text\tTRUE\n";
        setFormElement("tsv", tsv);
        clickNavButton("Import Data");
        assertTextPresent("5000.0");
        assertTextPresent("new text");
        assertTextPresent("QC State");
        assertTextPresent("unknown QC");

        // upload specimen data and verify import
        importSpecimenArchive(new File(getLabKeyRoot(), SPECIMEN_ARCHIVE_A), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getStudyLabel(), 1);
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Blood (Whole)");
        clickMenuButton("Page Size", "Page Size:All");
        assertTextNotPresent("DRT000XX-01");
        assertTextPresent("GAA082NH-01");
        clickLinkWithText("Hide Vial Info");
        assertTextPresent("Total:");
        assertTextPresent("466");

        assertTextNotPresent("BAD");

        clickLinkWithText("Show Vial Info");
        clickLinkContainingText("history");
        // verify that we're correctly parsing frozen time, which is a date with a time portion only:
        assertTextPresent("15:30:00");
        assertTextPresent("2.0&nbsp;ML");
        assertTextNotPresent("Added Comments");
        // confirm collection location:
        assertTextPresent("KCMC, Moshi, Tanzania");
        // confirm historical locations:
        assertTextPresent("Contract Lab Services, Johannesburg, South Africa");
        assertTextPresent("Aurum Health KOSH Lab, Orkney, South Africa");

        clickLinkWithText("Specimens");
        setFilter("SpecimenDetail", "QualityControlFlag", "Equals", "true");
        setSort("SpecimenDetail", "GlobalUniqueId", SortDirection.ASC);
        assertTextPresent("AAA07XK5-02");
        assertTextPresent("Conflicts found: AdditiveTypeId, DerivativeTypeId, PrimaryTypeId");
        clickLinkContainingText("history");
        assertTextPresent("Blood (Whole)");
        assertTextPresent("Vaginal Swab");
        assertTextPresent("Vial is flagged for quality control");
        clickLinkWithText("update");
        setFormElement("qualityControlFlag", "false");
        setFormElement("comments", "Manually removed flag");
        clickNavButton("Save Changes");
        assertTextPresent("Manually removed flag");
        assertTextPresent("Conflicts found: AdditiveTypeId, DerivativeTypeId, PrimaryTypeId");
        assertTextNotPresent("Vial is flagged for quality control");
        clickLinkWithText("return to vial view");
        assertTextNotPresent("AAA07XK5-02");
        assertTextPresent("KBH00S5S-01");
    }

    private void verifyDemographics()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");
        clickLinkWithText("24");
        assertTextPresent("This is the demographics dataset, dammit");
        assertTextPresent("Male");
        assertTextPresent("African American or Black");
        clickLinkWithText("999320016");
        assertTextPresent("right deltoid");
    }

    protected void verifyVisitMapPage()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Visits");

        // test optional/required/not associated
        clickLinkWithText("edit", 0);
        selectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 1, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 2, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 3, "OPTIONAL");
        selectOption("dataSetStatus", 4, "OPTIONAL");
        selectOption("dataSetStatus", 5, "OPTIONAL");
        selectOption("dataSetStatus", 6, "REQUIRED");
        selectOption("dataSetStatus", 7, "REQUIRED");
        selectOption("dataSetStatus", 8, "REQUIRED");
        clickNavButton("Save");
        clickLinkWithText("edit", 0);
        selectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 1, "OPTIONAL");
        selectOption("dataSetStatus", 2, "REQUIRED");
        selectOption("dataSetStatus", 3, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 4, "OPTIONAL");
        selectOption("dataSetStatus", 5, "REQUIRED");
        selectOption("dataSetStatus", 6, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 7, "OPTIONAL");
        selectOption("dataSetStatus", 8, "REQUIRED");
        clickNavButton("Save");
        clickLinkWithText("edit", 0);
        assertSelectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 1, "OPTIONAL");
        assertSelectOption("dataSetStatus", 2, "REQUIRED");
        assertSelectOption("dataSetStatus", 3, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 4, "OPTIONAL");
        assertSelectOption("dataSetStatus", 5, "REQUIRED");
        assertSelectOption("dataSetStatus", 6, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 7, "OPTIONAL");
        assertSelectOption("dataSetStatus", 8, "REQUIRED");
    }

    protected void verifyManageDatasetsPage()
    {
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");

        clickLinkWithText("489");
        assertTextPresent("ESIdt");
        assertTextPresent("Form Completion Date");
        assertTableCellTextEquals("details", 5, 1, "false");     // "Demographics Data" should be false

        // Verify that "Demographics Data" is checked and description is set
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("DEM-1: Demographics");
        assertTableCellTextEquals("details", 5, 1, "true");
        assertTableCellTextEquals("details", 8, 1, "This is the demographics dataset, dammit");

        // "Demographics Data" needs to be false for the rest of the test
        clickButtonContainingText("Edit Dataset Definition");
        waitForElement(Locator.name("description"), BaseSeleniumWebTest.WAIT_FOR_GWT);        
        uncheckCheckbox("demographicData");
        clickNavButton("Save");
    }

    private void verifyHiddenVisits()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");
        assertTextNotPresent("Screening Cycle");
        assertTextNotPresent("Cycle 1");
        assertTextPresent("Pre-exist Cond");
        clickLinkWithText("Show Hidden Data");
        assertTextPresent("Screening Cycle");
        assertTextPresent("Cycle 1");
        assertTextPresent("Pre-exist Cond");
    }

    private void verifyCohorts()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");
        clickLinkWithText("24");

        // verify that cohorts are working
        assertTextPresent("999320016");
        assertTextPresent("999320518");

        clickMenuButton("Cohorts", "Cohorts:Group 1");
        waitForPageToLoad();
        assertTextPresent("999320016");
        assertTextNotPresent("999320518");

        clickMenuButton("Cohorts", "Cohorts:Group 2");
        waitForPageToLoad();
        assertTextNotPresent("999320016");
        assertTextPresent("999320518");

        // verify that the participant view respects the cohort filter:
        setSort("Dataset", "ParticipantId", SortDirection.ASC);
        clickLinkWithText("999320518");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Participant >");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Participant >");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Participant >");
    }

    protected void createStudy()
    {
        initializeFolder();

        clickNavButton("Create Study");
        click(Locator.radioButtonByNameAndValue("simpleRepository", "false"));
        clickNavButton("Create Study");

        // change study label
        clickLinkWithText("Change Label");
        setFormElement("label", getStudyLabel());
        clickNavButton("Update");
        assertTextPresent(getStudyLabel());

        // import visit map
        clickLinkWithText("Manage Visits");
        clickLinkWithText("Import Visit Map");
        String visitMapData = getFileContents(VISIT_MAP);
        setLongTextField("content", visitMapData);
        clickNavButton("Import");

        // define forms
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Define Dataset Schemas");
        clickLinkWithText("Bulk Import Schemas");
        setFormElement("typeNameColumn", "platename");
        setFormElement("labelColumn", "platelabel");
        setFormElement("typeIdColumn", "plateno");
        setLongTextField("tsv", getFileContents(CRF_SCHEMAS));
        clickNavButton("Submit", 180000);

        // setup cohorts:
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Cohorts");
        selectOptionByText("participantCohortDataSetId", "EVC-1: Enrollment Vaccination");
        waitForPageToLoad();
        selectOptionByText("participantCohortProperty", "2. Enrollment group");
        clickNavButton("Update Assignments");

        // configure QC state management so that all data is displayed by default (we'll test with hidden data later):
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage QC States");
        selectOptionByText("showPrivateDataByDefault", "All data");
        clickNavButton("Save");

        // upload data:
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Data Pipeline");
        clickNavButton("Setup");
        setFormElement("path", getPipelinePath());
        submit();
        clickLinkWithText("Pipeline");
        clickNavButton("Process and Import Data");
        waitForElement(Locator.navButton("Import datasets"),5000);
        if (isNavButtonPresent("Delete log"))
            clickNavButton("Delete log");
        generateFiles();
        if (!isNavButtonPresent("Import datasets"))
            fail("Datasets must be generated: run 'makePlates.bat' from within sampledata/study.");
        clickNavButton("Import datasets");
        clickNavButton("Submit");
    }

    protected void waitForInitialUpload()
    {
        // Unfortunately isLinkWithTextPresent also picks up the "Errors" link in the header,
        // and it picks up the upload of 'FPX-1: Final Complete Physical Exam' as containing complete.
        // we exclude the word 'REPLACE' to catch this case:
        startTimer();
        while ((!isLinkPresentWithText("COMPLETE") || isLinkPresentWithText("REPLACE")) &&
                !isLinkPresentWithText("ERROR") &&
                elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            log("Waiting for data import");
            sleep(1000);
            refresh();
        }
        assertLinkNotPresentWithText("ERROR");  // Must be surrounded by an anchor tag.
        assertLinkPresentWithTextCount("COMPLETE", 1);
    }

    // This is separate from createStudy() because new visit map format supports setting visit visibility, but old format
    // does not (so we want to set it manually).
    protected void afterCreateStudy()
    {
        // Hide visits based on label -- manual create vs. import will result in different indexes for these visits
        hideVisits("Screening Cycle", "Cycle 1");

        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("DEM-1: Demographics");
        clickButtonContainingText("Edit Dataset Definition");
        waitForElement(Locator.name("description"), BaseSeleniumWebTest.WAIT_FOR_GWT);        
        checkCheckbox("demographicData");
        setFormElement("description", "This is the demographics dataset, dammit");
        clickNavButton("Save");
    }

    protected void hideVisits(String... visitLabel)
    {
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Visits");

        Set<String> labels = new HashSet<String>(Arrays.asList(visitLabel));
        int row = 2;  // Skip header row (row index is one-based)
        String currentLabel;

        // Loop until we find all the labels or we hit the end of the table
        while (!labels.isEmpty() && null != (currentLabel = getVisitLabel(row)))
        {
            if (labels.contains(currentLabel))
            {
                clickLinkWithText("edit", row - 2);   // Zero-based, plus the header row doesn't have an edit link
                uncheckCheckbox("showByDefault");
                clickNavButton("Save");
                labels.remove(currentLabel);
            }

            row++;
        }
    }

    // row is one-based
    private String getVisitLabel(int row)
    {
        try
        {
            return selenium.getText("//table[@id='visits']/tbody/tr[" + row + "]/th");
        }
        catch (SeleniumException e)
        {
            return null;
        }
    }

    protected void initializeFolder()
    {
        if (!isLinkPresentWithText(getProjectName()))
            createProject(getProjectName());
        createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", null, true);
    }

    private void generateFiles()
    {
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(getPipelinePath() + "/v068_dump.sql"));
            String s;
            while (null != (s = in.readLine()))
            {
                if (!s.startsWith("COPY"))
                    continue;

                String copy = s;
                String head = copy.substring(s.indexOf('(') + 1);
                head = head.substring(0, head.indexOf(')'));
                String plate = s.substring(5, s.indexOf('(')).trim();

                String[] cols = head.split(",");
                String header = "";
                for (String c : cols)
                    header += "\t" + c.trim();
                header = header.trim();

                File f = new File(getPipelinePath() + "/" + plate + ".tsv");
                PrintWriter out = new PrintWriter(new FileWriter(f));
                log("Generated plate " + f.getPath());
                out.println("# " + plate);
                out.println(header);
                while (null != (s = in.readLine()) && !s.startsWith("\\."))
                    out.println(s);
                out.flush();
                out.close();
            }
        }
        catch (IOException e)
        {
            fail("Unable to generate data files: " + e.getMessage());
        }
    }

    private void deleteGeneratedFiles()
    {
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(getPipelinePath() + "/v068_dump.sql"));
            String s;
            while (null != (s = in.readLine()))
            {
                if (!s.startsWith("COPY"))
                    continue;

                String plate = s.substring(5, s.indexOf('(')).trim();
                File f = new File(getPipelinePath() + "/" + plate + ".tsv");
                f.delete();
            }
        }
        catch (IOException e)
        {
            fail("Unable to generate data files: " + e.getMessage());
        }
    }

    private void deleteLogFiles()
    {
        File dataRoot = new File(getPipelinePath());
        File[] logFiles = dataRoot.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".log");
            }
        });
        for (File f : logFiles)
            f.delete();
    }

    private void deleteAssayUploadFiles()
    {
        deleteDir(new File(getPipelinePath(), "assaydata"));
    }

    private void deleteReportFiles()
    {
        deleteDir(new File(getPipelinePath(), "Reports"));
    }

    private void deleteDir(File dir)
    {
        if (!dir.exists())
            return;

        File[] assayDataFiles = dir.listFiles();
        for (File f : assayDataFiles)
            f.delete();

        dir.delete();
    }

    protected void selectOption(String name, int i, String value)
    {
        selectOptionByValue(Locator.tagWithName("select", name).index(i), value);
    }

    protected void assertSelectOption(String name, int i, String expected)
    {
        assertEquals(selenium.getSelectedValue(Locator.tagWithName("select", name).index(i).toString()), expected);
    }

    protected void createReport(String reportType)
    {
        // click the create button dropdown
        String id = ExtHelper.getExtElementId(this, "btn_createView");
        click(Locator.id(id));

        id = ExtHelper.getExtElementId(this, reportType);
        click(Locator.id(id));
        waitForPageToLoad();
    }
}
