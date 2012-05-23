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
package org.labkey.test.module;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PostgresOnlyTest;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Mar 23, 2012
 * Time: 12:47:27 PM
 */
public class CDSTest extends BaseSeleniumWebTest implements PostgresOnlyTest
{
    private static final String PROJECT_NAME = "CDSTest Project";
    private static final File STUDY_ZIP = new File(getSampledataPath(), "CDS/Dataspace.folder.zip");
    private static final String STUDIES[] = {"Demo Study", "Not Actually CHAVI 001", "NotRV144"};
    private static final String LABS[] = {"Arnold/Bellew Lab", "LabKey Lab", "Piehler/Eckels Lab"};
    private static final String GROUP_NAME = "CDSTest_AGroup";
    private static final String GROUP_NAME2 = "CDSTest_BGroup";
    private static final String GROUP_NAME3 = "CDSTest_CGroup";
    private static final String GROUP_NULL = "Group creation cancelled";
    private static final String GROUP_DESC = "Intersection of " +LABS[1]+ " and " + LABS[2];
    public final static int CDS_WAIT = 5000;

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/CDS";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public void doCleanup()
    {
        // Delete any containers and users created by the test.
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void doTestSteps()
    {
        createProject(PROJECT_NAME, "Study");
        importStudyFromZip(STUDY_ZIP.getPath());
        enableModule(PROJECT_NAME, "CDS");
        addWebPart("CDS Management");

        importCDSData("Antigens", new File(getSampledataPath(), "CDS/antigens.tsv"));
        importCDSData("Assays", new File(getSampledataPath(), "CDS/assays.tsv"));
        importCDSData("Studies", new File(getSampledataPath(), "CDS/studies.tsv"));
        importCDSData("Labs", new File(getSampledataPath(), "CDS/labs.tsv"));
        importCDSData("People", new File(getSampledataPath(), "CDS/people.tsv"));
        importCDSData("AssayPublications", new File(getSampledataPath(), "CDS/assay_publications.tsv"));

        populateFactTable();

        selenium.windowMaximize(); // Provides more useful screenshots on failure
        verifyCounts();
        verifyFilters();
        verifyNounPages();
    }

/// Test substeps

    private void importCDSData(String query, File dataFile)
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(query);
        ListHelper.clickImportData(this);

        setFormElement(Locator.id("tsv3"), getFileContents(dataFile), true);
        clickButton("Submit");
    }

    private void populateFactTable()
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Populate Fact Table");
        uncheckCheckbox("dataset", "HIV Test Results");
        uncheckCheckbox("dataset", "Physical Exam");
        submit();

        assertLinkPresentWithText("NAb");
        assertLinkPresentWithText("Luminex");
        assertLinkPresentWithText("Lab Results");
        assertLinkPresentWithText("MRNA");
        assertLinkPresentWithText("ADCC");
        assertTextPresent(
                //NAb
                "1 rows added to Antigen from VirusName",
                "195 rows added to fact table.",
                //Luminex
                "6 rows added to fact table. ",
                //Lab Results
                "rows added to Assay from 'Lab Results'",
                "23 rows added to fact table.",
                //MRNA
                "5 rows added to fact table.",
                //ADCC
                "48 rows added to fact table.");
    }

    private void verifyCounts()
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Application");

        assertLinkNotPresentWithText("Home");
        assertLinkNotPresentWithText("Admin");

        assertAllParticipantsPortalPage();

        // 14902
        click(SearchBy.Studies);
        assertFilterStatusPanel(STUDIES[0], "Demo Study", 6, 1, 3, 2, 20, 12, SearchBy.Studies);
        clickButton("use as filter", 0);
        waitForTextToDisappear("Not Actually CHAVI 001", CDS_WAIT);
        assertFilterStatusCounts(6, 1, 3, 2, 20);
        goToAppHome();
        waitForText("Current Active Filters", CDS_WAIT);
        waitForText("Demo Study", 2, CDS_WAIT);
        assertTextNotPresent("Not Actually CHAVI 001");
        selectCDSGroup("All participants", false);
        waitForText("Not Actually CHAVI 001", CDS_WAIT);
        selectCDSGroup("Active filters", false);
        assertTextPresent("This is the set of filters");
        click(SearchBy.Studies);
        assertFilterStatusPanel(STUDIES[0], "Demo Study", 6, 1, 3, 2, 20, 12, SearchBy.Studies);
        clickButton("clear filters", 0);
        waitForText("NotRV144", CDS_WAIT);
        goToAppHome();
        waitForText("Not Actually CHAVI 001", CDS_WAIT);
        // end 14902

        click(SearchBy.Studies);
        assertFilterStatusPanel(STUDIES[1], "Not Actually ...", 12, 1, 3, 2, 8, 12, SearchBy.Studies);
        assertFilterStatusPanel(STUDIES[2], "NotRV144", 11, 1, 3, 2, 3, 12, SearchBy.Studies);
        goToAppHome();
        click(SearchBy.Antigens);
        toggleExplorerBar("3");
        assertFilterStatusPanel("H061.14", "H061.14", 12, 1, 1, 1, 8, 12, SearchBy.Antigens);
        toggleExplorerBar("1A");
        assertFilterStatusPanel("SF162.LS", "SF162.LS", 6, 1, 1, 1, 20, 12, SearchBy.Antigens);
        toggleExplorerBar("1B");
        assertFilterStatusPanel("MW965.26", "MW965.26", 6, 1, 1, 1, 20, 6, SearchBy.Antigens);
        goToAppHome();
        click(SearchBy.Assays);
        assertFilterStatusPanel("Lab Results", "Lab Results", 23, 3, 5, 0, 0, 29, SearchBy.Assays);
        assertFilterStatusPanel("ADCC-Ferrari", "ADCC-Ferrari", 12, 1, 3, 1, 4, 29, SearchBy.Assays);
        assertFilterStatusPanel("Luminex-Sample-LabKey", "Luminex-Sampl...", 6, 1, 3, 1, 1, 29, SearchBy.Assays);
        assertFilterStatusPanel("NAb-Sample-LabKey", "NAb-Sample-La...", 29, 3, 5, 2, 26, 29, SearchBy.Assays);
        assertFilterStatusPanel("mRNA assay", "mRNA assay", 5, 1, 3, 1, 0, 0, SearchBy.Assays);
        goToAppHome();
        click(SearchBy.Labs);
        assertFilterStatusPanel(LABS[0], "Arnold/Bellew...", 6, 1, 1, 2, 1, 23, SearchBy.Labs);
        assertFilterStatusPanel(LABS[1], "LabKey Lab", 23, 3, 2, 3, 26, 23, SearchBy.Labs);
        assertFilterStatusPanel(LABS[2], "Piehler/Eckel...", 18, 2, 2, 2, 7, 23, SearchBy.Labs);
        goToAppHome();
        click(SearchBy.Participants);
        pickCDSSort("Country");
        assertFilterStatusPanel("South Africa", "South Africa", 5, 1, 1, 1, 3, 18, SearchBy.Participants);
        assertFilterStatusPanel("USA", "USA", 19, 3, 4, 3, 31, 19, SearchBy.Participants);
        assertFilterStatusPanel("Thailand", "Thailand", 5, 1, 3, 1, 3, 18, SearchBy.Participants);
        goToAppHome();
    }

    private void verifyFilters()
    {
        log("Verify multi-select");

        // 14910
        click(SearchBy.Antigens);
        toggleExplorerBar("1A");
        toggleExplorerBar("1B");
        shiftSelectBars("MW965.26", "ZM197M.PB7");
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(), 'DJ263.8')]"), WAIT_FOR_JAVASCRIPT);
        assertElementPresent(Locator.xpath("//div[@class='filtermember']"), 6);
        assertFilterStatusCounts(6, 1, 1, 1, 20);
        clickButton("clear selection", 0);
        goToAppHome();
        // end 14910

        click(SearchBy.Labs);
        selectBars(LABS[0], LABS[1]);
        assertFilterStatusCounts(6, 1, 0, 2, 0);
        selectBars(LABS[0], LABS[2]);
        assertFilterStatusCounts(0, 0, 0, 0, 0);
        selectBars(LABS[1], LABS[2]);
        assertFilterStatusCounts(12, 1, 0, 2, 0);
        clickButton("use as filter", 0);
        clickButton("save group", 0);
        waitForText("Selection and Active Filters");
        waitForText("Selection and Active Filters (12)");
        waitForText("Only Active Filters (12)");
        setFormElement("groupname", GROUP_NAME);
        setFormElement("groupdescription", GROUP_DESC);
        clickButton("Save", 0);
        waitForTextToDisappear(LABS[0]);
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(), '"+ GROUP_NAME +"')]"), WAIT_FOR_JAVASCRIPT);
        assertFilterStatusCounts(12,1,0,2,0);
        clickButton("clear filters", 0);
        waitForText(LABS[0]);
        assertFilterStatusCounts(29,3,5,3,31);

        //TODO: Shouldn't be able to create unfiltered group
//        clickButton("save group", 0);
//        waitForText("Selection and Active Filters (29)");
//        assertTextPresent("Only Active Filters (29)");
//        setFormElement("groupname", "Unfiltered" + GROUP_NAME);
//        clickButton("Save", 0);

        goToAppHome();
        selectCDSGroup(GROUP_NAME, true);
        assertTextPresent(GROUP_DESC);

        waitForText("No Matching Assays Found.", WAIT_FOR_JAVASCRIPT);
        assertCDSPortalRow(SearchBy.Studies, STUDIES[1], "1 total");
        assertCDSPortalRow(SearchBy.Antigens, "1 clades, 1 tiers, 1 sources (Unknown)", "0 total");
        assertCDSPortalRow(SearchBy.Assays, "No Matching Assays Found.", "0 total");
        assertCDSPortalRow(SearchBy.Labs, "LabKey Lab, Piehler/Eckels Lab", "2 total labs");
        assertCDSPortalRow(SearchBy.Participants, "4 races, 1 locations", "12 total participants");

        click(SearchBy.Labs);
        assertFilterStatusCounts(12,1,0,2,0);

        goToAppHome();
        selectCDSGroup("All participants", false);
        assertAllParticipantsPortalPage();

        //test more group saving
        selectCDSGroup(GROUP_NAME, true);
        click(SearchBy.Participants);
        pickCDSSort("Gender");
        selectBars("f");

        clickButton("save group", 0);
        waitForText("Selection and Active Filters");
        waitForText("Selection and Active Filters (8)");
        assertTextPresent("Only Active Filters (12)");
        click(Locator.css(".withSelectionRadio input"));
        setFormElement("groupname", GROUP_NULL);
        clickButton("Cancel", 0);
        waitForTextToDisappear("Selection and Active Filters (8)");

        selectBars("f");
        clickButton("save group", 0);
        waitForText("Selection and Active Filters");
        waitForText("Selection and Active Filters (8)");
        assertTextPresent("Only Active Filters (12)");
        click(Locator.css(".filterOnlyRadio input"));
        setFormElement("groupname", GROUP_NAME2);
        clickButton("Save", 0);
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(), '"+ GROUP_NAME2 +"')]"), WAIT_FOR_JAVASCRIPT);

        selectBars("f");
        clickButton("save group", 0);
        waitForText("Selection and Active Filters");
        waitForText("Selection and Active Filters (8)");
        assertTextPresent("Only Active Filters (12)");
        click(Locator.css(".withSelectionRadio input"));
        setFormElement("groupname", GROUP_NAME3);
        clickButton("Save", 0);
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(), '"+ GROUP_NAME3 +"')]"), WAIT_FOR_JAVASCRIPT);

        // saved filter without including current selection (should be the same as initial group)
        goToAppHome();
        selectCDSGroup(GROUP_NAME2, true);
        assertTextNotPresent(GROUP_DESC);

        waitForText("12 total participants", WAIT_FOR_JAVASCRIPT);
        assertCDSPortalRow(SearchBy.Studies, STUDIES[1], "1 total");
        assertCDSPortalRow(SearchBy.Antigens, "1 clades, 1 tiers, 1 sources (Unknown)", "0 total");
        assertCDSPortalRow(SearchBy.Assays, "No Matching Assays Found.", "0 total");
        assertCDSPortalRow(SearchBy.Labs, "LabKey Lab, Piehler/Eckels Lab", "2 total labs");
        assertCDSPortalRow(SearchBy.Participants, "4 races, 1 locations", "12 total participants");

        click(SearchBy.Labs);
        assertFilterStatusCounts(12,1,0,2,0);

        // saved filter including current selection (Gender: f)
        goToAppHome();
        selectCDSGroup(GROUP_NAME3, true);
        assertTextNotPresent(GROUP_DESC);
        assertTextPresent("Gender:");

        waitForText("8 total participants", WAIT_FOR_JAVASCRIPT);
        assertCDSPortalRow(SearchBy.Studies, STUDIES[1], "1 total");
        assertCDSPortalRow(SearchBy.Antigens, "1 clades, 1 tiers, 1 sources (Unknown)", "0 total");
        assertCDSPortalRow(SearchBy.Assays, "No Matching Assays Found.", "0 total");
        assertCDSPortalRow(SearchBy.Labs, "LabKey Lab, Piehler/Eckels Lab", "2 total labs");
        assertCDSPortalRow(SearchBy.Participants, "4 races, 1 locations", "8 total participants");

        click(SearchBy.Labs);
        assertFilterStatusCounts(8,1,0,2,0);

        // Group creation cancelled
        goToAppHome();
        assertTextNotPresent(GROUP_NULL);
    }

    private void verifyNounPages()
    {
        selectCDSGroup("All participants", false);
        click(SearchBy.Assays);

        // check placeholders
        assertAssayInfoPage("Lab Results", "default.png", "default.png", "", "", "", "", "");
        assertAssayInfoPage("ADCC-Ferrari", "team_Mark_Igra.jpg", "team_Alan_Vezina.jpg",
                "Mark Igra\n" +
                        "marki@labkey.com\n" +
                        "Partner",
                "Alan Vezina\n" +
                        "alanv@labkey.com\n" +
                        "Developer",
                "Methodology: ICS\n" +
                        "Target Area: Adaptive: humoral and B-cell",
                "This is an ADCC assay.",
                "Immune escape from HIV-specific antibody-dependent cellular cytotoxicity (ADCC) pressure.");
        assertAssayInfoPage("Luminex-Sample-LabKey", "team_Nick_Arnold.jpg", "team_Nick_Arnold.jpg",
                "Nick Arnold\n" +
                        "nicka@labkey.com\n" +
                        "Developer",
                "Nick Arnold\n" +
                        "nicka@labkey.com\n" +
                        "Developer",
                "Methodology: Luminex\n" +
                        "Target Area: Adaptive: humoral and B-cell",
                "We measured something using a Luminex assay",
                "Inhibition of HIV-1 replication in human lymphoid tissues ex vivo by measles virus.");
        assertAssayInfoPage("mRNA assay", "team_Mark_Igra.jpg", "team_Nick_Arnold.jpg",
                "Mark Igra\n" +
                        "marki@labkey.com\n" +
                        "Partner",
                "Nick Arnold\n" +
                        "nicka@labkey.com\n" +
                        "Developer",
                "Methodology: ICS\n" +
                        "Target Area: Innate",
                "This one tested gene expression.",
                "Development of an in vitro mRNA degradation assay utilizing extracts from HIV-1- and SIV-infected cells.");
        assertAssayInfoPage("NAb-Sample-LabKey", "team_Karl_Lum.jpg", "team_Kristin_Fitzsimmons.jpg",
                "Karl Lum\n" +
                        "klum@labkey.com\n" +
                        "Developer",
                "Kristin Fitzsimmons\n" +
                        "kristinf@labkey.com\n" +
                        "ScrumMaster",
                "Methodology: NAb\n" +
                        "Target Area: Adaptive: humoral and B-cell",
                "This tested antibodies.",
                "Vaccinology: precisely tuned antibodies nab HIV.");
    }

/// CDS App helpers

    private void pickCDSSort(String sortBy)
    {
        click(Locator.css(".sortDropdown"));
        waitAndClick(Locator.xpath("//span[text()='"+sortBy+"']"));
    }

    private void selectBars(String... bars)
    {
        String subselect = bars[0];
        if (subselect.length() > 10)
            subselect = subselect.substring(0, 9);
        sleep(1000);
        waitAndClick(Locator.xpath("//span[@class='barlabel' and text() = '" + bars[0] + "']"));
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(),'" + subselect + "')]"));
        if(bars.length > 1)
        {
            selenium.controlKeyDown();
            for(int i = 1; i < bars.length; i++)
            {
                click(Locator.xpath("//span[@class='barlabel' and text() = '"+bars[i]+"']"));
                subselect = bars[i];
                if (subselect.length() > 10)
                    subselect = subselect.substring(0, 9);
                waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(),'" + subselect + "')]"));
            }
            selenium.controlKeyUp();
        }
    }

    private void shiftSelectBars(String... bars)
    {
        String subselect = bars[0];
        if (subselect.length() > 10)
            subselect = subselect.substring(0, 9);
        sleep(1000);
        waitAndClick(Locator.xpath("//span[@class='barlabel' and text() = '" + bars[0] + "']"));
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(),'" + subselect + "')]"));
        if(bars.length > 1)
        {
            selenium.shiftKeyDown();
            for(int i = 1; i < bars.length; i++)
            {
                click(Locator.xpath("//span[@class='barlabel' and text() = '"+bars[i]+"']"));
                subselect = bars[i];
                if (subselect.length() > 10)
                    subselect = subselect.substring(0, 9);
                waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(),'" + subselect + "')]"));
            }
            selenium.shiftKeyUp();
        }
    }

    private void selectCDSGroup(String group, boolean titleShown)
    {
        waitAndClick(Locator.xpath("//span[text()='"+group+"']"));
        if(titleShown)
            waitForElement(Locator.css(".title:contains('"+group+"')"));
        else
            waitForElementToDisappear(Locator.xpath("//div[@class='title' and "+Locator.NOT_HIDDEN+"]"), WAIT_FOR_JAVASCRIPT);
    }

    private void goToAppHome()
    {
        clickAt(Locator.xpath("//div[contains(@class, 'connectorheader')]//div[contains(@class, 'logo')]"), "1,1");
        waitForElement(Locator.xpath("//div[contains(@class, 'connectorheader')]//div[contains(@class, 'logo')]/h2/br"), WAIT_FOR_JAVASCRIPT);
    }

    private void click(SearchBy by)
    {
        clickAt(Locator.xpath("//span[@class = 'label' and text() = ' "+by+"']"), "1,1");
        waitForText("Showing number of: Participants", WAIT_FOR_JAVASCRIPT);
    }

    private void viewInfo(String barLabel)
    {
        mouseOver(Locator.xpath("//span[@class='barlabel' and text() = '"+barLabel+"']/.."));
        mouseOver(Locator.xpath("//span[@class='barlabel' and text() = '"+barLabel+"']/..//button"));
        click(Locator.xpath("//span[@class='barlabel' and text() = '"+barLabel+"']/..//button"));
        waitForElement(Locator.button("X"));
        if(!isElementPresent(Locator.css(".savetitle")))
        {
            refresh();
            waitForElement(Locator.css(".savetitle"), WAIT_FOR_JAVASCRIPT);
        }
        waitForText(barLabel);
        assertEquals("Wrong page title.", barLabel, getText(Locator.css(".savetitle")));
    }

    private void closeInfoPage()
    {
        clickButton("X", 0);
        waitForElementToDisappear(Locator.button("X"), WAIT_FOR_JAVASCRIPT);
    }

/// CDS App asserts

    private void assertAllParticipantsPortalPage()
    {
        assertCDSPortalRow(SearchBy.Studies, STUDIES[0]+", "+STUDIES[1]+", "+STUDIES[2], "3 total");
        assertCDSPortalRow(SearchBy.Antigens, "5 clades, 5 tiers, 5 sources (Unknown, ccPBMC, Lung, Plasma, ucPBMC)", "31 total");
        assertCDSPortalRow(SearchBy.Assays, "Lab Results, ADCC-Ferrari, Luminex-Sample-LabKey, NAb-Sample-LabKey, mRNA assay", "5 total");
        assertCDSPortalRow(SearchBy.Labs, "Arnold/Bellew Lab, LabKey Lab, Piehler/Eckels Lab", "3 total labs");
        assertCDSPortalRow(SearchBy.Participants, "6 races, 3 locations", "29 total participants");
    }

    private void assertCDSPortalRow(SearchBy by, String expectedDetail, String expectedTotal)
    {
        waitForText(" " + by);
        assertTrue("'by "+by+"' search option is not present", isElementPresent(Locator.xpath("//div[starts-with(@id, 'summarydataview')]/div["+
                "./div[contains(@class, 'bycolumn')]/span[@class = 'label' and text() = ' "+by+"']]")));
        String actualDetail = getText(Locator.xpath("//div[starts-with(@id, 'summarydataview')]/div["+
                "./div[contains(@class, 'bycolumn')]/span[@class = 'label' and text() = ' "+by+"']]"+
                "/div[contains(@class, 'detailcolumn')]"));
        assertEquals("Wrong details for search by "+by+".", expectedDetail, actualDetail);
        String actualTotal = getText(Locator.xpath("//div[starts-with(@id, 'summarydataview')]/div["+
                "./div[contains(@class, 'bycolumn')]/span[@class = 'label' and text() = ' "+by+"']]"+
                "/div[contains(@class, 'totalcolumn')]"));
        assertEquals("Wrong total for search by "+by+".", expectedTotal, actualTotal);
    }

    // Sequential calls to this should have different participant counts.
    private void assertFilterStatusPanel(String barLabel, String filteredLabel, int participantCount, int studyCount, int assayCount, int contributorCount, int antigenCount, int maxCount, SearchBy searchBy)
    {
        Double barLen = ((double)participantCount/(double)maxCount)*100;
        String barLenStr = ((Long)Math.round(Math.floor(barLen))).toString();
//        waitForElement(Locator.xpath("//div[./span[@class='barlabel' and text() = '"+barLabel+"']]/span[@class='index' and contains(@style, 'width: "+barLenStr+"')]"), WAIT_FOR_JAVASCRIPT);
        selectBars(barLabel);
        assertFilterStatusCounts(participantCount, studyCount, assayCount, contributorCount, antigenCount);
//        waitForElement(Locator.xpath("//td[contains(text(), '" + searchBy + ":'"));
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(), '"+ filteredLabel +"')]"), WAIT_FOR_JAVASCRIPT);
//        waitForElement(Locator.xpath("//div[./span[@class='barlabel' and text() = '"+barLabel+"']]/span[@class='index' and contains(@style, 'width: "+barLenStr+"')]"), WAIT_FOR_JAVASCRIPT);
//        waitForElement(Locator.xpath("//div[./span[@class='barlabel' and text() = '"+barLabel+"']]/span[contains(@class, 'index-selected') and @style and not(contains(@style, 'width: 0%;'))]"), WAIT_FOR_JAVASCRIPT);
    }

    private void assertFilterStatusCounts(int participantCount, int studyCount, int assayCount, int contributorCount, int antigenCount)
    {
        waitForElement(Locator.xpath("//div[@class='highlight-value' and text()='"+participantCount+"']"), WAIT_FOR_JAVASCRIPT);
        waitForText(studyCount+(studyCount!=1?" Studies":" Study"));
        waitForText(assayCount+(assayCount!=1?" Assays":" Assay"));
        waitForText(contributorCount!=1?" Contributors":" Contributor");
        waitForText(antigenCount+(antigenCount!=1?" Antigens":" Antigen"));
    }

    // Assumes you are on find-by-assay page, returns there when done
    private void assertAssayInfoPage(String assay, String contributorImg, String pocImg, String leadContributor, String pointOfContact, String details, String assayAbstract, String relatedPubs)
    {
        viewInfo(assay);
        if(contributorImg.equals(pocImg))
        {
            Locator.XPathLocator imgLoc = Locator.xpath("//img[@src='/labkey/cds/images/pictures/"+pocImg+"']");
            waitForElement(imgLoc);
            assertElementPresent(imgLoc, 2);
        }
        else
        {
            Locator.XPathLocator imgLead = Locator.xpath("//img[@src='/labkey/cds/images/pictures/"+pocImg+"']");
            Locator.XPathLocator imgContact= Locator.xpath("//img[@src='/labkey/cds/images/pictures/"+contributorImg+"']");

            waitForElement(imgLead);
            waitForElement(imgContact);

            assertElementPresent(imgLead, 1);
            assertElementPresent(imgContact, 1);
        }
        assertEquals("Incorrect Lead Contributor", leadContributor.replace("\n", ""), getText(Locator.css(".assayInfoLeadContributor")).replace("\n", ""));
        assertEquals("Incorrect Assay Point of Contact", pointOfContact.replace("\n", ""), getText(Locator.css(".assayInfoPointOfContact")).replace("\n", ""));
        assertEquals("Incorrect Assay Details", details.replace("\n", ""), getText(Locator.css(".assayInfoDetails")).replace("\n", ""));
        //assertEquals("Incorrect Description", ("Description" + pointOfContact).replace("\n", ""), getText(Locator.css(".assayInfoDescription")).replace("\n", ""));
        assertEquals("Incorrect Assay Abstract", assayAbstract.replace("\n", ""), getText(Locator.css(".assayInfoAbstract")).replace("\n", ""));
        assertEquals("Incorrect Related Publications", relatedPubs.replace("\n", ""), getText(Locator.css(".assayInfoRelatedPublications")).replace("\n", ""));
        closeInfoPage();
    }

    private void toggleExplorerBar(String largeBarText)
    {
        click(Locator.xpath("//div[@class='bar large']//span[contains(@class, 'barlabel') and text()='" + largeBarText + "']//..//..//div[contains(@class, 'saecollapse')]"));
        sleep(350);
    }

/// CDS classes, enums and string generators

    private static enum SearchBy
    {
        Studies,
        Antigens,
        Assays,
        Labs,
        Participants
    }
    }
