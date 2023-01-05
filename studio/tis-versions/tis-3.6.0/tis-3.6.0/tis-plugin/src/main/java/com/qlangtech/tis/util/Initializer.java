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

package com.qlangtech.tis.util;

import com.qlangtech.tis.extension.init.InitMilestone;
import org.jvnet.hudson.reactor.Task;

import static com.qlangtech.tis.extension.init.InitMilestone.*;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-11 10:13
 **/
public @interface Initializer {
    /**
     * Indicates that the specified milestone is necessary before executing this initializer.
     *
     * <p>
     * This has the identical purpose as {@link #requires()}, but it's separated to allow better type-safety
     * when using {@link InitMilestone} as a requirement (since enum member definitions need to be constant).
     */
    InitMilestone after() default STARTED;

    /**
     * Indicates that this initializer is a necessary step before achieving the specified milestone.
     *
     * <p>
     * This has the identical purpose as {@link #attains()}. See {@link #after()} for why there are two things
     * to achieve the same goal.
     */
    InitMilestone before() default COMPLETED;

    /**
     * Indicates the milestones necessary before executing this initializer.
     */
    String[] requires() default {};

    /**
     * Indicates the milestones that this initializer contributes to.
     *
     * A milestone is considered attained if all the initializers that attains the given milestone
     * completes. So it works as a kind of join.
     */
    String[] attains() default {};

    /**
     * Key in {@code Messages.properties} that represents what this task is about. Used for rendering the progress.
     * Defaults to "${short class name}.${method Name}".
     */
    String displayName() default "";

    /**
     * Should the failure in this task prevent Hudson from starting up?
     *
     * @see Task#failureIsFatal()
     */
    boolean fatal() default true;
}
