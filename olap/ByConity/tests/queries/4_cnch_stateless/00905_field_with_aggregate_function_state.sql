select sumMerge((select sumState(1)));
select sumMerge((select sumState(number) from (select * from system.numbers limit 10)));
select quantileMerge((select quantileState(0.5)(number) from (select * from system.numbers limit 10)));

