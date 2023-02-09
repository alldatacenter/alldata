package com.aliyun.oss.model;

import com.aliyun.oss.common.utils.RangeSpec;
import com.aliyun.oss.event.ProgressListener;

import java.security.InvalidParameterException;

import static com.aliyun.oss.internal.RequestParameters.SUBRESOURCE_CSV_SELECT;
import static com.aliyun.oss.internal.RequestParameters.SUBRESOURCE_JSON_SELECT;

/**
 * This is the request class that is used to select an object from OSS. It
 * wraps all the information needed to select this object.
 * User can pass sql expression to filter csv objects.
 */
public class SelectObjectRequest extends GetObjectRequest {
    private static final String LINE_RANGE_PREFIX = "line-range=";
    private static final String SPLIT_RANGE_PREFIX = "split-range=";

    public enum ExpressionType {
        SQL,
    }

    private String expression;
    private boolean skipPartialDataRecord = false;
    private long maxSkippedRecordsAllowed = 0;
    private ExpressionType expressionType = ExpressionType.SQL;
    private InputSerialization inputSerialization = new InputSerialization();
    private OutputSerialization outputSerialization = new OutputSerialization();
    /**
     *  scanned bytes progress listener for select request,
     *  it is different from progressListener from {@link WebServiceRequest} which used for request and response bytes
     */
    private ProgressListener selectProgressListener;

    //lineRange is not a generic requirement, we will move it to somewhere else later.
    private long[] lineRange;

    //splitRange is a range of splits, one split is a collection of continuous lines
    private long[] splitRange;

    public SelectObjectRequest(String bucketName, String key) {
        super(bucketName, key);
        setProcess(SUBRESOURCE_CSV_SELECT);
    }

    public long[] getLineRange() {
        return lineRange;
    }

    /**
     * For text file, we can define line range for select operations.
     * Select will only scan data between startLine and endLine, that is [startLine, endLine]
     *
     * @param startLine
     *            <p>
     *            Start line number
     *            </p>
     *            <p>
     *            When the start is non-negative, it means the starting line
     *            to select. When the start is -1, it means the range is
     *            determined by the end only and the end could not be -1. For
     *            example, when start is -1 and end is 100. It means the
     *            select line range will be the last 100 lines.
     *            </p>
     * @param endLine
     *            <p>
     *            End line number
     *            </p>
     *            <p>
     *            When the end is non-negative, it means the ending line to
     *            select. When the end is -1, it means the range is determined
     *            by the start only and the start could not be -1. For example,
     *            when end is -1 and start is 100. It means the select range
     *            will be all exception first 100 lines.
     *            </p>
     */
    public void setLineRange(long startLine, long endLine) {
        lineRange = new long[] {startLine, endLine};
    }

    public SelectObjectRequest withLineRange(long startLine, long endLine) {
        setLineRange(startLine, endLine);
        return this;
    }

    public long[] getSplitRange() {
        return splitRange;
    }

    /**
     * For text file, we can define split range for select operations.
     * Select will only scan data between startSplit and endSplit, that is [startSplit, endSplit]
     *
     * @param startSplit
     *            <p>
     *            Start split number
     *            </p>
     *            <p>
     *            When the start is non-negative, it means the starting split
     *            to select. When the start is -1, it means the range is
     *            determined by the end only and the end could not be -1. For
     *            example, when start is -1 and end is 100. It means the
     *            select split range will be the last 100 splits.
     *            </p>
     *
     * @param endSplit
     *            <p>
     *            End split number
     *            </p>
     *            <p>
     *            When the end is non-negative, it means the ending split to
     *            select. When the end is -1, it means the range is determined
     *            by the start only and the start could not be -1. For example,
     *            when end is -1 and start is 100. It means the select range
     *            will be all exception first 100 splits.
     *            </p>
     */
    public void setSplitRange(long startSplit, long endSplit) {
        splitRange = new long[] {startSplit, endSplit};
    }

    public SelectObjectRequest withSplitRange(long startSplit, long endSplit) {
        setSplitRange(startSplit, endSplit);
        return this;
    }

    public String lineRangeToString(long[] range) {
        return rangeToString(LINE_RANGE_PREFIX, range);
    }

    public String splitRangeToString(long[] range) {
        return rangeToString(SPLIT_RANGE_PREFIX, range);
    }

    public String rangeToString(String rangePrefix, long[] range) {
        RangeSpec rangeSpec = RangeSpec.parse(range);
        switch (rangeSpec.getType()) {
            case NORMAL_RANGE:
                return String.format("%s%d-%d", rangePrefix, rangeSpec.getStart(), rangeSpec.getEnd());
            case START_TO:
                return String.format("%s%d-", rangePrefix, rangeSpec.getStart());
            case TO_END:
                return String.format("%s-%d", rangePrefix,  rangeSpec.getEnd());
        }

        return null;
    }

    /**
     * Get the expression which used to filter objects
     * @return The Base64-encoded SQL statement.
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Set the expression which used to filter objects
     *
     * @param expression
     *            The Base64-encoded SQL statement.
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    public SelectObjectRequest withExpression(String expression) {
        setExpression(expression);
        return this;
    }

    public boolean isSkipPartialDataRecord() {
        return skipPartialDataRecord;
    }

    public void setSkipPartialDataRecord(boolean skipPartialDataRecord) {
        this.skipPartialDataRecord = skipPartialDataRecord;
    }

    public SelectObjectRequest withSkipPartialDataRecord(boolean skipPartialDataRecord) {
        setSkipPartialDataRecord(skipPartialDataRecord);
        return this;
    }

    public long getMaxSkippedRecordsAllowed() {
        return maxSkippedRecordsAllowed;
    }

    public void setMaxSkippedRecordsAllowed(long maxSkippedRecordsAllowed) {
        this.maxSkippedRecordsAllowed = maxSkippedRecordsAllowed;
    }

    public SelectObjectRequest withMaxSkippedRecordsAllowed(long maxSkippedRecordsAllowed) {
        setMaxSkippedRecordsAllowed(maxSkippedRecordsAllowed);
        return this;
    }

    /**
     * Get the expression type, we only support SQL now.
     * @return The {@link ExpressionType}
     */
    public ExpressionType getExpressionType() {
        return expressionType;
    }

    /**
     * Get the input serialization, we use this to parse data
     * @return The {@link InputSerialization}
     */
    public InputSerialization getInputSerialization() {
        return inputSerialization;
    }

    /**
     * Set the input serialization, we use this to parse data
     *
     * @param inputSerialization
     *            The {@link InputSerialization} instance.
     */
    public void setInputSerialization(InputSerialization inputSerialization) {
        if (inputSerialization.getSelectContentFormat() == SelectContentFormat.CSV) {
            setProcess(SUBRESOURCE_CSV_SELECT);
        } else {
            setProcess(SUBRESOURCE_JSON_SELECT);
        }
        this.inputSerialization = inputSerialization;
    }

    public SelectObjectRequest withInputSerialization(InputSerialization inputSerialization) {
        setInputSerialization(inputSerialization);
        return this;
    }

    /**
     * Get the output serialization, it defines the output format
     * @return The {@link OutputSerialization} instance.
     */
    public OutputSerialization getOutputSerialization() {
        return outputSerialization;
    }

    /**
     * Set the output serialization, it defines the output format
     *
     * @param outputSerialization
     *            The {@link OutputSerialization} instance.
     */
    public void setOutputSerialization(OutputSerialization outputSerialization) {
        this.outputSerialization = outputSerialization;
    }

    public SelectObjectRequest withOutputSerialization(OutputSerialization outputSerialization) {
        setOutputSerialization(outputSerialization);
        return this;
    }

    public ProgressListener getSelectProgressListener() {
        return selectProgressListener;
    }

    public void setSelectProgressListener(ProgressListener selectProgressListener) {
        this.selectProgressListener = selectProgressListener;
    }

    public SelectObjectRequest withSelectProgressListener(ProgressListener selectProgressListener) {
        setSelectProgressListener(selectProgressListener);
        return this;
    }
}
