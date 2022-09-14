Streamis upgrade document. This article mainly introduces the upgrade steps of adapting DSS1.1.0 and linkis1.1.1 based on the original installation of Streamis service. The biggest difference between Streamis 0.2.0 and Streamis 0.1.0 is that it accesses DSS appconn and optimizes the start and stop of jobs.

# 1. Work before upgrading streamis
Before upgrading Streamis, please install linkis1.1.1 and DSS1.1.0 or above, and ensure that the linkis Flink engine and DSS can be used normally. For the installation of DSS and linkis, please refer to [dss & linkis one click installation and deployment document](https://github.com/WeBankFinTech/DataSphereStudio-Doc/blob/main/zh_CN/%E5%AE%89%E8%A3%85%E9%83%A8%E7%BD%B2/DSS%E5%8D%95%E6%9C%BA%E9%83%A8%E7%BD%B2%E6%96%87%E6%A1%A3.md).

# 2. Streamis upgrade steps

## Install streamisappconn
1) Delete the old version of StreamisAppconn package

Enter the following directory, find the appconn folder of streamis and delete it, if any:
```shell script
{DSS_Install_HOME}/dss/dss-appconns
```

2) StreamisAppconn installation deployment

To install the DSS StreamisAppConn plug-in. Please refer to: [StreamisAppConn plug-in installation document](development/StreamisAppConnInstallationDocument.md)

## Installing the Streamis backend
Update Lib in the obtained installation package to the path 'streamis-server/lib' under the streamis installation directory, and the file contents under 'streamis-server/conf' can be updated as needed.

Enter the installation directory and execute the update script to complete the update of database table structure and data:
```shell script
cd {Streamis_Install_HOME}
sh bin/upgrade.sh
```

Then complete the update and restart of the Streamis server through the following command:
```shell script
cd {Streamis_Install_HOME}/streamis-server
sh bin/stop-streamis-server.sh 
sh bin/start-streamis-server.sh 
```

##Installing the Streamis front end
First delete the front-end directory folder of the old version, and then replace it with the new front-end installation package.
```
mkdir ${STREAMIS_FRONT_PATH}
cd ${STREAMIS_FRONT_PATH}
#1.Delete front-end directory folder
#2.Place the front-end package
unzip streamis-${streamis-version}.zip
```