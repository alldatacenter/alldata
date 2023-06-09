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

import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.drill.exec.store.pcap.PcapFormatUtils.parseBytesToASCII;

/**
 * This class is the representation of a TCP session.
 */
public class TcpSession {

  private final List<Packet> packetsFromSender;
  private final List<Packet> packetsFromReceiver;
  private final TcpHandshake handshake;
  private final long sessionID;

  private long startTime;
  private long endTime;
  private int packetCount;
  private InetAddress srcIP;
  private InetAddress dstIP;
  private int srcPort;
  private int dstPort;
  private String srcMac;
  private String dstMac;
  private long synTime;
  private long ackTime;
  private long connectTime;
  private byte[] sentData;
  private byte[] receivedData;
  private int sentDataSize;
  private int receivedDataSize;
  private boolean hasCorruptedData = false;


  private static final Logger logger = LoggerFactory.getLogger(TcpSession.class);

  public TcpSession (long sessionID) {
    packetsFromSender = new ArrayList<>();
    packetsFromReceiver = new ArrayList<>();

    handshake = new TcpHandshake();
    this.sessionID = sessionID;
  }

  /**
   * This function adds a packet to the TCP session.
   * @param p The Packet to be added to the session
   */
  public void addPacket(Packet p) {

    // Only attempt to add TCP packets to session
    if (!p.getPacketType().equalsIgnoreCase("TCP")) {
      return;
    }

    // These variables should be consistent within a TCP session
    if (packetCount == 0) {
      srcIP = p.getSrc_ip();
      dstIP = p.getDst_ip();

      srcPort = p.getSrc_port();
      dstPort = p.getDst_port();

      srcMac = p.getEthernetSource();
      dstMac = p.getEthernetDestination();
      startTime = p.getTimestamp();
    } else if (p.getSessionHash() != sessionID) {
      logger.warn("Attempting to add session {} to incorrect TCP session.", sessionID);
      return;
    }

    // Add packet to appropriate list and increment the data size counter
    if (p.getSrc_ip().getHostAddress().equalsIgnoreCase(srcIP.getHostAddress())) {
      packetsFromSender.add(p);
      // Increment the data size counters
      if (p.getData() != null) {
        sentDataSize += p.getData().length;
      }

    } else {
      packetsFromReceiver.add(p);
      if (p.getData() != null) {
        receivedDataSize += p.getData().length;
      }
    }

    // Check flags if connection is not established
    if (!handshake.isConnected()) {
      if (p.getSynFlag() && p.getSrc_ip().getHostAddress().equalsIgnoreCase(srcIP.getHostAddress())) {
        // This is part 1 of the TCP session handshake
        // The host sends the first SYN packet
        handshake.setSyn();
        synTime = p.getTimestamp();
      } else if (p.getSynFlag() && p.getAckFlag() && p.getSrc_ip().getHostAddress().equalsIgnoreCase(dstIP.getHostAddress())) {
        // This condition represents the second part of the TCP Handshake,
        // where the receiver sends a frame with the SYN/ACK flags set to the originator
        handshake.setAck();
      } else if (p.getAckFlag() && p.getSrc_ip().getHostAddress().equalsIgnoreCase(srcIP.getHostAddress())) {
        // Finally, this condition represents a successful opening of a TCP session, when the originator sends a frame with only the ACK flag set.
        // At this point we finalize the session object and clear out the flags.
        handshake.setAck();
        ackTime = p.getTimestamp();
        connectTime = ackTime - synTime;
        //handshake.setConnected(sessionID);
      }
    } else {
      /* Check for flags to close connection. Closing a TCP session is more difficult than opening a session and there are
      * a lot of ways that it can go wrong. See https://accedian.com/enterprises/blog/close-tcp-sessions-diagnose-disconnections/ for references on
      * closing TCP sessions.
      *
      * To close a session correctly, there are four steps:
      * 1.  Party A sends a frame to party B with the FIN flag
      * 2.  Party B sends a frame with an ACK flag
      * 3.  Party B then follows with a frame with the FIN flag set
      * 4.  Party A then confirms with an ACK flag.
      *
      * Technically, the session is closed upon the first FIN flag and resources can be released at that point. However, a lot can go wrong, so to force the closing of a
      * session, either party can send a frame with a RST flag set which forces the closing of the session.
      */

      if (p.getRstFlag()) {
        // This is the case for a forced closed connection.  Session is immediately closed.
        handshake.setRst();
      } else if (p.getFinFlag()) {
        // This is the beginning of the normal session closure procedure.  If a FIN flag has been seen, the session is basically closed even if one party continues to send
        // data,
        handshake.setFin();
      } else if (handshake.getCurrentSessionState() == TcpHandshake.State.CLOSE_WAIT) {

      }
      if (p.getAckFlag() && p.getFinFlag()) {
        handshake.setAck();
      }
    }

    // Augment the packet counter
    packetCount++;

    if (p.isCorrupt()) {
      hasCorruptedData = true;
    }

    // Add the start and ending time stamp.  The packets are not necessarily received in order, so we have to check the timestamps this way
    if (p.getTimestampMicro() < startTime) {
      startTime = p.getTimestamp();
    }

    if (p.getTimestampMicro() > endTime) {
      endTime = p.getTimestamp();
    }

    // Close the session if the closing handshake is complete
    if (handshake.isClosed()) {
      closeSession();
    }
  }

  /**
   * This function returns true if the TCP session has been established, false if not.
   * @return True if the session has been established, false if not.
   */
  public boolean connectionEstablished() {
    return handshake.isConnected();
  }

  public boolean connectionClosed() {
    return handshake.isClosed();
  }

  public void closeSession() {
    logger.debug("Closing session {}", sessionID);
    /* The sent and received bytes cannot be written until the session is closed.
    * Upon receipt of the FIN->FIN/ACK handshake, write everything.
    *
    * Since it cannot be assumed that the packets were received in the correct order, we must:
    * 1. Sort them by TCP Sequence Number
    * 2. Write the data to the respective byte array
    */

    Collections.sort(packetsFromSender);
    Collections.sort(packetsFromReceiver);

    sentData = new byte[sentDataSize];
    receivedData = new byte[receivedDataSize];

    byte[] dataFromPacket;
    int dataOffset = 0;
    // Now that the lists are sorted, add packet data to the lists
    for (int i = 0; i < packetsFromSender.size(); i++) {
      // Get the packet;
      Packet p = packetsFromSender.get(i);
      dataFromPacket = p.getData();
      if (dataFromPacket != null) {
        for (int j = 0; j < dataFromPacket.length; j++) {
          sentData[dataOffset] = dataFromPacket[j];
          dataOffset++;
        }
      }
    }

    dataOffset = 0;
    for (int i = 0; i < packetsFromReceiver.size(); i++) {
      // Get the packet;
      Packet p = packetsFromReceiver.get(i);
      dataFromPacket = p.getData();
      if (dataFromPacket != null) {
        for (int j = 0; j < dataFromPacket.length; j++) {
          receivedData[dataOffset] = dataFromPacket[j];
          dataOffset++;
        }
      }
    }
  }

  public Instant getSessionStartTime() {
    return Instant.ofEpochMilli(startTime);
  }

  public Period getSessionDuration() {
    return new Period(endTime - startTime);
  }

  public Period getConnectionTime() {
    return new Period(connectTime);
  }

  public Instant getSessionEndTime() {
    return Instant.ofEpochMilli(endTime);
  }

  public String getSrcMac() {
    return srcMac;
  }

  public String getDstMac() {
    return dstMac;
  }

  public String getSrcIP() {
    return srcIP.getHostAddress();
  }

  public String getDstIP() {
    return dstIP.getHostAddress();
  }

  public int getSrcPort() {
    return srcPort;
  }

  public int getDstPort() {
    return dstPort;
  }

  public long getSessionID() {
    return sessionID;
  }

  public int getPacketCount() {
    return packetsFromReceiver.size() + packetsFromSender.size();
  }

  public int getPacketCountFromOrigin() { return packetsFromSender.size(); }

  public int getPacketCountFromRemote() { return packetsFromReceiver.size(); }

  public boolean hasCorruptedData() {
    return hasCorruptedData;
  }

  public int getDataVolumeFromOrigin() {
    return sentData.length;
  }

  public int getDataVolumeFromRemote() {
    return receivedData.length;
  }

  public byte[] getDataFromOriginator() {
    return sentData;
  }

  public String getDataFromOriginatorAsString() {
    return parseBytesToASCII(sentData);
  }

  public byte[] getDataFromRemote() {
    return receivedData;
  }

  public String getDataFromRemoteAsString() {
    return parseBytesToASCII(receivedData);
  }
}
