USE test;
DROP TABLE IF EXISTS final_test;
CREATE TABLE final_test (id String, version Date) ENGINE = CnchMergeTree
PARTITION BY version ORDER BY id;
INSERT INTO final_test (id, version) VALUES ('2018-01-01', '2018-01-01');
SELECT * FROM final_test FINAL PREWHERE id == '2018-01-02';
DROP TABLE final_test;
