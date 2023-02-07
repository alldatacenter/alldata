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
package org.apache.drill.exec.store.pcap.decoder;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Formatter;

import static org.apache.drill.exec.store.pcap.PcapFormatUtils.convertInt;
import static org.apache.drill.exec.store.pcap.PcapFormatUtils.convertShort;
import static org.apache.drill.exec.store.pcap.PcapFormatUtils.getByte;
import static org.apache.drill.exec.store.pcap.PcapFormatUtils.getIntFileOrder;
import static org.apache.drill.exec.store.pcap.PcapFormatUtils.getShort;

public class Packet implements Comparable<Packet> {
  // pcap header
  //        typedef struct pcaprec_hdr_s {
  //            guint32 ts_sec;         // timestamp seconds
  //            guint32 ts_usec;        // timestamp microseconds */
  //            guint32 incl_len;       // number of octets of packet saved in file */
  //            guint32 orig_len;       // actual length of packet */
  //        } pcaprec_hdr_t;
  private long timestamp;
  private long timestampMicro;
  private int originalLength;

  protected byte[] raw;

  // index into the raw data where the current ethernet packet starts
  private int etherOffset;
  // index into the raw data where the current IP packet starts. Should be just after etherOffset
  protected int ipOffset;

  private int packetLength;
  protected int etherProtocol;
  protected int protocol;
  protected boolean isRoutingV6;
  protected boolean isCorrupt = false;

  private static final Logger logger = LoggerFactory.getLogger(Packet.class);


  @SuppressWarnings("WeakerAccess")
  public boolean readPcap(final InputStream in, final boolean byteOrder, final int maxLength) throws IOException {
    byte[] pcapHeader = new byte[PacketConstants.PCAP_HEADER_SIZE];
    int n = in.read(pcapHeader);
    if (n < pcapHeader.length) {
      return false;
    }
    decodePcapHeader(pcapHeader, byteOrder, maxLength, 0);

    raw = new byte[originalLength];
    n = in.read(raw);
    if (n < 0) {
      return false;
    }
    etherOffset = 0;

    decodeEtherPacket();
    return true;
  }

  @SuppressWarnings("WeakerAccess")
  public int decodePcap(final byte[] buffer, final int offset, final boolean byteOrder, final int maxLength) {
    raw = buffer;
    etherOffset = offset + PacketConstants.PCAP_HEADER_SIZE;
    decodePcapHeader(raw, byteOrder, maxLength, offset);
    decodeEtherPacket();
    return offset + PacketConstants.PCAP_HEADER_SIZE + originalLength;
  }

  public String getPacketType() {
    if (isTcpPacket()) {
      return "TCP";
    } else if (isUdpPacket()) {
      return "UDP";
    } else if (isArpPacket()) {
      return "ARP";
    } else if (isIcmpPacket()) {
      return "ICMP";
    } else {
      return "unknown";
    }
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isIpV4Packet() {
    return etherProtocol == PacketConstants.IPv4_TYPE;
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isIpV6Packet() {
    return etherProtocol == PacketConstants.IPv6_TYPE;
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isPPPoV6Packet() {
    return etherProtocol == PacketConstants.PPPoV6_TYPE;
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isTcpPacket() {
    return protocol == PacketConstants.TCP_PROTOCOL;
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isUdpPacket() {
    return protocol == PacketConstants.UDP_PROTOCOL;
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isArpPacket() {
    return protocol == PacketConstants.ARP_PROTOCOL;
  }

  @SuppressWarnings("WeakerAccess")
  public boolean isIcmpPacket() {
    return protocol == PacketConstants.ICMP_PROTOCOL;
  }

  public long getSessionHash() {
    if (isTcpPacket()) {
      Murmur128 h1 = new Murmur128(1, 2);
      byte[] buf = getIpAddressBytes(true);
      if (buf == null) {
        return 0;
      }
      h1.hash(buf, 0, buf.length);
      h1.hash(getSrc_port());

      Murmur128 h2 = new Murmur128(1, 2);
      buf = getIpAddressBytes(false);
      if (buf == null) {
        return 0;
      }
      h2.hash(buf, 0, buf.length);
      h2.hash(getDst_port());

      return h1.digest64() ^ h2.digest64();
    } else {
      return 0;
    }
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getTimestampMicro() {
    return timestampMicro;
  }

  public int getPacketLength() {
    return packetLength;
  }

  public InetAddress getSrc_ip() {
    return getIPAddress(true);
  }

  public InetAddress getDst_ip() {
    return getIPAddress(false);
  }

  public String getSourceIpAddressString() {
    InetAddress address = getSrc_ip();
    if (address == null) {
      return null;
    } else {
      return address.getHostAddress();
    }
  }

  public String getDestinationIpAddressString() {
    InetAddress address = getDst_ip();
    if (address == null) {
      return null;
    } else {
      return address.getHostAddress();
    }
  }

  public String getEthernetSource() {
    return getEthernetAddress(PacketConstants.ETHER_SRC_OFFSET);
  }

  public String getEthernetDestination() {
    return getEthernetAddress(PacketConstants.ETHER_DST_OFFSET);
  }

  public int getSequenceNumber() {
    if (isTcpPacket()) {
      return convertInt(raw, ipOffset + getIPHeaderLength() + PacketConstants.TCP_SEQUENCE_OFFSET);
    } else {
      return 0;
    }
  }

  public int getAckNumber() {
    if (isTcpPacket()) {
        return convertInt(raw, ipOffset + getIPHeaderLength() + PacketConstants.TCP_ACK_OFFSET);
    } else {
      return 0;
    }
  }

  public int getFlags() {
    if (isTcpPacket()) {
      return convertShort(raw, ipOffset + getIPHeaderLength() + PacketConstants.TCP_FLAG_OFFSET) & 0xfff;
    } else {
      return 0;
    }
  }

  public String getParsedFlags() {
    return formatFlags(getFlags());
  }

  public void setIsCorrupt(boolean value) {
    isCorrupt = value;
  }

  public boolean getUrgFlag() {
    return (getFlags() & 0x20) != 0;
  }

  public boolean getPshFlag() {
    return (getFlags() & 0x8) != 0;
  }

  public boolean getEceFlag() {
    return (getFlags() & 0x40) != 0;
  }

  public boolean getSynFlag() {
    return (getFlags() & 0x2) != 0;
  }

  public boolean getAckFlag() {
    return (getFlags() & 0x10) != 0;
  }

  public boolean getRstFlag() {
    return (getFlags() & 0x4) != 0;
  }

  public boolean getFinFlag() {
    return (getFlags() & 0x1) != 0;
  }

  public boolean getNSFlag() {
    return (getFlags() & 0x100) != 0;
  }

  public boolean getCwrFlag() {
    return (getFlags() & 0x80) != 0;
  }

  public static String formatFlags(int flags) {
    int mask = 0x100;
    StringBuilder r = new StringBuilder();
    String separator = "";
    if ((flags & mask) != 0) {
      r.append(separator);
      r.append("NS");
      separator = "|";
    }
    mask = mask >> 1;

    if ((flags & mask) != 0) {
      r.append(separator);
      r.append("CWR");
      separator = "|";
    }
    mask = mask >> 1;

    if ((flags & mask) != 0) {
      r.append(separator);
      r.append("ECE");
      if ((flags & 2) != 0) {
        r.append(" (ECN capable)");
      } else {
        r.append(" (Congestion experienced)");
      }
      separator = "|";
    }
    mask = mask >> 1;

    if ((flags & mask) != 0) {
      r.append(separator);
      r.append("URG");
      separator = "|";
    }
    mask = mask >> 1;

    if ((flags & mask) != 0) {
      r.append(separator);
      r.append("ACK");
      separator = "|";
    }
    mask = mask >> 1;

    if ((flags & mask) != 0) {
      r.append(separator);
      r.append("PSH");
      separator = "|";
    }
    mask = mask >> 1;

    if ((flags & mask) != 0) {
      r.append(separator);
      r.append("RST");
      separator = "|";
    }
    mask = mask >> 1;

    if ((flags & mask) != 0) {
      r.append(separator);
      r.append("SYN");
      separator = "|";
    }
    mask = mask >> 1;

    if ((flags & mask) != 0) {
      r.append(separator);
      r.append("FIN");
    }
    return r.toString();
  }

  public int getSrc_port() {
    if (isPPPoV6Packet()) {
      return getPort(64);
    }
    if (isIpV6Packet()) {
      if (isRoutingV6) {
        return getPort(136);
      }
      return getPort(40);
    }
    return getPort(0);
  }

  public int getDst_port() {
    if (isPPPoV6Packet()) {
      return getPort(66);
    }
    if (isIpV6Packet()) {
      if (isRoutingV6) {
        return getPort(138);
      }
      return getPort(42);
    }
    return getPort(2);
  }

  public boolean isCorrupt(){
    return isCorrupt;
  }

  public byte[] getData() {
    int payloadDataStart = getIPHeaderLength();
    if (isTcpPacket()) {
      payloadDataStart += this.getTCPHeaderLength(raw);
    } else if (isUdpPacket()) {
      payloadDataStart += this.getUDPHeaderLength();
    } else {
      return null;
    }
    byte[] data = null;
    if (packetLength >= payloadDataStart) {
      data = new byte[packetLength - payloadDataStart];
      try {
        System.arraycopy(raw, ipOffset + payloadDataStart, data, 0, data.length);
      } catch (Exception e) {
        isCorrupt = true;
        String message = "Error while parsing PCAP data: {}";
        logger.debug(message, e.getMessage());
        logger.trace(message, e);      }
    }
    return data;
  }

  private InetAddress getIPAddress(final boolean src) {
    byte[] ipBuffer = getIpAddressBytes(src);
    if (ipBuffer == null) {
      return null;
    }
    try {
      return InetAddress.getByAddress(ipBuffer);
    } catch (UnknownHostException e) {
      return null;
    }
  }

  private byte[] getIpAddressBytes(final boolean src) {
    int srcPos;
    byte[] ipBuffer;
    int byteShift = 0;
    if (isIpV4Packet()) {
      ipBuffer = new byte[4];
      srcPos = src ? PacketConstants.IP4_SRC_OFFSET : PacketConstants.IP4_DST_OFFSET;
    } else if (isIpV6Packet()) {
      ipBuffer = new byte[16];
      if (isRoutingV6) {
        byteShift = 96;
      }
      srcPos = src ? PacketConstants.IP6_SRC_OFFSET + byteShift : PacketConstants.IP6_DST_OFFSET + byteShift;
    } else if (isPPPoV6Packet()) {
      ipBuffer = new byte[16];
      srcPos = src ? PacketConstants.IP6_SRC_OFFSET + PacketConstants.PPPoV6_IP_OFFSET : PacketConstants.IP6_DST_OFFSET + PacketConstants.PPPoV6_IP_OFFSET;
    } else {
      return null;
    }
    System.arraycopy(raw, etherOffset + srcPos, ipBuffer, 0, ipBuffer.length);
    return ipBuffer;
  }

  private int getIPHeaderLength() {
    return (raw[etherOffset + PacketConstants.VER_IHL_OFFSET] & 0xF) * 4;
  }

  private int getTCPHeaderLength(final byte[] packet) {
    final int inTCPHeaderDataOffset = 12;

    // tcp packet header can have options
    int dataOffset = etherOffset + getIPHeaderLength() + inTCPHeaderDataOffset;
    return 20 + ((packet[dataOffset] >> 4) & 0xF) * 4;
  }

  private int getUDPHeaderLength() {
    return 8;
  }

  private int ipV4HeaderLength() {
    return (getByte(raw, ipOffset) & 0xf) * 4;
  }

  protected int ipVersion() {
    return getByte(raw, ipOffset) >>> 4;
  }

  private void decodePcapHeader(final byte[] header, final boolean byteOrder, final int maxLength, final int offset) {
    timestampMicro = getTimestampMicro(header, byteOrder, offset);
    timestamp = timestampMicro / 1000L;
    originalLength = getIntFileOrder(byteOrder, header, offset + PacketConstants.ORIGINAL_LENGTH_OFFSET);
    packetLength = getIntFileOrder(byteOrder, header, offset + PacketConstants.ACTUAL_LENGTH_OFFSET);
    Preconditions.checkState(originalLength <= maxLength,
        "Packet too long (%s bytes)", originalLength);
  }

  private long getTimestampMicro(final byte[] header, final boolean byteOrder, final int offset) {
    return getIntFileOrder(byteOrder, header, offset + PacketConstants.TIMESTAMP_OFFSET) * 1000000L +
        getIntFileOrder(byteOrder, header, offset + PacketConstants.TIMESTAMP_MICRO_OFFSET);
  }

  private void decodeEtherPacket() {
    etherProtocol = getShort(raw, etherOffset + PacketConstants.PACKET_PROTOCOL_OFFSET);
    ipOffset = etherOffset + PacketConstants.IP_OFFSET;
    if (isIpV4Packet()) {
      protocol = processIpV4Packet();
    } else if (isIpV6Packet()) {
      protocol = processIpV6Packet();
    } else if (isPPPoV6Packet()) {
      protocol = getByte(raw, etherOffset + 48);
    }
    // everything is decoded lazily
  }

  protected int processIpV4Packet() {
    validateIpV4Packet();
    return getByte(raw, ipOffset + 9);
  }

  protected int processIpV6Packet() {
    Preconditions.checkState(ipVersion() == 6, "Should have seen IP version 6, got %d", ipVersion());
    int headerLength = 40;
    int nextHeader = raw[ipOffset + 6] & 0xff;
    while (nextHeader != PacketConstants.TCP_PROTOCOL && nextHeader != PacketConstants.UDP_PROTOCOL && nextHeader != PacketConstants.NO_NEXT_HEADER) {
      switch (nextHeader) {
        case PacketConstants.FRAGMENT_V6:
          nextHeader = getByte(raw, ipOffset + headerLength);
          headerLength += 8;
          break;
        case PacketConstants.ROUTING_V6:
          isRoutingV6 = true;
          nextHeader = getByte(raw, ipOffset + headerLength + 15);
          headerLength += (getByte(raw, ipOffset + headerLength) + 1) * 8;
          break;
        case PacketConstants.HOP_BY_HOP_EXTENSION_V6:
        case PacketConstants.DESTINATION_OPTIONS_V6:
        case PacketConstants.AUTHENTICATION_V6:
        case PacketConstants.ENCAPSULATING_SECURITY_V6:
        case PacketConstants.MOBILITY_EXTENSION_V6:
        case PacketConstants.HOST_IDENTITY_PROTOCOL:
        case PacketConstants.SHIM6_PROTOCOL:
          nextHeader = getByte(raw, ipOffset + headerLength);
          headerLength += (getByte(raw, ipOffset + headerLength) + 1) * 8;
          break;
        default:
          //noinspection ConstantConditions
          logger.warn("Unknown V6 extension or protocol: {}", nextHeader);
          return getByte(raw, ipOffset + headerLength);
      }
    }
    return nextHeader;
  }

  private void validateIpV4Packet() {
    Preconditions.checkState(ipVersion() == 4, "Should have seen IP version 4, got %d", ipVersion());
    int n = ipV4HeaderLength();
    Preconditions.checkState(n >= 20 && n < 200, "Invalid header length: ", n);
  }

  private String getEthernetAddress(int offset) {
    byte[] r = new byte[6];
    System.arraycopy(raw, etherOffset + offset, r, 0, 6);
    Formatter sb = new Formatter();
    for (int i = 0; i < r.length; i++) {
      sb.format("%02X%s", r[i], (i < r.length - 1) ? ":" : "");
    }
    return sb.toString();
  }

  private int getPort(int offset) {
    int dstPortOffset = ipOffset + getIPHeaderLength() + offset;
    return convertShort(raw, dstPortOffset);
  }

  /**
   * This function is here so that packets can be sorted for re-sessionization. Packets in TCP streams
   * are ordered by the sequence number, so being able to order the packets is necessary to reassemble the
   * TCP session.
   * @param o The packet to which the current packet is compared to.
   * @return Returns the difference in sequence number.
   */
  public int compareTo(Packet o) {
    return this.getSequenceNumber() - (o).getSequenceNumber();
  }
}
