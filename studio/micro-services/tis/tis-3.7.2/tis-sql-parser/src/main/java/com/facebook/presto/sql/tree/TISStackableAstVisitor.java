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
package com.facebook.presto.sql.tree;

import java.util.LinkedList;
import java.util.Optional;
import com.facebook.presto.sql.tree.AstVisitor;
import com.facebook.presto.sql.tree.Node;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TISStackableAstVisitor<R, C> extends AstVisitor<R, TISStackableAstVisitor.StackableAstVisitorContext<C>> {

    public R process(Node node, StackableAstVisitorContext<C> context) {
        context.push(node);
        try {
            // context.getStackDeepth());
            return node.accept(this, context);
        } finally {
            context.pop();
        }
    }

    public static class StackableAstVisitorContext<C> {

        private final LinkedList<Node> stack = new LinkedList<>();

        private final C context;

        private int stackDeepth;

        public boolean processSelect = false;

        public StackableAstVisitorContext(C context) {
            this.context = context;
        }

        public C getContext() {
            return context;
        }

        private void pop() {
            stackDeepth--;
            stack.pop();
        }

        public int getStackDeepth() {
            return stackDeepth;
        }

        void push(Node node) {
            stackDeepth++;
            stack.push(node);
        }

        public Optional<Node> getPreviousNode() {
            if (stack.size() > 1) {
                return Optional.of(stack.get(1));
            }
            return Optional.empty();
        }
    }

    public static void main(String[] args) {
        LinkedList<Integer> stack = new LinkedList<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        for (int i = 0; i < 3; i++) {
            System.out.println(stack.get(i));
        }
        System.out.println(stack.poll());
        System.out.println(stack.poll());
        System.out.println(stack.poll());
    }
}
