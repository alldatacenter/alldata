package com.alibaba.tesla.tkgone.server.domain.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 同义词
 *
 * @author feiquan
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Synonym extends Word {
    private String synonyms;
}
