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

package org.apache.inlong.sort.parser.result;

/**
 * The result of parse, it is the top-level interface abstraction of parse result
 */
public interface ParseResult {

    /**
     * Execute the parse result without waiting
     *
     * @throws Exception The exception may throws when executing
     */
    void execute() throws Exception;

    /**
     * Execute the parse result and wait result unit data is ready
     *
     * @throws Exception The exception may throws when executing
     */
    void waitExecute() throws Exception;

    /**
     * Try to execute, it mostly for unit test and syntax error checking
     *
     * @return true if try execute success else false
     * @throws Exception The exception may throws when executing
     */
    boolean tryExecute() throws Exception;

}
