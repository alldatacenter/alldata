package com.webank.wedatasphere.streamis.dss.appconn.structure.project;

import com.webank.wedatasphere.dss.standard.app.structure.project.*;
import com.webank.wedatasphere.dss.standard.app.structure.project.ref.DSSProjectContentRequestRef;
import com.webank.wedatasphere.streamis.dss.appconn.structure.ref.StreamisProjectContentReqRef;
import com.webank.wedatasphere.streamis.dss.appconn.structure.ref.StreamisProjectUpdateReqRef;

/**
 * Streamis project service
 */
public class StreamisProjectService extends ProjectService {

    @Override
    protected ProjectCreationOperation<DSSProjectContentRequestRef.DSSProjectContentRequestRefImpl> createProjectCreationOperation() {
        return new StreamisProjectCreationOperation();
    }

    @Override
    protected ProjectUpdateOperation<StreamisProjectUpdateReqRef> createProjectUpdateOperation() {
        return new StreamisProjectUpdateOperation();
    }

    @Override
    protected ProjectDeletionOperation<StreamisProjectContentReqRef> createProjectDeletionOperation() {
        return new StreamisPrejectDeleteOperation();
    }

    @Override
    protected ProjectSearchOperation<StreamisProjectContentReqRef> createProjectSearchOperation() {
        return new StreamisProjectSearchOperation();
    }
}
