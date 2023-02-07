# Drill-on-YARN User Guide

## Introduction

Drill's YARN integration launches your Drill cluster through YARN. Drill becomes a long-running application with YARN. When you launch Drill, YARN automatically deploys ("localizes") Drill software onto each node, avoiding the need to pre-install Drill on each node. Resource management is simplified because YARN is now aware of resources dedicated to Drill.

The discussion below assumes that you are familiar with the main YARN concepts: the Resource Manager (RM), Node Manager (NM) and so on.

The discussion also assumes that you already have a working Drill cluster that you wish to launch under YARN. Drill configuration is best tested by launching Drill directly; launch Drill under YARN when the configuration becomes stable.

### YARN Resource Settings

Drill, by design, aggressively uses all available resources to run queries at optimal speed. When running Drill under YARN, you inform YARN of the resources that Drill will consume. These settings are descriptive, not proscriptive. That is, Drill does not limit itself to the YARN settings; instead the YARN settings inform YARN of the resources that Drill will consume so that YARN does not over-allocate those same resources to other tasks.

All YARN distributions provide settings for memory and CPU (called "vcores" by YARN). Some distributions also provide disk settings.

For memory, you first configure Drill’s memory as described below, then you inform YARN of the Drill configuration.

Drill will use all available disk I/Os. Drill will also use all available CPU. Consider enabling Linux cgroups to limit Drill's CPU usage to match the YARN vcores allocation.

## Components

Major components include:

* Drill distribution archive: the original .tar.gz file for your Drill distribution. Drill-on-YARN uploads this archive to your distributed file system (DFS). YARN downloads it (localized it) to each worker node.
* Drill site directory: a directory that contains your Drill configuration and custom jar files. Drill-on-YARN copies this directory to each worker node.
Configuration: A configuration file which tells Drill-on-YARN how to manage your Drill cluster. This file is separate from your configuration files for Drill itself.
* Drill-on-YARN client: a command-line program to start, stop and monitor your YARN-managed Drill cluster.
* Drill Application Master (AM): The software that works with YARN to request resources, launch Drill-bits, and so on. The AM provides a web UI to manage your Drill cluster.
* Drill-Bit: The Drill daemon software which YARN runs on each node.
​
###​ Overview

The following are the key steps to launch Drill under YARN. Each step is explained in detail in this documentation.

* Create a Drill site directory with your site-specific files.
* Configure Drill-on-YARN using the the drill-on-yarn.conf configuration file.
* Use the Drill-on-YARN client tool to launch your Drill cluster.
* Use Drill-on-YARN client or web UI to monitor and shut down the Drill cluster.

## Quick-Start Guide

This section walks you through the steps needed to create a basic Drill cluster. Later sections discuss additional options and details needed in special situations.
​
### The Client Machine

YARN works by launching an application using a "client" application. For Drill, this is the Drill-on-YARN client. The client can run on any machine that has both the Drill and Hadoop software. The client machine need not be part of your YARN cluster; any host from which you currently launch YARN jobs can be the client. All the steps that follow are done on the client machine. When running Drill outside of YARN, you must install Drill on every node of your cluster. With YARN, you need install Drill only on the client machine; Drill-on-YARN automatically deploys  ("localizes") Drill to the worker nodes.

When running Drill without YARN, many users place their configuration files and custom code within the Drill distribution directory. When running under YARN, all your configuration and custom code resides in the site directory; you should not change anything in the Drill install. (This allows Drill-on-YARN to upload your original Drill install archive without rebuilding it.)
​
### Create a Master Directory

To localize Drill files, the client tool requires a copy of the original Drill distribution archive and the location of your site directory.  For ease of discussion, we assume all these components reside in a single "master directory" described as `$MASTER_DIR`. On the client machine, do the following to create the master directory:
```
export MASTER_DIR=/path/to/master/dir
mkdir $MASTER_DIR
cd $MASTER_DIR
```
The following is a summary of the steps needed to build your master directory. Each step is explained in detail in the following sections.

* Download the Drill archive to `$MASTER_DRILL`.
* Unpack the archive to create `$DRILL_HOME`.
* Create the site directory with the required configuration files.

### Install Drill

These instructions assume you are installing Drill as part of the Drill-on-YARN setup. You can use your existing Drill 1.8 or later install as long as it meets the criteria spelled out here.

Follow the Drill install directions to install Drill on your client host. The install steps are different for YARN than for the Embedded or Cluster install.

1. Select a Drill version. The name is used in multiple places below. For convenience, define an environment variable for the name:
```
export DRILL_NAME=apache-drill-x.y.z
```
Replace x.y.z with the selected version.

2. Download your Drill version.
```
wget \ http://apache.mesi.com.ar/drill/drill-x.y.z/$DRILL_NAME.tar.gz
```
or
```
curl -o $DRILL_NAME.tar.gz \ http://apache.mesi.com.ar/drill/drill-x.y.z/$DRILL_NAME.tar.gz
```
Again, replace x.y.z with the selected version.

3. Expand the Drill distribution into this folder to create the master directory
```
tar -xzf $DRILL_NAME.tar.gz
```
4. For ease of following the remaining steps, call your expanded Drill folder `$DRILL_HOME`:
```
export DRILL_HOME=$MASTER_DIR/$DRILL_NAME
```
Your master directory should now contain the original Drill archive along with an expanded copy of that archive.
​
### Create the Site Directory

The site directory contains your site-specific files for Drill. If you are converting an existing Drill install, see the "Site Directory" section later in this guide.

1. Create the site directory within your master directory:
```
export DRILL_SITE=$MASTER_DIR/site
mkdir $DRILL_SITE
```
When you install Drill fresh, Drill includes a conf directory under `$DRILL_HOME`. Use the files there to create your site directory.
```
cp $DRILL_HOME/conf/drill-override-example.conf $DRILL_SITE/drill-override.conf
cp $DRILL_HOME/conf/drill-on-yarn-example.conf $DRILL_SITE/drill-on-yarn.conf
cp $DRILL_HOME/conf/drill-env.sh $DRILL_SITE
cp $DRILL_HOME/conf/distrib-env.sh $DRILL_SITE
```
Then edit the above configuration files as per the Drill install instructions, and the Drill-on-YARN instructions below. (Note that, under YARN, you set the Drill memory limits in `drill-on-yarn.sh` instead of `drill-env.sh`.)

The instructions above have you copy the distribution-specific `distrib-env.sh` file to your site directory. This is done because the file often contains values set during Drill installation. When you upgrade Drill, be sure to replace the file with the latest version from `$DRILL_HOME/conf`.

If you develop custom code (data sources or user-defined functions (UDFs)), place the Java jar files in `$DRILL_SITE/jars`.

Your master directory should now contain the Drill software and your site directory with default files.

You will use the site directory each time you start Drill by using the `--site` (or `--config`) option. The following are examples, don’t run these yet:
```
drillbit.sh --site $DRILL_SITE
drill-on-yarn.sh --site $DRILL_SITE
```
Once you’ve created your site directory, upgrades are trivial. Simply delete the old Drill distribution and install the new one. Your files remain unchanged in the site directory.
​
### Configure Drill-on-YARN using Existing Settings

The next step is to configure Drill. If you have not yet used Drill, then you should start with Drill in distributed mode to learn which configuration options you need. YARN is an awkward environment in which to learn Drill configuration. Here, we assume that you have already worked out the required configuration on a separate Drill install. Let's call that location `$PROD_DRILL_HOME`.

From `$PROD_DRILL_HOME` copy the following to corresponding locations in `$DRILL_SITE`:
```
cp $PROD_DRILL_HOME/conf/drill-override.conf $DRILL_SITE
cp $PROD_DRILL_HOME/conf/drill-env.sh $DRILL_SITE
cp $PROD_DRILL_HOME/jars/3rdparty/yourJarName.jar $DRILL_SITE/jars
```

See the Preparation section above for changes you should make to your existing `drill-env.sh` file.

### Create Your Cluster Configuration File

The next step is to specify additional configuration which Drill-on-YARN requires to launch your Drill cluster.

Start by editing `$DRILL_SITE/drill-on-yarn.conf` using your favorite editor. This file is in the same [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md) format used by `drill-override.conf`.

Consult `$DRILL_HOME/conf/drill-on-yarn-example.conf` as an example. However, don’t just copy the example file; instead, copy only the specific configuration settings that you need; the others will automatically take the Drill-defined default values.

The following sections discuss each configuration option that you must set.

### Drill Resource Configuration

The two key Drill memory parameters are Java heap size and Direct memory. In a non-YARN cluster, you set these in `$DRILL_HOME/conf/drill-env.sh` as follows, shown with the default values:
```
DRILL_MAX_DIRECT_MEMORY="8G"
DRILL_HEAP="4G"
```
Drill-on-YARN uses a different mechanism to set these values. You set the values in `drill-on-yarn.conf`, then Drill-on-YARN copies the values into the environment variables when launching each Drill-bit.
```
  drillbit: {
    heap: "4G"
    max-direct-memory: "8G"
  }
```
To create the Drill-on-YARN setup, simply copy the values directly from your pre-YARN `drill-env.sh` file into the above configuration. (Drill-on-YARN will copy the values back into the environment variables when launching Drill.)

Next, you must determine the container size needed to run Drill under YARN. Typically this size is simply the sum of the heap and direct memory. However, if you are using custom libraries that perform their own memory allocation, or launch sub-processes, you must account for that memory usage as well. The YARN memory is expressed in MB. For example, for the default settings above, we need 12G of memory or 12288MB:
```
  drillbit: {
    memory-mb: 12288
  }
```
Finally, you must determine how much CPU to grant to Drill. Drill is a CPU-intensive operation and greatly benefits from each additional core. However, you can limit Drill’s CPU usage under YARN by specifying the number of YARN virtual cores (vcores) to allocate to Drill:
```
  drillbit: {
    vcores: 4
  }
```
Note that in the above, each configuration setting was shown separately. In your actual file, however, they appear within a single group as follows:
```
  drillbit: {
    heap: "4G"
    max-direct-memory: "8G"
    memory-mb: 6144
    vcores: 4
  }
```

### Drillbit Cluster Configuration

Drill-on-YARN uses the concept of a "cluster group" of drill-bits to describe the set of drill-bits to launch. A group can be one of three kinds:

* Basic: launches drill-bits anywhere in the YARN cluster where a container is available.
* Labeled: Uses YARN labels to identify the set of nodes that should run Drill.

This section describes how to create a basic group suitable for testing. See later sections for the other two types.

For a basic group, you need only specify the group type and the number of drill-bits to launch:
```
  cluster: [
    {
      name: "drillbits"
      type: "basic"
      count: 1
    }
  ]
```
The above syntax says that cluster is a list that contains a series of cluster group objects contained in braces. In this release, however, Drill supports just one cluster group.
​
### ZooKeeper Configuration

Drill uses ZooKeeper to coordinate between Drillbits. When run under YARN, the Drill Application Master uses ZooKeeper to monitor Drillbit health. Drill-on-YARN reads your `$DRILL_SITE/drill-override.conf` file for ZK settings.

### Configure the Drill Distribution Archive

Next configure the name of the Drill distribution archive that you downloaded earlier.
```
  drill-install: {
    client-path: "archive-path"
   }
```
Where archive-path is the location of your archive. In our example, this is `$MASTER_DIR/apache-drill.x.y.z.tar.gz` Use the full name of the master directory, not the environment variable. (Substitute your actual version number for x.y.z.)

### Select the Distributed File System Location

Drill copies your archive onto your distributed file system (such as HDFS) in a location you provide. Set the DFS options as follows:
```
  dfs: {
    connection: "hdfs://localhost/"
    dir: "/user/drill"
  }
```
Drill can read the connection information from your Hadoop configuration files (`$HADOOP_HOME/etc/hadoop/core-site.xml`). Or you can specify a connection directly in the Drill cluster configuration file using the connection attribute.

Then, choose a DFS file system location. Drill uses "`/user/drill`" by default.

### Hadoop Location

Apache Drill users must tell Drill-on-YARN the location of your Hadoop install. Set the `HADOOP_HOME` environment variable in `$DRILL_SITE/drill-env.sh` to point to your Hadoop installation:
```
export HADOOP_HOME=/path/to/hadoop-home
```
This assumes that Hadoop configuration is in the default location: `$HADOOP_HOME/etc/hadoop`. If your configuration is elsewhere, set `HADOOP_CONF_DIR` instead:
```
export HADOOP_CONF_DIR=/path/to/hadoop-config
```

## Launch Drill under YARN

Finally, use the client tool to launch your new Drill cluster:
```
$DRILL_HOME/bin/drill-on-yarn.sh --site $DRILL_SITE start
```
You will see a number of lines describing the startup process. The tool automatically archives and uploads your site directory, which YARN copies (along with Drill) onto each node. If all goes well, the tool prints a URL for the Drill Application Master process that you can use to monitor the cluster. Your Drillbits should now be up and running. (If not, see the Troubleshooting section below.)
To check the status of your Drill cluster:
```
$DRILL_HOME/bin/drill-on-yarn.sh --site $DRILL_SITE status
```
To stop your cluster:
```
$DRILL_HOME/bin/drill-on-yarn.sh --site $DRILL_SITE stop
```
Note, to avoid typing the site argument each time, you can set an environment variable:
```
export DRILL_CONF_DIR=$DRILL_SITE
$DRILL_HOME/bin/drill-on-yarn.sh start
```

## Configuration Reference

The Getting Started section presented the minimum configuration needed to launch Drill under YARN. Additional configuration options are available for specialized cases. See drill-on-yarn-example.conf for information about the other options.
​
### Application Name

The application name appears when starting or stopping the Drill cluster and in the Drill-on-YARN web UI. Choose a name helpful to you:
```
app-name: "My Drill Cluster"
```

### Drill Distribution Archive

The Drill distribution archive is assumed to expand to create a folder that has the same name as the archive itself (minus the .tar.gz suffix). That is, the archive `apache-drill-x.y.z.tar.gz` is assumed to expand to a directory named apache-drill-x.y.z. Apache Drill archives follow this pattern. In specialized cases, you may have to create your own archive. If you do, it is most convenient if you follow the same pattern. However, if cannot follow the pattern, you can configure Drill-on-YARN to follow a custom pattern using the drill-install.dir-name option:
```
  drill-install: {
    client-path: "/path/to/your-custom-archive.tar.gz"
    dir-name: "your-drill-directory"
  }
```
Where:

* */path/to/your-custom-archive.tar.gz* is the location of your archive.
* *your-drill-directory* is the name of your Drill directory within the archive.

​### Customize Web UI Port

If you run multiple Drill clusters per YARN cluster, then YARN may choose to place two Drill AM processes on the same node. To avoid port conflicts, change the HTTP port for one or both of the Drill clusters:
```
drill.yarn:
  http: {
    port: 12345
  }
}
```

### Customize Application Master Settings

The following settings apply to the Application Master. All are prefixed with `drill.yarn.am`.

| Name | Description | Default |
| ---- | ----------- | ------- |
| memory-mb | Memory, in MB, to allocate to the AM. | 512 |
| vcores | Number of CPUS to allocate to the AM.| 1 |
| heap | Java heap for the AM. | 450M |
| node-label-expr | YARN node label expression to use to select nodes to run the AM. | None |

​5.5.​ Drillbit Customization
The following Drill-on-YARN configuration options control the Drillbit processes. All properties start with drill.yarn.drillbit.

| Name | Description | Default |
| ---- | ----------- | ------- |
| memory-mb | Memory, in MB, to allocate to the Drillbit. | 13000 |
| vcores | Number of CPUS to allocate to the AM. | 4 |
| disks | Number of disk equivalents consumed by Drill (on versions of YARN that support disk resources.) | 1 |
| heap | Java heap memory. | 4G |
| max-direct-memory | Direct (off-heap) memory for the Drillbit. | 8G |
| log-gc | Enables Java garbage collector logging | false |
| class-path | Additional class-path entries. | blank |

Note that the Drillbit node expression is set in the labeled pool below.

### Cluster Groups

YARN was originally designed for Map-Reduce jobs that can run on any node, and that often can be combined onto a single node. Compared to the traditional Map-Reduce jobs, Drill has additional constraints:

* Only one Drillbit (per Drill cluster) can run per host (to avoid port conflict.)
* Drillbits work best when launched on the same host as the data that the Drillbit is to scan.

Drill provides two ways to launch your Drill cluster: one for convenience, the other for production.

#### Basic Cluster

A basic cluster launches n drill-bits on distinct nodes anywhere in your YARN cluster. The basic cluster is great for testing and other informal tasks: just configure the desired vcores and memory, along with a number of nodes, then launch Drill. YARN will locate a set of suitable hosts anywhere on the YARN cluster.

#### Using YARN Labels

More typically you will decide the set of hosts that are to run Drill. Perhaps you want to run Drill on a few hosts per rack for data locality. Perhaps certain hosts are allocated to certain departments or groups within your organization. There are two ways to do this: using a queue label or using node labels. In either case, you start by Identify the hosts on which Drill should run using YARN labels as described in the YARN documentation.

(Note: To be tested; this works on MapR's version of YARN, needs testing on the Apache version.)

#### Labeled Queue

(TODO: Need information on queue labels.)

#### Labeled Hosts

Drill-on-YARN can handle node placement directly without the use of labeled queues. You use the "labeled" pool type. Then, set the drillbit-label-expr property to a YARN label expression that matches the nodes on which Drill should run. You will most often care only about Drillbit placement. Finally, indicate the number of Drillbits to run on the selected nodes.

(Need example)

## Drill-on-YARN Command-line Tool

Drill provides the drill-on-yarn command line tool to start, stop, resize and check the status of your Drill cluster. The tool is located in:
```
$DRILL_HOME/bin/drill-on-yarn.sh --site $DRILL_SITE command
```
Where command is one of those described below.

### Start the Drill Cluster

Start your drill cluster with the start command:
```
$DRILL_HOME/bin/drill-on-yarn.sh start
```
The command shows the startup status followed by a summary of the application:
```
Launching Drill-on-YARN...
Application ID: application_1462842354064_0001
Application State: ACCEPTED
Starting......
Application State: RUNNING
Tracking URL: http://10.250.50.31:8088/proxy/application_1462842354064_0001/
Application Master URL: http://10.250.50.31:8048/
```
The first line confirms which cluster is starting by displaying the cluster name from your configuration file. The next line shows YARN’s application ID and tracks the job status from Accepted to Running. Once the job starts, you’ll see YARN’s job tracking URL along with Drill-on-YARN’s web UI url. Use this URL to visit the web UI described below.

Once the application starts, the Drill-on-YARN writes an "appid" file into your master directory:
```
ls $MASTER_DIR
…
drillbits1.appid
```
The file name is the same as your Drill cluster ID. The file contains the id if the Drill-on-YARN application for use by the other commands described below.

You can run only one Drill AM at a time. If you attempt to start as second one from the same client machine on which you started the first, the client command will complain that the appid file already exists. If you attempt to start the cluster from a different node, then the second AM will detect the conflict and will shut down again.
​
### Drill Cluster Status

You can retrieve basic information about the Drill cluster as follows:
```
$DRILL_HOME/bin/drill-on-yarn.sh status
```
You will see output something like the following:
```
Application ID: application_1462842354064_0001
Application State: RUNNING
Host: yosemite/10.250.50.31
Tracking URL: http://10.250.50.31:8088/proxy/application_1462842354064_0001/
Queue: default
User: drilluser
Start Time: 2016-05-09 16:56:40
Application Name: Drill-on-YARN
AM State: LIVE
Target Drillbit Count: 1
Live Drillbit Count: 1
For more information, visit: http://10.250.50.31:8048/
```
The first two several lines give you information about YARN’s state: the application ID, the application state and YARN’s tracking URL for the application. Next is the host on which the Drill AM is running, the queue on which the application was placed and the user who submitted the application. The start time tells you when YARN started the application.

The next few lines are specific to Drill: the name of the application (which you configured in the Drill-on-YARN configuration file), the Drill application master URL, the number of drillbits you requested to run and the number actually running.

Finally, the last line gives you the URL to use to access the Drill-on-YARN web UI described below.

### Stop the Drill Cluster

You can stop the Drill cluster from the command line:
```
$DRILL_HOME/bin/drill-on-yarn.sh stop
```
Note that this command is "forceful", it kills any in-flight queries. The output tracks the shutdown and displays the final YARN application status:
```
Stopping Application ID: application_1462842354064_0001
Stopping...
Stopped.
Final status: SUCCEEDED
```

### Resize the Drill Cluster

You can add or remove nodes to your Drill cluster while the cluster runs using the resize command. You can specify the change either by giving the number of nodes you want to run:
```
$DRILL_HOME/bin/drill-on-yarn.sh resize 10
```
Or by specifying the change in node count: + for increase, - for decrease. To add two nodes:
```
$DRILL_HOME/bin/drill-on-yarn.sh resize +2
```
To remove three nodes:
```
$DRILL_HOME/bin/drill-on-yarn.sh resize -3
```
Drill will add nodes only if additional nodes are available from YARN. If you request to stop more nodes than are running, Drill stops all the running nodes.

Note that in the current version of Drill, stopping nodes is a forceful operation: any in-flight queries will fail.

### Clean the DFS Files

If you run Drill-on-YARN for a temporary cluster, Drill will leave the Drill software archive in your designated DFS directory. You can remove those files with the following:
```
$DRILL_HOME/bin/drill-on-yarn.sh clean
```
Specifically, the first start uploads your Drill archive to DFS. Stop leaves the archive in DFS. Subsequent start commands reuse the cached archive if it is the same size as the version on the local disk. Clean removes the cached file, forcing Drill to upload a fresh copy if you again restart the Drill cluster.
​
## Using the Web UI

Applications that run under YARN provide an Application Master (AM) process to manage the application’s task. Drill provides the Drill-on-YARN Application Master for this purpose. When you launch Drill using the command line tool, the tool asks YARN to launch Drill’s AM, which in turn launches your Drillbits.

The Drill application master provides a web UI to monitor cluster status and to perform simple operations such as increasing or decreasing cluster size, or stop the cluster.

You can reach the UI using the URL provided when the application starts. You can also follow the link from the YARN Resource Manager UI. Find the page for the Drill application. Click on the Tracking URL link.

The UI provides five pages:

* A main page that provides overall cluster status.
* A configuration page that lets you view the complete set of configuration variables which the Drill AM uses.
* Detailed list of the running Drillbits.
* Management page with a number of simple operations to resize or stop the cluster.
* A history of stopped, killed or failed Drillbits. Use this to diagnose problems.

### Main Page

The main page shows the state of the Drill cluster.

Drill Cluster Status: the state of the Drill cluster, one of:

* LIVE: Normal state: shows that your Drill cluster is running.
* ENDING: The cluster is in the process of shutting down.

There is no "ENDED" state: once the cluster is shut down, the AM itself exists and the web UI is no longer available.

**Target Drillbit Count**: The number of Drillbits to run in the cluster. The actual number may be less if Drillbits have not yet started, or if YARN cannot allocate enough containers.

**Live Drillbit Count**: Number of drillbits that are ready for use. These have successfully started, have registered with ZooKeeper, and are ready for use. You can see the detail of all Drillbits (including those in the process of starting or stopping) using the Drillbits page. Each Drillbit must run on a separate node, so this is also the number of nodes in the cluster running Drill.

**Total Drillbit Memory and Virtual Cores**: The total number of YARN resources currently allocated to running Drillbits.

**YARN Node Count, Memory and Virtual Cores**: Reports general information about YARN itself including the number of nodes, the total cluster memory and total number of virtual cores.

**Groups**: Lists the cluster groups defined in the configuration file (of which only one is currently supported), along with the target and actual number of Drillbits in that group.
​
### Configuration Page

The configuration page shows the complete set of configuration values used for the current run. The values come from your own configuration along with Drill-provided defaults. Use this page to diagnose configuration-related issues. Names are shown in fully-expanded form. That is the name "drill.yarn.http.port" refers to the parameter defined as follows in your configuration file:
```
drill.yarn:
  http: {
    port: 8048
  }
}
```

### Drillbits Page

The Drillbits page lists all drillbits in all states.

**ID**: A sequential number assigned to each new Drillbit. Numbers may not start with 1 if you’ve previously shut down some drillbits.

**Group**: The cluster group that started the Drillbit. (Cluster groups are from the configuration file.)

**Host**: The host name or IP address on which the Drillbit runs. If the Drillbit is in normal operating state, this field is also a hyperlink to the Web UI for the drillbit.

**State**: The operating state of the Drillbit. The normal state is "Running." The drillbit passes through a number of states as YARN allocates a container and launches a process, as the AM waits for the Drillbit to become registered in ZooKeeper, and so on. Similarly, the Drillbit passes through a different set of states during shutdown. Use this value to diagnose problems.

If the Drillbit is in a live state, then this field shows an "[X]" link that you can use to kill this particular Drillbit. Use this if the Drillbit has startup problems or seems unresponsive. During the shut-down process, the kill link disappears and is replaced with a "(Cancelled)" note.

**ZK State**: The ZooKeeper handshake state. Normal state is "START_ACK", meaning that the Drillbit has registered with ZooKeeper. This state is useful when diagnosing problems.

**Container ID**: The YARN-assigned container ID for the Drillbit task. The ID is a link, it takes you to the YARN Node Manager UI for the Drillbit task.

**Memory and Virtual Cores**: The amount of resources actually allocated to the Drillbit by YARN.

**Start Time**: The date and time (in your local time-zone, displayed in ISO format) when the Drillbit launch started.

This page will also display unmanaged Drillbits, if present. An unmanage Drillbit is one that is running, has registered with ZooKeeper, but was not started by the Drill Application Master. Likely the Drillbit was launched using the drillbit.sh script directly. Use the host name to locate the machine running the Drillbit if you want to convert that Drillbit to run under YARN.
​
### Manage Page

The Manage page allows you to resize or stop the cluster. You can resize the cluster by adding Drillbits, removing Drillbits or setting the cluster to a desired size.

Drill is a long-running application. In normal practice, you leave Drill running indefinitely. You would shut down your Drill cluster only to, say, perform an upgrade of the Drill software or to change configuration options. When you terminate your Drill cluster, any in-progress queries will fail. Therefore, a good practice is to perform the shut down with users so that Drill is not processing any queries at the time of the shut-down.

When removing or shutting-down the cluster, you will receive a confirmation page asking if you really do want to stop Drillbit processes. Click Confirm to continue.

### History Page

The History page lists all Drillbits which have failed, been killed, or been restarted. The History page allows you to detect failures and diagnose problems. Use the YARN container ID listed on this page to locate the log files for the Drillbit.

## Multiple Drill Clusters

Drill-on-YARN allows you to easily define multiple Drill clusters on a single YARN cluster. Each Drill cluster is a collection of Drillbits that work as an independent unit. For example, you might define one test cluster of a few machines on the same physical cluster that runs larger clusters for, say, Development and Marketing.

Drill clusters coordinate using ZooKeeper, so you must assign each cluster a distinct ZooKeeper entry. YARN may launch Drillbits from different clusters on the same physical node, so each Drill cluster must use a distinct set of ports. Since each cluster requires its own setup, you must create a separate site directory for each. The instructions below explain the required setup.

### Create a New Site Directory

Create a new site directory for your new cluster. Let’s say that your new cluster has the name "second". Using the same structure as above, create a new site directory under your master directory:
```
export SECOND_SITE=$MASTER_DIR/second
mkdir $SECOND_SITE
```
Copy files into this new site directory as you did to create the first one. You can copy and modify an existing set of files, or create the site from scratch.

### Configure Drill

At a minimum, you must set the following configuration options in drill-override.sh:
```
drill.exec: {
  cluster-id: "drillbits",
  zk: {
    root: "second"
    connect: "zk-host:2181"
  }

  rpc {
    user.server.port: 41010
    bit.server.port: 41011
  }
  http.port: 9047
}
```
You have two options for how your new cluster interacts with the existing cluster. The normal case is a shared-nothing scenario in which the two clusters are entirely independent at the configuration level. For this case, ensure that the zk.root name is distinct from any existing cluster.

In the more advanced case, if both clusters share the same zk.root value, then they will share settings such as storage plugins. If the clusters share the same root, then they must have distinct cluster-id values.

In addition, the three ports must have values distinct from all other clusters. In the example above, we’ve added a 1 to the first digit of the default port numbers; you can choose any available ports.

### Drill-on-YARN Configuration

Create the drill-on-yarn.conf file as described in an earlier section. The following must be distinct for your cluster:
```
drill.yarn: {
  app-name: "Second Cluster"

  dfs: {
    app-dir: "/user/drill2"
  }

  http : {
    port: 9048
  }
}
```
That is, give your cluster a distinct name, a distinct upload directory in DFS, and a distinct port number.

### Start the Cluster

Use the site directory for the second cluster to start the cluster:
```
$DRILL_HOME/bin/drill-on-yarn.sh --site $SECOND_SITE start
```

## Enabling Web UI Security

Drill-on-YARN provides a web UI as described earlier. By default, the UI is open to everyone. You can secure the UI using either a simple predefined user name and password, or using Drill’s user authentication.
​
### Simple Security

Simple security is enabled using three configuration settings:
```
drill.yarn.http: {
    auth-type: "simple"
    user-name: "bob"
    password: "secret"
}
```
Restart the Drill-on-YARN application master. When you visit the web UI, a login page should appear, prompting you to log in. Only the above user and password are valid.

Obviously, simple security is not highly secure; but it is handy for testing, prototypes and the like.

### Using Drill’s User Authentication

Drill-on-YARN can use Drill’s authentication system. In this mode, the user name and password must match that of the user that started the Drill-on-YARN application. To enable Drill security:
```
drill.yarn.http: {
    auth-type: "drill"
}
```
You must have previously enabled Drill user authentication as described in the [Drill documentation](http://drill.apache.org/docs/configuring-user-authentication/):
```
  rpc {
    user.server.port: 41010
    bit.server.port: 41011
  }
  http.port: 9047
```

## Release Notes

Drill-on-YARN creates a tighter coupling between Drill and Hadoop than did previous Drill versions. You should be aware of a number of compatibility issues.

### Migrating the `$DRILL_HOME/conf/drill-env.sh` Script

Prior to Drill 1.8, the `drill-env.sh` script contained Drill defaults, distribution-specific settings, and configuration specific to your application ("site".) In Drill 1.8, the Drill and distribution settings are moved to other locations. The site-specific settings change in format to allow YARN to override them. The following section details the changes you must make if you reuse a `drill-env.sh` file from a prior release. (If you create a new file, you can skip this section.)

At the end of this process, your file should contain just two lines for memory settings, plus any additional custom settings you may have added.
​
#### Memory Settings

Most Drill configuration is done via the Drill configuration file and the configuration registry. However, certain options must be set at the time that Drill starts; such options are configured in the `$DRILL_HOME/conf/drill-env.sh` file. Under YARN, these settings are set in the YARN configuration. To ensure that the YARN configuration options are used, you must modify your existing `drill-env.sh` file as follows. (If you are installing Drill fresh, and don’t have an existing file, you can skip these steps. The Drill 1.8 and later files already have the correct format.)

Find the two lines that look like this:
```
DRILL_MAX_DIRECT_MEMORY="8G"
DRILL_HEAP="4G"
```
Replace them with the following two lines.
```
export DRILL_MAX_DIRECT_MEMORY=${DRILL_MAX_DIRECT_MEMORY:-"8G"}
export DRILL_HEAP=${DRILL_HEAP:-"4G"}
```
Copy the actual values from the old lines to the new ones (e.g. the "8G" and "4G" values.) Those are the values that Drill when use if you launch it outside of YARN. The new lines ensure that these values are replaced by those set by Drill-on-YARN when running under YARN.

If you omit this change, then Drill will ignore your memory settings in Drill-on-YARN, resulting in a potential mismatch between the Drill memory settings and the amount of memory requested from YARN.

#### Remove General Drill Settings

If you are reusing the drill-env.sh from a prior release, find lines similar to the following:
```
export DRILL_JAVA_OPTS="-Xms$DRILL_HEAP -Xmx$DRILL_HEAP -XX:MaxDirectMemorySize=$DRILL_MAX_DIRECT_MEMORY \ -XX:MaxPermSize=512M -XX:ReservedCodeCacheSize=1G \ -Ddrill.exec.enable-epoll=true"
```
Compare these lines to the original `drill-env.sh` to determine if you modified the lines. These general Drill defaults now reside in other Drill scripts and can be remove from your site-specific version of `drill-env.sh`.

#### Remove Distribution-specific Settings

Some Drill distributions added distribution-specific settings to the `drill-env.sh script`. Drill 1.8 moves these settings to a new `$DRILL_HOME/conf/distrib-env.sh` file. Compare drill-env.sh and distrib-env.sh. Lines that occur in both files should be removed from drill-env.sh. If you later find you need to change the settings in `distrib-env.sh`, simply copy the line to `drill-env.sh` and modify the line. Drill reads `drill-env.sh` after `distrib-env.sh`, so your site-specific settings will replace the default distribution settings.

### Hadoop Jar Files

Drill depends on certain Hadoop Java jar files which the Drill distribution includes in the `$DRILL_HOME/jars/3rdparty` directory. Although YARN offers Drill a Java class-path with the Hadoop jars, Drill uses its own copies instead to ensure Drill runs under the same configuration with which it was tested. Drill distributions that are part of a complete Hadoop distribution (such as the MapR distribution) have already verified version compatibility for you. If you are assembling your own Hadoop and Drill combination, you should verify that the Hadoop version packaged with Drill is compatible with the version running our YARN cluster.

### `$DRILL_HOME/conf/core-site.xml` Issue

Prior versions of Drill included a file in the `$DRILL_HOME/conf` directory called `core-site.xml`. As it turns out, YARN relies on a file of the same name in the Hadoop configuration directory. The Drill copy hides the YARN copy, preventing YARN from operating correctly. For this reason, version 1.8 of Drill renames the example file to `core-site-example.xml`.

When upgrading an existing Drill installation, do not copy the file from your current version of Drill to the new version.

If you modified core-site.xml, we recommend you merge your changes with Hadoop’s `core-site.xml` file.
​
### Mac OS setsid Issue

YARN has a bug which prevents Drillbits from properly shutting down when run under YARN on Mac OS.

[YARN-3066](https://issues.apache.org/jira/browse/YARN-3066): Hadoop leaves orphaned tasks running after job is killed

You may encounter this problem if you use a Mac to try out the YARN integration for Drill. The symptom is that you:

* Start Drill as described below
* Attempt to stop the Drill cluster as described below
* Afterwards use `jps` to list Java processes and find that Drillbit is still running.

The problem is that the setsid command is not available under MacOS. The workaround is to use the open source equivalent:

* Install the [XCode command line tools](https://developer.apple.com/library/ios/technotes/tn2339/_index.html).
* Using git, clone ersatz-ssid from [`https://github.com/jerrykuch/ersatz-setsid`](https://github.com/jerrykuch/ersatz-setsid)
* Cd into the ersatz-ssid directory and type: `make`
* Copy the resulting executable into `/usr/bin`: `sudo cp setsid /usr/bin`

### Apache YARN Node Labels and Labeled Queues

The Drill-on-YARN feature should work with Apache YARN node labels, but such support is currently not tested. Early indications are that the Apache YARN label documentation does not quite match the implementation, and that labels are very tricky. The Drill team is looking forward to community assistance to better support Apache YARN node labels.

### Apache YARN RM Failure and Recovery

Drill-on-YARN currently does not gracefully handle Apache YARN Resource Manager failure and recovery. According to the Apache YARN documentation, a failed RM may restart any in-flight Application Masters, then alert the AM of any in-flight tasks. Drill-on-YARN is not currently aware of this restart capability. Existing Drillbits will continue to run, at least for a time. They may be reported in the Drill-on-YARN web UI as unmanaged. Presumably, eventually YARN will kill the old Drillbits at which time Drill-on-YARN should start replacements. This is an area for future improvement based on community experience.

### Configuring User Authentication

The [Drill Documentation](http://drill.apache.org/docs/configuring-user-authentication/) describes how to configure user authentication using PAM. Two revisions are needed for Drill-on-YARN:

* Configure user authentication for Drill using a site directory
* Configure Drill-on-YARN authentication

The existing instructions explain how to configure PAM authentication by changing Drill config files and adding libraries to the Drill distribution directory. If you use that approach, you must rebuild the Drill software archive as described elsewhere in this document. It is easier, however, to configure security using the site directory as explained below.

#### Configuring User Authentication for the Drillbit

Existing instructions:

Untar the file, and copy the `libjpam.so` file into a directory that does not contain other Hadoop components.

Example: `/opt/pam/`

Revised instructions: You have the option of deploying the library to each node (as described above), or allowing YARN to distribute the library. To have YARN do the distribution:

Create the following directory:
`
$DRILL_SITE/lib
`
Untar the file and copy `libjpam.so` into `$DRILL_SITE/lib`.

Existing instructions:

> Add the following line to `<DRILL_HOME>/conf/drill-env.sh`, including the directory where the `libjpam.so` file is located:
>
> `export DRILLBIT_JAVA_OPTS="-Djava.library.path=<directory>"`
>
> Example: `export DRILLBIT_JAVA_OPTS="-Djava.library.path=/opt/pam/"`

Revised instructions:

If you are not using Drill-on-YARN, set a new environment variable in `drill-env.sh`:
```
export DRILL_JAVA_LIB_PATH="<directory>"
```
If you install the library yourself, either set `DRILL_JAVA_LIB_PATH` as above, or set the following in `drill-on-yarn.conf`:
```
drill.yarn.files: {
   library-path: "<directory>"
}
```
**Note**: do not explicitly set `DRILLBIT_JAVA_OPTS` as you may have done in previous releases; Drill won’t know how to add your `$DRILL_SITE/lib` directory or how to interpret the library-path item above.

If you put the library in the `$DRILL_SITE/lib` directory, then Drill-on-YARN will automatically do the needed configuration; there is nothing more for you to do.

### Implementing and Configuring a Custom Authenticator

Most of the existing steps are fine, except for step 3. Current text:

> Add the JAR file that you built to the following directory on each Drill node:

> `<DRILLINSTALL_HOME>/jars`

Revised text: Add the JAR file that you built to the following directory on each Drill node:
```
$DRILL_SITE/jars
```
If running under YARN, you only need to add the jar to the site directory on the node from which you start Drill-on-YARN (which we’ve referred to as $MASTER_DIR.)

Also, step 5:

Restart the Drillbit process on each Drill node.
```
<DRILLINSTALL_HOME>/bin/drillbit.sh restart
```
Under YARN, restart the YARN cluster:
```
$DRILL_HOME/bin/drill-on-yarn.sh --site $DRILL_SITE restart
```

#### Configuring User Authentication for the Application Master

If you configure user authentication for Drill, then user authentication is automatically configured in the Application Master also. Only users with admin privileges can use the AM web UI.

## Testing User Authentication on the Mac

The [Drill Documentation](http://drill.apache.org/docs/configuring-user-authentication/) describes how to configure user authentication using PAM, including instructions for downloading a required native library. However, if you are testing security on the Mac, the referenced library does not work on modern Macs. Instead, see the work-around in [DRILL-4756](https://issues.apache.org/jira/browse/DRILL-4756).
​
​## `drill-env.sh` Settings

When running Drill outside of YARN, you can set many startup options in `drill-env.sh`. Most users accept the defaults. However, some users require specialized settings.

Under YARN, Drill still reads your `$DRILL_SITE/drill-env.sh` file to pick up configuration. However, for most options, Drill-on-YARN provides configuration options in `drill-on-yarn.conf` to set options that were formerly set in `drill-env.sh`. The following provides a mapping:


| `drill-env.sh` Environment Variable | `drill-on-yarn.conf` Configuration Parameter |
| ----------------------------------- | -------------------------------------------- |
| DRILL_MAX_DIRECT_MEMORY * | drill.yarn.drillbit.max-direct-memory |
| DRILL_HEAP * | drill.yarn.drillbit.heap |
| DRILL_JAVA_OPTS | drill.yarn.drillbit.vm-args (Added to those in `drill-env.sh`.) |
| SERVER_GC_OPTS (to add GC logging) | `drill.yarn.drillbit.log-gc` (To enable GC logging) |
| DRILL_HOME | Set automatically when files are localized (`drill.yarn.drill-install.localize` is true), else `drill.yarn.drill-install.drill-home`.
| DRILL_CONF_DIR | Set automatically when files are localized, else uses the normal defaults. |
| DRILL_LOG_DIR | Set automatically to point to YARN’s log directory unless disabled by setting `drill.yarn.drillbit.disable-yarn-logs` to false. If disabled, uses the normal Drill defaults. |
| DRILL_CLASSPATH_PREFIX * | `drill.yarn.drillbit.prefix-class-path` |
| HADOOP_CLASSPATH * | `drill.yarn.hadoop.class-path` (or, better `drill.yarn.drillbit.extn-class-path`.) |
| HBASE_CLASSPATH * | `drill.yarn.hadoop.hbase-class-path` (or, better `drill.yarn.drillbit.extn-class-path`.) |
| EXTN_CLASSPATH * (New in Drill 1.8.) | `drill.yarn.drillbit.extn-class-path` |
| DRILL_CLASSPATH * | `drill.yarn.drillbit.class-path` |

\* If  you set these options in both places, then the value in `drill-env.sh` takes precedence.

Note that `EXTN_CLASSPATH` (and `drill.yarn.drillbit.extn-class-path`) are a newer, more general way to add extensions. Rather than setting specific Hadoop or HBase variables, you can combine any number of extensions into the single extension classpath.

## Troubleshooting

Drill-on-YARN starts a complex chain of events: the client starts the AM and the AM starts Drillbits, both using YARN. Many opportunities exist for configuration issues to derail the process. Below are a number of items to check if things go wrong.

### Client Start

The Drill-on-YARN client prints all messages to the console. Some common errors are:

#### Missing `HADOOP_HOME`

Drill-on-YARN requires access to your Hadoop configuration as described above. The client will display an error and fail if it is unable to load the DFS or YARN configuration, typically because HADOOP_HOME is not set.
​
#### Missing/wrong Drill Archive

Drill-on-YARN uploads your Drill archive to DFS. The client will fail if the archive configuration is missing, if the archive does not exist, is not readable, or is not in the correct format. Check that the drill.yarn.drill-install.client-path provides the full path name to the Drill archive.

#### DFS Configuration or Connection Problems

The Drill-on-YARN client uploads the Drill archive and your site directory to your DFS. Possible problems here include:

* Missing DFS configuration in the Hadoop configuration folder
* Incorrect DFS configuration (wrong host or port)
* Missing permissions to write to the folder identified by the `drill.yarn.dfs.app-dir` configuration property (`/user/drill` by default.)
​
#### Wrong Version of the Drill Archive

Drill-on-YARN uploads a new copy of your site archive each time you start your Drill cluster. However, the Drill software archive is large, so Drill-on-YARN uploads the archive only when it changes. Drill detects changes by comparing the size of the DFS copy with your local copy. Most of the time this works fine. However, if you suspect that Drill has not uploaded the most recent copy, you can force the client to perform an upload by either manually deleting the Drill archive from DFS, or using the -f option:
```
$DRILL_HOME/bin/drill-on-yarn.sh --site $DRILL_SITE start -f
```

#### Site Directory Problems

Drill creates a tar archive of your site directory using the following command:
```
tar -C $DRILL_SITE -czf /some/tmp/dir/temp-name.tar.gz
```
For some temporary directory selected by Java. This command can fail if your version of tar does not recognize the above arguments, if the site directory is not readable, or the temporary file cannot be created.

#### YARN Application Launch Failure

YARN may fail to launch the Drill AM for a number of reasons. The user running the Drill-on-YARN client may not have permission to launch YARN applications. YARN itself may encounter errors. Check the YARN log files for possible causes.

#### Diagnosing Post-Launch Problems

If the Drill AM starts, but does not launch Drillbits, your next step is to check the Drill AM web UI using the link provided when the application starts. If the AM exits quickly, then the URL may not be valid.

Instead, you can use YARN’s own Resource Manager UI to check the status of the application, using the Application ID provided when the application starts. Look at the application's log files for possible problems.

#### Application Master Port Conflict

The Drill-on-YARN Application Master provides a web UI on port 8048 (one greater than the Drill web UI port) by default. However, if another application is already bound to that port, the AM will fail to launch. Select a different port as follows:
```
drill.yarn.http.port: 12345
```

#### Preserve the YARN Launch Context

Some problems are easiest to diagnose if you set the YARN option to preserve the application’s launch context:

((Need option))

Then, locate the Node Manager host that launched the AM, and locate the nmPrivate subfolder for the application. Several Drill-on-YARN settings are passed to the Drill AM as environment variables. Review the launch_container.sh script to look for incorrect settings.

Next, look for the PWD variable and visit that directory to ensure that the drill and site directories are properly created and contain the expected files. As noted above, ensure that the subdirectory of the drill directory either has the same name as your archive file, or is identified using the app-dir property as defined above.

Finally, look at the directory used to create the stdout and stderr files in the launch container. Visit that directory and review the various log file to look for possible problems.

### AM Failure

#### AM Resource Limits

YARN is very fragile, if configuration is not exactly right, our tasks will silently fail or will hang indefinitely. The Drill AM is a small process, but must be within the limits assigned to all YARN tasks, to AM’s on the assigned queue, to AM limits on the assigned queue, and AM limits for the user. Submitting an AM that exceeds any of these limits will lead to silent failure.

#### Multiple AMs

It is easy to accidentally start multiple AMs for the same Drill cluster. Two lines of defense protect against this fault:
The Drill-on-YARN client look for an existing appid file and refuses to start a new AM when the file is present. (Use the -f file if the file is suprious.)

The AM registers with ZK and will automatically shut down if another AM is already registered.

## Recreate the Drill Archive

The above instructions assume that you make no changes to your Drill installation; that all your site-specific files reside in a separate site directory. Prior Drill versions put all configuration within the Drill directory. If you chose to continue that pattern, or if you change the Drill installation, you must rebuild the Drill archive and configure Drill-on-YARN to upload your custom archive in place of the standard archive. The steps below explain the process.

To change the contents of the Drill archive, you must perform two steps:

* Create an archive of the Drill install directory.
* Configure Drill-on-YARN to use that archive.

### Create the Drill Archive

The first step is to create the master archive of your Drill files. Do the following with the master directory as the current directory.
```
cd $MASTER_DIR
tar -czf archive-name.tar.gz $DRILL_HOME
```
Replace "archive-name" with the name you chose above.

### Configure Drill-on-YARN to Use the Archive

Modify your drill-on-yarn.conf file to identify the archive you must created:
```
drill.yarn.drill-install.client-path: "/path/to/archive-name.tar.gz"
```
YARN expects that, when extracting the master directory, that it creates a directory called archive-name that contains the Drill directories conf, jars, and so on. However, if archive-name is different than the name of the $DRILL_HOME directory, simply configure the correct name of the expanded folder:
```
drill.yarn.drill-install.dir-name: "your-dir-name"
```
