package com.qcloud.cos.model;


public class BucketIntelligentTierConfiguration {
    /**
     * bucket intelligent tier status indicating that intelligent tier is suspended for a
     * bucket. Use the "Suspended" status when you want to disable intelligent tier on
     * a bucket that has intelligent tier enabled.
     */
    public static final String SUSPENDED = "Suspended";

    /**
     * bucket intelligent tier status indicating that bucket intelligent is enabled for a
     * bucket.
     */
    public static final String ENABLED = "Enabled";

    public static class Transition {
        private int days = -1;
        private int requestFrequent = 1;
        public Transition() {}

        public Transition(int days) {
            this.days = days;
        }
        public int getDays() {
            return days;
        }

        public void setDays(int days) {
            this.days = days;
        }

        public int getRequestFrequent() {
            return requestFrequent;
        }

        private void setRequestFrequent(int requestFrequent) {
            this.requestFrequent = requestFrequent;
        }
    }

    private String status;

    private Transition transition ;

    /**
     * Creates a new bucket intelligent tier configuration,
     * Passing this new object directly to
     * {@link com.qcloud.cos.COSClient#setBucketIntelligentTieringConfiguration(SetBucketIntelligentTierConfigurationRequest)}}
     * </p>
     */
    public BucketIntelligentTierConfiguration() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Transition getTransition() {
        return transition;
    }

    public void setTransition(Transition transition) {
        this.transition = transition;
    }

}
