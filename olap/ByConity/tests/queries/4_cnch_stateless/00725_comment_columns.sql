SET show_table_uuid_in_table_create_query_if_not_nil = 0;
DROP TABLE IF EXISTS check_query_comment_column;

-- Check `ALTER TABLE table_name COMMENT COLUMN 'comment'` statement with CnchMergeTree engine
CREATE TABLE check_query_comment_column
  (
    first_column UInt8 COMMENT 'comment 1',
    second_column UInt8 COMMENT 'comment 2',
    third_column UInt8 COMMENT 'comment 3'
  ) ENGINE = CnchMergeTree()
        ORDER BY first_column
        PARTITION BY second_column
        SAMPLE BY first_column;

SHOW CREATE TABLE check_query_comment_column;
DESCRIBE TABLE check_query_comment_column;

ALTER TABLE check_query_comment_column
  COMMENT COLUMN first_column 'comment 1_2',
  COMMENT COLUMN second_column 'comment 2_2',
  COMMENT COLUMN third_column 'comment 3_2';

SHOW CREATE TABLE check_query_comment_column;
DESCRIBE TABLE check_query_comment_column;

ALTER TABLE check_query_comment_column
  MODIFY COLUMN first_column COMMENT 'comment 1_3',
  MODIFY COLUMN second_column COMMENT 'comment 2_3',
  MODIFY COLUMN third_column COMMENT 'comment 3_3';

SHOW CREATE TABLE check_query_comment_column;
DESCRIBE TABLE check_query_comment_column;

ALTER TABLE check_query_comment_column
  MODIFY COLUMN first_column DEFAULT 1 COMMENT 'comment 1_3',
  MODIFY COLUMN second_column COMMENT 'comment 2_3',            -- We can't change default value of partition key.
  MODIFY COLUMN third_column DEFAULT 1 COMMENT 'comment 3_3';

SHOW CREATE TABLE check_query_comment_column;
DESCRIBE TABLE check_query_comment_column;

DROP TABLE IF EXISTS check_query_comment_column;

-- TODO: add here tests with ReplicatedMergeTree
