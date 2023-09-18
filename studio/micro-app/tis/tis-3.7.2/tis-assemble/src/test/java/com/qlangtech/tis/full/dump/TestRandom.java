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
package com.qlangtech.tis.full.dump;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月21日
 */
public class TestRandom extends TestCase {

    /**
     * @param args
     */
    public void test() {
        List<String> allPerson = new ArrayList<String>();
        allPerson.add("何XX");
        allPerson.add("李XX");
        allPerson.add("屈XX");
        allPerson.add("刘XX");
        allPerson.add("王XX");
        allPerson.add("涂XX");
        allPerson.add("张XX");
        allPerson.add("方XX");
        allPerson.add("何XX");
        allPerson.add("周XX");
        List<String> aGroup = new ArrayList<String>();
        List<String> bGroup = new ArrayList<String>();
        Collections.shuffle(allPerson);
        vist(allPerson.iterator(), aGroup, bGroup);
        System.out.println(aGroup);
        System.out.println(bGroup);
    }

    private void vist(Iterator<String> it, List<String> aGroup, List<String> bGroup) {
        if (!it.hasNext()) {
            return;
        }
        aGroup.add(it.next());
        vist(it, bGroup, aGroup);
    }
}
