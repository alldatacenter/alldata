-- unique table doesn't support `cluster by`
DROP TABLE IF EXISTS u10100_cluster_by;

CREATE TABLE u10100_cluster_by (d Date, id Int32, s String)
ENGINE = CnchMergeTree PARTITION BY d ORDER BY id
UNIQUE KEY id
CLUSTER BY(id) INTO 32 BUCKETS; -- { serverError 36 }

-- unique key does not allow nullable column
DROP TABLE IF EXISTS u10100_nullable1;
DROP TABLE IF EXISTS u10100_nullable2;

CREATE TABLE u10100_nullable1 (d Date, id Nullable(Int32), val Int32)
ENGINE = CnchMergeTree PARTITION BY d ORDER BY val UNIQUE KEY id; -- { serverError 44 }

CREATE TABLE u10100_nullable2 (d Date, k1 Int32, k2 Nullable(String), val Int32)
ENGINE = CnchMergeTree PARTITION BY d ORDER BY val UNIQUE KEY (k1, k2); -- { serverError 44 }

-- unsupported data type for key column
DROP TABLE IF EXISTS u10100_bad_key_type;

CREATE TABLE u10100_bad_key_type (k Float32, v Int32) ENGINE = CnchMergeTree ORDER BY v UNIQUE KEY k; -- { serverError 44 }
CREATE TABLE u10100_bad_key_type (k Float64, v Int32) ENGINE = CnchMergeTree ORDER BY v UNIQUE KEY k; -- { serverError 44 }
CREATE TABLE u10100_bad_key_type (k Array(String), v Int32) ENGINE = CnchMergeTree ORDER BY v UNIQUE KEY k; -- { serverError 44 }
