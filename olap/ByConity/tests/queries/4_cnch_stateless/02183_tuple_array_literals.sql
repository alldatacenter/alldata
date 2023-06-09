CREATE TABLE tab (`a` Int8, `b` String, `c` Tuple(Int8), `d` Tuple(Tuple(Int8)), `e` Tuple(Int8, String), `f` Tuple(Tuple(Int8, String))) ENGINE = CnchMergeTree() PARTITION BY tuple() ORDER BY a;
INSERT INTO tab VALUES (1, 'a', tuple(1), tuple(tuple(1)), (1, 'a'), tuple((1, 'a')));
SELECT tuple(a, a) IN tuple(1, 1) FROM tab;
