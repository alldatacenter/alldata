
CREATE TABLE IF NOT EXISTS empty_Cnch_MergeTree(A UInt8) Engine = CnchMergeTree ORDER BY A;
SELECT A FROM empty_Cnch_MergeTree;
DROP TABLE empty_Cnch_MergeTree;