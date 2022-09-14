package com.alibaba.tesla.appmanager.server.addon;

import com.alibaba.tesla.appmanager.server.addon.req.ApplyAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.req.ReleaseAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.res.ApplyAddonRes;
import com.alibaba.tesla.appmanager.server.repository.AddonInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.AddonMetaRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonMetaDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * Addon 对象公共实现
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public abstract class BaseAddon implements Addon {

    @Resource
    private AddonMetaRepository addonMetaRepository;

    @Resource
    private AddonInstanceRepository addonInstanceRepository;

    /**
     * 获取 Addon 元信息数据
     *
     * @param addonId      Addon 唯一标识符
     * @param addonVersion Addon 版本
     * @return AddonMetaDO 对象
     */
    protected AddonMetaDO getAddonMeta(String addonId, String addonVersion) {
        AddonMetaQueryCondition condition = AddonMetaQueryCondition.builder()
                .addonId(addonId)
                .addonVersion(addonVersion)
                .build();
        List<AddonMetaDO> addonMetaDOS = addonMetaRepository.selectByCondition(condition);
        if (CollectionUtils.isEmpty(addonMetaDOS)) {
            throw new IllegalArgumentException("addon meta not exist. addonId=" + addonId);
        }
        if (addonMetaDOS.size() > 1) {
            throw new IllegalArgumentException("exist multi addon, addonId=" + addonId);
        }
        return addonMetaDOS.get(0);
    }

    /**
     * 申请 Addon 实例
     *
     * @param request 创建请求
     * @return addonInstanceId
     */
    @Override
    public ApplyAddonRes apply(ApplyAddonInstanceReq request) {
        return ApplyAddonRes.builder()
                .componentSchema(request.getSchema())
                .signature(null)
                .build();
    }

    /**
     * 释放 Addon 实例
     *
     * @param request 释放请求
     */
    @Override
    public void release(ReleaseAddonInstanceReq request) {
        String addonInstanceId = request.getAddonInstanceId();
        addonInstanceRepository.deleteByCondition(AddonInstanceQueryCondition.builder()
                .addonInstanceId(request.getAddonInstanceId())
                .build());
        log.info("addon instance {} has released", addonInstanceId);
    }
    //
    //    /**
    //     * 获取 Addon 扩展资源信息
    //     *
    //     * @return Addon 扩展资源对象
    //     */
    //    @Override
    //    public AddonBaseDataOutput getDataOutput(String addonInstanceId) {
    //        AddonInstanceQueryCondition condition = AddonInstanceQueryCondition.builder()
    //            .addonInstanceId(addonInstanceId)
    //            .build();
    //        AddonInstanceDO addonInstance = addonInstanceRepository.getByCondition(condition);
    //        if (addonInstance == null) {
    //            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
    //                String.format("cannot find addon instance %s", addonInstanceId));
    //        }
    //        String dataOutput = addonInstance.getDataOutput();
    //        return AppObjectConvertUtil.from(dataOutput, getDataOutputClass());
    //    }
}
