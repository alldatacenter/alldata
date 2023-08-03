DROP TABLE IF EXISTS index_for_like;
CREATE TABLE index_for_like (s String, d Date DEFAULT today()) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(d) ORDER BY (s, d) SETTINGS index_granularity=1;

INSERT INTO index_for_like (s) VALUES ('Hello'), ('Hello, World'), ('Hello, World 1'), ('Hello 1'), ('Goodbye'), ('Goodbye, World'), ('Goodbye 1'), ('Goodbye, World 1');

SET max_rows_to_read = 3;
SELECT s FROM index_for_like WHERE s LIKE 'Hello, World%' ORDER BY s;

SET max_rows_to_read = 2;
SELECT s FROM index_for_like WHERE s LIKE 'Hello, World %' ORDER BY s;

SET max_rows_to_read = 2;
SELECT s FROM index_for_like WHERE s LIKE 'Hello, World 1%' ORDER BY s;

SET max_rows_to_read = 1;
SELECT s FROM index_for_like WHERE s LIKE 'Hello, World 2%' ORDER BY s;

SET max_rows_to_read = 1;
SELECT s FROM index_for_like WHERE s LIKE 'Hello, Worle%' ORDER BY s;

SET max_rows_to_read = 3;
SELECT s FROM index_for_like WHERE s LIKE 'Hello, Wor%' ORDER BY s;

SET max_rows_to_read = 5;
SELECT s FROM index_for_like WHERE s LIKE 'Hello%' ORDER BY s;

SET max_rows_to_read = 2;
SELECT s FROM index_for_like WHERE s LIKE 'Hello %' ORDER BY s;

SET max_rows_to_read = 3;
SELECT s FROM index_for_like WHERE s LIKE 'Hello,%' ORDER BY s;

SET max_rows_to_read = 1;
SELECT s FROM index_for_like WHERE s LIKE 'Hello;%' ORDER BY s;

SET max_rows_to_read = 5;
SELECT s FROM index_for_like WHERE s LIKE 'H%' ORDER BY s;

SET max_rows_to_read = 4;
SELECT s FROM index_for_like WHERE s LIKE 'Good%' ORDER BY s;

SET max_rows_to_read = 8;
SELECT s FROM index_for_like WHERE s LIKE '%' ORDER BY s;
SELECT s FROM index_for_like WHERE s LIKE '%Hello%' ORDER BY s;
SELECT s FROM index_for_like WHERE s LIKE '%Hello' ORDER BY s;

SET max_rows_to_read = 3;
SELECT s FROM index_for_like WHERE s LIKE 'Hello, World% %' ORDER BY s;
SELECT s FROM index_for_like WHERE s LIKE 'Hello, Worl_%' ORDER BY s;

SET max_rows_to_read = 1;
SELECT s FROM index_for_like WHERE s LIKE 'Hello, Worl\\_%' ORDER BY s;

DROP TABLE index_for_like;
