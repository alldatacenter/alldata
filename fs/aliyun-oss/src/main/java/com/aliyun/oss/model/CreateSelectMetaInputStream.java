package com.aliyun.oss.model;

import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.event.ProgressListener;
import com.aliyun.oss.model.SelectObjectMetadata.SelectContentMetadataBase;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import static com.aliyun.oss.event.ProgressPublisher.publishSelectProgress;

public class CreateSelectMetaInputStream extends FilterInputStream {
    /**
     * Format of continuous frame
     * |--frame type(4 bytes)--|--payload length(4 bytes)--|--header checksum(4 bytes)--|
     * |--scanned data bytes(8 bytes)--|--payload checksum(4 bytes)--|
     */
    private static final int CONTINUOUS_FRAME_MAGIC = 8388612;

    /**
     * Format of csv end frame
     * |--frame type(4 bytes)--|--payload length(4 bytes)--|--header checksum(4 bytes)--|
     * |--scanned data bytes(8 bytes)--|--total scan size(8 bytes)--|
     * |--status code(4 bytes)--|--total splits count(4 bytes)--|
     * |--total lines(8 bytes)--|--columns count(4 bytes)--|--error message(optional)--|--payload checksum(4 bytes)--|
     */
    private static final int CSV_END_FRAME_MAGIC = 8388614;

    /**
     * Format of json end frame
     * |--frame type(4 bytes)--|--payload length(4 bytes)--|--header checksum(4 bytes)--|
     * |--scanned data bytes(8 bytes)--|--total scan size(8 bytes)--|
     * |--status code(4 bytes)--|--total splits count(4 bytes)--|
     * |--total lines(8 bytes)--|--error message(optional)--|--payload checksum(4 bytes)--|
     */
    private static final int JSON_END_FRAME_MAGIC = 8388615;
    private static final int SELECT_VERSION = 1;
    private static final long DEFAULT_NOTIFICATION_THRESHOLD = 50 * 1024 * 1024;//notify every scanned 50MB

    private long currentFrameOffset;
    private long currentFramePayloadLength;
    private byte[] currentFrameTypeBytes;
    private byte[] currentFramePayloadLengthBytes;
    private byte[] currentFrameHeaderChecksumBytes;
    private byte[] scannedDataBytes;
    private byte[] currentFramePayloadChecksumBytes;
    private boolean finished;
    private ProgressListener selectProgressListener;
    private long nextNotificationScannedSize;
    private CRC32 crc32;
    private SelectContentMetadataBase selectContentMetadataBase;
    private String requestId;
    /**
     * payload checksum is the last 4 bytes in one frame, we use this flag to indicate whether we
     * need read the 4 bytes before we advance to next frame.
     */
    private boolean firstReadFrame;

    public CreateSelectMetaInputStream(InputStream in, SelectContentMetadataBase selectContentMetadataBase, ProgressListener selectProgressListener) {
        super(in);
        currentFrameOffset = 0;
        currentFramePayloadLength = 0;
        currentFrameTypeBytes = new byte[4];
        currentFramePayloadLengthBytes = new byte[4];
        currentFrameHeaderChecksumBytes = new byte[4];
        scannedDataBytes = new byte[8];
        currentFramePayloadChecksumBytes = new byte[4];
        finished = false;
        firstReadFrame = true;
        this.selectContentMetadataBase = selectContentMetadataBase;
        this.selectProgressListener = selectProgressListener;
        this.nextNotificationScannedSize = DEFAULT_NOTIFICATION_THRESHOLD;
        this.crc32 = new CRC32();
        this.crc32.reset();
    }

    private void internalRead(byte[] buf, int off, int len) throws IOException {
        int bytesRead = 0;
        while (bytesRead < len) {
            int bytes = in.read(buf, off + bytesRead, len - bytesRead);
            if (bytes < 0) {
                throw new SelectObjectException(SelectObjectException.INVALID_INPUT_STREAM, "Invalid input stream end found, need another " + (len - bytesRead) + " bytes", requestId);
            }
            bytesRead += bytes;
        }
    }

    private void validateCheckSum(byte[] checksumBytes, CRC32 crc32) throws IOException {
        int currentChecksum = ByteBuffer.wrap(checksumBytes).getInt();
        if (crc32.getValue() != ((long)currentChecksum & 0xffffffffL)) {
            throw new SelectObjectException(SelectObjectException.INVALID_CRC, "Frame crc check failed, actual " + crc32.getValue() + ", expect: " + currentChecksum, requestId);
        }
        crc32.reset();
    }

    private void readFrame() throws IOException {
        while (currentFrameOffset >= currentFramePayloadLength && !finished) {
            if (!firstReadFrame) {
                internalRead(currentFramePayloadChecksumBytes, 0, 4);
                validateCheckSum(currentFramePayloadChecksumBytes, crc32);
            }
            firstReadFrame = false;
            //advance to next frame
            internalRead(currentFrameTypeBytes, 0, 4);
            //first byte is version byte
            if (currentFrameTypeBytes[0] != SELECT_VERSION) {
                throw new SelectObjectException(SelectObjectException.INVALID_SELECT_VERSION, "Invalid select version found " + currentFrameTypeBytes[0] + ", expect: " + SELECT_VERSION, requestId);
            }
            internalRead(currentFramePayloadLengthBytes, 0, 4);
            internalRead(currentFrameHeaderChecksumBytes, 0, 4);
            internalRead(scannedDataBytes, 0, 8);
            crc32.update(scannedDataBytes);

            currentFrameTypeBytes[0] = 0;
            int type = ByteBuffer.wrap(currentFrameTypeBytes).getInt();
            switch (type) {
                case CONTINUOUS_FRAME_MAGIC:
                    //just break, continue
                    break;
                case CSV_END_FRAME_MAGIC:
                case JSON_END_FRAME_MAGIC:
                    currentFramePayloadLength = ByteBuffer.wrap(currentFramePayloadLengthBytes).getInt() - 8;
                    byte[] totalScannedDataSizeBytes = new byte[8];
                    internalRead(totalScannedDataSizeBytes, 0, 8);
                    byte[] statusBytes = new byte[4];
                    internalRead(statusBytes, 0, 4);
                    byte[] splitBytes = new byte[4];
                    internalRead(splitBytes, 0, 4);
                    byte[] totalLineBytes = new byte[8];
                    internalRead(totalLineBytes, 0, 8);
                    byte[] columnBytes = new byte[4];
                    if (type == CSV_END_FRAME_MAGIC) {
                        internalRead(columnBytes, 0, 4);
                    }

                    crc32.update(totalScannedDataSizeBytes);
                    crc32.update(statusBytes);
                    crc32.update(splitBytes);
                    crc32.update(totalLineBytes);
                    if (type == CSV_END_FRAME_MAGIC) {
                        crc32.update(columnBytes);
                    }
                    int status = ByteBuffer.wrap(statusBytes).getInt();
                    int errorMessageSize;
                    if (type == CSV_END_FRAME_MAGIC) {
                        errorMessageSize = (int)(currentFramePayloadLength - 28);
                    } else {
                        errorMessageSize = (int)(currentFramePayloadLength - 24);
                    }

                    String error = "";
                    if (errorMessageSize > 0) {
                        byte[] errorMessageBytes = new byte[errorMessageSize];
                        internalRead(errorMessageBytes, 0, errorMessageSize);
                        error = new String(errorMessageBytes);
                        crc32.update(errorMessageBytes);
                    }

                    finished = true;
                    currentFramePayloadLength = currentFrameOffset;
                    internalRead(currentFramePayloadChecksumBytes, 0, 4);

                    validateCheckSum(currentFramePayloadChecksumBytes, crc32);
                    if (status / 100 != 2) {
                        if (error.contains(".")) {
                            throw new SelectObjectException(error.split("\\.")[0], error.substring(error.indexOf(".") + 1), requestId);
                        } else {
                            // forward compatbility consideration
                            throw new SelectObjectException(error, error, requestId);
                        }
                    }

                    selectContentMetadataBase.withSplits(ByteBuffer.wrap(splitBytes).getInt())
                            .withTotalLines(ByteBuffer.wrap(totalLineBytes).getLong());
                    break;
                default:
                    throw new SelectObjectException(SelectObjectException.INVALID_SELECT_FRAME, "Unsupported frame type " + type + " found", requestId);
            }
            //notify create select meta progress
            ProgressEventType eventType = ProgressEventType.SELECT_SCAN_EVENT;
            if (finished) {
                eventType = ProgressEventType.SELECT_COMPLETED_EVENT;
            }
            long scannedDataSize = ByteBuffer.wrap(scannedDataBytes).getLong();
            if (scannedDataSize >= nextNotificationScannedSize || finished) {
                publishSelectProgress(selectProgressListener, eventType, scannedDataSize);
                nextNotificationScannedSize += DEFAULT_NOTIFICATION_THRESHOLD;
            }
        }
    }

    @Override
    public int read() throws IOException {
        readFrame();
        return -1;
    }

    @Override
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        readFrame();
        return -1;
    }

    @Override
    public int available() throws IOException {
        throw new IOException("Create select meta input stream does not support available() operation");
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
