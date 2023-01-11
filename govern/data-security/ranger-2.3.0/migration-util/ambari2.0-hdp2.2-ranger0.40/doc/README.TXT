Utility to Migrate Ranger Config into Ambari (from a previous manual install) 
-----------------------------------------------------------------------------

Pre-requisites:
----------------
  * Ambari Server (2.0.0) installed on one of the host of the cluster. 
  * Ranger service is manually installed outside Ambari.
  * If user had Ambari 1.7.0 installed, Ambari needs to be upgraded to 2.0.0, before this script is run.
  * Ranger service is expected to be up and running.
  * Whichever component plugin needs to imported, that component should be up and running in Ambari server.
  * Whichever component plugin needs to be imported, that plugin needs to be installed as well as enabled independently by the user.
  * The installation folder paths for each component is as follows :
      * For Ranger Admin service: /usr/hdp/<hdp_vers>/ranger-admin
      * For Ranger Usersync service: /usr/hdp/<hdp_vers>/ranger-usersync
      * For Ranger Hbase plugin: /usr/hdp/<hdp_vers>/ranger-hbase-plugin
      * For Ranger Hdfs plugin: /usr/hdp/<hdp_vers>/ranger-hdfs-plugin
      * For Ranger Hive plugin: /usr/hdp/<hdp_vers>/ranger-hive-plugin
      * For Ranger Knox plugin: /usr/hdp/<hdp_vers>/ranger-knox-plugin
      * For Ranger Storm plugin: /usr/hdp/<hdp_vers>/ranger-storm-plugin

Pre Install:
------------ 
  * Please take a backup of existing ranger DB and configurations, in case required.
  * Please take a backup of existing ambari DB and configurations, in case required.
  * You must have an existing MySQL Server or Oracle Server database instance running to be used by Ranger.
  * Ensure that the access for the DB Admin user (root in case of MySQL or SYS in case of Oracle) is enabled in DB server from any host.
  * Execute the following command on the Ambari Server host. 
  * Replace database-type with mysql or oracle and /jdbc/driver/path based on the location of the MySQL or Oracle JDBC driver: 
        
 ambari-server setup --jdbc-db={database-type} --jdbc-driver={/jdbc/driver/path}

Steps to Add Ranger in a Ambari HDP cluster:
--------------------------------------------
  * In order to know about usage of the script, Execute the command:
        python import_ranger_to_ambari.py
  * Migration script locates the existing config in post-install properties files used by the service.
  * This script will collect required configs from the independently installed Ranger service configurations 
    and after verification from the user, post it to the specified  Ambari Server.
  * Set appropriate values in the <component>_install.properties template file, only if required.
       For Ranger Admin service, ranger_admin_install.properties
  * For script to use proper values, mandatory properties to be filled by user are as follows :
     1) During Ranger_Admin installation migration db_root_username and db_root_password are mandatory,
           which can be set in the ranger_admin_install.properties

To import Ranger (Admin and User-Sync) service and plugins we need to run the python script using command line as shown below, 
with valid input parameters, from the host where specific Ranger component has been installed / enabled :

python import_ranger_to_ambari.py  {install option} {ambari server url} {ambari server admin username:password} {cluster name} {FQDN of host having Ranger Admin or Ranger Usersync or plugins installed}

for example the actual command will be as :

python import_ranger_to_ambari.py 1 http://100.100.100.100:8080 admin:admin ambari_cluster rangerambari-feb09-rhel6-mp-sec-6.cs1cloud.internal

First parameter (install option) is to mention the service type to be ported to Ambari that is,
 1 for adding Ranger service and  Ranger Admin component to Ambari.
 2 for adding Ranger User sync component to Ambari.
 3 to import Ranger Hdfs Plugin configs to Ambari.
 4 to import Ranger Hive Plugin configs to Ambari.
 5 to import Ranger Hbase Plugin configs to Ambari.
 6 to import Ranger Knox Plugin configs to Ambari.
 7 to import Ranger Storm Plugin configs to Ambari.

  * After running the script with first parameter as 1, Ranger service should be visible as Ambari service and Ranger-Admin should be visible.
  * After running with 2 Ranger-Usersync should also be visible as its component, like-wise for the respective plugins.
  * After executing the script with options 3 to 7 - please visit Ambari UI and restart the individual component, after the UI reflects the changes.

Debugging:
----------
  * Make sure to back up the ranger config and DB prior to running the scripts.
  * It is possible that the service-components may be added to Ambari but the installation may fail. In that case, look for the logs from Ambari-UI. 
  * If any of the pre-requisite is not met this might happen, in that case, try installation from Ambari-UI itself.
  * If the services are configured and not started it may show up as Install Pending, reinstall both Admin and User-sync from Ambari UI, and then start the services.
  * In case if the services are installed and in stopped state, restart them from Ambari-UI.

