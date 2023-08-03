set enable_optimizer = 1;

drop table if exists t;
create table t(a Int32, b Int32, c Int32) engine = CnchMergeTree() order by a;

select a in (select b from t) from t group by a + 1; -- { serverError 215 }
select (a + 1) in (select b from t) from t group by a + 1;
select a = any (select b from t) from t group by a + 1;  -- { serverError 215 }
select (a + 1) = any (select b from t) from t group by a + 1;

drop table if exists aeolus_data_table_102024_prod;
CREATE TABLE aeolus_data_table_102024_prod
(
    `row_id_kmtq3k` Int64,
    `p_date` Date,
    `adv_id` Nullable(Int64),
    `ad_id` Nullable(Int64),
    `word_id` Nullable(Int64),
    `word` Nullable(String),
    `pre_audit_status` Nullable(Int64),
    `word_status` Nullable(Int64),
    `staff_id` Nullable(Int64),
    `human_check_area` Nullable(String),
    `process_name` Nullable(Int64),
    `first_industry_name` Nullable(String),
    `second_industry_name` Nullable(String),
    `third_industry_name` Nullable(String),
    `customer_name` Nullable(String),
    `company_name` Nullable(String),
    `hit_rule_type` Nullable(Int64),
    `machine_tag` Nullable(String),
    `machine_status` Nullable(String),
    `ready_time` Nullable(Int64),
    `create_time` Nullable(Int64),
    `word_submit_time` Nullable(Int64),
    `category` Nullable(Int64),
    `advertiser_role` Nullable(Int64),
    `log_start_time` Nullable(String),
    `log_create_time` Nullable(String),
    `log_ready_time` Nullable(String),
    `log_id` Nullable(Int64),
    `keyword_source` Nullable(Int64),
    `is_ad_butler` Nullable(Int64),
    `marketing_goal` Nullable(Int64),
    `live_type` Nullable(Int64),
    `image_mode` Nullable(String)
)
engine = CnchMergeTree() order by row_id_kmtq3k;

SELECT
    CAST(multiIf(hit_rule_type = 105, '已审词模型', hit_rule_type = 205, '已审词模型', hit_rule_type = 103, '黑词模型', hit_rule_type = 0, '人审', hit_rule_type = 119, '已审词推人审', NULL), 'Nullable(String)') AS _1700003661890,
    CAST(ad_id, 'Nullable(Int64)') AS _1700003661873,
    CAST(word_id, 'Nullable(Int64)') AS _1700003661874,
    CAST(word, 'Nullable(String)') AS _1700003661863,
    CAST(count(word_id), 'Nullable(Int64)') AS _1700003662698
FROM aeolus_data_table_102024_prod
WHERE (CAST(word_status, 'Nullable(Int64)') = 1) AND (CAST(multiIf(hit_rule_type = 105, '已审词模型', hit_rule_type = 205, '已审词模型', hit_rule_type = 103, '黑词模型', hit_rule_type = 0, '人审', hit_rule_type = 119, '已审词推人审', NULL), 'Nullable(String)') = '黑词模型') AND ((p_date >= '2023-02-07') AND (p_date <= '2023-02-07'))
GROUP BY
    CAST(multiIf(hit_rule_type = 105, '已审词模型', hit_rule_type = 205, '已审词模型', hit_rule_type = 103, '黑词模型', hit_rule_type = 0, '人审', hit_rule_type = 119, '已审词推人审', NULL), 'Nullable(String)'),
    CAST(ad_id, 'Nullable(Int64)'),
    CAST(word_id, 'Nullable(Int64)'),
    CAST(word, 'Nullable(String)')
HAVING (CAST(word_id, 'Nullable(Int64)') GLOBAL IN (
    SELECT _1700003661874
    FROM
    (
        SELECT DISTINCT
            p_date AS _1700003661862,
            CAST(adv_id, 'Nullable(Int64)') AS _1700003661872,
            CAST(ad_id, 'Nullable(Int64)') AS _1700003661873,
            CAST(word_id, 'Nullable(Int64)') AS _1700003661874,
            CAST(word, 'Nullable(String)') AS _1700003661863,
            CAST(customer_name, 'Nullable(String)') AS _1700003661867,
            CAST(first_industry_name, 'Nullable(String)') AS _1700003661864,
            CAST(second_industry_name, 'Nullable(String)') AS _1700003661865,
            CAST(third_industry_name, 'Nullable(String)') AS _1700003661866
        FROM aeolus_data_table_102024_prod
        WHERE ((p_date >= '2023-02-07') AND (p_date <= '2023-02-07')) AND (CAST(human_check_area, 'Nullable(Int64)') = 2) AND (CAST(pre_audit_status, 'Nullable(Int64)') = 1) AND (CAST(multiIf(toString(category) = '1000044', '竞价词审核 队列', toString(category) = '1100061', '竞价词审核队列', toString(category) = '1100060', '竞价词审核队列', toString(category) = '1100068', '竞价词审核队列', toString(category) = '1100061', '机审队列', toString(category) = '1000093', '配对审核队列', toString(category) = '1100062', '配对审核队列', toString(category) = '1001001', '召回队列', toString(category) = '1100100', '配对审核队列', toString(category) = '1000380', '搜索LU队列', toString(category) = '1001000', '动态创意召回队列', toString(category)), 'Nullable(String)') = '1100107')
        ORDER BY _1700003661862 ASC
    )
)) AND (CAST(ad_id, 'Nullable(Int64)') GLOBAL IN (
    SELECT _1700003661873
    FROM
    (
        SELECT DISTINCT
            p_date AS _1700003661862,
            CAST(adv_id, 'Nullable(Int64)') AS _1700003661872,
            CAST(ad_id, 'Nullable(Int64)') AS _1700003661873,
            CAST(word_id, 'Nullable(Int64)') AS _1700003661874,
            CAST(word, 'Nullable(String)') AS _1700003661863,
            CAST(customer_name, 'Nullable(String)') AS _1700003661867,
            CAST(first_industry_name, 'Nullable(String)') AS _1700003661864,
            CAST(second_industry_name, 'Nullable(String)') AS _1700003661865,
            CAST(third_industry_name, 'Nullable(String)') AS _1700003661866
        FROM aeolus_data_table_102024_prod
        WHERE ((p_date >= '2023-02-07') AND (p_date <= '2023-02-07')) AND (CAST(human_check_area, 'Nullable(Int64)') = 2) AND (CAST(pre_audit_status, 'Nullable(Int64)') = 1) AND (CAST(multiIf(toString(category) = '1000044', '竞价词审核 队列', toString(category) = '1100061', '竞价词审核队列', toString(category) = '1100060', '竞价词审核队列', toString(category) = '1100068', '竞价词审核队列', toString(category) = '1100061', '机审队列', toString(category) = '1000093', '配对审核队列', toString(category) = '1100062', '配对审核队列', toString(category) = '1001001', '召回队列', toString(category) = '1100100', '配对审核队列', toString(category) = '1000380', '搜索LU队列', toString(category) = '1001000', '动态创意召回队列', toString(category)), 'Nullable(String)') = '1100107')
        ORDER BY _1700003661862 ASC
    )
))
LIMIT 1000;
