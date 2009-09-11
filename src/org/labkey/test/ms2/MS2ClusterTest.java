/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
package org.labkey.test.ms2;

import org.apache.commons.io.FileUtils;
import org.labkey.test.pipeline.PipelineWebTestBase;
import org.labkey.test.pipeline.PipelineFolder;
import org.labkey.test.pipeline.PipelineTestParams;
import org.labkey.test.ms2.params.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

/**
 * @author brendanx
 */
public class MS2ClusterTest extends PipelineWebTestBase
{
    protected MS2TestsBase testSet = new MS2Tests_20070701__3_4_1(this);

    public static final String PROTOCOL_MODIFIER = "";
    public static final String PROTOCOL_MODIFIER_SEARCH = "";

    private static boolean CLEAN_DATA = true;
    private static boolean NEW_DATA = true;
    private static boolean NEW_SEARCH = true;
    private static boolean REMOVE_DATA = true;
    private static boolean USE_GLOBUS = true;

    protected static final int MAX_WAIT_SECONDS = 60*60*5;

    public MS2ClusterTest()
    {
        super("MS2ClusterProject");

        MS2PipelineFolder folder = new MS2PipelineFolder(this,
                "Pipeline",
                "T:/edi/pipeline/Test/regression",
                PipelineFolder.Type.enterprise);
        folder.setFastaPath("T:/data/databases");

        testSet.setFolder(folder);
        testSet.addTestsScoringMix();
        testSet.addTestsQuant();
//        testSet.addTestsScoringOrganisms();
//        testSet.addTestsISBMix();
//        testSet.addTestsIPAS();
    }

    // Return the directory of the module whose functionality this class tests, or "none" if multiple/all modules are tested
    public String getAssociatedModuleDirectory()
    {
        return "ms2";
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return USE_GLOBUS;
    }

    protected void doCleanup() throws Exception
    {
        if (CLEAN_DATA)
        {
            testSet.clean();
            super.doCleanup();
        }
    }

    protected void doTestSteps()
    {
        if (CLEAN_DATA)
        {
            testSet.verifyClean();

            testSet.setup();
        }
        else
        {
            testSet.beginAt();
        }

        if (NEW_DATA)
        {
            doAnalysis();
        }

        int seconds = 0;
        List<PipelineTestParams> listValidated = new ArrayList<PipelineTestParams>();
        while (seconds++ < MAX_WAIT_SECONDS)
        {
            PipelineTestParams[] completeParams = testSet.getCompleteParams();
            for (PipelineTestParams tp : completeParams)
            {
                pushLocation();
                tp.validate();
                popLocation();

                if (tp.isValid() && REMOVE_DATA)
                    tp.remove();

                testSet.removeParams(tp);
                listValidated.add(tp);
            }

            if (!testSet.hasParams())
                break;

            // If nothing was validated, wait for a minute.
            if (completeParams.length == 0)
            {
                log("Waiting to validate completed searches");
                sleep(60*1000);
            }
            refresh();
        }

        // Count uses case sensitive match.
        assertLinkPresentWithTextCount("ERROR", 0);

        for (PipelineTestParams tp : listValidated)
        {
            assertTrue("Tests failed.  Consult the log.", tp.isValid());
        }
    }

    protected void doAnalysis()
    {
        HashSet<String> searches = new HashSet<String>();

        for (PipelineTestParams tp : testSet.getParams())
        {
            String searchKey = tp.getRunKey();
            if (searches.contains(searchKey))
                continue;
            searches.add(searchKey);

            tp.startProcessing();

            if (!NEW_SEARCH)
            {
                File dirRoot = new File(testSet.getFolder().getPipelinePath());
                String analysisPath = tp.getDataPath() + File.separator +
                        "xtandem" + File.separator + tp.getProtocolName();
                File dirDest = new File(dirRoot,  analysisPath);

                // Strip modifier from the name.
                analysisPath = analysisPath.substring(0,
                        analysisPath.length() - PROTOCOL_MODIFIER.length());
                File dirSrc = new File(dirRoot,  analysisPath + PROTOCOL_MODIFIER_SEARCH);

                File[] tandemFiles = dirSrc.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith(".xtan.xml");
                    }
                });

                for (File fileTandem : tandemFiles)
                {
                    try
                    {
                        FileUtils.copyFileToDirectory(fileTandem, dirDest);
                    }
                    catch (IOException e)
                    {
                        assertTrue("Failed to copy search results '" + fileTandem + "'.", false);
                    }
                }
            }
        }
    }
}
