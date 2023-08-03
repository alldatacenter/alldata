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

package org.apache.inlong.dataproxy.source.v0msg;

public class MsgFieldConsts {

    public static final int BIN_MSG_FORMAT_SIZE = 29;
    public static final int BIN_MSG_TOTALLEN_OFFSET = 0;
    public static final int BIN_MSG_TOTALLEN_SIZE = 4;
    public static final int BIN_MSG_FIXED_CONTENT_SIZE = BIN_MSG_FORMAT_SIZE - BIN_MSG_TOTALLEN_SIZE;
    public static final int BIN_MSG_MSGTYPE_OFFSET = 4;
    public static final int BIN_MSG_MSGTYPE_SIZE = 1;
    public static final int BIN_MSG_GROUPIDNUM_OFFSET = 5;
    public static final int BIN_MSG_GROUPIDNUM_SIZE = 2;
    public static final int BIN_MSG_STREAMIDNUM_OFFSET = 7;
    public static final int BIN_MSG_STREAMIDNUM_SIZE = 2;
    public static final int BIN_MSG_EXTEND_OFFSET = 9;
    public static final int BIN_MSG_EXTEND_SIZE = 2;
    public static final int BIN_MSG_DT_OFFSET = 11;
    public static final int BIN_MSG_DT_SIZE = 4;
    public static final int BIN_MSG_CNT_OFFSET = 15;
    public static final int BIN_MSG_CNT_SIZE = 2;
    public static final int BIN_MSG_UNIQ_OFFSET = 17;
    public static final int BIN_MSG_UNIQ_SIZE = 4;
    public static final int BIN_MSG_SET_SNAPPY = (1 << 5);
    public static final int BIN_MSG_BODYLEN_OFFSET = 21;
    public static final int BIN_MSG_BODYLEN_SIZE = 4;
    public static final int BIN_MSG_BODY_OFFSET = BIN_MSG_BODYLEN_SIZE + BIN_MSG_BODYLEN_OFFSET;
    public static final int BIN_MSG_ATTRLEN_SIZE = 2;
    public static final int BIN_MSG_MAGIC_SIZE = 2;
    public static final int BIN_MSG_MAGIC = 0xEE01;

    public static final int BIN_HB_FORMAT_SIZE = 18;
    public static final int BIN_HB_TOTALLEN_OFFSET = 0;
    public static final int BIN_HB_TOTALLEN_SIZE = 4;
    public static final int BIN_HB_FIXED_CONTENT_SIZE = BIN_HB_FORMAT_SIZE - BIN_HB_TOTALLEN_SIZE;
    public static final int BIN_HB_MSGTYPE_OFFSET = 4;
    public static final int BIN_HB_MSGTYPE_SIZE = 1;
    public static final int BIN_HB_DATATIME_OFFSET = 5;
    public static final int BIN_HB_DATATIME_SIZE = 4;
    public static final int BIN_HB_VERSION_OFFSET = 9;
    public static final int BIN_HB_VERSION_SIZE = 1;
    public static final int BIN_HB_BODYLEN_OFFSET = 10;
    public static final int BIN_HB_BODYLEN_SIZE = 4;
    public static final int BIN_HB_BODY_OFFSET = BIN_HB_BODYLEN_SIZE + BIN_HB_BODYLEN_OFFSET;
    public static final int BIN_HB_ATTRLEN_SIZE = 2;

    public static final int TXT_MSG_FORMAT_SIZE = 13;
    public static final int TXT_MSG_TOTALLEN_OFFSET = 0;
    public static final int TXT_MSG_TOTALLEN_SIZE = 4;
    public static final int TXT_MSG_FIXED_CONTENT_SIZE = TXT_MSG_FORMAT_SIZE - TXT_MSG_TOTALLEN_SIZE;
    public static final int TXT_MSG_MSGTYPE_OFFSET = 4;
    public static final int TXT_MSG_MSGTYPE_SIZE = 1;
    public static final int TXT_MSG_BODYLEN_OFFSET = 5;
    public static final int TXT_MSG_BODYLEN_SIZE = 4;
    public static final int TXT_MSG_BODY_OFFSET = TXT_MSG_BODYLEN_SIZE + TXT_MSG_BODYLEN_OFFSET;
    public static final int TXT_MSG_ATTRLEN_SIZE = 4;

}
