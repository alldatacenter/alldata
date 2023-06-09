

DROP TABLE IF EXISTS detach_test;
CREATE TABLE detach_test (id Int32, score Int32) Engine = CnchMergeTree ORDER BY id;

INSERT INTO detach_test VALUES (1, 2), (3, 4);
SELECT COUNT() FROM detach_test;

DETACH TABLE detach_test PERMANENTLY;
SELECT COUNT() FROM detach_test; --{serverError 60}

ATTACH TABLE detach_test;
SELECT COUNT() FROM detach_test;

DROP TABLE detach_test;
