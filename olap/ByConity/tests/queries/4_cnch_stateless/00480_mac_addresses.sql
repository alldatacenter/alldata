SELECT mac_str, mac_num, hex(mac_num), mac_str2, mac_str = mac_str2, oui_num, hex(oui_num), oui_num2, oui_num = oui_num2
FROM
    (
        SELECT '01:02:03:04:05:06' AS mac_str,
               MACStringToNum('01:02:03:04:05:06') AS mac_num,
               MACNumToString(MACStringToNum('01:02:03:04:05:06')) AS mac_str2,
               MACStringToOUI('01:02:03:04:05:06') AS oui_num,
               MACStringToOUI(substring('01:02:03:04:05:06', 1, 8)) AS oui_num2
    );

SELECT mac_str, mac_num, hex(mac_num), mac_str2, mac_str = mac_str2, oui_num, hex(oui_num), oui_num2, oui_num = oui_num2
FROM
    (
        SELECT materialize('01:02:03:04:05:06') AS mac_str,
               MACStringToNum(materialize('01:02:03:04:05:06')) AS mac_num,
               MACNumToString(MACStringToNum(materialize('01:02:03:04:05:06'))) AS mac_str2,
               MACStringToOUI(materialize('01:02:03:04:05:06')) AS oui_num,
               MACStringToOUI(substring(materialize('01:02:03:04:05:06'), 1, 8)) AS oui_num2
    );
