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
package com.qlangtech.tis.extension;

/**
 * Filters out {@link ExtensionComponent}s discovered by {@link ExtensionFinder}s,
 * as if they were never discovered.
 * <p>
 * This is useful for those who are deploying restricted/simplified version of Jenkins
 * by reducing the functionality.
 * <p>
 * Because of the way {@link ExtensionFinder} works, even when an extension component
 * is rejected by a filter, its instance still gets created first.
 * @see ExtensionComponentSet#filtered()
 * @since 1.472
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class ExtensionFilter {

    /**
     * Checks if a newly discovered extension is allowed to participate into Jenkins.
     *
     * <p>
     * To filter {@link Descriptor}s based on the {@link Describable} subtypes, do as follows:
     *
     * <pre>
     * return !component.isDescriptorOf(Builder.class);
     * </pre>
     *
     * @param type The type of the extension that we are discovering. This is not the actual instance
     *             type, but the contract type, such as {@link Descriptor}, , etc.
     * @return true to let the component into Jenkins. false to drop it and pretend
     * as if it didn't exist. When any one of {@link ExtensionFilter}s veto
     * a component, it gets dropped.
     */
    public abstract <T> boolean allows(Class<T> type, ExtensionComponent<T> component);

    public static <T> boolean isAllowed(Class<T> type, ExtensionComponent<T> component) {
        // return true;
        return true;
    }
    /**
     * All registered {@link ExtensionFilter} instances.
     */
    // public static ExtensionList<ExtensionFilter> all() {
    // return ExtensionList.lookup(ExtensionFilter.class);
    // }
}
