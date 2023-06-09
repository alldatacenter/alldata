SELECT
    a, b, a = b, a != b, a < b, a > b, a <= b, a >= b,
    fa, fb, fa = fb, fa != fb, fa < fb, fa > fb, fa <= fb, fa >= fb
FROM
(
    SELECT a, b, toFixedString(a, 16) AS fa, toFixedString(b, 16) AS fb
    FROM
        (
            SELECT 'aaaaaaaaaaaaaaaa' AS a
            UNION ALL SELECT 'aaaaaaaaaaaaaaab'
            UNION ALL SELECT 'aaaaaaaaaaaaaaac'
            UNION ALL SELECT 'baaaaaaaaaaaaaaa'
            UNION ALL SELECT 'baaaaaaaaaaaaaab'
            UNION ALL SELECT 'baaaaaaaaaaaaaac'
            UNION ALL SELECT 'aaaaaaaabaaaaaaa'
            UNION ALL SELECT 'aaaaaaabaaaaaaaa'
            UNION ALL SELECT 'aaaaaaacaaaaaaaa'
        )
        CROSS JOIN
        (
            SELECT 'aaaaaaaaaaaaaaaa' AS b
            UNION ALL SELECT 'aaaaaaaaaaaaaaab'
            UNION ALL SELECT 'aaaaaaaaaaaaaaac'
            UNION ALL SELECT 'baaaaaaaaaaaaaaa'
            UNION ALL SELECT 'baaaaaaaaaaaaaab'
            UNION ALL SELECT 'baaaaaaaaaaaaaac'
            UNION ALL SELECT 'aaaaaaaabaaaaaaa'
            UNION ALL SELECT 'aaaaaaabaaaaaaaa'
            UNION ALL SELECT 'aaaaaaacaaaaaaaa'
        )
)
ORDER BY a, b;


SELECT
    a,
    b1,
    b2,
    b3,
    b4,
    b5,
    b6,
    b7,
    b8,
    b9,
    a = b1, a != b1, a < b1, a > b1, a <= b1, a >= b1,
    a = b2, a != b2, a < b2, a > b2, a <= b2, a >= b2,
    a = b3, a != b3, a < b3, a > b3, a <= b3, a >= b3,
    a = b4, a != b4, a < b4, a > b4, a <= b4, a >= b4,
    a = b5, a != b5, a < b5, a > b5, a <= b5, a >= b5,
    a = b6, a != b6, a < b6, a > b6, a <= b6, a >= b6,
    a = b7, a != b7, a < b7, a > b7, a <= b7, a >= b7,
    a = b8, a != b8, a < b8, a > b8, a <= b8, a >= b8,
    a = b9, a != b9, a < b9, a > b9, a <= b9, a >= b9
FROM
(
    SELECT
        toFixedString(a, 16) AS a,
        toFixedString('aaaaaaaaaaaaaaaa', 16) AS b1,
        toFixedString('aaaaaaaaaaaaaaab', 16) AS b2,
        toFixedString('aaaaaaaaaaaaaaac', 16) AS b3,
        toFixedString('baaaaaaaaaaaaaaa', 16) AS b4,
        toFixedString('baaaaaaaaaaaaaab', 16) AS b5,
        toFixedString('baaaaaaaaaaaaaac', 16) AS b6,
        toFixedString('aaaaaaaabaaaaaaa', 16) AS b7,
        toFixedString('aaaaaaabaaaaaaaa', 16) AS b8,
        toFixedString('aaaaaaacaaaaaaaa', 16) AS b9
    FROM
    (
        SELECT 'aaaaaaaaaaaaaaaa' AS a
        UNION ALL SELECT 'aaaaaaaaaaaaaaab'
        UNION ALL SELECT 'aaaaaaaaaaaaaaac'
        UNION ALL SELECT 'baaaaaaaaaaaaaaa'
        UNION ALL SELECT 'baaaaaaaaaaaaaab'
        UNION ALL SELECT 'baaaaaaaaaaaaaac'
        UNION ALL SELECT 'aaaaaaaabaaaaaaa'
        UNION ALL SELECT 'aaaaaaabaaaaaaaa'
        UNION ALL SELECT 'aaaaaaacaaaaaaaa'
    )
)
ORDER BY a;
