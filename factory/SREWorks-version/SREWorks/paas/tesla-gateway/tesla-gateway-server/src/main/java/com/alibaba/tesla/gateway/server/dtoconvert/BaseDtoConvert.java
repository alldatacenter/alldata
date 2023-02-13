package com.alibaba.tesla.gateway.server.dtoconvert;


import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public interface BaseDtoConvert<D, P> {
    /**
     * do -> dto
     * @param p
     * @return
     */
    D to(P p);

    /**
     * batch convert
     * @param pList
     * @return
     */
    default List<D> to(List<P> pList){
        if (CollectionUtils.isEmpty(pList)){
            return Collections.emptyList();
        }
        List<D> dList = new ArrayList<>();
        for (P p : pList) {
            dList.add(this.to(p));
        }
        return dList;
    }

    /**
     * dto -> do
     * @param dto
     * @return
     */
    P from(D dto);

    /**
     * dtos -> dos
     * @param dtos
     * @return
     */
    default List<P> from(List<D> dtos){
        if (CollectionUtils.isEmpty(dtos)){
            return Collections.emptyList();
        }
        List<P> pList = new ArrayList<>();
        for (D d : dtos) {
            pList.add(this.from(d));
        }
        return pList;
    }
}
