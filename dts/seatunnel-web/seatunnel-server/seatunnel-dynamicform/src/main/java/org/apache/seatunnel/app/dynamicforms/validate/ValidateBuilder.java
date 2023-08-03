/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.dynamicforms.validate;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ValidateBuilder {

    public static ValidateBuilder builder() {
        return new ValidateBuilder();
    }

    public NonEmptyValidateBuilder nonEmptyValidateBuilder() {
        return new NonEmptyValidateBuilder();
    }

    public UnionNonEmptyValidateBuilder unionNonEmptyValidateBuilder() {
        return new UnionNonEmptyValidateBuilder();
    }

    public MutuallyExclusiveValidateBuilder mutuallyExclusiveValidateBuilder() {
        return new MutuallyExclusiveValidateBuilder();
    }

    public static class NonEmptyValidateBuilder {
        public NonEmptyValidate nonEmptyValidate() {
            return new NonEmptyValidate();
        }
    }

    public static class UnionNonEmptyValidateBuilder {
        private List<String> fields = new ArrayList<>();

        public UnionNonEmptyValidateBuilder fields(@NonNull String... fields) {
            for (String field : fields) {
                this.fields.add(field);
            }
            return this;
        }

        public UnionNonEmptyValidate unionNonEmptyValidate() {
            return new UnionNonEmptyValidate(fields);
        }
    }

    public static class MutuallyExclusiveValidateBuilder {
        private List<String> fields = new ArrayList<>();

        public MutuallyExclusiveValidateBuilder fields(@NonNull String... fields) {
            for (String field : fields) {
                this.fields.add(field);
            }
            return this;
        }

        public MutuallyExclusiveValidate mutuallyExclusiveValidate() {
            return new MutuallyExclusiveValidate(fields);
        }
    }
}
