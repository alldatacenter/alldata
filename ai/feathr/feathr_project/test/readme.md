# Developer Guide
Before you run any commands, make sure non one is using the job queue or using the test database. Otherwise, you may 
read others' test data or wipe/overwrite others test data.

## Clean Up Old Data
If you don't clean the test data, your test may depend on older test data.

```python3 feathr_project/test/clean_azure_test_data.py```

## Initialize the data

```python3 feathr_project/test/prep_azure_test_data.py```

The job will take a few mins to complete. So wait some time before you continue to next step.

## Run Tests
```pytest```

Ensure all tests passed. If tests failed, please fix them.

## Clean Up the Test Data

```python3 feathr_project/test/clean_azure_test_data.py```

