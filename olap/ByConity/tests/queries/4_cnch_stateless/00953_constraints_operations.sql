
DROP TABLE IF EXISTS test_constraints;
CREATE TABLE test_constraints
(
        a       UInt32,
        b       UInt32,
        CONSTRAINT b_constraint CHECK b > 0
)
ENGINE = CnchMergeTree ORDER BY (a);

INSERT INTO test_constraints VALUES (1, 2);
SELECT * FROM test_constraints;

INSERT INTO test_constraints VALUES (1, 0); -- { serverError 469 }
SELECT * FROM test_constraints;

ALTER TABLE test_constraints DROP CONSTRAINT b_constraint;

INSERT INTO test_constraints VALUES (1, 0);

ALTER TABLE test_constraints ADD CONSTRAINT b_constraint CHECK b > 10;

INSERT INTO test_constraints VALUES (1, 10); -- { serverError 469 }

INSERT INTO test_constraints VALUES (1, 11);

DROP TABLE test_constraints;
