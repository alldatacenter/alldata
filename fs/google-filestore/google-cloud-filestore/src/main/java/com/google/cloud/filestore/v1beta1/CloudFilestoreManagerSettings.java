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

package com.google.cloud.filestore.v1beta1;

import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListBackupsPagedResponse;
import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListInstancesPagedResponse;
import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListSharesPagedResponse;
import static com.google.cloud.filestore.v1beta1.CloudFilestoreManagerClient.ListSnapshotsPagedResponse;

import com.google.api.core.ApiFunction;
import com.google.api.core.BetaApi;
import com.google.api.gax.core.GoogleCredentialsProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.httpjson.InstantiatingHttpJsonChannelProvider;
import com.google.api.gax.rpc.ApiClientHeaderProvider;
import com.google.api.gax.rpc.ClientContext;
import com.google.api.gax.rpc.ClientSettings;
import com.google.api.gax.rpc.OperationCallSettings;
import com.google.api.gax.rpc.PagedCallSettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.api.gax.rpc.UnaryCallSettings;
import com.google.cloud.common.OperationMetadata;
import com.google.cloud.filestore.v1beta1.stub.CloudFilestoreManagerStubSettings;
import com.google.longrunning.Operation;
import com.google.protobuf.Empty;
import java.io.IOException;
import java.util.List;
import javax.annotation.Generated;

// AUTO-GENERATED DOCUMENTATION AND CLASS.
/**
 * Settings class to configure an instance of {@link CloudFilestoreManagerClient}.
 *
 * <p>The default instance has everything set to sensible defaults:
 *
 * <ul>
 *   <li>The default service address (file.googleapis.com) and default port (443) are used.
 *   <li>Credentials are acquired automatically through Application Default Credentials.
 *   <li>Retries are configured for idempotent methods but not for non-idempotent methods.
 * </ul>
 *
 * <p>The builder of this class is recursive, so contained classes are themselves builders. When
 * build() is called, the tree of builders is called to create the complete settings object.
 *
 * <p>For example, to set the total timeout of getInstance to 30 seconds:
 *
 * <pre>{@code
 * // This snippet has been automatically generated and should be regarded as a code template only.
 * // It will require modifications to work:
 * // - It may require correct/in-range values for request initialization.
 * // - It may require specifying regional endpoints when creating the service client as shown in
 * // https://cloud.google.com/java/docs/setup#configure_endpoints_for_the_client_library
 * CloudFilestoreManagerSettings.Builder cloudFilestoreManagerSettingsBuilder =
 *     CloudFilestoreManagerSettings.newBuilder();
 * cloudFilestoreManagerSettingsBuilder
 *     .getInstanceSettings()
 *     .setRetrySettings(
 *         cloudFilestoreManagerSettingsBuilder
 *             .getInstanceSettings()
 *             .getRetrySettings()
 *             .toBuilder()
 *             .setTotalTimeout(Duration.ofSeconds(30))
 *             .build());
 * CloudFilestoreManagerSettings cloudFilestoreManagerSettings =
 *     cloudFilestoreManagerSettingsBuilder.build();
 * }</pre>
 */
@BetaApi
@Generated("by gapic-generator-java")
public class CloudFilestoreManagerSettings extends ClientSettings<CloudFilestoreManagerSettings> {

  /** Returns the object with the settings used for calls to listInstances. */
  public PagedCallSettings<ListInstancesRequest, ListInstancesResponse, ListInstancesPagedResponse>
      listInstancesSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).listInstancesSettings();
  }

  /** Returns the object with the settings used for calls to getInstance. */
  public UnaryCallSettings<GetInstanceRequest, Instance> getInstanceSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).getInstanceSettings();
  }

  /** Returns the object with the settings used for calls to createInstance. */
  public UnaryCallSettings<CreateInstanceRequest, Operation> createInstanceSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).createInstanceSettings();
  }

  /** Returns the object with the settings used for calls to createInstance. */
  public OperationCallSettings<CreateInstanceRequest, Instance, OperationMetadata>
      createInstanceOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings())
        .createInstanceOperationSettings();
  }

  /** Returns the object with the settings used for calls to updateInstance. */
  public UnaryCallSettings<UpdateInstanceRequest, Operation> updateInstanceSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).updateInstanceSettings();
  }

  /** Returns the object with the settings used for calls to updateInstance. */
  public OperationCallSettings<UpdateInstanceRequest, Instance, OperationMetadata>
      updateInstanceOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings())
        .updateInstanceOperationSettings();
  }

  /** Returns the object with the settings used for calls to restoreInstance. */
  public UnaryCallSettings<RestoreInstanceRequest, Operation> restoreInstanceSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).restoreInstanceSettings();
  }

  /** Returns the object with the settings used for calls to restoreInstance. */
  public OperationCallSettings<RestoreInstanceRequest, Instance, OperationMetadata>
      restoreInstanceOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings())
        .restoreInstanceOperationSettings();
  }

  /** Returns the object with the settings used for calls to revertInstance. */
  public UnaryCallSettings<RevertInstanceRequest, Operation> revertInstanceSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).revertInstanceSettings();
  }

  /** Returns the object with the settings used for calls to revertInstance. */
  public OperationCallSettings<RevertInstanceRequest, Instance, OperationMetadata>
      revertInstanceOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings())
        .revertInstanceOperationSettings();
  }

  /** Returns the object with the settings used for calls to deleteInstance. */
  public UnaryCallSettings<DeleteInstanceRequest, Operation> deleteInstanceSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).deleteInstanceSettings();
  }

  /** Returns the object with the settings used for calls to deleteInstance. */
  public OperationCallSettings<DeleteInstanceRequest, Empty, OperationMetadata>
      deleteInstanceOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings())
        .deleteInstanceOperationSettings();
  }

  /** Returns the object with the settings used for calls to listSnapshots. */
  public PagedCallSettings<ListSnapshotsRequest, ListSnapshotsResponse, ListSnapshotsPagedResponse>
      listSnapshotsSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).listSnapshotsSettings();
  }

  /** Returns the object with the settings used for calls to getSnapshot. */
  public UnaryCallSettings<GetSnapshotRequest, Snapshot> getSnapshotSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).getSnapshotSettings();
  }

  /** Returns the object with the settings used for calls to createSnapshot. */
  public UnaryCallSettings<CreateSnapshotRequest, Operation> createSnapshotSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).createSnapshotSettings();
  }

  /** Returns the object with the settings used for calls to createSnapshot. */
  public OperationCallSettings<CreateSnapshotRequest, Snapshot, OperationMetadata>
      createSnapshotOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings())
        .createSnapshotOperationSettings();
  }

  /** Returns the object with the settings used for calls to deleteSnapshot. */
  public UnaryCallSettings<DeleteSnapshotRequest, Operation> deleteSnapshotSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).deleteSnapshotSettings();
  }

  /** Returns the object with the settings used for calls to deleteSnapshot. */
  public OperationCallSettings<DeleteSnapshotRequest, Empty, OperationMetadata>
      deleteSnapshotOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings())
        .deleteSnapshotOperationSettings();
  }

  /** Returns the object with the settings used for calls to updateSnapshot. */
  public UnaryCallSettings<UpdateSnapshotRequest, Operation> updateSnapshotSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).updateSnapshotSettings();
  }

  /** Returns the object with the settings used for calls to updateSnapshot. */
  public OperationCallSettings<UpdateSnapshotRequest, Snapshot, OperationMetadata>
      updateSnapshotOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings())
        .updateSnapshotOperationSettings();
  }

  /** Returns the object with the settings used for calls to listBackups. */
  public PagedCallSettings<ListBackupsRequest, ListBackupsResponse, ListBackupsPagedResponse>
      listBackupsSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).listBackupsSettings();
  }

  /** Returns the object with the settings used for calls to getBackup. */
  public UnaryCallSettings<GetBackupRequest, Backup> getBackupSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).getBackupSettings();
  }

  /** Returns the object with the settings used for calls to createBackup. */
  public UnaryCallSettings<CreateBackupRequest, Operation> createBackupSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).createBackupSettings();
  }

  /** Returns the object with the settings used for calls to createBackup. */
  public OperationCallSettings<CreateBackupRequest, Backup, OperationMetadata>
      createBackupOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).createBackupOperationSettings();
  }

  /** Returns the object with the settings used for calls to deleteBackup. */
  public UnaryCallSettings<DeleteBackupRequest, Operation> deleteBackupSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).deleteBackupSettings();
  }

  /** Returns the object with the settings used for calls to deleteBackup. */
  public OperationCallSettings<DeleteBackupRequest, Empty, OperationMetadata>
      deleteBackupOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).deleteBackupOperationSettings();
  }

  /** Returns the object with the settings used for calls to updateBackup. */
  public UnaryCallSettings<UpdateBackupRequest, Operation> updateBackupSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).updateBackupSettings();
  }

  /** Returns the object with the settings used for calls to updateBackup. */
  public OperationCallSettings<UpdateBackupRequest, Backup, OperationMetadata>
      updateBackupOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).updateBackupOperationSettings();
  }

  /** Returns the object with the settings used for calls to listShares. */
  public PagedCallSettings<ListSharesRequest, ListSharesResponse, ListSharesPagedResponse>
      listSharesSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).listSharesSettings();
  }

  /** Returns the object with the settings used for calls to getShare. */
  public UnaryCallSettings<GetShareRequest, Share> getShareSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).getShareSettings();
  }

  /** Returns the object with the settings used for calls to createShare. */
  public UnaryCallSettings<CreateShareRequest, Operation> createShareSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).createShareSettings();
  }

  /** Returns the object with the settings used for calls to createShare. */
  public OperationCallSettings<CreateShareRequest, Share, OperationMetadata>
      createShareOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).createShareOperationSettings();
  }

  /** Returns the object with the settings used for calls to deleteShare. */
  public UnaryCallSettings<DeleteShareRequest, Operation> deleteShareSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).deleteShareSettings();
  }

  /** Returns the object with the settings used for calls to deleteShare. */
  public OperationCallSettings<DeleteShareRequest, Empty, OperationMetadata>
      deleteShareOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).deleteShareOperationSettings();
  }

  /** Returns the object with the settings used for calls to updateShare. */
  public UnaryCallSettings<UpdateShareRequest, Operation> updateShareSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).updateShareSettings();
  }

  /** Returns the object with the settings used for calls to updateShare. */
  public OperationCallSettings<UpdateShareRequest, Share, OperationMetadata>
      updateShareOperationSettings() {
    return ((CloudFilestoreManagerStubSettings) getStubSettings()).updateShareOperationSettings();
  }

  public static final CloudFilestoreManagerSettings create(CloudFilestoreManagerStubSettings stub)
      throws IOException {
    return new CloudFilestoreManagerSettings.Builder(stub.toBuilder()).build();
  }

  /** Returns a builder for the default ExecutorProvider for this service. */
  public static InstantiatingExecutorProvider.Builder defaultExecutorProviderBuilder() {
    return CloudFilestoreManagerStubSettings.defaultExecutorProviderBuilder();
  }

  /** Returns the default service endpoint. */
  public static String getDefaultEndpoint() {
    return CloudFilestoreManagerStubSettings.getDefaultEndpoint();
  }

  /** Returns the default service scopes. */
  public static List<String> getDefaultServiceScopes() {
    return CloudFilestoreManagerStubSettings.getDefaultServiceScopes();
  }

  /** Returns a builder for the default credentials for this service. */
  public static GoogleCredentialsProvider.Builder defaultCredentialsProviderBuilder() {
    return CloudFilestoreManagerStubSettings.defaultCredentialsProviderBuilder();
  }

  /** Returns a builder for the default gRPC ChannelProvider for this service. */
  public static InstantiatingGrpcChannelProvider.Builder defaultGrpcTransportProviderBuilder() {
    return CloudFilestoreManagerStubSettings.defaultGrpcTransportProviderBuilder();
  }

  /** Returns a builder for the default REST ChannelProvider for this service. */
  @BetaApi
  public static InstantiatingHttpJsonChannelProvider.Builder
      defaultHttpJsonTransportProviderBuilder() {
    return CloudFilestoreManagerStubSettings.defaultHttpJsonTransportProviderBuilder();
  }

  public static TransportChannelProvider defaultTransportChannelProvider() {
    return CloudFilestoreManagerStubSettings.defaultTransportChannelProvider();
  }

  @BetaApi("The surface for customizing headers is not stable yet and may change in the future.")
  public static ApiClientHeaderProvider.Builder defaultApiClientHeaderProviderBuilder() {
    return CloudFilestoreManagerStubSettings.defaultApiClientHeaderProviderBuilder();
  }

  /** Returns a new gRPC builder for this class. */
  public static Builder newBuilder() {
    return Builder.createDefault();
  }

  /** Returns a new REST builder for this class. */
  @BetaApi
  public static Builder newHttpJsonBuilder() {
    return Builder.createHttpJsonDefault();
  }

  /** Returns a new builder for this class. */
  public static Builder newBuilder(ClientContext clientContext) {
    return new Builder(clientContext);
  }

  /** Returns a builder containing all the values of this settings class. */
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected CloudFilestoreManagerSettings(Builder settingsBuilder) throws IOException {
    super(settingsBuilder);
  }

  /** Builder for CloudFilestoreManagerSettings. */
  public static class Builder
      extends ClientSettings.Builder<CloudFilestoreManagerSettings, Builder> {

    protected Builder() throws IOException {
      this(((ClientContext) null));
    }

    protected Builder(ClientContext clientContext) {
      super(CloudFilestoreManagerStubSettings.newBuilder(clientContext));
    }

    protected Builder(CloudFilestoreManagerSettings settings) {
      super(settings.getStubSettings().toBuilder());
    }

    protected Builder(CloudFilestoreManagerStubSettings.Builder stubSettings) {
      super(stubSettings);
    }

    private static Builder createDefault() {
      return new Builder(CloudFilestoreManagerStubSettings.newBuilder());
    }

    @BetaApi
    private static Builder createHttpJsonDefault() {
      return new Builder(CloudFilestoreManagerStubSettings.newHttpJsonBuilder());
    }

    public CloudFilestoreManagerStubSettings.Builder getStubSettingsBuilder() {
      return ((CloudFilestoreManagerStubSettings.Builder) getStubSettings());
    }

    /**
     * Applies the given settings updater function to all of the unary API methods in this service.
     *
     * <p>Note: This method does not support applying settings to streaming methods.
     */
    public Builder applyToAllUnaryMethods(
        ApiFunction<UnaryCallSettings.Builder<?, ?>, Void> settingsUpdater) {
      super.applyToAllUnaryMethods(
          getStubSettingsBuilder().unaryMethodSettingsBuilders(), settingsUpdater);
      return this;
    }

    /** Returns the builder for the settings used for calls to listInstances. */
    public PagedCallSettings.Builder<
            ListInstancesRequest, ListInstancesResponse, ListInstancesPagedResponse>
        listInstancesSettings() {
      return getStubSettingsBuilder().listInstancesSettings();
    }

    /** Returns the builder for the settings used for calls to getInstance. */
    public UnaryCallSettings.Builder<GetInstanceRequest, Instance> getInstanceSettings() {
      return getStubSettingsBuilder().getInstanceSettings();
    }

    /** Returns the builder for the settings used for calls to createInstance. */
    public UnaryCallSettings.Builder<CreateInstanceRequest, Operation> createInstanceSettings() {
      return getStubSettingsBuilder().createInstanceSettings();
    }

    /** Returns the builder for the settings used for calls to createInstance. */
    public OperationCallSettings.Builder<CreateInstanceRequest, Instance, OperationMetadata>
        createInstanceOperationSettings() {
      return getStubSettingsBuilder().createInstanceOperationSettings();
    }

    /** Returns the builder for the settings used for calls to updateInstance. */
    public UnaryCallSettings.Builder<UpdateInstanceRequest, Operation> updateInstanceSettings() {
      return getStubSettingsBuilder().updateInstanceSettings();
    }

    /** Returns the builder for the settings used for calls to updateInstance. */
    public OperationCallSettings.Builder<UpdateInstanceRequest, Instance, OperationMetadata>
        updateInstanceOperationSettings() {
      return getStubSettingsBuilder().updateInstanceOperationSettings();
    }

    /** Returns the builder for the settings used for calls to restoreInstance. */
    public UnaryCallSettings.Builder<RestoreInstanceRequest, Operation> restoreInstanceSettings() {
      return getStubSettingsBuilder().restoreInstanceSettings();
    }

    /** Returns the builder for the settings used for calls to restoreInstance. */
    public OperationCallSettings.Builder<RestoreInstanceRequest, Instance, OperationMetadata>
        restoreInstanceOperationSettings() {
      return getStubSettingsBuilder().restoreInstanceOperationSettings();
    }

    /** Returns the builder for the settings used for calls to revertInstance. */
    public UnaryCallSettings.Builder<RevertInstanceRequest, Operation> revertInstanceSettings() {
      return getStubSettingsBuilder().revertInstanceSettings();
    }

    /** Returns the builder for the settings used for calls to revertInstance. */
    public OperationCallSettings.Builder<RevertInstanceRequest, Instance, OperationMetadata>
        revertInstanceOperationSettings() {
      return getStubSettingsBuilder().revertInstanceOperationSettings();
    }

    /** Returns the builder for the settings used for calls to deleteInstance. */
    public UnaryCallSettings.Builder<DeleteInstanceRequest, Operation> deleteInstanceSettings() {
      return getStubSettingsBuilder().deleteInstanceSettings();
    }

    /** Returns the builder for the settings used for calls to deleteInstance. */
    public OperationCallSettings.Builder<DeleteInstanceRequest, Empty, OperationMetadata>
        deleteInstanceOperationSettings() {
      return getStubSettingsBuilder().deleteInstanceOperationSettings();
    }

    /** Returns the builder for the settings used for calls to listSnapshots. */
    public PagedCallSettings.Builder<
            ListSnapshotsRequest, ListSnapshotsResponse, ListSnapshotsPagedResponse>
        listSnapshotsSettings() {
      return getStubSettingsBuilder().listSnapshotsSettings();
    }

    /** Returns the builder for the settings used for calls to getSnapshot. */
    public UnaryCallSettings.Builder<GetSnapshotRequest, Snapshot> getSnapshotSettings() {
      return getStubSettingsBuilder().getSnapshotSettings();
    }

    /** Returns the builder for the settings used for calls to createSnapshot. */
    public UnaryCallSettings.Builder<CreateSnapshotRequest, Operation> createSnapshotSettings() {
      return getStubSettingsBuilder().createSnapshotSettings();
    }

    /** Returns the builder for the settings used for calls to createSnapshot. */
    public OperationCallSettings.Builder<CreateSnapshotRequest, Snapshot, OperationMetadata>
        createSnapshotOperationSettings() {
      return getStubSettingsBuilder().createSnapshotOperationSettings();
    }

    /** Returns the builder for the settings used for calls to deleteSnapshot. */
    public UnaryCallSettings.Builder<DeleteSnapshotRequest, Operation> deleteSnapshotSettings() {
      return getStubSettingsBuilder().deleteSnapshotSettings();
    }

    /** Returns the builder for the settings used for calls to deleteSnapshot. */
    public OperationCallSettings.Builder<DeleteSnapshotRequest, Empty, OperationMetadata>
        deleteSnapshotOperationSettings() {
      return getStubSettingsBuilder().deleteSnapshotOperationSettings();
    }

    /** Returns the builder for the settings used for calls to updateSnapshot. */
    public UnaryCallSettings.Builder<UpdateSnapshotRequest, Operation> updateSnapshotSettings() {
      return getStubSettingsBuilder().updateSnapshotSettings();
    }

    /** Returns the builder for the settings used for calls to updateSnapshot. */
    public OperationCallSettings.Builder<UpdateSnapshotRequest, Snapshot, OperationMetadata>
        updateSnapshotOperationSettings() {
      return getStubSettingsBuilder().updateSnapshotOperationSettings();
    }

    /** Returns the builder for the settings used for calls to listBackups. */
    public PagedCallSettings.Builder<
            ListBackupsRequest, ListBackupsResponse, ListBackupsPagedResponse>
        listBackupsSettings() {
      return getStubSettingsBuilder().listBackupsSettings();
    }

    /** Returns the builder for the settings used for calls to getBackup. */
    public UnaryCallSettings.Builder<GetBackupRequest, Backup> getBackupSettings() {
      return getStubSettingsBuilder().getBackupSettings();
    }

    /** Returns the builder for the settings used for calls to createBackup. */
    public UnaryCallSettings.Builder<CreateBackupRequest, Operation> createBackupSettings() {
      return getStubSettingsBuilder().createBackupSettings();
    }

    /** Returns the builder for the settings used for calls to createBackup. */
    public OperationCallSettings.Builder<CreateBackupRequest, Backup, OperationMetadata>
        createBackupOperationSettings() {
      return getStubSettingsBuilder().createBackupOperationSettings();
    }

    /** Returns the builder for the settings used for calls to deleteBackup. */
    public UnaryCallSettings.Builder<DeleteBackupRequest, Operation> deleteBackupSettings() {
      return getStubSettingsBuilder().deleteBackupSettings();
    }

    /** Returns the builder for the settings used for calls to deleteBackup. */
    public OperationCallSettings.Builder<DeleteBackupRequest, Empty, OperationMetadata>
        deleteBackupOperationSettings() {
      return getStubSettingsBuilder().deleteBackupOperationSettings();
    }

    /** Returns the builder for the settings used for calls to updateBackup. */
    public UnaryCallSettings.Builder<UpdateBackupRequest, Operation> updateBackupSettings() {
      return getStubSettingsBuilder().updateBackupSettings();
    }

    /** Returns the builder for the settings used for calls to updateBackup. */
    public OperationCallSettings.Builder<UpdateBackupRequest, Backup, OperationMetadata>
        updateBackupOperationSettings() {
      return getStubSettingsBuilder().updateBackupOperationSettings();
    }

    /** Returns the builder for the settings used for calls to listShares. */
    public PagedCallSettings.Builder<ListSharesRequest, ListSharesResponse, ListSharesPagedResponse>
        listSharesSettings() {
      return getStubSettingsBuilder().listSharesSettings();
    }

    /** Returns the builder for the settings used for calls to getShare. */
    public UnaryCallSettings.Builder<GetShareRequest, Share> getShareSettings() {
      return getStubSettingsBuilder().getShareSettings();
    }

    /** Returns the builder for the settings used for calls to createShare. */
    public UnaryCallSettings.Builder<CreateShareRequest, Operation> createShareSettings() {
      return getStubSettingsBuilder().createShareSettings();
    }

    /** Returns the builder for the settings used for calls to createShare. */
    public OperationCallSettings.Builder<CreateShareRequest, Share, OperationMetadata>
        createShareOperationSettings() {
      return getStubSettingsBuilder().createShareOperationSettings();
    }

    /** Returns the builder for the settings used for calls to deleteShare. */
    public UnaryCallSettings.Builder<DeleteShareRequest, Operation> deleteShareSettings() {
      return getStubSettingsBuilder().deleteShareSettings();
    }

    /** Returns the builder for the settings used for calls to deleteShare. */
    public OperationCallSettings.Builder<DeleteShareRequest, Empty, OperationMetadata>
        deleteShareOperationSettings() {
      return getStubSettingsBuilder().deleteShareOperationSettings();
    }

    /** Returns the builder for the settings used for calls to updateShare. */
    public UnaryCallSettings.Builder<UpdateShareRequest, Operation> updateShareSettings() {
      return getStubSettingsBuilder().updateShareSettings();
    }

    /** Returns the builder for the settings used for calls to updateShare. */
    public OperationCallSettings.Builder<UpdateShareRequest, Share, OperationMetadata>
        updateShareOperationSettings() {
      return getStubSettingsBuilder().updateShareOperationSettings();
    }

    @Override
    public CloudFilestoreManagerSettings build() throws IOException {
      return new CloudFilestoreManagerSettings(this);
    }
  }
}
