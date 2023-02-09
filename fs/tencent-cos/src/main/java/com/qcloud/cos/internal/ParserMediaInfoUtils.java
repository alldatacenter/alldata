package com.qcloud.cos.internal;

import com.qcloud.cos.model.ciModel.job.MediaTranscodeVideoObject;
import com.qcloud.cos.model.ciModel.job.MediaVideoObject;
import com.qcloud.cos.model.ciModel.job.MediaTimeIntervalObject;
import com.qcloud.cos.model.ciModel.job.MediaTransConfigObject;
import com.qcloud.cos.model.ciModel.job.MediaAudioObject;
import com.qcloud.cos.model.ciModel.job.MediaRemoveWaterMark;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaFormat;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoAudio;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoSubtitle;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoVideo;
import com.qcloud.cos.model.ciModel.template.MediaSnapshotObject;
import com.qcloud.cos.model.ciModel.template.MediaWaterMarkImage;
import com.qcloud.cos.model.ciModel.template.MediaWaterMarkText;
import com.qcloud.cos.model.ciModel.template.MediaWatermark;

/**
 * MediaInfo 解析工具类
 */
public class ParserMediaInfoUtils {

    public static void ParsingMediaVideo(MediaInfoVideo video, String name, String value) {
        switch (name) {
            case "AvgFps":
                video.setAvgFps(value);
                break;
            case "CodecLongName":
                video.setCodecLongName(value);
                break;
            case "CodecName":
                video.setCodecName(value);
                break;
            case "CodecTag":
                video.setCodecTag(value);
                break;
            case "CodecTagString":
                video.setCodecTagString(value);
                break;
            case "CodecTimeBase":
                video.setCodecTimeBase(value);
                break;
            case "Duration":
                video.setDuration(value);
                break;
            case "FieldOrder":
                video.setFieldOrder(value);
                break;
            case "Fps":
                video.setFps(value);
                break;
            case "HasBFrame":
                video.setHasBFrame(value);
                break;
            case "Height":
                video.setHeight(value);
                break;
            case "Index":
                video.setIndex(value);
                break;
            case "Level":
                video.setLevel(value);
                break;
            case "PixFormat":
                video.setPixFormat(value);
                break;
            case "Profile":
                video.setProfile(value);
                break;
            case "RefFrames":
                video.setRefFrames(value);
                break;
            case "StartTime":
                video.setStartTime(value);
                break;
            case "Timebase":
                video.setTimebase(value);
                break;
            case "Width":
                video.setWidth(value);
                break;
            case "Dar":
                video.setDar(value);
                break;
            case "Rotation":
                video.setRotation(value);
                break;
            case "Sar":
                video.setSar(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingMediaVideo(MediaVideoObject video, String name, String value) {
        switch (name) {
            case "Codec":
                video.setCodec(value);
                break;
            case "Crf":
                video.setCrf(value);
                break;
            case "Crop":
                video.setCrop(value);
                break;
            case "Fps":
                video.setFps(value);
                break;
            case "Height":
                video.setHeight(value);
                break;
            case "Width":
                video.setWidth(value);
                break;
            case "PixFmt":
                video.setPixFmt(value);
                break;
            case "Maxrate":
                video.setMaxrate(value);
                break;
            case "BufSize":
                video.setBufSize(value);
                break;
            case "Preset":
                video.setPreset(value);
                break;
            case "Bitrate":
                video.setBitrate(value);
                break;
            case "Profile":
                video.setProfile(value);
                break;
            case "AnimateOnlyKeepKeyFrame":
                video.setAnimateOnlyKeepKeyFrame(value);
                break;
            case "AnimateFramesPerSecond":
                video.setAnimateFramesPerSecond(value);
                break;
            case "AnimateTimeIntervalOfFrame":
                video.setAnimateTimeIntervalOfFrame(value);
                break;
            case "Gop":
                video.setGop(value);
                break;
            case "HlsTsTime":
                video.setHlsTsTime(value);
                break;
            case "LongShortMode":
                video.setLongShortMode(value);
                break;
            case "Pad":
                video.setPad(value);
                break;
            case "Qality":
                video.setQality(value);
                break;
            case "Quality":
                video.setQuality(value);
                break;
            case "Remove":
                video.setRemove(value);
                break;
            case "ScanMode":
                video.setScanMode(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingMediaVideo(MediaTranscodeVideoObject video, String name, String value) {
        switch (name) {
            case "Codec":
                video.setCodec(value);
                break;
            case "Crf":
                video.setCrf(value);
                break;
            case "Fps":
                video.setFps(value);
                break;
            case "Height":
                video.setHeight(value);
                break;
            case "Width":
                video.setWidth(value);
                break;
            case "Maxrate":
                video.setMaxrate(value);
                break;
            case "BufSize":
                video.setBufSize(value);
                break;
            case "Preset":
                video.setPreset(value);
                break;
            case "Bitrate":
                video.setBitrate(value);
                break;
            case "Profile":
                video.setProfile(value);
                break;
            case "Gop":
                video.setGop(value);
                break;
            case "Remove":
                video.setRemove(value);
                break;
            case "ScanMode":
                video.setScanMode(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingMediaTimeInterval(MediaTimeIntervalObject timeInterval, String name, String value) {
        switch (name) {
            case "Duration":
                timeInterval.setDuration(value);
                break;
            case "Start":
                timeInterval.setStart(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingSnapshot(MediaSnapshotObject snapshot, String name, String value) {
        switch (name) {
            case "Count":
                snapshot.setCount(value);
                break;
            case "Mode":
                snapshot.setMode(value);
                break;
            case "Start":
                snapshot.setStart(value);
                break;
            case "Width":
                snapshot.setWidth(value);
                break;
            case "Fps":
                snapshot.setFps(value);
                break;
            case "Height":
                snapshot.setHeight(value);
                break;
            case "TimeInterval":
                snapshot.setTimeInterval(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingMediaAudio(MediaAudioObject audio, String name, String value) {
        switch (name) {
            case "Channels":
                audio.setChannels(value);
                break;
            case "Bitrate":
                audio.setBitrate(value);
                break;
            case "Samplerate":
                audio.setSamplerate(value);
                break;
            case "Codec":
                audio.setCodec(value);
                break;
            case "Profile":
                audio.setProfile(value);
                break;
            case "Remove":
                audio.setRemove(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingTransConfig(MediaTransConfigObject transConfig, String name, String value) {
        switch (name) {
            case "ResoAdjMethod":
                transConfig.setResoAdjMethod(value);
                break;
            case "IsCheckReso":
                transConfig.setIsCheckReso(value);
                break;
            case "AdjDarMethod":
                transConfig.setAdjDarMethod(value);
                break;
            case "AudioBitrateAdjMethod":
                transConfig.setAudioBitrateAdjMethod(value);
                break;
            case "IsCheckAudioBitrate":
                transConfig.setIsCheckAudioBitrate(value);
                break;
            case "IsCheckVideoBitrate":
                transConfig.setIsCheckVideoBitrate(value);
                break;
            case "TransMode":
                transConfig.setTransMode(value);
                break;
            case "VideoBitrateAdjMethod":
                transConfig.setVideoBitrateAdjMethod(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingWatermark(MediaWatermark watermark, String name, String value) {
        switch (name) {
            case "Dx":
                watermark.setDx(value);
                break;
            case "Dy":
                watermark.setDy(value);
                break;
            case "EndTime":
                watermark.setEndTime(value);
                break;
            case "LocMode":
                watermark.setLocMode(value);
                break;
            case "Pos":
                watermark.setPos(value);
                break;
            case "StartTime":
                watermark.setStartTime(value);
                break;
            case "Type":
                watermark.setType(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingWatermarkText(MediaWaterMarkText text, String name, String value) {
        switch (name) {
            case "FontType":
                text.setFontType(value);
                break;
            case "Transparency":
                text.setTransparency(value);
                break;
            case "FontSize":
                text.setFontSize(value);
                break;
            case "Text":
                text.setText(value);
                break;
            case "FontColor":
                text.setFontColor(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingWatermarkImage(MediaWaterMarkImage image, String name, String value) {
        switch (name) {
            case "Transparency":
                image.setTransparency(value);
                break;
            case "Height":
                image.setHeight(value);
                break;
            case "Width":
                image.setWidth(value);
                break;
            case "Mode":
                image.setMode(value);
                break;
            case "Url":
                image.setUrl(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingMediaFormat(MediaFormat format, String name, String value) {
        switch (name) {
            case "Bitrate":
                format.setBitrate(value);
                break;
            case "Duration":
                format.setDuration(value);
                break;
            case "FormatLongName":
                format.setFormatLongName(value);
                break;
            case "FormatName":
                format.setFormatName(value);
                break;
            case "NumProgram":
                format.setNumProgram(value);
                break;
            case "NumStream":
                format.setNumStream(value);
                break;
            case "Size":
                format.setSize(value);
                break;
            case "StartTime":
                format.setStartTime(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingStreamAudio(MediaInfoAudio audio, String name, String value) {
        switch (name) {
            case "Timebase":
                audio.setTimebase(value);
                break;
            case "StartTime":
                audio.setStartTime(value);
                break;
            case "SampleRate":
                audio.setSampleRate(value);
                break;
            case "SampleFmt":
                audio.setSampleFmt(value);
                break;
            case "Language":
                audio.setLanguage(value);
                break;
            case "Index":
                audio.setIndex(value);
                break;
            case "Duration":
                audio.setDuration(value);
                break;
            case "CodecTimeBase":
                audio.setCodecTimeBase(value);
                break;
            case "CodecTagString":
                audio.setCodecTagString(value);
                break;
            case "CodecTag":
                audio.setCodecTag(value);
                break;
            case "Bitrate":
                audio.setBitrate(value);
                break;
            case "Channel":
                audio.setChannel(value);
                break;
            case "ChannelLayout":
                audio.setChannelLayout(value);
                break;
            case "CodecLongName":
                audio.setCodecLongName(value);
                break;
            case "CodecName":
                audio.setCodecName(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingStreamAudio(MediaAudioObject audio, String name, String value) {
        switch (name) {
            case "Codec":
                audio.setCodec(value);
                break;
            case "Samplerate":
                audio.setSamplerate(value);
                break;
            case "Bitrate":
                audio.setBitrate(value);
                break;
            case "Channels":
                audio.setChannels(value);
                break;
            case "Remove":
                audio.setRemove(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingSubtitle(MediaInfoSubtitle subtitle, String name, String value) {
        switch (name) {
            case "Index":
                subtitle.setIndex(value);
                break;
            case "Language":
                subtitle.setLanguage(value);
                break;
            default:
                break;
        }
    }

    public static void ParsingRemoveWatermark(MediaRemoveWaterMark removeWatermark, String name, String value) {
        switch (name) {
            case "Switch":
                removeWatermark.set_switch(value);
                break;
            case "Dx":
                removeWatermark.setDx(value);
                break;
            case "Dy":
                removeWatermark.setDy(value);
                break;
            case "Height":
                removeWatermark.setHeight(value);
                break;
            case "Width":
                removeWatermark.setWidth(value);
                break;
            default:
                break;
        }
    }
}