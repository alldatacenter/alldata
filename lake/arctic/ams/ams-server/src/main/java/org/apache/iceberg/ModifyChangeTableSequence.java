package org.apache.iceberg;

import org.apache.iceberg.events.CreateSnapshotEvent;
import org.apache.iceberg.exceptions.RuntimeIOException;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModifyChangeTableSequence extends ArcticSnapshotProducer<ModifyTableSequence> implements
    ModifyTableSequence {
  private final String tableName;
  private final TableOperations ops;
  private final PartitionSpec spec;
  private final SnapshotSummary.Builder summaryBuilder = SnapshotSummary.builder();
  private final List<DataFile> newFiles = Lists.newArrayList();
  private final List<ManifestFile> appendManifests = Lists.newArrayList();
  private final List<ManifestFile> rewrittenAppendManifests = Lists.newArrayList();
  private ManifestFile newManifest = null;
  private boolean hasNewFiles = false;

  public ModifyChangeTableSequence(String tableName, TableOperations ops) {
    super(ops);
    this.tableName = tableName;
    this.ops = ops;
    this.spec = ops.current().spec();
  }

  @Override
  protected ModifyTableSequence self() {
    return this;
  }

  @Override
  public ModifyTableSequence set(String property, String value) {
    summaryBuilder.set(property, value);
    return this;
  }

  @Override
  protected String operation() {
    return DataOperations.APPEND;
  }

  @Override
  protected Map<String, String> summary() {
    summaryBuilder.setPartitionSummaryLimit(ops.current().propertyAsInt(
        TableProperties.WRITE_PARTITION_SUMMARY_LIMIT, TableProperties.WRITE_PARTITION_SUMMARY_LIMIT_DEFAULT));
    return summaryBuilder.build();
  }

  private ManifestFile copyManifest(ManifestFile manifest) {
    TableMetadata current = ops.current();
    InputFile toCopy = ops.io().newInputFile(manifest.path());
    OutputFile newManifestPath = newManifestOutput();
    return ManifestFiles.copyAppendManifest(
        current.formatVersion(), toCopy, current.specsById(), newManifestPath, snapshotId(), summaryBuilder);
  }

  @Override
  public List<ManifestFile> apply(TableMetadata base) {
    List<ManifestFile> newManifests = Lists.newArrayList();

    try {
      ManifestFile manifest = writeManifest();
      if (manifest != null) {
        newManifests.add(manifest);
      }
    } catch (IOException e) {
      throw new RuntimeIOException(e, "Failed to write manifest");
    }

    Iterable<ManifestFile> appendManifestsWithMetadata = Iterables.transform(
        Iterables.concat(appendManifests, rewrittenAppendManifests),
        manifest -> GenericManifestFile.copyOf(manifest).withSnapshotId(snapshotId()).build());
    Iterables.addAll(newManifests, appendManifestsWithMetadata);

    if (base.currentSnapshot() != null) {
      newManifests.addAll(base.currentSnapshot().allManifests());
    }

    return newManifests;
  }

  @Override
  public Object updateEvent() {
    long snapshotId = snapshotId();
    Snapshot snapshot = ops.current().snapshot(snapshotId);
    long sequenceNumber = snapshot.sequenceNumber();
    return new CreateSnapshotEvent(
        tableName,
        operation(),
        snapshotId,
        sequenceNumber,
        snapshot.summary());
  }

  @Override
  protected void cleanUncommitted(Set<ManifestFile> committed) {
    if (newManifest != null && !committed.contains(newManifest)) {
      deleteFile(newManifest.path());
    }

    // clean up only rewrittenAppendManifests as they are always owned by the table
    // don't clean up appendManifests as they are added to the manifest list and are not compacted
    for (ManifestFile manifest : rewrittenAppendManifests) {
      if (!committed.contains(manifest)) {
        deleteFile(manifest.path());
      }
    }
  }

  private ManifestFile writeManifest() throws IOException {
    if (hasNewFiles && newManifest != null) {
      deleteFile(newManifest.path());
      newManifest = null;
    }

    if (newManifest == null && newFiles.size() > 0) {
      ManifestWriter writer = newManifestWriter(spec);
      try {
        writer.addAll(newFiles);
      } finally {
        writer.close();
      }

      this.newManifest = writer.toManifestFile();
      hasNewFiles = false;
    }

    return newManifest;
  }
}
