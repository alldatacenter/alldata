package com.webank.wedatasphere.streamis.dss.appconn.structure.project;

import com.webank.wedatasphere.dss.standard.app.sso.origin.request.action.DSSDeleteAction;
import com.webank.wedatasphere.dss.standard.app.structure.AbstractStructureOperation;
import com.webank.wedatasphere.dss.standard.app.structure.project.ProjectDeletionOperation;
import com.webank.wedatasphere.dss.standard.common.entity.ref.ResponseRef;
import com.webank.wedatasphere.dss.standard.common.exception.operation.ExternalOperationFailedException;
import com.webank.wedatasphere.streamis.dss.appconn.structure.ref.StreamisProjectContentReqRef;
import com.webank.wedatasphere.streamis.dss.appconn.utils.StreamisCommonUtil;

import static com.webank.wedatasphere.streamis.dss.appconn.constraints.Constraints.API_REQUEST_PREFIX;
import static com.webank.wedatasphere.streamis.dss.appconn.constraints.Constraints.STREAMIS_APPCONN_NAME;

public class StreamisPrejectDeleteOperation extends AbstractStructureOperation<StreamisProjectContentReqRef, ResponseRef>
        implements ProjectDeletionOperation<StreamisProjectContentReqRef> {

    private String projectUrl;

    @Override
    protected String getAppConnName() {
        return STREAMIS_APPCONN_NAME;
    }

    @Override
    public ResponseRef deleteProject(StreamisProjectContentReqRef refProjectContentRequestRef) throws ExternalOperationFailedException {
        DSSDeleteAction deleteAction = new DSSDeleteAction();
        deleteAction.setUser(refProjectContentRequestRef.getUserName());
        deleteAction.setParameter("projectId", refProjectContentRequestRef.getRefProjectId());
        return StreamisCommonUtil.getInternalResponseRef(refProjectContentRequestRef, ssoRequestOperation, projectUrl, deleteAction);
    }

    @Override
    public void init() {
        super.init();
        projectUrl = mergeBaseUrl(mergeUrl(API_REQUEST_PREFIX, "deleteProject"));
    }

}
