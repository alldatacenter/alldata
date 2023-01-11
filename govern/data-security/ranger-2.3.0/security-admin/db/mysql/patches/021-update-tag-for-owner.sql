-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- ---------------------------------------
-- add column in x_tag.owned_by
-- ---------------------------------------
DROP PROCEDURE IF EXISTS add_columns_x_tag;

DELIMITER ;;
CREATE PROCEDURE add_columns_x_tag() BEGIN
  IF EXISTS (SELECT * FROM information_schema.tables WHERE table_schema=database() AND table_name = 'x_tag') THEN
    IF NOT EXISTS (SELECT * FROM information_schema.columns WHERE table_schema=database() AND table_name = 'x_tag' AND column_name = 'owned_by') THEN
      ALTER TABLE `x_tag` ADD COLUMN `owned_by` SMALLINT DEFAULT 0 NOT NULL;
    END IF;
  END IF;
END;;

DELIMITER ;
CALL add_columns_x_tag();
DROP PROCEDURE IF EXISTS add_columns_x_tag;
