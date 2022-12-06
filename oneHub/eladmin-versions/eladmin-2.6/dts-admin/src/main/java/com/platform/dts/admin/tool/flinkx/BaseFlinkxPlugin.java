package com.platform.dts.admin.tool.flinkx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象实现类
 *
 * @author AllDataDC
 * @ClassName BaseFlinkxPlugin
 * @Version 1.0
 * @since 2022/11/31 9:45
 */
public abstract class BaseFlinkxPlugin implements FlinkxPluginInterface {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

}
