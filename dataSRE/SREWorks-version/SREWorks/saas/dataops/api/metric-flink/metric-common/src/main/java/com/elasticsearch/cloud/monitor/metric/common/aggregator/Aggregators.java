package com.elasticsearch.cloud.monitor.metric.common.aggregator;

import com.google.common.base.Preconditions;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.util.ResizableDoubleArray;

public class Aggregators {

    public static Aggregator get(final String name) {
        switch (name) {
            case "avg":
                return new Avg();
            case "sum":
                return new Sum();
            case "max":
                return new Max();
            case "min":
                return new Min();
            case "count":
                return new Count();
            case "p99":
                return new PercentileAgg(99d);
            case "p95":
                return new PercentileAgg(95d);
            case "p90":
                return new PercentileAgg(90d);
            case "p75":
                return new PercentileAgg(75d);
            case "p50":
                return new PercentileAgg(50d);
            default:
                throw new IllegalArgumentException("No such aggregator: " + name);
        }
    }

    public static class Avg implements Aggregator {
        private double sum = 0;
        private int count = 0;

        @Override
        public void addValue(double value) {
            sum += value;
            count++;
        }

        @Override
        public double getValue() {
            Preconditions.checkState(count > 0, "count <= 0");
            return sum / count;
        }
    }


    public static class Sum implements Aggregator {
        private double sum = 0;

        @Override
        public void addValue(double value) {
            sum += value;
        }

        @Override
        public double getValue() {
            return sum;
        }
    }


    public static class Max implements Aggregator {
        private double max = Double.NEGATIVE_INFINITY;

        @Override
        public void addValue(double value) {
            max = Math.max(max, value);
        }

        @Override
        public double getValue() {
            return max;
        }
    }


    public static class Min implements Aggregator {
        private double min = Double.MAX_VALUE;

        @Override
        public void addValue(double value) {
            min = Math.min(min, value);
        }

        @Override
        public double getValue() {
            return min;
        }
    }


    public static class Count implements Aggregator {
        private int count = 0;

        @Override
        public void addValue(double value) {
            count++;
        }

        @Override
        public double getValue() {
            return count;
        }
    }


    /**
     * Percentile aggregator based on apache commons math3 implementation <br>The default
     * calculation is: index=(N+1)p estimate=x⌈h−1/2⌉ minLimit=0 maxLimit=1
     */
    static final class PercentileAgg implements Aggregator {
        private final Percentile percentile;
        private final ResizableDoubleArray doubleValues;
        private int n = 0;

        public PercentileAgg(final double percentile) {
            Preconditions.checkArgument(percentile > 0 && percentile <= 100, "Invalid percentile value");
            this.percentile = new Percentile(percentile);
            this.doubleValues = new ResizableDoubleArray();
        }

        @Override
        public void addValue(double value) {
            if (!Double.isNaN(value)) {
                doubleValues.addElement(value);
                n++;
            }
        }

        @Override
        public double getValue() {
            Preconditions.checkState(n > 0, "n <= 0");
            percentile.setData(doubleValues.getElements());
            return percentile.evaluate();
        }
    }
}
