-- We add 1, because function toString has special behaviour for zero datetime
SELECT count() FROM numbers(1000000) WHERE formatDateTime(toDateTime(1 + rand() % 0xFFFFFFFF), '%F %T') != toString(toDateTime(1 + rand() % 0xFFFFFFFF));
SELECT count() FROM numbers(1000000) WHERE formatDateTime(toDateTime(1 + rand() % 0xFFFFFFFF), '%Y-%m-%d %H:%M:%S') != toString(toDateTime(1 + rand() % 0xFFFFFFFF));
SELECT count() FROM numbers(1000000) WHERE formatDateTime(toDateTime(1 + rand() % 0xFFFFFFFF), '%Y-%m-%d %R:%S') != toString(toDateTime(1 + rand() % 0xFFFFFFFF));
SELECT count() FROM numbers(1000000) WHERE formatDateTime(toDateTime(1 + rand() % 0xFFFFFFFF), '%F %R:%S') != toString(toDateTime(1 + rand() % 0xFFFFFFFF));

SELECT count() FROM numbers(1000000) WHERE formatDateTime(toDate(today() + rand() % 4096), '%F') != toString(toDate(today() + rand() % 4096));
SELECT count() FROM numbers(1000000) WHERE formatDateTime(toDate(today() + rand() % 4096), '%F %T') != toString(toDateTime(toDate(today() + rand() % 4096)));
