package com.hw.lineage.loader.service.impl.pluginb;

import com.hw.lineage.loader.service.TestService;

/**
 * @description: Second implementation of {@link TestService}.
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class TestServiceB implements TestService {
    @Override
    public String say(String name) {
        return "B-" + name;
    }
}
