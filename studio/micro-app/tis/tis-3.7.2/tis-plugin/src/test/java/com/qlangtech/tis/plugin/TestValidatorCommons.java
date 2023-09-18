/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin;

import com.qlangtech.tis.common.utils.Assert;
import junit.framework.TestCase;

import java.util.regex.Matcher;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-01 13:16
 **/
public class TestValidatorCommons extends TestCase implements ValidatorCommons {

    public void testUserName() {

//        Validator userNameValidator = Validator.user_name;
//        userNameValidator.validate()

        String userName = "BASIC$datav";
        Matcher matcher = ValidatorCommons.pattern_user_name.matcher(userName);
        Assert.assertTrue("userName shall be valid:" + userName, matcher.matches());

        userName = "BAS-IC_datav";
         matcher = ValidatorCommons.pattern_user_name.matcher(userName);
        Assert.assertTrue("userName shall be valid:" + userName, matcher.matches());

        userName = "BASIC$datav*";
        matcher = ValidatorCommons.pattern_user_name.matcher(userName);
        Assert.assertFalse("userName shall not be valid:" + userName, matcher.matches());
    }

    public void testPATTERN_ABSOLUTE_PATH() {
        Matcher matcher = PATTERN_ABSOLUTE_PATH.matcher("/home/hanfa.shf/ftpReaderTest/data");
        assertTrue("must match", matcher.matches());

        matcher = PATTERN_ABSOLUTE_PATH.matcher("/home/hanfa.shf/ftpReaderTest/*");
        assertTrue("must match", matcher.matches());

        matcher = PATTERN_ABSOLUTE_PATH.matcher("/home/hanfa.shf/ftpReaderTest/prefix*");
        assertTrue("must match", matcher.matches());

        matcher = PATTERN_ABSOLUTE_PATH.matcher("/prefix*");
        assertTrue("must match", matcher.matches());

        matcher = PATTERN_ABSOLUTE_PATH.matcher("/prefix");
        assertTrue("must match", matcher.matches());

    }

    public void testDBColName() {
        Matcher matcher = ValidatorCommons.PATTERN_DB_COL_NAME.matcher("0DbName1");
        assertTrue(matcher.matches());

        matcher = ValidatorCommons.PATTERN_DB_COL_NAME.matcher("_0DbName1");
        assertTrue(matcher.matches());

        matcher = ValidatorCommons.PATTERN_DB_COL_NAME.matcher(",_0DbName1");
        assertFalse(matcher.matches());

        matcher = ValidatorCommons.PATTERN_DB_COL_NAME.matcher("ORDER_DB");
        assertTrue(matcher.matches());
    }

    public void testPattern_identity() {
        Matcher matcher = pattern_identity.matcher("0DbName1");
        assertTrue(matcher.matches());

        matcher = pattern_identity.matcher("0DbNa-me1_");
        assertTrue(matcher.matches());

        matcher = pattern_identity.matcher("0Db@Na-me1");
        assertFalse(matcher.matches());
    }

    public void testNoneBlank() {
        Matcher matcher = PATTERN_NONE_BLANK.matcher("0DbName1");
        assertTrue(matcher.matches());

        matcher = PATTERN_NONE_BLANK.matcher("0DbN ame1");
        assertFalse(matcher.matches());

        matcher = PATTERN_NONE_BLANK.matcher(" 0DbName1");
        assertFalse(matcher.matches());
    }
}
