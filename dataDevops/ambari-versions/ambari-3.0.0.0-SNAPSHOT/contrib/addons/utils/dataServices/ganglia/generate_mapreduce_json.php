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
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&g=load_report',
           'title' => 'Load Report',
           'description' => 'Key load metrics on the MapReduce JobTracker',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=load_report'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&g=mem_report',
           'title' => 'Memory Report',
           'description' => 'Key memory metrics on the MapReduce JobTracker',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mem_report'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&g=cpu_report',
           'title' => 'CPU Report',
           'description' => 'Key CPU metrics on the MapReduce JobTracker',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=cpu_report'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&g=network_report',
           'title' => 'Network I/O Report',
           'description' => 'Key network I/O metrics on the MapReduce JobTracker',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=network_report'
        )
     ),
     'CPU' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=cpu_aidle',
           'title' => 'CPU AIdle',
           'description' => 'Percent of time since boot idle CPU',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=cpu_aidle'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=cpu_idle',
           'title' => 'CPU Idle',
           'description' => 'Percent CPU idle',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=cpu_idle'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=cpu_nice',
           'title' => 'CPU Nice',
           'description' => 'Percent CPU nice',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=cpu_nice'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=cpu_system',
           'title' => 'CPU System',
           'description' => 'Percent CPU system',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=cpu_system'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=cpu_user',
           'title' => 'CPU User',
           'description' => 'Percent CPU user',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=cpu_user'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=cpu_wio',
           'title' => 'CPU Wait I/O',
           'description' => 'Percent CPU spent waiting on I/O',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=cpu_wio'
        )
     ),
     'Disk' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=disk_free',
           'title' => 'Disk Free',
           'description' => 'Total free disk space',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=disk_free'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=disk_total',
           'title' => 'Disk Total',
           'description' => 'Total available disk space',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=disk_total'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=part_max_used',
           'title' => 'Max Disk Partition Used',
           'description' => 'Maximum percent used for all disk partitions',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=part_max_used'
        )
     ),
     'Load' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=load_fifteen',
           'title' => 'Load Fifteen',
           'description' => 'Fifteen minute load average',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=load_fifteen'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=load_five',
           'title' => 'Load Five',
           'description' => 'Five minute load average',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=load_five'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=load_one',
           'title' => 'Load One',
           'description' => 'One minute load average',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=load_one'
        )
     ),
     'Memory' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mem_buffers',
           'title' => 'Memory Buffers',
           'description' => 'Amount of buffered memory',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mem_buffers'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mem_cached',
           'title' => 'Cached Memory',
           'description' => 'Amount of cached memory',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mem_cached'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mem_free',
           'title' => 'Free Memory',
           'description' => 'Amount of available memory',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mem_free'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mem_shared',
           'title' => 'Shared Memory',
           'description' => 'Amount of shared memory',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mem_shared'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=swap_free',
           'title' => 'Free Swap Space',
           'description' => 'Total amount of swap memory',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=swap_free'
        )
     ),
     'Network' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=bytes_in',
           'title' => 'Bytes Received',
           'description' => 'Number of bytes in per second',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=bytes_in'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=bytes_out',
           'title' => 'Bytes Sent',
           'description' => 'Number of bytes out per second',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=bytes_out'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=pkts_in',
           'title' => 'Packets Received',
           'description' => 'Packets in per second',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=pkts_in'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=pkts_out',
           'title' => 'Packets Sent',
           'description' => 'Packets out per second',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=pkts_out'
        )
     ),
     'Process' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=proc_run',
           'title' => 'Total Running Processes',
           'description' => 'Total number of running processes',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=proc_run'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=proc_total',
           'title' => 'Total Processes',
           'description' => 'Total number of processes',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=proc_total'
        )
     ),
     'MapReduce Queue' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_completed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_completed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_failed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_failed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_killed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_killed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_preparing',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_preparing'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_running',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_running'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_submitted',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.jobs_submitted'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.maps_completed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.maps_completed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.maps_failed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.maps_failed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.maps_killed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.maps_killed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.maps_launched',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.maps_launched'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.reduces_completed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.reduces_completed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.reduces_failed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.reduces_failed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.reduces_killed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.reduces_killed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.reduces_launched',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.reduces_launched'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.reserved_map_slots',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.reserved_map_slots'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.reserved_reduce_slots',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.reserved_reduce_slots'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.waiting_maps',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.waiting_maps'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.Queue.waiting_reduces',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.Queue.waiting_reduces'
        )
     ),
     'JobTracker' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.blacklisted_maps',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.blacklisted_maps'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.blacklisted_reduces',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.blacklisted_reduces'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.heartbeats',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.heartbeats'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_completed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_completed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_failed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_failed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_killed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_killed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_preparing',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_preparing'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_running',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_running'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_submitted',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.jobs_submitted'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.map_slots',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.map_slots'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.maps_completed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.maps_completed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.maps_failed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.maps_failed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.maps_killed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.maps_killed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.maps_launched',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.maps_launched'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.occupied_map_slots',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.occupied_map_slots'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.occupied_reduce_slots',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.occupied_reduce_slots'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.reduce_slots',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.reduce_slots'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.reduces_completed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.reduces_completed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.reduces_failed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.reduces_failed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.reduces_killed',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.reduces_killed'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.reduces_launched',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.reduces_launched'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.reserved_map_slots',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.reserved_map_slots'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.reserved_reduce_slots',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.reserved_reduce_slots'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.running_maps',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.running_maps'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.running_reduces',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.running_reduces'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.trackers',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.trackers'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.trackers_blacklisted',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.trackers_blacklisted'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.trackers_decommissioned',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.trackers_decommissioned'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.trackers_graylisted',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.trackers_graylisted'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.waiting_maps',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.waiting_maps'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=mapred.jobtracker.waiting_reduces',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=mapred.jobtracker.waiting_reduces'
        )
     ),
     'JVM' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.gcCount',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.gcCount'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.gcTimeMillis',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.gcTimeMillis'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.logError',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.logError'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.logFatal',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.logFatal'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.logInfo',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.logInfo'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.logWarn',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.logWarn'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.memHeapCommittedM',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.memHeapCommittedM'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.memHeapUsedM',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.memHeapUsedM'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.memNonHeapCommittedM',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.memNonHeapCommittedM'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.memNonHeapUsedM',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.memNonHeapUsedM'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.threadsBlocked',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.threadsBlocked'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.threadsNew',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.threadsNew'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.threadsRunnable',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.threadsRunnable'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.threadsTerminated',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.threadsTerminated'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.threadsTimedWaiting',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.threadsTimedWaiting'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=jvm.metrics.threadsWaiting',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=jvm.metrics.threadsWaiting'
        )
     ),
     'MetricsSystem' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.dropped_pub_all',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.dropped_pub_all'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.num_sinks',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.num_sinks'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.num_sources',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.num_sources'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_imax_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_imax_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_imin_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_imin_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_max_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_max_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_min_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_min_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_stdev_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.publish_stdev_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.dropped',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.dropped'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.latency_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.latency_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.latency_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.latency_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.qsize',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.sink.ganglia.qsize'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_imax_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_imax_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_imin_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_imin_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_max_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_max_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_min_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_min_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_stdev_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=metricssystem.MetricsSystem.snapshot_stdev_time'
        )
     ),
     'RPC' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.NumOpenConnections',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.NumOpenConnections'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.ReceivedBytes',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.ReceivedBytes'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.RpcProcessingTime_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.RpcProcessingTime_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.RpcProcessingTime_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.RpcProcessingTime_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.RpcQueueTime_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.RpcQueueTime_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.RpcQueueTime_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.RpcQueueTime_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.SentBytes',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.SentBytes'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.callQueueLen',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.callQueueLen'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.rpcAuthenticationFailures',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.rpcAuthenticationFailures'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.rpcAuthenticationSuccesses',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.rpcAuthenticationSuccesses'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.rpcAuthorizationFailures',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.rpcAuthorizationFailures'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpc.rpc.rpcAuthorizationSuccesses',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpc.rpc.rpcAuthorizationSuccesses'
        )
     ),
     'RPCDetailed' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getBuildVersion_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getBuildVersion_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getBuildVersion_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getBuildVersion_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobCounters_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobCounters_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobCounters_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobCounters_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobProfile_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobProfile_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobProfile_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobProfile_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobStatus_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobStatus_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobStatus_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getJobStatus_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getProtocolVersion_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getProtocolVersion_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getNewJobId_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getNewJobId_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getNewJobId_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getNewJobId_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getQueueAdmins_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getQueueAdmins_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getQueueAdmins_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getQueueAdmins_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getStagingAreaDir_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getStagingAreaDir_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getStagingAreaDir_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getStagingAreaDir_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getSystemDir_avg_tim',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getSystemDir_avg_tim'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getSystemDir_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getSystemDir_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getTaskCompletionEvents_avg_tim',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getTaskCompletionEvents_avg_tim'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getTaskCompletionEvents_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getTaskCompletionEvents_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.heartbeat_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.heartbeat_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.heartbeat_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.heartbeat_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.submitJob_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.submitJob_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.submitJob_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.submitJob_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getProtocolVersion_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=rpcdetailed.rpcdetailed.getProtocolVersion_num_ops'
        )
     ),
     'UGI' => array
     (
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=ugi.ugi.loginFailure_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=ugi.ugi.loginFailure_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=ugi.ugi.loginFailure_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=ugi.ugi.loginFailure_num_ops'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=ugi.ugi.loginSuccess_avg_time',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=ugi.ugi.loginSuccess_avg_time'
        ),
        array
        (
           'url' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%JobTrackerClusterName%&m=ugi.ugi.loginSuccess_num_ops',
           'title' => '',
           'description' => '',
           'link' =>
           'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%JobTrackerClusterName%&m=ugi.ugi.loginSuccess_num_ops'
        )
     )
  );

echo json_encode($data);

?>
