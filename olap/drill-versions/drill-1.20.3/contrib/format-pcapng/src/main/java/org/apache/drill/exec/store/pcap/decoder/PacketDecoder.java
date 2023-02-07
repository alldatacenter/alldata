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

import static org.apache.drill.exec.store.pcap.PcapFormatUtils.getInt;
import static org.apache.drill.exec.store.pcap.PcapFormatUtils.getIntFileOrder;
import static org.apache.drill.exec.store.pcap.PcapFormatUtils.getShortFileOrder;

public class PacketDecoder {
  //  typedef struct pcap_hdr_s {
  //      guint32 magic_number;   /* magic number */
  //      guint16 version_major;  /* major version number */
  //      guint16 version_minor;  /* minor version number */
  //      gint32  thiszone;       /* GMT to local correction */
  //      guint32 sigfigs;        /* accuracy of timestamps */
  //      guint32 snaplen;        /* max length of captured packets, in octets */
  //      guint32 network;        /* data link type */
  //  } pcap_hdr_t;
  //  magic_number: used to detect the file format itself and the byte ordering. The writing application writes
  //    0xa1b2c3d4 with it's native byte ordering format into this field. The reading application will read
  //    either 0xa1b2c3d4 (identical) or 0xd4c3b2a1 (swapped). If the reading application reads the swapped
  //    0xd4c3b2a1 value, it knows that all the following fields will have to be swapped too. For
  //    nanosecond-resolution files, the writing application writes 0xa1b23c4d, with the two nibbles
  //    of the two lower-order bytes swapped, and the reading application will read either 0xa1b23c4d
  //    (identical) or 0x4d3cb2a1 (swapped).
  //  version_major, version_minor: the version number of this file format (current version is 2.4)
  //  thiszone: the correction time in seconds between GMT (UTC) and the local timezone of the following
  //     packet header timestamps. Examples: If the timestamps are in GMT (UTC), thiszone is simply 0.
  //     If the timestamps are in Central European time (Amsterdam, Berlin, ...) which is GMT + 1:00,
  //     thiszone must be -3600. In practice, time stamps are always in GMT, so thiszone is always 0.
  //  sigfigs: in theory, the accuracy of time stamps in the capture; in practice, all tools set it to 0
  //  snaplen: the "snapshot length" for the capture (typically 65535 or even more, but might be limited
  //     by the user), see: incl_len vs. orig_len below
  //  network: link-layer header type, specifying the type of headers at the beginning of the packet (e.g.
  //     1 for Ethernet, see tcpdump.org's link-layer header types page for details); this can be various
  //     types such as 802.11, 802.11 with various radio information, PPP, Token Ring, FDDI, etc.
  private static final int GLOBAL_HEADER_SIZE = 24;
  private static final int PCAP_MAGIC_LITTLE_ENDIAN = 0xD4C3B2A1;
  private static final int PCAP_MAGIC_NUMBER = 0xA1B2C3D4;
  private static final int PCAPNG_MAGIC_LITTLE_ENDIAN = 0x4D3C2B1A;
  private static final int PCAPNG_MAGIC_NUMBER = 0x0A0D0D0A;

  private static final Logger logger = LoggerFactory.getLogger(PacketDecoder.class);

  private final int maxLength;
  private final int network;
  private boolean bigEndian;
  private FileFormat fileFormat;

  private InputStream input;

  public PacketDecoder(final InputStream input) throws IOException {
    this.input = input;
    byte[] globalHeader = new byte[GLOBAL_HEADER_SIZE];
    int n = input.read(globalHeader);
    if (n != globalHeader.length) {
      throw new IOException("Can't read PCAP file header");
    }
    switch (getInt(globalHeader, 0)) {
      case PCAP_MAGIC_NUMBER:
        bigEndian = true;
        fileFormat = FileFormat.PCAP;
        break;
      case PCAP_MAGIC_LITTLE_ENDIAN:
        bigEndian = false;
        fileFormat = FileFormat.PCAP;
        break;
      case PCAPNG_MAGIC_NUMBER:
        bigEndian = true;
        fileFormat = FileFormat.PCAPNG;
        break;
      case PCAPNG_MAGIC_LITTLE_ENDIAN:
        bigEndian = false;
        fileFormat = FileFormat.PCAPNG;
        break;
      default:
        //noinspection ConstantConditions
        Preconditions.checkState(false,
            String.format("Bad magic number = %08x", getIntFileOrder(bigEndian, globalHeader, 0)));
    }
    if(fileFormat == FileFormat.PCAP) {
      Preconditions.checkState(getShortFileOrder(bigEndian, globalHeader, 4) == 2, "Wanted major version == 2");
    } // todo: pcapng major version == 1 precondition
    maxLength = getIntFileOrder(bigEndian, globalHeader, 16);
    network = getIntFileOrder(bigEndian, globalHeader, 20);
  }

  public final int getMaxLength() {
    return maxLength;
  }

  public int decodePacket(final byte[] buffer, final int offset, Packet p, int maxPacket, int validBytes) {
    int r = p.decodePcap(buffer, offset, bigEndian, Math.min(maxPacket, validBytes - offset));
    if (r > validBytes) {
      logger.error("Invalid packet at offset {}", offset);
    }
    return r;
  }

  public Packet packet() {
    return new Packet();
  }

  public int getNetwork() {
    return network;
  }

  public boolean isBigEndian() {
    return bigEndian;
  }

  public FileFormat getFileFormat() {
    return fileFormat;
  }

  public Packet nextPacket() throws IOException {
    Packet r = new Packet();
    if (r.readPcap(input, bigEndian, maxLength)) {
      return r;
    } else {
      return null;
    }
  }

  public enum FileFormat {
    PCAP,
    PCAPNG,
    UNKNOWN
  }
}
