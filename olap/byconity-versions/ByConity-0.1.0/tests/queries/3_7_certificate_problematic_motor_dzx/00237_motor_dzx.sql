SET output_format_write_statistics = 0;
SELECT max(p_date) AS p_date FROM dzx.dim_business_ad SETTINGS enable_optimize_predicate_expression=0 FORMAT JSONCompact