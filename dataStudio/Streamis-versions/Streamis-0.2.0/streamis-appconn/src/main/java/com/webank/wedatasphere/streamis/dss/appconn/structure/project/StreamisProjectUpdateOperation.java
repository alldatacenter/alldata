package com.webank.wedatasphere.streamis.dss.appconn.structure.project;

import com.webank.wedatasphere.dss.common.entity.project.DSSProject;
import com.webank.wedatasphere.dss.standard.app.sso.Workspace;
import com.webank.wedatasphere.dss.standard.app.sso.origin.request.action.DSSPutAction;
import com.webank.wedatasphere.dss.standard.app.structure.AbstractStructureOperation;
import com.webank.wedatasphere.dss.standard.app.structure.project.ProjectUpdateOperation;
import com.webank.wedatasphere.dss.standard.app.structure.project.ref.DSSProjectPrivilege;
import com.webank.wedatasphere.dss.standard.common.entity.ref.ResponseRef;
import com.webank.wedatasphere.dss.standard.common.exception.operation.ExternalOperationFailedException;
import com.webank.wedatasphere.streamis.dss.appconn.exception.StreamisAppConnErrorException;
import com.webank.wedatasphere.streamis.dss.appconn.structure.ref.StreamisProjectUpdateReqRef;
import com.webank.wedatasphere.streamis.dss.appconn.utils.StreamisCommonUtil;

import static com.webank.wedatasphere.streamis.dss.appconn.constraints.Constraints.API_REQUEST_PREFIX;
import static com.webank.wedatasphere.streamis.dss.appconn.constraints.Constraints.STREAMIS_APPCONN_NAME;

public class StreamisProjectUpdateOperation extends AbstractStructureOperation<StreamisProjectUpdateReqRef, ResponseRef>
        implements ProjectUpdateOperation<StreamisProjectUpdateReqRef> {

    private String projectUrl;

    @Override
    protected String getAppConnName() {
        return STREAMIS_APPCONN_NAME;
    }

    @Override
    public ResponseRef updateProject(StreamisProjectUpdateReqRef projectUpdateRequestRef) throws ExternalOperationFailedException {
        DSSPutAction updateAction = new DSSPutAction();
        updateAction.setUser(projectUpdateRequestRef.getUserName());
        DSSProject dssProject = projectUpdateRequestRef.getDSSProject();
        DSSProjectPrivilege dssProjectPrivilege = projectUpdateRequestRef.getDSSProjectPrivilege();
        Workspace workspace = projectUpdateRequestRef.getWorkspace();
        if(dssProject == null || dssProjectPrivilege == null){
            throw new StreamisAppConnErrorException(600500, "the dssProject or dssProjectPrivilege is null");
        }
        updateAction.addRequestPayload("projectId",projectUpdateRequestRef.getRefProjectId());
        updateAction.addRequestPayload("projectName",dssProject.getName());
        updateAction.addRequestPayload("workspaceId", workspace==null?null:workspace.getWorkspaceId());
        updateAction.addRequestPayload("releaseUsers",dssProjectPrivilege.getReleaseUsers());
        updateAction.addRequestPayload("editUsers",dssProjectPrivilege.getEditUsers());
        updateAction.addRequestPayload("accessUsers",dssProjectPrivilege.getAccessUsers());
        return StreamisCommonUtil.getInternalResponseRef(projectUpdateRequestRef, ssoRequestOperation, projectUrl, updateAction);
    }


    @Override
    public void init() {
        super.init();
        projectUrl = mergeBaseUrl(mergeUrl(API_REQUEST_PREFIX, "updateProject"));
    }

}


