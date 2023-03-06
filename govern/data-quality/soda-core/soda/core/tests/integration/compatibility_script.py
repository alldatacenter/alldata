import os

import psycopg2


def create_connection():
    return psycopg2.connect(
        user=os.getenv("POSTGRES_USERNAME", "sodasql"),
        password=os.getenv("POSTGRES_PASSWORD"),
        host=os.getenv("POSTGRES_HOST", "localhost"),
        port=os.getenv("POSTGRES_PORT"),
        database=os.getenv("POSTGRES_DATABASE", "sodasql"),
    )


connection = create_connection()


def execute(sql: str):
    cursor = connection.cursor()
    try:
        print(f"Executing{sql}")

        cursor.execute(sql)
        connection.commit()
    finally:
        cursor.close()


execute(
    """
        DROP SCHEMA IF EXISTS dev_tom CASCADE
    """
)

execute(
    """
        CREATE SCHEMA IF NOT EXISTS dev_tom AUTHORIZATION CURRENT_USER
    """
)

execute(
    """
        CREATE TABLE dev_tom.SODATEST_Customers_a0344266 (
             id VARCHAR(255),
             cst_size FLOAT,
             cst_size_txt VARCHAR(255),
             distance INT,
             pct VARCHAR(255),
             cat VARCHAR(255),
             country VARCHAR(255),
             zip VARCHAR(255),
             email VARCHAR(255),
             date_updated DATE,
             ts TIMESTAMP,
             ts_with_tz TIMESTAMPTZ
           )
    """
)

execute(
    """
        INSERT INTO dev_tom.SODATEST_Customers_a0344266 VALUES
             ('ID1',1,'1',0,'- 28,42 %','HIGH','BE','2360','john.doe@example.com',DATE '2020-06-23','2020-06-23T00:00:10','2020-06-23T00:00:10+00:00'),
             ('ID2',0.5,'.5',-999,'+22,75 %','HIGH','BE','2361','JOE.SMOE@EXAMPLE.COM',DATE '2020-06-23','2020-06-23T00:01:10','2020-06-23T00:01:10+00:00'),
             ('ID3',-1.2,'-1.2',5,'.92 %','MEDIUM','BE','2362','milan.lukáč@example.com',DATE '2020-06-23','2020-06-23T00:02:10','2020-06-23T00:02:10+00:00'),
             ('ID4',-0.4,'-.4',10,'0.26 %','LOW','BE','2363','john.doe@ĚxamplÉ.com',DATE '2020-06-23','2020-06-23T00:03:10','2020-06-23T00:03:10+00:00'),
             ('ID5',-3,'-3',999,'18,32%',NULL,'BE','2364','invalid@email',DATE '2020-06-23','2020-06-23T00:04:10','2020-06-23T00:04:10+00:00'),
             ('ID6',5,'5',999,'18,32%',NULL,'BE','2365',NULL,DATE '2020-06-23','2020-06-23T00:05:10','2020-06-23T00:05:10+00:00'),
             ('ID7',6,'6',999,'error',NULL,'NL','2360',NULL,DATE '2020-06-24','2020-06-24T00:01:10','2020-06-24T00:01:10+00:00'),
             ('ID8',NULL,NULL,999,'No value',NULL,'NL','2361',NULL,DATE '2020-06-24','2020-06-24T00:02:10','2020-06-24T00:02:10+00:00'),
             ('ID9',NULL,NULL,999,'N/A',NULL,'NL','2362',NULL,DATE '2020-06-24','2020-06-24T00:03:10','2020-06-24T00:03:10+00:00'),
             (NULL,NULL,NULL,NULL,NULL,'HIGH','NL','2363',NULL,DATE '2020-06-24','2020-06-24T00:04:10','2020-06-24T00:04:10+00:00');
    """
)

execute(
    """
        CREATE TABLE dev_tom.SODATEST_Orders_f7532be6 (
             id VARCHAR(255),
             customer_id_nok VARCHAR(255),
             customer_id_ok VARCHAR(255),
             customer_country VARCHAR(255),
             customer_zip VARCHAR(255),
             text VARCHAR(255)
           )
    """
)

execute(
    """
        INSERT INTO dev_tom.SODATEST_Orders_f7532be6 VALUES
             ('O1','ID1','ID1','BE','2360','one'),
             ('O2','ID99','ID1','BE','2360','two'),
             ('O3','ID1','ID2','BE','2000','three'),
             ('O4',NULL,'ID1','BE',NULL,'four'),
             ('O5','ID98','ID4',NULL,'2360','five'),
             ('O6','ID99','ID1','UK','2360','six'),
             (NULL,NULL,'ID3',NULL,NULL,'seven');
    """
)

# SODATEST_Customers_a0344266.aggregation[0]
execute(
    r"""
        SELECT
          COUNT(*),
          COUNT(CASE WHEN cat = 'HIGH' THEN 1 END),
          COUNT(CASE WHEN pct IS NULL THEN 1 END),
          COUNT(CASE WHEN NOT (pct IS NULL) AND NOT (pct ~ '^ *[-+]? *(\d+([\.,]\d+)?|([\.,]\d+)) *% *$') THEN 1 END),
          MIN(LENGTH(cat)),
          MAX(LENGTH(cat)),
          AVG(LENGTH(cat)),
          STDDEV(cst_size),
          STDDEV_POP(cst_size),
          STDDEV_SAMP(cst_size),
          VARIANCE(cst_size),
          VAR_POP(cst_size),
          VAR_SAMP(cst_size),
          PERCENTILE_DISC(0.7) WITHIN GROUP (ORDER BY distance),
          MAX(ts)
        FROM dev_tom.SODATEST_Customers_a0344266
    """
)

# pct.failed_rows[missing_count]
execute(
    """
        SELECT *
        FROM dev_tom.SODATEST_Customers_a0344266
        WHERE pct IS NULL
    """
)

# pct.failed_rows[invalid_count]
execute(
    r"""
        SELECT *
        FROM dev_tom.SODATEST_Customers_a0344266
        WHERE NOT pct IS NULL AND NOT pct ~ '^ *[-+]? *(\d+([\.,]\d+)?|([\.,]\d+)) *% *$'
    """
)

# SODATEST_Customers_a0344266.schema[SODATEST_Customers_a0344266]
execute(
    """
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE lower(table_name) = 'sodatest_customers_a0344266'
          AND lower(table_catalog) = 'sodasql'
          AND lower(table_schema) = 'dev_tom'
        ORDER BY ORDINAL_POSITION
    """
)

# reference[customer_id_nok]
execute(
    """
        SELECT SOURCE.*
        FROM dev_tom.SODATEST_Orders_f7532be6 as SOURCE
             LEFT JOIN dev_tom.SODATEST_Customers_a0344266 as TARGET on SOURCE.customer_id_nok = TARGET.id
        WHERE TARGET.id IS NULL
    """
)

# discover-tables-find-tables-and-row-counts
execute(
    """
        SELECT relname, n_live_tup
        FROM pg_stat_user_tables
        WHERE (lower(relname) like 'sodatest_customers_a0344266')
              AND lower(schemaname) = 'dev_tom'
    """
)

# discover-tables-column-metadata-for-sodatest_customers_a0344266
execute(
    """
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE lower(table_name) = 'sodatest_customers_a0344266'
          AND lower(table_catalog) = 'sodasql'
          AND lower(table_schema) = 'dev_tom'
        ORDER BY ORDINAL_POSITION
    """
)

# get_table_names
execute(
    """
        SELECT table_name
        FROM information_schema.tables
        WHERE (lower(table_name) like 'sodatest_customers_a0344266')
              AND lower(table_schema) = 'dev_tom'
    """
)

# SODATEST_Customers_a0344266.aggregation[0]
execute(
    r"""
        SELECT
          COUNT(*),
          COUNT(CASE WHEN cat = 'HIGH' THEN 1 END),
          COUNT(CASE WHEN pct IS NULL THEN 1 END),
          COUNT(CASE WHEN NOT (pct IS NULL) AND NOT (pct ~ '^ *[-+]? *(\d+([\.,]\d+)?|([\.,]\d+)) *% *$') THEN 1 END),
          MIN(LENGTH(cat)),
          MAX(LENGTH(cat)),
          AVG(LENGTH(cat)),
          STDDEV(cst_size),
          STDDEV_POP(cst_size),
          STDDEV_SAMP(cst_size),
          VARIANCE(cst_size),
          VAR_POP(cst_size),
          VAR_SAMP(cst_size),
          PERCENTILE_DISC(0.7) WITHIN GROUP (ORDER BY distance),
          MAX(ts)
        FROM dev_tom.SODATEST_Customers_a0344266
    """
)

# pct.failed_rows[missing_count]
execute(
    """
        SELECT *
        FROM dev_tom.SODATEST_Customers_a0344266
        WHERE pct IS NULL
    """
)

# pct.failed_rows[invalid_count]
execute(
    r"""
        SELECT *
        FROM dev_tom.SODATEST_Customers_a0344266
        WHERE NOT pct IS NULL AND NOT pct ~ '^ *[-+]? *(\d+([\.,]\d+)?|([\.,]\d+)) *% *$'
    """
)

# sodatest_customers_a0344266.aggregation[0]
execute(
    """
        SELECT
          COUNT(*)
        FROM dev_tom.sodatest_customers_a0344266
    """
)

# SODATEST_Customers_a0344266.schema[SODATEST_Customers_a0344266]
execute(
    """
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE lower(table_name) = 'sodatest_customers_a0344266'
          AND lower(table_catalog) = 'sodasql'
          AND lower(table_schema) = 'dev_tom'
        ORDER BY ORDINAL_POSITION
    """
)

# reference[customer_id_nok]
execute(
    """
        SELECT SOURCE.*
        FROM dev_tom.SODATEST_Orders_f7532be6 as SOURCE
             LEFT JOIN dev_tom.SODATEST_Customers_a0344266 as TARGET on SOURCE.customer_id_nok = TARGET.id
        WHERE TARGET.id IS NULL
    """
)

# SODATEST_Customers_a0344266.aggregation[0]
execute(
    r"""
        SELECT
          COUNT(*),
          COUNT(CASE WHEN cat = 'HIGH' THEN 1 END),
          COUNT(CASE WHEN pct IS NULL THEN 1 END),
          COUNT(CASE WHEN NOT (pct IS NULL) AND NOT (pct ~ '^ *[-+]? *(\d+([\.,]\d+)?|([\.,]\d+)) *% *$') THEN 1 END),
          MIN(LENGTH(cat)),
          MAX(LENGTH(cat)),
          AVG(LENGTH(cat)),
          STDDEV(cst_size),
          STDDEV_POP(cst_size),
          STDDEV_SAMP(cst_size),
          VARIANCE(cst_size),
          VAR_POP(cst_size),
          VAR_SAMP(cst_size),
          PERCENTILE_DISC(0.7) WITHIN GROUP (ORDER BY distance),
          MAX(ts)
        FROM dev_tom.SODATEST_Customers_a0344266
    """
)

# pct.failed_rows[missing_count]
execute(
    """
        SELECT *
        FROM dev_tom.SODATEST_Customers_a0344266
        WHERE pct IS NULL
    """
)

# pct.failed_rows[invalid_count]
execute(
    r"""
        SELECT *
        FROM dev_tom.SODATEST_Customers_a0344266
        WHERE NOT pct IS NULL AND NOT pct ~ '^ *[-+]? *(\d+([\.,]\d+)?|([\.,]\d+)) *% *$'
    """
)

# sodatest_customers_a0344266.aggregation[0]
execute(
    """
        SELECT
          COUNT(*)
        FROM dev_tom.sodatest_customers_a0344266
    """
)

# SODATEST_Customers_a0344266.schema[SODATEST_Customers_a0344266]
execute(
    """
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE lower(table_name) = 'sodatest_customers_a0344266'
          AND lower(table_catalog) = 'sodasql'
          AND lower(table_schema) = 'dev_tom'
        ORDER BY ORDINAL_POSITION
    """
)

# reference[customer_id_nok]
execute(
    """
        SELECT SOURCE.*
        FROM dev_tom.SODATEST_Orders_f7532be6 as SOURCE
             LEFT JOIN dev_tom.SODATEST_Customers_a0344266 as TARGET on SOURCE.customer_id_nok = TARGET.id
        WHERE TARGET.id IS NULL
    """
)

# sodatest_customers_a0344266.schema[sodatest_customers_a0344266]
execute(
    """
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE lower(table_name) = 'sodatest_customers_a0344266'
          AND lower(table_catalog) = 'sodasql'
          AND lower(table_schema) = 'dev_tom'
        ORDER BY ORDINAL_POSITION
    """
)

# profile-columns-get-tables-and-row-counts
execute(
    """
        SELECT relname, n_live_tup
        FROM pg_stat_user_tables
        WHERE (lower(relname) like 'sodatest_customers_a0344266' OR lower(relname) like 'sodatest_customers_a0344266')
              AND lower(schemaname) = 'dev_tom'
    """
)

# profile-columns-get-column-metadata-for-sodatest_customers_a0344266
execute(
    """
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE lower(table_name) = 'sodatest_customers_a0344266'
          AND lower(table_catalog) = 'sodasql'
          AND lower(table_schema) = 'dev_tom'
          AND (lower(column_name) LIKE lower('cst_size_txt') OR lower(column_name) LIKE lower('cst_size'))
        ORDER BY ORDINAL_POSITION
    """
)

# profiling-sodatest_customers_a0344266-size-value-frequencies-numeric
execute(
    """
        WITH
            value_frequencies AS (
                SELECT \"cst_size\" AS value_, count(*) AS frequency_
                FROM dev_tom.sodatest_customers_a0344266
                WHERE \"cst_size\" IS NOT NULL
                GROUP BY \"cst_size\"
            ),
            mins AS (
                SELECT CAST('mins' AS VARCHAR) AS metric_, ROW_NUMBER() OVER(ORDER BY value_ ASC) AS index_, value_, frequency_
                FROM value_frequencies
                WHERE value_ IS NOT NULL
                ORDER BY value_ ASC
                LIMIT 5
            ),
            maxs AS (
                SELECT CAST('maxs' AS VARCHAR) AS metric_, ROW_NUMBER() OVER(ORDER BY value_ DESC) AS index_, value_, frequency_
                FROM value_frequencies
                WHERE value_ IS NOT NULL
                ORDER BY value_ DESC
                LIMIT 5
            ),
            frequent_values AS (
                SELECT CAST('frequent_values' AS VARCHAR) AS metric_, ROW_NUMBER() OVER(ORDER BY frequency_ DESC) AS index_, value_, frequency_
                FROM value_frequencies
                ORDER BY frequency_ desc
                LIMIT 10
            ),
            result AS (
                SELECT * FROM mins
                UNION
                SELECT * FROM maxs
                UNION
                SELECT * FROM frequent_values
            )
        SELECT *
        FROM result
        ORDER BY metric_ ASC, index_ ASC
    """
)

# profiling-sodatest_customers_a0344266-size-profiling-aggregates
execute(
    """
        SELECT
            avg(\"cst_size\") as average
            , sum(\"cst_size\") as sum
            , variance(\"cst_size\") as variance
            , stddev(\"cst_size\") as standard_deviation
            , count(distinct(\"cst_size\")) as distinct_values
            , sum(case when \"cst_size\" is null then 1 else 0 end) as missing_values
        FROM dev_tom.sodatest_customers_a0344266
    """
)

# profiling-sodatest_customers_a0344266-size-histogram
execute(
    """
        WITH
                       value_frequencies AS (
                                   SELECT \"cst_size\" AS value_, count(*) AS frequency_
                                   FROM dev_tom.sodatest_customers_a0344266
                                   WHERE \"cst_size\" IS NOT NULL
                                   GROUP BY \"cst_size\"
                               )
                   SELECT SUM(CASE WHEN value_ < -2.55 THEN frequency_ END),
        SUM(CASE WHEN -2.55 <= value_ AND value_ < -2.1 THEN frequency_ END),
        SUM(CASE WHEN -2.1 <= value_ AND value_ < -1.65 THEN frequency_ END),
        SUM(CASE WHEN -1.65 <= value_ AND value_ < -1.2 THEN frequency_ END),
        SUM(CASE WHEN -1.2 <= value_ AND value_ < -0.75 THEN frequency_ END),
        SUM(CASE WHEN -0.75 <= value_ AND value_ < -0.3 THEN frequency_ END),
        SUM(CASE WHEN -0.3 <= value_ AND value_ < 0.15 THEN frequency_ END),
        SUM(CASE WHEN 0.15 <= value_ AND value_ < 0.6 THEN frequency_ END),
        SUM(CASE WHEN 0.6 <= value_ AND value_ < 1.05 THEN frequency_ END),
        SUM(CASE WHEN 1.05 <= value_ AND value_ < 1.5 THEN frequency_ END),
        SUM(CASE WHEN 1.5 <= value_ AND value_ < 1.95 THEN frequency_ END),
        SUM(CASE WHEN 1.95 <= value_ AND value_ < 2.4 THEN frequency_ END),
        SUM(CASE WHEN 2.4 <= value_ AND value_ < 2.85 THEN frequency_ END),
        SUM(CASE WHEN 2.85 <= value_ AND value_ < 3.3 THEN frequency_ END),
        SUM(CASE WHEN 3.3 <= value_ AND value_ < 3.75 THEN frequency_ END),
        SUM(CASE WHEN 3.75 <= value_ AND value_ < 4.2 THEN frequency_ END),
        SUM(CASE WHEN 4.2 <= value_ AND value_ < 4.65 THEN frequency_ END),
        SUM(CASE WHEN 4.65 <= value_ AND value_ < 5.1 THEN frequency_ END),
        SUM(CASE WHEN 5.1 <= value_ AND value_ < 5.55 THEN frequency_ END),
        SUM(CASE WHEN 5.55 <= value_ THEN frequency_ END)
                   FROM value_frequencies
    """
)

# profiling-sodatest_customers_a0344266-cst_size_txt-value-frequencies-text
execute(
    """
        WITH
            value_frequencies AS (
                SELECT \"cst_size_txt\" AS value_, count(*) AS frequency_
                FROM dev_tom.sodatest_customers_a0344266
                WHERE \"cst_size_txt\" IS NOT NULL
                GROUP BY \"cst_size_txt\"
            ),
            frequent_values AS (
                SELECT CAST('frequent_values' AS VARCHAR) AS metric_, ROW_NUMBER() OVER(ORDER BY frequency_ DESC) AS index_, value_, frequency_
                FROM value_frequencies
                ORDER BY frequency_ desc
                LIMIT 10
            )
        SELECT *
        FROM frequent_values
        ORDER BY metric_ ASC, index_ ASC
    """
)

# profiling: sodatest_customers_a0344266, cst_size_txt: get textual aggregates
execute(
    """
        SELECT
            count(distinct(\"cst_size_txt\")) as distinct_values
            , sum(case when \"cst_size_txt\" is null then 1 else 0 end) as missing_values
            , avg(length(\"cst_size_txt\")) as avg_length
            , min(length(\"cst_size_txt\")) as min_length
            , max(length(\"cst_size_txt\")) as max_length
        FROM dev_tom.sodatest_customers_a0344266
    """
)

execute(
    """
        DROP SCHEMA IF EXISTS dev_tom CASCADE
    """
)
