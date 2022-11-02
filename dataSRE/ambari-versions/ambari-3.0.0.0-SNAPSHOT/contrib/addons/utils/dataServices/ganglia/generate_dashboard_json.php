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
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%GridSlavesClusterName%&g=load_report',
           'title' => 'Load Report',
           'description' => 'Key load metrics, aggregated across the slaves in the grid',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%GridSlavesClusterName%'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%GridSlavesClusterName%&g=mem_report',
           'title' => 'Memory Report',
           'description' => 'Key memory metrics, aggregated across the slaves in the grid',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%GridSlavesClusterName%'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%GridSlavesClusterName%&g=cpu_report',
           'title' => 'CPU Report',
           'description' => 'Key CPU metrics, aggregated across the slaves in the grid',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%GridSlavesClusterName%'
        ),
        array
        (
           'url' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/graph.php?c=%GridSlavesClusterName%&g=network_report',
           'title' => 'Network I/O Report',
           'description' => 'Key network I/O metrics, aggregated across the slaves in the grid',
           'link' => 'http://%GangliaWebHostName%:%GangliaWebPort%/ganglia/?c=%GridSlavesClusterName%'
        )
     )
  );

echo json_encode($data);

?>
