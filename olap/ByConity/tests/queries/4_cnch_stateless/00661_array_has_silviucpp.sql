DROP TABLE IF EXISTS has_function;
CREATE TABLE has_function(arr Array(Nullable(String))) ENGINE = CnchMergeTree ORDER BY arr;
INSERT INTO has_function(arr) values ([null, 'str1', 'str2']),(['str1', 'str2']), ([]), ([]);
SELECT arr, has(`arr`, 'str1') FROM has_function ORDER BY arr DESC;
SELECT has([null, 'str1', 'str2'], 'str1');
DROP TABLE has_function;
