SET adaptive_type_cast=1;
SELECT 'Enable adaptive type cast';

SELECT toDateTime(0xFFFFFFFF + 1, 'UTC'), toDateTime32(0xFFFFFFFF + 1, 'UTC'),
       CAST(0xFFFFFFFF + 1 AS DateTime('UTC')), FROM_UNIXTIME(0xFFFFFFFF + 1, 'UTC'),
       toDateTime(toUInt64(0xFFFFFFFF + 1), 'UTC'), toDateTime32(toUInt64(0xFFFFFFFF + 1), 'UTC'),
       CAST(toUInt64(0xFFFFFFFF + 1) AS DateTime('UTC')), FROM_UNIXTIME(toUInt64(0xFFFFFFFF + 1), 'UTC');
SELECT toDateTime(0xFFFFFFFF * 999, 'UTC'), toDateTime32(0xFFFFFFFF * 999, 'UTC'),
       CAST(0xFFFFFFFF * 999 AS DateTime('UTC')), FROM_UNIXTIME(0xFFFFFFFF * 999, 'UTC'),
       toDateTime(toUInt64(0xFFFFFFFF * 999), 'UTC'), toDateTime32(toUInt64(0xFFFFFFFF * 999), 'UTC'),
       CAST(toUInt64(0xFFFFFFFF * 999) AS DateTime('UTC')), FROM_UNIXTIME(toUInt64(0xFFFFFFFF * 999), 'UTC');
SELECT toDateTime(0xFFFFFFFF * 1000, 'UTC'), toDateTime32(0xFFFFFFFF * 1000, 'UTC'),
       CAST(0xFFFFFFFF * 1000 AS DateTime('UTC')), FROM_UNIXTIME(0xFFFFFFFF * 1000, 'UTC'),
       toDateTime(toUInt64(0xFFFFFFFF * 1000), 'UTC'), toDateTime32(toUInt64(0xFFFFFFFF * 1000), 'UTC'),
       CAST(toUInt64(0xFFFFFFFF * 1000) AS DateTime('UTC')), FROM_UNIXTIME(toUInt64(0xFFFFFFFF * 1000), 'UTC');
SELECT toDateTime(0xFFFFFFFF * 1001, 'UTC'), toDateTime32(0xFFFFFFFF * 1001, 'UTC'),
       CAST(0xFFFFFFFF * 1001 AS DateTime('UTC')), FROM_UNIXTIME(0xFFFFFFFF * 1001, 'UTC'),
       toDateTime(toUInt64(0xFFFFFFFF * 1001), 'UTC'), toDateTime32(toUInt64(0xFFFFFFFF * 1001), 'UTC'),
       CAST(toUInt64(0xFFFFFFFF * 1001) AS DateTime('UTC')), FROM_UNIXTIME(toUInt64(0xFFFFFFFF * 1001), 'UTC');


SET adaptive_type_cast=0;
SELECT 'Disable adaptive type cast';

SELECT toDateTime(0xFFFFFFFF - 1, 'UTC');
SELECT toDateTime(0xFFFFFFFF, 'UTC');
SELECT toDateTime(0xFFFFFFFF + 1);  -- {serverError 69}
SELECT toDateTime(0xFFFFFFFF + 1, 'UTC');  -- {serverError 69}
