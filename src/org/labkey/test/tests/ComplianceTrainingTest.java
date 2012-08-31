package org.labkey.test.tests;

import junit.framework.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 8/24/12
 * Time: 7:02 AM
 */
public class ComplianceTrainingTest extends BaseSeleniumWebTest
{
    private String listZIP =  getLabKeyRoot() + "/server/customModules/EHR_ComplianceDB/tools/SOP_Lists.zip";

    @Override
    protected String getProjectName()
    {
        return "ComplianceTraining";// + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUpTest();
        testSopSubmission();

    }

    private void testSopSubmission() throws Exception
    {
        beginAt("/ehr_compliancedb/" + getProjectName() + "/SOP_submission.view");
        reloadPage();

        Assert.assertTrue("Submit button not disabled", isElementPresent(Locator.xpath("//button[@id='SOPsubmitButton']/@disabled")));

        DataRegionTable dr1 = getDataRegion(0);
        DataRegionTable dr2 = getDataRegion(1);
        Assert.assertEquals("Incorrect row count found", 1, dr1.getDataRowCount());
        Assert.assertEquals("Incorrect row count found", 0, dr2.getDataRowCount());

        dr1.checkAllOnPage();
        clickButton("Mark Read");
        reloadPage();

        dr1 = getDataRegion(0);
        dr2 = getDataRegion(1);
        Assert.assertEquals("Incorrect row count found", 0, dr1.getDataRowCount());
        Assert.assertEquals("Incorrect row count found", 1, dr2.getDataRowCount());

        Assert.assertFalse("Submit button is still disabled", isElementPresent(Locator.xpath("//button[@id='SOPsubmitButton']/@disabled")));

        dr2.checkAllOnPage();
        clickButton("Mark Reread");
        reloadPage();

        click(Locator.xpath("//input[@id='sopCheck']"));
        clickButton("Submit", 0);
        waitForElement(Ext4Helper.ext4Window("SOPs Complete"));
        clickButton("OK");
        waitForPageToLoad();
    }

    private void reloadPage()
    {
        waitForPageToLoad();
        waitForText("Mark Read");
        waitForText("Mark Reread");
    }

    private DataRegionTable getDataRegion(int idx)
    {
        String id = getWrapper().getEval("window.Ext4.query('form > table[id^=dataregion_]')[" + idx + "].id");
        id = id.split("_")[1];
        return new DataRegionTable(id, this);
    }

    protected void setUpTest() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Compliance and Training");
        goToProjectHome();

        String[] props = {"/", "EmployeeContainer", "/" + getProjectName()};
        setModuleProperties(Collections.singletonMap("EHR_ComplianceDB", Collections.singletonList(props)));

        log("Creating Lists");
        ListHelper.importListArchive(this, getProjectName(), new File(listZIP));

        try
        {
            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

            InsertRowsCommand insertCmd;
            Map<String,Object> rowMap;

            //verify SOP requirement present
            String reqName = "SOP REVIEW-ANNUAL";
            SelectRowsCommand select = new SelectRowsCommand("ehr_compliancedb", "requirements");
            select.addFilter(new Filter("requirementname", reqName, Filter.Operator.EQUAL));
            select.setContainerFilter(ContainerFilter.AllFolders);
            SelectRowsResponse resp = select.execute(cn, getProjectName());

            if (resp.getRows().size() == 0)
            {
                insertCmd = new InsertRowsCommand("ehr_compliancedb", "requirements");
                rowMap = new HashMap<String,Object>();
                rowMap.put("requirementname", reqName);

                insertCmd.addRow(rowMap);
                insertCmd.execute(cn, getProjectName());
            }

            //verify category present
            String category = "Category";
            select = new SelectRowsCommand("ehr_compliancedb", "employeecategory");
            select.addFilter(new Filter("categoryname", category, Filter.Operator.EQUAL));
            resp = select.execute(cn, getProjectName());

            if (resp.getRows().size() == 0)
            {
                insertCmd = new InsertRowsCommand("ehr_compliancedb", "employeecategory");
                rowMap = new HashMap<String,Object>();
                rowMap.put("categoryname", category);

                insertCmd.addRow(rowMap);
                insertCmd.execute(cn, getProjectName());
            }

            //create employee record
            insertCmd = new InsertRowsCommand("ehr_compliancedb", "employees");
            rowMap = new HashMap<String,Object>();
            rowMap.put("employeeid", PasswordUtil.getUsername());
            rowMap.put("email", PasswordUtil.getUsername());
            rowMap.put("firstname", "Test");
            rowMap.put("lastname", "User");
            rowMap.put("category", category);

            insertCmd.addRow(rowMap);
            insertCmd.execute(cn, getProjectName());

            //add SOP record
            insertCmd = new InsertRowsCommand("lists", "SOPs");
            rowMap = new HashMap<String,Object>();
            rowMap.put("Id", "SOP1");
            rowMap.put("name", "SOP 1");

            insertCmd.addRow(rowMap);
            insertCmd.execute(cn, getProjectName());

            //add record to SOP requirements
            insertCmd = new InsertRowsCommand("ehr_compliancedb", "sopbycategory");
            rowMap = new HashMap<String,Object>();
            rowMap.put("sop_id", "SOP1");
            rowMap.put("category", category);

            insertCmd.addRow(rowMap);
            insertCmd.execute(cn, getProjectName());
        }
        catch (CommandException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doCleanup() throws Exception
    {
        deleteProject(getProjectName());
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    public boolean isFileUploadTest()
    {
        return true;
    }
}
