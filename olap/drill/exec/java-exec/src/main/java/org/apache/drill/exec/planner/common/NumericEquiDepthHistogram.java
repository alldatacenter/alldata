/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.planner.common;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexLiteral;
import com.tdunning.math.stats.MergingDigest;
import org.apache.calcite.sql.SqlOperator;
import org.apache.drill.metastore.statistics.Histogram;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.BoundType;
import org.apache.drill.shaded.guava.com.google.common.collect.Range;

/**
 * A column specific equi-depth histogram which is meant for numeric data types
 */
@JsonTypeName("numeric-equi-depth")
public class NumericEquiDepthHistogram implements Histogram {

  /**
   * Use a small non-zero selectivity rather than 0 to account for the fact that
   * histogram boundaries are approximate and even if some values lie outside the
   * range, we cannot be absolutely sure
   */
  private static final double SMALL_SELECTIVITY = 0.0001;

  /** For equi-depth, all buckets will have same (approx) number of rows */
  @JsonProperty("numRowsPerBucket")
  private double numRowsPerBucket;

  /** An array of buckets arranged in increasing order of their start boundaries
   *  Note that the buckets only maintain the start point of the bucket range.
   * End point is assumed to be the same as the start point of next bucket, although
   * when evaluating the filter selectivity we should treat the interval as [start, end)
   * i.e closed on the start and open on the end
   */
  @JsonProperty("buckets")
  private Double[] buckets;

  // Default constructor for deserializer
  public NumericEquiDepthHistogram() {}

  public NumericEquiDepthHistogram(int numBuckets) {
    // If numBuckets = N, we are keeping N + 1 entries since the (N+1)th bucket's
    // starting value is the MAX value for the column and it becomes the end point of the
    // Nth bucket.
    buckets = new Double[numBuckets + 1];
    for (int i = 0; i < buckets.length; i++) {
      buckets[i] = new Double(0.0);
    }
    numRowsPerBucket = -1;
  }

  public double getNumRowsPerBucket() {
    return numRowsPerBucket;
  }

  public void setNumRowsPerBucket(double numRows) {
    this.numRowsPerBucket = numRows;
  }

  public Double[] getBuckets() {
    return buckets;
  }

  @VisibleForTesting
  protected void setBucketValue(int index, Double value) {
    buckets[index] = value;
  }

  /**
   * Get the number of buckets in the histogram
   * number of buckets is 1 less than the total # entries in the buckets array since last
   * entry is the end point of the last bucket
   */
  @JsonIgnore
  public int getNumBuckets() {
    return buckets.length - 1;
  }

  /**
   * Estimate the selectivity of a filter which may contain several range predicates and in the general case is of
   * type: col op value1 AND col op value2 AND col op value3 ...
   *  <p>
   *    e.g a > 10 AND a < 50 AND a >= 20 AND a <= 70 ...
   *  </p>
   * Even though in most cases it will have either 1 or 2 range conditions, we still have to handle the general case
   * For each conjunct, we will find the histogram bucket ranges and intersect them, taking into account that the
   * first and last bucket may be partially covered and all other buckets in the middle are fully covered.
   */
  @Override
  public Double estimatedSelectivity(final RexNode columnFilter, final long totalRowCount, final long ndv) {
    if (numRowsPerBucket == 0) {
      return null;
    }

    // at a minimum, the histogram should have a start and end point of 1 bucket, so at least 2 entries
    Preconditions.checkArgument(buckets.length >= 2,  "Histogram has invalid number of entries");

    List<RexNode> filterList = RelOptUtil.conjunctions(columnFilter);

    Range<Double> fullRange = Range.all();
    List<RexNode> unknownFilterList = new ArrayList<RexNode>();

    Range<Double> valuesRange = getValuesRange(filterList, fullRange, unknownFilterList);

    long numSelectedRows;
    // unknown counter is a count of filter predicates whose bucket ranges cannot be
    // determined from the histogram; this may happen for instance when there is an expression or
    // function involved..e.g  col > CAST('10' as INT)
    int unknown = unknownFilterList.size();

    if (valuesRange.hasLowerBound() || valuesRange.hasUpperBound()) {
      numSelectedRows = getSelectedRows(valuesRange, ndv);
    } else {
      numSelectedRows = 0;
    }

    if (numSelectedRows <= 0) {
      return SMALL_SELECTIVITY;
    } else {
      // for each 'unknown' range filter selectivity, use a default of 0.5 (matches Calcite)
      double scaleFactor = Math.pow(0.5, unknown);
      return  ((double) numSelectedRows / totalRowCount) * scaleFactor;
    }
  }

  private Range<Double> getValuesRange(List<RexNode> filterList, Range<Double> fullRange, List<RexNode> unkownFilterList) {
    Range<Double> currentRange = fullRange;
    for (RexNode filter : filterList) {
      if (filter instanceof RexCall) {
        Double value = getLiteralValue(filter);
        if (value == null) {
          unkownFilterList.add(filter);
        } else {
          // get the operator
          SqlOperator op = ((RexCall) filter).getOperator();
          Range range;
          switch (op.getKind()) {
            case GREATER_THAN:
              range = Range.greaterThan(value);
              currentRange = currentRange.intersection(range);
              break;
            case GREATER_THAN_OR_EQUAL:
              range = Range.atLeast(value);
              currentRange = currentRange.intersection(range);
              break;
            case LESS_THAN:
              range = Range.lessThan(value);
              currentRange = currentRange.intersection(range);
              break;
            case LESS_THAN_OR_EQUAL:
              range = Range.atMost(value);
              currentRange = currentRange.intersection(range);
              break;
            default:
              break;
          }
        }
      }
    }
    return currentRange;
  }

  @VisibleForTesting
  protected long getSelectedRows(final Range range, final long ndv) {
    double startBucketFraction = 1.0;
    double endBucketFraction = 1.0;
    long numRows = 0;
    int result;
    Double lowValue = null;
    Double highValue = null;
    final int firstStartPointIndex = 0;
    final int lastEndPointIndex = buckets.length - 1;
    int startBucket = firstStartPointIndex;
    int endBucket = lastEndPointIndex - 1;

    if (range.hasLowerBound()) {
      lowValue = (Double) range.lowerEndpoint();

      // if low value is greater than the end point of the last bucket or if it is equal but the range is open (i.e
      // predicate is of type > 5 where 5 is the end point of last bucket) then none of the rows qualify
      result = lowValue.compareTo(buckets[lastEndPointIndex]);
      if (result > 0 || result == 0 && range.lowerBoundType() == BoundType.OPEN)  {
        return 0;
      }
      result = lowValue.compareTo(buckets[firstStartPointIndex]);

      // if low value is less than or equal to the first bucket's start point then start with the first bucket and all
      // rows in first bucket are included
      if (result <= 0) {
        startBucket = firstStartPointIndex;
        startBucketFraction = 1.0;
      } else {
        startBucket = getContainingBucket(lowValue, lastEndPointIndex, true);

        // expecting start bucket to be >= 0 since other conditions have been handled previously
        Preconditions.checkArgument(startBucket >= 0, "Expected start bucket id >= 0");

       if (buckets[startBucket + 1].doubleValue() == buckets[startBucket].doubleValue()) {
         // if start and end points of the bucket are the same, consider entire bucket
         startBucketFraction = 1.0;
       } else if (range.lowerBoundType() == BoundType.CLOSED && buckets[startBucket + 1].doubleValue() == lowValue.doubleValue()) {
         // predicate is of type >= 5.0 and 5.0 happens to be the start point of the bucket
         // In this case, use the overall NDV to approximate
         startBucketFraction = 1.0 / ndv;
       } else {
          startBucketFraction = ((double) (buckets[startBucket + 1] - lowValue)) / (buckets[startBucket + 1] - buckets[startBucket]);
        }
      }
    }

    if (range.hasUpperBound()) {
      highValue = (Double) range.upperEndpoint();

      // if the high value is less than the start point of the first bucket or if it is equal but the range is open (i.e
      // predicate is of type < 1 where 1 is the start point of the first bucket) then none of the rows qualify
      result = highValue.compareTo(buckets[firstStartPointIndex]);
      if (result < 0 || (result == 0 && range.upperBoundType() == BoundType.OPEN)) {
        return 0;
      }

      result = highValue.compareTo(buckets[lastEndPointIndex]);

      // if high value is greater than or equal to the last bucket's end point then include the last bucket and all rows in
      // last bucket qualify
      if (result >= 0) {
        endBucket = lastEndPointIndex - 1;
        endBucketFraction = 1.0;
      } else {
        endBucket = getContainingBucket(highValue, lastEndPointIndex, false);

        // expecting end bucket to be >= 0 since other conditions have been handled previously
        Preconditions.checkArgument(endBucket >= 0, "Expected end bucket id >= 0");

        if (buckets[endBucket + 1].doubleValue() == buckets[endBucket].doubleValue()) {
          // if start and end points of the bucket are the same, consider entire bucket
          endBucketFraction = 1.0;
        } else if (range.upperBoundType() == BoundType.CLOSED && buckets[endBucket].doubleValue() == highValue.doubleValue()) {
          // predicate is of type <= 5.0 and 5.0 happens to be the start point of the bucket
          // In this case, use the overall NDV to approximate
          endBucketFraction = 1.0/ndv;
        } else {
          endBucketFraction = ((double) (highValue - buckets[endBucket])) / (buckets[endBucket + 1] - buckets[endBucket]);
        }
      }
    }

    Preconditions.checkArgument(startBucket >= 0 && startBucket + 1 <= lastEndPointIndex, "Invalid startBucket: " + startBucket);
    Preconditions.checkArgument(endBucket >= 0 && endBucket + 1 <= lastEndPointIndex, "Invalid endBucket: " +  endBucket);
    Preconditions.checkArgument(startBucket <= endBucket,
      "Start bucket: " + startBucket + " should be less than or equal to end bucket: " + endBucket);

    if (startBucket == endBucket) {
      // if the start and end buckets are the same, interpolate based on the difference between the high and low value
      if (highValue != null && lowValue != null) {
        numRows = (long) ((highValue - lowValue) / (buckets[startBucket + 1] - buckets[startBucket]) * numRowsPerBucket);
      } else if (highValue != null) {
        numRows = (long) (endBucketFraction * numRowsPerBucket);
      } else {
        numRows = (long) (startBucketFraction * numRowsPerBucket);
      }
    } else {
      int numIntermediateBuckets = (endBucket > startBucket + 1) ? (endBucket - startBucket - 1) : 0;
      numRows = (long) ((startBucketFraction + endBucketFraction) * numRowsPerBucket + numIntermediateBuckets * numRowsPerBucket);
    }

    return numRows;
  }

  /**
   * Get the start point of the containing bucket for the supplied value. If there are multiple buckets with the
   * same start point, return either the first matching or last matching depending on firstMatching flag
   * @param value the input double value
   * @param lastEndPointIndex
   * @param firstMatching If true, return the first bucket that matches the specified criteria otherwise return the last one
   * @return index of either the first or last matching bucket if a match was found, otherwise return -1
   */
  private int getContainingBucket(final Double value, final int lastEndPointIndex, final boolean firstMatching) {
    int i = 0;
    int containing_bucket = -1;

    // check which bucket this value falls in
    for (; i <= lastEndPointIndex; i++) {
      int result = buckets[i].compareTo(value);
      if (result > 0) {
        containing_bucket = i - 1;
        break;
      } else if (result == 0) {
        // if we are already at the lastEndPointIndex, mark the containing bucket
        // as i-1 because the containing bucket should correspond to the start point of the bucket
        // (recall that lastEndPointIndex is the final end point of the last bucket)
        containing_bucket = (i == lastEndPointIndex) ? i - 1 : i;
        if (firstMatching) {
          // break if we are only interested in the first matching bucket
          break;
        }
      }
    }
    return containing_bucket;
   }

  private Double getLiteralValue(final RexNode filter) {
    Double value = null;
    List<RexNode> operands = ((RexCall) filter).getOperands();
    if (operands.size() == 2 && operands.get(1) instanceof RexLiteral) {
      RexLiteral l = ((RexLiteral) operands.get(1));

      switch (l.getTypeName()) {
        case DATE:
        case TIMESTAMP:
        case TIME:
          value = (double) ((java.util.Calendar) l.getValue()).getTimeInMillis();
          break;
        case INTEGER:
        case BIGINT:
        case FLOAT:
        case DOUBLE:
        case DECIMAL:
        case BOOLEAN:
          value = l.getValueAs(Double.class);
          break;
        default:
          break;
      }
    }
    return value;
  }

  /**
   * Build a Numeric Equi-Depth Histogram from a t-digest byte array
   * @param tdigest_array
   * @param numBuckets
   * @param nonNullCount
   * @return An instance of NumericEquiDepthHistogram
   */
  public static NumericEquiDepthHistogram buildFromTDigest(final byte[] tdigest_array,
                                                           final int numBuckets,
                                                           final long nonNullCount) {
    MergingDigest tdigest = MergingDigest.fromBytes(java.nio.ByteBuffer.wrap(tdigest_array));

    NumericEquiDepthHistogram histogram = new NumericEquiDepthHistogram(numBuckets);

    final double q = 1.0/numBuckets;
    int i = 0;
    for (; i < numBuckets; i++) {
      // get the starting point of the i-th quantile
      double start = tdigest.quantile(q * i);
      histogram.buckets[i] = start;
    }
    // for the N-th bucket, the end point corresponds to the 1.0 quantile but we don't keep the end
    // points; only the start point, so this is stored as the start point of the (N+1)th bucket
    histogram.buckets[i] = tdigest.quantile(1.0);

    // Each bucket stores approx equal number of rows.  Here, we take into consideration the nonNullCount
    // supplied since the stats may have been collected with sampling.  Sampling of 20% means only 20% of the
    // tuples will be stored in the t-digest.  However, the overall stats such as totalRowCount, nonNullCount and
    // NDV would have already been extrapolated up from the sample. So, we take the max of the t-digest size and
    // the supplied nonNullCount. Why non-null ? Because only non-null values are stored in the t-digest.
    double numRowsPerBucket = (double)(Math.max(tdigest.size(), nonNullCount))/numBuckets;
    histogram.setNumRowsPerBucket(numRowsPerBucket);

    return histogram;
  }

}
