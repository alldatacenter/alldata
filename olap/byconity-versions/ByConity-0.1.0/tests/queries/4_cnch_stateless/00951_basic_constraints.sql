
DROP TABLE IF EXISTS test_constraints;
CREATE TABLE test_constraints
(
        a       UInt32,
        b       UInt32,
        CONSTRAINT b_constraint CHECK b > 0
)
ENGINE = CnchMergeTree ORDER BY (a);

INSERT INTO test_constraints VALUES (1, 2);
SELECT * FROM test_constraints ORDER BY a;

INSERT INTO test_constraints VALUES (3, 4), (1, 0); -- { serverError 469 }
SELECT * FROM test_constraints ORDER BY a;

DROP TABLE test_constraints;

CREATE TABLE test_constraints
(
        a       UInt32,
        b       UInt32,
        CONSTRAINT b_constraint CHECK b > 10,
        CONSTRAINT a_constraint CHECK a < 10
)
ENGINE = CnchMergeTree ORDER BY (a);

INSERT INTO test_constraints VALUES (1, 2); -- { serverError 469 }
SELECT * FROM test_constraints ORDER BY a;

INSERT INTO test_constraints VALUES (5, 16), (10, 11); -- { serverError 469 }
SELECT * FROM test_constraints ORDER BY a;

INSERT INTO test_constraints VALUES (7, 18), (0, 11);
SELECT * FROM test_constraints ORDER BY a;

DROP TABLE test_constraints;
