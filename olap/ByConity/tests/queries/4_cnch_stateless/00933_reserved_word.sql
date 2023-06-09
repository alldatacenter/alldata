
DROP TABLE IF EXISTS reserved_word_table;
CREATE TABLE reserved_word_table (`index` UInt8) ENGINE = CnchMergeTree ORDER BY `index`;

DETACH TABLE reserved_word_table PERMANENTLY;
ATTACH TABLE reserved_word_table;

DROP TABLE reserved_word_table;
