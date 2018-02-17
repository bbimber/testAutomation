/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

import junit.framework.TestSuite;

import java.util.Map;

/**
 * Created by matthew on 9/11/15.
 */

//@Category({DRT.class, Base.class})
public class JUnitDRTTest
{
    public static boolean accept(Map<String, Object> test)
    {
        return JUnitTest.getCategories(test).contains("DRT");
    }

    public static TestSuite suite() throws Exception
    {
        return JUnitTest._suite(JUnitDRTTest::accept);
    }
}

