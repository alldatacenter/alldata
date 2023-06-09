CREATE DATABASE IF NOT EXISTS test;
USE test;

DROP TABLE IF EXISTS test.aeolus_data_table_8_1014318_prod;

CREATE TABLE test.aeolus_data_table_8_1014318_prod
(`row_id_kmtq3k` Int64,
 `p_date` Date,
 `c2_work_order` Nullable(Int64),
 `advertiser_id` Nullable(Int64),
 `is_value` Nullable(String),
 `ad_last_audit_object_category` Nullable(Int64),
 `second_item` Nullable(String),
 `cc` Nullable(Int64),
 `c2_create_time` Nullable(String))
ENGINE = CnchMergeTree PARTITION BY p_date ORDER BY (`row_id_kmtq3k`, intHash64(`row_id_kmtq3k`)) SAMPLE BY intHash64(`row_id_kmtq3k`) TTL p_date + toIntervalDay(31);

SET enable_optimizer=1, enable_dynamic_filter=0, dialect_type = 'CLICKHOUSE';


SELECT
    _1700023781767,
    table_1700023778093_1d._1700023781767 AS table_1700023778093_1d_1700023781767_store,
    _1700023781767 - table_1700023778093_1d._1700023781767 AS table_1700023778093_1d_1700023781767_diff,
    _1700025903619,
    _1700025903743
FROM
    (
        SELECT
            _1700023781767,
            _1700025903619,
            _1700025903743
        FROM
            (
                SELECT
                    *
                FROM
                    (
                        SELECT
                            (
                                avg(
                                    case
                                        when year((toDate(c2_create_time))) in ('2023')
                                        and month((toDate(c2_create_time))) in (1, 2) then 0.9
                                        else null
                                    end
                                )
                            ) AS _1700025903619
                        FROM
                            test.`aeolus_data_table_8_1014318_prod`
                        WHERE
                            (
                                (
                                    (
                                        (
                                            (p_date >= '2023-01-02')
                                            AND (p_date <= '2023-01-02')
                                        )
                                        AND (
                                            (toDate(c2_create_time) >= '2023-01-01')
                                            AND (toDate(c2_create_time) <= '2023-01-01')
                                        )
                                    )
                                )
                            )
                    )
                    CROSS JOIN (
                        SELECT
                            (_5143b68f89) / (_50500d5c62) AS _1700023781767
                        FROM
                            (
                                SELECT
                                    count(
                                        DISTINCT CASE
                                            WHEN (_d215f6bdaa) <= 1 THEN _34a9736cc0
                                            ELSE null
                                        END
                                    ) AS _5143b68f89
                                FROM
                                    (
                                        SELECT
                                            _34a9736cc0
                                        FROM
                                            (
                                                SELECT
                                                    (c2_work_order) AS _34a9736cc0
                                                FROM
                                                    test.`aeolus_data_table_8_1014318_prod`
                                                WHERE
                                                    (
                                                        (
                                                            (
                                                                (
                                                                    (p_date >= '2023-01-02')
                                                                    AND (p_date <= '2023-01-02')
                                                                )
                                                                AND (
                                                                    (toDate(c2_create_time) >= '2023-01-01')
                                                                    AND (toDate(c2_create_time) <= '2023-01-01')
                                                                )
                                                            )
                                                        )
                                                    )
                                            )
                                    )
                                    LEFT OUTER JOIN (
                                        SELECT
                                            (c2_work_order) AS _34a9736cc0,
                                            (sum(cc)) AS _d215f6bdaa
                                        FROM
                                            test.`aeolus_data_table_8_1014318_prod`
                                        WHERE
                                            (
                                                (
                                                    (
                                                        (p_date >= '2023-01-02')
                                                        AND (p_date <= '2023-01-02')
                                                    )
                                                )
                                            )
                                        GROUP BY
                                            _34a9736cc0
                                    ) USING (_34a9736cc0)
                            )
                            CROSS JOIN (
                                SELECT
                                    (count(DISTINCT c2_work_order)) AS _50500d5c62
                                FROM
                                    test.`aeolus_data_table_8_1014318_prod`
                                WHERE
                                    (
                                        (
                                            (
                                                (
                                                    (p_date >= '2023-01-02')
                                                    AND (p_date <= '2023-01-02')
                                                )
                                                AND (
                                                    (toDate(c2_create_time) >= '2023-01-01')
                                                    AND (toDate(c2_create_time) <= '2023-01-01')
                                                )
                                            )
                                        )
                                    )
                            )
                    )
            )
            CROSS JOIN (
                SELECT
                    CASE
                        WHEN ((_5143b68f89) / (_50500d5c62)) >= (_e5841b5283) THEN '是'
                        ELSE '否'
                    END AS _1700025903743
                FROM
                    (
                        SELECT
                            *
                        FROM
                            (
                                SELECT
                                    count(
                                        DISTINCT CASE
                                            WHEN (_d215f6bdaa) <= 1 THEN _34a9736cc0
                                            ELSE null
                                        END
                                    ) AS _5143b68f89
                                FROM
                                    (
                                        SELECT
                                            _34a9736cc0
                                        FROM
                                            (
                                                SELECT
                                                    (c2_work_order) AS _34a9736cc0
                                                FROM
                                                    test.`aeolus_data_table_8_1014318_prod`
                                                WHERE
                                                    (
                                                        (
                                                            (
                                                                (
                                                                    (p_date >= '2023-01-02')
                                                                    AND (p_date <= '2023-01-02')
                                                                )
                                                                AND (
                                                                    (toDate(c2_create_time) >= '2023-01-01')
                                                                    AND (toDate(c2_create_time) <= '2023-01-01')
                                                                )
                                                            )
                                                        )
                                                    )
                                            )
                                    )
                                    LEFT OUTER JOIN (
                                        SELECT
                                            (c2_work_order) AS _34a9736cc0,
                                            (sum(cc)) AS _d215f6bdaa
                                        FROM
                                            test.`aeolus_data_table_8_1014318_prod`
                                        WHERE
                                            (
                                                (
                                                    (
                                                        (p_date >= '2023-01-02')
                                                        AND (p_date <= '2023-01-02')
                                                    )
                                                )
                                            )
                                        GROUP BY
                                            _34a9736cc0
                                    ) USING (_34a9736cc0)
                            )
                            CROSS JOIN (
                                SELECT
                                    (count(DISTINCT c2_work_order)) AS _50500d5c62
                                FROM
                                    test.`aeolus_data_table_8_1014318_prod`
                                WHERE
                                    (
                                        (
                                            (
                                                (
                                                    (p_date >= '2023-01-02')
                                                    AND (p_date <= '2023-01-02')
                                                )
                                                AND (
                                                    (toDate(c2_create_time) >= '2023-01-01')
                                                    AND (toDate(c2_create_time) <= '2023-01-01')
                                                )
                                            )
                                        )
                                    )
                            )
                    )
                    CROSS JOIN (
                        SELECT
                            (
                                avg(
                                    CASE
                                        WHEN year((toDate(c2_create_time))) in('2023')
                                        and month((toDate(c2_create_time))) in(1, 2) THEN 0.9
                                        ELSE null
                                    END
                                )
                            ) AS _e5841b5283
                        FROM
                            test.`aeolus_data_table_8_1014318_prod`
                        WHERE
                            (
                                (
                                    (
                                        (
                                            (p_date >= '2023-01-02')
                                            AND (p_date <= '2023-01-02')
                                        )
                                        AND (
                                            (toDate(c2_create_time) >= '2023-01-01')
                                            AND (toDate(c2_create_time) <= '2023-01-01')
                                        )
                                    )
                                )
                            )
                    )
            )
    )
    CROSS JOIN (
        SELECT
            _1700023781767,
            _1700025903619,
            _1700025903743
        FROM
            (
                SELECT
                    *
                FROM
                    (
                        SELECT
                            (
                                avg(
                                    case
                                        when year((toDate(c2_create_time))) in ('2023')
                                        and month((toDate(c2_create_time))) in (1, 2) then 0.9
                                        else null
                                    end
                                )
                            ) AS _1700025903619
                        FROM
                            test.`aeolus_data_table_8_1014318_prod`
                        WHERE
                            (
                                (
                                    (
                                        (
                                            (p_date >= '2023-01-02')
                                            AND (p_date <= '2023-01-02')
                                        )
                                        AND (
                                            (toDate(c2_create_time) >= '2022-12-31')
                                            AND (toDate(c2_create_time) <= '2022-12-31')
                                        )
                                    )
                                )
                            )
                    )
                    CROSS JOIN (
                        SELECT
                            (_5143b68f89) / (_50500d5c62) AS _1700023781767
                        FROM
                            (
                                SELECT
                                    count(
                                        DISTINCT CASE
                                            WHEN (_d215f6bdaa) <= 1 THEN _34a9736cc0
                                            ELSE null
                                        END
                                    ) AS _5143b68f89
                                FROM
                                    (
                                        SELECT
                                            _34a9736cc0
                                        FROM
                                            (
                                                SELECT
                                                    (c2_work_order) AS _34a9736cc0
                                                FROM
                                                    test.`aeolus_data_table_8_1014318_prod`
                                                WHERE
                                                    (
                                                        (
                                                            (
                                                                (
                                                                    (p_date >= '2023-01-02')
                                                                    AND (p_date <= '2023-01-02')
                                                                )
                                                                AND (
                                                                    (toDate(c2_create_time) >= '2022-12-31')
                                                                    AND (toDate(c2_create_time) <= '2022-12-31')
                                                                )
                                                            )
                                                        )
                                                    )
                                            )
                                    )
                                    LEFT OUTER JOIN (
                                        SELECT
                                            (c2_work_order) AS _34a9736cc0,
                                            (sum(cc)) AS _d215f6bdaa
                                        FROM
                                            test.`aeolus_data_table_8_1014318_prod`
                                        WHERE
                                            (
                                                (
                                                    (
                                                        (p_date >= '2023-01-02')
                                                        AND (p_date <= '2023-01-02')
                                                    )
                                                )
                                            )
                                        GROUP BY
                                            _34a9736cc0
                                    ) USING (_34a9736cc0)
                            )
                            CROSS JOIN (
                                SELECT
                                    (count(DISTINCT c2_work_order)) AS _50500d5c62
                                FROM
                                    test.`aeolus_data_table_8_1014318_prod`
                                WHERE
                                    (
                                        (
                                            (
                                                (
                                                    (p_date >= '2023-01-02')
                                                    AND (p_date <= '2023-01-02')
                                                )
                                                AND (
                                                    (toDate(c2_create_time) >= '2022-12-31')
                                                    AND (toDate(c2_create_time) <= '2022-12-31')
                                                )
                                            )
                                        )
                                    )
                            )
                    )
            )
            CROSS JOIN (
                SELECT
                    CASE
                        WHEN ((_5143b68f89) / (_50500d5c62)) >= (_e5841b5283) THEN '是'
                        ELSE '否'
                    END AS _1700025903743
                FROM
                    (
                        SELECT
                            *
                        FROM
                            (
                                SELECT
                                    count(
                                        DISTINCT CASE
                                            WHEN (_d215f6bdaa) <= 1 THEN _34a9736cc0
                                            ELSE null
                                        END
                                    ) AS _5143b68f89
                                FROM
                                    (
                                        SELECT
                                            _34a9736cc0
                                        FROM
                                            (
                                                SELECT
                                                    (c2_work_order) AS _34a9736cc0
                                                FROM
                                                    test.`aeolus_data_table_8_1014318_prod`
                                                WHERE
                                                    (
                                                        (
                                                            (
                                                                (
                                                                    (p_date >= '2023-01-02')
                                                                    AND (p_date <= '2023-01-02')
                                                                )
                                                                AND (
                                                                    (toDate(c2_create_time) >= '2022-12-31')
                                                                    AND (toDate(c2_create_time) <= '2022-12-31')
                                                                )
                                                            )
                                                        )
                                                    )
                                            )
                                    )
                                    LEFT OUTER JOIN (
                                        SELECT
                                            (c2_work_order) AS _34a9736cc0,
                                            (sum(cc)) AS _d215f6bdaa
                                        FROM
                                            test.`aeolus_data_table_8_1014318_prod`
                                        WHERE
                                            (
                                                (
                                                    (
                                                        (p_date >= '2023-01-02')
                                                        AND (p_date <= '2023-01-02')
                                                    )
                                                )
                                            )
                                        GROUP BY
                                            _34a9736cc0
                                    ) USING (_34a9736cc0)
                            )
                            CROSS JOIN (
                                SELECT
                                    (count(DISTINCT c2_work_order)) AS _50500d5c62
                                FROM
                                    test.`aeolus_data_table_8_1014318_prod`
                                WHERE
                                    (
                                        (
                                            (
                                                (
                                                    (p_date >= '2023-01-02')
                                                    AND (p_date <= '2023-01-02')
                                                )
                                                AND (
                                                    (toDate(c2_create_time) >= '2022-12-31')
                                                    AND (toDate(c2_create_time) <= '2022-12-31')
                                                )
                                            )
                                        )
                                    )
                            )
                    )
                    CROSS JOIN (
                        SELECT
                            (
                                avg(
                                    CASE
                                        WHEN year((toDate(c2_create_time))) in('2023')
                                        and month((toDate(c2_create_time))) in(1, 2) THEN 0.9
                                        ELSE null
                                    END
                                )
                            ) AS _e5841b5283
                        FROM
                            test.`aeolus_data_table_8_1014318_prod`
                        WHERE
                            (
                                (
                                    (
                                        (
                                            (p_date >= '2023-01-02')
                                            AND (p_date <= '2023-01-02')
                                        )
                                        AND (
                                            (toDate(c2_create_time) >= '2022-12-31')
                                            AND (toDate(c2_create_time) <= '2022-12-31')
                                        )
                                    )
                                )
                            )
                    )
            )
    ) AS table_1700023778093_1d
LIMIT 1000;

DROP TABLE IF EXISTS test.aeolus_data_table_8_1014318_prod;
