package com.qcloud.cos.demo.ci;

import com.qcloud.cos.model.ciModel.auditing.AbuseInfo;
import com.qcloud.cos.model.ciModel.auditing.AdsInfo;
import com.qcloud.cos.model.ciModel.auditing.AuditingInfo;
import com.qcloud.cos.model.ciModel.auditing.AuditingJobsDetail;
import com.qcloud.cos.model.ciModel.auditing.AuditingResult;
import com.qcloud.cos.model.ciModel.auditing.AudtingCommonInfo;
import com.qcloud.cos.model.ciModel.auditing.BatchImageJobDetail;
import com.qcloud.cos.model.ciModel.auditing.DetectType;
import com.qcloud.cos.model.ciModel.auditing.IllegalInfo;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.PoliticsInfo;
import com.qcloud.cos.model.ciModel.auditing.PornInfo;
import com.qcloud.cos.model.ciModel.auditing.SectionInfo;
import com.qcloud.cos.model.ciModel.auditing.SnapshotInfo;
import com.qcloud.cos.model.ciModel.auditing.TerroristInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 审核结果获取工具类
 * 目前场景以下几类：
 * Porn（涉黄）、Terrorism（涉暴恐）、Politics（政治敏感）、Ads（广告）、Illegal（违法）、Abuse（谩骂）
 */
public class AuditingResultUtil {
    private static AuditingResult result;

    public static AuditingResult getAuditingResult(AuditingJobsDetail jobsDetail) {
        result = new AuditingResult();
        parsingAuditingResult(jobsDetail, result);
        return result;
    }

    public static List<AuditingInfo> getImageInfoList(ImageAuditingResponse response) {
        AuditingJobsDetail jobsDetail = new AuditingJobsDetail();
        List<SectionInfo> sectionList = jobsDetail.getSectionList();
        SectionInfo sectionInfo = new SectionInfo();
        sectionInfo.setPoliticsInfo(response.getPoliticsInfo());
        sectionInfo.setPornInfo(response.getPornInfo());
        sectionInfo.setAdsInfo(response.getAdsInfo());
        sectionInfo.setTerroristInfo(response.getTerroristInfo());
        sectionList.add(sectionInfo);
        return getAuditingInfoList(jobsDetail);
    }

    public static List<AuditingInfo> getBatchImageInfoList(BatchImageJobDetail response) {
        AuditingJobsDetail jobsDetail = new AuditingJobsDetail();
        List<SectionInfo> sectionList = jobsDetail.getSectionList();
        SectionInfo sectionInfo = new SectionInfo();
        sectionInfo.setPoliticsInfo(response.getPoliticsInfo());
        sectionInfo.setPornInfo(response.getPornInfo());
        sectionInfo.setAdsInfo(response.getAdsInfo());
        sectionInfo.setTerroristInfo(response.getTerroristInfo());
        sectionList.add(sectionInfo);
        return getAuditingInfoList(jobsDetail);
    }

    public static List<AuditingInfo> getAuditingInfoList(AuditingJobsDetail jobsDetail) {
        List<AuditingInfo> list = new ArrayList<>();
        List<SnapshotInfo> snapshotList = jobsDetail.getSnapshotList();
        for (SnapshotInfo snapshotInfo : snapshotList) {
            AdsInfo adsInfo = snapshotInfo.getAdsInfo();
            PoliticsInfo politicsInfo = snapshotInfo.getPoliticsInfo();
            PornInfo pornInfo = snapshotInfo.getPornInfo();
            TerroristInfo terroristInfo = snapshotInfo.getTerroristInfo();
            addAuditingInfoList(list, DetectType.Ads, adsInfo);
            addAuditingInfoList(list, DetectType.Politics, politicsInfo);
            addAuditingInfoList(list, DetectType.Porn, pornInfo);
            addAuditingInfoList(list, DetectType.Terrorism, terroristInfo);
        }
        List<SectionInfo> sectionList = jobsDetail.getSectionList();
        for (SectionInfo sectionInfo : sectionList) {
            AdsInfo adsInfo = sectionInfo.getAdsInfo();
            PoliticsInfo politicsInfo = sectionInfo.getPoliticsInfo();
            PornInfo pornInfo = sectionInfo.getPornInfo();
            TerroristInfo terroristInfo = sectionInfo.getTerroristInfo();
            IllegalInfo illegalInfo = sectionInfo.getIllegalInfo();
            AbuseInfo abuseInfo = sectionInfo.getAbuseInfo();
            addAuditingInfoList(list, DetectType.Ads, adsInfo);
            addAuditingInfoList(list, DetectType.Politics, politicsInfo);
            addAuditingInfoList(list, DetectType.Porn, pornInfo);
            addAuditingInfoList(list, DetectType.Terrorism, terroristInfo);
            addAuditingInfoList(list, DetectType.Illegal, illegalInfo);
            addAuditingInfoList(list, DetectType.Abuse, abuseInfo);
        }
        return list;
    }

    private static void addAuditingInfoList(List<AuditingInfo> list, DetectType detectType, AudtingCommonInfo info) {
        int flag = getAuditingResult(info);
        if (flag != 0) {
            AuditingInfo auditingInfo = new AuditingInfo();
            auditingInfo.setType(detectType);
            auditingInfo.setTypeName(detectType.getName());
            auditingInfo.setCount(info.getCount());
            auditingInfo.setHitFlag(info.getHitFlag());
            auditingInfo.setScore(info.getScore());
            String label = info.getLabel();
            if (!"".equals(label) && label != null) {
                auditingInfo.setKeyWords(label.split(","));
            }
            list.add(auditingInfo);
        }
    }

    private static void parsingAuditingResult(AuditingJobsDetail jobsDetail, AuditingResult result) {
        AdsInfo adsInfo = jobsDetail.getAdsInfo();
        addHitMapAndUpdateHitFlag("Ads", adsInfo);
        PoliticsInfo politicsInfo = jobsDetail.getPoliticsInfo();
        addHitMapAndUpdateHitFlag("Politics", politicsInfo);
        PornInfo pornInfo = jobsDetail.getPornInfo();
        addHitMapAndUpdateHitFlag("Porn", pornInfo);
        TerroristInfo terroristInfo = jobsDetail.getTerroristInfo();
        addHitMapAndUpdateHitFlag("Terrorism", terroristInfo);
        AbuseInfo abuseInfo = jobsDetail.getAbuseInfo();
        addHitMapAndUpdateHitFlag("Abuse", abuseInfo);
        IllegalInfo illegalInfo = jobsDetail.getIllegalInfo();
        addHitMapAndUpdateHitFlag("Illegal", illegalInfo);

        List<SectionInfo> sectionList = jobsDetail.getSectionList();
        List<SnapshotInfo> snapshotList = jobsDetail.getSnapshotList();
        for (SectionInfo sectionInfo : sectionList) {
            addHitMapAndUpdateHitFlag("Ads", sectionInfo.getAdsInfo());
            addHitMapAndUpdateHitFlag("Politics", sectionInfo.getPoliticsInfo());
            addHitMapAndUpdateHitFlag("Porn", sectionInfo.getPornInfo());
            addHitMapAndUpdateHitFlag("Terrorism", sectionInfo.getTerroristInfo());
            addHitMapAndUpdateHitFlag("Abuse", sectionInfo.getAbuseInfo());
            addHitMapAndUpdateHitFlag("Illegal", sectionInfo.getIllegalInfo());
        }
        for (SnapshotInfo snapshotInfo : snapshotList) {
            addHitMapAndUpdateHitFlag("Ads", snapshotInfo.getAdsInfo());
            addHitMapAndUpdateHitFlag("Politics", snapshotInfo.getPoliticsInfo());
            addHitMapAndUpdateHitFlag("Porn", snapshotInfo.getPornInfo());
            addHitMapAndUpdateHitFlag("Terrorism", snapshotInfo.getTerroristInfo());
        }
    }

    private static int getAuditingResult(AudtingCommonInfo info) {
        if (info == null)
            return 0;
        String hitFlag = info.getHitFlag();
        if ("0".equals(hitFlag)) {
            return 0;
        } else if ("1".equals(hitFlag)) {
            return 1;
        } else if ("2".equals(hitFlag)) {
            return 2;
        }
        return 0;
    }

    private static void addHitMapAndUpdateHitFlag(String key, AudtingCommonInfo commonInfo) {
        Integer hitFlag = result.getHitFlag();
        Map<String, Integer> hitMap = result.getHitMap();
        int flag = getAuditingResult(commonInfo);
        if (flag != 0) {
            if (hitFlag == 0) {
                result.setHitFlag(flag);
            } else if (hitFlag == 2 && flag == 1) {
                result.setHitFlag(flag);
            }
            Integer score = hitMap.get("key");
            if (commonInfo.getScore() != null && !"".equals(commonInfo.getScore())) {
                int score2 = Integer.parseInt(commonInfo.getScore());
                if (score != null && score2 > score) {
                    hitMap.put(key, score2);
                } else if (score == null) {
                    hitMap.put(key, score2);
                }
            } else {
                if (score == null) {
                    hitMap.put(key, 0);
                }
            }
        }
    }

}
