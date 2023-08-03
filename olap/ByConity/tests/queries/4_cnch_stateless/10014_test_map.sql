

DROP TABLE IF EXISTS types;
CREATE TABLE types(I16 Int16, I32 Int32, I64 Int64, F32 Float32, F64 Float64, D Date, DT DateTime) Engine = CnchMergeTree ORDER BY tuple();
INSERT INTO TABLE types VALUES (1, 2, 4, 1.0, 2.0, '2021-01-01', '2021-01-01 00:00:00'), (8, 16, 32, 8.0, 16.0, '2021-01-01', '2021-01-01 00:00:00');

SELECT map('key1', I16, 'key2', I32) FROM types;
SELECT map('key1', I16, 'key2', I32, 'key3', I64) FROM types;
SELECT map(I16, I32, I16, I32) FROM types;
SELECT map(I16, I64, I32, I32, I64, I16) FROM types;
SELECT map(D, DT) FROM types;

SELECT toTypeName(map('key1', I16, 'key2', I32)) FROM types LIMIT 1;
SELECT toTypeName(map('key1', I16, 'key2', I32, 'key3', I64)) FROM types LIMIT 1;
SELECT toTypeName(map(I16, I32, I16, I32)) FROM types LIMIT 1;
SELECT toTypeName(map(I16, I64, I32, I32, I64, I16)) FROM types LIMIT 1;
SELECT toTypeName(map('key1', F64, 'key2', I32, 'key3', I16)) FROM types LIMIT 1;
SELECT toTypeName(map('key1', F32, 'key2', I32, 'key3', I64)) FROM types LIMIT 1; -- { serverError 386 }
SELECT toTypeName(map('key1', F32, 'key2', F64, 'key3', I64)) FROM types LIMIT 1; -- { serverError 386 }
SELECT toTypeName(map(D, DT)) from types LIMIT 1;
SELECT toTypeName(map(D, DT, DT, D)) from types LIMIT 1;

DROP TABLE types;
CREATE TABLE types(A32 Array(Int32), AN32 Array(Nullable(Int32)), NA32 Nullable(Array(Int32))) Engine = CnchMergeTree ORDER BY tuple();
INSERT INTO TABLE types VALUES ([1, 2], [1, 2, NULL], [1, 2]), ([4, 8], [4, 8, NULL], NULL);

SELECT map('key1', A32, 'key2', AN32) FROM types;
SELECT map('key1', A32, 'key2', NA32) FROM types; -- { serverError 386 }
SELECT map(A32, AN32) FROM types; -- { clientError 36 }

SELECT toTypeName(map('key1', A32, 'key2', AN32)) FROM types LIMIT 1;

DROP TABLE types;

select map(); -- { serverError 42 }
select map('a', 'b', 'c'); -- { serverError 42 }
SELECT map(CAST('key', 'Nullable(String)'), 'yy');
SELECT map('key', CAST('value', 'Nullable(String)'));

