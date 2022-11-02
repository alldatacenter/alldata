package com.alibaba.tesla.appmanager.common.assembly;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * base converter
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
public abstract class BaseDtoConvert<D, P> {

    public Class<D> clazzD;

    public Class<P> clazzP;

    public BaseDtoConvert(Class<D> clazzD, Class<P> clazzP) {
        this.clazzD = clazzD;
        this.clazzP = clazzP;
    }

    public D to(P source) {
        if (source == null) {
            return null;
        }
        D target;
        try {
            target = clazzD.newInstance();
        } catch (Exception e) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, String.format("cannot initialize target class. %s",
                ExceptionUtils.getStackTrace(e)));
        }
        ClassUtil.copy(source, target);
        return target;
    }

    public List<D> to(List<P> sourceList) {
        if (sourceList == null || sourceList.size() == 0) {
            return new ArrayList<>();
        }
        List<D> targetList = new ArrayList<>();
        for (P p : sourceList) {
            targetList.add(this.to(p));
        }
        return targetList;
    }

    public P from(D source) {
        if (source == null) {
            return null;
        }
        P target;
        try {
            target = clazzP.newInstance();
        } catch (Exception e) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, String.format("cannot initialize target class. %s",
                ExceptionUtils.getStackTrace(e)));
        }
        ClassUtil.copy(source, target);
        return target;
    }

    public List<P> from(List<D> sourceList) {
        if (sourceList == null || sourceList.size() == 0) {
            return Collections.emptyList();
        }
        List<P> targetList = new ArrayList<>();
        for (D d : sourceList) {
            targetList.add(this.from(d));
        }
        return targetList;
    }

    public PageInfo<D> to(Page<P> sourceList) {
        List<P> instances = sourceList.getResult();
        List<D> instanceDTOS = this.to(instances);
        PageInfo<D> pageInfo = new PageInfo<>(instanceDTOS);
        pageInfo.setPageNum(sourceList.getPageNum());
        pageInfo.setPageSize(sourceList.getPageSize());
        pageInfo.setPages(sourceList.getPages());
        pageInfo.setTotal(sourceList.getTotal());
        return pageInfo;
    }
}
