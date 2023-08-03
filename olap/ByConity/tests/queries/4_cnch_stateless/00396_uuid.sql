SELECT uuid_string, hex(UUIDStringToNum(uuid_string)) = hex AS test1, UUIDStringToNum(uuid_string) = bytes AS test2
FROM
(
    SELECT
    '0123456789ABCDEF0123456789ABCDEF' AS hex,
    unhex('0123456789ABCDEF0123456789ABCDEF') AS bytes,
    toFixedString(unhex('0123456789ABCDEF0123456789ABCDEF'), 16) AS uuid_binary,
    UUIDNumToString(toFixedString(unhex('0123456789ABCDEF0123456789ABCDEF'), 16)) AS uuid_string
);

SELECT uuid_string, hex(UUIDStringToNum(uuid_string)) = hex AS test1, UUIDStringToNum(uuid_string) = bytes AS test2
FROM
(
    SELECT
    materialize('0123456789ABCDEF0123456789ABCDEF') AS hex,
    unhex(materialize('0123456789ABCDEF0123456789ABCDEF')) AS bytes,
    toFixedString(unhex(materialize('0123456789ABCDEF0123456789ABCDEF')), 16) AS uuid_binary,
    UUIDNumToString(toFixedString(unhex(materialize('0123456789ABCDEF0123456789ABCDEF')), 16)) AS uuid_string
);

SELECT hex(UUIDStringToNum('01234567-89ab-cdef-0123-456789abcdef'));
SELECT hex(UUIDStringToNum(materialize('01234567-89ab-cdef-0123-456789abcdef')));
SELECT str, UUIDNumToString(UUIDStringToNum(str)), UUIDNumToString(UUIDStringToNum(toFixedString(str, 36))) FROM (SELECT '01234567-89ab-cdef-0123-456789abcdef' AS str);
SELECT str, UUIDNumToString(UUIDStringToNum(str)), UUIDNumToString(UUIDStringToNum(toFixedString(str, 36))) FROM (SELECT materialize('01234567-89ab-cdef-0123-456789abcdef') AS str);
SELECT toString(toUUID('3f1ed72e-f7fe-4459-9cbe-95fe9298f845'));
