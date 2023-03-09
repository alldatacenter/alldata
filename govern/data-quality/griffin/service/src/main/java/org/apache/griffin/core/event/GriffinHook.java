/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.event;

import org.apache.griffin.core.exception.GriffinException;

/**
 * The Hook interface for receiving internal events.
 * The class that is interested in processing an event
 * implements this interface, and the object created with that
 * class is registered to griffin, using the configuration.
 * When the event occurs, that object's <code>onEvent</code> method is
 * invoked.
 *
 * @author Eugene Liu
 * @since 0.3
 */
public interface GriffinHook {
    /**
     * Invoked when an action occurs.
     *
     * @see GriffinEvent
     */
    void onEvent(GriffinEvent event) throws GriffinException;
}
