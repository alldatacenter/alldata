--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
ALTER TABLE TXNS MODIFY (
  TXN_ID NUMBER(19),
  TXN_STARTED NUMBER(19),
  TXN_LAST_HEARTBEAT NUMBER(19)
);

ALTER TABLE TXN_COMPONENTS MODIFY (
  TC_TXNID NUMBER(19)
);

ALTER TABLE COMPLETED_TXN_COMPONENTS MODIFY (
  CTC_TXNID NUMBER(19)
);

ALTER TABLE NEXT_TXN_ID MODIFY (
  NTXN_NEXT NUMBER(19)
);

ALTER TABLE HIVE_LOCKS MODIFY (
  HL_LOCK_EXT_ID NUMBER(19),
  HL_LOCK_INT_ID NUMBER(19),
  HL_TXNID NUMBER(19),
  HL_LAST_HEARTBEAT NUMBER(19),
  HL_ACQUIRED_AT NUMBER(19)
);

ALTER TABLE NEXT_LOCK_ID MODIFY (
  NL_NEXT NUMBER(19)
);

ALTER TABLE COMPACTION_QUEUE MODIFY (
  CQ_ID NUMBER(19),
  CQ_START NUMBER(19)
);

ALTER TABLE NEXT_COMPACTION_QUEUE_ID MODIFY (
  NCQ_NEXT NUMBER(19)
);
