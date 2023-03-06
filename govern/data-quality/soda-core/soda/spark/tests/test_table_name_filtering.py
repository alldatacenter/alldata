from soda.data_sources.spark_data_source import SparkSQLBase


def test_table_name_filtering():
    table_names = ["aa", "Ab", "abc", "ca", "d"]
    assert table_names is SparkSQLBase._filter_include_exclude(
        table_names=table_names, include_tables=[], exclude_tables=[]
    )
    assert ["aa", "Ab", "abc"] == SparkSQLBase._filter_include_exclude(
        table_names=table_names, include_tables=["a%"], exclude_tables=[]
    )
    assert ["aa", "ca", "d"] == SparkSQLBase._filter_include_exclude(
        table_names=table_names, include_tables=[], exclude_tables=["%b%"]
    )
    assert ["aa"] == SparkSQLBase._filter_include_exclude(
        table_names=table_names, include_tables=["a%", "b", "%x%"], exclude_tables=["%b%", "c", "%x%"]
    )
    assert ["aa", "Ab", "abc"] == SparkSQLBase._filter_include_exclude(
        table_names=table_names, include_tables=["a%"], exclude_tables=["non-existing"]
    )
