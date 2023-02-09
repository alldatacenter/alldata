/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.internal;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.CASJobParameters;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.RestoreObjectRequest;
import com.qcloud.cos.model.SelectObjectContentRequest;
import com.qcloud.cos.model.RequestProgress;
import com.qcloud.cos.model.SelectParameters;
import com.qcloud.cos.model.CSVInput;
import com.qcloud.cos.model.JSONInput;
import com.qcloud.cos.model.CSVOutput;
import com.qcloud.cos.model.JSONOutput;
import com.qcloud.cos.model.InputSerialization;
import com.qcloud.cos.model.OutputSerialization;
import com.qcloud.cos.model.ScanRange;
import com.qcloud.cos.model.ciModel.auditing.AudioAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.AuditingInputObject;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingInputObject;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.Conf;
import com.qcloud.cos.model.ciModel.auditing.DocumentAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.VideoAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.WebpageAuditingRequest;
import com.qcloud.cos.model.ciModel.bucket.DocBucketRequest;
import com.qcloud.cos.model.ciModel.common.MediaOutputObject;
import com.qcloud.cos.model.ciModel.job.DocJobObject;
import com.qcloud.cos.model.ciModel.job.DocJobRequest;
import com.qcloud.cos.model.ciModel.job.DocProcessObject;
import com.qcloud.cos.model.ciModel.job.MediaAudioObject;
import com.qcloud.cos.model.ciModel.job.MediaConcatFragmentObject;
import com.qcloud.cos.model.ciModel.job.MediaConcatTemplateObject;
import com.qcloud.cos.model.ciModel.job.MediaJobOperation;
import com.qcloud.cos.model.ciModel.job.MediaJobsRequest;
import com.qcloud.cos.model.ciModel.job.MediaRemoveWaterMark;
import com.qcloud.cos.model.ciModel.job.MediaTimeIntervalObject;
import com.qcloud.cos.model.ciModel.job.MediaTransConfigObject;
import com.qcloud.cos.model.ciModel.job.MediaTranscodeObject;
import com.qcloud.cos.model.ciModel.job.MediaTranscodeVideoObject;
import com.qcloud.cos.model.ciModel.job.MediaVideoObject;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoRequest;
import com.qcloud.cos.model.ciModel.queue.DocQueueRequest;
import com.qcloud.cos.model.ciModel.queue.MediaQueueRequest;
import com.qcloud.cos.model.ciModel.snapshot.SnapshotRequest;
import com.qcloud.cos.model.ciModel.template.MediaSnapshotObject;
import com.qcloud.cos.model.ciModel.template.MediaTemplateRequest;
import com.qcloud.cos.model.ciModel.template.MediaWaterMarkImage;
import com.qcloud.cos.model.ciModel.template.MediaWaterMarkText;
import com.qcloud.cos.model.ciModel.template.MediaWatermark;
import com.qcloud.cos.model.ciModel.workflow.MediaOperation;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowDependency;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowNode;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowRequest;
import com.qcloud.cos.utils.StringUtils;


public class RequestXmlFactory {

    /**
     * Converts the specified list of PartETags to an XML fragment that can be sent to the
     * CompleteMultipartUpload operation of Qcloud COS.
     *
     * @param partETags The list of part ETags containing the data to include in the new XML
     *                  fragment.
     * @return A byte array containing the data
     */
    public static byte[] convertToXmlByteArray(List<PartETag> partETags) {
        XmlWriter xml = new XmlWriter();
        xml.start("CompleteMultipartUpload");
        if (partETags != null) {
            Collections.sort(partETags, new Comparator<PartETag>() {
                public int compare(PartETag tag1, PartETag tag2) {
                    if (tag1.getPartNumber() < tag2.getPartNumber())
                        return -1;
                    if (tag1.getPartNumber() > tag2.getPartNumber())
                        return 1;
                    return 0;
                }
            });

            for (PartETag partEtag : partETags) {
                xml.start("Part");
                xml.start("PartNumber").value(Integer.toString(partEtag.getPartNumber())).end();
                xml.start("ETag").value(partEtag.getETag()).end();
                xml.end();
            }
        }
        xml.end();

        return xml.getBytes();
    }

    /**
     * Converts the SelectObjectContentRequest to an XML fragment that can be sent to
     * the SelectObjectContent operation of COS.
     */
    public static byte[] convertToXmlByteArray(SelectObjectContentRequest selectRequest) {
        XmlWriter xml = new XmlWriter();
        xml.start("SelectObjectContentRequest");

        addIfNotNull(xml, "Expression", selectRequest.getExpression());
        addIfNotNull(xml, "ExpressionType", selectRequest.getExpressionType());

        addScanRangeIfNotNull(xml, selectRequest.getScanRange());
        addRequestProgressIfNotNull(xml, selectRequest.getRequestProgress());
        addInputSerializationIfNotNull(xml, selectRequest.getInputSerialization());
        addOutputSerializationIfNotNull(xml, selectRequest.getOutputSerialization());

        xml.end();
        return xml.getBytes();
    }


    private static void addRequestProgressIfNotNull(XmlWriter xml, RequestProgress requestProgress) {
        if (requestProgress == null) {
            return;
        }

        xml.start("RequestProgress");

        addIfNotNull(xml, "Enabled", requestProgress.getEnabled());

        xml.end();
    }

    private static void addSelectParametersIfNotNull(XmlWriter xml, SelectParameters selectParameters) {
        if (selectParameters == null) {
            return;
        }

        xml.start("SelectParameters");

        addInputSerializationIfNotNull(xml, selectParameters.getInputSerialization());

        addIfNotNull(xml, "ExpressionType", selectParameters.getExpressionType());
        addIfNotNull(xml, "Expression", selectParameters.getExpression());

        addOutputSerializationIfNotNull(xml, selectParameters.getOutputSerialization());

        xml.end();
    }

    private static void addScanRangeIfNotNull(XmlWriter xml, ScanRange scanRange) {
        if (scanRange != null) {
            xml.start("ScanRange");

            addIfNotNull(xml, "Start", scanRange.getStart());
            addIfNotNull(xml, "End", scanRange.getEnd());

            xml.end();
        }
    }

    private static void addInputSerializationIfNotNull(XmlWriter xml, InputSerialization inputSerialization) {
        if (inputSerialization != null) {
            xml.start("InputSerialization");

            if (inputSerialization.getCsv() != null) {
                xml.start("CSV");
                CSVInput csvInput = inputSerialization.getCsv();
                addIfNotNull(xml, "FileHeaderInfo", csvInput.getFileHeaderInfo());
                addIfNotNull(xml, "Comments", csvInput.getCommentsAsString());
                addIfNotNull(xml, "QuoteEscapeCharacter", csvInput.getQuoteEscapeCharacterAsString());
                addIfNotNull(xml, "RecordDelimiter", csvInput.getRecordDelimiterAsString());
                addIfNotNull(xml, "FieldDelimiter", csvInput.getFieldDelimiterAsString());
                addIfNotNull(xml, "QuoteCharacter", csvInput.getQuoteCharacterAsString());
                addIfNotNull(xml, "AllowQuotedRecordDelimiter", csvInput.getAllowQuotedRecordDelimiter());
                xml.end();
            }

            if (inputSerialization.getJson() != null) {
                xml.start("JSON");
                JSONInput jsonInput = inputSerialization.getJson();
                addIfNotNull(xml, "Type", jsonInput.getType());
                xml.end();
            }

            if (inputSerialization.getParquet() != null) {
                xml.start("Parquet");
                xml.end();
            }

            addIfNotNull(xml, "CompressionType", inputSerialization.getCompressionType());

            xml.end();
        }
    }

    private static void addOutputSerializationIfNotNull(XmlWriter xml, OutputSerialization outputSerialization) {
        if (outputSerialization != null) {
            xml.start("OutputSerialization");

            if (outputSerialization.getCsv() != null) {
                xml.start("CSV");
                CSVOutput csvOutput = outputSerialization.getCsv();
                addIfNotNull(xml, "QuoteFields", csvOutput.getQuoteFields());
                addIfNotNull(xml, "QuoteEscapeCharacter", csvOutput.getQuoteEscapeCharacterAsString());
                addIfNotNull(xml, "RecordDelimiter", csvOutput.getRecordDelimiterAsString());
                addIfNotNull(xml, "FieldDelimiter", csvOutput.getFieldDelimiterAsString());
                addIfNotNull(xml, "QuoteCharacter", csvOutput.getQuoteCharacterAsString());
                xml.end();
            }

            if (outputSerialization.getJson() != null) {
                xml.start("JSON");
                JSONOutput jsonOutput = outputSerialization.getJson();
                addIfNotNull(xml, "RecordDelimiter", jsonOutput.getRecordDelimiterAsString());
                xml.end();
            }

            xml.end();
        }
    }

    /**
     * Converts the RestoreObjectRequest to an XML fragment that can be sent to the RestoreObject
     * operation of COS.
     *
     * @param restoreObjectRequest The container which provides options for restoring an object,
     *                             which was transitioned to the CAS from COS when it was expired, into COS again.
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(RestoreObjectRequest restoreObjectRequest)
            throws CosClientException {
        XmlWriter xml = new XmlWriter();

        xml.start("RestoreRequest");
        xml.start("Days").value(Integer.toString(restoreObjectRequest.getExpirationInDays())).end();
        final CASJobParameters casJobParameters = restoreObjectRequest.getCasJobParameters();
        if (casJobParameters != null) {
            xml.start("CASJobParameters");
            addIfNotNull(xml, "Tier", casJobParameters.getTier());
            xml.end();
        }
        xml.end();

        return xml.getBytes();
    }

    /**
     * Converts the MediaWorkflowRequest to an XML fragment that can be sent to the RestoreObject
     * operation of COS.
     *
     * @param request The container which provides options for restoring an object,
     *                which was transitioned to the CAS from COS when it was expired, into COS again.
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(MediaWorkflowRequest request)
            throws CosClientException, UnsupportedEncodingException {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("MediaWorkflow");
        xml.start("Name").value(request.getName()).end();
        addIfNotNull(xml, "State", request.getState());
        addIfNotNull(xml, "WorkflowId", request.getWorkflowId());
        xml.start("Topology");
        xml.start("Dependencies");
        Map<String, MediaWorkflowDependency> mediaWorkflowDependency = request.getTopology().getMediaWorkflowDependency();
        for (String key : mediaWorkflowDependency.keySet()) {
            xml.start(key).value(mediaWorkflowDependency.get(key).getValue()).end();
        }
        xml.end();
        xml.start("Nodes");
        Map<String, MediaWorkflowNode> mediaWorkflowNodes = request.getTopology().getMediaWorkflowNodes();
        for (String key : mediaWorkflowNodes.keySet()) {
            xml.start(key);
            MediaWorkflowNode mediaWorkflowNode = mediaWorkflowNodes.get(key);
            xml.start("Type").value(mediaWorkflowNode.getType()).end();
            if (mediaWorkflowNode.getInput().getObjectPrefix() != null || mediaWorkflowNode.getInput().getQueueId() != null) {
                xml.start("Input");
                addIfNotNull(xml, "QueueId", mediaWorkflowNode.getInput().getQueueId());
                addIfNotNull(xml, "ObjectPrefix", mediaWorkflowNode.getInput().getObjectPrefix());
                xml.end();
            }
            MediaOperation operation = mediaWorkflowNode.getOperation();
            MediaOutputObject output = operation.getOutput();
            if (operation.getTemplateId() != null || output.getBucket() != null
                    || output.getObject() != null || output.getRegion() != null) {
                xml.start("Operation");
                addIfNotNull(xml, "TemplateId", operation.getTemplateId());
                xml.start("Output");
                addIfNotNull(xml, "Region", output.getRegion());
                addIfNotNull(xml, "Bucket", output.getBucket());
                addIfNotNull(xml, "Object", output.getObject());
                xml.end();
                xml.end();
            }
            xml.end();
        }
        xml.end();
        xml.end();
        xml.end();
        xml.end();
        return xml.getBytes();
    }

    /**
     * Converts the MediaJobsRequest to an XML fragment that can be sent to the JobObject of COS
     *
     * @param request The container which provides options for restoring an object
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(MediaJobsRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("Tag").value(request.getTag()).end();
        xml.start("BucketName").value(request.getBucketName()).end();
        xml.start("Input");
        xml.start("Object").value(request.getInput().getObject()).end();
        xml.end();

        MediaJobOperation operation = request.getOperation();
        xml.start("Operation");
        addIfNotNull(xml,"TemplateId",operation.getTemplateId());

        List<String> watermarkTemplateId = operation.getWatermarkTemplateId();
        if (watermarkTemplateId.size() != 0) {
            for (String templateId : watermarkTemplateId) {
                xml.start("WatermarkTemplateId").value(templateId).end();
            }
        }

        MediaWatermark watermark = operation.getWatermark();
        if (CheckObjectUtils.objIsNotValid(watermark)) {
            addIfNotNull(xml, "Type", watermark.getType());
            addIfNotNull(xml, "Dx", watermark.getDx());
            addIfNotNull(xml, "Dy", watermark.getDy());
            addIfNotNull(xml, "EndTime", watermark.getEndTime());
            addIfNotNull(xml, "LocMode", watermark.getLocMode());
            addIfNotNull(xml, "Pos", watermark.getPos());
            addIfNotNull(xml, "StartTime", watermark.getStartTime());

            if ("Text".equalsIgnoreCase(watermark.getType())) {
                MediaWaterMarkText text = watermark.getText();
                xml.start("Text");
                addIfNotNull(xml, "FontColor", text.getFontColor());
                addIfNotNull(xml, "FontSize", text.getFontSize());
                addIfNotNull(xml, "FontType", text.getFontType());
                addIfNotNull(xml, "Text", text.getText());
                addIfNotNull(xml, "Transparency", text.getTransparency());
                xml.end();
            } else if ("Image".equalsIgnoreCase(watermark.getType())) {
                MediaWaterMarkImage image = watermark.getImage();
                xml.start("Image");
                addIfNotNull(xml, "Height", image.getHeight());
                addIfNotNull(xml, "Mode", image.getMode());
                addIfNotNull(xml, "Transparency", image.getTransparency());
                addIfNotNull(xml, "Url", image.getUrl());
                addIfNotNull(xml, "Width", image.getWidth());
                xml.end();
            }
        }
        MediaRemoveWaterMark removeWatermark = operation.getRemoveWatermark();
        if (CheckObjectUtils.objIsNotValid(removeWatermark)) {
            xml.start("RemoveWatermark");
            addIfNotNull(xml, "Height", removeWatermark.getHeight());
            addIfNotNull(xml, "Dx", removeWatermark.getDx());
            addIfNotNull(xml, "Dy", removeWatermark.getDy());
            addIfNotNull(xml, "Switch", removeWatermark.get_switch());
            addIfNotNull(xml, "Width", removeWatermark.getWidth());
            xml.end();
        }

        MediaConcatTemplateObject mediaConcatTemplate = operation.getMediaConcatTemplate();
        if (CheckObjectUtils.objIsNotValid(mediaConcatTemplate)){
            xml.start("ConcatTemplate");
            List<MediaConcatFragmentObject> concatFragmentList = mediaConcatTemplate.getConcatFragmentList();
            for (MediaConcatFragmentObject concatFragment : concatFragmentList) {
                xml.start("ConcatFragment");
                addIfNotNull(xml,"Mode",concatFragment.getMode());
                addIfNotNull(xml,"Url",concatFragment.getUrl());
                xml.end();
            }
            addVideo(xml,mediaConcatTemplate.getVideo());
            addAudio(xml,mediaConcatTemplate.getAudio());
            addIfNotNull(xml,"Index",mediaConcatTemplate.getIndex());
            String format = mediaConcatTemplate.getContainer().getFormat();
            if (!StringUtils.isNullOrEmpty(format)){
                xml.start("Container");
                xml.start("Format").value(format).end();
                xml.end();
            }
            xml.end();
        }

        MediaTranscodeObject transcode = operation.getTranscode();
        String format = transcode.getContainer().getFormat();
        if (CheckObjectUtils.objIsNotValid(transcode) && !StringUtils.isNullOrEmpty(format)) {
            xml.start("Transcode");
            MediaTranscodeVideoObject video = transcode.getVideo();
            MediaAudioObject audio = transcode.getAudio();
            MediaTransConfigObject transConfig = transcode.getTransConfig();
            MediaTimeIntervalObject timeInterval = transcode.getTimeInterval();
            if (format != null) {
                xml.start("Container");
                xml.start("Format").value(format).end();
                xml.end();
            }
            if (CheckObjectUtils.objIsNotValid(timeInterval)) {
                xml.start("TimeInterval");
                xml.start("Duration").value(timeInterval.getDuration()).end();
                xml.start("Start").value(timeInterval.getStart()).end();
                xml.end();
            }
            if (CheckObjectUtils.objIsNotValid(video)) {
                addVideo(xml, video);

            }
            if (CheckObjectUtils.objIsNotValid(audio)) {
                addAudio(xml, audio);
            }

            if (CheckObjectUtils.objIsNotValid(transConfig)) {
                xml.start("TransConfig");
                addIfNotNull(xml, "AdjDarMethod", transConfig.getAdjDarMethod());
                addIfNotNull(xml, "AudioBitrateAdjMethod", transConfig.getAudioBitrateAdjMethod());
                addIfNotNull(xml, "IsCheckAudioBitrate", transConfig.getIsCheckAudioBitrate());
                addIfNotNull(xml, "IsCheckReso", transConfig.getIsCheckReso());
                addIfNotNull(xml, "IsCheckVideoBitrate", transConfig.getIsCheckVideoBitrate());
                addIfNotNull(xml, "ResoAdjMethod", transConfig.getResoAdjMethod());
                addIfNotNull(xml, "TransMode", transConfig.getTransMode());
                addIfNotNull(xml, "VideoBitrateAdjMethod", transConfig.getVideoBitrateAdjMethod());
                xml.end();
            }
            xml.end();
        }

        xml.start("Output");
        xml.start("Region").value(operation.getOutput().getRegion()).end();
        xml.start("Object").value(operation.getOutput().getObject()).end();
        xml.start("Bucket").value(operation.getOutput().getBucket()).end();
        xml.end();

        xml.end();
        xml.start("QueueId").value(request.getQueueId()).end();
        addIfNotNull(xml, "CallBack", request.getCallBack());
        xml.end();
        return xml.getBytes();
    }

    /**
     * Converts the MediaQueueRequest to an XML fragment that can be sent to the QueueObject of COS
     *
     * @param request The container which provides options for restoring an object
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(MediaQueueRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("Name").value(request.getName()).end();
        xml.start("QueueId").value(request.getQueueId()).end();
        xml.start("State").value(request.getState()).end();
        xml.start("NotifyConfig");
        addIfNotNull(xml, "Type", request.getNotifyConfig().getType());
        addIfNotNull(xml, "Url", request.getNotifyConfig().getUrl());
        addIfNotNull(xml, "Event", request.getNotifyConfig().getEvent());
        xml.end();
        xml.end();
        return xml.getBytes();
    }

    /**
     * Converts the SnapshotRequest to an XML fragment that can be sent to the SnapshotObject of COS
     *
     * @param request The container which provides options for restoring an object
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(SnapshotRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("Input");
        xml.start("Object").value(request.getInput().getObject()).end();
        xml.end();

        xml.start("Output");
        xml.start("Region").value(request.getOutput().getRegion()).end();
        xml.start("Object").value(request.getOutput().getObject()).end();
        xml.start("Bucket").value(request.getOutput().getBucket()).end();
        xml.end();

        addIfNotNull(xml, "Time", request.getTime());
        addIfNotNull(xml, "Format", request.getFormat());
        addIfNotNull(xml, "Height", request.getHeight());
        addIfNotNull(xml, "Mode", request.getMode());
        addIfNotNull(xml, "Rotate", request.getRotate());
        addIfNotNull(xml, "Width", request.getWidth());

        xml.end();
        return xml.getBytes();
    }

    /**
     * Converts the MediaTemplateRequest to an XML fragment that can be sent to the TemplateObject of COS MediaTemplate
     *
     * @param request The container which provides options for restoring an object
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(MediaTemplateRequest request) {
        XmlWriter xml = new XmlWriter();
        String tag = request.getTag();
        xml.start("Request");
        xml.start("Tag").value(tag).end();
        xml.start("Name").value(request.getName()).end();

        if ("Animation".equalsIgnoreCase(tag)) {
            xml.start("Container");
            xml.start("Format").value(request.getContainer().getFormat()).end();
            xml.end();
            addVideo(xml, request);
            if (CheckObjectUtils.objIsNotValid(request.getTimeInterval())) {
                xml.start("TimeInterval");
                xml.start("Duration").value(request.getTimeInterval().getDuration()).end();
                xml.start("Start").value(request.getTimeInterval().getStart()).end();
                xml.end();
            }
        } else if ("Snapshot".equalsIgnoreCase(tag)) {
            if (CheckObjectUtils.objIsNotValid(request.getSnapshot())) {
                xml.start("Snapshot");
                MediaSnapshotObject snapshot = request.getSnapshot();
                addIfNotNull(xml, "Mode", snapshot.getMode());
                addIfNotNull(xml, "Count", snapshot.getCount());
                addIfNotNull(xml, "Fps", snapshot.getFps());
                addIfNotNull(xml, "Height", snapshot.getHeight());
                addIfNotNull(xml, "Start", snapshot.getStart());
                addIfNotNull(xml, "TimeInterval", snapshot.getTimeInterval());
                addIfNotNull(xml, "Width", snapshot.getWidth());
                xml.end();
            }
        } else if ("Watermark".equalsIgnoreCase(tag)) {
            xml.start("Watermark");
            addIfNotNull(xml, "Type", request.getWatermark().getType());
            addIfNotNull(xml, "Dx", request.getWatermark().getDx());
            addIfNotNull(xml, "Dy", request.getWatermark().getDy());
            addIfNotNull(xml, "EndTime", request.getWatermark().getEndTime());
            addIfNotNull(xml, "LocMode", request.getWatermark().getLocMode());
            addIfNotNull(xml, "Pos", request.getWatermark().getPos());
            addIfNotNull(xml, "StartTime", request.getWatermark().getStartTime());
            if ("Text".equalsIgnoreCase(request.getWatermark().getType())) {
                xml.start("Text");
                MediaWaterMarkText text = request.getWatermark().getText();
                addIfNotNull(xml, "FontColor", text.getFontColor());
                addIfNotNull(xml, "FontSize", text.getFontSize());
                addIfNotNull(xml, "FontType", text.getFontType());
                addIfNotNull(xml, "Text", text.getText());
                addIfNotNull(xml, "Transparency", text.getTransparency());
                xml.end();
            } else if ("Image".equalsIgnoreCase(request.getWatermark().getType())) {
                xml.start("Image");
                MediaWaterMarkImage image = request.getWatermark().getImage();
                addIfNotNull(xml, "Height", image.getHeight());
                addIfNotNull(xml, "Mode", image.getMode());
                addIfNotNull(xml, "Url", image.getUrl());
                addIfNotNull(xml, "Transparency", image.getTransparency());
                addIfNotNull(xml, "Width", image.getWidth());
                xml.end();
            }
            xml.end();
        } else if ("Transcode".equalsIgnoreCase(tag)) {
            xml.start("Container");
            xml.start("Format").value(request.getContainer().getFormat()).end();
            xml.end();
            xml.start("TimeInterval");
            xml.start("Duration").value(request.getTimeInterval().getDuration()).end();
            xml.start("Start").value(request.getTimeInterval().getStart()).end();
            xml.end();

            addAudio(xml, request.getAudio());

            xml.start("TransConfig");
            addIfNotNull(xml, "AdjDarMethod", request.getTransConfig().getAdjDarMethod());
            addIfNotNull(xml, "AudioBitrateAdjMethod", request.getTransConfig().getAudioBitrateAdjMethod());
            addIfNotNull(xml, "IsCheckAudioBitrate", request.getTransConfig().getIsCheckAudioBitrate());
            addIfNotNull(xml, "IsCheckReso", request.getTransConfig().getIsCheckReso());
            addIfNotNull(xml, "IsCheckVideoBitrate", request.getTransConfig().getIsCheckVideoBitrate());
            addIfNotNull(xml, "ResoAdjMethod", request.getTransConfig().getResoAdjMethod());
            addIfNotNull(xml, "TransMode", request.getTransConfig().getTransMode());
            addIfNotNull(xml, "VideoBitrateAdjMethod", request.getTransConfig().getVideoBitrateAdjMethod());
            xml.end();

            addVideo(xml, request);

        }
        xml.end();
        return xml.getBytes();
    }

    private static void addAudio(XmlWriter xml, MediaAudioObject audio) {
        xml.start("Audio");
        addIfNotNull(xml, "Bitrate", audio.getBitrate());
        addIfNotNull(xml, "Channels", audio.getChannels());
        addIfNotNull(xml, "Codec", audio.getCodec());
        addIfNotNull(xml, "Profile", audio.getProfile());
        addIfNotNull(xml, "Remove", audio.getRemove());
        addIfNotNull(xml, "Samplerate", audio.getSamplerate());
        xml.end();
    }

    /**
     * Converts the MediaInfoRequest to an XML fragment that can be sent to the MediaObject of COS
     *
     * @param request The container which provides options for restoring an object
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(MediaInfoRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("Input");
        xml.start("Object").value(request.getInput().getObject()).end();
        xml.end();
        xml.end();
        return xml.getBytes();
    }

    /**
     * Converts the DocJobRequest to an XML fragment that can be sent to the DocJobRequestParams of CI
     *
     * @param request The container which provides options for restoring an object
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(DocJobRequest request) {
        XmlWriter xml = new XmlWriter();
        DocJobObject docJobObject = request.getDocJobObject();

        xml.start("Request");
        xml.start("Tag").value(docJobObject.getTag()).end();
        xml.start("QueueId").value(docJobObject.getQueueId()).end();
        xml.start("Input");
        xml.start("Object").value(docJobObject.getInput().getObject()).end();
        xml.end();

        if (CheckObjectUtils.objIsNotValid(docJobObject)){
            xml.start("Operation");
            xml.start("Output");
            MediaOutputObject output = docJobObject.getOperation().getOutput();
            addIfNotNull(xml, "Region", output.getRegion());
            addIfNotNull(xml, "Bucket", output.getBucket());
            addIfNotNull(xml, "Object", output.getObject());
            xml.end();

            xml.start("DocProcess");
            DocProcessObject docProcess = docJobObject.getOperation().getDocProcessObject();
            addIfNotNull(xml, "SrcType", docProcess.getSrcType());
            addIfNotNull(xml, "TgtType", docProcess.getTgtType());
            addIfNotNull(xml, "SheetId", docProcess.getSheetId());
            addIfNotNull(xml, "StartPage", docProcess.getStartPage());
            addIfNotNull(xml, "EndPage", docProcess.getEndPage());
            addIfNotNull(xml, "ImageParams", docProcess.getImageParams());
            addIfNotNull(xml, "DocPassword", docProcess.getDocPassword());
            addIfNotNull(xml, "Comments", docProcess.getComments());
            addIfNotNull(xml, "PaperDirection", docProcess.getPaperDirection());
            addIfNotNull(xml, "Quality", docProcess.getQuality());
            addIfNotNull(xml, "Zoom", docProcess.getZoom());
            xml.end();

            xml.end();
        }
        xml.end();
        return xml.getBytes();
    }

    /**
     * Converts the DocQueueRequest to an XML fragment that can be sent to the QueueObject of COS
     *
     * @param request The container which provides options for restoring an object
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(DocQueueRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("Name").value(request.getName()).end();
        xml.start("QueueId").value(request.getQueueId()).end();
        xml.start("State").value(request.getState()).end();
        xml.start("NotifyConfig");
        addIfNotNull(xml, "Type", request.getNotifyConfig().getType());
        addIfNotNull(xml, "Url", request.getNotifyConfig().getUrl());
        addIfNotNull(xml, "Event", request.getNotifyConfig().getEvent());
        addIfNotNull(xml, "State", request.getNotifyConfig().getState());
        xml.end();
        xml.end();
        return xml.getBytes();
    }

    /**
     * Converts the VideoAuditingRequest to an XML fragment that can be sent to the CreateVideoAuditingJob of CI
     *
     * @param request The container which provides options for restoring an object
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(VideoAuditingRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("Input");
        addIfNotNull(xml,"Object",request.getInput().getObject());
        addIfNotNull(xml, "Url", request.getInput().getUrl());
        addIfNotNull(xml, "DataId", request.getInput().getDataId());
        xml.end();
        Conf conf = request.getConf();
        xml.start("Conf");
        String detectType = conf.getDetectType();
        if ("all".equalsIgnoreCase(detectType)) {
            detectType = "Porn,Terrorism,Politics,Ads,Illegal,Abuse";
        }
        addIfNotNull(xml,"DetectType", detectType);
        addIfNotNull(xml,"BizType", conf.getBizType());
        addIfNotNull(xml,"DetectContent", conf.getDetectContent());
        addIfNotNull(xml,"CallbackVersion", conf.getCallbackVersion());
        xml.start("Snapshot");
        addIfNotNull(xml,"Mode", conf.getSnapshot().getMode());
        addIfNotNull(xml,"TimeInterval", conf.getSnapshot().getTimeInterval());
        addIfNotNull(xml,"Count", conf.getSnapshot().getCount());
        xml.end();
        addIfNotNull(xml,"Callback", conf.getCallback());
        xml.end();

        xml.end();
        return xml.getBytes();
    }

    /**
     * Converts the AudioAuditingRequest to an XML fragment that can be sent to the CreateAudioAuditingJob of CI
     *
     * @param request The container which provides options for restoring an object
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(AudioAuditingRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("Input");
        addIfNotNull(xml, "Object", request.getInput().getObject());
        addIfNotNull(xml, "Url", request.getInput().getUrl());
        addIfNotNull(xml, "DataId", request.getInput().getDataId());
        xml.end();
        Conf conf = request.getConf();
        xml.start("Conf");
        String detectType = conf.getDetectType();
        if ("all".equalsIgnoreCase(detectType)) {
            detectType = "Porn,Terrorism,Politics,Ads,Illegal,Abuse";
        }
        addIfNotNull(xml, "DetectType", detectType);
        addIfNotNull(xml, "Callback", conf.getCallback());
        addIfNotNull(xml, "CallbackVersion", conf.getCallbackVersion());
        addIfNotNull(xml, "BizType", conf.getBizType());
        xml.end();

        xml.end();
        return xml.getBytes();
    }

    /**
     * Converts the TextAuditingRequest to an XML fragment that can be sent to the CreateTextAuditingJob of CI
     *
     * @param request The container which provides options for restoring an object
     * @return A byte array containing the data
     * @throws CosClientException
     */
    public static byte[] convertToXmlByteArray(TextAuditingRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("Input");
        addIfNotNull(xml, "Object", request.getInput().getObject());
        addIfNotNull(xml, "Content", request.getInput().getContent());
        addIfNotNull(xml, "Url", request.getInput().getUrl());
        addIfNotNull(xml, "DataId", request.getInput().getDataId());
        xml.end();
        Conf conf = request.getConf();
        xml.start("Conf");
        String detectType = conf.getDetectType();
        if ("all".equalsIgnoreCase(detectType)) {
            detectType = "Porn,Terrorism,Politics,Ads,Illegal,Abuse";
        }
        addIfNotNull(xml, "DetectType", detectType);
        addIfNotNull(xml, "Callback", conf.getCallback());
        addIfNotNull(xml, "BizType", conf.getBizType());
        xml.end();

        xml.end();
        return xml.getBytes();
    }

    public static byte[] convertToXmlByteArray(DocumentAuditingRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("Input");
        addIfNotNull(xml, "Url", request.getInput().getUrl());
        addIfNotNull(xml, "Object", request.getInput().getObject());
        addIfNotNull(xml, "Type", request.getInput().getType());
        addIfNotNull(xml, "DataId", request.getInput().getDataId());
        xml.end();
        Conf conf = request.getConf();
        xml.start("Conf");
        String detectType = conf.getDetectType();
        if ("all".equalsIgnoreCase(detectType)) {
            detectType = "Porn,Terrorism,Politics,Ads,Illegal,Abuse";
        }
        addIfNotNull(xml, "DetectType", detectType);
        addIfNotNull(xml, "Callback", conf.getCallback());
        addIfNotNull(xml, "BizType", conf.getBizType());
        xml.end();

        xml.end();
        return xml.getBytes();
    }

    public static byte[] convertToXmlByteArray(WebpageAuditingRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        xml.start("Input");
        addIfNotNull(xml, "Url", request.getInput().getUrl());
        xml.end();
        Conf conf = request.getConf();
        xml.start("Conf");
        String detectType = conf.getDetectType();
        if ("all".equalsIgnoreCase(detectType)) {
            detectType = "Porn,Terrorism,Politics,Ads,Illegal,Abuse";
        }
        addIfNotNull(xml, "DetectType", detectType);
        addIfNotNull(xml, "Callback", conf.getCallback());
        addIfNotNull(xml, "BizType", conf.getBizType());
        addIfNotNull(xml, "ReturnHighlightHtml", conf.getReturnHighlightHtml());
        xml.end();

        xml.end();
        return xml.getBytes();
    }

    public static byte[] convertToXmlByteArray(BatchImageAuditingRequest request) {
        XmlWriter xml = new XmlWriter();

        xml.start("Request");
        List<BatchImageAuditingInputObject> inputList = request.getInputList();
        for (BatchImageAuditingInputObject inputObject : inputList) {
            xml.start("Input");
            addIfNotNull(xml, "Url", inputObject.getUrl());
            addIfNotNull(xml, "Object", inputObject.getObject());
            addIfNotNull(xml, "DataId", inputObject.getDataId());
            addIfNotNull(xml, "MaxFrames", inputObject.getMaxFrames());
            addIfNotNull(xml, "Interval", inputObject.getInterval());
            xml.end();
        }

        Conf conf = request.getConf();
        xml.start("Conf");
        String detectType = conf.getDetectType();
        if ("all".equalsIgnoreCase(detectType)) {
            detectType = "Porn,Terrorism,Politics,Ads";
        }
        addIfNotNull(xml, "DetectType", detectType);
        addIfNotNull(xml, "Callback", conf.getCallback());
        addIfNotNull(xml, "BizType", conf.getBizType());
        xml.end();

        xml.end();
        return xml.getBytes();
    }


    private static void addIfNotNull(XmlWriter xml, String xmlTag, String value) {
        if (value != null) {
            xml.start(xmlTag).value(value).end();
        }
    }

    private static void addIfNotNull(XmlWriter xml, String xmlTag, Object value) {
        if (value != null && value.toString() != null) {
            xml.start(xmlTag).value(value.toString()).end();
        }
    }

    private static void addVideo(XmlWriter xml, MediaTemplateRequest request) {
        MediaVideoObject video = request.getVideo();
        addVideo(xml,video);
    }
    private static void addVideo(XmlWriter xml, MediaVideoObject video){
        if (CheckObjectUtils.objIsValid(video)) {
            return;
        }
        xml.start("Video");
        addIfNotNull(xml, "Codec", video.getCodec());
        addIfNotNull(xml, "Width", video.getWidth());
        addIfNotNull(xml, "Height", video.getHeight());
        addIfNotNull(xml, "Fps", video.getFps());
        addIfNotNull(xml, "Bitrate", video.getBitrate());
        addIfNotNull(xml, "BufSize", video.getBufSize());
        addIfNotNull(xml, "Crf", video.getCrf());
        addIfNotNull(xml, "Crop", video.getCrop());
        addIfNotNull(xml, "Gop", video.getGop());
        addIfNotNull(xml, "LongShortMode", video.getLongShortMode());
        addIfNotNull(xml, "Maxrate", video.getMaxrate());
        addIfNotNull(xml, "Pad", video.getPad());
        addIfNotNull(xml, "PixFmt", video.getPixFmt());
        addIfNotNull(xml, "Preset", video.getPreset());
        addIfNotNull(xml, "Profile", video.getProfile());
        addIfNotNull(xml, "Qality", video.getQality());
        addIfNotNull(xml, "Quality", video.getQuality());
        addIfNotNull(xml, "Remove", video.getRemove());
        addIfNotNull(xml, "ScanMode", video.getScanMode());
        addIfNotNull(xml, "HlsTsTime", video.getHlsTsTime());
        addIfNotNull(xml, "AnimateFramesPerSecond", video.getAnimateFramesPerSecond());
        addIfNotNull(xml, "AnimateTimeIntervalOfFrame", video.getAnimateTimeIntervalOfFrame());
        addIfNotNull(xml, "AnimateOnlyKeepKeyFrame", video.getAnimateOnlyKeepKeyFrame());
        xml.end();
    }

    private static void addVideo(XmlWriter xml, MediaTranscodeVideoObject video){
        if (CheckObjectUtils.objIsValid(video)) {
            return;
        }
        xml.start("Video");
        addIfNotNull(xml, "Codec", video.getCodec());
        addIfNotNull(xml, "Width", video.getWidth());
        addIfNotNull(xml, "Height", video.getHeight());
        addIfNotNull(xml, "Fps", video.getFps());
        addIfNotNull(xml, "Bitrate", video.getBitrate());
        addIfNotNull(xml, "BufSize", video.getBufSize());
        addIfNotNull(xml, "Crf", video.getCrf());
        addIfNotNull(xml, "Gop", video.getGop());
        addIfNotNull(xml, "Maxrate", video.getMaxrate());
        addIfNotNull(xml, "Preset", video.getPreset());
        addIfNotNull(xml, "Profile", video.getProfile());
        addIfNotNull(xml, "Remove", video.getRemove());
        addIfNotNull(xml, "ScanMode", video.getScanMode());
        xml.end();
    }


    /**
     * 对象校验内部静态工具类
     */
    private static class CheckObjectUtils {

        /**
         * 校验对象是否有效，判断对象中是否含有有效的字段。
         *
         * @param obj
         * @return 包含有效值返回 true 所有字段都为空返回 false
         */
        public static Boolean objIsNotValid(Object obj) {
            //查询出对象所有的属性
            Field[] fields = obj.getClass().getDeclaredFields();
            //用于判断所有属性是否为空,如果参数为空则不查询
            for (Field field : fields) {
                //不检查 直接取值
                field.setAccessible(true);
                try {
                    Object o = field.get(obj);
                    if (!isValid(o)) {
                        //不为空
                        return true;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        public static Boolean objIsValid(Object obj) {
            return !objIsNotValid(obj);
        }

        public static boolean isValid(Object obj) {
            if (obj == null || isValid(obj.toString())) {
                return true;
            }
            return false;
        }

        public static boolean isValid(String str) {
            return str == null || "".equals(str.trim())
                    || "null".equalsIgnoreCase(str);
        }
    }
}



