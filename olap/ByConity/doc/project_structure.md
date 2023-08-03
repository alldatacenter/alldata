The major modules specific for ByConity in [src](../src):

| Module                      | Description |
|----------------------------|-------------|
| [Catalog](../src/Catalog)               | Catalog service used for metadata management    |
| [DaemonManager](../src/DaemonManager/)       |    Background task management     |
| [Optimizer](../src/Optimizer/)       | Query optimizer        |
| [QueryPlan](../src/QueryPlan/)           | Query planner and plan        |
| [ResourceManager](../src/ResourceManagement/)         | Computing resource management        |
| [Statistics](../src/Statistics/) | Statistic module for CBO        |
| [Storages](../src/Storages/) | Table engine : StorageCnchMergeTree.cpp       |
| [Transaction](../src/Transaction/) | Transaction implementation        |
| [TSO](../src/TSO/) | Timestamp oracle        |