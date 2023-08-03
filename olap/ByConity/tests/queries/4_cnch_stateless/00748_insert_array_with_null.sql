DROP TABLE IF EXISTS arraytest;

CREATE TABLE arraytest ( created_date Date DEFAULT toDate(created_at), created_at DateTime DEFAULT now(), strings Array(String) DEFAULT emptyArrayString()) ENGINE = CnchMergeTree() PARTITION BY toYYYYMM(created_date) SAMPLE BY cityHash64(created_at) ORDER BY (created_date, cityHash64(created_at)) SETTINGS index_granularity=8192;

INSERT INTO arraytest (created_at, strings) VALUES (now(), ['aaaaa', 'bbbbb', 'ccccc']);
INSERT INTO arraytest (created_at, strings) VALUES (now(), ['aaaaa', 'bbbbb', null]); -- { clientError 349 }

SELECT strings from arraytest;

DROP TABLE IF EXISTS arraytest;

