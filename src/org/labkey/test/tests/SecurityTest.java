/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Igra
 * Date: March 23, 2007
 */
public class SecurityTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "SecurityVerifyProject";
    protected static final String ADMIN_USER_TEMPLATE = "_admin.template@security.test";
    protected static final String NORMAL_USER_TEMPLATE = "_user.template@security.test";
    protected static final String BOGUS_USER_TEMPLATE = "bogus@bogus@bogus";
    protected static final String PROJECT_ADMIN_USER = "admin@security.test";
    protected static final String NORMAL_USER = "user@security.test";
    protected static final String[] PASSWORDS= {"0asdfgh!", "1asdfgh!", "2asdfgh!", "3asdfgh!", "4asdfgh!", "5asdfgh!", "6asdfgh!", "7asdfgh!", "8asdfgh!", "9asdfgh!", "10asdfgh!"};
    protected static String NORMAL_USER_PASSWORD = PASSWORDS[0];
    protected static final String TO_BE_DELETED_USER = "delete_me@security.test";
    protected static final String SITE_ADMIN_USER = "siteadmin@security.test";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/core";
    }

    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}

        deleteUser(ADMIN_USER_TEMPLATE);
        deleteUser(NORMAL_USER_TEMPLATE);
        deleteUser(PROJECT_ADMIN_USER);
        deleteUser(NORMAL_USER);
        deleteUser(TO_BE_DELETED_USER);
        deleteUser(SITE_ADMIN_USER);
    }

    protected void doTestSteps()
    {
        enableEmailRecorder();

        displayNameTest();
        clonePermissionsTest();
        tokenAuthenticationTest();
        impersonationTest();
        guestTest();
        addRemoveSiteAdminTest();

        log("Check welcome emails [6 new users]");
        goToModule("Dumbster");
        assertEquals("Expected 12 notification emails (+3 rows).", 15, getTableRowCount("dataregion_EmailRecord"));
        // Once in the message itself, plus copies in the headers
        assertTextPresent(": Welcome", 18);
        cantReachAdminToolFromUserAccount(false);
        if(!isQuickTest())
        {
            passwordStrengthTest();
            dumbsterTest();
        }
        passwordResetTest();
    }

    protected static final String HISTORY_TAB_TITLE = "History:";
    protected static final String[] unreachableUrlsShortList = {"/admin/showAdmin.view?",
            "/user/showUsers.view?",
            "/security/group.view?group=Administrators"};

    protected static final String[] unreachableUrlsExtendedList = {"/security/group.view?group=Developers",
        "user/showUsers.view?","security/project.view?returnUrl=%2Fuser%2FshowUsers.view%3F","admin/createFolder.view?",
        "/analytics/begin.view?","/login/configure.view?", "/admin/customizeEmail.view?",
        "/admin/filesSiteSettings.view?", "/admin/projectSettings.view?",
        "/flow/flowAdmin.view?","/admin/folderManagement.view?",
        "/admin/reorderFolders.view?", "/admin/customizeSite.view?",
        "/reports/configureReportsAndScripts.view?", "/audit/showAuditLog.view?",
        "/search/admin.view?", "/ms1/showAdmin.view?", "/ms2/showMS2Admin.view?",
        "/experiment-types/begin.view?", "/pipeline-status/showList.view?", "/pipeline/setup.view?",
        "/ms2/showProteinAdmin.view?", "/admin/actions.view?", "/admin/caches.view?",
        "/admin/dbChecker.view?",
            //"/admin/credits.view?",
        "/query/dataSourceAdmin.view?",
        "/admin/dumpHeap.view?", "/admin/environmentVariables.view?", "/admin/memTracker.view?",
        "/admin/queries.view?", "/admin/resetErrorMark.view?", "/admin/showThreads.view?",
        "/admin-sql/scripts.view?", "/admin/systemProperties.view?", "/admin/emailTest.view?",
        "/admin/showAllErrors.view?","/admin/showErrorsSinceMark.view?",  "/admin/showPrimaryLog.view?"};

    /**
     * verify that a normal user does not get a link to a the admin console or see their own history, nor
     * reach an admin-only url directly.
     *
     *
     * @param longForm if and only if longFrom = true, test against the extended list of admin urls.
     * if longForm = false, test against only a few high priority URLs.
     */
    protected void cantReachAdminToolFromUserAccount(boolean longForm)
    {
        //just in case, create user
//        createUserAndNotify(NORMAL_USER, null);

        //verify that you can see the text "history" in the appropriate area as admin.  If this fails, the
        //check below is worthless

        goToMyAccount();
        assertTextPresent(HISTORY_TAB_TITLE);

        //log in as normal user
        impersonate(NORMAL_USER);

        //admin site link not available
        assertTextNotPresent("Admin");

        //can't reach admin urls directly either

        for(String url : unreachableUrlsShortList)
        {
                assertUrlUnreachableDueToPermissions(url);
        }

        if(longForm)
        {

            for(String url : unreachableUrlsExtendedList)
            {
                assertUrlUnreachableDueToPermissions(url);
            }
        }

        ///user/showUsers.view?

        //shouldn't be able to view own history either
        clickNavButton("Home");
        goToMyAccount();
        assertTextNotPresent(HISTORY_TAB_TITLE);

        stopImpersonating();
    }

    public void assertUrlUnreachableDueToPermissions(String url)
    {
        log("Attempting to reach URL user does not have permission for:  " + url);
        beginAt(url);
        assertAtUserUserLacksPermissionPage();
    }

    /**
     *
     * preconditions:  NORAM_USER exists with password NORMAL_USER_PASSWORD.  Currently logged in as admin
     * post conditions
     */
    public void passwordResetTest()
    {
        //get user a password
        String username = NORMAL_USER;
        String password = NORMAL_USER_PASSWORD;

        password = adminPasswordResetTest(username, password+"adminReset");
        
        String resetUrl = userForgotPasswordWorkflowTest(username, password);
        
        userPasswordResetTest(username, resetUrl);

        ensureSignedInAsAdmin();
    }

    private void dumbsterTest()
    {
        assertNoDumbsterPermission(PROJECT_ADMIN_USER);
        assertNoDumbsterPermission(NORMAL_USER);
    }

    private void assertNoDumbsterPermission(String user)
    {
        clickLinkWithText(PROJECT_NAME);
        goToModule("Dumbster");
        pushLocation();
        impersonate(user);
        popLocation();
        assertTextPresent("You must be a site administrator to view the email record");
        stopImpersonating();
    }

    protected static enum PasswordAlterType {RESET_PASSWORD, CHANGE_PASSWORD}

    //issue 3876

    /**
     * Precondtions:  able to reset user's password at resetUrl, db in weak-password mode
     */
    protected void userPasswordResetTest(String username, String resetUrl)
    {
        ensureSignedOut();

        beginAt(resetUrl);

        String[][] passwords = {{"fooba", null}, {"foobar","foobar2"}};
        String[][] messages = {{"Your password must be six characters or more."}, {"Your password entries didn't match."}};
        attemptSetInvalidPasswords(PasswordAlterType.RESET_PASSWORD, passwords, messages);

        resetPassword(resetUrl, NORMAL_USER, NORMAL_USER_PASSWORD);
    }

//    protected static final int RESET_PASSWORD = 1;
//    protected static final int CHANGE_PASSWORD = 2;

    /**RESET_PASSWORD means user or admin initiated password change that
     * involves visiting a webpage specified in an e-mail to change the password,
     * without knowing the past password.
     * CHANGE_PASSWORD means a user went into their account information and initiated the change by
     * selecting "change password".  This requires the old password to work.
      */

    protected void attemptSetInvalidPasswords(PasswordAlterType changeType, String[][] passwords, String[][] errors)
    {
        //if reset, should already be at reset Url
        for (int i = 0; i < errors.length; i++)
        {
            switch (changeType)
            {
                case RESET_PASSWORD:
                    attemptSetInvalidPassword(changeType, passwords[i], errors[i]);
            }
        }
    }

    protected void attemptSetInvalidPassword(PasswordAlterType changeType, String[] passwords, String... errors)
    {
        switch (changeType)
        {
            case CHANGE_PASSWORD:
                fail("unsupported use of change password type");
                break;
            case RESET_PASSWORD:
                setFormElement("password", passwords[0]);
                String password2 = passwords[1];
                if(password2==null) password2 = passwords[0];
                setFormElement("password2", password2);
                clickNavButton("Set Password");                
                assertTextPresent(errors);
        }
    }

    /**
     * preconditions: there exists user username with password password
     * postcondtions:  user can reset password at return value, not signed in
     *
     * @param username  user's username
     * @param password user's password
     * @return URL to use to reset user password
     */
    //issue 3876
    private String userForgotPasswordWorkflowTest(String username, String password)
    {
        ensureSignedOut();

        String resetUrl = userInitiatePasswordReset(username);

        signOut();

        //attempt sign in with old password- should succeed
        signIn(username,password, true);
        signOut();

        return resetUrl;
    }

    public String userInitiatePasswordReset(String username)
    {
        goToHome();
        ensureSignedOut();

        clickLinkContainingText("Sign In");
        clickLinkContainingText("Forgot your password?");
        setText("email", username);
        clickButtonContainingText("Submit", 0);

//        clickButtonContainingText("Home");
        signIn();
        String resetUrl = getPasswordResetUrl(username);

        //sign out

        return resetUrl;
    }

    String[] wrongPasswordEntered =
                new String[] {"The e-mail address and password you entered did not match any accounts on file.",
                "Note: Passwords are case sensitive; make sure your Caps Lock is off."};

    /**
     *
     * preconditions: logged in as admin
     * postconditions:  not signed in, username's password is return value
     *
     * @param username username to initiate password rest for
     * @param password user's current password (before test starts)
     * @return user's new password
     */
    private String adminPasswordResetTest(String username, String password)
    {
        String newPassword = password +"1";
        goToSiteUsers();
        clickLinkContainingText(username);
        clickButtonContainingText("Reset Password", 0);
        getConfirmationAndWait();
        clickNavButton("Done");

        String url = getPasswordResetUrl(username);


        //make sure user can't log in with current password
        signOut();
        signInShouldFail(username, password, wrongPasswordEntered);

        resetPassword(url, username, newPassword);
        
        signOut();
        
        //attempt to log in with old password (should fail)
        signInShouldFail(username, password, wrongPasswordEntered);
        
//        resetPasswordDisallowedValues();
//
//        resetPasswordValid(password);

        return newPassword;
    }

    protected void addRemoveSiteAdminTest()
    {
        // test for issue 13921
        goToSiteAdmins();
        setFormElement("names", NORMAL_USER);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
        assertTextPresent(NORMAL_USER);
        checkCheckbox("delete", NORMAL_USER);
        selenium.chooseOkOnNextConfirmation();
        clickNavButton("Update Group Membership");
        assertEquals(selenium.getConfirmation(), "Permanently remove selected users from this group?");
        assertElementNotPresent(Locator.checkboxByNameAndValue("delete", NORMAL_USER));
        clickLinkWithText(PROJECT_NAME);
    }

    protected void guestTest()
    {
        clickLinkWithText(PROJECT_NAME);
        enterPermissionsUI();
        setSiteGroupPermissions("All Site Users", "Author");
        setSiteGroupPermissions("Guests", "Reader");
        exitPermissionsUI();

        addWebPart("Messages");
        assertNavButtonPresent("New");
        pushLocation();
        signOut();
        popLocation();
        clickLinkWithText(PROJECT_NAME);
        assertNavButtonNotPresent("New");
        signIn();
        clickLinkWithText(PROJECT_NAME);
        assertNavButtonPresent("New");
        impersonate(NORMAL_USER);
        clickLinkWithText(PROJECT_NAME);
        assertNavButtonPresent("New");
        stopImpersonating();
    }

    protected void displayNameTest()
    {
        //set display name to user's email minus domain
        String oldDisplayName = getDisplayName();
        goToMyAccount();
        clickNavButton("Edit");

        String email = PasswordUtil.getUsername();
        String displayName;
        if (email.contains("@"))
        {
            displayName = email.substring(0, email.indexOf("@"));
        }
        else
        {
            displayName = email;
        }
        setFormElement("displayName", displayName);
        clickNavButton("Submit");

        //now set it back
        clickNavButton("Edit");
        setFormElement("displayName", oldDisplayName);
        clickNavButton("Submit");
    }

    protected void clonePermissionsTest()
    {
        // create admin templates, plus test bogus & duplicate email addresses
        createUserAndNotify(ADMIN_USER_TEMPLATE + '\n' + NORMAL_USER_TEMPLATE + '\n' + NORMAL_USER_TEMPLATE + '\n' + BOGUS_USER_TEMPLATE, null, false);
        assertTextPresent("Failed to create user bogus@bogus@bogus: Invalid email address");
        //nav trail check
        assertTextPresent("Site Users >  ");
        assertTextPresent(NORMAL_USER_TEMPLATE + " was already a registered system user. Click here to see this user's profile and history.");

        // create the project and set permissions
        _containerHelper.createProject(PROJECT_NAME, null);
        createPermissionsGroup("Administrators");
        clickManageGroup("Administrators");
        setFormElement("names", ADMIN_USER_TEMPLATE);
        clickNavButton("Update Group Membership");
        setPermissions("Administrators", "Project Administrator");

        createPermissionsGroup("Testers");
        assertPermissionSetting("Testers", "No Permissions");
        setPermissions("Testers", "Editor");
        clickManageGroup("Testers");
        setFormElement("names", NORMAL_USER_TEMPLATE);
        clickNavButton("Update Group Membership");

        // create users and verify permissions
        createUserAndNotify(PROJECT_ADMIN_USER, ADMIN_USER_TEMPLATE);
        createUserAndNotify(SITE_ADMIN_USER, PasswordUtil.getUsername());
        createUserAndNotify(NORMAL_USER, NORMAL_USER_TEMPLATE);
        createUserAndNotify(TO_BE_DELETED_USER, NORMAL_USER_TEMPLATE);

        // verify permissions
        checkGroupMembership(PROJECT_ADMIN_USER, "SecurityVerifyProject/Administrators", 2);
        checkGroupMembership(NORMAL_USER, "SecurityVerifyProject/Testers", 1);
        assertTextPresent("Site Users >  User Details >  Permissions >  ");
    }

    protected void checkGroupMembership(String userName, String groupName, int expectedCount)
    {
        goToSiteUsers();

        Locator userAccessLink = Locator.xpath("//td[text()='" + userName + "']/..//td/a[contains(@href,'userAccess.view')]");
        boolean isPresent = isElementPresent(userAccessLink);

        // If user is not found but paging indicators are, then show all 
        if (!isPresent && isLinkPresentContainingText("Next") && isLinkPresentContainingText("Last"))
        {
            clickNavButton("Page Size", 0);
            clickLinkWithText("Show All");
            isPresent = isElementPresent(userAccessLink);
        }

        if (isPresent)
        {
            clickLink(userAccessLink);
            
            // check for the expected number of group membership links (note: they may be hidden by expandos)
            assertLinkPresentWithTextCount(groupName, expectedCount);
            return;
        }
        fail("Unable to verify group membership of cloned user privileges");
    }

    protected void tokenAuthenticationTest()
    {
        clickLinkWithText(PROJECT_NAME);
        String homePageUrl = removeUrlParameters(getURL().toString());  // Absolute URL for redirect, get rid of '?'
        String relUrl = getCurrentRelativeURL();
        boolean newSchool = relUrl.contains("project-");
        String baseUrl = removeUrlParameters(getCurrentRelativeURL()).replaceAll("/project/", "/login/");
        baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/')+1);
        if (newSchool)
            baseUrl += "login-";
        // Attempt to verify bogus token -- should result in failure
        String xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=ABC");
        assertFailureAuthenticationToken(xml);

        beginAt(baseUrl + "createToken.view?returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertTrue(removeUrlParameters(getURL().toString()).equals(homePageUrl));

        Map<String, String> params = getUrlParameters();
        String email = params.get("labkeyEmail");
        String emailName;
        String userName = PasswordUtil.getUsername();
        // If we are using IE, then the email will be stripped of its @etc.
        if (!userName.contains("@"))
        {
            emailName = email.substring(0, email.indexOf("@"));
        }
        else
        {
            emailName = email;
        }
        assertTrue(emailName.equals(userName));
        String token = params.get("labkeyToken");
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertSuccessAuthenticationToken(xml, token, email, 32783);

        beginAt(baseUrl + "invalidateToken.view?labkeyToken=" + token + "&returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertTrue(removeUrlParameters(getURL().toString()).equals(homePageUrl));
        // Should fail now
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertFailureAuthenticationToken(xml);

        impersonate(NORMAL_USER);

        beginAt(baseUrl + "createToken.view?returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertTrue(removeUrlParameters(getURL().toString()).equals(homePageUrl));

        params = getUrlParameters();
        email = params.get("labkeyEmail");
        assertTrue(email.equals(NORMAL_USER));
        token = params.get("labkeyToken");
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertSuccessAuthenticationToken(xml, token, email, 15);

        // Back to the admin user
        stopImpersonating();

        // Test that LabKey Server sign out invalidates the token
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertFailureAuthenticationToken(xml);
    }


    protected void assertFailureAuthenticationToken(String xml)
    {
        assertTrue(xml.startsWith("<TokenAuthentication success=\"false\" message=\"Unknown token\"/>"));
    }


    protected void assertSuccessAuthenticationToken(String xml, String token, String email, int permissions)
    {
        String correct = "<TokenAuthentication success=\"true\" token=\"" + token + "\" email=\"" + email + "\" permissions=\"" + permissions + "\"/>";
        assertTrue(xml.startsWith(correct));
    }


    private String retrieveFromUrl(String relativeUrl)
    {
        String newline = System.getProperty("line.separator");
        InputStream is = null;

        try
        {
            StringBuilder sb = new StringBuilder();
            URL url = new URL(WebTestHelper.getBaseURL() + "/" + relativeUrl);
            is = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
                sb.append(newline);
            }

            return sb.toString();
        }
        catch (Exception e)
        {
            log("Failure attempting to retrieve " + relativeUrl);
            log(e.getMessage());
            fail();
            return null;
        }
        finally
        {
            if (null != is)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    //
                }
            }
        }
    }


    // Move to BaseWebTest?
    private Map<String, String> getUrlParameters()
    {
        String queryString = null;

        try
        {
            queryString = getURL().toURI().getQuery();  // URI decodes the query string
        }
        catch (URISyntaxException e)
        {
            log(e.getMessage());
            fail();
        }

        Map<String, String> map = new HashMap<String, String>();
        if (queryString != null)
        {
            String[] params = queryString.split("&");

            for (String param : params)
            {
                int index = param.indexOf('=');
                map.put(param.substring(0, index), param.substring(index + 1));
            }
        }

        return map;
    }


    private String removeUrlParameters(String url)
    {
        int index = url.indexOf('?');

        if (-1 == index)
            return url;
        else
            return url.substring(0, index);
    }


    protected void impersonationTest()
    {
        String testUserDisplayName = getDisplayName();

        impersonate(TO_BE_DELETED_USER);
        String deletedUserDisplayName = getDisplayName();
        assertTextNotPresent("Admin Console");
        stopImpersonating();

        impersonate(SITE_ADMIN_USER);
        String siteAdminDisplayName = getDisplayName();
        ensureAdminMode();
        goToAdminConsole();
        assertTextPresent("Already impersonating; click here to change back to " + testUserDisplayName);
        deleteUser(TO_BE_DELETED_USER, true);
        stopImpersonating();

        ensureAdminMode();
        goToAdminConsole();
        clickLinkWithText("audit log");

        selectOptionByText("view", "User events");
        waitForPageToLoad();

        DataRegionTable table = new DataRegionTable("audit", this, false);

        table.getDataAsText(2, 2);
        String createdBy      = table.getDataAsText(2, "Created By");
        String impersonatedBy = table.getDataAsText(2, "Impersonated By");
        String user           = table.getDataAsText(2, "User");
        String comment        = table.getDataAsText(2, "Comment");

        assertTrue("Incorrect display for deleted user -- expected '<nnnn>', found '" + user + "'", user.matches("<\\d{4,}>"));
        assertEquals("Incorrect log entry for deleted user", createdBy + impersonatedBy + user + comment, siteAdminDisplayName + testUserDisplayName + user + deletedUserDisplayName + " was deleted from the system");
    }

    protected void passwordStrengthTest()
    {
        String simplePassword = "3asdfghi"; // Only two character types. 8 characters long.
        String shortPassword = "4asdfg!"; // Only 7 characters long. 3 character types.
        setDbLoginConfig(PasswordRule.Strong, null);

        setInitialPassword(NORMAL_USER, simplePassword);
        assertTextPresent("Your password must contain three of the following"); // fail, too simple

        setFormElement("password", shortPassword);
        setFormElement("password2", shortPassword);
        clickNavButton("Set Password");
        assertTextPresent("Your password must be eight characters or more."); // fail, too short

        setFormElement("password", PASSWORDS[0]);
        setFormElement("password2", PASSWORDS[0]);
        clickNavButton("Set Password");
        assertSignOutAndMyAccountPresent();
        //success
        impersonate(NORMAL_USER);

        changePassword(PASSWORDS[0], PASSWORDS[1]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[1], simplePassword); // fail, too simple
        assertTextPresent("Your password must contain three of the following");
        changePassword(PASSWORDS[1], shortPassword); // fail, too short
        assertTextPresent("Your password must be eight characters or more.");
        changePassword(PASSWORDS[1], PASSWORDS[2]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[2], PASSWORDS[3]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[3], PASSWORDS[4]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[4], PASSWORDS[5]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[5], PASSWORDS[6]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[6], PASSWORDS[7]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[7], PASSWORDS[8]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[8], PASSWORDS[9]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[9], PASSWORDS[10]);
        assertTextNotPresent("Choose a new password.");
        changePassword(PASSWORDS[10], PASSWORDS[1]); // fail, used 9 passwords ago.
        assertTextPresent("Your password must not match a recently used password.");
        changePassword(PASSWORDS[10], PASSWORDS[0]);
        assertTextNotPresent("Choose a new password.");

        stopImpersonating();
        resetDbLoginConfig();
    }
}
