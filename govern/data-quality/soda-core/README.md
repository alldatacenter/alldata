
<h1 align="center">Soda Core</h1>
<p align="center"><b>Data quality testing for SQL-, Spark-, and Pandas-accessible data.</b></p>

<p align="center">
  <a href="https://github.com/sodadata/soda-core/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-Apache%202-blue.svg" alt="License: Apache 2.0"></a>
  <a href="https://join.slack.com/t/soda-community/shared_invite/zt-m77gajo1-nXJF7JtbbRht2zwaiLb9pg"><img alt="Slack" src="https://img.shields.io/badge/chat-slack-green.svg"></a>
  <a href="#"><img src="https://static.pepy.tech/personalized-badge/soda-core?period=total&units=international_system&left_color=black&right_color=green&left_text=Downloads"></a>
</p>
<br />


&#10004;  An open-source, CLI tool and Python library for data quality testing<br />
&#10004;  Compatible with the <a href="https://docs.soda.io/soda-cl/soda-cl-overview.html" target="_blank">Soda Checks Language (SodaCL)</a>  <br />
&#10004;  Enables data quality testing both in and out of your data pipelines and development workflows<br />
&#10004;  Integrated to allow a Soda scan in a data pipeline, or programmatic scans on a time-based schedule <br />


Soda Core is a free, open-source, command-line tool and Python library that enables you to use the Soda Checks Language to turn user-defined input into aggregated SQL queries. 

When it runs a scan on a dataset, Soda Core executes the checks to find invalid, missing, or unexpected data. When your Soda Checks fail, they surface the data that you defined as bad-quality.

#### Soda Library 

Consider using **[Soda Library](https://docs.soda.io/soda/quick-start-sip.html)**, an extension of Soda Core that offers more features and functionality, and enables you to connect to a [Soda Cloud](https://docs.soda.io/soda-cloud/overview.html) account to collaborate with your team on data quality.
[Install Soda Library](https://docs.soda.io/soda-library/install.html) and get started with a 45-day free trial.

<br />

## Get started

Soda Core currently supports connections to several data sources. See [Compatibility](/docs/installation.md#compatibility) for a complete list.

**Requirements**
* Python 3.8 or greater
* Pip 21.0 or greater


**Install and run**
1. To get started, use the install command, replacing `soda-core-postgres` with the package that matches your data source.  See [Install Soda Core](/docs/installation.md) for a complete list.<br />
    ```shell
    pip install soda-core-postgres
    ```

2. Prepare a `configuration.yml` file to connect to your data source. Then, write data quality checks in a `checks.yml` file. See [Configure Soda Core](/docs/configuration.md).

3. Run a scan to review checks that passed, failed, or warned during a scan. See [Run a Soda Core scan](/docs/scan-core.md).
    ```shell
    soda scan -d your_datasource -c configuration.yml checks.yml
    ```

#### Example checks
```yaml
# Checks for basic validations
checks for dim_customer:
  - row_count between 10 and 1000
  - missing_count(birth_date) = 0
  - invalid_percent(phone) < 1 %:
      valid format: phone number
  - invalid_count(number_cars_owned) = 0:
      valid min: 1
      valid max: 6
  - duplicate_count(phone) = 0

# Checks for schema changes
checks for dim_product:
  - schema:
      name: Find forbidden, missing, or wrong type
      warn:
        when required column missing: [dealer_price, list_price]
        when forbidden column present: [credit_card]
        when wrong column type:
          standard_cost: money
      fail:
        when forbidden column present: [pii*]
        when wrong column index:
          model_name: 22
# Check for freshness 
  - freshness(start_date) < 1d

# Check for referential integrity
checks for dim_department_group:
  - values in (department_group_name) must exist in dim_employee (department_name)
```
<br />

## Documentation

* [Soda Core](/docs/overview-main.md)
* [Soda Checks Language (SodaCL)](https://docs.soda.io/soda-cl/soda-cl-overview.html)

