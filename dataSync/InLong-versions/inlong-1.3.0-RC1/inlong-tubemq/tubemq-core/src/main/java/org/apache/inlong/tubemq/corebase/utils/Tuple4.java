/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.utils;

public class Tuple4<T0, T1, T2, T3> {

    /** Field 0 of the tuple. */
    private T0 f0 = null;
    /** Field 1 of the tuple. */
    private T1 f1 = null;
    /** Field 2 of the tuple. */
    private T2 f2 = null;
    /** Field 3 of the tuple. */
    private T3 f3 = null;

    /**
     * Creates a new tuple where all fields are null.
     */
    public Tuple4() {

    }

    /**
     * Creates a new tuple and assigns the given values to the tuple's fields.
     *
     * @param value0 The value for field 0
     * @param value1 The value for field 1
     * @param value2 The value for field 2
     * @param value3 The value for field 3
     */
    public Tuple4(T0 value0, T1 value1, T2 value2, T3 value3) {
        setFieldsValue(value0, value1, value2, value3);
    }

    public T0 getF0() {
        return f0;
    }

    public T1 getF1() {
        return f1;
    }

    public T2 getF2() {
        return f2;
    }

    public T3 getF3() {
        return f3;
    }

    public void setFieldsValue(T0 value0, T1 value1, T2 value2, T3 value3) {
        this.f0 = value0;
        this.f1 = value1;
        this.f2 = value2;
        this.f3 = value3;
    }

}
