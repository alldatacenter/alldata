/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.common.enums;

import static java.util.Objects.requireNonNull;

public enum ManagerOpEnum {
    ADD(0), DEL(1), RETRY(2), BACKTRACK(3), FROZEN(4),
    ACTIVE(5), CHECK(6), REDOMETRIC(7), MAKEUP(8);

    private int type;

    ManagerOpEnum(int type) {
        this.type = type;
    }

    public static ManagerOpEnum getOpType(int opType) {
        requireNonNull(opType);
        switch (opType) {
            case 0:
                return ADD;
            case 1:
                return DEL;
            case 2:
                return RETRY;
            case 3:
                return BACKTRACK;
            case 4:
                return FROZEN;
            case 5:
                return ACTIVE;
            case 6:
                return CHECK;
            case 7:
                return REDOMETRIC;
            case 8:
                return MAKEUP;
            default:
                throw new RuntimeException("such op type doesn't exist");
        }
    }

    public int getType() {
        return type;
    }
}
