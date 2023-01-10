DROP DICTIONARY IF EXISTS dict1;
CREATE DICTIONARY dict1 ( k UInt64, s String DEFAULT ''); -- { serverError 489 }
CREATE DICTIONARY dict1 ( k UInt64, s String DEFAULT '') LAYOUT(FLAT()); -- { serverError 489 }
CREATE DICTIONARY dict1 ( k UInt64, s String DEFAULT '') LAYOUT(FLAT()) LIFETIME(MIN 10 MAX 20); -- { serverError 489 }
CREATE DICTIONARY dict1 ( k UInt64, s String DEFAULT '') PRIMARY KEY k LAYOUT(FLAT()) LIFETIME(MIN 10 MAX 20); -- { serverError 489 }
CREATE DICTIONARY dict1 ( k UInt64, s String DEFAULT '' ) PRIMARY KEY a LAYOUT(FLAT()) LIFETIME(MIN 10 MAX 20) SOURCE(CLICKHOUSE(HOST '127.0.0.1' PORT 'port' USER 'default' TABLE 'dictionary_name' PASSWORD '' DB 'test')); -- { serverError 489 }
CREATE DICTIONARY dict1 ( k UInt64, s String DEFAULT '' ) PRIMARY KEY k,s LAYOUT(FLAT()) LIFETIME(MIN 10 MAX 20) SOURCE(CLICKHOUSE(HOST '127.0.0.1' PORT 'port' USER 'default' TABLE 'dictionary_name' PASSWORD '' DB 'test')); -- { serverError 489 }

CREATE DICTIONARY dict1 ( k abc, s String DEFAULT '' ) PRIMARY KEY k,s LAYOUT(FLAT()) LIFETIME(MIN 10 MAX 20) SOURCE(CLICKHOUSE(HOST '127.0.0.1' PORT 'port' USER 'default' TABLE 'dictionary_name' PASSWORD '' DB 'test')); -- { serverError 489 }
CREATE DICTIONARY dict1 ( k UInt64, s abc ) PRIMARY KEY k,s LAYOUT(FLAT()) LIFETIME(MIN 10 MAX 20) SOURCE(CLICKHOUSE(HOST '127.0.0.1' PORT 'port' USER 'default' TABLE 'dictionary_name' PASSWORD '' DB 'test')); -- { serverError 489 }

CREATE DICTIONARY dict1 ( k UInt64, s String DEFAULT '') PRIMARY KEY k LAYOUT(FLAT()) LIFETIME(MIN 10 MAX 20) SOURCE(CLICKHOUSE(HOST '127.0.0.1' PORT 1234 USER 'default' TABLE 'dictionary_name' PASSWORD '' DB 'test'));
DROP DICTIONARY IF EXISTS dict1;
