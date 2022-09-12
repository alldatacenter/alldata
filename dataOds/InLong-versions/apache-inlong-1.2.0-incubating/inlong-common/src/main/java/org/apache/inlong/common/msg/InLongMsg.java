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

package org.apache.inlong.common.msg;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.xerial.snappy.Snappy;

public class InLongMsg {
    private static final int DEFAULT_CAPACITY = 4096;
    private final int capacity;

    private static final int BIN_MSG_NO_ZIP = 0;
    private static final int BIN_MSG_SNAPPY_TYPE = 1;

    private static final int BIN_MSG_TOTALLEN_OFFSET = 0;
    private static final int BIN_MSG_GROUPID_OFFSET = 5;
    private static final int BIN_MSG_STREAMID_OFFSET = 7;
    private static final int BIN_MSG_EXTFIELD_OFFSET = 9;
    private static final int BIN_MSG_COUNT_OFFSET = 15;
    private static final int BIN_MSG_DATATIME_OFFSET = 11;
    private static final int BIN_MSG_TOTALLEN_SIZE = 4;
    private static final int BIN_MSG_MSGTYPE_OFFSET = 4;
    private static final int BIN_MSG_SET_SNAPPY = (1 << 5);
    private static final int BIN_MSG_BODYLEN_SIZE = 4;
    private static final int BIN_MSG_BODYLEN_OFFSET = 21;
    private static final int BIN_MSG_BODY_OFFSET =
            BIN_MSG_BODYLEN_SIZE + BIN_MSG_BODYLEN_OFFSET;
    private static final int BIN_MSG_ATTRLEN_SIZE = 2;
    private static final int BIN_MSG_FORMAT_SIZE = 29;
    private static final int BIN_MSG_MAGIC_SIZE = 2;
    private static final int BIN_MSG_MAGIC = 0xEE01;

    private static final byte[] MAGIC0 = {(byte) 0xf, (byte) 0x0};
    // with timestamp
    private static final byte[] MAGIC1 = {(byte) 0xf, (byte) 0x1};
    // with msg cnt 20130619
    private static final byte[] MAGIC2 = {(byte) 0xf, (byte) 0x2};
    // support msg_type = 6
    private static final byte[] MAGIC3 = {(byte) 0xf, (byte) 0x3};
    // support binmsg
    private static final byte[] MAGIC4 = {(byte) 0xf, (byte) 0x4};

    private final boolean addmode;

    private static final Joiner.MapJoiner MAP_JOINER =
            Joiner.on(AttributeConstants.SEPARATOR)
                    .withKeyValueSeparator(AttributeConstants.KEY_VALUE_SEPARATOR);
    private static final Splitter.MapSplitter MAP_SPLITTER =
            Splitter.on(AttributeConstants.SEPARATOR)
                    .trimResults().withKeyValueSeparator(AttributeConstants.KEY_VALUE_SEPARATOR);

    static class DataBuffer {

        DataOutputBuffer out;
        int cnt;

        public DataBuffer() {
            out = new DataOutputBuffer();
        }

        public void write(byte[] array, int position, int len)
                throws IOException {
            cnt++;
            out.writeInt(len);
            out.write(array, position, len);
        }
    }

    private LinkedHashMap<String, DataBuffer> attr2MsgBuffer;
    private ByteBuffer binMsgBuffer;
    private int datalen = 0;
    private int msgcnt = 0;
    private boolean compress;
    private boolean isNumGroupId = false;
    private boolean ischeck = true;

    private final Version version;
    private long timeoffset = 0;

    public void setTimeoffset(long offset) {
        this.timeoffset = offset;
    }

    private enum Version {
        vn(-1), v0(0), v1(1),
        v2(2), v3(3), v4(4);

        private static final Map<Integer, Version> INT_TO_TYPE_MAP =
                new HashMap<Integer, Version>();

        static {
            for (Version type : Version.values()) {
                INT_TO_TYPE_MAP.put(type.value, type);
            }
        }

        private final int value;

        private Version(int value) {
            this.value = value;
        }

        public int intValue() {
            return value;
        }

        public static Version of(int v) {
            if (!INT_TO_TYPE_MAP.containsKey(v)) {
                return vn;
            }
            return INT_TO_TYPE_MAP.get(v);
        }

    }

    /**
     * capacity: 4096, compress: true, version: 1
     *
     * @return
     */
    public static InLongMsg newInLongMsg() {
        return newInLongMsg(true);
    }

    /**
     * capacity: 4096, version: 1
     *
     * @param compress if copress
     * @return InLongMsg
     */
    public static InLongMsg newInLongMsg(boolean compress) {
        return newInLongMsg(DEFAULT_CAPACITY, compress);
    }

    /**
     * capacity: 4096, compress: true
     *
     * @param v version info
     * @return InLongMsg
     */
    public static InLongMsg newInLongMsg(int v) {
        return newInLongMsg(DEFAULT_CAPACITY, true, v);
    }

    /**
     * capacity: 4096
     *
     * @param compress if compress
     * @param v        version
     * @return InLongMsg
     */
    public static InLongMsg newInLongMsg(boolean compress, int v) {
        return newInLongMsg(DEFAULT_CAPACITY, compress, v);
    }

    /**
     * version: 1
     *
     * @param capacity data capacity
     * @param compress if compress
     * @return InLongMsg
     */
    public static InLongMsg newInLongMsg(int capacity, boolean compress) {
        return new InLongMsg(capacity, compress, Version.v1);
    }

    /**
     * netInLongMsg
     * @param capacity data capacity
     * @param compress compress
     * @param v        version
     * @return InLongMsg
     */
    public static InLongMsg newInLongMsg(int capacity, boolean compress, int v) {
        return new InLongMsg(capacity, compress, Version.of(v));
    }

    // for create
    private InLongMsg(int capacity, boolean compress, Version v) {
        version = v;
        addmode = true;
        this.compress = compress;
        this.capacity = capacity;
        attr2MsgBuffer = new LinkedHashMap<String, DataBuffer>();
        parsedInput = null;
        reset();
    }

    /**
     * return false means current msg is big enough, no other data should be
     * added again, but attention: the input data has already been added, and if
     * you add another data after return false it can also be added
     * successfully.
     *
     * @param attr   attribute info
     * @param data   binary data
     * @param offset data start offset
     * @param len    data length
     * @return boolean
     */
    public boolean addMsg(String attr, byte[] data, int offset, int len) {
        return addMsg(attr, ByteBuffer.wrap(data, offset, len));
    }

    /**
     * add msg
     * @param attr
     * @param data
     * @return
     */
    public boolean addMsg(String attr, ByteBuffer data) {
        checkMode(true);

        if ((version.intValue() == Version.v3.intValue())
                && !checkData(data)) {
            return false;
        }

        DataBuffer outputBuffer = attr2MsgBuffer.get(attr);
        if (outputBuffer == null) {
            outputBuffer = new DataBuffer();
            attr2MsgBuffer.put(attr, outputBuffer);
            // attrlen + utflen + meglen + compress
            this.datalen += attr.length() + 2 + 4 + 1;
        }

        int len = data.remaining();
        try {
            outputBuffer.write(data.array(), data.position(), len);
            this.datalen += len + 4;
            if (version.intValue() == Version.v2.intValue()) {
                this.datalen += 4;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        msgcnt++;
        return checkLen(attr, len);
    }

    public boolean addMsg(String attr, byte[] data) {
        return addMsg(attr, ByteBuffer.wrap(data));
    }

    public boolean addMsg(byte[] data) {
        return addMsg(ByteBuffer.wrap(data));
    }

    public boolean addMsg(ByteBuffer data) {
        if (!checkBinData(data)) {
            return false;
        }

        if (binMsgBuffer != null) {
            return false;
        }

        binMsgBuffer = ByteBuffer.allocate(data.remaining());
        binMsgBuffer.put(data);

        binMsgBuffer.position(BIN_MSG_TOTALLEN_OFFSET);
        msgcnt = getBinMsgCnt(binMsgBuffer);
        return true;
    }

    private int getBinMsgtype(ByteBuffer data) {
        return data.get(BIN_MSG_MSGTYPE_OFFSET);
    }

    private int getBinMsgCnt(ByteBuffer data) {
        return data.getShort(BIN_MSG_COUNT_OFFSET);
    }

    private long getBinCreatetime(ByteBuffer data) {
        return data.getInt(BIN_MSG_DATATIME_OFFSET) * 1000L;
    }

    private boolean getBinNumFlag(ByteBuffer data) {
        return (data.getShort(BIN_MSG_EXTFIELD_OFFSET) & 0x4) == 0;
    }

    private boolean checkBinData(ByteBuffer data) {
        int totalLen = data.getInt(BIN_MSG_TOTALLEN_OFFSET);
        int bodyLen = data.getInt(BIN_MSG_BODYLEN_OFFSET);
        int attrLen = data.getShort(BIN_MSG_BODY_OFFSET + bodyLen);
        int msgMagic = (data.getShort(BIN_MSG_BODY_OFFSET + bodyLen
                + BIN_MSG_ATTRLEN_SIZE + attrLen) & 0xFFFF);

        if ((totalLen + BIN_MSG_TOTALLEN_SIZE != (bodyLen + attrLen + BIN_MSG_FORMAT_SIZE))
                || (msgMagic != BIN_MSG_MAGIC)) {
            return false;
        }

        return true;
    }

    public boolean addMsgs(String attr, ByteBuffer data) {
        boolean res = true;
        Iterator<ByteBuffer> it = getIteratorBuffer(data);
        setCheckMode(false);
        while (it.hasNext()) {
            res = this.addMsg(attr, it.next());
        }
        setCheckMode(true);
        return res;
    }

    private void setCheckMode(boolean mode) {
        if (version.intValue() == Version.v3.intValue()) {
            ischeck = mode;
        }
    }

    // Version 3 message, need check data content
    private boolean checkData(ByteBuffer data) {
        if ((version.intValue() == Version.v3.intValue()) && !ischeck) {
            return true;
        }

        // check data
        data.mark();
        int msgnum = 0;
        while (data.remaining() > 0) {
            int datalen = data.getInt();
            if (datalen > data.remaining()) {
                return false;
            }
            msgnum++;
            byte[] record = new byte[datalen];
            data.get(record, 0, datalen);
        }

        msgnum = msgnum / 2;
        if (msgnum > 1) {
            msgcnt += msgnum - 1;
        }
        data.reset();
        return true;
    }

    private boolean checkLen(String attr, int len) {
        return datalen < capacity;
    }

    public boolean isfull() {
        checkMode(true);
        if (datalen >= capacity) {
            return true;
        }
        return false;
    }

    private ByteBuffer defaultBuild(long createtime) {
        try {
            this.createtime = createtime;
            DataOutputBuffer out = new DataOutputBuffer(capacity);

            writeHeader(out);
            out.writeInt(attr2MsgBuffer.size());

            if (compress) {
                for (String attr : attr2MsgBuffer.keySet()) {
                    out.writeUTF(attr);
                    DataBuffer data = attr2MsgBuffer.get(attr);
                    if (version.intValue() == Version.v2.intValue()) {
                        out.writeInt(data.cnt);
                    }
                    int guessLen =
                            Snappy.maxCompressedLength(data.out.getLength());
                    byte[] tmpData = new byte[guessLen];
                    int len = Snappy.compress(data.out.getData(), 0,
                            data.out.getLength(), tmpData, 0);
                    out.writeInt(len + 1);
                    out.writeBoolean(compress);
                    out.write(tmpData, 0, len);
                }
            } else {
                for (String attr : attr2MsgBuffer.keySet()) {
                    out.writeUTF(attr);
                    DataBuffer data = attr2MsgBuffer.get(attr);
                    if (version.intValue() == Version.v2.intValue()) {
                        out.writeInt(data.cnt);
                    }
                    out.writeInt(data.out.getLength() + 1);
                    out.writeBoolean(compress);
                    out.write(data.out.getData(), 0, data.out.getLength());
                }
            }
            writeMagic(out);
            out.close();
            return ByteBuffer.wrap(out.getData(), 0, out.getLength());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ByteBuffer binBuild(long createtime) {
        try {
            this.createtime = createtime;
            DataOutputBuffer out = new DataOutputBuffer(capacity);

            writeMagic(out);

            int msgType = getBinMsgtype(binMsgBuffer);
            int compressType = ((msgType & 0xE0) >> 5);
            if ((compressType == 0) && (compress)) {

                binMsgBuffer.position(BIN_MSG_BODYLEN_OFFSET);
                // copy body data
                int bodyLen = binMsgBuffer.getInt();
                byte[] body = new byte[bodyLen];
                binMsgBuffer.get(body, 0, bodyLen);

                // copy attributes
                int attrLen =
                        binMsgBuffer.getShort(BIN_MSG_BODY_OFFSET + bodyLen);
                byte[] attr =
                        new byte[BIN_MSG_ATTRLEN_SIZE + attrLen + BIN_MSG_MAGIC_SIZE];
                binMsgBuffer.get(attr, 0, attr.length);

                int guessLen = Snappy.maxCompressedLength(bodyLen);
                byte[] tmpData = new byte[guessLen];
                int realLen = Snappy.compress(body, 0,
                        body.length, tmpData, 0);

                int totalDataLen = binMsgBuffer.getInt(BIN_MSG_TOTALLEN_OFFSET);
                ByteBuffer dataBuf = ByteBuffer.allocate(
                        totalDataLen + BIN_MSG_TOTALLEN_SIZE - body.length + realLen);

                // copy headers
                dataBuf.put(binMsgBuffer.array(), 0, BIN_MSG_BODYLEN_OFFSET);
                // set compress flag
                dataBuf.put(BIN_MSG_MSGTYPE_OFFSET, (byte) (msgType | BIN_MSG_SET_SNAPPY));
                dataBuf.putInt(BIN_MSG_TOTALLEN_OFFSET,
                        realLen + attrLen + BIN_MSG_FORMAT_SIZE - 4);
                // set data length
                dataBuf.putInt(BIN_MSG_BODYLEN_OFFSET, realLen);
                // fill compressed data
                System.arraycopy(tmpData, 0,
                        dataBuf.array(), BIN_MSG_BODY_OFFSET, realLen);
                // fill attributes and MAGIC
                System.arraycopy(attr, 0, dataBuf.array(),
                        BIN_MSG_BODY_OFFSET + realLen, attr.length);

                out.write(dataBuf.array(), 0, dataBuf.capacity());
            } else {
                out.write(binMsgBuffer.array(), 0, binMsgBuffer.capacity());
            }

            writeMagic(out);
            out.close();
            return ByteBuffer.wrap(out.getData(), 0, out.getLength());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ByteBuffer build() {
        return build(System.currentTimeMillis() + timeoffset);
    }

    public ByteBuffer build(long createtime) {
        checkMode(true);
        if (version.intValue() != Version.v4.intValue()) {
            return defaultBuild(createtime);
        } else {
            return binBuild(createtime);
        }
    }

    private void writeHeader(DataOutputBuffer out) throws IOException {
        writeMagic(out);
        if (version.intValue() == Version.v4.intValue()) {
            return;
        }

        if (version.intValue() >= Version.v1.intValue()) {
            // createtime = System.currentTimeMillis() + timeoffset;
            out.writeLong(createtime);
        }
        if (version.intValue() >= Version.v2.intValue()) {
            out.writeInt(this.getMsgCnt());
        }
    }

    private void writeMagic(DataOutputBuffer out) throws IOException {
        if (version == Version.v1) {
            out.write(MAGIC1[0]);
            out.write(MAGIC1[1]);
        } else if (version == Version.v2) {
            out.write(MAGIC2[0]);
            out.write(MAGIC2[1]);
        } else if (version == Version.v3) {
            out.write(MAGIC3[0]);
            out.write(MAGIC3[1]);
        } else if (version == Version.v4) {
            out.write(MAGIC4[0]);
            out.write(MAGIC4[1]);
        } else {
            throw new IOException("wrong version : " + version.intValue());
        }
    }

    public byte[] buildArray() {
        return buildArray(System.currentTimeMillis() + timeoffset);
    }

    public byte[] buildArray(long createtime) {
        ByteBuffer buffer = this.build(createtime);
        if (buffer == null) {
            return null;
        }
        byte[] res = new byte[buffer.remaining()];
        System.arraycopy(buffer.array(), buffer.position(), res, 0, res.length);
        return res;
    }

    public void reset() {
        checkMode(true);
        this.attr2MsgBuffer.clear();
        this.datalen = getHeaderLen();
        msgcnt = 0;
    }

    private int getHeaderLen() {
        int len = 4; // magic
        if (version.intValue() >= Version.v1.intValue()) {
            len += 8; // create time
        }
        if (version.intValue() == Version.v2.intValue()) {
            len += 4; // msgcnt
        }

        return len + 4; // attrcnt
    }

    // for both mode
    public int getMsgCnt() {
        return msgcnt;
    }

    public int getMsgCnt(String attr) {
        if (addmode) {
            return this.attr2MsgBuffer.get(attr).cnt;
        } else {
            return this.attr2Rawdata.get(attr).cnt;
        }
    }

    private void checkMode(boolean add) {
        if (addmode != add) {
            throw new RuntimeException(
                    addmode ? "illegal operation in add mode !!!"
                            : "illegal operation in parse mode !!!");
        }
    }

    private int attrcnt = -1;

    // private LinkedHashMap<String, ByteBuffer> attr2Rawdata = null;
    static class DataByteBuffer {
        final int cnt;
        ByteBuffer buffer;
        DataOutputBuffer inoutBuffer;

        public DataByteBuffer(int cnt, ByteBuffer buffer) {
            this.cnt = cnt;
            this.buffer = buffer;
        }

        public DataByteBuffer(int cnt, DataOutputBuffer inoutbuffer) {
            this.cnt = cnt;
            this.inoutBuffer = inoutbuffer;
        }

        public void syncByteBuffer() {
            this.buffer = ByteBuffer.wrap(inoutBuffer.getData(), 0, inoutBuffer.getLength());
        }
    }

    private LinkedHashMap<String, DataByteBuffer> attr2Rawdata = null;

    // not used right now
    // private LinkedHashMap<String, Integer> attr2index = null;
    private long createtime = -1;
    private boolean parsed = false;
    private DataInputBuffer parsedInput;
    private ByteBuffer parsedBinInput;

    // for parsed
    private InLongMsg(ByteBuffer buffer, Version magic) throws IOException {
        version = magic;
        addmode = false;
        capacity = 0;

        if (version.intValue() != Version.v4.intValue()) {
            parsedInput = new DataInputBuffer();
            parsedInput.reset(buffer.array(), buffer.position() + 2,
                    buffer.remaining());
            if (version.intValue() >= Version.v1.intValue()) {
                createtime = parsedInput.readLong();
            }

            if (version.intValue() >= Version.v2.intValue()) {
                this.msgcnt = parsedInput.readInt();
            }

            attrcnt = parsedInput.readInt();
        } else {
            byte[] binMsg = new byte[buffer.remaining() - 2];
            System.arraycopy(buffer.array(),
                    buffer.position() + 2, binMsg, 0, binMsg.length);
            parsedBinInput = ByteBuffer.wrap(binMsg);
            this.createtime = getBinCreatetime(parsedBinInput);
            this.msgcnt = getBinMsgCnt(parsedBinInput);
            this.isNumGroupId = getBinNumFlag(parsedBinInput);
        }
    }

    private void parseDefault() throws IOException {
        attr2Rawdata = new LinkedHashMap<String, DataByteBuffer>(
                attrcnt * 10 / 7);
        for (int i = 0; i < attrcnt; i++) {
            String attr = parsedInput.readUTF();
            int cnt = 0;
            if (version.intValue() == Version.v2.intValue()) {
                cnt = parsedInput.readInt();
            }
            int len = parsedInput.readInt();
            int pos = parsedInput.getPosition();
            attr2Rawdata.put(
                    attr,
                    new DataByteBuffer(cnt, ByteBuffer.wrap(
                            parsedInput.getData(), pos, len)));
            parsedInput.skip(len);
        }
    }

    private void parseMixAttr() throws IOException {
        attr2Rawdata = new LinkedHashMap<String, DataByteBuffer>(
                this.msgcnt * 10 / 7);

        for (int i = 0; i < attrcnt; i++) {
            ByteBuffer bodyBuffer;
            String commonAttr = parsedInput.readUTF();
            int len = parsedInput.readInt();
            int compress = parsedInput.readByte();
            int pos = parsedInput.getPosition();

            if (compress == 1) {
                byte[] uncompressdata = new byte[Snappy.uncompressedLength(
                        parsedInput.getData(), pos, len - 1)];
                int msgLen = Snappy.uncompress(parsedInput.getData(), pos, len - 1,
                        uncompressdata, 0);
                bodyBuffer = ByteBuffer.wrap(uncompressdata, 0, msgLen);
            } else {
                bodyBuffer = ByteBuffer.wrap(parsedInput.getData(), pos, len - 1);
            }
            parsedInput.skip(len - 1);

            while (bodyBuffer.remaining() > 0) {
                // total message length = (data length + attributes length) * N
                int singleTotalLen = bodyBuffer.getInt();
                if (singleTotalLen > bodyBuffer.remaining()) {
                    return;
                }

                while (singleTotalLen > 0) {
                    // single data length
                    int msgItemLen = bodyBuffer.getInt();
                    if (msgItemLen <= 0 || msgItemLen > singleTotalLen) {
                        return;
                    }

                    byte[] record = new byte[1 + 4 + msgItemLen];
                    record[0] = 0;
                    record[1] = (byte) ((msgItemLen >> 24) & 0xFF);
                    record[2] = (byte) ((msgItemLen >> 16) & 0xFF);
                    record[3] = (byte) ((msgItemLen >> 8) & 0xFF);
                    record[4] = (byte) (msgItemLen & 0xFF);
                    bodyBuffer.get(record, 1 + 4, msgItemLen);

                    // single attribute length
                    int singleAttrLen = bodyBuffer.getInt();
                    if (singleAttrLen <= 0 || singleAttrLen > singleTotalLen) {
                        return;
                    }
                    byte[] attrBuf = new byte[singleAttrLen];
                    bodyBuffer.get(attrBuf, 0, singleAttrLen);
                    String finalAttr = commonAttr + "&" + new String(attrBuf);

                    DataByteBuffer inputBuffer = attr2Rawdata.get(finalAttr);
                    if (inputBuffer == null) {
                        inputBuffer = new DataByteBuffer(0,
                                new DataOutputBuffer(msgItemLen + 4 + 1));
                        attr2Rawdata.put(finalAttr, inputBuffer);
                        inputBuffer.inoutBuffer.write(record, 0, msgItemLen + 4 + 1);
                    } else {
                        inputBuffer.inoutBuffer.write(record, 1, msgItemLen + 4);
                    }
                    singleTotalLen = singleTotalLen - msgItemLen - singleAttrLen - 8;
                }
            }
        }

        // sync data
        for (String attr : attr2Rawdata.keySet()) {
            DataByteBuffer data = attr2Rawdata.get(attr);
            data.syncByteBuffer();
        }
    }

    private void parseBinMsg() throws IOException {
        Map<String, String> commonAttrMap = new HashMap<String, String>();

        int totalLen = parsedBinInput.getInt(BIN_MSG_TOTALLEN_OFFSET);
        final int msgtype = parsedBinInput.get(BIN_MSG_MSGTYPE_OFFSET);
        int groupIdNum = parsedBinInput.getShort(BIN_MSG_GROUPID_OFFSET);
        int streamIdNum = parsedBinInput.getShort(BIN_MSG_STREAMID_OFFSET);
        int bodyLen = parsedBinInput.getInt(BIN_MSG_BODYLEN_OFFSET);
        long dataTime = parsedBinInput.getInt(BIN_MSG_DATATIME_OFFSET);
        final int extField = parsedBinInput.getShort(BIN_MSG_EXTFIELD_OFFSET);
        int attrLen = parsedBinInput.getShort(BIN_MSG_BODY_OFFSET + bodyLen);
        int msgMagic = (parsedBinInput.getShort(BIN_MSG_BODY_OFFSET
                + bodyLen + BIN_MSG_ATTRLEN_SIZE + attrLen) & 0xFFFF);
        dataTime = dataTime * 1000;

        //read common attributes
        if (attrLen != 0) {
            byte[] attr = new byte[attrLen];
            parsedBinInput.position(BIN_MSG_BODY_OFFSET + bodyLen + BIN_MSG_ATTRLEN_SIZE);
            parsedBinInput.get(attr);
            String strAttr = new String(attr);

            commonAttrMap = new HashMap<String, String>(MAP_SPLITTER.split(strAttr));
        }

        commonAttrMap.put(AttributeConstants.DATA_TIME, String.valueOf(dataTime));

        //unzip data
        ByteBuffer bodyBuffer;
        byte[] body = new byte[bodyLen + 1];
        parsedBinInput.position(BIN_MSG_BODY_OFFSET);
        parsedBinInput.get(body, 1, bodyLen);
        int zipType = (msgtype & 0xE0) >> 5;
        switch (zipType) {
            case (BIN_MSG_SNAPPY_TYPE):
                byte[] uncompressdata =
                        new byte[Snappy.uncompressedLength(body, 1, body.length - 1) + 1];
                // uncompress flag
                uncompressdata[0] = 0;
                int msgLen = Snappy.uncompress(body, 1, body.length - 1,
                        uncompressdata, 1);
                bodyBuffer = ByteBuffer.wrap(uncompressdata, 0, msgLen + 1);
                break;

            case (BIN_MSG_NO_ZIP):
            default:
                //set uncompress flag
                body[0] = 0;
                bodyBuffer = ByteBuffer.wrap(body, 0, body.length);
                break;
        }

        //number groupId/streamId
        boolean isUseNumGroupId = ((extField & 0x4) == 0x0);
        if (isUseNumGroupId) {
            commonAttrMap.put(AttributeConstants.GROUP_ID, String.valueOf(groupIdNum));
            commonAttrMap.put(AttributeConstants.INTERFACE_ID, String.valueOf(streamIdNum));
        }

        boolean hasOtherAttr = ((extField & 0x1) == 0x1);
        commonAttrMap.put(AttributeConstants.MESSAGE_COUNT, "1");
        // with private attributes,
        // need to splice private attributes + public attributes
        if (!hasOtherAttr) {
            // general attributes and data map
            attr2Rawdata = new LinkedHashMap<String, DataByteBuffer>();
            attr2Rawdata.put(MAP_JOINER.join(commonAttrMap),
                    new DataByteBuffer(0, bodyBuffer));
        } else {
            attr2Rawdata = new LinkedHashMap<String, DataByteBuffer>(
                    this.msgcnt * 10 / 7);
            Map<String, String> finalAttrMap = commonAttrMap;

            //skip compress flag
            bodyBuffer.get();
            int bodyBufLen = bodyBuffer.capacity() - 1;
            while (bodyBufLen > 0) {
                // get single message length
                int singleMsgLen = bodyBuffer.getInt();
                if (singleMsgLen <= 0 || singleMsgLen > bodyBufLen) {
                    return;
                }

                byte[] record = new byte[1 + 4 + singleMsgLen];
                record[0] = 0;
                record[1] = (byte) ((singleMsgLen >> 24) & 0xFF);
                record[2] = (byte) ((singleMsgLen >> 16) & 0xFF);
                record[3] = (byte) ((singleMsgLen >> 8) & 0xFF);
                record[4] = (byte) (singleMsgLen & 0xFF);
                bodyBuffer.get(record, 1 + 4, singleMsgLen);

                // get single attribute length
                int singleAttrLen = bodyBuffer.getInt();
                if (singleAttrLen <= 0 || singleAttrLen > bodyBufLen) {
                    return;
                }
                byte[] attrBuf = new byte[singleAttrLen];
                bodyBuffer.get(attrBuf, 0, singleAttrLen);
                String attrBufStr = new String(attrBuf);

                finalAttrMap = new HashMap<String, String>(MAP_SPLITTER.split(attrBufStr));
                finalAttrMap.putAll(commonAttrMap);

                DataByteBuffer inputBuffer = attr2Rawdata.get(MAP_JOINER.join(finalAttrMap));
                if (inputBuffer == null) {
                    inputBuffer = new DataByteBuffer(0,
                            new DataOutputBuffer(singleMsgLen + 4 + 1));
                    attr2Rawdata.put(MAP_JOINER.join(finalAttrMap), inputBuffer);
                    inputBuffer.inoutBuffer.write(record, 0, singleMsgLen + 4 + 1);
                } else {
                    inputBuffer.inoutBuffer.write(record, 1, singleMsgLen + 4);
                }

                bodyBufLen = bodyBufLen - singleMsgLen - singleAttrLen - 8;
            }

            // sync data
            for (String attr : attr2Rawdata.keySet()) {
                DataByteBuffer data = attr2Rawdata.get(attr);
                data.syncByteBuffer();
            }
        }
    }

    private void parse() throws IOException {
        if (parsed) {
            return;
        }

        if (version.intValue() < Version.v3.intValue()) {
            parseDefault();
        } else if (version.intValue() == Version.v3.intValue()) {
            parseMixAttr();
        } else {
            parseBinMsg();
        }

        parsed = true;
    }

    private static Version getMagic(ByteBuffer buffer) {
        // #lizard forgives
        byte[] array = buffer.array();
        if (buffer.remaining() < 4) {
            return Version.vn;
        }
        int pos = buffer.position();
        int rem = buffer.remaining();
        if (array[pos] == MAGIC1[0] && array[pos + 1] == MAGIC1[1]
                && array[pos + rem - 2] == MAGIC1[0]
                && array[pos + rem - 1] == MAGIC1[1]) {
            return Version.v1;
        }
        if (array[pos] == MAGIC2[0] && array[pos + 1] == MAGIC2[1]
                && array[pos + rem - 2] == MAGIC2[0]
                && array[pos + rem - 1] == MAGIC2[1]) {
            return Version.v2;
        }
        if (array[pos] == MAGIC3[0] && array[pos + 1] == MAGIC3[1]
                && array[pos + rem - 2] == MAGIC3[0]
                && array[pos + rem - 1] == MAGIC3[1]) {
            return Version.v3;
        }
        if (array[pos] == MAGIC4[0] && array[pos + 1] == MAGIC4[1]
                && array[pos + rem - 2] == MAGIC4[0]
                && array[pos + rem - 1] == MAGIC4[1]) {
            return Version.v4;
        }
        if (array[pos] == MAGIC0[0] && array[pos + 1] == MAGIC0[1]
                && array[pos + rem - 2] == MAGIC0[0]
                && array[pos + rem - 1] == MAGIC0[1]) {
            return Version.v0;
        }
        return Version.vn;
    }

    public static InLongMsg parseFrom(byte[] data) {
        return parseFrom(ByteBuffer.wrap(data));
    }

    public static InLongMsg parseFrom(ByteBuffer buffer) {
        Version magic = getMagic(buffer);
        if (magic == Version.vn) {
            return null;
        }

        try {
            return new InLongMsg(buffer, magic);
        } catch (IOException e) {
            return null;
        }
    }

    private void makeSureParsed() {
        if (!parsed) {
            try {
                parse();
            } catch (IOException e) {
                //
            }
        }
    }

    public Set<String> getAttrs() {
        checkMode(false);
        makeSureParsed();
        return this.attr2Rawdata.keySet();
    }

    public byte[] getRawData(String attr) {
        checkMode(false);
        makeSureParsed();
        ByteBuffer buffer = getRawDataBuffer(attr);
        byte[] data = new byte[buffer.remaining()];
        System.arraycopy(buffer.array(), buffer.position(), data, 0,
                buffer.remaining());
        return data;
    }

    public ByteBuffer getRawDataBuffer(String attr) {
        checkMode(false);
        makeSureParsed();
        return this.attr2Rawdata.get(attr).buffer;
    }

    public Iterator<byte[]> getIterator(String attr) {
        checkMode(false);
        makeSureParsed();
        return getIterator(this.attr2Rawdata.get(attr).buffer);
    }

    public static Iterator<byte[]> getIterator(byte[] rawdata) {
        return getIterator(ByteBuffer.wrap(rawdata));
    }

    /**
     * getIterator
     * @param rawdata
     * @return
     */
    public static Iterator<byte[]> getIterator(ByteBuffer rawdata) {
        try {
            final DataInputBuffer input = new DataInputBuffer();
            byte[] array = rawdata.array();
            int pos = rawdata.position();
            int rem = rawdata.remaining() - 1;
            int compress = array[pos];

            if (compress == 1) {
                byte[] uncompressdata = new byte[Snappy.uncompressedLength(
                        array, pos + 1, rem)];
                int len = Snappy.uncompress(array, pos + 1, rem,
                        uncompressdata, 0);
                input.reset(uncompressdata, len);
            } else {
                input.reset(array, pos + 1, rem);
            }

            return new Iterator<byte[]>() {

                @Override
                public boolean hasNext() {
                    try {
                        return input.available() > 0;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }

                @Override
                public byte[] next() {
                    try {
                        int len;
                        len = input.readInt();
                        byte[] res = new byte[len];
                        input.read(res);
                        return res;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                public void remove() {
                    this.next();
                }
            };
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static Iterator<ByteBuffer> getIteratorBuffer(byte[] rawdata) {
        return getIteratorBuffer(ByteBuffer.wrap(rawdata));
    }

    public Iterator<ByteBuffer> getIteratorBuffer(String attr) {
        checkMode(false);
        makeSureParsed();
        return getIteratorBuffer(this.attr2Rawdata.get(attr).buffer);
    }

    public static Iterator<ByteBuffer> getIteratorBuffer(ByteBuffer rawdata) {

        try {
            final DataInputBuffer input = new DataInputBuffer();
            byte[] array = rawdata.array();
            int pos = rawdata.position();
            int rem = rawdata.remaining() - 1;
            int compress = array[pos];

            if (compress == 1) {
                byte[] uncompressdata = new byte[Snappy.uncompressedLength(
                        array, pos + 1, rem)];
                int len = Snappy.uncompress(array, pos + 1, rem,
                        uncompressdata, 0);
                input.reset(uncompressdata, len);
            } else {
                input.reset(array, pos + 1, rem);
            }

            final byte[] uncompressdata = input.getData();

            return new Iterator<ByteBuffer>() {

                @Override
                public boolean hasNext() {
                    try {
                        return input.available() > 0;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }

                @Override
                public ByteBuffer next() {
                    try {
                        int len = input.readInt();
                        int pos = input.getPosition();
                        input.skip(len);
                        return ByteBuffer.wrap(uncompressdata, pos, len);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                public void remove() {
                    this.next();
                }
            };
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public long getCreatetime() {
        return createtime;
    }

    public int getAttrCount() {
        checkMode(false);
        return attrcnt;
    }

    public boolean isNumGroupId() {
        checkMode(false);
        return isNumGroupId;
    }
}
