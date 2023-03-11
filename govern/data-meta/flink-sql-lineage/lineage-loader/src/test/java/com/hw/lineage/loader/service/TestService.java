package com.hw.lineage.loader.service;

import com.hw.lineage.common.plugin.Plugin;

/**
 * @description: Service interface for tests of plugin mechanism.
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface TestService extends Plugin {

    String say(String name);
}
