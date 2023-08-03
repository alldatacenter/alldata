# Postgres example

## Description
A quick example to illustrate basic functionality with postgres.

## Requirements

- docker (https://get.docker.com/)
- miniconda (https://docs.conda.io/en/main/miniconda.html)
- soda-core (https://github.com/sodadata/soda-core)
- python

## Example

```
# install docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# install miniconda
mkdir -p ~/miniconda3
wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -O ~/miniconda3/miniconda.sh
bash ~/miniconda3/miniconda.sh -b -u -p ~/miniconda3
rm -rf ~/miniconda3/miniconda.sh
~/miniconda3/bin/conda init bash

# create a virtual environment
conda create -y -n soda python=3.8
conda activate soda

# install soda-core-postgres
pip install soda-core-postgres

# following https://docs.soda.io/soda/quick-start-sip.html

# build an example data source and host db on postgres
sudo docker run -d \
 --name sip-of-soda \
 -p 5432:5432 \
 -e POSTGRES_PASSWORD=secret \
 sodadata/soda-adventureworks

# create a working directory
mkdir soda
cd soda

# create the example configuration.yml file
cat <<EOT >> configuration.yml
 data_source adventureworks:
   type: postgres
   connection:
     host: localhost
     username: postgres
     password: secret
   database: postgres
   schema: public
EOT

# test connection
soda test-connection -d adventureworks -c configuration.yml

# create example checks.yml
cat <<EOT >> checks.yml
checks for dim_customer:
  - invalid_count(email_address) = 0:
       valid format: email
       name: Ensure values are formatted as email addresses
  - missing_count(last_name) = 0:
       name: Ensure there are no null values in the Last Name column
  - duplicate_count(phone) = 0:
       name: No duplicate phone numbers
  - freshness(date_first_purchase) < 7d:
       name: Data in this dataset is less than 7 days old
  - schema:
       warn:
         when schema changes: any
       name: Columns have not been added, removed, or changed
EOT

# run the scan!
soda scan -d adventureworks -c configuration.yml checks.yml

# note that an error is thrown for one test, as change-over-time checks
# require you to connect to Soda Cloud

```





