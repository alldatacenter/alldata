select (1 ? ('abc') : 'def') = 'abc';
select (1 ? toFixedString('abc', 3) : 'def') = 'abc';
select (1 ? toFixedString('abc', 3) : toFixedString('def', 3)) = 'abc';
select (1 ? ('abc') : toFixedString('def', 3)) = 'abc';

select (1 ? (today()) : yesterday()) = today();

select (1 ? (now()) : now() - 1) = now();

select (1 ? (toUInt8(0)) : toUInt8(1)) = toUInt8(0);
select (1 ? (toUInt16(0)) : toUInt8(1)) = toUInt16(0);
select (1 ? (toUInt32(0)) : toUInt8(1)) = toUInt32(0);
select (1 ? (toUInt64(0)) : toUInt8(1)) = toUInt64(0);
select (1 ? (toInt8(0)) : toUInt8(1)) = toInt8(0);
select (1 ? (toInt16(0)) : toUInt8(1)) = toInt16(0);
select (1 ? (toInt32(0)) : toUInt8(1)) = toInt32(0);
select (1 ? (toInt64(0)) : toUInt8(1)) = toInt64(0);

select (1 ? (toUInt8(0)) : toUInt16(1)) = toUInt8(0);
select (1 ? (toUInt16(0)) : toUInt16(1)) = toUInt16(0);
select (1 ? (toUInt32(0)) : toUInt16(1)) = toUInt32(0);
select (1 ? (toUInt64(0)) : toUInt16(1)) = toUInt64(0);
select (1 ? (toInt8(0)) : toUInt16(1)) = toInt8(0);
select (1 ? (toInt16(0)) : toUInt16(1)) = toInt16(0);
select (1 ? (toInt32(0)) : toUInt16(1)) = toInt32(0);
select (1 ? (toInt64(0)) : toUInt16(1)) = toInt64(0);

select (1 ? (toUInt8(0)) : toUInt32(1)) = toUInt8(0);
select (1 ? (toUInt16(0)) : toUInt32(1)) = toUInt16(0);
select (1 ? (toUInt32(0)) : toUInt32(1)) = toUInt32(0);
select (1 ? (toUInt64(0)) : toUInt32(1)) = toUInt64(0);
select (1 ? (toInt8(0)) : toUInt32(1)) = toInt8(0);
select (1 ? (toInt16(0)) : toUInt32(1)) = toInt16(0);
select (1 ? (toInt32(0)) : toUInt32(1)) = toInt32(0);
select (1 ? (toInt64(0)) : toUInt32(1)) = toInt64(0);

select (1 ? (toUInt8(0)) : toUInt64(1)) = toUInt8(0);
select (1 ? (toUInt16(0)) : toUInt64(1)) = toUInt16(0);
select (1 ? (toUInt32(0)) : toUInt64(1)) = toUInt32(0);
select (1 ? (toUInt64(0)) : toUInt64(1)) = toUInt64(0);
--select (1 ? (toInt8(0)) : toUInt64(1)) = toInt8(0);
--select (1 ? (toInt16(0)) : toUInt64(1)) = toInt16(0);
--select (1 ? (toInt32(0)) : toUInt64(1)) = toInt32(0);
--select (1 ? (toInt64(0)) : toUInt64(1)) = toInt64(0);

select (1 ? (toUInt8(0)) : toInt8(1)) = toUInt8(0);
select (1 ? (toUInt16(0)) : toInt8(1)) = toUInt16(0);
select (1 ? (toUInt32(0)) : toInt8(1)) = toUInt32(0);
--select (1 ? (toUInt64(0)) : toInt8(1)) = toUInt64(0);
select (1 ? (toInt8(0)) : toInt8(1)) = toInt8(0);
select (1 ? (toInt16(0)) : toInt8(1)) = toInt16(0);
select (1 ? (toInt32(0)) : toInt8(1)) = toInt32(0);
select (1 ? (toInt64(0)) : toInt8(1)) = toInt64(0);

select (1 ? (toUInt8(0)) : toInt16(1)) = toUInt8(0);
select (1 ? (toUInt16(0)) : toInt16(1)) = toUInt16(0);
select (1 ? (toUInt32(0)) : toInt16(1)) = toUInt32(0);
--select (1 ? (toUInt64(0)) : toInt16(1)) = toUInt64(0);
select (1 ? (toInt8(0)) : toInt16(1)) = toInt8(0);
select (1 ? (toInt16(0)) : toInt16(1)) = toInt16(0);
select (1 ? (toInt32(0)) : toInt16(1)) = toInt32(0);
select (1 ? (toInt64(0)) : toInt16(1)) = toInt64(0);

select (1 ? (toUInt8(0)) : toInt32(1)) = toUInt8(0);
select (1 ? (toUInt16(0)) : toInt32(1)) = toUInt16(0);
select (1 ? (toUInt32(0)) : toInt32(1)) = toUInt32(0);
--select (1 ? (toUInt64(0)) : toInt32(1)) = toUInt64(0);
select (1 ? (toInt8(0)) : toInt32(1)) = toInt8(0);
select (1 ? (toInt16(0)) : toInt32(1)) = toInt16(0);
select (1 ? (toInt32(0)) : toInt32(1)) = toInt32(0);
select (1 ? (toInt64(0)) : toInt32(1)) = toInt64(0);

select (1 ? (toUInt8(0)) : toInt64(1)) = toUInt8(0);
select (1 ? (toUInt16(0)) : toInt64(1)) = toUInt16(0);
select (1 ? (toUInt32(0)) : toInt64(1)) = toUInt32(0);
--select (1 ? (toUInt64(0)) : toInt64(1)) = toUInt64(0);
select (1 ? (toInt8(0)) : toInt64(1)) = toInt8(0);
select (1 ? (toInt16(0)) : toInt64(1)) = toInt16(0);
select (1 ? (toInt32(0)) : toInt64(1)) = toInt32(0);
select (1 ? (toInt64(0)) : toInt64(1)) = toInt64(0);
