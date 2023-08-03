-- Licensed to the Apache Software Foundation(ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
--(the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

create table dbo.x_db_version_h(
id bigint identity not null primary key,
version varchar(64) not null,
inst_at datetime not null,
inst_by varchar(256) not null,
updated_at datetime not null,
updated_by varchar(256) not null,
active varchar(1) default 'Y' check(active IN ('Y', 'N'))
)
GO
exit