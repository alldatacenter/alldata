/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.shai.xmodifier;

import org.junit.Assert;
import org.junit.Test;
import org.shai.xmodifier.util.Cons;
import org.shai.xmodifier.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Shenghai on 14-11-28.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class StringUtilTest {

    @Test
    public void splitBySeparator() {
        String s = "ns:root//ns:element1/ns:element11";
        String[] strings = StringUtils.splitBySeparator(s, new String[] { "/", "//" }, new char[][] { { '\'', '\'' }, { '[', ']' }, { '(', ')' } }, true);
        Assert.assertEquals("[ns:root, //ns:element1, /ns:element11]", Arrays.toString(strings));
    }

    @Test
    public void splitBySeparator2() {
        String s = "@attr=1";
        String[] strings = StringUtils.splitBySeparator(s, new String[] { "/", "//" }, new char[][] { { '\'', '\'' }, { '[', ']' }, { '(', ')' } }, true);
        Assert.assertEquals("[@attr=1]", Arrays.toString(strings));
    }

    @Test
    public void findQuotingString() {
        String s = "aad(:xxx(yy[xxx)yy))eee";
        List<Cons<String, String>> escapeList = new ArrayList<Cons<String, String>>();
        escapeList.add(new Cons<String, String>("(", ")"));
        escapeList.add(new Cons<String, String>("[", ")"));
        Cons<String, String> result = StringUtils.findFirstQuotingString(s, new Cons<String, String>("(:", ")"), escapeList);
        System.out.println("result = " + result);
    }

    @Test
    public void removeQuotingString() {
        String s = "adfd(:lkjkl(kjlkj))lkjflkds(:lkfjlksdj(ldkj))lkjfdslj";
        List<Cons<String, String>> escapeList = new ArrayList<Cons<String, String>>();
        escapeList.add(new Cons<String, String>("(", ")"));
        escapeList.add(new Cons<String, String>("[", ")"));
        System.out.println(StringUtils.removeQuotingString(s, new Cons<String, String>("(:", ")"), escapeList));
    }
}
