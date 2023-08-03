CREATE DATABASE IF NOT EXISTS test Engine = Cnch;
DROP TABLE IF EXISTS test.table_for_no_ip_port_dict;
CREATE TABLE test.table_for_no_ip_port_dict(id UInt64, a UInt64, b Int32, c String) ENGINE = CnchMergeTree() ORDER BY id;
INSERT INTO test.table_for_no_ip_port_dict VALUES (1, 100, -100, 'clickhouse'), (2, 3, 4, 'database'), (5, 6, 7, 'columns'), (10, 9, 8, '');
INSERT INTO test.table_for_no_ip_port_dict SELECT number, 0, -1, 'a' FROM system.numbers WHERE number NOT IN (1, 2, 5, 10) LIMIT 370;
INSERT INTO test.table_for_no_ip_port_dict SELECT number, 0, -1, 'b' FROM system.numbers WHERE number NOT IN (1, 2, 5, 10) LIMIT 370, 370;
INSERT INTO test.table_for_no_ip_port_dict SELECT number, 0, -1, 'c' FROM system.numbers WHERE number NOT IN (1, 2, 5, 10) LIMIT 700, 370;

DROP TABLE IF EXISTS test.interger_col;
CREATE TABLE test.interger_col (id UInt64) Engine = CnchMergeTree ORDER by id;
INSERT INTO test.interger_col VALUES (1), (2), (3), (4), (5), (6);

DROP DICTIONARY IF EXISTS test.dict_flat_no_ip_port;
CREATE DICTIONARY test.dict_flat_no_ip_port(id UInt64, a UInt64 DEFAULT 0, b Int32 DEFAULT -1, c String DEFAULT 'none') PRIMARY KEY id SOURCE(CLICKHOUSE(USER 'default' TABLE 'table_for_no_ip_port_dict' PASSWORD '' DB 'test')) LIFETIME(MIN 1000 MAX 2000) LAYOUT(FLAT());

SELECT status FROM system.dictionaries where (database = 'test') AND (name = 'dict_flat_no_ip_port');

SELECT '-- select from table column b';
SELECT id, dictGetInt32('test.dict_flat_no_ip_port', 'b', id) from test.interger_col ORDER BY id;
SELECT '-- select from table column a';
SELECT id, dictGetUInt64('test.dict_flat_no_ip_port', 'a', id) from test.interger_col ORDER BY id;
SELECT '-- select from table column c';
SELECT id, dictGetString('test.dict_flat_no_ip_port', 'c', id) from test.interger_col ORDER BY id;

SELECT dictGetInt32('test.dict_flat_no_ip_port', 'b', toUInt64(1));
SELECT dictGetInt32('test.dict_flat_no_ip_port', 'b', toUInt64(4));
SELECT dictGetUInt64('test.dict_flat_no_ip_port', 'a', toUInt64(5));
SELECT dictGetUInt64('test.dict_flat_no_ip_port', 'a', toUInt64(6));
SELECT dictGetString('test.dict_flat_no_ip_port', 'c', toUInt64(2));
SELECT dictGetString('test.dict_flat_no_ip_port', 'c', toUInt64(3));
SELECT status FROM system.dictionaries where (database = 'test') AND (name = 'dict_flat_no_ip_port');

DROP DICTIONARY IF EXISTS test.dict_flat_no_ip_port;
DROP TABLE IF EXISTS test.interger_col;
DROP TABLE IF EXISTS test.table_for_no_ip_port_dict;
