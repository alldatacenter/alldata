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

package org.apache.inlong.manager.pojo.user;

/**
 * User role code.
 *
 * The permission control model of inlong consists of tenant permission control and the whole permission control.
 *
 * Assume that there are several resources belongs to tenant A, B and C.
 * --------------------------------------------------------------
 * |  Tenant   |      A     |     B      |     C    |   inlong |
 * -------------------------------------------------------------
 * | Resources | r1, r2, r3 | r4, r5, r6 |  r7, r8  |          |
 * -------------------------------------------------------------
 * |   Admin  |  Alice, Bob |  Alice,Eve |  Charlie  |   Dave  |
 * -------------------------------------------------------------
 * | Operator |  Charlie   |    Bob     |  Alice,Bob |   Eve   |
 *--------------------------------------------------------------
 * As the table shown above,
 *
 * Alice has all permission of tenant A and B, and she can do all save/update/delete/get operation to resources
 * r1-r3 in tenant A and r4-r6 in tenant B. Alice also the operator of tenant C, hence she can look up r7 r8, but
 * cannot do any modification.
 *
 * Bob has all permission to r1-r3, and can look up r4-r6
 *
 * Charlie has all permission to r7 r8, and can look up r1-r3.
 *
 * Dave is the admin of inlong, hence he can operate all resources r1-r8.
 *
 * Eve is the operator of inlong, hence she can look up all resources r1-r8,
 * she also the admin of tenant B, hence she has all permission of r4-r6.
 *
 *
 */
public class UserRoleCode {

    /**
     * Has all permission for the resources of a specific tenant
     */
    public static final String TENANT_ADMIN = "TENANT_ADMIN";
    /**
     * Has query permission for the resources of a specific tenant
     */
    public static final String TENANT_OPERATOR = "TENANT_OPERATOR";

    /**
     * Has all permission for all resources
     */
    public static final String INLONG_ADMIN = "INLONG_ADMIN";

    /**
     * Has query permission for all resources
     */
    public static final String INLONG_OPERATOR = "INLONG_OPERATOR";

    /**
     * The requests from Inlong Service do not need to filter by tenant
     */
    public static final String INLONG_SERVICE = "INLONG_SERVICE";

}
