/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.iceberg.sink;

import org.apache.inlong.sort.iceberg.sink.multiple.IcebergProcessOperator;
import org.apache.inlong.sort.iceberg.sink.multiple.IcebergSingleFileCommiter;
import org.apache.inlong.sort.iceberg.sink.util.SimpleDataUtil;
import org.apache.inlong.sort.iceberg.sink.util.TestTableLoader;
import org.apache.inlong.sort.iceberg.sink.util.TestTables;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.JobID;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.runtime.checkpoint.OperatorSubtaskState;
import org.apache.flink.runtime.jobgraph.OperatorID;
import org.apache.flink.runtime.operators.testutils.MockEnvironment;
import org.apache.flink.runtime.operators.testutils.MockEnvironmentBuilder;
import org.apache.flink.runtime.operators.testutils.MockInputSplitProvider;
import org.apache.flink.streaming.api.operators.AbstractStreamOperatorFactory;
import org.apache.flink.streaming.api.operators.OneInputStreamOperatorFactory;
import org.apache.flink.streaming.api.operators.StreamOperator;
import org.apache.flink.streaming.api.operators.StreamOperatorParameters;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.actions.ActionsProvider;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.flink.sink.FlinkAppenderFactory;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.util.Pair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.iceberg.TableProperties.DEFAULT_FILE_FORMAT;
import static org.apache.inlong.sort.iceberg.sink.ManifestOutputFileFactory.FLINK_MANIFEST_LOCATION;

public class TestRollbackAndRecover {

    public static final String DATABASE = "default";
    public static final String TABLE = "t";
    private static final org.apache.hadoop.conf.Configuration CONF = new org.apache.hadoop.conf.Configuration();

    private Table table;
    private TableLoader tableLoader;
    private final FileFormat format = FileFormat.fromString("avro");
    private File flinkManifestFolder;
    private File metadataDir;
    private File tableDir;

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Rule
    public final HadoopCatalogResource catalogResource = new HadoopCatalogResource(TEMPORARY_FOLDER, DATABASE, TABLE);

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void before() throws IOException {
        flinkManifestFolder = temp.newFolder();

        this.tableDir = temp.newFolder();
        this.metadataDir = new File(tableDir, "metadata");
        Assert.assertTrue(tableDir.delete());

        // Construct the iceberg table.
        table = TestTables.create(tableDir, "test", SimpleDataUtil.SCHEMA, PartitionSpec.unpartitioned(), 2);

        table
                .updateProperties()
                .set(DEFAULT_FILE_FORMAT, format.name())
                .set(FLINK_MANIFEST_LOCATION, flinkManifestFolder.getAbsolutePath())
                .set("flink.max-continuous-empty-commits", "1")
                .commit();
    }

    @After
    public void after() {
        TestTables.clearTables();
    }

    // case1: Commit normally, and then submit multiple times. At this time, reset to a previous chk, \
    // and the snapshot corresponding to the chk has not expired
    @Test
    public void testRollbackToSnapshotWithRestoreCheckpointId() throws Exception {
        long timestamp = 0;
        long checkpoint = 10;
        OperatorSubtaskState initCheckpoint;
        JobID jobID = new JobID();
        ImmutableList firstResult;

        try (OneInputStreamOperatorTestHarness<WriteResult, Void> op = createStreamSink(jobID)) {
            FileAppenderFactory<RowData> appenderFactory = createDeletableAppenderFactory();

            op.setup();
            op.open();

            RowData insert1 = SimpleDataUtil.createInsert(1, "aaa");
            RowData insert2 = SimpleDataUtil.createInsert(2, "bbb");
            RowData delete3 = SimpleDataUtil.createDelete(3, "ccc");
            firstResult = ImmutableList.of(insert1, insert2);
            DataFile dataFile1 = writeDataFile("data-file-1", ImmutableList.of(insert1, insert2));
            DeleteFile deleteFile1 = writeEqDeleteFile(appenderFactory, "delete-file-1", ImmutableList.of(delete3));
            op.processElement(
                    WriteResult.builder().addDataFiles(dataFile1).addDeleteFiles(deleteFile1).build(),
                    ++timestamp);

            // The 1th snapshotState.
            initCheckpoint = op.snapshot(checkpoint, ++timestamp);
            op.notifyOfCompletedCheckpoint(checkpoint);

            RowData insert4 = SimpleDataUtil.createInsert(4, "ddd");
            RowData delete2 = SimpleDataUtil.createDelete(2, "bbb");
            DataFile dataFile2 = writeDataFile("data-file-2", ImmutableList.of(insert4));
            DeleteFile deleteFile2 = writeEqDeleteFile(appenderFactory, "delete-file-2", ImmutableList.of(delete2));
            op.processElement(
                    WriteResult.builder().addDataFiles(dataFile2).addDeleteFiles(deleteFile2).build(),
                    ++timestamp);

            // The 2nd snapshotState.
            op.snapshot(++checkpoint, ++timestamp);
            op.notifyOfCompletedCheckpoint(checkpoint);
            assertMaxCommittedCheckpointId(jobID, checkpoint);
            SimpleDataUtil.assertTableRows(table, ImmutableList.of(insert1, insert4));
        }

        try (OneInputStreamOperatorTestHarness<WriteResult, Void> op = createStreamSink(jobID)) {
            // init state from begin checkpoint
            op.setup();
            op.initializeState(initCheckpoint);
            op.open();

            // test if rollback success
            // check snapshot information
            assertMaxCommittedCheckpointId(jobID, checkpoint - 1);
            table.refresh();
            SimpleDataUtil.assertTableRows(table, firstResult);
        }
    }

    // case2: Commit normally, and then submit multiple times. At this time, reset to a previous chk, but the snapshot
    // corresponding to the chk has been expired
    @Test
    public void testFallbackToRecoverWithUnCompletedNotification() throws Exception {
        long timestamp = 0;
        long checkpoint = 10;
        OperatorSubtaskState initCheckpoint;
        long initTimestamp;
        JobID jobID = new JobID();
        ImmutableList result;

        try (OneInputStreamOperatorTestHarness<WriteResult, Void> op = createStreamSink(jobID)) {
            FileAppenderFactory<RowData> appenderFactory = createDeletableAppenderFactory();

            op.setup();
            op.open();

            RowData insert1 = SimpleDataUtil.createInsert(1, "aaa");
            RowData insert2 = SimpleDataUtil.createInsert(2, "bbb");
            RowData delete3 = SimpleDataUtil.createDelete(3, "ccc");
            DataFile dataFile1 = writeDataFile("data-file-1", ImmutableList.of(insert1, insert2));
            DeleteFile deleteFile1 = writeEqDeleteFile(appenderFactory, "delete-file-1", ImmutableList.of(delete3));
            op.processElement(
                    WriteResult.builder().addDataFiles(dataFile1).addDeleteFiles(deleteFile1).build(),
                    ++timestamp);

            // The 1th snapshotState.
            initCheckpoint = op.snapshot(checkpoint, ++timestamp);
            op.notifyOfCompletedCheckpoint(checkpoint);
            initTimestamp = System.currentTimeMillis();

            RowData insert4 = SimpleDataUtil.createInsert(4, "ddd");
            RowData delete2 = SimpleDataUtil.createDelete(2, "bbb");
            DataFile dataFile2 = writeDataFile("data-file-2", ImmutableList.of(insert4));
            DeleteFile deleteFile2 = writeEqDeleteFile(appenderFactory, "delete-file-2", ImmutableList.of(delete2));
            op.processElement(
                    WriteResult.builder().addDataFiles(dataFile2).addDeleteFiles(deleteFile2).build(),
                    ++timestamp);

            // The 2nd snapshotState.
            op.snapshot(++checkpoint, ++timestamp);
            op.notifyOfCompletedCheckpoint(checkpoint);
            assertMaxCommittedCheckpointId(jobID, checkpoint);
            result = ImmutableList.of(insert1, insert4);
            SimpleDataUtil.assertTableRows(table, result);

        }

        table.expireSnapshots().expireOlderThan(initTimestamp).commit();

        try (OneInputStreamOperatorTestHarness<WriteResult, Void> op = createStreamSink(jobID)) {
            // init state from begin checkpoint
            op.setup();
            op.initializeState(initCheckpoint);
            op.open();

            // check snapshot information
            assertMaxCommittedCheckpointId(jobID, checkpoint);
            SimpleDataUtil.assertTableRows(table, result); // same as before
        }
    }

    // case3: Commit normally, and then commit multiple times. At this time, reset to a previous chk.
    // There are multiple uncommitted manifests in this chk. These manifests are really not committed
    // successfully
    @Test
    public void testRecoveryFromSnapshotWithoutCompletedNotification() throws Exception {
        long timestamp = 0;
        long checkpoint = 10;
        long restoreCheckpoint = 0;
        OperatorSubtaskState restoreCheckpointState;
        long initTimestamp = 0;
        JobID jobID = new JobID();
        List result;

        // first commmit 3 snapshot
        try (OneInputStreamOperatorTestHarness<WriteResult, Void> op = createStreamSink(jobID)) {
            FileAppenderFactory<RowData> appenderFactory = createDeletableAppenderFactory();

            op.setup();
            op.open();

            // write 1nd data
            RowData insert1 = SimpleDataUtil.createInsert(1, "aaa");
            RowData insert2 = SimpleDataUtil.createInsert(2, "bbb");
            RowData delete3 = SimpleDataUtil.createDelete(3, "ccc");
            DataFile dataFile1 = writeDataFile("data-file-1", ImmutableList.of(insert1, insert2));
            DeleteFile deleteFile1 = writeEqDeleteFile(appenderFactory, "delete-file-1", ImmutableList.of(delete3));
            op.processElement(
                    WriteResult.builder().addDataFiles(dataFile1).addDeleteFiles(deleteFile1).build(),
                    ++timestamp);

            // The 1th snapshotState.
            restoreCheckpoint = checkpoint;
            restoreCheckpointState = op.snapshot(checkpoint, ++timestamp);
            op.notifyOfCompletedCheckpoint(checkpoint);

            // write 2nd data
            RowData insert4 = SimpleDataUtil.createInsert(4, "ddd");
            RowData delete2 = SimpleDataUtil.createDelete(2, "bbb");
            DataFile dataFile2 = writeDataFile("data-file-2", ImmutableList.of(insert4));
            DeleteFile deleteFile2 = writeEqDeleteFile(appenderFactory, "delete-file-2", ImmutableList.of(delete2));
            op.processElement(
                    WriteResult.builder().addDataFiles(dataFile2).addDeleteFiles(deleteFile2).build(),
                    ++timestamp);

            // The 2nd snapshotState.
            op.snapshot(++checkpoint, ++timestamp);
            op.notifyOfCompletedCheckpoint(checkpoint);
            assertMaxCommittedCheckpointId(jobID, checkpoint);
            initTimestamp = System.currentTimeMillis();

            // write 3nd data
            RowData insert5 = SimpleDataUtil.createInsert(5, "eee");
            RowData insert6 = SimpleDataUtil.createInsert(6, "fff");
            DataFile dataFile3 = writeDataFile("data-file-3", ImmutableList.of(insert5, insert6));
            op.processElement(WriteResult.builder().addDataFiles(dataFile3).build(), ++timestamp);

            // The 3nd snapshotState.
            op.snapshot(++checkpoint, ++timestamp);
            op.notifyOfCompletedCheckpoint(checkpoint);
            assertMaxCommittedCheckpointId(jobID, checkpoint);
            result = ImmutableList.of(insert1, insert4, insert5, insert6);
        }

        // only retain 3th snapshot
        table.expireSnapshots().expireOlderThan(initTimestamp).commit();
        checkpoint = restoreCheckpoint;
        try (OneInputStreamOperatorTestHarness<WriteResult, Void> op = createStreamSink(jobID)) {
            FileAppenderFactory<RowData> appenderFactory = createDeletableAppenderFactory();
            // init state from begin checkpoint
            op.setup();
            op.initializeState(restoreCheckpointState);
            op.open();

            // write data
            RowData insert4 = SimpleDataUtil.createInsert(4, "ddd");
            RowData delete2 = SimpleDataUtil.createDelete(2, "bbb");
            RowData insert5 = SimpleDataUtil.createInsert(5, "eee");
            RowData insert6 = SimpleDataUtil.createInsert(6, "fff");
            RowData delete1 = SimpleDataUtil.createDelete(1, "aaa"); // new delete
            RowData insert7 = SimpleDataUtil.createInsert(7, "ggg"); // new insert
            DataFile dataFile4 = writeDataFile("data-file-4", ImmutableList.of(insert4, insert5, insert6, insert7));
            DeleteFile deleteFile4 =
                    writeEqDeleteFile(appenderFactory, "delete-file-4", ImmutableList.of(delete2, delete1));
            op.processElement(WriteResult.builder().addDataFiles(dataFile4).addDeleteFiles(deleteFile4).build(),
                    initTimestamp);
            // snapshotState
            restoreCheckpointState = op.snapshot(++checkpoint, ++timestamp);
            op.notifyOfCompletedCheckpoint(checkpoint);
            SimpleDataUtil.assertTableRows(table, result);
            result = ImmutableList.of(insert4, insert5, insert6, insert7);
        }

        try (OneInputStreamOperatorTestHarness<WriteResult, Void> op = createStreamSink(jobID)) {
            op.setup();
            op.initializeState(restoreCheckpointState);
            op.open();

            // check snapshot information
            assertMaxCommittedCheckpointId(jobID, checkpoint);
            SimpleDataUtil.assertTableRows(table, result);
        }
    }

    public IcebergProcessOperator<WriteResult, Void> buildCommitter(
            boolean overwritemode,
            ActionsProvider actionsProvider,
            ReadableConfig tableOptions) {
        IcebergSingleFileCommiter commiter = new IcebergSingleFileCommiter(
                TableIdentifier.of(table.name()),
                tableLoader,
                overwritemode,
                actionsProvider,
                tableOptions);
        return new IcebergProcessOperator(commiter);
    }

    private OneInputStreamOperatorTestHarness<WriteResult, Void> createStreamSink(JobID jobID)
            throws Exception {
        TestOperatorFactory factory = TestOperatorFactory.of(table.location(), TableIdentifier.of(table.name()));
        return new OneInputStreamOperatorTestHarness<>(factory, createEnvironment(jobID));
    }

    private FileAppenderFactory<RowData> createDeletableAppenderFactory() {
        int[] equalityFieldIds = new int[]{
                table.schema().findField("id").fieldId(), table.schema().findField("data").fieldId()
        };
        return new FlinkAppenderFactory(
                table.schema(),
                FlinkSchemaUtil.convert(table.schema()),
                table.properties(),
                table.spec(),
                equalityFieldIds,
                table.schema(),
                null);
    }

    private DataFile writeDataFile(String filename, List<RowData> rows) throws IOException {
        return SimpleDataUtil.writeFile(
                table,
                table.schema(),
                table.spec(),
                CONF,
                table.location(),
                format.addExtension(filename),
                rows);
    }

    private DataFile writeDataFile(
            String filename, List<RowData> rows, PartitionSpec spec, StructLike partition)
            throws IOException {
        return SimpleDataUtil.writeFile(
                table,
                table.schema(),
                spec,
                CONF,
                table.location(),
                format.addExtension(filename),
                rows,
                partition);
    }

    private DeleteFile writeEqDeleteFile(
            FileAppenderFactory<RowData> appenderFactory, String filename, List<RowData> deletes)
            throws IOException {
        return SimpleDataUtil.writeEqDeleteFile(table, format, filename, appenderFactory, deletes);
    }

    private DeleteFile writePosDeleteFile(
            FileAppenderFactory<RowData> appenderFactory,
            String filename,
            List<Pair<CharSequence, Long>> positions)
            throws IOException {
        return SimpleDataUtil.writePosDeleteFile(table, format, filename, appenderFactory, positions);
    }

    private void assertSnapshotSize(int expectedSnapshotSize) {
        table.refresh();
        Assert.assertEquals(expectedSnapshotSize, Lists.newArrayList(table.snapshots()).size());
    }

    private void assertMaxCommittedCheckpointId(JobID jobID, OperatorID operatorID, long expectedId) {
        table.refresh();
        long actualId = IcebergSingleFileCommiter.getMaxCommittedCheckpointId(table, jobID.toString());
        Assert.assertEquals(expectedId, actualId);
    }

    private List<Path> assertFlinkManifests(int expectedCount) throws IOException {
        List<Path> manifests = Files.list(flinkManifestFolder.toPath())
                .filter(p -> !p.toString().endsWith(".crc"))
                .collect(Collectors.toList());
        Assert.assertEquals(
                String.format("Expected %s flink manifests, but the list is: %s", expectedCount, manifests),
                expectedCount,
                manifests.size());
        return manifests;
    }

    private void assertMaxCommittedCheckpointId(JobID jobID, long expectedId) {
        table.refresh();
        long actualId = IcebergSingleFileCommiter.getMaxCommittedCheckpointId(table, jobID.toString());
        Assert.assertEquals(expectedId, actualId);
    }

    private static MockEnvironment createEnvironment(JobID jobID) {
        return new MockEnvironmentBuilder()
                .setTaskName("test task")
                .setManagedMemorySize(32 * 1024)
                .setInputSplitProvider(new MockInputSplitProvider())
                .setBufferSize(256)
                .setTaskConfiguration(new org.apache.flink.configuration.Configuration())
                .setExecutionConfig(new ExecutionConfig())
                .setMaxParallelism(16)
                .setJobID(jobID)
                .build();
    }

    private static class TestOperatorFactory extends AbstractStreamOperatorFactory<Void>
            implements
                OneInputStreamOperatorFactory<WriteResult, Void> {

        private final String tablePath;
        private final TableIdentifier tableIdentifier;

        private TestOperatorFactory(String tablePath, TableIdentifier tableIdentifier) {
            this.tablePath = tablePath;
            this.tableIdentifier = tableIdentifier;
        }

        private static TestOperatorFactory of(String tablePath, TableIdentifier tableIdentifier) {
            return new TestOperatorFactory(tablePath, tableIdentifier);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends StreamOperator<Void>> T createStreamOperator(
                StreamOperatorParameters<Void> param) {
            IcebergSingleFileCommiter commiter = new IcebergSingleFileCommiter(
                    tableIdentifier,
                    new TestTableLoader(tablePath),
                    false,
                    null,
                    new Configuration());
            IcebergProcessOperator<?, Void> operator = new IcebergProcessOperator(commiter);
            operator.setup(param.getContainingTask(), param.getStreamConfig(), param.getOutput());
            return (T) operator;
        }

        @Override
        public Class<? extends StreamOperator> getStreamOperatorClass(ClassLoader classLoader) {
            return IcebergProcessOperator.class;
        }
    }
}
