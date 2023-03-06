# Contribute support for a datasource

Thanks for considering contributing to Soda Core's library of supported data sources! 

To make a data source available to our user community, we require that you provide the following:
- a **working data source**
    - locally - provide a command to launch a docker container with the data source, either a Docker file or docker-compose file.
    - in the cloud - provide a service account to connect to.
- a **Python library** for the data source connection. Usually, you need to install an existing official connector library.
- a **data source package** that handles the following:
    - get connection properties
    - connect to the data source
    - access any data source-specific code to ensure full support

## Implementation basics

**Datasource file and folder structure**
- The package goes to `soda/xy`, following the same structure as other datasource packages.
- The main file is `soda/xy/soda/data_sources/xy_data_source.py` with a `XyDataSource(DataSource)` class.

**Basic code in the data source class**
- Implement the `__init__` method to retrieve and save connection properties.
- Implement the `connect` method that returns a PEP 249-compatible connection object.

**Required overrides**
- Type mappings; refer to the base DataSource class comments for more detail.
    - `SCHEMA_CHECK_TYPES_MAPPING`
    - `SQL_TYPE_FOR_CREATE_TABLE_MAP`
    - `SQL_TYPE_FOR_SCHEMA_CHECK_MAP`
    - `NUMERIC_TYPES_FOR_PROFILING`
    - `TEXT_TYPES_FOR_PROFILING`
- `safe_connection_data()` method

**Optional overrides, frequent**
- `sql_get_table_names_with_count()` - SQL query to retrieve all tables and their respective counts. This is usually data source-specific.
- `default_casify_*()` - indicates any default case manipulation that a data source does when retrieving respective identifiers.
- Table/column metadata methods
    - `column_metadata_columns()`
    - `column_metadata_catalog_column()`
    - `sql_get_table_names_with_count()`
- Regex support
    - `escape_regex()` or `escape_string()` to ensure correct regex formatting.
    - `regex_replace_flags()` - for data sources that support regex replace flags; for example, `g` for `global`.
- Identifier quoting - `quote_*()` methods handle identifier quoting; `qualified_table_name()` creates a fully-qualified table name.

**Optional overrides, infrequent**
- Any of the `sql_*` methods when a particular data source needs a specific query to get a desired result.

**Further considerations**
- How are schemas (or the equivalent) handled? Can they be set globally for the connection, or do they need to be prefixed in all the queries?



## Test the datasource support

**Required tests**
- Create a `soda/xy/tests/text_xy.py` file with `test_xy()` method. Use this file for any data source-specific tests.
- Implement `XyDataSourceFixture` for everything related to tests:
    - `_build_configuration_dict()` - connection configuration the tests use
    - `_create_schema_if_not_exists_sql()` / `_drop_schema_if_exists_sql` - DDL to create or drop a new schema or database

**To test the data source**

1. Create an `.env` file based on `.env.example` and add the appropriate variables for the data source.
2. Change the `test_data_source` variable to the data source you are testing.
3. Run the tests using `pytest`.
