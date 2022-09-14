package com.webank.wedatasphere.streamis.dss.appconn.structure;

import com.webank.wedatasphere.dss.standard.app.structure.AbstractStructureIntegrationStandard;
import com.webank.wedatasphere.dss.standard.app.structure.project.ProjectService;
import com.webank.wedatasphere.streamis.dss.appconn.structure.project.StreamisProjectService;

/**
 * Structure integration standard
 */
public class StreamisStructureIntegrationStandard extends AbstractStructureIntegrationStandard {

    /**
     * Singleton project service
     * @return
     */
    @Override
    protected ProjectService createProjectService() {
        return new StreamisProjectService();
    }
}
