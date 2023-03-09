<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
# Measures
measures to calculate data quality metrics.

### Accuracy measure
accuracy measure is to compare source and target content, given corresponding mapping relationship.

#### Introduction
How to measure accuracy dimension of one target dataset T, given source of truth as golden dataset S.
To measure accuracy quality of target dataset T,
basic approach is to calculate discrepancy between target and source datasets by going through their contents,
examining whether all fields are exactly matched as below,
```
                Count(source.field1 == target.field1 && source.field2 == target.field2 && ...source.fieldN == target.fieldN)
Accuracy  =     ---------------------------------------------------------------------------------------------------------------
                Count(source)

```

Since two datasets are too big to fit in one box, so our approach is to leverage map reduce programming model by distributed computing.

The real challenge is how to make this comparing algorithm generic enough to release data analysts and data scientists from coding burdens, and at the same time, it keeps flexibility to cover most of accuracy requirements.

Traditional way is to use SQL based join to calculate this, like scripts in hive.

But this SQL based solution can be improved since it has not considered unique natures of source dataset and target dataset in this context.

Our approach is to provide a generic accuracy measure, after taking into consideration of special natures of source dataset and target dataset.

Our implementation is in scala, leveraging scala's declarative capability to cater for various requirements, and running in spark cluster.

To make it concrete, schema for Source is as below

```
|-- uid: string (nullable = true)
|-- site_id: string (nullable = true)
|-- page_id: string (nullable = true)
|-- curprice: string (nullable = true)
|-- itm: string (nullable = true)
|-- itmcond: string (nullable = true)
|-- itmtitle: string (nullable = true)
|-- l1: string (nullable = true)
|-- l2: string (nullable = true)
|-- leaf: string (nullable = true)
|-- meta: string (nullable = true)
|-- st: string (nullable = true)
|-- dc: string (nullable = true)
|-- tr: string (nullable = true)
|-- eventtimestamp: string (nullable = true)
|-- cln: string (nullable = true)
|-- siid: string (nullable = true)
|-- ciid: string (nullable = true)
|-- sellerid: string (nullable = true)
|-- pri: string (nullable = true)
|-- pt: string (nullable = true)
|-- dt: string (nullable = true)
|-- hour: string (nullable = true)
```

and schema for target is below as

```
|-- uid: string (nullable = true)
|-- page_id: string (nullable = true)
|-- site_id: string (nullable = true)
|-- js_ev_mak: string (nullable = true)
|-- js_ev_orgn: string (nullable = true)
|-- curprice: string (nullable = true)
|-- itm: string (nullable = true)
|-- itmcond: string (nullable = true)
|-- itmtitle: string (nullable = true)
|-- l1: string (nullable = true)
|-- l2: string (nullable = true)
|-- leaf: string (nullable = true)
|-- meta: string (nullable = true)
|-- st: string (nullable = true)
|-- dc: string (nullable = true)
|-- tr: string (nullable = true)
|-- eventtimestamp: string (nullable = true)
|-- cln: string (nullable = true)
|-- siid: string (nullable = true)
|-- ciid: string (nullable = true)
|-- sellerid: string (nullable = true)
|-- product_ref_id: string (nullable = true)
|-- product_type: string (nullable = true)
|-- is_bu: string (nullable = true)
|-- is_udid: string (nullable = true)
|-- is_userid: string (nullable = true)
|-- is_cguid: string (nullable = true)
|-- dt: string (nullable = true)
|-- hour: string (nullable = true)
```


#### Accuracy Measure In Deep

##### Pre-Process phase (transform raw data)
For efficient, we will convert our raw record to some key-value pair , after that, we just need to compare values which have the same key.
Since two dataset might have different names for the same field, and fields might come in different order, we will keep original information in associative map for later process.

The records will look like,
```
((uid,eventtimestamp)->(curprice->value(curprice),itm->value(itm),itmcond->value(itmcond),itmtitle->value(itmtitle),...)
```
and to track where are the data from, we add one labeling tag here.
for source dataset, we add label tag "\_\_source\_\_" and for target dataset, we add label tag "\_\_target\_\_".
```
((uid,eventtimestamp)->("__source__",(curprice->value(curprice),itm->value(itm),itmcond->value(itmcond),itmtitle->value(itmtitle),...)))
((uid,eventtimestamp)->("__target__",(curprice->value(curprice),itm->value(itm),itmcond->value(itmcond),itmtitle->value(itmtitle),...)))
```
Ideally, in dataset, applying those composite keys, we should be able to get unique records for every composite key.
but the reality is , for various unknown reasons, dataset might have duplicate records given one unique composite key.
To cover this problem, and to track all records from source node, we will append all duplicate records in a list during this step.
The record will look like after pre process ,
```
((uid,eventtimestamp)->List(("__source__",(curprice->value(curprice),itm->value(itm),itmcond->value(itmcond),itmtitle->value(itmtitle),...)),...,("__source__",(curprice->value(curprice),itm->value(itm),itmcond->value(itmcond),itmtitle->value(itmtitle),...))))
```
To save all records from target node, we will insert all records in a set during this step.
The record will look like after pre process ,
```
((uid,eventtimestamp)->Set(("__target__",(curprice->value(curprice),itm->value(itm),itmcond->value(itmcond),itmtitle->value(itmtitle),...)),...,("__target__",(curprice->value(curprice),itm->value(itm),itmcond->value(itmcond),itmtitle->value(itmtitle),...))))
```
##### Aggregate and Comparing phase
Union source and target together, execute one aggregate for all, we can apply rules defined by users to check whether records in source and target are matched or not.

```
aggregate { (List(sources),Set(targets)) =>
 if(foreach element from List(sources) in Set(targets)) emit true
 else emit false
}
```
We can also execute one aggregate to count the mismatch records in source
```
aggregate (missedCount = 0) { (List(sources), Set(targets)) =>
 foreach (element in List(sources)) {
  if (element in Set(targets)) continue
  else missedCount += 1
 }
}
```
#### Benefits
 + It is two times faster than traditional SQL JOIN based solution, since it is using algorithm customized for this special accuracy problem.
 + It is easily to iterate new accuracy metric as it is packaged as a common library as a basic service, previously it took us one week to develop and deploy one new metrics from scratch, but after applying this approach , it only need several hours to get all done.




#### Further discussion
 + How to select keys?
	How many keys we should use, if we use too many keys, it will reduce our calculation performance, otherwise, it might have too many duplicate records, which will make our comparison logic complex.
 + How to define content equation?
	For some data, it is straightforward, but for some data, it might require transform by some UDFS, how can we make our system extensible to support different raw data.
 + How to fix data latency issue?
	To compare, we have to have data available, but how to handle data latency issue which happens often in real enterprise environment.
 + How to restore lost data?
	Detect data lost is good, but the further action is how can we restore those lost data?
