import dask.datasets
import pandas as pd
from soda.scan import Scan

# Create soda scan object
scan = Scan()
scan.set_scan_definition_name("test")
scan.set_data_source_name("dask")

# Load timeseries data from dask datasets
df_timeseries = dask.datasets.timeseries().reset_index()
df_timeseries["email"] = "a@soda.io"

# Create an artificial pandas dataframe
df_employee = pd.DataFrame({"email": ["a@soda.io", "b@soda.io", "c@soda.io"]})

# Add dask dataframe to scan and assign a dataset name to refer from checks yaml
scan.add_dask_dataframe(dataset_name="timeseries", dask_df=df_timeseries)

# Add pandas dataframe to scan and assign a dataset name to refer from checks yaml
scan.add_pandas_dataframe(dataset_name="employee", pandas_df=df_employee)

# Define checks in yaml format
# alternatively you can refer to a yaml file using scan.add_sodacl_yaml_file(<filepath>)
checks = """
for each dataset T:
  datasets:
    - include %
  checks:
    - row_count > 0

profile columns:
  columns:
    - employee.%

checks for employee:
    - values in (email) must exist in timeseries (email) # Error expected
    - row_count same as timeseries # Error expected

checks for timeseries:
  - avg_x_minus_y between -1 and 1:
      avg_x_minus_y expression: AVG(x - y)
  - failed rows:
      samples limit: 50
      fail condition: x >= 3
  - schema:
      name: Confirm that required columns are present
      warn:
        when required column missing: [x]
        when forbidden column present: [email]
        when wrong column type:
          email: varchar
      fail:
        when required column missing:
          - y
  - invalid_count(email) = 0:
      valid format: email
  - valid_count(email) > 0:
      valid format: email
  - duplicate_count(name) < 4:
      samples limit: 2
  - missing_count(y):
      warn: when > -1
  - missing_percent(x) < 5%
  - missing_count(y) = 0
  - avg(x) between -1 and 1
  - max(x) > 0
  - min(x) < 1:
      filter: x > 0.2
  - freshness(timestamp) < 1d
  - values in (email) must exist in employee (email)
"""

scan.add_sodacl_yaml_str(checks)

scan.set_verbose(True)
scan.execute()
