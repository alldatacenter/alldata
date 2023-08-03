SELECT '--hex--';
SELECT hex(0);
SELECT hex(42);
select hex(18446744073709551615); -- 2^64 - 1
select hex(9223372036854775808); -- 2^63
SELECT hex(42.42);
SELECT hex('mooncake');
SELECT hex(toDate('2020-08-20'));
SELECT hex(toDateTime('2021-08-20 07:35:16'));
SELECT hex(toDecimal64(42,2));
SELECT hex(sum(number)) as hex_presentation FROM numbers(1,64);
-- select hex(-9223372036854775807); -- Error 43

SELECT '--unhex--';
SELECT reinterpretAsUInt64(reverse(unhex(hex(42))));
SELECT reinterpretAsUInt64(reverse(unhex(hex(18446744073709551615))));
SELECT reinterpretAsUInt64(reverse(unhex(hex(9223372036854775808))));
SELECT reinterpretAsFloat64(unhex(hex(toFloat64(42.42))));
SELECT reinterpretAsFloat32(unhex(hex(toFloat32(42.42))));
SELECT unhex(hex('mooncake'));
SELECT reinterpretAsDate(reverse(unhex(hex(toDate('2020-08-20')))));
SELECT reinterpretAsDateTime(reverse(unhex(hex(toDateTime('2021-08-20 07:35:16')))));
SELECT reinterpretAsUInt64(unhex(hex(toDecimal64(42,2))));