## clusterId

The `cluster-id`, which should be no more than 45 characters, is used for identifying a unique Flink cluster. 
 
## jmMemory

`Total Process Memory size for the JobManager`. 

This includes all the memory that a JobManager JVM process consumes, consisting of Total Flink Memory, JVM Metaspace, and JVM Overhead. 
In containerized setups, this should be set to the container memory. 
See also 'jobmanager.memory.flink.size' for Total Flink Memory size configuration.

单位：兆

## tmMemory

`Total Flink Memory size for the TaskExecutors`. 

This includes all the memory that a TaskExecutor consumes, except for JVM Metaspace and JVM Overhead. 
It consists of Framework Heap Memory, Task Heap Memory, Task Off-Heap Memory, Managed Memory, and Network Memory. 
See also '%s' for total process memory size configuration.

单位：兆
