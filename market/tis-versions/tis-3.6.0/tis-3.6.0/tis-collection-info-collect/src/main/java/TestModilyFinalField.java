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

import java.lang.reflect.Field;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestModilyFinalField {

    private final String testFiled = "2";

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        TestModilyFinalField finalField = new TestModilyFinalField();
        Field field = finalField.getClass().getDeclaredField("testFiled");
        field.setAccessible(true);
        field.set(finalField, "3");
        System.out.println(finalField.testFiled);
    // String a = "abc";
    // Field f = a.getClass().getDeclaredField("value");
    // f.setAccessible(true);
    // char[] ch = new char[3];
    // ch[0] = 'b';
    // ch[1] = 'c';
    // ch[2] = 'd';
    // f.set(a, ch);
    // System.out.println(a);
    }
}
