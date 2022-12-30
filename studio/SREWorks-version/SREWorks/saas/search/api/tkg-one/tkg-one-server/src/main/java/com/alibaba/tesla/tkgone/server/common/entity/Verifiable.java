package com.alibaba.tesla.tkgone.server.common.entity;

/**
 * 实现本接口的类都可以对自身数据的合法性进行检查
 * @author feiquan
 */
public interface Verifiable {
    /**
     * 检查自身数据合法性
     * @throws IllegalArgumentException
     */
    void verify() throws IllegalArgumentException;
}
