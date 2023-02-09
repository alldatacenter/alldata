/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.provider;

public final class TestConfig {

    // Available region of RAM
    public static String RAM_REGION_ID = null;

    // Root user's AK/SK
    public static String ROOT_ACCESS_KEY_ID = null;
    public static String ROOT_ACCESS_KEY_SECRET = null;

    // Child user's AK/SK
    public static String USER_ACCESS_KEY_ID = null;
    public static String USER_ACCESS_KEY_SECRET = null;

    // Role's ARN
    public static String RAM_ROLE_ARN = null;

    // ECS bound role name, note NOT ARN
    public static String ECS_ROLE_NAME = null;

    // The RSA key pair
    public static String PUBLIC_KEY_PATH = null;
    public static String PRIVATE_KEY_PATH = null;

    // The host authentication server
    public static String OSS_AUTH_SERVER_HOST = null;

    // OSS test configuration
    public static String OSS_ENDPOINT = null;
    public static String OSS_BUCKET = null;

}
