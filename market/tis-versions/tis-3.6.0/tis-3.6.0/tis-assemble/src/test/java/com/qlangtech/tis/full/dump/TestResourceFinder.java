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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年12月28日 下午5:44:27
 */
public class TestResourceFinder extends TestCase {

    private static final Pattern CONFIG_RES_PATTERN = Pattern.compile("/(search4.+?)/");

    public void testFind() throws Exception {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        Resource[] res = resourcePatternResolver.getResources("classpath*:com.qlangtech.tis/assemble/search4*/join.xml");
        System.out.println("res.length:" + res.length);
        Matcher matcher = null;
        for (Resource r : res) {
            matcher = CONFIG_RES_PATTERN.matcher(r.getURI().getPath());
            if (matcher.find()) {
                System.out.println(matcher.group(1));
            }
        // System.out.println(r.getURI());
        // System.out.println(r.getURL());
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
    }
}
