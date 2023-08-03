
SET enable_optimizer = 1;

DROP TABLE IF EXISTS tob_apps;
CREATE TABLE tob_apps
(
    `app_id` UInt32,
    `app_name` String,
    `app_version` String,
    `device_id` String,
    `device_model` String,
    `device_brand` String,
    `hash_uid` UInt64,
    `os_name` String,
    `os_version` String,
    `network_type` String,
    `network_carrier` String,
    `app_channel` String,
    `user_register_ts` Nullable(UInt64),
    `server_time` UInt64,
    `time` UInt64,
    `event` String,
    `user_unique_id` String,
    `event_date` Date,
    `ab_version` Array(Int32),
    `tea_app_id` UInt32,
    `app_region` String,
    `region` String,
    `app_language` String,
    `language` String,
    `gender` String,
    `age` String,
    `user_id` String,
    `ssid` String,
    `content` String
)
ENGINE = CnchMergeTree()
PARTITION BY (toStartOfDay(`event_date`), `tea_app_id`)
PRIMARY KEY (`tea_app_id`, `event`, `event_date`, `hash_uid`, `user_unique_id`)
ORDER BY (`tea_app_id`, `event`, `event_date`, `hash_uid`, `user_unique_id`)
SETTINGS index_granularity = 8192;

INSERT INTO tob_apps VALUES (0, '11', '22', '33', '44', '55', 0, '11', '22', '33', '44', '55', 1000, 0, 0, '11', '22', '2020.1.1', [1, 2, 3], 0, '', '', '', '', '', '', '', '', '');

SELECT
    path,
    count() AS cnt
FROM
    (
        SELECT arrayJoin(pathSplit(600000, 10)(1000, 1 AS e, '/path')) AS path
        FROM tob_apps AS et
        GROUP BY hash_uid
    )
GROUP BY path
    SETTINGS distributed_perfect_shard = 1;

SELECT pathCount(10, 10)(path, 1, cnt) AS pathfind
FROM
    (
        SELECT
            path,
            count() AS cnt
        FROM
            (
                SELECT arrayJoin(pathSplit(600000, 10)(multiIf(server_time < 1609948800, server_time, time > 2000000000, toUInt32(time / 1000), time), multiIf(event = 'goods_pay_success', 1, event = 'goods_pay_success', 2, event = 'goods_pay_success', 3, 0) AS e, '')) AS path
                FROM tob_apps AS et
                WHERE (tea_app_id = 173715) AND ((event = 'goods_pay_success') OR (event = 'goods_pay_success') OR (event = 'goods_pay_success')) AND ((event_date >= '2022-06-22') AND (event_date <= '2022-06-28') AND (multiIf(server_time < 1609948800, server_time, time > 2000000000, toUInt32(time / 1000), time) >= 1655827200) AND (multiIf(server_time < 1609948800, server_time, time > 2000000000, toUInt32(time / 1000), time) <= 1656431999))
                GROUP BY hash_uid
            )
        GROUP BY path
            SETTINGS distributed_perfect_shard = 1
    );

DROP TABLE tob_apps;
