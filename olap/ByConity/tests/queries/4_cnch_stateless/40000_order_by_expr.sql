
DROP TABLE IF EXISTS tbl;

CREATE TABLE tbl(`id` Int32, i32 Nullable(Int32))
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

INSERT INTO tbl values (1, 0) (2, 1) (3, 2) (4, NULL);

select * from tbl t1, tbl t2  order by t2.id + t2.i32, t1.id;

DROP TABLE IF EXISTS tbl;
