DROP TABLE IF EXISTS cnch_partial_part_bucket;
CREATE TABLE cnch_partial_part_bucket(name String, age Int8) ENGINE = CnchMergeTree() PARTITION BY name CLUSTER BY age INTO 1 BUCKETS ORDER BY name;
INSERT INTO cnch_partial_part_bucket VALUES ('kobe', 10);

SET cnch_part_allocation_algorithm = 3;
SELECT name, age FROM cnch_partial_part_bucket;
ALTER TABLE cnch_partial_part_bucket MODIFY COLUMN age UInt16;

SYSTEM START MERGES cnch_partial_part_bucket;
SELECT sleep(3) FORMAT Null;
OPTIMIZE TABLE cnch_partial_part_bucket;
SELECT sleep(3) FORMAT Null;
SELECT name, age FROM cnch_partial_part_bucket;

DROP TABLE cnch_partial_part_bucket;