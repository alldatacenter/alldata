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
 * {@link ExtensionList} listener.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 * @since 1.614
 */
public abstract class ExtensionListListener {

    /**
     * {@link ExtensionList} contents has changed.
     * <p>
     * This would be called when an entry gets added to or removed from the list for any reason e.g.
     * when a dynamically loaded plugin introduces a new {@link 'ExtensionPoint'} implementation
     * that adds an entry to the {@link ExtensionList} being listened to.
     */
    public abstract void onChange();
}

