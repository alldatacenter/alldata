package com.netease.arctic.spark.distributions;

public class ClusterDistributionImpl implements ClusteredDistribution {

  private Expression[] clusterExprs;

  public ClusterDistributionImpl(Expression[] clusterExprs) {
    this.clusterExprs = clusterExprs;
  }

  @Override
  public Expression[] clustering() {
    return clusterExprs;
  }
}
