select histogram(5)(number-10) from (select * from system.numbers limit 20);
select histogram(5)(number) from (select * from system.numbers limit 20);

select round(arrayJoin(histogram(3)(sin(number))).1, 2), round(arrayJoin(histogram(3)(sin(number))).2, 2), round(arrayJoin(histogram(3)(sin(number))).3, 2) from (select * from system.numbers limit 10);
SELECT round(arrayJoin(histogram(1)(sin(number-40))).1, 2), round(arrayJoin(histogram(1)(sin(number-40))).2, 2), round(arrayJoin(histogram(1)(sin(number-40))).3, 2) from (select * from system.numbers limit 80);

SELECT histogram(10)(-2);

select histogramIf(3)(number, number > 11) from (select * from system.numbers limit 10);
