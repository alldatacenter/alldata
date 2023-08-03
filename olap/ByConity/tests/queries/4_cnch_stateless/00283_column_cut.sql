SELECT
    number,
    toString(number),
    range(number) AS arr,
    arrayMap(x -> toString(x), range(number)) AS arr_s,
    arrayMap(x -> range(x), range(number)) AS arr_arr,
    arrayMap(x -> arrayMap(y -> toString(y), x), arrayMap(x -> range(x), range(number))) AS arr_arr_s,
    arrayMap(x -> toFixedString(x, 3), arrayMap(x -> toString(x), range(number))) AS arr_fs
FROM system.numbers
LIMIT 5, 10;
