DROP TABLE IF EXISTS default;

CREATE TABLE default (d Date DEFAULT toDate(t), t DateTime) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(d) ORDER BY t;
INSERT INTO default (t) VALUES ('1234567890');
SELECT toStartOfMonth(d), toUInt32(t) FROM default;

DROP TABLE default;
