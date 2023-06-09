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
package org.apache.drill.exec.store.pcap;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.pcap.decoder.Packet;
import org.apache.drill.exec.store.pcap.decoder.PacketDecoder;
import org.apache.drill.exec.store.pcap.decoder.TcpSession;
import org.apache.drill.exec.store.pcap.schema.Schema;
import org.apache.drill.exec.store.pcap.plugin.PcapFormatConfig;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.hadoop.mapred.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.apache.drill.exec.store.pcap.PcapFormatUtils.parseBytesToASCII;

public class PcapBatchReader implements ManagedReader<FileSchemaNegotiator> {

  protected static final int BUFFER_SIZE = 500_000;

  private static final Logger logger = LoggerFactory.getLogger(PcapBatchReader.class);

  private FileSplit split;
  private PacketDecoder decoder;
  private InputStream fsStream;
  private RowSetLoader rowWriter;
  private int validBytes;
  private byte[] buffer;
  private int offset;
  private ScalarWriter typeWriter;
  private ScalarWriter timestampWriter;
  private ScalarWriter timestampMicroWriter;
  private ScalarWriter networkWriter;
  private ScalarWriter srcMacAddressWriter;
  private ScalarWriter dstMacAddressWriter;
  private ScalarWriter dstIPWriter;
  private ScalarWriter srcIPWriter;
  private ScalarWriter srcPortWriter;
  private ScalarWriter dstPortWriter;
  private ScalarWriter packetLengthWriter;
  private ScalarWriter tcpSessionWriter;
  private ScalarWriter tcpSequenceWriter;
  private ScalarWriter tcpAckNumberWriter;
  private ScalarWriter tcpFlagsWriter;
  private ScalarWriter tcpParsedFlagsWriter;
  private ScalarWriter tcpNsWriter;
  private ScalarWriter tcpCwrWriter;
  private ScalarWriter tcpEceWriter;
  private ScalarWriter tcpFlagsEceEcnCapableWriter;
  private ScalarWriter tcpFlagsCongestionWriter;
  private ScalarWriter tcpUrgWriter;
  private ScalarWriter tcpAckWriter;
  private ScalarWriter tcpPshWriter;
  private ScalarWriter tcpRstWriter;
  private ScalarWriter tcpSynWriter;
  private ScalarWriter tcpFinWriter;
  private ScalarWriter dataWriter;
  private ScalarWriter isCorruptWriter;
  private final PcapFormatConfig readerConfig;
  // Writers for TCP Sessions
  private ScalarWriter sessionStartTimeWriter;
  private ScalarWriter sessionEndTimeWriter;
  private ScalarWriter sessionDurationWriter;
  private ScalarWriter connectionTimeWriter;
  private ScalarWriter packetCountWriter;
  private ScalarWriter originPacketCounterWriter;
  private ScalarWriter remotePacketCounterWriter;
  private ScalarWriter originDataVolumeWriter;
  private ScalarWriter remoteDataVolumeWriter;
  private ScalarWriter hostDataWriter;
  private ScalarWriter remoteDataWriter;
  private final int maxRecords;
  private Map<Long, TcpSession> sessionQueue;


  public PcapBatchReader(PcapFormatConfig readerConfig, int maxRecords) {
    this.readerConfig = readerConfig;
    if (readerConfig.getSessionizeTCPStreams()) {
      sessionQueue = new HashMap<>();
    }
    this.maxRecords = maxRecords;
  }

  @Override
  public boolean open(FileSchemaNegotiator negotiator) {
    split = negotiator.split();
    openFile(negotiator);
    SchemaBuilder builder = new SchemaBuilder();
    Schema pcapSchema = new Schema(readerConfig.getSessionizeTCPStreams());
    TupleMetadata schema = pcapSchema.buildSchema(builder);
    negotiator.tableSchema(schema, false);
    ResultSetLoader loader = negotiator.build();

    // Creates writers for all fields (Since schema is known)
    rowWriter = loader.writer();
    populateColumnWriters(rowWriter);

    return true;
  }

  @Override
  public boolean next() {
    while (!rowWriter.isFull()) {
      if (!parseNextPacket(rowWriter)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() {

    /* This warning could occur in the event of a corrupt or incomplete PCAP file. Specifically,
     * if a session is started in one file and the end of the session is not captured in the same file.
     */
    if (sessionQueue != null && !sessionQueue.isEmpty()) {
      logger.warn("Unclosed sessions remaining in PCAP");
    }

    try {
      fsStream.close();
    } catch (IOException e) {
      throw UserException.
        dataReadError()
        .addContext("Error closing InputStream: " + e.getMessage())
        .build(logger);
    }
    fsStream = null;
    buffer = null;
    decoder = null;
  }

  private void openFile(FileSchemaNegotiator negotiator) {
    try {
      fsStream = negotiator.fileSystem().openPossiblyCompressedStream(split.getPath());
      decoder = new PacketDecoder(fsStream);
      buffer = new byte[BUFFER_SIZE + decoder.getMaxLength()];
      validBytes = fsStream.read(buffer);
    } catch (IOException io) {
      throw UserException
        .dataReadError(io)
        .addContext("File name:", split.getPath().toString())
        .build(logger);
    }
  }

  private void populateColumnWriters(RowSetLoader rowWriter) {
    if (readerConfig.getSessionizeTCPStreams()) {
      srcMacAddressWriter = rowWriter.scalar("src_mac_address");
      dstMacAddressWriter = rowWriter.scalar("dst_mac_address");
      dstIPWriter = rowWriter.scalar("dst_ip");
      srcIPWriter = rowWriter.scalar("src_ip");
      srcPortWriter = rowWriter.scalar("src_port");
      dstPortWriter = rowWriter.scalar("dst_port");
      sessionStartTimeWriter = rowWriter.scalar("session_start_time");
      sessionEndTimeWriter = rowWriter.scalar("session_end_time");
      sessionDurationWriter = rowWriter.scalar("session_duration");
      connectionTimeWriter = rowWriter.scalar("connection_time");
      tcpSessionWriter = rowWriter.scalar("tcp_session");
      packetCountWriter = rowWriter.scalar("total_packet_count");
      hostDataWriter = rowWriter.scalar("data_from_originator");
      remoteDataWriter = rowWriter.scalar("data_from_remote");

      originPacketCounterWriter = rowWriter.scalar("packet_count_from_origin");
      remotePacketCounterWriter = rowWriter.scalar("packet_count_from_remote");
      originDataVolumeWriter = rowWriter.scalar("data_volume_from_origin");
      remoteDataVolumeWriter = rowWriter.scalar("data_volume_from_remote");
      isCorruptWriter = rowWriter.scalar("is_corrupt");

    } else {
      typeWriter = rowWriter.scalar("type");
      timestampWriter = rowWriter.scalar("packet_timestamp");
      timestampMicroWriter = rowWriter.scalar("timestamp_micro");
      networkWriter = rowWriter.scalar("network");
      srcMacAddressWriter = rowWriter.scalar("src_mac_address");
      dstMacAddressWriter = rowWriter.scalar("dst_mac_address");
      dstIPWriter = rowWriter.scalar("dst_ip");
      srcIPWriter = rowWriter.scalar("src_ip");
      srcPortWriter = rowWriter.scalar("src_port");
      dstPortWriter = rowWriter.scalar("dst_port");
      packetLengthWriter = rowWriter.scalar("packet_length");

      //Writers for TCP Packets
      tcpSessionWriter = rowWriter.scalar("tcp_session");
      tcpSequenceWriter = rowWriter.scalar("tcp_sequence");
      tcpAckNumberWriter = rowWriter.scalar("tcp_ack");
      tcpFlagsWriter = rowWriter.scalar("tcp_flags");
      tcpParsedFlagsWriter = rowWriter.scalar("tcp_parsed_flags");
      tcpNsWriter = rowWriter.scalar("tcp_flags_ns");
      tcpCwrWriter = rowWriter.scalar("tcp_flags_cwr");
      tcpEceWriter = rowWriter.scalar("tcp_flags_ece");
      tcpFlagsEceEcnCapableWriter = rowWriter.scalar("tcp_flags_ece_ecn_capable");
      tcpFlagsCongestionWriter = rowWriter.scalar("tcp_flags_ece_congestion_experienced");

      tcpUrgWriter = rowWriter.scalar("tcp_flags_urg");
      tcpAckWriter = rowWriter.scalar("tcp_flags_ack");
      tcpPshWriter = rowWriter.scalar("tcp_flags_psh");
      tcpRstWriter = rowWriter.scalar("tcp_flags_rst");
      tcpSynWriter = rowWriter.scalar("tcp_flags_syn");
      tcpFinWriter = rowWriter.scalar("tcp_flags_fin");

      dataWriter = rowWriter.scalar("data");
      isCorruptWriter = rowWriter.scalar("is_corrupt");
    }
  }

  private boolean parseNextPacket(RowSetLoader rowWriter) {

    // Push down limit
    if (rowWriter.limitReached(maxRecords)) {
      return false;
    }

    // Decode the packet
    Packet packet = new Packet();

    if (offset >= validBytes) {
      return false;
    }
    if (validBytes - offset < decoder.getMaxLength()) {
      getNextPacket(rowWriter);
    }

    int old = offset;
    offset = decoder.decodePacket(buffer, offset, packet, decoder.getMaxLength(), validBytes);
    if (offset > validBytes) {
      // Mark that the packet is corrupt and extract whatever data can be extracted from it
      packet.setIsCorrupt(true);
      logger.debug("Invalid packet at offset {}", old);
    }

    // If we are resessionizing the TCP Stream, add the packet to the stream
    if (readerConfig.getSessionizeTCPStreams()) {
      // If the session has not been seen before, add it to the queue
      long sessionID = packet.getSessionHash();
      if (!sessionQueue.containsKey(sessionID)) {
        logger.debug("Adding session {} to session queue.", sessionID);
        sessionQueue.put(sessionID, new TcpSession(sessionID));
      }

      // When the session is closed, write it and remove it from the session queue.
      sessionQueue.get(sessionID).addPacket(packet);
      if (sessionQueue.get(sessionID).connectionClosed()) {
        // Write out the session
        addSessionDataToTable(sessionQueue.get(sessionID), rowWriter);
        // Remove from the queue
        sessionQueue.remove(sessionID);
      }

    } else {
      addDataToTable(packet, decoder.getNetwork(), rowWriter);
    }
    return true;
  }

  private boolean getNextPacket(RowSetLoader rowWriter) {
    Packet packet = new Packet();
    try {
      if (validBytes == buffer.length) {
        // shift data and read more. This is the common case.
        System.arraycopy(buffer, offset, buffer, 0, validBytes - offset);
        validBytes = validBytes - offset;
        offset = 0;

        int n = fsStream.read(buffer, validBytes, buffer.length - validBytes);
        if (n > 0) {
          validBytes += n;
        }
        logger.debug("read {} bytes, at {} offset", n, validBytes);
      } else {
        // near the end of the file, we will just top up the buffer without shifting
        int n = fsStream.read(buffer, offset, buffer.length - offset);
        if (n > 0) {
          validBytes = validBytes + n;
          logger.debug("Topped up buffer with {} bytes to yield {}", n, validBytes);
        }
      }
    } catch (Exception e) {
      // Exception denotes EOF or unreadable file
      return false;
    }
    return true;
  }

  private void addSessionDataToTable(TcpSession session, RowSetLoader rowWriter) {
    rowWriter.start();

    sessionStartTimeWriter.setTimestamp(session.getSessionStartTime());
    sessionEndTimeWriter.setTimestamp(session.getSessionEndTime());
    sessionDurationWriter.setPeriod(session.getSessionDuration());
    connectionTimeWriter.setPeriod(session.getConnectionTime());

    srcMacAddressWriter.setString(session.getSrcMac());
    dstMacAddressWriter.setString(session.getDstMac());
    srcIPWriter.setString(session.getSrcIP());
    dstIPWriter.setString(session.getDstIP());
    srcPortWriter.setInt(session.getSrcPort());
    dstPortWriter.setInt(session.getDstPort());
    tcpSessionWriter.setLong(session.getSessionID());
    packetCountWriter.setInt(session.getPacketCount());

    originPacketCounterWriter.setInt(session.getPacketCountFromOrigin());
    remotePacketCounterWriter.setInt(session.getPacketCountFromRemote());
    originDataVolumeWriter.setInt(session.getDataFromOriginator().length);
    remoteDataVolumeWriter.setInt(session.getDataFromRemote().length);
    isCorruptWriter.setBoolean(session.hasCorruptedData());

    hostDataWriter.setString(session.getDataFromOriginatorAsString());
    remoteDataWriter.setString(session.getDataFromRemoteAsString());
    rowWriter.save();
  }

  private void addDataToTable(Packet packet, int networkType, RowSetLoader rowWriter) {
    rowWriter.start();

    typeWriter.setString(packet.getPacketType());
    timestampWriter.setTimestamp(Instant.ofEpochMilli(packet.getTimestamp()));
    timestampMicroWriter.setLong(packet.getTimestampMicro());
    networkWriter.setInt(networkType);
    srcMacAddressWriter.setString(packet.getEthernetSource());
    dstMacAddressWriter.setString(packet.getEthernetDestination());

    String destinationIp = packet.getDestinationIpAddressString();
    if (destinationIp == null) {
      dstIPWriter.setNull();
    } else {
      dstIPWriter.setString(destinationIp);
    }

    String sourceIp = packet.getSourceIpAddressString();
    if (sourceIp == null) {
      srcIPWriter.setNull();
    } else {
      srcIPWriter.setString(sourceIp);
    }
    srcPortWriter.setInt(packet.getSrc_port());
    dstPortWriter.setInt(packet.getDst_port());
    packetLengthWriter.setInt(packet.getPacketLength());

    // TCP Only Packet Data
    tcpSessionWriter.setLong(packet.getSessionHash());
    tcpSequenceWriter.setInt(packet.getSequenceNumber());
    tcpAckNumberWriter.setInt(packet.getAckNumber());
    tcpFlagsWriter.setInt(packet.getFlags());
    tcpParsedFlagsWriter.setString(packet.getParsedFlags());

    // TCP Flags
    tcpNsWriter.setBoolean((packet.getFlags() & 0x100) != 0);
    tcpCwrWriter.setBoolean((packet.getFlags() & 0x80) != 0);
    tcpEceWriter.setBoolean((packet.getFlags() & 0x40) != 0);
    tcpFlagsEceEcnCapableWriter.setBoolean((packet.getFlags() & 0x42) == 0x42);
    tcpFlagsCongestionWriter.setBoolean((packet.getFlags() & 0x42) == 0x40);
    tcpUrgWriter.setBoolean((packet.getFlags() & 0x20) != 0);
    tcpAckWriter.setBoolean((packet.getFlags() & 0x10) != 0);
    tcpPshWriter.setBoolean((packet.getFlags() & 0x8) != 0);
    tcpRstWriter.setBoolean((packet.getFlags() & 0x4) != 0);
    tcpSynWriter.setBoolean((packet.getFlags() & 0x2) != 0);
    tcpFinWriter.setBoolean((packet.getFlags() & 0x1) != 0);

    // Note:  getData() MUST be called before isCorrupt
    dataWriter.setString(parseBytesToASCII(packet.getData()));
    isCorruptWriter.setBoolean(packet.isCorrupt());
    rowWriter.save();

    // TODO Parse Data Packet Here:
    // Description of work in
    // DRILL-7400: Add Packet Decoders with Interface to Drill
  }
}
