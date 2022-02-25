<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Instructions to setup ambari-scom
======


###Setup the HadoopMonitoring database


1. Setup SQLServer for mixed mode authentication (the metrics2 sink and Ambari metrics provider will connect through SQLServer authentication).
2. Create SQLServer login and password (or use the default sa login)
3. Use SQLServer Configuration Manager to make sure that TCP/IP is active and enabled (note the port : default 1433).
4. Run the DDL script from the ambari-scom project to create the HadoopMoitoring database.  The script should be available as **db/MonitoringDatabase.sql** in the ambari-scom project folder.
5. Download the SqlServer JDBC driver jar **(sqljdbc4.jar)** from [here](http://msdn.microsoft.com/en-us/data/aa937724.aspx).

###Install Hadoop and SQLServerSink


1. Follow the hadoop install instructions [here](http://docs.hortonworks.com/HDPDocuments/HDP1/HDP-Win-1.3.0/index.html), making sure you download the 1.3 MSI.
2. Note the location of the **clusterproperties.txt** configuration file.
3. Setup Metrics2 SqlServerSink by editing the **hadoop-metrics2.properties** file in the hadoop bin folder (e.g. C:\HDP\hadoop-1.2.0.1.3.0.0-0380\bin\hadoop-metrics2.properties).  
Note that this step should be repeated on each node of the cluster installed above.  Set the following properties…

        *.sink.sql.class=org.apache.hadoop.metrics2.sink.SqlServerSink

        namenode.sink.sql.databaseUrl=jdbc:sqlserver://[server]:[port];databaseName=HadoopMonitoring;user=[user];password=[password]
        datanode.sink.sql.databaseUrl=jdbc:sqlserver://[server]:[port];databaseName=HadoopMonitoring;user=[user];password=[password]
        jobtracker.sink.sql.databaseUrl=jdbc:sqlserver://[server]:[port];databaseName=HadoopMonitoring;user=[user];password=[password]
        tasktracker.sink.sql.databaseUrl=jdbc:sqlserver://[server]:[port];databaseName=HadoopMonitoring;user=[user];password=[password]
        maptask.sink.sql.databaseUrl=jdbc:sqlserver://[server]:[port];databaseName=HadoopMonitoring;user=[user];password=[password]
        reducetask.sink.sql.databaseUrl=jdbc:sqlserver://[server]:[port];databaseName=HadoopMonitoring;user=[user];password=[password]

   Note that the server, port, user and password should work with the SQLServer install from above 
   (e.g. jdbc:sqlserver://ambari1:1433;databaseName=HadoopMonitoring;user=sa;password=BigData1).
4. Copy the SqlServer JDBC driver jar and the ambari-scom jar to each node of the cluster.
5. Set ambari-scom jar and SQLServer driver jar in class path by editing the class path in the **namenode.xml, datanode.xml, jobtracker.xml, tasktracker.xml and other component xml files** of the hadoop bin folder (e.g. C:\HDP\hadoop-1.2.0.1.3.0.0-0380\bin\namenode xml).  
Note that this step should be repeated on each node of the cluster.  In each file prepend the following to -classpath… 
  
        [path to ambari-scom jar];[path to SqlServer JDBC jar]; 
For example, '-classpath C:\ambari-scom\target\ambari-scom-1.0.jar;C:\hadoop\sqljdbc4.jar;…'.

6. Start (or restart) Hadoop by using the scripts in the HDP install folder to start the services as directed by the link in step 1.

        C:\HDP>start_local_hdp_services.cmd
       
7. Check to see that metrics are being directed to the SQLServer database by querying the records table. 

        select * from HadoopMonitoring.dbo.MetricRecord
   The table should not be empty.


###Run AmbariServer


1. Edit the **ambari.properties** file from the ambari-scom project (ambari-scom/config/ambari.properties or unzip target/ambari-scom-1.0-conf.zip to desired location).  Add the following properties ...

        scom.sink.db.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
        scom.sink.db.url=jdbc:sqlserver://[server]:[port];databaseName=HadoopMonitoring;user=[user];password=[password]
    Note that the server, port, user and password should work with the SQLServer install from above (e.g. jdbc:sqlserver://ambari1:1433;databaseName=HadoopMonitoring;user=sa;password=BigData1).

2. Use java to run the ambari server **(org.apache.ambari.scom.AmbariServer)** from a Windows command prompt.  Include the following in the classpath ...

   * the folder containing the **ambari.properties** file (see step #1).
   * the folder containing the **clusterproperties.txt** file from the hadoop install.
   * the SqlServer JDBC jar **(sqljdbc4.jar)** from the steps above.
   * the ambari-scom jar from the ambari-scom build target.
   * the lib folder containing the dependency jars from the ambari-scom build target.
   

   The command should look something like this ...
    
        java -server -XX:NewRatio=3 -XX:+UseConcMarkSweepGC -XX:-UseGCOverheadLimit -XX:CMSInitiatingOccupancyFraction=60 -Xms512m -Xmx2048m -cp "c:\ambari-scom\conf;c:\hadoop\sqljdbc4.jar;c:\ambari-scom\target\ambari-scom-1.0.jar;c:\ambari-scom\target\lib\*" org.apache.ambari.scom.AmbariServer


###Test the API


1. From a browser access the API...
  
        http://[server]:8080/api/v1/clusters

2. Verify that metrics are being reported…

        http://[server]:8080/api/v1/clusters/ambari/services/HDFS/components/NAMENODE
