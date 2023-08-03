SET max_query_cpu_second = 3;
SELECT count() FROM system.numbers; -- { serverError 159 }
select count() from numbers(40);