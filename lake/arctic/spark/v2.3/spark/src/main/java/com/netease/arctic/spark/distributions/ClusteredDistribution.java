package com.netease.arctic.spark.distributions;

import org.apache.spark.annotation.Experimental;

@Experimental
public interface ClusteredDistribution extends Distribution {
  Expression[] clustering();
}
