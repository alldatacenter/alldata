#
# Copyright 2017 ABSA Group Limited
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Enable Spline tracking.
# For Spark 2.3+ we recommend the codeless approach to enable Spline - by setting spark.sql.queryExecutionListeners
# (See: examples/README.md)
# Otherwise execute the following method to enable Spline manually.
sc._jvm.za.co.absa.spline.harvester \
    .SparkLineageInitializer.enableLineageTracking(spark._jsparkSession)

# Execute a Spark job as usual:
spark.read \
    .option("header", "true") \
    .option("inferschema", "true") \
    .csv("data/input/batch/wikidata.csv") \
    .write \
    .mode('overwrite') \
    .csv("data/output/batch/python-sample.csv")
