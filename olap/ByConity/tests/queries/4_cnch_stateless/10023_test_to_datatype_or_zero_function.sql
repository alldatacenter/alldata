-- string to T
select toInt32OrZero('test'), toTypeName(toInt32OrZero('test'));
select toInt32OrNull('test'), toTypeName(toInt32OrNull('test'));
select toDateOrZero('2020-12'), toTypeName(toDateOrZero('2020-12'));
select toDateOrNull('2020-12'), toTypeName(toDateOrNull('2020-12'));
select toDateTimeOrZero('2020-12', 'Etc/UTC'), toTypeName(toDateTimeOrZero('2020-12'));
select toDateTimeOrNull('2020-12'), toTypeName(toDateTimeOrNull('2020-12'));
select toDateTime64OrZero('2020-12', 3, 'Etc/UTC'), toTypeName(toDateTime64OrZero('2020-12', 3, 'Etc/UTC'));
select toDateTime64OrNull('2020-12', 3, 'Etc/UTC'), toTypeName(toDateTime64OrNull('2020-12', 3, 'Etc/UTC'));
select toUUIDOrZero('test_uuid'), toTypeName(toUUIDOrZero('test_uuid'));
select toUUIDOrNull('test_uuid'), toTypeName(toUUIDOrNull('test_uuid'));
select toFloat32OrZero('1.222_test'), toTypeName(toFloat32OrZero('1.222_test'));
select toFloat32OrNull('1.222_test'), toTypeName(toFloat32OrNull('1.222_test'));
select toDecimal32OrZero('test_decimal', 6), toTypeName(toDecimal32OrZero('test_decimal', 6));
select toDecimal32OrNull('test_decimal', 6), toTypeName(toDecimal32OrNull('test_decimal', 6));
select toFixedStringOrZero('convert_from_string_test', 10), toTypeName(toFixedStringOrZero('convert_from_string_test', 10));
select toFixedStringOrNull('convert_from_string_test', 10), toTypeName(toFixedStringOrNull('convert_from_string_test', 10));

-- int to T
select toInt32OrNull('16'), toTypeName(toInt32OrNull('16'));
select toInt32OrZero('16'), toTypeName(toInt32OrZero('16'));
select toDateOrNull('1'), toTypeName(toDateOrNull('1'));
select toDateOrZero('1'), toTypeName(toDateOrZero('1'));
select toDateTimeOrZero('1', 'Asia/Shanghai'), toTypeName(toDateTimeOrZero('1', 'Asia/Shanghai'));
select toDateTimeOrNull('1', 'Asia/Shanghai'), toTypeName(toDateTimeOrNull('1', 'Asia/Shanghai'));
select toDateTime64OrZero('1', 3, 'Etc/UTC'), toTypeName(toDateTime64OrZero('1', 3, 'Etc/UTC'));
select toDateTime64OrNull('1', 3, 'Etc/UTC'), toTypeName(toDateTime64OrNull('1', 3, 'Etc/UTC'));
select toUUIDOrZero('11'), toTypeName(toUUIDOrZero('11'));
select toUUIDOrNull('1'), toTypeName(toUUIDOrNull('1'));
select toFloat32OrZero('1'), toTypeName(toFloat32OrZero('1'));
select toFloat32OrNull('1'), toTypeName(toFloat32OrNull('1'));
select toDecimal32OrZero('1', 6), toTypeName(toDecimal32OrZero('1', 6));
select toDecimal32OrNull('1', 6), toTypeName(toDecimal32OrNull('1', 6));

-- string convert to fixedString
select toFixedString('test_fixed_string', 10); -- { serverError 131 }
select toFixedStringOrZero('test_fixed_zero', 10);
select toFixedStringOrNull('test_fixed_null', 10);

-- test array
select cast('[1, 2]', 'Array(Int32)');
select cast('[1, 2]', 'Array(String)'); -- { serverError 26 }
select cast('[\'1\', \'2\']', 'Array(String)');
select cast('[\'1\', null]', 'Array(String)'); -- { serverError 26 }
select cast('[\'1\', null]', 'Array(Nullable(String))');
select cast('[[1, 2], [1, null]]', 'Array(Nullable(Array(Int32)))');  -- { serverError 130 }
select cast('[[1, 2], [1, null]]', 'Array(Array(Nullable(Int32)))');
select cast([['1', null], ['a', 'b']], 'Array(Array(Int32))'); -- { serverError 349 }
select cast([['1', null], ['a', 'b']], 'Array(Array(Nullable(Int32)))');

-- test decimal convert Fail
select toDecimal32(pow(2, 62), 8); -- { serverError 407 }
select toDecimal32OrZero(toString(pow(2, 62)), 8);
select toDecimal32OrNull(toString(pow(2, 62)), 8);
select cast(cast(pow(2, 62), 'Nullable(Decimal128(6))'), 'Decimal32(8)'); -- { serverError 407 }
select cast(cast(pow(2, 62), 'Nullable(Decimal128(6))'), 'Nullable(Decimal32(8))'); -- { serverError 407 }

--
select cast(toDateTime('2021-06-18 08:00:00'), 'Date');
select cast(toDateTime('2021-06-18 08:00:00'), 'Nullable(Date)');
select cast(toDateTime64('2021-06-18 08:00:00.000', 3), 'Nullable(Date)');
select cast(toDate('2021-06-18'), 'DateTime');
select cast(toDate('2021-06-18'), 'Nullable(DateTime)');
select cast(toDate('2021-06-18'), 'Nullable(DateTime64(3, \'Europe/Moscow\'))');
select cast(1623974400, 'Date');
select cast(1623974400, 'DateTime(\'UTC\')');
select cast(1623974400, 'DateTime64(3, \'UTC\')');
select cast(1623974400000, 'Date');
select cast(1623974400000, 'DateTime(\'UTC\')') settings adaptive_type_cast=1;
select cast(18796, 'Date');
select cast(18796, 'DateTime(\'UTC\')');
select cast(18796, 'DateTime64(3, \'UTC\')');

--
select cast(-1, 'Date');
select cast(-1, 'DateTime(\'UTC\')');
select cast(-1, 'DateTime64(3, \'UTC\')');
