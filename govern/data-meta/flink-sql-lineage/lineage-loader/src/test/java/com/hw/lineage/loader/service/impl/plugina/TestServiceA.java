package com.hw.lineage.loader.service.impl.plugina;

import com.hw.lineage.loader.service.TestService;

/**
 * @description: First implementation of {@link TestService}.
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class TestServiceA implements TestService {

    private final TestService dynamicDelegate;

    public TestServiceA() {
        try {
            dynamicDelegate = (TestService) Class.forName(DynamicClassA.class.getName()).newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load dynamic class.");
        }
    }

    @Override
    public String say(String name) {
        return "A-" + name + "-" + dynamicDelegate.say(name);
    }
}
