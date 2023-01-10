SELECT emptyArrayToSingle(arrayFilter(x -> x != 99, arrayJoin([[1, 2], [99], [4, 5, 6]])));
SELECT emptyArrayToSingle(emptyArrayString()), emptyArrayToSingle(emptyArrayDate()), arrayMap(x->toDateTime(x, 'Etc/UTC'), emptyArrayToSingle(emptyArrayDateTime()));

SELECT
    emptyArrayToSingle(range(number % 3)),
    emptyArrayToSingle(arrayMap(x -> toString(x), range(number % 2))),
    arrayMap(x->toDateTime(x, 'Etc/UTC'), emptyArrayToSingle(arrayMap(x -> toDateTime('2015-01-01 00:00:00', 'Etc/UTC') + x, range(number % 5)))),
    emptyArrayToSingle(arrayMap(x -> toDate('2015-01-01') + x, range(number % 4))) FROM system.numbers LIMIT 10;
