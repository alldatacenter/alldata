
DROP TABLE IF EXISTS Cnch_test;
CREATE TABLE Cnch_test (x UInt8) ENGINE = CnchMergeTree ORDER BY x;
SELECT * FROM Cnch_test ORDER BY x;
INSERT INTO Cnch_test VALUES (1), (2);
SELECT * FROM Cnch_test ORDER BY x;
DROP TABLE Cnch_test;
