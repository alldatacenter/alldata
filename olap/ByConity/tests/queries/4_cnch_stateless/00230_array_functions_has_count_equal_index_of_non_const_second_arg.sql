select has([0], 0);
select has([0], materialize(0));
select has(materialize([0]), 0);
select has(materialize([0]), materialize(0));

select has([toString(0)], toString(0));
select has([toString(0)], materialize(toString(0)));
select has(materialize([toString(0)]), toString(0));
select has(materialize([toString(0)]), materialize(toString(0)));

select has([toUInt64(0)], number) from system.numbers limit 10;
select has([toUInt64(0)], toUInt64(number % 3)) from system.numbers limit 10;
select has(materialize([toUInt64(0)]), number) from system.numbers limit 10;
select has(materialize([toUInt64(0)]), toUInt64(number % 3)) from system.numbers limit 10;

select has([toString(0)], toString(number)) from system.numbers limit 10;
select has([toString(0)], toString(number % 3)) from system.numbers limit 10;
select has(materialize([toString(0)]), toString(number)) from system.numbers limit 10;
select has(materialize([toString(0)]), toString(number % 3)) from system.numbers limit 10;

select 3 = countEqual([0, 1, 0, 0], 0);
select 3 = countEqual([0, 1, 0, 0], materialize(0));
select 3 = countEqual(materialize([0, 1, 0, 0]), 0);
select 3 = countEqual(materialize([0, 1, 0, 0]), materialize(0));

select 3 = countEqual([0, 1, 0, 0], 0) from system.numbers limit 10;
select 3 = countEqual([0, 1, 0, 0], materialize(0)) from system.numbers limit 10;
select 3 = countEqual(materialize([0, 1, 0, 0]), 0) from system.numbers limit 10;
select 3 = countEqual(materialize([0, 1, 0, 0]), materialize(0)) from system.numbers limit 10;

select 4 = indexOf([0, 1, 2, 3], 3);
select 4 = indexOf([0, 1, 2, 3], materialize(3));
select 4 = indexOf(materialize([0, 1, 2, 3]), 3);
select 4 = indexOf(materialize([0, 1, 2, 3]), materialize(3));

select 4 = indexOf([0, 1, 2, 3], 3) from system.numbers limit 10;
select 4 = indexOf([0, 1, 2, 3], materialize(3)) from system.numbers limit 10;
select 4 = indexOf(materialize([0, 1, 2, 3]), 3) from system.numbers limit 10;
select 4 = indexOf(materialize([0, 1, 2, 3]), materialize(3)) from system.numbers limit 10;
