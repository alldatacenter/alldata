<!--
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

Ambari Server integration with SNMP traps
=========================================
Ambari uses nagios to manage alerts in the cluster.  When a node goes down or a service state changes in the cluster, nagios
will handle those events and is monitored using Ambari-web.  This document describes how to integrate alerts with remote
SNMP management station by sending SNMP traps.  By enabling SNMP traps, Ambari & Hadoop cluster alerts can be monitored
using remote management station (like OpenNMS, HP OpenView, etc.,).

    This will work with Ambari Server 1.7.0 and below.  In Ambari 2.0.0, this feature will be
    replaced by alert framework.

Prerequisites:
--------------
1. Nagios server should be running in one of the hadoop cluster node. (Need not to be same node as Ambari server).
2. SNMP should be installed in the node where nagios server is running.  Run the following command to install net-snmp
and net-snmp-utils.
> yum install net-snmp net-snmp-utils net-snmp-devel
3. There should be connectivity between the hadoop node running nagios server and the management station.  The snmptrap
command will use 162/udp to send trap to the management station.

Instructions:
-------------
1. Copy the file src/nagios/objects/snmp-commands.cfg to {nagios\_home\_dir}/objects/snmp-commands.cfg in the node where nagios is running.
This file defines the command to send traps for service and host failures.

    The default home directory (nagios_home_dir) for nagios is /etc/nagios

2. Copy the file src/nagios/objects/snmp-contacts.cfg to {nagios\_home\_dir}/objects/snmp-contacts.cfg in the node where nagios is running.
This file defines the **snmp-management-station** contact.

3. In the node where ambari-server is running, edit file /var/lib/ambari-server/resources/stacks/HDP/2.0.6/services/NAGIOS/package/templates/nagios.cfg.j2
and add below lines just before the {{nagios\_host\_cfg}}

	<pre><code>&#35;Definitions for SNMP traps
	cfg_file=/etc/nagios/objects/snmp-commands.cfg
	cfg_file=/etc/nagios/objects/snmp-contacts.cfg</code></pre>

    Note: If the home directory is different than /etc/nagios, use the updated home directory.  The updated configuration will be automatically
    pushed to the nagios server when ambari-server restarted.

4. To enable SNMP trap, edit file /var/lib/ambari-server/resources/stacks/HDP/2.0.6/services/NAGIOS/package/templates/contacts.cfg.j2
in the ambari-server and add **snmp-management-station** to the contract group **admins**

	>  members {{nagios\_web\_login}},sys_logger,**snmp-management-station**

5. Copy the file src/scripts/send-service-trap to /usr/local/bin/send-service-trap in the node where nagios is running.
Also, run the following command

	> chmod +x /usr/local/bin/send-service-trap

	> chown nagios:nagios /usr/local/bin/send-service-trap

6. Copy the file src/scripts/send-host-trap to /usr/local/bin/send-host-trap in the node where nagios is running.
Also, run the following command

	> chmod +x /usr/local/bin/send-service-trap

	> chown nagios:nagios /usr/local/bin/send-service-trap

7. Download nagios MIBS from *http://ftp.cc.uoc.gr/mirrors/monitoring-plugins/mib/nagiosmib-1.0.0.tar.gz* and
extract the files to /usr/share/snmp/mibs/ directory.

8. Restart ambari-server
	> ambari-server restart

9. Launch ambari-web (or GUI) in the browser and login.  Select Nagios server and restart the service.

10. Configure management station by editing file /etc/hosts and add the below line

		<MGMT_STATION_IP> snmp-manager

11. For integrating with existing management station or NMS system,
	1. Download the nagios MIB's from *http://ftp.cc.uoc.gr/mirrors/monitoring-plugins/mib/nagiosmib-1.0.0.tar.gz*
	2. Extract and copy the files under MIB directory of the management station's (or NMS) mib directory.
	3. Import the mibs if required.

Testing:
-------
To test whether the snmptraps are triggered, use the following procedure.

1. Load the MIB in the snmp management system.
2. Make sure the snmp management system IP (or FQDN) is configured in the /etc/hosts file in the node where
nagios server is running.
3. Open the ambari-web in the browser and login.  Try to stop some services from ambari-web and check the snmptraps
are received by the snmp management station.
