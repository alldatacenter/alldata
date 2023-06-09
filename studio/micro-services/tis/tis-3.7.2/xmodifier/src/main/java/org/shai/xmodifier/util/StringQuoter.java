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
package org.shai.xmodifier.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by Shenghai on 2014/11/29.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class StringQuoter {

    private List<Cons<String, String>> quoterList = new ArrayList<Cons<String, String>>();

    private Stack<Cons<String, String>> stack = new Stack<Cons<String, String>>();

    public int check(String str) {
        if (isQuoting()) {
            Cons<String, String> peek = stack.peek();
            if (str.startsWith(peek.getRight())) {
                Cons<String, String> pop = stack.pop();
                return pop.getRight().length();
            }
        }
        if (quoterList != null) {
            for (Cons<String, String> quoter : quoterList) {
                if (str.startsWith(quoter.getLeft())) {
                    stack.push(quoter);
                    return quoter.getLeft().length();
                }
            }
        }
        return 0;
    }

    public boolean isQuoting() {
        return !stack.empty();
    }

    public void addQuoter(Cons<String, String> quoter) {
        quoterList.add(quoter);
    }

    public void addAllQuoters(List<Cons<String, String>> quoterList) {
        if (quoterList == null) {
            return;
        }
        this.quoterList.addAll(quoterList);
    }
}
