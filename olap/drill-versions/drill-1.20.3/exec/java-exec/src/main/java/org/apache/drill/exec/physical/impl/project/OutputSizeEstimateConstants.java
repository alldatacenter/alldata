/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.physical.impl.project;

public class OutputSizeEstimateConstants {
    public static final int USER_NAME_LENGTH = 32; //libc useradd limit
    public static final int SCHEMA_LENGTH = 1024;
    public static final int USER_ID_LENGTH = 32;   //UUID length
    public static final int DATE_TIME_LENGTH = 100; //DateTypeFunctions timeofday truncates to 100
    public static final int CONVERT_TO_FLOAT_LENGTH = 4; //float 4 to varbinary
    public static final int CONVERT_TO_TINYINT_LENGTH = 1; // tiny int to varbinary
    public static final int CONVERT_TO_INT_LENGTH = 4; // INT to BigEndian Int
    public static final int CONVERT_TO_BIGINT_LENGTH = 8; // convert to BigEndianBigInt/BigInt
    public static final int CONVERT_TO_UINT4_LENGTH = 4; // convert_toUINT4
    public static final int CONVERT_TO_SMALLINT_LENGTH = 2; // convert_toSMALLINT_BE
    public static final int CONVERT_TO_TIME_EPOCH_LENGTH = 8; // convert_toTIME_EPOCH_BE
    public static final int CONVERT_TO_DOUBLE_LENGTH = 8; // convert_to_double_be
    public static final int CONVERT_TO_BOOLEAN_BYTE_LENGTH = 1; // tiny int to varbinary
    public static final int CONVERT_TO_DATE_EPOCH_LENGTH = 8; // tiny int to varbinary
    public static final int CONVERT_TO_TIMESTAMP_EPOCH_LENGTH = 8; // tiny int to varbinary
    public static final int CONVERT_TO_HADOOPV_LENGTH = 9; // Hadoop Variable length integer. 1 - 9 bytes
    public static final int CONVERT_TO_UINT8_LENGTH = 8; // uint8 length

    public static final int CHAR_LENGTH = 1;

    //TODO Make this a user config?
    public static final int NON_DRILL_FUNCTION_OUTPUT_SIZE_ESTIMATE = 50;

    //TODO Make this a user config?
    public static final int COMPLEX_FIELD_ESTIMATE = 50;
}
