SELECT '--bin--';
SELECT bin(0);
SELECT bin(42);
select bin(18446744073709551615); -- 2^64 - 1
select bin(9223372036854775808); -- 2^63
SELECT bin(42.42);
SELECT bin('mooncake');
SELECT bin(toDate('2020-08-20'));
SELECT bin(toDateTime('2021-08-20 07:35:16'));
SELECT bin(toDecimal64(42,2));
SELECT bin(sum(number)) as bin_presentation FROM numbers(1,64);
select bin(-9223372036854775807); -- {serverError 43}

SELECT '--unbin--';
SELECT reinterpretAsUInt64(reverse(unbin(bin(42))));
SELECT reinterpretAsUInt64(reverse(unbin(bin(18446744073709551615))));
SELECT reinterpretAsUInt64(reverse(unbin(bin(9223372036854775808))));
SELECT reinterpretAsFloat64(unbin(bin(toFloat64(42.42))));
SELECT reinterpretAsFloat32(unbin(bin(toFloat32(42.42))));
SELECT unbin(bin('mooncake'));
SELECT reinterpretAsDate(reverse(unbin(bin(toDate('2020-08-20')))));
SELECT reinterpretAsDateTime(reverse(unbin(bin(toDateTime('2021-08-20 07:35:16')))));
SELECT reinterpretAsUInt64(unbin(bin(toDecimal64(42,2))));