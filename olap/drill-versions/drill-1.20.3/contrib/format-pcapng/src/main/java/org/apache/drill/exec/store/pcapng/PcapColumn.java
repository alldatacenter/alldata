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
package org.apache.drill.exec.store.pcapng;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.store.pcap.PcapFormatUtils;
import org.apache.drill.exec.vector.accessor.ScalarWriter;

import fr.bmartel.pcapdecoder.structure.options.inter.IOptionsStatisticsHeader;
import fr.bmartel.pcapdecoder.structure.types.IPcapngType;
import fr.bmartel.pcapdecoder.structure.types.inter.IDescriptionBlock;
import fr.bmartel.pcapdecoder.structure.types.inter.IEnhancedPacketBLock;
import fr.bmartel.pcapdecoder.structure.types.inter.INameResolutionBlock;
import fr.bmartel.pcapdecoder.structure.types.inter.ISectionHeaderBlock;
import fr.bmartel.pcapdecoder.structure.types.inter.IStatisticsBlock;

public abstract class PcapColumn {

  private static final Map<String, PcapColumn> columns = new LinkedHashMap<>();
  private static final Map<String, PcapColumn> summary_columns = new LinkedHashMap<>();
  public static final String DUMMY_NAME = "dummy";
  public static final String PATH_NAME = "path";

  static {
    // Basic
    columns.put("timestamp", new PcapTimestamp());
    columns.put("packet_length", new PcapPacketLength());
    columns.put("type", new PcapType());
    columns.put("src_ip", new PcapSrcIp());
    columns.put("dst_ip", new PcapDstIp());
    columns.put("src_port", new PcapSrcPort());
    columns.put("dst_port", new PcapDstPort());
    columns.put("src_mac_address", new PcapSrcMac());
    columns.put("dst_mac_address", new PcapDstMac());
    columns.put("tcp_session", new PcapTcpSession());
    columns.put("tcp_ack", new PcapTcpAck());
    columns.put("tcp_flags", new PcapTcpFlags());
    columns.put("tcp_flags_ns", new PcapTcpFlagsNs());
    columns.put("tcp_flags_cwr", new PcapTcpFlagsCwr());
    columns.put("tcp_flags_ece", new PcapTcpFlagsEce());
    columns.put("tcp_flags_ece_ecn_capable", new PcapTcpFlagsEceEcnCapable());
    columns.put("tcp_flags_ece_congestion_experienced", new PcapTcpFlagsEceCongestionExperienced());
    columns.put("tcp_flags_urg", new PcapTcpFlagsUrg());
    columns.put("tcp_flags_ack", new PcapTcpFlagsAck());
    columns.put("tcp_flags_psh", new PcapTcpFlagsPsh());
    columns.put("tcp_flags_rst", new PcapTcpFlagsRst());
    columns.put("tcp_flags_syn", new PcapTcpFlagsSyn());
    columns.put("tcp_flags_fin", new PcapTcpFlagsFin());
    columns.put("tcp_parsed_flags", new PcapTcpParsedFlags());
    columns.put("packet_data", new PcapPacketData());

    // Extensions
    summary_columns.put("path", new PcapStatPath());
    // Section Header Block
    summary_columns.put("shb_hardware", new PcapHardware());
    summary_columns.put("shb_os", new PcapOS());
    summary_columns.put("shb_userappl", new PcapUserAppl());
    // Interface Description Block
    summary_columns.put("if_name", new PcapIfName());
    summary_columns.put("if_description", new PcapIfDescription());
    summary_columns.put("if_ipv4addr", new PcapIfIPv4addr());
    summary_columns.put("if_ipv6addr", new PcapIfIPv6addr());
    summary_columns.put("if_macaddr", new PcapIfMACaddr());
    summary_columns.put("if_euiaddr", new PcapIfEUIaddr());
    summary_columns.put("if_speed", new PcapIfSpeed());
    summary_columns.put("if_tsresol", new PcapIfTsresol());
    summary_columns.put("if_tzone", new PcapIfTzone());
    summary_columns.put("if_os", new PcapIfOS());
    summary_columns.put("if_fcslen", new PcapIfFcslen());
    summary_columns.put("if_tsoffset", new PcapIfTsOffset());
    // Name Resolution Block
    summary_columns.put("ns_dnsname", new PcapDnsName());
    summary_columns.put("ns_dnsip4addr", new PcapDnsIP4addr());
    summary_columns.put("ns_dnsip6addr", new PcapDnsIP6addr());
    // Interface Statistics Block
    summary_columns.put("isb_starttime", new PcapIsbStarttime());
    summary_columns.put("isb_endtime", new PcapIsbEndtime());
    summary_columns.put("isb_ifrecv", new PcapIsbIfrecv());
    summary_columns.put("isb_ifdrop", new PcapIsbIfdrop());
    summary_columns.put("isb_filteraccept", new PcapIsbFilterAccept());
    summary_columns.put("isb_osdrop", new PcapIsbOSdrop());
    summary_columns.put("isb_usrdeliv", new PcapIsbUsrdeliv());
  }

  abstract MajorType getType();

  abstract void process(IPcapngType block, ScalarWriter writer);

  public static Map<String, PcapColumn> getColumns() {
    return Collections.unmodifiableMap(columns);
  }

  public static Map<String, PcapColumn> getSummaryColumns() {
    return Collections.unmodifiableMap(summary_columns);
  }

  static class PcapDummy extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) { }
  }

  static class PcapStatPath extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) { }
  }

  static class PcapTimestamp extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.required(MinorType.TIMESTAMP);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      writer.setTimestamp(Instant.ofEpochMilli(((IEnhancedPacketBLock) block).getTimeStamp() / 1000));
    }
  }

  static class PcapPacketLength extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.required(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      writer.setInt(((IEnhancedPacketBLock) block).getPacketLength());
    }
  }

  static class PcapType extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setString(packet.getPacketType());
      }
    }
  }

  static class PcapSrcIp extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setString(packet.getSrc_ip().getHostAddress());
      }
    }
  }

  static class PcapDstIp extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setString(packet.getDst_ip().getHostAddress());
      }
    }
  }

  static class PcapSrcPort extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setInt(packet.getSrc_port());
      }
    }
  }

  static class PcapDstPort extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setInt(packet.getDst_port());
      }
    }
  }

  static class PcapSrcMac extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setString(packet.getEthernetSource());
      }
    }
  }

  static class PcapDstMac extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setString(packet.getEthernetDestination());
      }
    }
  }

  static class PcapTcpSession extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.BIGINT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setLong(packet.getSessionHash());
      }
    }
  }

  static class PcapTcpAck extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setInt(packet.getAckNumber());
      }
    }
  }

  static class PcapTcpFlags extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setInt(packet.getFlags());
      }
    }
  }

  static class PcapTcpFlagsNs extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x100) != 0);
      }
    }
  }

  static class PcapTcpFlagsCwr extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x80) != 0);
      }
    }
  }

  static class PcapTcpFlagsEce extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x40) != 0);
      }
    }
  }

  static class PcapTcpFlagsEceEcnCapable extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x42) == 0x42);
      }
    }
  }

  static class PcapTcpFlagsEceCongestionExperienced extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x42) == 0x40);
      }
    }
  }

  static class PcapTcpFlagsUrg extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x20) != 0);
      }
    }
  }

  static class PcapTcpFlagsAck extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x10) != 0);
      }
    }
  }

  static class PcapTcpFlagsPsh extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x8) != 0);
      }
    }
  }

  static class PcapTcpFlagsRst extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x4) != 0);
      }
    }
  }

  static class PcapTcpFlagsSyn extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x2) != 0);
      }
    }
  }

  static class PcapTcpFlagsFin extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setBoolean((packet.getFlags() & 0x1) != 0);
      }
    }
  }

  static class PcapTcpParsedFlags extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setString(packet.getParsedFlags());
      }
    }
  }

  static class PcapPacketData extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      PacketDecoder packet = new PacketDecoder();
      if (packet.readPcapng(((IEnhancedPacketBLock) block).getPacketData())) {
        writer.setString(PcapFormatUtils.parseBytesToASCII(((IEnhancedPacketBLock) block).getPacketData()));
      }
    }
  }

  /**
   * shb_hardware: description of the hardware
   */
  static class PcapHardware extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof ISectionHeaderBlock)) {
        return;
      }
      writer.setString(((ISectionHeaderBlock) block).getOptions().getHardware());
    }
  }

  // Section Header Block

  /**
   * shb_os: name of the OS
   */
  static class PcapOS extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof ISectionHeaderBlock)) {
        return;
      }
      writer.setString(((ISectionHeaderBlock) block).getOptions().getOS());
    }
  }

  /**
   * shb_userappl: name of the user application
   */
  static class PcapUserAppl extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof ISectionHeaderBlock)) {
        return;
      }
      writer.setString(((ISectionHeaderBlock) block).getOptions().getUserAppl());
    }
  }

  // Interface Description Block

  /**
   * if_name: name of the device used to capture
   */
  static class PcapIfName extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setString(((IDescriptionBlock) block).getOptions().getInterfaceName());
    }
  }

  /**
   * if_description: Description of the device used to capture the data
   */
  static class PcapIfDescription extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setString(((IDescriptionBlock) block).getOptions().getInterfaceDescription());
    }
  }

  /**
   * if_IPv4addr: IPV4 address
   */
  static class PcapIfIPv4addr extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setString(((IDescriptionBlock) block).getOptions().getInterfaceIpv4NetworkAddr());
    }
  }

  /**
   * if_IPv6addr: IPV6 address
   */
  static class PcapIfIPv6addr extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setString(((IDescriptionBlock) block).getOptions().getIpv6NetworkAddr());
    }
  }

  /**
   * if_MACaddr: MAC address
   */
  static class PcapIfMACaddr extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setString(((IDescriptionBlock) block).getOptions().getInterfaceMacAddr());
    }
  }

  /**
   * if_EUIaddr: EUI address
   */
  static class PcapIfEUIaddr extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setString(((IDescriptionBlock) block).getOptions().getInterfaceEuiAddr());
    }
  }

  /**
   * if_speed: interface speed in bps
   */
  static class PcapIfSpeed extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setInt(((IDescriptionBlock) block).getOptions().getInterfaceSpeed());
    }
  }

  /**
   * if_tsresol: Resolution of timestamp (6 means microsecond resolution for instance)
   */
  static class PcapIfTsresol extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setInt(((IDescriptionBlock) block).getOptions().getTimeStampResolution());
    }
  }

  /**
   * if_tzone: indicate Time zone => offset from UTC time
   */
  static class PcapIfTzone extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setInt(((IDescriptionBlock) block).getOptions().getTimeBias());
    }
  }

  /**
   * if_os: Name of the operating system
   */
  static class PcapIfOS extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setString(((IDescriptionBlock) block).getOptions().getInterfaceOperatingSystem());
    }
  }

  /**
   * if_fcslen: Length of the Frame Check Sequence (in bits)
   */
  static class PcapIfFcslen extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setInt(((IDescriptionBlock) block).getOptions().getInterfaceFrameCheckSequenceLength());
    }
  }

  /**
   * if_tsoffset: Timestamp offset for each packet / if not present timestamp are absolute
   */
  static class PcapIfTsOffset extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.INT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IDescriptionBlock)) {
        return;
      }
      writer.setInt(((IDescriptionBlock) block).getOptions().getTimeStampOffset());
    }
  }

  // Name Resolution Block

  /**
   * ns_dnsname: Retrieve DNS server name
   */
  static class PcapDnsName extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof INameResolutionBlock)) {
        return;
      }
      writer.setString(((INameResolutionBlock) block).getOptions().getDnsName());
    }
  }

  /**
   * ns_dnsIP4addr: Retrieve DNS IPV4 server address
   */
  static class PcapDnsIP4addr extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof INameResolutionBlock)) {
        return;
      }
      writer.setString(((INameResolutionBlock) block).getOptions().getDnsIpv4Addr());
    }
  }

  /**
   * ns_dnsIP6addr: Retrieve DNS IPV6 server address
   */
  static class PcapDnsIP6addr extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.VARCHAR);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof INameResolutionBlock)) {
        return;
      }
      writer.setString(((INameResolutionBlock) block).getOptions().getDnsIpv6Addr());
    }
  }

  // Interface Statistics Block

  /**
   * isb_starttime: capture start time (timestamp resolution is defined in Interface description header check exemple)
   */
  static class PcapIsbStarttime extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.TIMESTAMP);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IStatisticsBlock)) {
        return;
      }
      IOptionsStatisticsHeader statisticsHeader = ((IStatisticsBlock) block).getOptions();
      writer.setTimestamp(Instant.ofEpochMilli(statisticsHeader.getCaptureStartTime() / 1000));
    }
  }

  /**
   * isb_endtime: capture end time (timestamp resolution is defined in Interface description header check example)
   */
  static class PcapIsbEndtime extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.TIMESTAMP);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IStatisticsBlock)) {
        return;
      }
      IOptionsStatisticsHeader statisticsHeader = ((IStatisticsBlock) block).getOptions();
      writer.setTimestamp(Instant.ofEpochMilli(statisticsHeader.getCaptureEndTime() / 1000));
    }
  }

  /**
   * isb_ifrecv: packet received count
   */
  static class PcapIsbIfrecv extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.BIGINT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IStatisticsBlock)) {
        return;
      }
      writer.setLong(((IStatisticsBlock) block).getOptions().getPacketReceivedCount());
    }
  }

  /**
   * isb_ifdrop: packet drop count
   */
  static class PcapIsbIfdrop extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.BIGINT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IStatisticsBlock)) {
        return;
      }
      writer.setLong(((IStatisticsBlock) block).getOptions().getPacketDropCount());
    }
  }

  /**
   * isb_filteraccept: packet accepted by filter count
   */
  static class PcapIsbFilterAccept extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.BIGINT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IStatisticsBlock)) {
        return;
      }
      writer.setLong(((IStatisticsBlock) block).getOptions().getPacketAcceptedByFilterCount());
    }
  }

  /**
   * isb_osdrop: packet dropped by Operating system count
   */
  static class PcapIsbOSdrop extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.BIGINT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IStatisticsBlock)) {
        return;
      }
      writer.setLong(((IStatisticsBlock) block).getOptions().getPacketDroppedByOS());
    }
  }

  /**
   * isb_usrdeliv: packet deliver to use count
   */
  static class PcapIsbUsrdeliv extends PcapColumn {

    @Override
    MajorType getType() {
      return Types.optional(MinorType.BIGINT);
    }

    @Override
    void process(IPcapngType block, ScalarWriter writer) {
      if (!(block instanceof IStatisticsBlock)) {
        return;
      }
      writer.setLong(((IStatisticsBlock) block).getOptions().getPacketDeliveredToUser());
    }
  }
}