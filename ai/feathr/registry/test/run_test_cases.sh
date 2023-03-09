# run test cases and check coverage for purview-registry; 
# need to set "PURVIEW_NAME" in env
pytest --cov-report term-missing --cov=registry/purview-registry/registry --cov-config=registry/test/.coveragerc registry/test/test_purview_registry.py
# run test cases and check coverage for sql-registry; 
# need to set "CONNECTION_STR" in env
pytest --cov-report term-missing --cov=registry/sql-registry/registry --cov-config=registry/test/.coveragerc registry/test/test_sql_registry.py
