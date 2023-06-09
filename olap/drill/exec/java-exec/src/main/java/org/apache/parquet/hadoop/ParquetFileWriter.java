/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.parquet.hadoop;

import static org.apache.parquet.format.Util.writeFileCryptoMetaData;
import static org.apache.parquet.format.Util.writeFileMetaData;
import static org.apache.parquet.format.converter.ParquetMetadataConverter.MAX_STATS_SIZE;
import static org.apache.parquet.hadoop.ParquetWriter.DEFAULT_BLOCK_SIZE;
import static org.apache.parquet.hadoop.ParquetWriter.MAX_PADDING_SIZE_DEFAULT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.CRC32;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.parquet.Preconditions;
import org.apache.parquet.Version;
import org.apache.parquet.bytes.BytesInput;
import org.apache.parquet.bytes.BytesUtils;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.Encoding;
import org.apache.parquet.column.EncodingStats;
import org.apache.parquet.column.ParquetProperties;
import org.apache.parquet.column.page.DictionaryPage;
import org.apache.parquet.column.statistics.Statistics;
import org.apache.parquet.column.values.bloomfilter.BloomFilter;
import org.apache.parquet.crypto.AesCipher;
import org.apache.parquet.crypto.ColumnEncryptionProperties;
import org.apache.parquet.crypto.FileEncryptionProperties;
import org.apache.parquet.crypto.InternalColumnEncryptionSetup;
import org.apache.parquet.crypto.InternalFileEncryptor;
import org.apache.parquet.crypto.ModuleCipherFactory;
import org.apache.parquet.crypto.ModuleCipherFactory.ModuleType;
import org.apache.parquet.crypto.ParquetCryptoRuntimeException;
import org.apache.parquet.hadoop.ParquetOutputFormat.JobSummaryLevel;
import org.apache.parquet.hadoop.metadata.ColumnPath;
import org.apache.parquet.format.BlockCipher;
import org.apache.parquet.format.Util;
import org.apache.parquet.format.converter.ParquetMetadataConverter;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.metadata.StrictKeyValueMetadataMergeStrategy;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.hadoop.metadata.GlobalMetaData;
import org.apache.parquet.hadoop.metadata.KeyValueMetadataMergeStrategy;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.parquet.hadoop.util.HadoopStreams;
import org.apache.parquet.internal.column.columnindex.ColumnIndex;
import org.apache.parquet.internal.column.columnindex.ColumnIndexBuilder;
import org.apache.parquet.internal.column.columnindex.OffsetIndex;
import org.apache.parquet.internal.column.columnindex.OffsetIndexBuilder;
import org.apache.parquet.internal.hadoop.metadata.IndexReference;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.SeekableInputStream;
import org.apache.parquet.io.ParquetEncodingException;
import org.apache.parquet.io.PositionOutputStream;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal implementation of the Parquet file writer as a block container<br>
 * Note: this is temporary Drill-Parquet class needed to write empty parquet files. Details in
 * <a href="https://issues.apache.org/jira/browse/PARQUET-2026">PARQUET-2026</a> and
 * <a href="https://issues.apache.org/jira/browse/DRILL-7907">DRILL-7907</a>
 */
public class ParquetFileWriter {
    private static final Logger LOG = LoggerFactory.getLogger(ParquetFileWriter.class);

    private final ParquetMetadataConverter metadataConverter;

    public static final String PARQUET_METADATA_FILE = "_metadata";
    public static final String MAGIC_STR = "PAR1";
    public static final byte[] MAGIC = MAGIC_STR.getBytes(StandardCharsets.US_ASCII);
    public static final String EF_MAGIC_STR = "PARE";
    public static final byte[] EFMAGIC = EF_MAGIC_STR.getBytes(StandardCharsets.US_ASCII);
    public static final String PARQUET_COMMON_METADATA_FILE = "_common_metadata";
    public static final int CURRENT_VERSION = 1;

    // File creation modes
    public static enum Mode {
        CREATE,
        OVERWRITE
    }

    protected final PositionOutputStream out;

    private final MessageType schema;
    private final AlignmentStrategy alignment;
    private final int columnIndexTruncateLength;

    // file data
    private List<BlockMetaData> blocks = new ArrayList<BlockMetaData>();

    // The column/offset indexes per blocks per column chunks
    private final List<List<ColumnIndex>> columnIndexes = new ArrayList<>();
    private final List<List<OffsetIndex>> offsetIndexes = new ArrayList<>();

    // The Bloom filters
    private final List<Map<String, BloomFilter>> bloomFilters = new ArrayList<>();

    // The file encryptor
    private final InternalFileEncryptor fileEncryptor;

    // row group data
    private BlockMetaData currentBlock; // appended to by endColumn

    // The column/offset indexes for the actual block
    private List<ColumnIndex> currentColumnIndexes;
    private List<OffsetIndex> currentOffsetIndexes;

    // The Bloom filter for the actual block
    private Map<String, BloomFilter> currentBloomFilters;

    // row group data set at the start of a row group
    private long currentRecordCount; // set in startBlock

    // column chunk data accumulated as pages are written
    private EncodingStats.Builder encodingStatsBuilder;
    private Set<Encoding> currentEncodings;
    private long uncompressedLength;
    private long compressedLength;
    private Statistics currentStatistics; // accumulated in writePage(s)
    private ColumnIndexBuilder columnIndexBuilder;
    private OffsetIndexBuilder offsetIndexBuilder;

    // column chunk data set at the start of a column
    private CompressionCodecName currentChunkCodec; // set in startColumn
    private ColumnPath currentChunkPath;            // set in startColumn
    private PrimitiveType currentChunkType;         // set in startColumn
    private long currentChunkValueCount;            // set in startColumn
    private long currentChunkFirstDataPage;         // set in startColumn & page writes
    private long currentChunkDictionaryPageOffset;  // set in writeDictionaryPage

    // set when end is called
    private ParquetMetadata footer = null;

    private final CRC32 crc;
    private boolean pageWriteChecksumEnabled;

    /**
     * Captures the order in which methods should be called
     */
    private enum STATE {
        NOT_STARTED {
            STATE start() {
                return STARTED;
            }
        },
        STARTED {
            STATE startBlock() {
                return BLOCK;
            }
            STATE end() {
                return ENDED;
            }
        },
        BLOCK  {
            STATE startColumn() {
                return COLUMN;
            }
            STATE endBlock() {
                return STARTED;
            }
        },
        COLUMN {
            STATE endColumn() {
                return BLOCK;
            };
            STATE write() {
                return this;
            }
        },
        ENDED;

        STATE start() throws IOException { return error(); }
        STATE startBlock() throws IOException { return error(); }
        STATE startColumn() throws IOException { return error(); }
        STATE write() throws IOException { return error(); }
        STATE endColumn() throws IOException { return error(); }
        STATE endBlock() throws IOException { return error(); }
        STATE end() throws IOException { return error(); }

        private final STATE error() throws IOException {
            throw new IOException("The file being written is in an invalid state. Probably caused by an error thrown previously. Current state: " + this.name());
        }
    }

    private STATE state = STATE.NOT_STARTED;

    /**
     * @param configuration Hadoop configuration
     * @param schema the schema of the data
     * @param file the file to write to
     * @throws IOException if the file can not be created
     * @deprecated will be removed in 2.0.0
     */
    @Deprecated
    public ParquetFileWriter(Configuration configuration, MessageType schema,
                             Path file) throws IOException {
        this(HadoopOutputFile.fromPath(file, configuration),
                schema, Mode.CREATE, DEFAULT_BLOCK_SIZE, MAX_PADDING_SIZE_DEFAULT);
    }

    /**
     * @param configuration Hadoop configuration
     * @param schema the schema of the data
     * @param file the file to write to
     * @param mode file creation mode
     * @throws IOException if the file can not be created
     * @deprecated will be removed in 2.0.0
     */
    @Deprecated
    public ParquetFileWriter(Configuration configuration, MessageType schema,
                             Path file, Mode mode) throws IOException {
        this(HadoopOutputFile.fromPath(file, configuration),
                schema, mode, DEFAULT_BLOCK_SIZE, MAX_PADDING_SIZE_DEFAULT);
    }

    /**
     * @param configuration Hadoop configuration
     * @param schema the schema of the data
     * @param file the file to write to
     * @param mode file creation mode
     * @param rowGroupSize the row group size
     * @param maxPaddingSize the maximum padding
     * @throws IOException if the file can not be created
     * @deprecated will be removed in 2.0.0
     */
    @Deprecated
    public ParquetFileWriter(Configuration configuration, MessageType schema,
                             Path file, Mode mode, long rowGroupSize,
                             int maxPaddingSize)
            throws IOException {
        this(HadoopOutputFile.fromPath(file, configuration),
                schema, mode, rowGroupSize, maxPaddingSize);
    }

    /**
     * @param file OutputFile to create or overwrite
     * @param schema the schema of the data
     * @param mode file creation mode
     * @param rowGroupSize the row group size
     * @param maxPaddingSize the maximum padding
     * @throws IOException if the file can not be created
     * @deprecated will be removed in 2.0.0
     */
    @Deprecated
    public ParquetFileWriter(OutputFile file, MessageType schema, Mode mode,
                             long rowGroupSize, int maxPaddingSize)
            throws IOException {
        this(file, schema, mode, rowGroupSize, maxPaddingSize,
                ParquetProperties.DEFAULT_COLUMN_INDEX_TRUNCATE_LENGTH,
                ParquetProperties.DEFAULT_STATISTICS_TRUNCATE_LENGTH,
                ParquetProperties.DEFAULT_PAGE_WRITE_CHECKSUM_ENABLED);
    }

    /**
     * @param file OutputFile to create or overwrite
     * @param schema the schema of the data
     * @param mode file creation mode
     * @param rowGroupSize the row group size
     * @param maxPaddingSize the maximum padding
     * @param columnIndexTruncateLength the length which the min/max values in column indexes tried to be truncated to
     * @param statisticsTruncateLength the length which the min/max values in row groups tried to be truncated to
     * @param pageWriteChecksumEnabled whether to write out page level checksums
     * @throws IOException if the file can not be created
     */
    public ParquetFileWriter(OutputFile file, MessageType schema, Mode mode,
                             long rowGroupSize, int maxPaddingSize, int columnIndexTruncateLength,
                             int statisticsTruncateLength, boolean pageWriteChecksumEnabled)
            throws IOException{
        this(file, schema, mode, rowGroupSize, maxPaddingSize, columnIndexTruncateLength,
                statisticsTruncateLength, pageWriteChecksumEnabled, null);
    }

    public ParquetFileWriter(OutputFile file, MessageType schema, Mode mode,
                             long rowGroupSize, int maxPaddingSize, int columnIndexTruncateLength,
                             int statisticsTruncateLength, boolean pageWriteChecksumEnabled,
                             FileEncryptionProperties encryptionProperties)
            throws IOException {
        TypeUtil.checkValidWriteSchema(schema);

        this.schema = schema;

        long blockSize = rowGroupSize;
        if (file.supportsBlockSize()) {
            blockSize = Math.max(file.defaultBlockSize(), rowGroupSize);
            this.alignment = PaddingAlignment.get(blockSize, rowGroupSize, maxPaddingSize);
        } else {
            this.alignment = NoAlignment.get(rowGroupSize);
        }

        if (mode == Mode.OVERWRITE) {
            this.out = file.createOrOverwrite(blockSize);
        } else {
            this.out = file.create(blockSize);
        }

        this.encodingStatsBuilder = new EncodingStats.Builder();
        this.columnIndexTruncateLength = columnIndexTruncateLength;
        this.pageWriteChecksumEnabled = pageWriteChecksumEnabled;
        this.crc = pageWriteChecksumEnabled ? new CRC32() : null;

        this.metadataConverter = new ParquetMetadataConverter(statisticsTruncateLength);

        if (null == encryptionProperties) {
            this.fileEncryptor = null;
        } else {
            // Verify that every encrypted column is in file schema
            Map<ColumnPath, ColumnEncryptionProperties> columnEncryptionProperties = encryptionProperties.getEncryptedColumns();
            if (null != columnEncryptionProperties) { // if null, every column in file schema will be encrypted with footer key
                for (Map.Entry<ColumnPath, ColumnEncryptionProperties> entry : columnEncryptionProperties.entrySet()) {
                    String[] path = entry.getKey().toArray();
                    if(!schema.containsPath(path)) {
                        throw new ParquetCryptoRuntimeException("Encrypted column " + Arrays.toString(path) + " not in file schema");
                    }
                }
            }
            this.fileEncryptor = new InternalFileEncryptor(encryptionProperties);
        }
    }

    /**
     * FOR TESTING ONLY. This supports testing block padding behavior on the local FS.
     *
     * @param configuration Hadoop configuration
     * @param schema the schema of the data
     * @param file the file to write to
     * @param rowAndBlockSize the row group size
     * @param maxPaddingSize the maximum padding
     * @throws IOException if the file can not be created
     */
    ParquetFileWriter(Configuration configuration, MessageType schema,
                      Path file, long rowAndBlockSize, int maxPaddingSize)
            throws IOException {
        FileSystem fs = file.getFileSystem(configuration);
        this.schema = schema;
        this.alignment = PaddingAlignment.get(
                rowAndBlockSize, rowAndBlockSize, maxPaddingSize);
        this.out = HadoopStreams.wrap(
                fs.create(file, true, 8192, fs.getDefaultReplication(file), rowAndBlockSize));
        this.encodingStatsBuilder = new EncodingStats.Builder();
        // no truncation is needed for testing
        this.columnIndexTruncateLength = Integer.MAX_VALUE;
        this.pageWriteChecksumEnabled = ParquetOutputFormat.getPageWriteChecksumEnabled(configuration);
        this.crc = pageWriteChecksumEnabled ? new CRC32() : null;
        this.metadataConverter = new ParquetMetadataConverter(ParquetProperties.DEFAULT_STATISTICS_TRUNCATE_LENGTH);
        this.fileEncryptor = null;
    }
    /**
     * start the file
     * @throws IOException if there is an error while writing
     */
    public void start() throws IOException {
        state = state.start();
        LOG.debug("{}: start", out.getPos());
        byte[] magic = MAGIC;
        if (null != fileEncryptor && fileEncryptor.isFooterEncrypted()) {
            magic = EFMAGIC;
        }
        out.write(magic);
    }

    InternalFileEncryptor getEncryptor() {
        return fileEncryptor;
    }

    /**
     * start a block
     * @param recordCount the record count in this block
     * @throws IOException if there is an error while writing
     */
    public void startBlock(long recordCount) throws IOException {
        state = state.startBlock();
        LOG.debug("{}: start block", out.getPos());
//    out.write(MAGIC); // TODO: add a magic delimiter

        alignment.alignForRowGroup(out);

        currentBlock = new BlockMetaData();
        currentRecordCount = recordCount;

        currentColumnIndexes = new ArrayList<>();
        currentOffsetIndexes = new ArrayList<>();

        currentBloomFilters = new HashMap<>();
    }

    /**
     * start a column inside a block
     * @param descriptor the column descriptor
     * @param valueCount the value count in this column
     * @param compressionCodecName a compression codec name
     * @throws IOException if there is an error while writing
     */
    public void startColumn(ColumnDescriptor descriptor,
                            long valueCount,
                            CompressionCodecName compressionCodecName) throws IOException {
        state = state.startColumn();
        encodingStatsBuilder.clear();
        currentEncodings = new HashSet<Encoding>();
        currentChunkPath = ColumnPath.get(descriptor.getPath());
        currentChunkType = descriptor.getPrimitiveType();
        currentChunkCodec = compressionCodecName;
        currentChunkValueCount = valueCount;
        currentChunkFirstDataPage = -1;
        compressedLength = 0;
        uncompressedLength = 0;
        // The statistics will be copied from the first one added at writeDataPage(s) so we have the correct typed one
        currentStatistics = null;

        columnIndexBuilder = ColumnIndexBuilder.getBuilder(currentChunkType, columnIndexTruncateLength);
        offsetIndexBuilder = OffsetIndexBuilder.getBuilder();
    }

    /**
     * writes a dictionary page page
     * @param dictionaryPage the dictionary page
     * @throws IOException if there is an error while writing
     */
    public void writeDictionaryPage(DictionaryPage dictionaryPage) throws IOException {
        writeDictionaryPage(dictionaryPage, null, null);
    }

    public void writeDictionaryPage(DictionaryPage dictionaryPage,
                                    BlockCipher.Encryptor headerBlockEncryptor, byte[] AAD) throws IOException {
        state = state.write();
        LOG.debug("{}: write dictionary page: {} values", out.getPos(), dictionaryPage.getDictionarySize());
        currentChunkDictionaryPageOffset = out.getPos();
        int uncompressedSize = dictionaryPage.getUncompressedSize();
        int compressedPageSize = (int)dictionaryPage.getBytes().size(); // TODO: fix casts
        if (pageWriteChecksumEnabled) {
            crc.reset();
            crc.update(dictionaryPage.getBytes().toByteArray());
            metadataConverter.writeDictionaryPageHeader(
                    uncompressedSize,
                    compressedPageSize,
                    dictionaryPage.getDictionarySize(),
                    dictionaryPage.getEncoding(),
                    (int) crc.getValue(),
                    out,
                    headerBlockEncryptor,
                    AAD);
        } else {
            metadataConverter.writeDictionaryPageHeader(
                    uncompressedSize,
                    compressedPageSize,
                    dictionaryPage.getDictionarySize(),
                    dictionaryPage.getEncoding(),
                    out,
                    headerBlockEncryptor,
                    AAD);
        }
        long headerSize = out.getPos() - currentChunkDictionaryPageOffset;
        this.uncompressedLength += uncompressedSize + headerSize;
        this.compressedLength += compressedPageSize + headerSize;
        LOG.debug("{}: write dictionary page content {}", out.getPos(), compressedPageSize);
        dictionaryPage.getBytes().writeAllTo(out); // for encrypted column, dictionary page bytes are already encrypted
        encodingStatsBuilder.addDictEncoding(dictionaryPage.getEncoding());
        currentEncodings.add(dictionaryPage.getEncoding());
    }


    /**
     * writes a single page
     * @param valueCount count of values
     * @param uncompressedPageSize the size of the data once uncompressed
     * @param bytes the compressed data for the page without header
     * @param rlEncoding encoding of the repetition level
     * @param dlEncoding encoding of the definition level
     * @param valuesEncoding encoding of values
     * @throws IOException if there is an error while writing
     */
    @Deprecated
    public void writeDataPage(
            int valueCount, int uncompressedPageSize,
            BytesInput bytes,
            Encoding rlEncoding,
            Encoding dlEncoding,
            Encoding valuesEncoding) throws IOException {
        state = state.write();
        // We are unable to build indexes without rowCount so skip them for this column
        offsetIndexBuilder = OffsetIndexBuilder.getNoOpBuilder();
        columnIndexBuilder = ColumnIndexBuilder.getNoOpBuilder();
        long beforeHeader = out.getPos();
        LOG.debug("{}: write data page: {} values", beforeHeader, valueCount);
        int compressedPageSize = (int)bytes.size();
        metadataConverter.writeDataPageV1Header(
                uncompressedPageSize, compressedPageSize,
                valueCount,
                rlEncoding,
                dlEncoding,
                valuesEncoding,
                out);
        long headerSize = out.getPos() - beforeHeader;
        this.uncompressedLength += uncompressedPageSize + headerSize;
        this.compressedLength += compressedPageSize + headerSize;
        LOG.debug("{}: write data page content {}", out.getPos(), compressedPageSize);
        bytes.writeAllTo(out);
        encodingStatsBuilder.addDataEncoding(valuesEncoding);
        currentEncodings.add(rlEncoding);
        currentEncodings.add(dlEncoding);
        currentEncodings.add(valuesEncoding);
        if (currentChunkFirstDataPage < 0) {
            currentChunkFirstDataPage = beforeHeader;
        }
    }

    /**
     * writes a single page
     * @param valueCount count of values
     * @param uncompressedPageSize the size of the data once uncompressed
     * @param bytes the compressed data for the page without header
     * @param statistics statistics for the page
     * @param rlEncoding encoding of the repetition level
     * @param dlEncoding encoding of the definition level
     * @param valuesEncoding encoding of values
     * @throws IOException if there is an error while writing
     * @deprecated this method does not support writing column indexes; Use
     *             {@link #writeDataPage(int, int, BytesInput, Statistics, long, Encoding, Encoding, Encoding)} instead
     */
    @Deprecated
    public void writeDataPage(
            int valueCount, int uncompressedPageSize,
            BytesInput bytes,
            Statistics statistics,
            Encoding rlEncoding,
            Encoding dlEncoding,
            Encoding valuesEncoding) throws IOException {
        // We are unable to build indexes without rowCount so skip them for this column
        offsetIndexBuilder = OffsetIndexBuilder.getNoOpBuilder();
        columnIndexBuilder = ColumnIndexBuilder.getNoOpBuilder();
        innerWriteDataPage(valueCount, uncompressedPageSize, bytes, statistics, rlEncoding, dlEncoding, valuesEncoding);
    }

    /**
     * Writes a single page
     * @param valueCount count of values
     * @param uncompressedPageSize the size of the data once uncompressed
     * @param bytes the compressed data for the page without header
     * @param statistics the statistics of the page
     * @param rowCount the number of rows in the page
     * @param rlEncoding encoding of the repetition level
     * @param dlEncoding encoding of the definition level
     * @param valuesEncoding encoding of values
     * @throws IOException if any I/O error occurs during writing the file
     */
    public void writeDataPage(
            int valueCount, int uncompressedPageSize,
            BytesInput bytes,
            Statistics statistics,
            long rowCount,
            Encoding rlEncoding,
            Encoding dlEncoding,
            Encoding valuesEncoding) throws IOException {
        long beforeHeader = out.getPos();
        innerWriteDataPage(valueCount, uncompressedPageSize, bytes, statistics, rlEncoding, dlEncoding, valuesEncoding);

        offsetIndexBuilder.add((int) (out.getPos() - beforeHeader), rowCount);
    }

    private void innerWriteDataPage(
            int valueCount, int uncompressedPageSize,
            BytesInput bytes,
            Statistics statistics,
            Encoding rlEncoding,
            Encoding dlEncoding,
            Encoding valuesEncoding) throws IOException {
        state = state.write();
        long beforeHeader = out.getPos();
        if (currentChunkFirstDataPage < 0) {
            currentChunkFirstDataPage = beforeHeader;
        }
        LOG.debug("{}: write data page: {} values", beforeHeader, valueCount);
        int compressedPageSize = (int) bytes.size();
        if (pageWriteChecksumEnabled) {
            crc.reset();
            crc.update(bytes.toByteArray());
            metadataConverter.writeDataPageV1Header(
                    uncompressedPageSize, compressedPageSize,
                    valueCount,
                    rlEncoding,
                    dlEncoding,
                    valuesEncoding,
                    (int) crc.getValue(),
                    out);
        } else {
            metadataConverter.writeDataPageV1Header(
                    uncompressedPageSize, compressedPageSize,
                    valueCount,
                    rlEncoding,
                    dlEncoding,
                    valuesEncoding,
                    out);
        }
        long headerSize = out.getPos() - beforeHeader;
        this.uncompressedLength += uncompressedPageSize + headerSize;
        this.compressedLength += compressedPageSize + headerSize;
        LOG.debug("{}: write data page content {}", out.getPos(), compressedPageSize);
        bytes.writeAllTo(out);

        // Copying the statistics if it is not initialized yet so we have the correct typed one
        if (currentStatistics == null) {
            currentStatistics = statistics.copy();
        } else {
            currentStatistics.mergeStatistics(statistics);
        }

        columnIndexBuilder.add(statistics);

        encodingStatsBuilder.addDataEncoding(valuesEncoding);
        currentEncodings.add(rlEncoding);
        currentEncodings.add(dlEncoding);
        currentEncodings.add(valuesEncoding);
    }

    /**
     * Add a Bloom filter that will be written out. This is only used in unit test.
     *
     * @param column the column name
     * @param bloomFilter the bloom filter of column values
     */
    void addBloomFilter(String column, BloomFilter bloomFilter) {
        currentBloomFilters.put(column, bloomFilter);
    }

    /**
     * Writes a single v2 data page
     * @param rowCount count of rows
     * @param nullCount count of nulls
     * @param valueCount count of values
     * @param repetitionLevels repetition level bytes
     * @param definitionLevels definition level bytes
     * @param dataEncoding encoding for data
     * @param compressedData compressed data bytes
     * @param uncompressedDataSize the size of uncompressed data
     * @param statistics the statistics of the page
     * @throws IOException if any I/O error occurs during writing the file
     */
    public void writeDataPageV2(int rowCount, int nullCount, int valueCount,
                                BytesInput repetitionLevels,
                                BytesInput definitionLevels,
                                Encoding dataEncoding,
                                BytesInput compressedData,
                                int uncompressedDataSize,
                                Statistics<?> statistics) throws IOException {
        state = state.write();
        int rlByteLength = toIntWithCheck(repetitionLevels.size());
        int dlByteLength = toIntWithCheck(definitionLevels.size());

        int compressedSize = toIntWithCheck(
                compressedData.size() + repetitionLevels.size() + definitionLevels.size()
        );

        int uncompressedSize = toIntWithCheck(
                uncompressedDataSize + repetitionLevels.size() + definitionLevels.size()
        );

        long beforeHeader = out.getPos();
        if (currentChunkFirstDataPage < 0) {
            currentChunkFirstDataPage = beforeHeader;
        }

        metadataConverter.writeDataPageV2Header(
                uncompressedSize, compressedSize,
                valueCount, nullCount, rowCount,
                dataEncoding,
                rlByteLength,
                dlByteLength,
                out);

        long headersSize  = out.getPos() - beforeHeader;
        this.uncompressedLength += uncompressedSize + headersSize;
        this.compressedLength += compressedSize + headersSize;

        if (currentStatistics == null) {
            currentStatistics = statistics.copy();
        } else {
            currentStatistics.mergeStatistics(statistics);
        }

        columnIndexBuilder.add(statistics);
        currentEncodings.add(dataEncoding);
        encodingStatsBuilder.addDataEncoding(dataEncoding);

        BytesInput.concat(repetitionLevels, definitionLevels, compressedData)
                .writeAllTo(out);

        offsetIndexBuilder.add((int) (out.getPos() - beforeHeader), rowCount);
    }

    /**
     * Writes a column chunk at once
     * @param descriptor the descriptor of the column
     * @param valueCount the value count in this column
     * @param compressionCodecName the name of the compression codec used for compressing the pages
     * @param dictionaryPage the dictionary page for this column chunk (might be null)
     * @param bytes the encoded pages including page headers to be written as is
     * @param uncompressedTotalPageSize total uncompressed size (without page headers)
     * @param compressedTotalPageSize total compressed size (without page headers)
     * @param totalStats accumulated statistics for the column chunk
     * @param columnIndexBuilder the builder object for the column index
     * @param offsetIndexBuilder the builder object for the offset index
     * @param bloomFilter the bloom filter for this column
     * @param rlEncodings the RL encodings used in this column chunk
     * @param dlEncodings the DL encodings used in this column chunk
     * @param dataEncodings the data encodings used in this column chunk
     * @throws IOException if there is an error while writing
     */
    void writeColumnChunk(ColumnDescriptor descriptor,
                          long valueCount,
                          CompressionCodecName compressionCodecName,
                          DictionaryPage dictionaryPage,
                          BytesInput bytes,
                          long uncompressedTotalPageSize,
                          long compressedTotalPageSize,
                          Statistics<?> totalStats,
                          ColumnIndexBuilder columnIndexBuilder,
                          OffsetIndexBuilder offsetIndexBuilder,
                          BloomFilter bloomFilter,
                          Set<Encoding> rlEncodings,
                          Set<Encoding> dlEncodings,
                          List<Encoding> dataEncodings) throws IOException {
        writeColumnChunk(descriptor, valueCount, compressionCodecName, dictionaryPage, bytes,
                uncompressedTotalPageSize, compressedTotalPageSize, totalStats, columnIndexBuilder, offsetIndexBuilder,
                bloomFilter, rlEncodings, dlEncodings, dataEncodings, null, 0, 0, null);
    }

    void writeColumnChunk(ColumnDescriptor descriptor,
                          long valueCount,
                          CompressionCodecName compressionCodecName,
                          DictionaryPage dictionaryPage,
                          BytesInput bytes,
                          long uncompressedTotalPageSize,
                          long compressedTotalPageSize,
                          Statistics<?> totalStats,
                          ColumnIndexBuilder columnIndexBuilder,
                          OffsetIndexBuilder offsetIndexBuilder,
                          BloomFilter bloomFilter,
                          Set<Encoding> rlEncodings,
                          Set<Encoding> dlEncodings,
                          List<Encoding> dataEncodings,
                          BlockCipher.Encryptor headerBlockEncryptor,
                          int rowGroupOrdinal,
                          int columnOrdinal,
                          byte[] fileAAD) throws IOException {
        startColumn(descriptor, valueCount, compressionCodecName);

        state = state.write();
        if (dictionaryPage != null) {
            byte[] dictonaryPageHeaderAAD = null;
            if (null != headerBlockEncryptor) {
                dictonaryPageHeaderAAD = AesCipher.createModuleAAD(fileAAD, ModuleType.DictionaryPageHeader,
                        rowGroupOrdinal, columnOrdinal, -1);
            }
            writeDictionaryPage(dictionaryPage, headerBlockEncryptor, dictonaryPageHeaderAAD);
        }

        if (bloomFilter != null) {
            // write bloom filter if one of data pages is not dictionary encoded
            boolean isWriteBloomFilter = false;
            for (Encoding encoding : dataEncodings) {
                if (encoding != Encoding.RLE_DICTIONARY) {
                    isWriteBloomFilter = true;
                    break;
                }
            }
            if (isWriteBloomFilter) {
                currentBloomFilters.put(String.join(".", descriptor.getPath()), bloomFilter);
            }
        }
        LOG.debug("{}: write data pages", out.getPos());
        long headersSize = bytes.size() - compressedTotalPageSize;
        this.uncompressedLength += uncompressedTotalPageSize + headersSize;
        this.compressedLength += compressedTotalPageSize + headersSize;
        LOG.debug("{}: write data pages content", out.getPos());
        currentChunkFirstDataPage = out.getPos();
        bytes.writeAllTo(out);
        encodingStatsBuilder.addDataEncodings(dataEncodings);
        if (rlEncodings.isEmpty()) {
            encodingStatsBuilder.withV2Pages();
        }
        currentEncodings.addAll(rlEncodings);
        currentEncodings.addAll(dlEncodings);
        currentEncodings.addAll(dataEncodings);
        currentStatistics = totalStats;

        this.columnIndexBuilder = columnIndexBuilder;
        this.offsetIndexBuilder = offsetIndexBuilder;

        endColumn();
    }

    /**
     * end a column (once all rep, def and data have been written)
     * @throws IOException if there is an error while writing
     */
    public void endColumn() throws IOException {
        state = state.endColumn();
        LOG.debug("{}: end column", out.getPos());
        if (columnIndexBuilder.getMinMaxSize() > columnIndexBuilder.getPageCount() * MAX_STATS_SIZE) {
            currentColumnIndexes.add(null);
        } else {
            currentColumnIndexes.add(columnIndexBuilder.build());
        }
        currentOffsetIndexes.add(offsetIndexBuilder.build(currentChunkFirstDataPage));
        currentBlock.addColumn(ColumnChunkMetaData.get(
                currentChunkPath,
                currentChunkType,
                currentChunkCodec,
                encodingStatsBuilder.build(),
                currentEncodings,
                currentStatistics,
                currentChunkFirstDataPage,
                currentChunkDictionaryPageOffset,
                currentChunkValueCount,
                compressedLength,
                uncompressedLength));
        this.currentBlock.setTotalByteSize(currentBlock.getTotalByteSize() + uncompressedLength);
        this.uncompressedLength = 0;
        this.compressedLength = 0;
        columnIndexBuilder = null;
        offsetIndexBuilder = null;
    }

    /**
     * ends a block once all column chunks have been written
     * @throws IOException if there is an error while writing
     */
    public void endBlock() throws IOException {
//        if (currentRecordCount == 0) {
//            throw new ParquetEncodingException("End block with zero record");
//        }

        state = state.endBlock();
        LOG.debug("{}: end block", out.getPos());
        currentBlock.setRowCount(currentRecordCount);
        currentBlock.setOrdinal(blocks.size());
        blocks.add(currentBlock);
        columnIndexes.add(currentColumnIndexes);
        offsetIndexes.add(currentOffsetIndexes);
        bloomFilters.add(currentBloomFilters);
        currentColumnIndexes = null;
        currentOffsetIndexes = null;
        currentBloomFilters = null;
        currentBlock = null;
    }

    /**
     * @param conf a configuration
     * @param file a file path to append the contents of to this file
     * @throws IOException if there is an error while reading or writing
     * @deprecated will be removed in 2.0.0; use {@link #appendFile(InputFile)} instead
     */
    @Deprecated
    public void appendFile(Configuration conf, Path file) throws IOException {
        ParquetFileReader.open(conf, file).appendTo(this);
    }

    public void appendFile(InputFile file) throws IOException {
        try (ParquetFileReader reader = ParquetFileReader.open(file)) {
            reader.appendTo(this);
        }
    }

    /**
     * @param file a file stream to read from
     * @param rowGroups row groups to copy
     * @param dropColumns whether to drop columns from the file that are not in this file's schema
     * @throws IOException if there is an error while reading or writing
     * @deprecated will be removed in 2.0.0;
     *             use {@link #appendRowGroups(SeekableInputStream,List,boolean)} instead
     */
    @Deprecated
    public void appendRowGroups(FSDataInputStream file,
                                List<BlockMetaData> rowGroups,
                                boolean dropColumns) throws IOException {
        appendRowGroups(HadoopStreams.wrap(file), rowGroups, dropColumns);
    }

    public void appendRowGroups(SeekableInputStream file,
                                List<BlockMetaData> rowGroups,
                                boolean dropColumns) throws IOException {
        for (BlockMetaData block : rowGroups) {
            appendRowGroup(file, block, dropColumns);
        }
    }

    /**
     * @param from a file stream to read from
     * @param rowGroup row group to copy
     * @param dropColumns whether to drop columns from the file that are not in this file's schema
     * @throws IOException if there is an error while reading or writing
     * @deprecated will be removed in 2.0.0;
     *             use {@link #appendRowGroup(SeekableInputStream,BlockMetaData,boolean)} instead
     */
    @Deprecated
    public void appendRowGroup(FSDataInputStream from, BlockMetaData rowGroup,
                               boolean dropColumns) throws IOException {
        appendRowGroup(HadoopStreams.wrap(from), rowGroup, dropColumns);
    }

    public void appendRowGroup(SeekableInputStream from, BlockMetaData rowGroup,
                               boolean dropColumns) throws IOException {
        startBlock(rowGroup.getRowCount());

        Map<String, ColumnChunkMetaData> columnsToCopy =
                new HashMap<String, ColumnChunkMetaData>();
        for (ColumnChunkMetaData chunk : rowGroup.getColumns()) {
            columnsToCopy.put(chunk.getPath().toDotString(), chunk);
        }

        List<ColumnChunkMetaData> columnsInOrder =
                new ArrayList<ColumnChunkMetaData>();

        for (ColumnDescriptor descriptor : schema.getColumns()) {
            String path = ColumnPath.get(descriptor.getPath()).toDotString();
            ColumnChunkMetaData chunk = columnsToCopy.remove(path);
            if (chunk != null) {
                columnsInOrder.add(chunk);
            } else {
                throw new IllegalArgumentException(String.format(
                        "Missing column '%s', cannot copy row group: %s", path, rowGroup));
            }
        }

        // complain if some columns would be dropped and that's not okay
        if (!dropColumns && !columnsToCopy.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Columns cannot be copied (missing from target schema): %s",
                    String.join(", ", columnsToCopy.keySet())));
        }

        // copy the data for all chunks
        long start = -1;
        long length = 0;
        long blockUncompressedSize = 0L;
        for (int i = 0; i < columnsInOrder.size(); i += 1) {
            ColumnChunkMetaData chunk = columnsInOrder.get(i);

            // get this chunk's start position in the new file
            long newChunkStart = out.getPos() + length;

            // add this chunk to be copied with any previous chunks
            if (start < 0) {
                // no previous chunk included, start at this chunk's starting pos
                start = chunk.getStartingPos();
            }
            length += chunk.getTotalSize();

            if ((i + 1) == columnsInOrder.size() ||
                    columnsInOrder.get(i + 1).getStartingPos() != (start + length)) {
                // not contiguous. do the copy now.
                copy(from, out, start, length);
                // reset to start at the next column chunk
                start = -1;
                length = 0;
            }

            // TODO: column/offset indexes are not copied
            // (it would require seeking to the end of the file for each row groups)
            currentColumnIndexes.add(null);
            currentOffsetIndexes.add(null);

            Offsets offsets = Offsets.getOffsets(from, chunk, newChunkStart);
            currentBlock.addColumn(ColumnChunkMetaData.get(
                    chunk.getPath(),
                    chunk.getPrimitiveType(),
                    chunk.getCodec(),
                    chunk.getEncodingStats(),
                    chunk.getEncodings(),
                    chunk.getStatistics(),
                    offsets.firstDataPageOffset,
                    offsets.dictionaryPageOffset,
                    chunk.getValueCount(),
                    chunk.getTotalSize(),
                    chunk.getTotalUncompressedSize()));

            blockUncompressedSize += chunk.getTotalUncompressedSize();
        }

        currentBlock.setTotalByteSize(blockUncompressedSize);

        endBlock();
    }

    /**
     * @param descriptor the descriptor for the target column
     * @param from a file stream to read from
     * @param chunk the column chunk to be copied
     * @param bloomFilter the bloomFilter for this chunk
     * @param columnIndex the column index for this chunk
     * @param offsetIndex the offset index for this chunk
     * @throws IOException
     */
    public void appendColumnChunk(ColumnDescriptor descriptor, SeekableInputStream from, ColumnChunkMetaData chunk,
                                  BloomFilter bloomFilter, ColumnIndex columnIndex, OffsetIndex offsetIndex) throws IOException {
        long start = chunk.getStartingPos();
        long length = chunk.getTotalSize();
        long newChunkStart = out.getPos();

        copy(from, out, start, length);

        currentBloomFilters.put(String.join(".", descriptor.getPath()), bloomFilter);
        currentColumnIndexes.add(columnIndex);
        currentOffsetIndexes.add(offsetIndex);

        Offsets offsets = Offsets.getOffsets(from, chunk, newChunkStart);
        currentBlock.addColumn(ColumnChunkMetaData.get(
                chunk.getPath(),
                chunk.getPrimitiveType(),
                chunk.getCodec(),
                chunk.getEncodingStats(),
                chunk.getEncodings(),
                chunk.getStatistics(),
                offsets.firstDataPageOffset,
                offsets.dictionaryPageOffset,
                chunk.getValueCount(),
                chunk.getTotalSize(),
                chunk.getTotalUncompressedSize()));

        currentBlock.setTotalByteSize(currentBlock.getTotalByteSize() + chunk.getTotalUncompressedSize());
    }

    // Buffers for the copy function.
    private static final ThreadLocal<byte[]> COPY_BUFFER = ThreadLocal.withInitial(() -> new byte[8192]);

    /**
     * Copy from a FS input stream to an output stream. Thread-safe
     *
     * @param from a {@link SeekableInputStream}
     * @param to any {@link PositionOutputStream}
     * @param start where in the from stream to start copying
     * @param length the number of bytes to copy
     * @throws IOException if there is an error while reading or writing
     */
    private static void copy(SeekableInputStream from, PositionOutputStream to,
                             long start, long length) throws IOException{
        LOG.debug("Copying {} bytes at {} to {}", length, start, to.getPos());
        from.seek(start);
        long bytesCopied = 0;
        byte[] buffer = COPY_BUFFER.get();
        while (bytesCopied < length) {
            long bytesLeft = length - bytesCopied;
            int bytesRead = from.read(buffer, 0,
                    (buffer.length < bytesLeft ? buffer.length : (int) bytesLeft));
            if (bytesRead < 0) {
                throw new IllegalArgumentException(
                        "Unexpected end of input file at " + start + bytesCopied);
            }
            to.write(buffer, 0, bytesRead);
            bytesCopied += bytesRead;
        }
    }

    /**
     * ends a file once all blocks have been written.
     * closes the file.
     * @param extraMetaData the extra meta data to write in the footer
     * @throws IOException if there is an error while writing
     */
    public void end(Map<String, String> extraMetaData) throws IOException {
        state = state.end();
        serializeColumnIndexes(columnIndexes, blocks, out, fileEncryptor);
        serializeOffsetIndexes(offsetIndexes, blocks, out, fileEncryptor);
        serializeBloomFilters(bloomFilters, blocks, out, fileEncryptor);
        LOG.debug("{}: end", out.getPos());
        this.footer = new ParquetMetadata(new FileMetaData(schema, extraMetaData, Version.FULL_VERSION), blocks);
        serializeFooter(footer, out, fileEncryptor);
        out.close();
    }

    private static void serializeColumnIndexes(
            List<List<ColumnIndex>> columnIndexes,
            List<BlockMetaData> blocks,
            PositionOutputStream out,
            InternalFileEncryptor fileEncryptor) throws IOException {
        LOG.debug("{}: column indexes", out.getPos());
        for (int bIndex = 0, bSize = blocks.size(); bIndex < bSize; ++bIndex) {
            BlockMetaData block = blocks.get(bIndex);
            List<ColumnChunkMetaData> columns = block.getColumns();
            List<ColumnIndex> blockColumnIndexes = columnIndexes.get(bIndex);
            for (int cIndex = 0, cSize = columns.size(); cIndex < cSize; ++cIndex) {
                ColumnChunkMetaData column = columns.get(cIndex);
                org.apache.parquet.format.ColumnIndex columnIndex = ParquetMetadataConverter
                        .toParquetColumnIndex(column.getPrimitiveType(), blockColumnIndexes.get(cIndex));
                if (columnIndex == null) {
                    continue;
                }
                BlockCipher.Encryptor columnIndexEncryptor = null;
                byte[] columnIndexAAD = null;
                if (null != fileEncryptor) {
                    InternalColumnEncryptionSetup columnEncryptionSetup = fileEncryptor.getColumnSetup(column.getPath(), false, cIndex);
                    if (columnEncryptionSetup.isEncrypted()) {
                        columnIndexEncryptor = columnEncryptionSetup.getMetaDataEncryptor();
                        columnIndexAAD = AesCipher.createModuleAAD(fileEncryptor.getFileAAD(), ModuleType.ColumnIndex,
                                block.getOrdinal(), columnEncryptionSetup.getOrdinal(), -1);
                    }
                }
                long offset = out.getPos();
                Util.writeColumnIndex(columnIndex, out, columnIndexEncryptor, columnIndexAAD);
                column.setColumnIndexReference(new IndexReference(offset, (int) (out.getPos() - offset)));
            }
        }
    }

    private int toIntWithCheck(long size) {
        if ((int)size != size) {
            throw new ParquetEncodingException("Cannot write page larger than " + Integer.MAX_VALUE + " bytes: " + size);
        }
        return (int)size;
    }

    private static void serializeOffsetIndexes(
            List<List<OffsetIndex>> offsetIndexes,
            List<BlockMetaData> blocks,
            PositionOutputStream out,
            InternalFileEncryptor fileEncryptor) throws IOException {
        LOG.debug("{}: offset indexes", out.getPos());
        for (int bIndex = 0, bSize = blocks.size(); bIndex < bSize; ++bIndex) {
            BlockMetaData block = blocks.get(bIndex);
            List<ColumnChunkMetaData> columns = block.getColumns();
            List<OffsetIndex> blockOffsetIndexes = offsetIndexes.get(bIndex);
            for (int cIndex = 0, cSize = columns.size(); cIndex < cSize; ++cIndex) {
                OffsetIndex offsetIndex = blockOffsetIndexes.get(cIndex);
                if (offsetIndex == null) {
                    continue;
                }
                ColumnChunkMetaData column = columns.get(cIndex);
                BlockCipher.Encryptor offsetIndexEncryptor = null;
                byte[] offsetIndexAAD = null;
                if (null != fileEncryptor) {
                    InternalColumnEncryptionSetup columnEncryptionSetup = fileEncryptor.getColumnSetup(column.getPath(), false, cIndex);
                    if (columnEncryptionSetup.isEncrypted()) {
                        offsetIndexEncryptor = columnEncryptionSetup.getMetaDataEncryptor();
                        offsetIndexAAD = AesCipher.createModuleAAD(fileEncryptor.getFileAAD(), ModuleType.OffsetIndex,
                                block.getOrdinal(), columnEncryptionSetup.getOrdinal(), -1);
                    }
                }
                long offset = out.getPos();
                Util.writeOffsetIndex(ParquetMetadataConverter.toParquetOffsetIndex(offsetIndex), out, offsetIndexEncryptor, offsetIndexAAD);
                column.setOffsetIndexReference(new IndexReference(offset, (int) (out.getPos() - offset)));
            }
        }
    }

    private static void serializeBloomFilters(
            List<Map<String, BloomFilter>> bloomFilters,
            List<BlockMetaData> blocks,
            PositionOutputStream out,
            InternalFileEncryptor fileEncryptor) throws IOException {
        LOG.debug("{}: bloom filters", out.getPos());
        for (int bIndex = 0, bSize = blocks.size(); bIndex < bSize; ++bIndex) {
            BlockMetaData block = blocks.get(bIndex);
            List<ColumnChunkMetaData> columns = block.getColumns();
            Map<String, BloomFilter> blockBloomFilters = bloomFilters.get(bIndex);
            if (blockBloomFilters.isEmpty()) {
                continue;
            }
            for (int cIndex = 0, cSize = columns.size(); cIndex < cSize; ++cIndex) {
                ColumnChunkMetaData column = columns.get(cIndex);
                BloomFilter bloomFilter = blockBloomFilters.get(column.getPath().toDotString());
                if (bloomFilter == null) {
                    continue;
                }

                long offset = out.getPos();
                column.setBloomFilterOffset(offset);

                BlockCipher.Encryptor bloomFilterEncryptor = null;
                byte[] bloomFilterHeaderAAD = null;
                byte[] bloomFilterBitsetAAD = null;
                if (null != fileEncryptor) {
                    InternalColumnEncryptionSetup columnEncryptionSetup = fileEncryptor.getColumnSetup(column.getPath(), false, cIndex);
                    if (columnEncryptionSetup.isEncrypted()) {
                        bloomFilterEncryptor = columnEncryptionSetup.getMetaDataEncryptor();
                        int columnOrdinal = columnEncryptionSetup.getOrdinal();
                        bloomFilterHeaderAAD = AesCipher.createModuleAAD(fileEncryptor.getFileAAD(), ModuleType.BloomFilterHeader,
                                block.getOrdinal(), columnOrdinal, -1);
                        bloomFilterBitsetAAD = AesCipher.createModuleAAD(fileEncryptor.getFileAAD(), ModuleType.BloomFilterBitset,
                                block.getOrdinal(), columnOrdinal, -1);
                    }
                }

                Util.writeBloomFilterHeader(ParquetMetadataConverter.toBloomFilterHeader(bloomFilter), out,
                        bloomFilterEncryptor, bloomFilterHeaderAAD);

                ByteArrayOutputStream tempOutStream = new ByteArrayOutputStream();
                bloomFilter.writeTo(tempOutStream);
                byte[] serializedBitset = tempOutStream.toByteArray();
                if (null != bloomFilterEncryptor) {
                    serializedBitset = bloomFilterEncryptor.encrypt(serializedBitset, bloomFilterBitsetAAD);
                }
                out.write(serializedBitset);
            }
        }
    }

    private static void serializeFooter(ParquetMetadata footer, PositionOutputStream out,
                                        InternalFileEncryptor fileEncryptor) throws IOException {

        ParquetMetadataConverter metadataConverter = new ParquetMetadataConverter();

        // Unencrypted file
        if (null == fileEncryptor) {
            long footerIndex = out.getPos();
            org.apache.parquet.format.FileMetaData parquetMetadata = metadataConverter.toParquetMetadata(CURRENT_VERSION, footer);
            writeFileMetaData(parquetMetadata, out);
            LOG.debug("{}: footer length = {}", out.getPos(), (out.getPos() - footerIndex));
            BytesUtils.writeIntLittleEndian(out, (int) (out.getPos() - footerIndex));
            out.write(MAGIC);
            return;
        }

        org.apache.parquet.format.FileMetaData parquetMetadata =
                metadataConverter.toParquetMetadata(CURRENT_VERSION, footer, fileEncryptor);

        // Encrypted file with plaintext footer
        if (!fileEncryptor.isFooterEncrypted()) {
            long footerIndex = out.getPos();
            parquetMetadata.setEncryption_algorithm(fileEncryptor.getEncryptionAlgorithm());
            // create footer signature (nonce + tag of encrypted footer)
            byte[] footerSigningKeyMetaData = fileEncryptor.getFooterSigningKeyMetaData();
            if (null != footerSigningKeyMetaData) {
                parquetMetadata.setFooter_signing_key_metadata(footerSigningKeyMetaData);
            }
            ByteArrayOutputStream tempOutStream = new ByteArrayOutputStream();
            writeFileMetaData(parquetMetadata, tempOutStream);
            byte[] serializedFooter = tempOutStream.toByteArray();
            byte[] footerAAD = AesCipher.createFooterAAD(fileEncryptor.getFileAAD());
            byte[] encryptedFooter = fileEncryptor.getSignedFooterEncryptor().encrypt(serializedFooter, footerAAD);
            byte[] signature = new byte[AesCipher.NONCE_LENGTH + AesCipher.GCM_TAG_LENGTH];
            System.arraycopy(encryptedFooter, ModuleCipherFactory.SIZE_LENGTH, signature, 0, AesCipher.NONCE_LENGTH); // copy Nonce
            System.arraycopy(encryptedFooter, encryptedFooter.length - AesCipher.GCM_TAG_LENGTH,
                    signature, AesCipher.NONCE_LENGTH, AesCipher.GCM_TAG_LENGTH); // copy GCM Tag
            out.write(serializedFooter);
            out.write(signature);
            LOG.debug("{}: footer and signature length = {}", out.getPos(), (out.getPos() - footerIndex));
            BytesUtils.writeIntLittleEndian(out, (int) (out.getPos() - footerIndex));
            out.write(MAGIC);
            return;
        }

        // Encrypted file with encrypted footer
        long cryptoFooterIndex = out.getPos();
        writeFileCryptoMetaData(fileEncryptor.getFileCryptoMetaData(), out);
        byte[] footerAAD = AesCipher.createFooterAAD(fileEncryptor.getFileAAD());
        writeFileMetaData(parquetMetadata, out, fileEncryptor.getFooterEncryptor(), footerAAD);
        int combinedMetaDataLength = (int)(out.getPos() - cryptoFooterIndex);
        LOG.debug("{}: crypto metadata and footer length = {}", out.getPos(), combinedMetaDataLength);
        BytesUtils.writeIntLittleEndian(out, combinedMetaDataLength);
        out.write(EFMAGIC);
    }

    public ParquetMetadata getFooter() {
        Preconditions.checkState(state == STATE.ENDED, "Cannot return unfinished footer.");
        return footer;
    }

    /**
     * Given a list of metadata files, merge them into a single ParquetMetadata
     * Requires that the schemas be compatible, and the extraMetadata be exactly equal.
     * @param files a list of files to merge metadata from
     * @param conf a configuration
     * @return merged parquet metadata for the files
     * @throws IOException if there is an error while writing
     * @deprecated metadata files are not recommended and will be removed in 2.0.0
     */
    @Deprecated
    public static ParquetMetadata mergeMetadataFiles(List<Path> files,  Configuration conf) throws IOException {
        return mergeMetadataFiles(files, conf, new StrictKeyValueMetadataMergeStrategy());
    }

    /**
     * Given a list of metadata files, merge them into a single ParquetMetadata
     * Requires that the schemas be compatible, and the extraMetadata be exactly equal.
     * @param files a list of files to merge metadata from
     * @param conf a configuration
     * @param keyValueMetadataMergeStrategy strategy to merge values for same key, if there are multiple
     * @return merged parquet metadata for the files
     * @throws IOException if there is an error while writing
     * @deprecated metadata files are not recommended and will be removed in 2.0.0
     */
    @Deprecated
    public static ParquetMetadata mergeMetadataFiles(List<Path> files, Configuration conf,
                                                     KeyValueMetadataMergeStrategy keyValueMetadataMergeStrategy) throws IOException {
        Preconditions.checkArgument(!files.isEmpty(), "Cannot merge an empty list of metadata");

        GlobalMetaData globalMetaData = null;
        List<BlockMetaData> blocks = new ArrayList<BlockMetaData>();

        for (Path p : files) {
            ParquetMetadata pmd = ParquetFileReader.readFooter(conf, p, ParquetMetadataConverter.NO_FILTER);
            FileMetaData fmd = pmd.getFileMetaData();
            globalMetaData = mergeInto(fmd, globalMetaData, true);
            blocks.addAll(pmd.getBlocks());
        }

        // collapse GlobalMetaData into a single FileMetaData, which will throw if they are not compatible
        return new ParquetMetadata(globalMetaData.merge(keyValueMetadataMergeStrategy), blocks);
    }

    /**
     * Given a list of metadata files, merge them into a single metadata file.
     * Requires that the schemas be compatible, and the extraMetaData be exactly equal.
     * This is useful when merging 2 directories of parquet files into a single directory, as long
     * as both directories were written with compatible schemas and equal extraMetaData.
     * @param files a list of files to merge metadata from
     * @param outputPath path to write merged metadata to
     * @param conf a configuration
     * @throws IOException if there is an error while reading or writing
     * @deprecated metadata files are not recommended and will be removed in 2.0.0
     */
    @Deprecated
    public static void writeMergedMetadataFile(List<Path> files, Path outputPath, Configuration conf) throws IOException {
        ParquetMetadata merged = mergeMetadataFiles(files, conf);
        writeMetadataFile(outputPath, merged, outputPath.getFileSystem(conf));
    }

    /**
     * writes a _metadata and _common_metadata file
     * @param configuration the configuration to use to get the FileSystem
     * @param outputPath the directory to write the _metadata file to
     * @param footers the list of footers to merge
     * @throws IOException if there is an error while writing
     * @deprecated metadata files are not recommended and will be removed in 2.0.0
     */
    @Deprecated
    public static void writeMetadataFile(Configuration configuration, Path outputPath, List<Footer> footers) throws IOException {
        writeMetadataFile(configuration, outputPath, footers, JobSummaryLevel.ALL);
    }

    /**
     * writes _common_metadata file, and optionally a _metadata file depending on the {@link JobSummaryLevel} provided
     * @param configuration the configuration to use to get the FileSystem
     * @param outputPath the directory to write the _metadata file to
     * @param footers the list of footers to merge
     * @param level level of summary to write
     * @throws IOException if there is an error while writing
     * @deprecated metadata files are not recommended and will be removed in 2.0.0
     */
    @Deprecated
    public static void writeMetadataFile(Configuration configuration, Path outputPath, List<Footer> footers, JobSummaryLevel level) throws IOException {
        Preconditions.checkArgument(level == JobSummaryLevel.ALL || level == JobSummaryLevel.COMMON_ONLY,
                "Unsupported level: " + level);

        FileSystem fs = outputPath.getFileSystem(configuration);
        outputPath = outputPath.makeQualified(fs);
        ParquetMetadata metadataFooter = mergeFooters(outputPath, footers);

        if (level == JobSummaryLevel.ALL) {
            writeMetadataFile(outputPath, metadataFooter, fs, PARQUET_METADATA_FILE);
        }

        metadataFooter.getBlocks().clear();
        writeMetadataFile(outputPath, metadataFooter, fs, PARQUET_COMMON_METADATA_FILE);
    }

    /**
     * @deprecated metadata files are not recommended and will be removed in 2.0.0
     */
    @Deprecated
    private static void writeMetadataFile(Path outputPathRoot, ParquetMetadata metadataFooter, FileSystem fs, String parquetMetadataFile)
            throws IOException {
        Path metaDataPath = new Path(outputPathRoot, parquetMetadataFile);
        writeMetadataFile(metaDataPath, metadataFooter, fs);
    }

    /**
     * @deprecated metadata files are not recommended and will be removed in 2.0.0
     */
    @Deprecated
    private static void writeMetadataFile(Path outputPath, ParquetMetadata metadataFooter, FileSystem fs)
            throws IOException {
        PositionOutputStream metadata = HadoopStreams.wrap(fs.create(outputPath));
        metadata.write(MAGIC);
        serializeFooter(metadataFooter, metadata, null);
        metadata.close();
    }

    /**
     * Will merge the metadata of all the footers together
     * @param root the directory containing all footers
     * @param footers the list files footers to merge
     * @return the global meta data for all the footers
     */
    static ParquetMetadata mergeFooters(Path root, List<Footer> footers) {
        return mergeFooters(root, footers, new StrictKeyValueMetadataMergeStrategy());
    }

    /**
     * Will merge the metadata of all the footers together
     * @param root the directory containing all footers
     * @param footers the list files footers to merge
     * @param keyValueMergeStrategy strategy to merge values for a given key (if there are multiple values)
     * @return the global meta data for all the footers
     */
    static ParquetMetadata mergeFooters(Path root, List<Footer> footers, KeyValueMetadataMergeStrategy keyValueMergeStrategy) {
        String rootPath = root.toUri().getPath();
        GlobalMetaData fileMetaData = null;
        List<BlockMetaData> blocks = new ArrayList<BlockMetaData>();
        for (Footer footer : footers) {
            String footerPath = footer.getFile().toUri().getPath();
            if (!footerPath.startsWith(rootPath)) {
                throw new ParquetEncodingException(footerPath + " invalid: all the files must be contained in the root " + root);
            }
            footerPath = footerPath.substring(rootPath.length());
            while (footerPath.startsWith("/")) {
                footerPath = footerPath.substring(1);
            }
            fileMetaData = mergeInto(footer.getParquetMetadata().getFileMetaData(), fileMetaData);
            for (BlockMetaData block : footer.getParquetMetadata().getBlocks()) {
                block.setPath(footerPath);
                blocks.add(block);
            }
        }
        return new ParquetMetadata(fileMetaData.merge(keyValueMergeStrategy), blocks);
    }

    /**
     * @return the current position in the underlying file
     * @throws IOException if there is an error while getting the current stream's position
     */
    public long getPos() throws IOException {
        return out.getPos();
    }

    public long getNextRowGroupSize() throws IOException {
        return alignment.nextRowGroupSize(out);
    }

    /**
     * Will merge the metadata of all the footers together
     * @param footers the list files footers to merge
     * @return the global meta data for all the footers
     */
    static GlobalMetaData getGlobalMetaData(List<Footer> footers) {
        return getGlobalMetaData(footers, true);
    }

    static GlobalMetaData getGlobalMetaData(List<Footer> footers, boolean strict) {
        GlobalMetaData fileMetaData = null;
        for (Footer footer : footers) {
            ParquetMetadata currentMetadata = footer.getParquetMetadata();
            fileMetaData = mergeInto(currentMetadata.getFileMetaData(), fileMetaData, strict);
        }
        return fileMetaData;
    }

    /**
     * Will return the result of merging toMerge into mergedMetadata
     * @param toMerge the metadata toMerge
     * @param mergedMetadata the reference metadata to merge into
     * @return the result of the merge
     */
    static GlobalMetaData mergeInto(
            FileMetaData toMerge,
            GlobalMetaData mergedMetadata) {
        return mergeInto(toMerge, mergedMetadata, true);
    }

    static GlobalMetaData mergeInto(
            FileMetaData toMerge,
            GlobalMetaData mergedMetadata,
            boolean strict) {
        MessageType schema = null;
        Map<String, Set<String>> newKeyValues = new HashMap<String, Set<String>>();
        Set<String> createdBy = new HashSet<String>();
        if (mergedMetadata != null) {
            schema = mergedMetadata.getSchema();
            newKeyValues.putAll(mergedMetadata.getKeyValueMetaData());
            createdBy.addAll(mergedMetadata.getCreatedBy());
        }
        if ((schema == null && toMerge.getSchema() != null)
                || (schema != null && !schema.equals(toMerge.getSchema()))) {
            schema = mergeInto(toMerge.getSchema(), schema, strict);
        }
        for (Entry<String, String> entry : toMerge.getKeyValueMetaData().entrySet()) {
            Set<String> values = newKeyValues.get(entry.getKey());
            if (values == null) {
                values = new LinkedHashSet<String>();
                newKeyValues.put(entry.getKey(), values);
            }
            values.add(entry.getValue());
        }
        createdBy.add(toMerge.getCreatedBy());
        return new GlobalMetaData(
                schema,
                newKeyValues,
                createdBy);
    }

    /**
     * will return the result of merging toMerge into mergedSchema
     * @param toMerge the schema to merge into mergedSchema
     * @param mergedSchema the schema to append the fields to
     * @return the resulting schema
     */
    static MessageType mergeInto(MessageType toMerge, MessageType mergedSchema) {
        return mergeInto(toMerge, mergedSchema, true);
    }

    /**
     * will return the result of merging toMerge into mergedSchema
     * @param toMerge the schema to merge into mergedSchema
     * @param mergedSchema the schema to append the fields to
     * @param strict should schema primitive types match
     * @return the resulting schema
     */
    static MessageType mergeInto(MessageType toMerge, MessageType mergedSchema, boolean strict) {
        if (mergedSchema == null) {
            return toMerge;
        }

        return mergedSchema.union(toMerge, strict);
    }

    private interface AlignmentStrategy {
        void alignForRowGroup(PositionOutputStream out) throws IOException;

        long nextRowGroupSize(PositionOutputStream out) throws IOException;
    }

    private static class NoAlignment implements AlignmentStrategy {
        public static NoAlignment get(long rowGroupSize) {
            return new NoAlignment(rowGroupSize);
        }

        private final long rowGroupSize;

        private NoAlignment(long rowGroupSize) {
            this.rowGroupSize = rowGroupSize;
        }

        @Override
        public void alignForRowGroup(PositionOutputStream out) {
        }

        @Override
        public long nextRowGroupSize(PositionOutputStream out) {
            return rowGroupSize;
        }
    }

    /**
     * Alignment strategy that pads when less than half the row group size is
     * left before the next DFS block.
     */
    private static class PaddingAlignment implements AlignmentStrategy {
        private static final byte[] zeros = new byte[4096];

        public static PaddingAlignment get(long dfsBlockSize, long rowGroupSize,
                                           int maxPaddingSize) {
            return new PaddingAlignment(dfsBlockSize, rowGroupSize, maxPaddingSize);
        }

        protected final long dfsBlockSize;
        protected final long rowGroupSize;
        protected final int maxPaddingSize;

        private PaddingAlignment(long dfsBlockSize, long rowGroupSize,
                                 int maxPaddingSize) {
            this.dfsBlockSize = dfsBlockSize;
            this.rowGroupSize = rowGroupSize;
            this.maxPaddingSize = maxPaddingSize;
        }

        @Override
        public void alignForRowGroup(PositionOutputStream out) throws IOException {
            long remaining = dfsBlockSize - (out.getPos() % dfsBlockSize);

            if (isPaddingNeeded(remaining)) {
                LOG.debug("Adding {} bytes of padding (row group size={}B, block size={}B)", remaining, rowGroupSize, dfsBlockSize);
                for (; remaining > 0; remaining -= zeros.length) {
                    out.write(zeros, 0, (int) Math.min((long) zeros.length, remaining));
                }
            }
        }

        @Override
        public long nextRowGroupSize(PositionOutputStream out) throws IOException {
            if (maxPaddingSize <= 0) {
                return rowGroupSize;
            }

            long remaining = dfsBlockSize - (out.getPos() % dfsBlockSize);

            if (isPaddingNeeded(remaining)) {
                return rowGroupSize;
            }

            return Math.min(remaining, rowGroupSize);
        }

        protected boolean isPaddingNeeded(long remaining) {
            return (remaining <= maxPaddingSize);
        }
    }
}
