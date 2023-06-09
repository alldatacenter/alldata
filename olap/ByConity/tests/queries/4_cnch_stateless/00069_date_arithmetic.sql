SELECT toTypeName(now() - now()) = 'Int32';
SELECT toTypeName(now() + 1) = 'DateTime';
SELECT toTypeName(1 + now()) = 'DateTime';
SELECT toTypeName(now() - 1) = 'DateTime';
SELECT toDateTime(1) + 1 = toDateTime(2);
SELECT 1 + toDateTime(1) = toDateTime(2);
SELECT toDateTime(1) - 1 = toDateTime(0);

SELECT toTypeName(today()) = 'Date';
SELECT today() = toDate(now());

SELECT toTypeName(yesterday()) = 'Date';
SELECT yesterday() = toDate(now() - 24*60*60);

SELECT toTypeName(today() - today()) = 'Int32';
SELECT toTypeName(today() + 1) = 'Date';
SELECT toTypeName(1 + today()) = 'Date';
SELECT toTypeName(today() - 1) = 'Date';
SELECT yesterday() + 1 = today();
SELECT 1 + yesterday() = today();
SELECT today() - 1 = yesterday();

SELECT '-----numeric to date------';
SELECT toDate(toInt32(1668268800), 'UTC'), toDate(toInt64(1668268800), 'UTC');
SELECT toDate(toUInt32(1668268800), 'UTC'), toDate(toUInt64(1668268800), 'UTC');
SELECT toDate(toFloat32(1668268800.1), 'UTC'), toDate(toFloat64(1668268800.1), 'UTC');
SELECT toDate(toInt32(0xFFFF), 'UTC'), toDate(toUInt32(0xFFFF), 'UTC'),
       toDate(toInt64(0xFFFF), 'UTC'), toDate(toUInt64(0xFFFF), 'UTC');
SELECT toDate(toUInt32(4294880895), 'UTC'), toDate(toInt64(4294880895), 'UTC'), toDate(toUInt64(4294880895), 'UTC');
SELECT toDate(toUInt32(0xFFFFFFFF), 'UTC'), toDate(toInt64(0xFFFFFFFF), 'UTC'), toDate(toUInt64(0xFFFFFFFF), 'UTC');

