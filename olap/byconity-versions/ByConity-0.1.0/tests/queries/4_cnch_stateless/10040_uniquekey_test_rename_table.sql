DROP TABLE IF EXISTS rename_unique_table;
DROP TABLE IF EXISTS rename_unique_table_new;

CREATE TABLE rename_unique_table (d Date, id Int32, s String, arr Array(Int32), sum materialized arraySum(arr))
    ENGINE = CnchMergeTree PARTITION BY d ORDER BY (s, id) PRIMARY KEY s UNIQUE KEY id;

INSERT INTO rename_unique_table VALUES ('2020-10-29', 1001, '1001A', [1,2]);

SELECT d, id, s, arr, sum FROM rename_unique_table ORDER BY d, id;

RENAME TABLE rename_unique_table TO rename_unique_table_new;

-- Test insert into the renamed table
INSERT INTO rename_unique_table_new VALUES ('2020-10-30', 1001, '1002B', [9, 10]);

SELECT d, id, s, arr, sum FROM rename_unique_table ORDER BY d, id; -- { serverError 60 };
SELECT d, id, s, arr, sum FROM rename_unique_table_new ORDER BY d, id;

DROP TABLE IF EXISTS rename_unique_table;
DROP TABLE IF EXISTS rename_unique_table_new;
