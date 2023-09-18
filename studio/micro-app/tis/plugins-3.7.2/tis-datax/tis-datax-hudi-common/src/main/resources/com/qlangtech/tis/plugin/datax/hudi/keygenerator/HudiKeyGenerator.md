## partition

HDFS Path contain hive partition values for the keys it is partitioned on. This mapping is not straight forward and
requires a pluggable implementation to extract the partition value from HDFS path.

**e.g** Hive table partitioned by datestr=yyyy-mm-dd and hdfs path /app/hoodie/dataset1/YYYY=[yyyy]/MM=[mm]/DD=[dd]

There are some types of partition strategies :

- **fieldValBased**: base on Hudi class `org.apache.hudi.hive.MultiPartKeysValueExtractor` that Partition Key extractor treating each value delimited by slash as separate key.

- **slashEncodedDay** :
   base on hudi class `org.apache.hudi.hive.SlashEncodedDayPartitionValueExtractor`
   HDFS Path contain hive partition values for the keys it is partitioned on. 
   This mapping is not straight forward and requires a pluggable implementation to extract the partition value from HDFS path.
   
   This implementation extracts `datestr=yyyy-mm-dd` from path of type `/yyyy/mm/dd`

- **slashEncodedHour** :   
   
   base on hudi class `org.apache.hudi.hive.SlashEncodedHourPartitionValueExtractor`

   HDFS Path contain hive partition values for the keys it is partitioned on. This mapping is not straight forward and
   requires a pluggable implementation to extract the partition value from HDFS path.
   This implementation extracts `datestr=yyyy-mm-dd-HH` from path of type `/yyyy/mm/dd/HH`
