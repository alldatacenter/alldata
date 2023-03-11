package com.hw.lineage.loader.service.impl.plugina;

import com.hw.lineage.loader.service.TestService;

/**
 * This class exists in the test to validate that dynamic classloading ({@link
 * Class#forName(String)} works inside of plugin code.
 *
 * @description: DynamicClassA
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class DynamicClassA implements TestService {
    @Override
    public String say(String name) {
        return "Dynamic-A-" + name;
    }
}
