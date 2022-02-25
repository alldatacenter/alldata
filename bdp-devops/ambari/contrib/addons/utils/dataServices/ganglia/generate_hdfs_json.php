<?php
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


$data = array
  (
     'Global' => array
     (
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&g=load_report',
           'title' => 'Load Report',
           'description' => 'Key load metrics on the HDFS NameNode',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=load_report'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&g=mem_report',
           'title' => 'Memory Report',
           'description' => 'Key memory metrics on the HDFS NameNode',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=mem_report'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&g=cpu_report',
           'title' => 'CPU Report',
           'description' => 'Key CPU metrics on the HDFS NameNode',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=cpu_report'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&g=network_report',
           'title' => 'Network I/O Report',
           'description' => 'Key network I/O metrics on the HDFS NameNode',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=network_report'
        )
     ),
     'CPU' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=cpu_aidle',
           'title' => 'CPU AIdle',
           'description' => 'Percent of time since boot idle CPU',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=cpu_aidle'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=cpu_idle',
           'title' => 'CPU Idle',
           'description' => 'Percent CPU idle',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=cpu_idle'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=cpu_nice',
           'title' => 'CPU Nice',
           'description' => 'Percent CPU nice',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=cpu_nice'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=cpu_system',
           'title' => 'CPU System',
           'description' => 'Percent CPU system',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=cpu_system'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=cpu_user',
           'title' => 'CPU User',
           'description' => 'Percent CPU user',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=cpu_user'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=cpu_wio',
           'title' => 'CPU Wait I/O',
           'description' => 'Percent CPU spent waiting on I/O',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=cpu_wio'
        )
     ),
     'Disk' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=disk_free',
           'title' => 'Disk Free',
           'description' => 'Total free disk space',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=disk_free'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=disk_total',
           'title' => 'Disk Total',
           'description' => 'Total available disk space',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=disk_total'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=part_max_used',
           'title' => 'Max Disk Partition Used',
           'description' => 'Maximum percent used for all disk partitions',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=part_max_used'
        )
     ),
     'Load' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=load_fifteen',
           'title' => 'Load Fifteen',
           'description' => 'Fifteen minute load average',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=load_fifteen'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=load_five',
           'title' => 'Load Five',
           'description' => 'Five minute load average',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=load_five'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=load_one',
           'title' => 'Load One',
           'description' => 'One minute load average',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=load_one'
        )
     ),
     'Memory' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=mem_buffers',
           'title' => 'Memory Buffers',
           'description' => 'Amount of buffered memory',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=mem_buffers'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=mem_cached',
           'title' => 'Cached Memory',
           'description' => 'Amount of cached memory',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=mem_cached'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=mem_free',
           'title' => 'Free Memory',
           'description' => 'Amount of available memory',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=mem_free'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=mem_shared',
           'title' => 'Shared Memory',
           'description' => 'Amount of shared memory',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=mem_shared'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=swap_free',
           'title' => 'Free Swap Space',
           'description' => 'Total amount of swap memory',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=swap_free'
        )
     ),
     'Network' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=bytes_in',
           'title' => 'Bytes Received',
           'description' => 'Number of bytes in per second',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=bytes_in'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=bytes_out',
           'title' => 'Bytes Sent',
           'description' => 'Number of bytes out per second',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=bytes_out'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=pkts_in',
           'title' => 'Packets Received',
           'description' => 'Packets in per second',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=pkts_in'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=pkts_out',
           'title' => 'Packets Sent',
           'description' => 'Packets out per second',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=pkts_out'
        )
     ),
     'Process' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=proc_run',
           'title' => 'Total Running Processes',
           'description' => 'Total number of running processes',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=proc_run'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=proc_total',
           'title' => 'Total Processes',
           'description' => 'Total number of processes',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=proc_total'
        )
     ),
     'FSNamesystem' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.BlockCapacity',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.BlockCapacity'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.BlocksTotal',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.BlocksTotal'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.CapacityRemainingGB',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.CapacityRemainingGB'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.CapacityTotalGB',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.CapacityTotalGB'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.CapacityUsedGB',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.CapacityUsedGB'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.CorruptBlocks',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.CorruptBlocks'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.ExcessBlocks',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.ExcessBlocks'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.FilesTotal',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.FilesTotal'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.MissingBlocks',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.MissingBlocks'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.PendingDeletionBlocks',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.PendingDeletionBlocks'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.PendingReplicationBlocks',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.PendingReplicationBlocks'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.ScheduledReplicationBlocks',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.ScheduledReplicationBlocks'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.TotalLoad',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.TotalLoad'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.FSNamesystem.UnderReplicatedBlocks',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.FSNamesystem.UnderReplicatedBlocks'
        )
     ),
     'NameNode' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.AddBlockOps',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.AddBlockOps'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.CreateFileOps',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.CreateFileOps'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.DeleteFileOps',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.DeleteFileOps'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.FileInfoOps',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.FileInfoOps'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.FilesAppended',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.FilesAppended'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.FilesCreated',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.FilesCreated'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.FilesDeleted',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.FilesDeleted'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.FilesInGetListingOps',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.FilesInGetListingOps'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.FilesRenamed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.FilesRenamed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.GetBlockLocations',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.GetBlockLocations'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.GetListingOps',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.GetListingOps'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.JournalTransactionsBatchedInSync',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.JournalTransactionsBatchedInSync'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.SafemodeTime',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.SafemodeTime'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.Syncs_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.Syncs_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.Syncs_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.Syncs_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.Transactions_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.Transactions_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.Transactions_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.Transactions_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.blockReport_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.blockReport_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.blockReport_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.blockReport_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=dfs.namenode.fsImageLoadTime',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=dfs.namenode.fsImageLoadTime'
        )
     ),
     'JVM' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.gcCount',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.gcCount'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.gcTimeMillis',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.gcTimeMillis'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.logError',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.logError'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.logFatal',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.logFatal'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.logInfo',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.logInfo'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.logWarn',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.logWarn'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.memHeapCommittedM',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.memHeapCommittedM'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.memHeapUsedM',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.memHeapUsedM'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.memNonHeapCommittedM',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.memNonHeapCommittedM'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.memNonHeapUsedM',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.memNonHeapUsedM'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.threadsBlocked',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.threadsBlocked'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.threadsNew',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.threadsNew'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.threadsRunnable',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.threadsRunnable'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.threadsTerminated',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.threadsTerminated'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.threadsTimedWaiting',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.threadsTimedWaiting'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=jvm.metrics.threadsWaiting',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=jvm.metrics.threadsWaiting'
        )
     ),
     'MetricsSystem' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.dropped_pub_all',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.dropped_pub_all'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.num_sinks',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.num_sinks'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.num_sources',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.num_sources'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_imax_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_imax_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_imin_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_imin_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_max_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_max_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_min_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_min_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_stdev_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.publish_stdev_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.dropped',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.dropped'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.latency_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.latency_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.latency_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.latency_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.qsize',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.qsize'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_imax_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_imax_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_imin_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_imin_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_max_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_max_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_min_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_min_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_stdev_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=metricssystem.MetricsSystem.snapshot_stdev_time'
        )
     ),
     'RPC' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.NumOpenConnections',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.NumOpenConnections'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.ReceivedBytes',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.ReceivedBytes'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.RpcProcessingTime_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.RpcProcessingTime_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.RpcProcessingTime_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.RpcProcessingTime_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.RpcQueueTime_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.RpcQueueTime_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.RpcQueueTime_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.RpcQueueTime_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.SentBytes',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.SentBytes'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.callQueueLen',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.callQueueLen'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.rpcAuthenticationFailures',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.rpcAuthenticationFailures'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.rpcAuthenticationSuccesses',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.rpcAuthenticationSuccesses'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.rpcAuthorizationFailures',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.rpcAuthorizationFailures'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpc.rpc.rpcAuthorizationSuccesses',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpc.rpc.rpcAuthorizationSuccesses'
        )
     ),
     'RPCDetailed' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.addBlock_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.addBlock_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.blockReceived_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.blockReceived_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.blockReport_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.blockReport_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.complete_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.complete_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.create_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.create_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.delete_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.delete_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getFileInfo_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getFileInfo_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getListing_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getListing_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getProtocolVersion_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getProtocolVersion_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.mkdirs_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.mkdirs_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.register_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.register_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.renewLease_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.renewLease_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.sendHeartbeat_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.sendHeartbeat_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.setPermission_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.setPermission_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.versionRequest_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.versionRequest_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.addBlock_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.addBlock_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.blockReceived_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.blockReceived_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.blockReport_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.blockReport_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.complete_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.complete_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.create_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.create_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.delete_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.delete_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getFileInfo_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getFileInfo_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getListing_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getListing_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getProtocolVersion_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.getProtocolVersion_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.mkdirs_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.mkdirs_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.register_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.register_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.renewLease_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.renewLease_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.sendHeartbeat_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.sendHeartbeat_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.setPermission_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.setPermission_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.versionRequest_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=rpcdetailed.rpcdetailed.versionRequest_num_ops'
        )
     ),
     'UGI' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=ugi.ugi.loginFailure_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=ugi.ugi.loginFailure_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=ugi.ugi.loginFailure_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=ugi.ugi.loginFailure_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=ugi.ugi.loginSuccess_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=ugi.ugi.loginSuccess_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%NameNodeClusterName%&m=ugi.ugi.loginSuccess_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%NameNodeClusterName%&m=ugi.ugi.loginSuccess_num_ops'
        )
     )
  );

echo json_encode($data);

?>
