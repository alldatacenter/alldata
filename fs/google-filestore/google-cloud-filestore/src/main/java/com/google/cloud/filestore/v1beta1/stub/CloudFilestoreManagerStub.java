/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.filestore.v1beta1.stub;

import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListBackupsPagedResponse;
import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListInstancesPagedResponse;
import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListSharesPagedResponse;
import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListSnapshotsPagedResponse;

import com.google.api.core.BetaApi;
import com.google.api.gax.core.BackgroundResource;
import com.google.api.gax.rpc.OperationCallable;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.common.OperationMetadata;
import com.google.cloud.filestore.v1beta1.Backup;
import com.google.cloud.filestore.v1beta1.CreateBackupRequest;
import com.google.cloud.filestore.v1beta1.CreateInstanceRequest;
import com.google.cloud.filestore.v1beta1.CreateShareRequest;
import com.google.cloud.filestore.v1beta1.CreateSnapshotRequest;
import com.google.cloud.filestore.v1beta1.DeleteBackupRequest;
import com.google.cloud.filestore.v1beta1.DeleteInstanceRequest;
import com.google.cloud.filestore.v1beta1.DeleteShareRequest;
import com.google.cloud.filestore.v1beta1.DeleteSnapshotRequest;
import com.google.cloud.filestore.v1beta1.GetBackupRequest;
import com.google.cloud.filestore.v1beta1.GetInstanceRequest;
import com.google.cloud.filestore.v1beta1.GetShareRequest;
import com.google.cloud.filestore.v1beta1.GetSnapshotRequest;
import com.google.cloud.filestore.v1beta1.Instance;
import com.google.cloud.filestore.v1beta1.ListBackupsRequest;
import com.google.cloud.filestore.v1beta1.ListBackupsResponse;
import com.google.cloud.filestore.v1beta1.ListInstancesRequest;
import com.google.cloud.filestore.v1beta1.ListInstancesResponse;
import com.google.cloud.filestore.v1beta1.ListSharesRequest;
import com.google.cloud.filestore.v1beta1.ListSharesResponse;
import com.google.cloud.filestore.v1beta1.ListSnapshotsRequest;
import com.google.cloud.filestore.v1beta1.ListSnapshotsResponse;
import com.google.cloud.filestore.v1beta1.RestoreInstanceRequest;
import com.google.cloud.filestore.v1beta1.RevertInstanceRequest;
import com.google.cloud.filestore.v1beta1.Share;
import com.google.cloud.filestore.v1beta1.Snapshot;
import com.google.cloud.filestore.v1beta1.UpdateBackupRequest;
import com.google.cloud.filestore.v1beta1.UpdateInstanceRequest;
import com.google.cloud.filestore.v1beta1.UpdateShareRequest;
import com.google.cloud.filestore.v1beta1.UpdateSnapshotRequest;
import com.google.longrunning.Operation;
import com.google.longrunning.stub.OperationsStub;
import com.google.protobuf.Empty;
import javax.annotation.Generated;

// AUTO-GENERATED DOCUMENTATION AND CLASS.
/**
 * Base stub class for the CloudFilestoreManager service API.
 *
 * <p>This class is for advanced usage and reflects the underlying API directly.
 */
@BetaApi
@Generated("by gapic-generator-java")
public abstract class CloudFilestoreManagerStub implements BackgroundResource {

  public OperationsStub getOperationsStub() {
    return null;
  }

  public com.google.api.gax.httpjson.longrunning.stub.OperationsStub getHttpJsonOperationsStub() {
    return null;
  }

  public UnaryCallable<ListInstancesRequest, ListInstancesPagedResponse>
      listInstancesPagedCallable() {
    throw new UnsupportedOperationException("Not implemented: listInstancesPagedCallable()");
  }

  public UnaryCallable<ListInstancesRequest, ListInstancesResponse> listInstancesCallable() {
    throw new UnsupportedOperationException("Not implemented: listInstancesCallable()");
  }

  public UnaryCallable<GetInstanceRequest, Instance> getInstanceCallable() {
    throw new UnsupportedOperationException("Not implemented: getInstanceCallable()");
  }

  public OperationCallable<CreateInstanceRequest, Instance, OperationMetadata>
      createInstanceOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: createInstanceOperationCallable()");
  }

  public UnaryCallable<CreateInstanceRequest, Operation> createInstanceCallable() {
    throw new UnsupportedOperationException("Not implemented: createInstanceCallable()");
  }

  public OperationCallable<UpdateInstanceRequest, Instance, OperationMetadata>
      updateInstanceOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: updateInstanceOperationCallable()");
  }

  public UnaryCallable<UpdateInstanceRequest, Operation> updateInstanceCallable() {
    throw new UnsupportedOperationException("Not implemented: updateInstanceCallable()");
  }

  public OperationCallable<RestoreInstanceRequest, Instance, OperationMetadata>
      restoreInstanceOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: restoreInstanceOperationCallable()");
  }

  public UnaryCallable<RestoreInstanceRequest, Operation> restoreInstanceCallable() {
    throw new UnsupportedOperationException("Not implemented: restoreInstanceCallable()");
  }

  public OperationCallable<RevertInstanceRequest, Instance, OperationMetadata>
      revertInstanceOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: revertInstanceOperationCallable()");
  }

  public UnaryCallable<RevertInstanceRequest, Operation> revertInstanceCallable() {
    throw new UnsupportedOperationException("Not implemented: revertInstanceCallable()");
  }

  public OperationCallable<DeleteInstanceRequest, Empty, OperationMetadata>
      deleteInstanceOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: deleteInstanceOperationCallable()");
  }

  public UnaryCallable<DeleteInstanceRequest, Operation> deleteInstanceCallable() {
    throw new UnsupportedOperationException("Not implemented: deleteInstanceCallable()");
  }

  public UnaryCallable<ListSnapshotsRequest, ListSnapshotsPagedResponse>
      listSnapshotsPagedCallable() {
    throw new UnsupportedOperationException("Not implemented: listSnapshotsPagedCallable()");
  }

  public UnaryCallable<ListSnapshotsRequest, ListSnapshotsResponse> listSnapshotsCallable() {
    throw new UnsupportedOperationException("Not implemented: listSnapshotsCallable()");
  }

  public UnaryCallable<GetSnapshotRequest, Snapshot> getSnapshotCallable() {
    throw new UnsupportedOperationException("Not implemented: getSnapshotCallable()");
  }

  public OperationCallable<CreateSnapshotRequest, Snapshot, OperationMetadata>
      createSnapshotOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: createSnapshotOperationCallable()");
  }

  public UnaryCallable<CreateSnapshotRequest, Operation> createSnapshotCallable() {
    throw new UnsupportedOperationException("Not implemented: createSnapshotCallable()");
  }

  public OperationCallable<DeleteSnapshotRequest, Empty, OperationMetadata>
      deleteSnapshotOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: deleteSnapshotOperationCallable()");
  }

  public UnaryCallable<DeleteSnapshotRequest, Operation> deleteSnapshotCallable() {
    throw new UnsupportedOperationException("Not implemented: deleteSnapshotCallable()");
  }

  public OperationCallable<UpdateSnapshotRequest, Snapshot, OperationMetadata>
      updateSnapshotOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: updateSnapshotOperationCallable()");
  }

  public UnaryCallable<UpdateSnapshotRequest, Operation> updateSnapshotCallable() {
    throw new UnsupportedOperationException("Not implemented: updateSnapshotCallable()");
  }

  public UnaryCallable<ListBackupsRequest, ListBackupsPagedResponse> listBackupsPagedCallable() {
    throw new UnsupportedOperationException("Not implemented: listBackupsPagedCallable()");
  }

  public UnaryCallable<ListBackupsRequest, ListBackupsResponse> listBackupsCallable() {
    throw new UnsupportedOperationException("Not implemented: listBackupsCallable()");
  }

  public UnaryCallable<GetBackupRequest, Backup> getBackupCallable() {
    throw new UnsupportedOperationException("Not implemented: getBackupCallable()");
  }

  public OperationCallable<CreateBackupRequest, Backup, OperationMetadata>
      createBackupOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: createBackupOperationCallable()");
  }

  public UnaryCallable<CreateBackupRequest, Operation> createBackupCallable() {
    throw new UnsupportedOperationException("Not implemented: createBackupCallable()");
  }

  public OperationCallable<DeleteBackupRequest, Empty, OperationMetadata>
      deleteBackupOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: deleteBackupOperationCallable()");
  }

  public UnaryCallable<DeleteBackupRequest, Operation> deleteBackupCallable() {
    throw new UnsupportedOperationException("Not implemented: deleteBackupCallable()");
  }

  public OperationCallable<UpdateBackupRequest, Backup, OperationMetadata>
      updateBackupOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: updateBackupOperationCallable()");
  }

  public UnaryCallable<UpdateBackupRequest, Operation> updateBackupCallable() {
    throw new UnsupportedOperationException("Not implemented: updateBackupCallable()");
  }

  public UnaryCallable<ListSharesRequest, ListSharesPagedResponse> listSharesPagedCallable() {
    throw new UnsupportedOperationException("Not implemented: listSharesPagedCallable()");
  }

  public UnaryCallable<ListSharesRequest, ListSharesResponse> listSharesCallable() {
    throw new UnsupportedOperationException("Not implemented: listSharesCallable()");
  }

  public UnaryCallable<GetShareRequest, Share> getShareCallable() {
    throw new UnsupportedOperationException("Not implemented: getShareCallable()");
  }

  public OperationCallable<CreateShareRequest, Share, OperationMetadata>
      createShareOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: createShareOperationCallable()");
  }

  public UnaryCallable<CreateShareRequest, Operation> createShareCallable() {
    throw new UnsupportedOperationException("Not implemented: createShareCallable()");
  }

  public OperationCallable<DeleteShareRequest, Empty, OperationMetadata>
      deleteShareOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: deleteShareOperationCallable()");
  }

  public UnaryCallable<DeleteShareRequest, Operation> deleteShareCallable() {
    throw new UnsupportedOperationException("Not implemented: deleteShareCallable()");
  }

  public OperationCallable<UpdateShareRequest, Share, OperationMetadata>
      updateShareOperationCallable() {
    throw new UnsupportedOperationException("Not implemented: updateShareOperationCallable()");
  }

  public UnaryCallable<UpdateShareRequest, Operation> updateShareCallable() {
    throw new UnsupportedOperationException("Not implemented: updateShareCallable()");
  }

  @Override
  public abstract void close();
}
