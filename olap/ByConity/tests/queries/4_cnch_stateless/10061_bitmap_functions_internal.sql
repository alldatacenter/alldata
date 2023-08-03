
SELECT bitmapToArray(arrayToBitmap([1, 2, 3, 4, 5]));
SELECT bitmapToArray(bitmapAnd(arrayToBitmap([1,2,3]),arrayToBitmap([3,4,5])));
SELECT bitmapToArray(bitmapOr(arrayToBitmap([1,2,3]),arrayToBitmap([3,4,5])));
SELECT bitmapToArray(bitmapXor(arrayToBitmap([1,2,3]),arrayToBitmap([3,4,5])));
SELECT bitmapToArray(bitmapAndnot(arrayToBitmap([1,2,3]),arrayToBitmap([3,4,5])));
SELECT bitmapCardinality(arrayToBitmap([1, 2, 3, 4, 5]));
SELECT bitmapAndCardinality(arrayToBitmap([1,2,3]),arrayToBitmap([3,4,5]));
SELECT bitmapOrCardinality(arrayToBitmap([1,2,3]),arrayToBitmap([3,4,5]));
SELECT bitmapXorCardinality(arrayToBitmap([1,2,3]),arrayToBitmap([3,4,5]));
SELECT bitmapAndnotCardinality(arrayToBitmap([1,2,3]),arrayToBitmap([3,4,5]));
SELECT bitmapAndCardinality(arrayToBitmap([100, 200, 500]), arrayToBitmap(CAST([100, 200], 'Array(UInt16)')));
SELECT bitmapToArray(bitmapAnd(arrayToBitmap([100, 200, 500]), arrayToBitmap(CAST([100, 200], 'Array(UInt16)'))));

-- between column and expression test
DROP TABLE IF EXISTS bitmap_column_expr_test;
CREATE TABLE bitmap_column_expr_test
(
    t DateTime,
    z BitMap64
)
ENGINE = CnchMergeTree()
PARTITION BY toYYYYMMDD(t)
ORDER BY t;

INSERT INTO bitmap_column_expr_test VALUES (now(), [3,19,47]);

SELECT bitmapAndCardinality( arrayToBitmap(cast([19,7] AS Array(UInt32))), z) FROM bitmap_column_expr_test;
SELECT bitmapAndCardinality( z, arrayToBitmap(cast([19,7] AS Array(UInt32))) ) FROM bitmap_column_expr_test;

SELECT bitmapCardinality(bitmapAnd(arrayToBitmap(cast([19,7] AS Array(UInt32))), z )) FROM bitmap_column_expr_test;
SELECT bitmapCardinality(bitmapAnd(z, arrayToBitmap(cast([19,7] AS Array(UInt32))))) FROM bitmap_column_expr_test;

DROP TABLE IF EXISTS bitmap_column_expr_test2;
CREATE TABLE bitmap_column_expr_test2
(
    tag_id String,
    z BitMap64
)
ENGINE = CnchMergeTree()
ORDER BY tag_id;

INSERT INTO bitmap_column_expr_test2 VALUES ('tag1', [1,2,3,4,5,6,7,8,9,10]);
INSERT INTO bitmap_column_expr_test2 VALUES ('tag2', [6,7,8,9,10,11,12,13,14,15]);
INSERT INTO bitmap_column_expr_test2 VALUES ('tag3', [2,4,6,8,10,12]);

SELECT bitmapColumnCardinality(z) FROM bitmap_column_expr_test2 WHERE like(tag_id, 'tag%');
SELECT arraySort(bitmapToArray(bitmapColumnOr(z))) FROM bitmap_column_expr_test2 WHERE like(tag_id, 'tag%');

SELECT bitmapCardinality(bitmapColumnOr(z)) FROM bitmap_column_expr_test2 WHERE like(tag_id, 'tag%');
SELECT arraySort(bitmapToArray(bitmapColumnOr(z))) FROM bitmap_column_expr_test2 WHERE like(tag_id, 'tag%');

SELECT bitmapCardinality(bitmapColumnAnd(z)) FROM bitmap_column_expr_test2 WHERE like(tag_id, 'tag%');
SELECT arraySort(bitmapToArray(bitmapColumnAnd(z))) FROM bitmap_column_expr_test2 WHERE like(tag_id, 'tag%');

SELECT bitmapCardinality(bitmapColumnXor(z)) FROM bitmap_column_expr_test2 WHERE like(tag_id, 'tag%');
SELECT arraySort(bitmapToArray(bitmapColumnXor(z))) FROM bitmap_column_expr_test2 WHERE like(tag_id, 'tag%');

DROP TABLE IF EXISTS bitmap_test;
DROP TABLE IF EXISTS bitmap_state_test;
DROP TABLE IF EXISTS bitmap_column_expr_test;
DROP TABLE IF EXISTS bitmap_column_expr_test2;

-- bitmapHasAny:
---- Empty
SELECT bitmapHasAny(arrayToBitmap([1, 2, 3, 5]), arrayToBitmap(emptyArrayUInt8()));
SELECT bitmapHasAny(arrayToBitmap(emptyArrayUInt32()), arrayToBitmap(emptyArrayUInt32()));
SELECT bitmapHasAny(arrayToBitmap(emptyArrayUInt16()), arrayToBitmap([1, 2, 3, 500]));
---- Small x Small
SELECT bitmapHasAny(arrayToBitmap([1, 2, 3, 5]),arrayToBitmap([0, 3, 7]));
SELECT bitmapHasAny(arrayToBitmap([1, 2, 3, 5]),arrayToBitmap([0, 4, 7]));
---- Small x Large
select bitmapHasAny(arrayToBitmap([100,110,120]),arrayToBitmap([ 99, 100, 101,
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33]));
select bitmapHasAny(arrayToBitmap([100,200,500]),arrayToBitmap([ 99, 101, 600,
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33]));
---- Large x Small
select bitmapHasAny(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,230]),arrayToBitmap([ 99, 100, 101]));
select bitmapHasAny(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]),arrayToBitmap([ 99, 101, 600]));
---- Large x Large
select bitmapHasAny(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    40,50,60]),arrayToBitmap([ 41, 50, 61,
    99,98,97,96,95,94,93,92,91,90,89,88,87,86,85,84,83,82,81,80,79,78,77,76,75,74,73,72,71,70,69,68,67,66,65]));
select bitmapHasAny(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    40,50,60]),arrayToBitmap([ 41, 49, 51, 61,
    99,98,97,96,95,94,93,92,91,90,89,88,87,86,85,84,83,82,81,80,79,78,77,76,75,74,73,72,71,70,69,68,67,66,65]));

-- bitmapHasAll:
---- Empty
SELECT bitmapHasAll(arrayToBitmap([1, 2, 3, 5]), arrayToBitmap(emptyArrayUInt8()));
SELECT bitmapHasAll(arrayToBitmap(emptyArrayUInt32()), arrayToBitmap(emptyArrayUInt32()));
SELECT bitmapHasAll(arrayToBitmap(emptyArrayUInt16()), arrayToBitmap([1, 2, 3, 500]));
---- Small x Small
select bitmapHasAll(arrayToBitmap([1,5,7,9]),arrayToBitmap([5,7]));
select bitmapHasAll(arrayToBitmap([1,5,7,9]),arrayToBitmap([5,7,2]));
---- Small x Large
select bitmapHasAll(arrayToBitmap([100,110,120]),arrayToBitmap([ 99, 100, 101,
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33]));
select bitmapHasAll(arrayToBitmap([100,200,500]),arrayToBitmap([ 99, 101, 600,
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33]));
---- Small x LargeSmall
select bitmapHasAll(arrayToBitmap([1,5,7,9]),bitmapXor(arrayToBitmap([1,5,7]), arrayToBitmap([5,7,9])));
select bitmapHasAll(arrayToBitmap([1,5,7,9]),bitmapXor(arrayToBitmap([1,5,7]), arrayToBitmap([2,5,7])));
---- Large x Small
select bitmapHasAll(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]),arrayToBitmap([100, 500]));
select bitmapHasAll(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]),arrayToBitmap([ 99, 100, 500]));
---- LargeSmall x Small
select bitmapHasAll(bitmapXor(arrayToBitmap([1,7]), arrayToBitmap([5,7,9])), arrayToBitmap([1,5]));
select bitmapHasAll(bitmapXor(arrayToBitmap([1,7]), arrayToBitmap([5,7,9])), arrayToBitmap([1,5,7]));
---- Large x Large
select bitmapHasAll(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]),arrayToBitmap([ 100, 200, 500,
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33]));
select bitmapHasAll(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]),arrayToBitmap([ 100, 200, 501,
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33]));

-- bitmapContains:
---- Empty
SELECT bitmapContains(arrayToBitmap(emptyArrayUInt32()), toUInt32(0));
SELECT bitmapContains(arrayToBitmap(emptyArrayUInt16()), toUInt32(5));
---- Small
select bitmapContains(arrayToBitmap([1,5,7,9]),toUInt32(0));
select bitmapContains(arrayToBitmap([1,5,7,9]),toUInt32(9));
---- Large
select bitmapContains(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]),toUInt32(100));
select bitmapContains(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]),toUInt32(101));
select bitmapContains(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]),toUInt32(500));

-- bitmapSubsetInRange:
---- Empty
SELECT bitmapToArray(bitmapSubsetInRange(arrayToBitmap(emptyArrayUInt32()), toUInt32(0), toUInt32(10)));
SELECT bitmapToArray(bitmapSubsetInRange(arrayToBitmap(emptyArrayUInt16()), toUInt32(0), toUInt32(10)));
---- Small
select bitmapToArray(bitmapSubsetInRange(arrayToBitmap([1,5,7,9]), toUInt32(0), toUInt32(4)));
select bitmapToArray(bitmapSubsetInRange(arrayToBitmap([1,5,7,9]), toUInt32(10), toUInt32(10)));
select bitmapToArray(bitmapSubsetInRange(arrayToBitmap([1,5,7,9]), toUInt32(3), toUInt32(7)));
---- Large
select bitmapToArray(bitmapSubsetInRange(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]), toUInt32(0), toUInt32(100)));
select bitmapToArray(bitmapSubsetInRange(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]), toUInt32(30), toUInt32(200)));
select bitmapToArray(bitmapSubsetInRange(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]), toUInt32(100), toUInt32(200)));

-- bitmapSubsetLimit:
---- Empty
SELECT bitmapToArray(bitmapSubsetLimit(arrayToBitmap(emptyArrayUInt32()), toUInt32(0), toUInt32(10)));
SELECT bitmapToArray(bitmapSubsetLimit(arrayToBitmap(emptyArrayUInt16()), toUInt32(0), toUInt32(10)));
---- Small
select bitmapToArray(bitmapSubsetLimit(arrayToBitmap([1,5,7,9]), toUInt32(0), toUInt32(4)));
select bitmapToArray(bitmapSubsetLimit(arrayToBitmap([1,5,7,9]), toUInt32(10), toUInt32(10)));
select bitmapToArray(bitmapSubsetLimit(arrayToBitmap([1,5,7,9]), toUInt32(3), toUInt32(7)));
---- Large
select bitmapToArray(bitmapSubsetLimit(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]), toUInt32(0), toUInt32(100)));
select bitmapToArray(bitmapSubsetLimit(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]), toUInt32(30), toUInt32(200)));
select bitmapToArray(bitmapSubsetLimit(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]), toUInt32(100), toUInt32(200)));

-- bitmapMin:
---- Empty
SELECT bitmapMin(arrayToBitmap(emptyArrayUInt8()));
SELECT bitmapMin(arrayToBitmap(emptyArrayUInt16()));
SELECT bitmapMin(arrayToBitmap(emptyArrayUInt32()));
---- Small
select bitmapMin(arrayToBitmap([1,5,7,9]));
---- Large
select bitmapMin(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]));

-- bitmapMax:
---- Empty
SELECT bitmapMax(arrayToBitmap(emptyArrayUInt8()));
SELECT bitmapMax(arrayToBitmap(emptyArrayUInt16()));
SELECT bitmapMax(arrayToBitmap(emptyArrayUInt32()));
---- Small
select bitmapMax(arrayToBitmap([1,5,7,9]));
---- Large
select bitmapMax(arrayToBitmap([
    0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,
    100,200,500]));
