/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.metrics.ganglia;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 *
 * @author root
 */
public class GangliaMetricTest {
  


  /**
   * Test of setDatapoints method, of class GangliaMetric.
   */
  @Test
  public void testSetDatapointsOfPercentValue() {
    System.out.println("setDatapoints");
    List<GangliaMetric.TemporalMetric> listTemporalMetrics =
      new ArrayList<>();
    GangliaMetric instance = new GangliaMetric();
    instance.setDs_name("dsName");
    instance.setCluster_name("c1");
    instance.setHost_name("localhost");
    instance.setMetric_name("cpu_wio");
    
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("111.0", new Long(1362440880)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("11.0", new Long(1362440881)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("100.0", new Long(1362440882)));
    instance.setDatapointsFromList(listTemporalMetrics);
    assertTrue(instance.getDatapoints().length == 2);
  }

  /**
   * Test of setDatapoints method, of class GangliaMetric.
   */
  //@Test
  public void testSetDatapointsOfgcTimeMillisValue() {
    System.out.println("setDatapoints");
    List<GangliaMetric.TemporalMetric> listTemporalMetrics =
      new ArrayList<>();
    GangliaMetric instance = new GangliaMetric();
    instance.setDs_name("dsName");
    instance.setCluster_name("c1");
    instance.setHost_name("localhost");
    instance.setMetric_name("jvm.metrics.gcTimeMillis");
    
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(1)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(2)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(3)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("111.0", new Long(4)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("11.0", new Long(5)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("100.0", new Long(6)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(7)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("11.0", new Long(8)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(9)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(10)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(11)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(12)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("11.0", new Long(13)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("100.0", new Long(14)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(15)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(16)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(17)));
    listTemporalMetrics.add(new GangliaMetric.TemporalMetric("0.0", new Long(18)));
    instance.setDatapointsFromList(listTemporalMetrics);
    System.out.println(instance);
    assertTrue(instance.getDatapoints().length == 11);
  }  
  
    /**
   * Test of GangliaMetric.TemporalMetric constructor.
   */
  @Test
  public void testTemporalMetricFineValue() {
    System.out.println("GangliaMetric.TemporalMetric");
    GangliaMetric.TemporalMetric tm;
    tm = new GangliaMetric.TemporalMetric("100", new Long(1362440880));
    assertTrue("GangliaMetric.TemporalMetric is valid", tm.isValid());
  }

    /**
   * Test of GangliaMetric.TemporalMetric constructor.
   */
  @Test
  public void testTemporalMetricIsNaNValue() {
    System.out.println("GangliaMetric.TemporalMetric");
    GangliaMetric.TemporalMetric tm;
    tm = new GangliaMetric.TemporalMetric("any string", new Long(1362440880));
    assertFalse("GangliaMetric.TemporalMetric is invalid", tm.isValid());
  }
  

  
}
