 # Description
## overview
The audit sdk is used to count the receiving and sending volume of each module in real time according to the cycle, 
and the statistical results are sent to the audit access layer according to the cycle.

## features
### data uniqueness
The audit sdk will add a unique mark to each audit audit, which can be used to remove duplicates.

### unified audit standard
The audit sdk uses log production time as the audit standard, 
which can ensure that each module is reconciled in accordance with the unified audit standard.

## usage
### setAuditProxy
Set the audit access layer ip:port list. The audit sdk will summarize the results according to the cycle 
and send them to the ip:port list set by the interface.
If the ip:port of the audit access layer is fixed, then this interface needs to be called once. 
If the audit access changes in real time, then the business program needs to call this interface periodically to update
```java
    HashSet<String> ipPortList=new HashSet<>();
    ipPortList.add("0.0.0.0:54041");
    AuditImp.getInstance().setAuditProxy(ipPortList);
```

### add
Call the add method for statistics, where the auditID parameter uniquely identifies an audit object,
inlongGroupID,inlongStreamID,logTime are audit dimensions, count is the number of items, size is the size, and logTime is milliseconds.
```java
    AuditImp.getInstance().add(1, "inlongGroupIDTest","inlongStreamIDTest", System.currentTimeMillis(), 1, 1);
```