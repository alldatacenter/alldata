package com.aliyun.oss.model;

import com.aliyun.oss.event.ProgressListener;

import static com.aliyun.oss.internal.RequestParameters.*;

public class CreateSelectObjectMetadataRequest extends HeadObjectRequest {
    private String process;
    private InputSerialization inputSerialization = new InputSerialization();
    private boolean overwrite;
    private ProgressListener selectProgressListener;

    public CreateSelectObjectMetadataRequest(String bucketName, String key) {
        super(bucketName, key);
        setProcess(SUBRESOURCE_CSV_META);
        setOverwrite(false);
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public CreateSelectObjectMetadataRequest withProcess(String process) {
        setProcess(process);
        return this;
    }

    public InputSerialization getInputSerialization() {
        return inputSerialization;
    }

    public void setInputSerialization(InputSerialization inputSerialization) {
        if (inputSerialization.getSelectContentFormat() == SelectContentFormat.CSV) {
            setProcess(SUBRESOURCE_CSV_META);
        } else {
            setProcess(SUBRESOURCE_JSON_META);
        }
        this.inputSerialization = inputSerialization;
    }

    public CreateSelectObjectMetadataRequest withInputSerialization(InputSerialization inputSerialization) {
        setInputSerialization(inputSerialization);
        return this;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public CreateSelectObjectMetadataRequest withOverwrite(boolean overwrite) {
        setOverwrite(overwrite);
        return this;
    }

    public ProgressListener getSelectProgressListener() {
        return selectProgressListener;
    }

    public void setSelectProgressListener(ProgressListener selectProgressListener) {
        this.selectProgressListener = selectProgressListener;
    }

    public CreateSelectObjectMetadataRequest withSelectProgressListener(ProgressListener selectProgressListener) {
        setSelectProgressListener(selectProgressListener);
        return this;
    }
}
