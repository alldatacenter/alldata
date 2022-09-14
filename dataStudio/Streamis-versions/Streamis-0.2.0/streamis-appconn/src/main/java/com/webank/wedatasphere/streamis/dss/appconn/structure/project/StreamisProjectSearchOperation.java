package com.webank.wedatasphere.streamis.dss.appconn.structure.project;

import com.webank.wedatasphere.dss.common.utils.DSSCommonUtils;
import com.webank.wedatasphere.dss.standard.app.sso.origin.request.action.DSSGetAction;
import com.webank.wedatasphere.dss.standard.app.structure.AbstractStructureOperation;
import com.webank.wedatasphere.dss.standard.app.structure.project.ProjectSearchOperation;
import com.webank.wedatasphere.dss.standard.app.structure.project.ref.ProjectResponseRef;
import com.webank.wedatasphere.dss.standard.common.entity.ref.InternalResponseRef;
import com.webank.wedatasphere.dss.standard.common.exception.operation.ExternalOperationFailedException;
import com.webank.wedatasphere.streamis.dss.appconn.structure.ref.StreamisProjectContentReqRef;
import com.webank.wedatasphere.streamis.dss.appconn.utils.StreamisCommonUtil;

import static com.webank.wedatasphere.streamis.dss.appconn.constraints.Constraints.API_REQUEST_PREFIX;
import static com.webank.wedatasphere.streamis.dss.appconn.constraints.Constraints.STREAMIS_APPCONN_NAME;

public class StreamisProjectSearchOperation extends AbstractStructureOperation<StreamisProjectContentReqRef, ProjectResponseRef>
        implements ProjectSearchOperation<StreamisProjectContentReqRef> {

    private String projectUrl;

    @Override
    protected String getAppConnName() {
        return STREAMIS_APPCONN_NAME;
    }

    @Override
    public ProjectResponseRef searchProject(StreamisProjectContentReqRef streamisProjectContentReqRef) throws ExternalOperationFailedException {
        DSSGetAction getAction = new DSSGetAction();
        getAction.setUser(streamisProjectContentReqRef.getUserName());
        getAction.setParameter("projectName",streamisProjectContentReqRef.getProjectName());
        InternalResponseRef responseRef = StreamisCommonUtil.getInternalResponseRef(streamisProjectContentReqRef, ssoRequestOperation, projectUrl, getAction);
        if(responseRef.getData().get("projectId")==null){
            return ProjectResponseRef.newExternalBuilder().success();
        }
        Long projectId = DSSCommonUtils.parseToLong(responseRef.getData().get("projectId"));
        return ProjectResponseRef.newExternalBuilder()
                .setRefProjectId(projectId).success();
    }

    @Override
    public void init() {
        super.init();
        projectUrl = mergeBaseUrl(mergeUrl(API_REQUEST_PREFIX, "searchProject"));
    }
}
