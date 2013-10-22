/*
 * Copyright (c) 2011-2013 LabKey Corporation
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
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.MS2;
import org.labkey.test.ms2.MS2TestBase;

/**
 * User: elvan
 * Date: 8/19/11
 * Time: 3:32 PM
 */
@Category({DailyB.class, MS2.class})
public class LibraTest extends MS2TestBase
{
    private String standardView = "Standard View";
    protected String proteinProphetView = "Protein Prophet View";
    private String iTRAQ_QUANTITATION_RATIO = "Ratio ";

    @Override
    protected String getProjectName()
    {
        return "LibraTest" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    protected void configure()
    {
        _containerHelper.createProject(getProjectName(), "MS2");
        setPipelineRoot(getLabKeyRoot() + "/sampledata/xarfiles/ms2pipe/iTRAQ/");
        clickProject(getProjectName());

        clickButton("Process and Import Data");
        _fileBrowserHelper.selectFileBrowserItem("xtandem/Libra/iTRAQ.search.xar.xml");
        _fileBrowserHelper.selectImportDataAction("Import Experiment");
        goToModule("Pipeline");
        waitForPipelineJobsToComplete(1, "Experiment Import - iTRAQ.search.xar.xml", false);
        clickProject(getProjectName());
        for (int i = 0; i < 10; i++)
        {
            refresh();
            if (isLinkPresentContainingText(runName))
            {
                break;
            }
            // Takes a moment for run to appear after import.
            sleep(1000);
        }

        clickAndWait(Locator.linkContainingText(runName));
    }


    String runName = "itraq/iTRAQ (Libra)";
    int normalizationCount = 8;

    @Override
    protected void doTestSteps()
    {
        configure();
        waitForText("Grouping");
        selenium.select("viewTypeGrouping", "Standard");
        clickButton("Go");
        _customizeViewsHelper.openCustomizeViewPanel();
        addNormalizationCount();

        _customizeViewsHelper.saveCustomView(standardView);

        checkForITRAQNormalization();

        proteinProphetTest();
        groupTest();
        specificProteinTest();

        spectraCountTest();
    }

    private void spectraCountTest()
    {
        clickProject(getProjectName());
        checkAllOnPage("MS2SearchRuns");
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickButton("Compare", 0);
        clickAndWait(Locator.linkWithText("Spectra Count"));
        clickRadioButtonById("SpectraCountPeptide");
        clickButton("Compare");
        assertTextPresent("-.MM'EILRGSPALSAFR.I");
        assertLinkPresentWithTextCount("itraq/iTRAQ (Libra)", 27);

        // Try setting a target protein
        clickAndWait(Locator.linkWithText("Spectra Count Options"));
        setFormElement("targetProtein", "gi|34392343");
        clickButton("Compare");
        assertLinkPresentWithTextCount("itraq/iTRAQ (Libra)", 1);
        assertTextPresent("R.TDTGEPM'GR.G");
        clickAndWait(Locator.linkContainingText("gi|34392343"));
        assertTextPresent("84,731");
        goBack();

        // Customize view to pull in other columns
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("TrimmedPeptide");
        _customizeViewsHelper.addCustomizeViewColumn(new String[] {"Protein", "ProtSequence"});
        _customizeViewsHelper.addCustomizeViewColumn(new String[] {"Protein", "BestName"});
        _customizeViewsHelper.addCustomizeViewColumn(new String[] {"Protein", "Mass"});
        _customizeViewsHelper.saveDefaultView();
        assertTextPresent("84731", "MPEETQAQDQPMEEEEVETFAFQAEIAQLM");

        // Try a TSV export
        addUrlParameter("exportAsWebPage=true");
        clickExportToText();
        assertTextPresent("# Target protein: gi|34392343", "R.TDTGEPM'GR.G", "84731", "MPEETQAQDQPMEEEEVETFAFQAEIAQLM");

        // Try filtering based on a custom view using a different grouping
        goBack();
        clickAndWait(Locator.linkWithText("Spectra Count Options"));
        click(Locator.linkWithText("Create or Edit View"));
        waitForElement(Locator.xpath("//button[text()='Save']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _customizeViewsHelper.addCustomizeViewFilter("Hyper", "Hyper", "Is Greater Than", "250");
        assertRadioButtonSelected("spectraConfig", "SpectraCountPeptide");
        _customizeViewsHelper.saveCustomView("HyperFilter");
        clickRadioButtonById("SpectraCountPeptideCharge");
        setFormElement(Locator.id("PeptidesFilter.viewName"), "HyperFilter");
        setFormElement("targetProtein", "");
        clickButton("Compare");
        assertLinkPresentWithTextCount("itraq/iTRAQ (Libra)", 12);
        assertTextPresent("-.MM'EILRGSPALSAFR.I", "R.TDTGEPM'GR.G");
        assertTextNotPresent("R.AEGTFPGK.I", "R.ILEKSGSPER.I");

        // Try a TSV export
        addUrlParameter("exportAsWebPage=true");
        clickExportToText();
        assertTextPresent("# Peptide filter: (Hyper > 250)", "-.MM'EILRGSPALSAFR.I", "R.TDTGEPM'GR.G");
        assertTextNotPresent("R.AEGTFPGK.I", "R.ILEKSGSPER.I");

        // Validate that it remembers our options
        goBack();
        clickAndWait(Locator.linkWithText("Spectra Count Options"));
        assertRadioButtonSelected("spectraConfig", "SpectraCountPeptideCharge");
        assertFormElementEquals(Locator.id("PeptidesFilter.viewName"), "HyperFilter");
    }

    protected void newWindowTest(String linkToClick, String verificationString, String... additionalChecks)
    {
        selenium.openWindow("", "prot");
        click(Locator.linkContainingText(linkToClick));
        selenium.selectWindow("prot");
        waitForPageToLoad();
        waitForText(verificationString);

        checkForITRAQNormalization();
        checkForITRAQQuantitation();

        assertTextPresent(additionalChecks);
        selenium.close();
        selenium.selectWindow(null);

    }
    private void specificProteinTest()
    {

        newWindowTest("gi|2144275|JC5226_ubiquitin_", "Protein Sequence");
        //TODO:  single cell check
    }

    private void groupTest()
    {
        newWindowTest("4", "Scan", "gi|28189228|similar_to_polyub");
    }

    private void checkForITRAQNormalization()
    {
        checkForNormalizationCountofSomething("Normalized ");
    }

    private void checkForITRAQQuantitation()
    {
        checkForNormalizationCountofSomething(iTRAQ_QUANTITATION_RATIO);
    }

    protected void checkForNormalizationCountofSomething(String toCheck)
    {
        for(int i = 1; i <= normalizationCount; i++)
        {
            assertTextPresent(toCheck + i);
        }

    }

    private void addNormalizationCount()
    {
        for(int i = 1; i <= normalizationCount; i++)
        {
            _customizeViewsHelper.addCustomizeViewColumn("iTRAQQuantitation/Normalized" + i, "Normalized " + i);
        }
    }

    private void proteinProphetTest()
    {
        clickMenuButton("Views", "ProteinProphet");

        waitForElement(Locator.navButton("Views"), WAIT_FOR_JAVASCRIPT);
        _customizeViewsHelper.openCustomizeViewPanel();
        for(int i=1; i<=normalizationCount; i++)
        {
            _customizeViewsHelper.addCustomizeViewColumn("ProteinProphetData/ProteinGroupId/iTRAQQuantitation/Ratio" + i, "Ratio " + i);
        }

        addNormalizationCount();

        _customizeViewsHelper.saveCustomView(proteinProphetView);
        checkForITRAQQuantitation();


        assertTableCellTextEquals("dataregion_MS2Peptides", 2, "Ratio 1", "0.71");

        Locator img = Locator.xpath("//img[contains(@id,'MS2Peptides-Handle')]");
        click(img);
        checkForITRAQNormalization();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/ms2";
    }
}
