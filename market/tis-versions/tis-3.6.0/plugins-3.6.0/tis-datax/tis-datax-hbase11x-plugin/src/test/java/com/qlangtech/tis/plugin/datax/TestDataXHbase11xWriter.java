package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.extension.util.PluginExtraProps;
import junit.framework.TestCase;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXHbase11xWriter extends TestCase {
    public void testGetDftTemplate() {
        String dftTemplate = DataXHbase11xWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXHbase11xWriter.class);
        assertTrue(extraProps.isPresent());
    }
}
