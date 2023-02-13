

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.extension.util.PluginExtraProps;
import junit.framework.TestCase;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXHbase20xsqlWriter extends TestCase {
    public void testGetDftTemplate() {
        String dftTemplate = DataXHbase20xsqlWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXHbase20xsqlWriter.class);
        assertTrue(extraProps.isPresent());
    }
}
