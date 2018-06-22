package org.labkey.test.tests;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.admin.FileRootsManagementPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

@Category({DailyA.class})
public class FileRootMigrationTest extends BaseWebDriverTest
{
    private static final String FOLDER = "subfolder";
    private final File targetFileRoot = TestFileUtils.getTestTempDir();
    private File defaultProjectFileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
    private File defaultFolderFileRoot = TestFileUtils.getDefaultFileRoot(getProjectName() + "/" + FOLDER);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        FileRootMigrationTest init = (FileRootMigrationTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(getDriver());
        _containerHelper.createProject(getProjectName(), null);
        portalHelper.addWebPart("Files");
        _containerHelper.createSubfolder(getProjectName(), FOLDER);
        portalHelper.addWebPart("Files");
    }

    @Before
    public void cleanFiles() throws IOException
    {
        FileUtils.deleteDirectory(targetFileRoot);
        targetFileRoot.mkdirs();
        FileRootsManagementPage.beginAt(this, getProjectName())
                .useDefaultFileRoot()
                .clickSave();
        goToProjectHome();
        _fileBrowserHelper.deleteAll();
        deleteAllPipelineJobs();

        FileRootsManagementPage.beginAt(this, getProjectName())
                .useDefaultFileRoot()
                .clickSave();
        clickFolder(FOLDER);
        _fileBrowserHelper.deleteAll();
    }

    @Test
    public void testMigrateMove()
    {
        final File projFile1 = TestFileUtils.getSampleData("fileTypes/sample.txt");
        final File projFile2 = TestFileUtils.getSampleData("fileTypes/rtf_sample.rtf");
        final File folderFile1 = TestFileUtils.getSampleData("fileTypes/csv_sample.csv");
        final File folderFile2 = TestFileUtils.getSampleData("fileTypes/cmd_sample.cmd");
        List<File> sourceFiles = new ArrayList<>();

        String folderName = "folder " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;

        log("Upload files to project");
        goToProjectHome();
        _fileBrowserHelper.uploadFile(projFile1);
        sourceFiles.add(new File(defaultProjectFileRoot, projFile1.getName()));
        _fileBrowserHelper.createFolder(folderName);
        _fileBrowserHelper.selectFileBrowserItem(folderName + "/");
        _fileBrowserHelper.uploadFile(projFile2);
        sourceFiles.add(new File(defaultProjectFileRoot, folderName + "/" + projFile2.getName()));

        log("Upload files to subfolder");
        clickFolder(FOLDER);
        _fileBrowserHelper.uploadFile(folderFile1);
        sourceFiles.add(new File(defaultFolderFileRoot, folderFile1.getName()));
        _fileBrowserHelper.createFolder(folderName);
        _fileBrowserHelper.selectFileBrowserItem(folderName + "/");
        _fileBrowserHelper.uploadFile(folderFile2);
        sourceFiles.add(new File(defaultFolderFileRoot, folderName + "/" + folderFile2.getName()));

        goToProjectHome();

        FileRootsManagementPage fileRootsManagementPage = goToFolderManagement().goToFilesTab();

        PipelineStatusTable pipelineStatusTable = fileRootsManagementPage
                .useCustomFileRoot(targetFileRoot.getAbsolutePath())
                .saveAndMoveFiles();
        pipelineStatusTable.setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_AND_SUBFOLDERS);
        waitForPipelineJobsToComplete(1, "Copy Files for File Root Change", false);
        clickAndWait(Locator.linkWithText("COMPLETE"));

        assertTextPresent("Container: /" + getProjectName() + "/" + FOLDER,
                projFile1.getName(),
                projFile2.getName(),
                folderFile1.getName(),
                folderFile2.getName());
        boolean hasUploadLogs = isTextPresent(".upload.log");
        assertTextPresent("Copying file", hasUploadLogs ? 6 : 4);
        assertTextPresent("Copying directory", 4);

        clickFolder(getProjectName());
        _fileBrowserHelper.selectFileBrowserItem("/" + projFile1.getName());
        _fileBrowserHelper.selectFileBrowserItem("/" + folderName + "/" + projFile2.getName());
        clickFolder(FOLDER);
        _fileBrowserHelper.selectFileBrowserItem("/" + folderFile1.getName());
        _fileBrowserHelper.selectFileBrowserItem("/" + folderName + "/" + folderFile2.getName());

        List<String> foundFiles = sourceFiles.stream().filter(File::exists).map(File::getName).collect(Collectors.toList());
        assertTrue("Files not deleted from original file root after move: " + String.join(", ", foundFiles), foundFiles.isEmpty());
    }

    @Test
    public void testMigrateCopy()
    {
        File projFile1 = TestFileUtils.getSampleData("fileTypes/sample.txt");
        File projFile2 = TestFileUtils.getSampleData("fileTypes/rtf_sample.rtf");
        File folderFile1 = TestFileUtils.getSampleData("fileTypes/csv_sample.csv");
        File folderFile2 = TestFileUtils.getSampleData("fileTypes/cmd_sample.cmd");
        List<File> sourceFiles = new ArrayList<>();

        String folderName = "folder " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;

        log("Upload files to project");
        goToProjectHome();
        _fileBrowserHelper.uploadFile(projFile1);
        sourceFiles.add(new File(defaultProjectFileRoot, projFile1.getName()));
        _fileBrowserHelper.createFolder(folderName);
        _fileBrowserHelper.selectFileBrowserItem(folderName + "/");
        _fileBrowserHelper.uploadFile(projFile2);
        sourceFiles.add(new File(defaultProjectFileRoot, folderName + "/" + projFile2.getName()));

        log("Upload files to subfolder");
        clickFolder(FOLDER);
        _fileBrowserHelper.uploadFile(folderFile1);
        sourceFiles.add(new File(defaultFolderFileRoot, folderFile1.getName()));
        _fileBrowserHelper.createFolder(folderName);
        _fileBrowserHelper.selectFileBrowserItem(folderName + "/");
        _fileBrowserHelper.uploadFile(folderFile2);
        sourceFiles.add(new File(defaultFolderFileRoot, folderName + "/" + folderFile2.getName()));

        goToProjectHome();

        FileRootsManagementPage fileRootsManagementPage = goToFolderManagement().goToFilesTab();

        PipelineStatusTable pipelineStatusTable = fileRootsManagementPage
                .useCustomFileRoot(targetFileRoot.getAbsolutePath())
                .saveAndCopyFiles();
        pipelineStatusTable.setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_AND_SUBFOLDERS);
        waitForPipelineJobsToComplete(1, "Copy Files for File Root Change", false);
        clickAndWait(Locator.linkWithText("COMPLETE"));

        assertTextPresent("Container: /" + getProjectName() + "/" + FOLDER,
                projFile1.getName(),
                projFile2.getName(),
                folderFile1.getName(),
                folderFile2.getName());
        boolean hasUploadLogs = isTextPresent(".upload.log");
        assertTextPresent("Copying file", hasUploadLogs ? 6 : 4);
        assertTextPresent("Copying directory", 4);

        clickFolder(getProjectName());
        _fileBrowserHelper.selectFileBrowserItem("/" + projFile1.getName());
        _fileBrowserHelper.selectFileBrowserItem("/" + folderName + "/" + projFile2.getName());
        clickFolder(FOLDER);
        _fileBrowserHelper.selectFileBrowserItem("/" + folderFile1.getName());
        _fileBrowserHelper.selectFileBrowserItem("/" + folderName + "/" + folderFile2.getName());

        List<String> unfoundFiles = sourceFiles.stream().filter(f -> !f.exists()).map(File::getName).collect(Collectors.toList());
        assertTrue("Files missing from original file root after copy: " + String.join(", ", unfoundFiles), unfoundFiles.isEmpty());
    }

    @Test
    public void testMigrateToNonExistentFolder()
    {
        File projFile1 = TestFileUtils.getSampleData("fileTypes/sample.txt");

        goToProjectHome();
        _fileBrowserHelper.uploadFile(projFile1);

        FileRootsManagementPage fileRootsManagementPage = goToFolderManagement().goToFilesTab();

        PipelineStatusTable pipelineStatusTable = fileRootsManagementPage
                .useCustomFileRoot(targetFileRoot.getAbsolutePath())
                .saveAndMoveFiles();

        assertElementPresent(Locators.labkeyError);
    }

    @Test @Ignore("TODO")
    public void testMigratingProjectWithNonInheritingSubfolder()
    {

    }

    @Test @Ignore("TODO")
    public void testDisableEnableFileSharing()
    {

    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "FileRootMigrationTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
