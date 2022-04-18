package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.list.ManageListsGrid;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebTestHelper.getRemoteApiConnection;

@Category(Daily.class)
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class ProjectCreatorUserTest extends BaseWebDriverTest
{
    private static final String PROJECT_CREATOR_USER = "project_creator@permission.test";
    private static final String READER = "reader@permission.test";
    private static final String PROJECT_NAME_PC = "Folder by Project Creator";
    private static final String TEMPLATE_PROJECT = "Template project";

    @BeforeClass
    public static void setup()
    {
        ProjectCreatorUserTest initTest = (ProjectCreatorUserTest) getCurrentTest();
        initTest.doSetup();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    private void doSetup()
    {
        _containerHelper.createProject(TEMPLATE_PROJECT, "Study");
        importStudyFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"));
        _userHelper.createUser(PROJECT_CREATOR_USER, true, true);
        _userHelper.createUser(READER, true, true);
        ApiPermissionsHelper permHelper = new ApiPermissionsHelper(this);
        permHelper.addMemberToRole(PROJECT_CREATOR_USER, "Project Creator", PermissionsHelper.MemberType.user, "/");
        permHelper.addMemberToRole(PROJECT_CREATOR_USER, "Folder Admin", PermissionsHelper.MemberType.user, TEMPLATE_PROJECT);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(TEMPLATE_PROJECT, afterTest);
        _userHelper.deleteUsers(false, PROJECT_CREATOR_USER, READER);
    }

    @Before
    public void beforeTest()
    {
        _containerHelper.deleteProject(PROJECT_NAME_PC, false, WAIT_FOR_PAGE);
    }

    @Test
    public void testCreateProjectByProjectCreator() throws IOException, CommandException
    {
        log("Project Creator creating the project with admin permission");
        goToHome();
        impersonate(PROJECT_CREATOR_USER);
        Map<String, Object> params = new HashMap<>();
        params.put("name", PROJECT_NAME_PC);
        params.put("assignProjectAdmin", "true");
        params.put("folderType", "Collaboration");
        createProject(params);
        stopImpersonating();

        log("Verifying the permissions");
        goToProjectHome(PROJECT_NAME_PC);
        navBar().goToPermissionsPage().assertPermissionSetting(PROJECT_CREATOR_USER, "Project Administrator");
        navBar().goToPermissionsPage().assertPermissionSetting(PROJECT_CREATOR_USER, "Folder Administrator");

        log("Cleanup : Deleting the project");
        _containerHelper.deleteProject(PROJECT_NAME_PC, false, WAIT_FOR_PAGE);

        log("Project Creator creating the project without admin permission");
        goToHome();
        impersonate(PROJECT_CREATOR_USER);
        params = new HashMap<>();
        params.put("name", PROJECT_NAME_PC);
        params.put("assignProjectAdmin", "false");
        params.put("folderType", "Collaboration");
        createProject(params);
        stopImpersonating();

        log("Verifying the permissions");
        goToProjectHome(PROJECT_NAME_PC);
        navBar().goToPermissionsPage().assertNoPermission(PROJECT_CREATOR_USER, "No Permissions");
    }

    @Test
    public void testCreateProjectByReader() throws IOException, CommandException
    {
        log("Verifying Reader creating the project fails");
        goToHome();
        impersonate(READER);
        Map<String, Object> params = new HashMap<>();
        params.put("name", PROJECT_NAME_PC);
        params.put("assignProjectAdmin", "false");
        params.put("folderType", "Collaboration");
        String response = createProject(params);
        stopImpersonating();

        assertEquals("Should not be able to create the project", "403 : Forbidden", response);
        assertFalse(projectMenu().projectLinkExists(PROJECT_NAME_PC));
    }

    @Test
    public void testCreateProjectByTemplate() throws IOException, CommandException
    {
        String containerId = ((APIContainerHelper) _containerHelper).getContainerId(TEMPLATE_PROJECT);
        goToHome();
        impersonate(PROJECT_CREATOR_USER);
        Map<String, Object> params = new HashMap<>();
        params.put("name", PROJECT_NAME_PC);
        params.put("assignProjectAdmin", "true");
        params.put("folderType", "Template");
        params.put("templateSourceId", containerId);
        params.put("templateWriterTypes", "Lists");
        createProject(params);
        stopImpersonating();

        goToProjectHome(PROJECT_NAME_PC);
        assertTrue(projectMenu().projectLinkExists(PROJECT_NAME_PC));
        navBar().goToPermissionsPage().assertPermissionSetting(PROJECT_CREATOR_USER, "Project Administrator");
        navBar().goToPermissionsPage().assertPermissionSetting(PROJECT_CREATOR_USER, "Folder Administrator");

        ManageListsGrid listsGrid = goToManageLists().getGrid();
        assertEquals("Incorrect lists copied from template", Arrays.asList("Lab Machines", "Reagents", "Technicians"), listsGrid.getListNames());
    }

    private String createProject(Map<String, Object> params) throws IOException
    {
        PostCommand<CommandResponse> command = new PostCommand<>("admin", "createProject");
        command.setParameters(params);
        try
        {
            command.execute(getRemoteApiConnection(), "/");
        }
        catch (CommandException e)
        {
            return e.getMessage();
        }

        return "Success";
    }


    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}