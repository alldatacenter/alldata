package com.alibaba.tesla.appmanager.server.addon.task.dag;

import com.alibaba.tesla.dag.local.AbstractLocalDagBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Addon Instance Task DAG
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class AddonInstanceTaskDag extends AbstractLocalDagBase {

    public static String name = "addon_instance_task_runner";

    @Override
    public void draw() throws Exception {
        node("AddonInstanceTaskRunnerNode");
    }
}
