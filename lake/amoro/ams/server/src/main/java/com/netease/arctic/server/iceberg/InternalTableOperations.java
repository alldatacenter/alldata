package com.netease.arctic.server.iceberg;

import com.netease.arctic.server.persistence.PersistentBase;
import com.netease.arctic.server.persistence.mapper.TableMetaMapper;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.utils.IcebergTableUtil;
import org.apache.iceberg.LocationProviders;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.TableOperations;
import org.apache.iceberg.exceptions.CommitFailedException;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.LocationProvider;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class InternalTableOperations extends PersistentBase implements TableOperations {

  private final ServerTableIdentifier identifier;

  private TableMetadata current;
  private final FileIO io;
  private com.netease.arctic.server.table.TableMetadata tableMetadata;


  public static InternalTableOperations buildForLoad(
      com.netease.arctic.server.table.TableMetadata tableMetadata,
      FileIO io
  ) {
    return new InternalTableOperations(
        tableMetadata.getTableIdentifier(),
        tableMetadata,
        io);
  }

  public InternalTableOperations(
      ServerTableIdentifier identifier,
      com.netease.arctic.server.table.TableMetadata tableMetadata,
      FileIO io
  ) {
    this.io = io;
    this.tableMetadata = tableMetadata;
    this.identifier = identifier;
  }


  @Override
  public TableMetadata current() {
    if (this.current == null) {
      this.refresh();
    }
    return this.current;
  }

  @Override
  public TableMetadata refresh() {
    if (this.tableMetadata == null) {
      this.tableMetadata = getAs(TableMetaMapper.class, mapper -> mapper.selectTableMetaById(this.identifier.getId()));
    }
    if (this.tableMetadata == null) {
      return null;
    }
    this.current = IcebergTableUtil.loadIcebergTableMetadata(io, this.tableMetadata);
    return this.current;
  }

  @Override
  public void commit(TableMetadata base, TableMetadata metadata) {
    Preconditions.checkArgument(base != null,
        "Invalid table metadata for create transaction, base is null");

    Preconditions.checkArgument(metadata != null,
        "Invalid table metadata for create transaction, new metadata is null");
    if (base != current()) {
      throw new CommitFailedException("Cannot commit: stale table metadata");
    }

    String newMetadataFileLocation = IcebergTableUtil.genNewMetadataFileLocation(base, metadata);

    try {
      IcebergTableUtil.commitTableInternal(
          tableMetadata, base, metadata, newMetadataFileLocation, io);
      com.netease.arctic.server.table.TableMetadata updatedMetadata = doCommit();
      IcebergTableUtil.checkCommitSuccess(updatedMetadata, newMetadataFileLocation);
    } catch (Exception e) {
      io.deleteFile(newMetadataFileLocation);
    } finally {
      this.tableMetadata = null;
    }
    refresh();
  }

  @Override
  public FileIO io() {
    return this.io;
  }

  @Override
  public String metadataFileLocation(String fileName) {
    return IcebergTableUtil.genMetadataFileLocation(current(), fileName);
  }


  @Override
  public LocationProvider locationProvider() {
    return LocationProviders.locationsFor(current().location(), current().properties());
  }

  @Override
  public TableOperations temp(TableMetadata uncommittedMetadata) {
    return TableOperations.super.temp(uncommittedMetadata);
  }

  private com.netease.arctic.server.table.TableMetadata doCommit() {
    ServerTableIdentifier tableIdentifier = tableMetadata.getTableIdentifier();
    AtomicInteger effectRows = new AtomicInteger();
    AtomicReference<com.netease.arctic.server.table.TableMetadata> metadataRef = new AtomicReference<>();
    doAsTransaction(
        () -> {
          int effects = getAs(TableMetaMapper.class,
              mapper -> mapper.commitTableChange(tableIdentifier.getId(), tableMetadata));
          effectRows.set(effects);
        },
        () -> {
          com.netease.arctic.server.table.TableMetadata m = getAs(TableMetaMapper.class,
              mapper -> mapper.selectTableMetaById(tableIdentifier.getId()));
          metadataRef.set(m);
        }
    );
    if (effectRows.get() == 0) {
      throw new CommitFailedException("commit failed for version: " +
          tableMetadata.getMetaVersion() + " has been committed");
    }
    return metadataRef.get();
  }
}
