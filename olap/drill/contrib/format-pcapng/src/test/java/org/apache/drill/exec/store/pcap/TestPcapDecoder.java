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

import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.exec.store.pcap.decoder.Packet;
import org.apache.drill.exec.store.pcap.decoder.PacketDecoder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPcapDecoder extends BaseTestQuery {
  private static final Logger logger = LoggerFactory.getLogger(TestPcapDecoder.class);

  private static File bigFile;

  @Test
  public void testByteOrdering() throws IOException {
    File f = File.createTempFile("foo", "pcap");
    f.deleteOnExit();
    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(f))) {
      writeHeader(out);
    }

    try (InputStream in = new FileInputStream(f)) {
      PacketDecoder pd = new PacketDecoder(in);
      assertTrue(pd.isBigEndian());
    }
  }

  @Test
  public void testBasics() throws IOException {
    InputStream in = Resources.getResource("pcap/tcp-2.pcap").openStream();
    PacketDecoder pd = new PacketDecoder(in);
    Packet p = pd.packet();
    int offset = 0;


    byte[] buffer = new byte[PcapBatchReader.BUFFER_SIZE + pd.getMaxLength()];
    int validBytes = in.read(buffer);
    assertTrue(validBytes > 50);

    offset = pd.decodePacket(buffer, offset, p, pd.getMaxLength(), validBytes);
    offset = pd.decodePacket(buffer, offset, p, pd.getMaxLength(), validBytes);
    assertEquals(228, offset);

    assertEquals("FE:00:00:00:00:02", p.getEthernetDestination());
    assertEquals("FE:00:00:00:00:01", p.getEthernetSource());
    assertEquals("/192.168.0.1", p.getSrc_ip().toString());
    assertEquals("/192.168.0.2", p.getDst_ip().toString());
    assertEquals(161, p.getSrc_port());
    assertEquals(0, p.getDst_port());
    assertEquals(0, p.getTimestampMicro());
  }

  private static void writeHeader(DataOutputStream out) throws IOException {
    //        typedef struct pcap_hdr_s {
    //            guint32 magic_number;   /* magic number */
    //            guint16 version_major;  /* major version number */
    //            guint16 version_minor;  /* minor version number */
    //            gint32  thiszone;       /* GMT to local correction */
    //            guint32 sigfigs;        /* accuracy of timestamps */
    //            guint32 snaplen;        /* max length of captured packets, in octets */
    //            guint32 network;        /* data link type */
    //        } pcap_hdr_t;
    //        magic_number: used to detect the file format itself and the byte ordering. The writing application writes 0xa1b2c3d4 with it's native byte ordering format into this field. The reading application will read either 0xa1b2c3d4 (identical) or 0xd4c3b2a1 (swapped). If the reading application reads the swapped 0xd4c3b2a1 value, it knows that all the following fields will have to be swapped too. For nanosecond-resolution files, the writing application writes 0xa1b23c4d, with the two nibbles of the two lower-order bytes swapped, and the reading application will read either 0xa1b23c4d (identical) or 0x4d3cb2a1 (swapped).
    //        version_major, version_minor: the version number of this file format (current version is 2.4)
    //        thiszone: the correction time in seconds between GMT (UTC) and the local timezone of the following packet header timestamps. Examples: If the timestamps are in GMT (UTC), thiszone is simply 0. If the timestamps are in Central European time (Amsterdam, Berlin, ...) which is GMT + 1:00, thiszone must be -3600. In practice, time stamps are always in GMT, so thiszone is always 0.
    //        sigfigs: in theory, the accuracy of time stamps in the capture; in practice, all tools set it to 0
    //        snaplen: the "snapshot length" for the capture (typically 65535 or even more, but might be limited by the user), see: incl_len vs. orig_len below
    //        network: link-layer header type, specifying the type of headers at the beginning of the packet (e.g. 1 for Ethernet, see tcpdump.org's link-layer header types page for details); this can be various types such as 802.11, 802.11 with various radio information, PPP, Token Ring, FDDI, etc.

    out.writeInt(0xa1b2c3d4);        // PCAP magic number
    out.writeShort(2);               // version 2.4
    out.writeShort(4);
    out.writeInt(0);                 // assume GMT times
    out.writeInt(0);                 // everybody does this
    out.writeInt(65536);             // customary length limit
    out.writeInt(1);                 // ETHERNET
  }

  // ----------------------------------------
  // the code from here down is useful in that it tests the assumptions that
  // the entire package is based on, but it doesn't really define tests.
  // As such, it can be run as a main class, but isn't supported as unit tests.

  /**
   * This tests the speed when creating an actual object for each packet.
   * <p>
   * Even with decent buffering, this isn't very fast.
   *
   * @throws IOException If file can't be read.
   */
  private static void checkConventionalApproach() throws IOException {
    speedRun(new FileInputStream(bigFile), " without buffering");
  }

  /**
   * This checks the speed when creating an actual object for each packet.
   * <p>
   * Even with decent buffering, this isn't very fast.
   *
   * @throws IOException If file can't be read.
   */
  private static void checkBufferedApproach() throws IOException {
    speedRun(new BufferedInputStream(new FileInputStream(bigFile), 100000), " with buffering");
  }

  private static void speedRun(InputStream in, String msg) throws IOException {
    PacketDecoder pd = new PacketDecoder(in);
    Packet p = pd.nextPacket();
    long total = 0;
    int tcpCount = 0;
    int udpCount = 0;
    int allCount = 0;
    long t0 = System.nanoTime();
    while (p != null) {
      total += p.getPacketLength();
      allCount++;
      if (p.isTcpPacket()) {
        tcpCount++;
      } else if (p.isUdpPacket()) {
        udpCount++;
      }
      // compare to pd.decodePacket() as used in testFastApproach
      p = pd.nextPacket();
    }
    long t1 = System.nanoTime();
    logger.info("Speed test for per packet object {}", msg);
    logger.info(String.format("    Read %.1f MB in %.2f s for %.1f MB/s\n", total / 1e6, (t1 - t0) / 1e9, (double) total * 1e3 / (t1 - t0)));
    logger.info(String.format("    %d packets, %d TCP packets, %d UDP\n", allCount, tcpCount, udpCount));
    logger.info("\n\n\n");
  }

  /**
   * Tests speed for in-place decoding. This is enormously faster than creating objects, largely
   * because we rarely have to move any data. Instead, we can examine as it lies in the buffer.
   *
   * @throws IOException If file can't be read.
   */
  private static void checkFastApproach() throws IOException {
    InputStream in = new FileInputStream(bigFile);
    PacketDecoder pd = new PacketDecoder(in);
    Packet p = pd.packet();

    byte[] buffer = new byte[PcapBatchReader.BUFFER_SIZE + pd.getMaxLength()];
    int validBytes = in.read(buffer);

    int offset = 0;
    long total = 0;
    int tcpCount = 0;
    int udpCount = 0;
    int allCount = 0;
    long t0 = System.nanoTime();
    while (offset < validBytes) {
      // get new data and shift current data to beginning of buffer if there is any danger
      // of straddling the buffer end in the next packet
      // even with jumbo packets this should be enough space to guarantee parsing
      if (validBytes - offset < pd.getMaxLength()) {
        System.arraycopy(buffer, 0, buffer, offset, validBytes - offset);
        validBytes = validBytes - offset;
        offset = 0;

        int n = in.read(buffer, validBytes, buffer.length - validBytes);
        if (n > 0) {
          validBytes += n;
        }
      }

      // decode the packet as it lies
      offset = pd.decodePacket(buffer, offset, p, pd.getMaxLength(), validBytes);
      total += p.getPacketLength();
      allCount++;
      if (p.isTcpPacket()) {
        tcpCount++;
      } else if (p.isUdpPacket()) {
        udpCount++;
      }
    }
    long t1 = System.nanoTime();
    logger.info("Speed test for in-place packet decoding");
    logger.info(String.format("    Read %.1f MB in %.2f s for %.1f MB/s\n", total / 1e6, (t1 - t0) / 1e9, (double) total * 1e3 / (t1 - t0)));
    logger.info(String.format("    %d packets, %d TCP packets, %d UDP\n", allCount, tcpCount, udpCount));
    logger.info("\n\n\n");
  }

  /**
   * Creates an ephemeral file of about a GB in size
   *
   * @throws IOException If input file can't be read or output can't be written.
   */
  private static void buildBigTcpFile() throws IOException {
    bigFile = File.createTempFile("tcp", ".pcap");
    bigFile.deleteOnExit();
    boolean first = true;
    logger.info("Building large test file");
    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(bigFile))) {
      for (int i = 0; i < 1000e6 / (29208 - 24) + 1; i++) {
        // might be faster to keep this open and rewind each time, but
        // that is hard to do with a resource, especially if it comes
        // from the class path instead of files.
        try (InputStream in = Resources.getResource("pcap/tcp-2.pcap").openStream()) {
          ConcatPcap.copy(first, in, out);
        }
        first = false;
      }
    }
  }

  public static void main(String[] args) throws IOException {
    logger.info("Checking speeds for various approaches");
    buildBigTcpFile();
    checkConventionalApproach();
    checkBufferedApproach();
    checkFastApproach();
  }
}
