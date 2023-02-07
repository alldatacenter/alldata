# Running Apache Drill

## Prerequisites
  
  * Linux, Windows or OSX 
  * Oracle/OpenJDK 8 (JDK, not JRE)

Additional requirements when running in clustered mode:
  * Hadoop 2.3+ distribution of Hadoop (such as Apache or MapR)
  * Zookeeper is required for a clustered installation

## Installing the Tarball
    
  1. mkdir /opt/drill
  2. tar xvzf [tarball] --strip=1 -C /opt/drill 

## Running in embedded mode
    
  1. cd /opt/drill
  2. bin/sqlline -u jdbc:drill:zk=local
  3. Run a query (below).

## Running in clustered mode

  1. Edit drill-override.conf to provide zookeeper location
  2. Start the drillbit using bin/drillbit.sh start
  3. Repeat on other nodes
  4. Connect with sqlline by using bin/sqlline -u "jdbc:drill:zk=[zk_host:port]"
  5. Run a query (below).
   
## Run a query

Drill comes preinstalled with a number of example data files including a small copy of the TPCH data in self describing Parquet files as well as the foodmart database in JSON.  You can query these files using the cp schema.  For example:    

    USE cp;
    
    SELECT 
      employee_id, 
      first_name
    FROM `employee.json`; 
    
## More information 

For more information including how to run a Apache Drill cluster, visit the [Apache Drill Documentation](http://drill.apache.org/docs/)
