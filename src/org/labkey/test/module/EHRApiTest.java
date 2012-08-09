package org.labkey.test.module;

import com.sun.jna.platform.win32.Guid;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.labkey.test.WebTestHelper;
import org.labkey.test.testpicker.TestHelper;
import org.labkey.test.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 8/6/12
 * Time: 6:05 PM
 */
public class EHRApiTest extends EHRStudyTest
{
    private static final String PROJECT_NAME = "EHR_TestProject";// + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private static final String FOLDER_NAME = "EHR";
    private static final String CONTAINER_PATH = PROJECT_NAME + "/" + FOLDER_NAME;

    private static final String STUDY_ZIP = "/sampledata/study/EHR Study Anon Small.zip";

    private static String FIELD_QCSTATELABEL = "QCStateLabel";
    private static String FIELD_OBJECTID = "objectid";
    private static String FIELD_LSID = "lsid";

    private String[] weightFields = {"Id", "date", "enddate", "project", "weight", FIELD_QCSTATELABEL, FIELD_OBJECTID, FIELD_LSID, "_recordid"};
    private static final String DATE_SUBSTITUTION = "@@CURDATE@@";
    private Object[] weightData1 = {"TestSubject1", DATE_SUBSTITUTION, null, null, "12", EHRQCState.IN_PROGRESS.label, null, null, "_recordID"};
    private List<Long> _saveRowsTimes;
    private SimpleDateFormat _tf = new SimpleDateFormat("yyyy-MM-dd kk:mm");

    private static String[] SUBJECTS = {"rh0000", "cy999", "r12345"};
    private static String[] ROOMS = {"Room1", "Room2", "Room3"};
    private static String[] CAGES = {"1", "2", "3"};
    private static Integer[] PROJECTS = {12345, 123456, 1234567};

    @Override
    public void runUITests()
    {
        log("There are no UI tests");
    }

    @Override
    public void runApiTests() throws Exception
    {
        initProject();
        defineQCStates();
        setupEhrPermissions();
        goToProjectHome();

        doSecurityTest();
        doTriggerScriptTest();

    }

    @Override
    public void doCleanup()
    {
//        long startTime = System.currentTimeMillis();
//        try {deleteProject(PROJECT_NAME);} catch (Throwable t) { /*ignore*/ }
//        if(isTextPresent(PROJECT_NAME))
//        {
//            log("Wait extra long for folder to finish deleting.");
//            while (isTextPresent(PROJECT_NAME) && System.currentTimeMillis() - startTime < 300000) // 5 minutes max.
//            {
//                sleep(5000);
//                refresh();
//            }
//            if (!isTextPresent(PROJECT_NAME)) log("Test Project deleted in " + (System.currentTimeMillis() - startTime) + "ms");
//            else Assert.fail("Test Project not finished deleting after 5 minutes");
//        }
//        goToHome();
//
//        try{deleteUser(DATA_ADMIN.getUser());}catch(Throwable T){}
//        try{deleteUser(REQUESTER.getUser());}catch(Throwable T){}
//        try{deleteUser(BASIC_SUBMITTER.getUser());}catch(Throwable T){}
//        try{deleteUser(REQUEST_ADMIN.getUser());}catch(Throwable T){}
//        try{deleteUser(FULL_UPDATER.getUser());}catch(Throwable T){}
//        try{deleteUser(FULL_SUBMITTER.getUser());}catch(Throwable T){}
    }

    protected void initProject()
    {
        //TODO: maybe just inherit from parent?
        enableEmailRecorder();

        _containerHelper.createProject(PROJECT_NAME, null);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "Collaboration", new String[]{"EHR", "Pipeline", "Study"});
        enableModule(PROJECT_NAME, "EHR");

        clickLinkWithText(FOLDER_NAME);

        //import the study first, so we have fewer queries to validate
        goToModule("Study");
        importStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());

        String[] prop = {"/" + PROJECT_NAME, "EHRStudyContainer", "/" + CONTAINER_PATH};
        setModuleProperties(Collections.singletonMap("EHR", Collections.singletonList(prop)));

        beginAt(getBaseURL() + "/ehr/" + CONTAINER_PATH + "/_initEHR.view");
        clickNavButton("Delete All", 0);
        waitForText("Delete Complete", 120000);
        clickNavButton("Populate All", 0);
        waitForText("Populate Complete", 120000);

        goToProjectHome();
    }

    private void doSecurityTest() throws Exception
    {
        testUserAgainstAllStates(DATA_ADMIN);
        testUserAgainstAllStates(REQUESTER);
        testUserAgainstAllStates(BASIC_SUBMITTER);
        testUserAgainstAllStates(FULL_SUBMITTER);
        testUserAgainstAllStates(FULL_UPDATER);
        testUserAgainstAllStates(REQUEST_ADMIN);
        resetErrors(); //note: inserting records without permission will log errors by design.  the UI should prevent this from happening, so we want to be aware if it does occur
    }

    private void doTriggerScriptTest() throws Exception
    {
        _saveRowsTimes = new ArrayList<Long>();
        createTestSubjects();

        weightValidationTest();


        calculateAverage();
    }

    private void createTestSubjects()
    {
        try
        {
            JSONObject extraContext = new JSONObject();
            extraContext.put("errorThreshold", "ERROR");
            extraContext.put("skipIdFormatCheck", true);
            extraContext.put("allowAnyId", true);
            extraContext.put("targetQC", "Completed");

            //insert into demographics
            log("Creating test subjects");
            String[] fields = new String[]{"Id", "Species", "Birth", "Gender", "date"};
            Object[][] data = new Object[][]{
                {SUBJECTS[0], "Rhesus", (new Date()).toString(), "m", new Date()},
                {SUBJECTS[1], "Cynomolgus", (new Date()).toString(), "m", new Date()},
                {SUBJECTS[2], "Rhesus", (new Date()).toString(), "f", new Date()}
            };
            JSONObject insertCommand = prepareInsertCommand("study", "demographics", FIELD_LSID, fields, data);
            doSaveRows(DATA_ADMIN, Collections.singletonList(insertCommand), extraContext, true);

            //set housing
            log("Creating initial housing records");
            Date pastDate1 = _tf.parse("2012-01-03 09:30");
            Date pastDate2 = _tf.parse("2012-05-03 19:20");
            fields = new String[]{"Id", "date", "enddate", "room", "cage"};
            data = new Object[][]{
                {SUBJECTS[0], pastDate1, pastDate2, ROOMS[0], CAGES[0]},
                {SUBJECTS[1], pastDate1, pastDate2, ROOMS[0], CAGES[0]},
                {SUBJECTS[1], pastDate2, null, ROOMS[2], CAGES[2]}
            };
            insertCommand = prepareInsertCommand("study", "Housing", FIELD_LSID, fields, data);
            doSaveRows(DATA_ADMIN, Collections.singletonList(insertCommand), extraContext, true);

            //set a base weight
            log("Setting initial weights");
            fields = new String[]{"Id", "date", "weight", "QCStateLabel"};
            data = new Object[][]{
                {SUBJECTS[0], pastDate2, 10.5, EHRQCState.COMPLETED.label},
                {SUBJECTS[0], new Date(), 12, EHRQCState.COMPLETED.label}
            };
            insertCommand = prepareInsertCommand("study", "Weight", FIELD_LSID, fields, data);
            doSaveRows(DATA_ADMIN, Collections.singletonList(insertCommand), extraContext, true);

            //set assignment
            log("Setting initial assignments");
            fields = new String[]{"Id", "date", "enddate", "project"};
            data = new Object[][]{
                {SUBJECTS[0], pastDate1, pastDate2, PROJECTS[0]},
                {SUBJECTS[1], pastDate1, pastDate2, PROJECTS[0]},
                {SUBJECTS[1], pastDate2, null, PROJECTS[2]}
            };
            insertCommand = prepareInsertCommand("study", "Housing", FIELD_LSID, fields, data);
            doSaveRows(DATA_ADMIN, Collections.singletonList(insertCommand), extraContext, true);
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void weightValidationTest()
    {
        //expect weight out of range
        Object[][] data = new Object[][]{
            {SUBJECTS[0], new Date(), null, null, 120, EHRQCState.IN_PROGRESS.label, null, null, "recordID"}
        };
        Map<String, List<String>> expected = new HashMap<String, List<String>>();
        expected.put("weight", Arrays.asList("Weight above the allowable value of 35 kg for Rhesus", "Weight gain of >10%. Last weight 12 kg"));
        testValidationMessage("study", "weight", weightFields, data, expected);

        //expect INFO for +10% diff
        data = new Object[][]{
            {SUBJECTS[0], new Date(), null, null, 20, EHRQCState.IN_PROGRESS.label, null, null, "recordID"}
        };
        expected = new HashMap<String, List<String>>();
        expected.put("weight", Collections.singletonList("Weight gain of >10%. Last weight 12 kg"));
        testValidationMessage("study", "weight", weightFields, data, expected);

        //expect INFO for -10% diff
        data = new Object[][]{
            {SUBJECTS[0], new Date(), null, null, 5, EHRQCState.IN_PROGRESS.label, null, null, "recordID"}
        };
        expected = new HashMap<String, List<String>>();
        expected.put("weight", Collections.singletonList("Weight drop of >10%. Last weight 12 kg"));
        testValidationMessage("study", "weight", weightFields, data, expected);
    }

    private void arrivalTest()
    {
        //TODO
        //arrival: verify cascade insert into demographics, housing

    }

    private void assignmentTest()
    {
        //verify
        // removeTimeFromDate(),
        //  set release for row.enddate && row.projectedRelease
        //nnumber of allowed animals



    }

    private void birthTest()
    {
        //on public, insert into demographics for center births
        //also weight
    }

    private void bloodTest()
    {
        //on public, do clinpath inserts
        //on after insert: request emails

        //on insert: blood volume
    }

    private void chargesTest()
    {
        //calculate total price
    }

    private void clinicalRemarksTest()
    {
        //if(!row.so && !row.a && !row.p && !row.remark)
    }

    private void clinicalObservationsTest()
    {
        //require observation or remark
    }

    private void clinpathRunsTest()
    {
        //check clinpath_tests

        //send request
    }

    private void deathsTest()
    {
        //tattoo validation
    }

    private void demographicsTest()
    {
        //update status field
    }

    private void departureTest()
    {
        //update status
    }

    private void drugTest()
    {
//        if(row.begindate)
//            row.date = row.begindate;
        //verify math for conc
    }

    private void housingTest()
    {
        //verify existing animals in cage
        //on public: enforce only 1 active per animal

        //verify cagemates set
    }

    private void irregularObsTest()
    {
        //verify female for mens

        //verify location set
    }

    private void mensesTest()
    {
        //verify is female
    }

    private void necropsyTest()
    {
        //verify caseno is unique

    }

    private void pregnanciesTest()
    {
        //verify is female
    }

    private void prenatalDeathsTest()
    {
        //TODO
    }

    private void problemListTest()
    {
        //verify problem # incremented
        //verify remote date from time / enddate
    }

    private void surgeryTest()
    {
        //set age
    }

    private void tbTestsTest()
    {
        //setting of missing results
    }

    private void treatmentOrdersTest()
    {
        //math around amount
    }

    private void cageObservationsTest()
    {
        //test cascade insert
        //setting of no_observations

    }

    private void cageTest()
    {
        //verify padding of digits
    }





    private void testValidationMessage(String schemaName, String queryName, String[] fields, Object[][] data, Map<String, List<String>> expectedErrors)
    {
        expectedErrors.put("_validateOnly", Collections.singletonList("ERROR: Ignore this error"));
        try
        {
            log("Testing validation for table: " + schemaName + "." + queryName);

            JSONObject extraContext = new JSONObject();
            extraContext.put("errorThreshold", "INFO");
            extraContext.put("skipIdFormatCheck", true);
            extraContext.put("allowAnyId", true);
            extraContext.put("validateOnly", true); //a flag to force failure
            extraContext.put("targetQC", EHRQCState.IN_PROGRESS.label);

            JSONObject insertCommand = prepareInsertCommand(schemaName, queryName, FIELD_LSID, fields, data);
            String response = doSaveRows(DATA_ADMIN, Collections.singletonList(insertCommand), extraContext, false);
            Map<String, List<String>> errors = processResponse(response);

            //JSONHelper.compareMap()
            Assert.assertEquals("Incorrect number of fields have errors", expectedErrors.keySet().size(), errors.keySet().size());
            for (String field : expectedErrors.keySet())
            {
                Assert.assertEquals("No errors found for field: " + field, true, errors.containsKey(field));
                List<String> expectedErrs = expectedErrors.get(field);
                List<String> errs = errors.get(field);

                log("Expected " + expectedErrs.size() + " errors for field " + field);
                Assert.assertEquals("Wrong number of errors found for field " + field + ": ", expectedErrs.size(), errs.size());
                for (String e : expectedErrs)
                {
                    boolean success = errs.remove(e);
                    Assert.assertTrue("Error not found for field: " + field + ".  Missing error is: " + e, success);
                }
                Assert.assertEquals("Unexpected error found for field: " + field + ".  They are: " + StringUtils.join(errs, "; "), 0, errs.size());
            }

        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void testUserAgainstAllStates(EHRUser user) throws Exception
    {
        JSONObject extraContext = new JSONObject();
        extraContext.put("errorThreshold", "ERROR");
        extraContext.put("skipIdFormatCheck", true);
        extraContext.put("allowAnyId", true);
        String response;

        //maintain list of insert/update times for interest
        _saveRowsTimes = new ArrayList<Long>();

        //test insert
        Object[][] insertData = {weightData1};
        JSONObject insertCommand = prepareInsertCommand("study", "Weight", FIELD_LSID, weightFields, insertData);

        for (EHRQCState qc : EHRQCState.values())
        {
            extraContext.put("targetQC", qc.label);
            boolean successExpected = successExpected(user.getRole(), qc, "insert");
            log("Testing role: " + user.getRole().name() + " with insert of QCState: " + qc.label);
            doSaveRows(user, Collections.singletonList(insertCommand), extraContext, successExpected);
        }
        calculateAverage();

        //then update.  update is fun b/c we need to test many QCState combinations.  Updating a row from 1 QCstate to a new QCState technically
        //requires update permission on the original QCState, plus insert permission into the new QCState
        for (EHRQCState originalQc : EHRQCState.values())
        {
            // first create an initial row as a data admin
            UUID objectId = UUID.randomUUID();
            Object[][] originalData = {weightData1};
            originalData[0][Arrays.asList(weightFields).indexOf(FIELD_QCSTATELABEL)] = originalQc.label;
            originalData[0][Arrays.asList(weightFields).indexOf(FIELD_OBJECTID)] = objectId.toString();
            JSONObject initialInsertCommand = prepareInsertCommand("study", "Weight", FIELD_LSID, weightFields, originalData);
            log("Inserting initial record for update test, with initial QCState of: " + originalQc.label);
            response = doSaveRows(DATA_ADMIN, Collections.singletonList(initialInsertCommand), extraContext, true);

            String lsid = getLsidFromResponse(response);
            originalData[0][Arrays.asList(weightFields).indexOf(FIELD_LSID)] = lsid.toString();

            //then try to update to all other QCStates
            for (EHRQCState qc : EHRQCState.values())
            {
                boolean successExpected = originalQc.equals(qc) ? successExpected(user.getRole(), originalQc, "update") : successExpected(user.getRole(), originalQc, "update") && successExpected(user.getRole(), qc, "insert");
                log("Testing role: " + user.getRole().name() + " with update from QCState " + originalQc.label + " to: " + qc.label);
                originalData[0][Arrays.asList(weightFields).indexOf(FIELD_QCSTATELABEL)] = qc.label;
                JSONObject updateCommand = prepareUpdateCommand("study", "Weight", FIELD_LSID, weightFields, originalData);
                doSaveRows(user, Collections.singletonList(updateCommand), extraContext, successExpected);

                if (successExpected)
                {
                    log("Resetting QCState of record to: " + originalQc.label);
                    originalData[0][Arrays.asList(weightFields).indexOf(FIELD_QCSTATELABEL)] = originalQc.label;
                    updateCommand = prepareUpdateCommand("study", "Weight", FIELD_LSID, weightFields, originalData);
                    doSaveRows(DATA_ADMIN, Collections.singletonList(updateCommand), extraContext, true);
                }
            }
        }

        //log the average save time
        //TODO: eventually we should set a threshold and assert we dont exceed it
        calculateAverage();
    }

    private String getLsidFromResponse(String response)
    {
        try
        {
            JSONObject o = new JSONObject(response);
            if (o.has("exception"))
            {
                //TODO
                throw new RuntimeException("NYI");
            }
            else if (o.has("result"))
            {
                return o.getJSONArray("result").getJSONObject(0).getJSONArray("rows").getJSONObject(0).getJSONObject("values").getString(FIELD_LSID);
            }
            return null;
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
    }

    private boolean successExpected(EHRRole role, EHRQCState qcState, String permission)
    {
        // Expand to other request types once we start testing them. Insert only for now.
        return allowedActions.contains(new Permission(role, qcState, permission));
    }

    private JSONObject prepareInsertCommand(String schema, String queryName, String pkName, String[] fieldNames, Object[][] rows)
    {
        return prepareCommand("insertWithKeys", schema, queryName, pkName, fieldNames, rows);
    }

    private JSONObject prepareUpdateCommand(String schema, String queryName, String pkName, String[] fieldNames, Object[][] rows)
    {
        return prepareCommand("updateChangingKeys", schema, queryName, pkName, fieldNames, rows);
    }

    private JSONObject prepareCommand(String command, String schema, String queryName, String pkName, String[] fieldNames, Object[][] rows)
    {
        try
        {
            JSONObject resp = new JSONObject();
            resp.put("schemaName", schema);
            resp.put("queryName", queryName);
            resp.put("command", command);
            JSONArray jsonRows = new JSONArray();
            for (Object[] row : rows)
            {
                JSONObject oldKeys = new JSONObject();
                JSONObject values = new JSONObject();

                int position = 0;
                for (String name : fieldNames)
                {
                    Object v = row[position];

                    //allow mechanism to use current time,
                    if (DATE_SUBSTITUTION.equals(v))
                        v = (new Date()).toString();

                    values.put(name, v);
                    if (pkName.equals(name))
                        oldKeys.put(name, v);

                    position++;
                }
                JSONObject ro = new JSONObject();
                ro.put("oldKeys", oldKeys);
                ro.put("values", values);
                jsonRows.put(ro);
            }
            resp.put("rows", jsonRows);

            return resp;
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
    }

    private String doSaveRows(EHRUser user, List<JSONObject> commands, JSONObject extraContext, boolean expectSuccess)
    {
        long start = System.currentTimeMillis();
        PostMethod method = null;
        try
        {
            JSONObject json = new JSONObject();
            json.put("commands", commands);
            json.put("extraContext", extraContext);

            String requestUrl = WebTestHelper.getBaseURL() + "/query/" + CONTAINER_PATH +"/saveRows.view";
            method = new PostMethod(requestUrl);
            method.addRequestHeader("Content-Type", "application/json");
            method.setRequestEntity(new StringRequestEntity(json.toString(), "application/json", "UTF-8"));
            HttpClient client = WebTestHelper.getHttpClient(requestUrl, user.getUser(), PasswordUtil.getPassword());
            int status = client.executeMethod(method);
            long stop = System.currentTimeMillis();
            _saveRowsTimes.add(stop - start);

            log("Expect success: " + expectSuccess + ", actual: " + (HttpStatus.SC_OK == status));

            if (expectSuccess && HttpStatus.SC_OK != status)
            {
                logResponse(method);
                Assert.assertEquals("SaveRows request failed unexpectedly with code: " + status, HttpStatus.SC_OK, status);
            }
            else if (!expectSuccess && HttpStatus.SC_BAD_REQUEST != status)
            {
                logResponse(method);
                Assert.assertEquals("SaveRows request failed unexpectedly with code: " + status, HttpStatus.SC_BAD_REQUEST, status);
            }

            return method.getResponseBodyAsString();
        }
        catch (URIException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            method.releaseConnection();
        }
    }

    private void logResponse(HttpMethod method)
    {
        try
        {
            String response = method.getResponseBodyAsString();
            JSONObject o = new JSONObject(response);
            if (o.has("exception"))
                log("Expection: " + o.getString("exception"));

            Map<String, List<String>> ret = processResponse(method.getResponseBodyAsString());
            for (String field : ret.keySet())
            {
                log("Error in field: " + field);
                for (String err : ret.get(field))
                {
                    log(err);
                }
            }
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Map<String, List<String>> processResponse(String response)
    {
        try
        {
            Map<String, List<String>> ret = new HashMap<String, List<String>>();
            JSONObject o = new JSONObject(response);
            if (o.has("errors"))
            {
                JSONArray errors = o.getJSONArray("errors");
                for (int i = 0; i < errors.length(); i++)
                {
                    JSONObject error = errors.getJSONObject(i);
                    JSONArray subErrors = error.getJSONArray("errors");
                    if (subErrors != null)
                    {
                        for (int j = 0; j < subErrors.length(); j++)
                        {
                            JSONObject subError = subErrors.getJSONObject(j);
                            String msg = subError.getString("message");
                            String field = subError.getString("field");

                            List<String> list = ret.get(field);
                            if (list == null)
                                list = new ArrayList<String>();

                            list.add(msg);
                            ret.put(field, list);
                        }
                    }
                }
            }

            //append errors from extraContext
            if (o.has("extraContext"))
            {
                JSONObject errors = o.getJSONObject("extraContext").getJSONObject("skippedErrors");
                Iterator keys = errors.keys();
                while (keys.hasNext())
                {
                    String key = (String)keys.next();
                    JSONArray errorArray = errors.getJSONArray(key);
                    for (int i=0;i<errorArray.length();i++)
                    {
                        JSONObject subError = errorArray.getJSONObject(i);
                        String msg = subError.getString("message");
                        String field = subError.getString("field");

                        List<String> list = ret.get(field);
                        if (list == null)
                            list = new ArrayList<String>();

                        list.add(msg);
                        ret.put(field, list);
                    }
                }
            }

            return ret;
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected static final ArrayList<Permission> allowedActions = new ArrayList<Permission>()
    {
        {
            // Data Admin - Users with this role are permitted to make any edits to datasets
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.ABNORMAL, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.COMPLETED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.DELETE_REQUESTED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_APPROVED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.SCHEDULED, "insert"));

            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.ABNORMAL, "update"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.COMPLETED, "update"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.DELETE_REQUESTED, "update"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.IN_PROGRESS, "update"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_APPROVED, "update"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_COMPLETE, "update"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_DENIED, "update"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_PENDING, "update"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REVIEW_REQUIRED, "update"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.SCHEDULED, "update"));

            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.ABNORMAL, "delete"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.COMPLETED, "delete"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.DELETE_REQUESTED, "delete"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.IN_PROGRESS, "delete"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_APPROVED, "delete"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_COMPLETE, "delete"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_DENIED, "delete"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REQUEST_PENDING, "delete"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.REVIEW_REQUIRED, "delete"));
            add(new Permission(EHRRole.DATA_ADMIN, EHRQCState.SCHEDULED, "delete"));

            //for the purpose of tests, full updater is essentially the save as data admin.  they just lack admin privs, which we dont really test
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.ABNORMAL, "insert"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.COMPLETED, "insert"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.DELETE_REQUESTED, "insert"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_APPROVED, "insert"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.SCHEDULED, "insert"));

            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.ABNORMAL, "update"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.COMPLETED, "update"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.DELETE_REQUESTED, "update"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.IN_PROGRESS, "update"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_APPROVED, "update"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_COMPLETE, "update"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_DENIED, "update"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_PENDING, "update"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REVIEW_REQUIRED, "update"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.SCHEDULED, "update"));

            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.ABNORMAL, "delete"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.COMPLETED, "delete"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.DELETE_REQUESTED, "delete"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.IN_PROGRESS, "delete"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_APPROVED, "delete"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_COMPLETE, "delete"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_DENIED, "delete"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REQUEST_PENDING, "delete"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.REVIEW_REQUIRED, "delete"));
            add(new Permission(EHRRole.FULL_UPDATER, EHRQCState.SCHEDULED, "delete"));

            // Requester - Users with this role are permitted to submit requests, but not approve them
            add(new Permission(EHRRole.REQUESTER, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.REQUESTER, EHRQCState.REQUEST_PENDING, "update"));
            //add(new Permission(EHRRole.REQUESTER, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.REQUESTER, EHRQCState.REQUEST_DENIED, "update"));

            // Full Submitter - Users with this role are permitted to submit and approve records.  They cannot modify public data.
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.ABNORMAL, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.COMPLETED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.DELETE_REQUESTED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_APPROVED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.SCHEDULED, "insert"));

            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.ABNORMAL, "update"));
            //add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.COMPLETED, "update"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.DELETE_REQUESTED, "update"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.IN_PROGRESS, "update"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_APPROVED, "update"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_COMPLETE, "update"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_DENIED, "update"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_PENDING, "update"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REVIEW_REQUIRED, "update"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.SCHEDULED, "update"));

            //add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.ABNORMAL, "delete"));
            //add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.COMPLETED, "delete"));
            //add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.DELETE_REQUESTED, "delete"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.IN_PROGRESS, "delete"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_APPROVED, "delete"));
            //add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_COMPLETE, "delete"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_DENIED, "delete"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REQUEST_PENDING, "delete"));
            //add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.REVIEW_REQUIRED, "delete"));
            add(new Permission(EHRRole.FULL_SUBMITTER, EHRQCState.SCHEDULED, "delete"));

            // Basic Submitter - Users with this role are permitted to submit and edit non-public records, but cannot alter public ones
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.DELETE_REQUESTED, "insert"));
            //request approved: none
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.SCHEDULED, "insert"));

            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.IN_PROGRESS, "update"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REVIEW_REQUIRED, "update"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_PENDING, "update"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.DELETE_REQUESTED, "update"));
            //request approved: none
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_DENIED, "update"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.REQUEST_COMPLETE, "update"));
            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.SCHEDULED, "update"));

            add(new Permission(EHRRole.BASIC_SUBMITTER, EHRQCState.IN_PROGRESS, "delete"));

            // Request Admin is basically the same as Full Submitter, except they also have RequestAdmin permission, which is not currently tested.  It is primarily used in UI
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.ABNORMAL, "insert"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.COMPLETED, "insert"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.DELETE_REQUESTED, "insert"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.IN_PROGRESS, "insert"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_APPROVED, "insert"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_COMPLETE, "insert"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_DENIED, "insert"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_PENDING, "insert"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REVIEW_REQUIRED, "insert"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.SCHEDULED, "insert"));

            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.ABNORMAL, "update"));
            //add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.COMPLETED, "update"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.DELETE_REQUESTED, "update"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.IN_PROGRESS, "update"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_APPROVED, "update"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_COMPLETE, "update"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_DENIED, "update"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_PENDING, "update"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REVIEW_REQUIRED, "update"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.SCHEDULED, "update"));

            //add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.ABNORMAL, "delete"));
            //add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.COMPLETED, "delete"));
            //add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.DELETE_REQUESTED, "delete"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.IN_PROGRESS, "delete"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_APPROVED, "delete"));
            //add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_COMPLETE, "delete"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_DENIED, "delete"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REQUEST_PENDING, "delete"));
            //add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.REVIEW_REQUIRED, "delete"));
            add(new Permission(EHRRole.REQUEST_ADMIN, EHRQCState.SCHEDULED, "delete"));
        }
    };

    private void calculateAverage()
    {
        if (_saveRowsTimes.size() == 0)
            return;

        long sum = 0;
        for(int i=0; i < _saveRowsTimes.size(); i++){
            sum = sum + _saveRowsTimes.get(i);
        }

        //calculate average of all elements
        long average = sum / _saveRowsTimes.size();
        average = average / 1000;
        log("The average save time per record was : " + average + " seconds");

        _saveRowsTimes = new ArrayList<Long>();
    }
}
