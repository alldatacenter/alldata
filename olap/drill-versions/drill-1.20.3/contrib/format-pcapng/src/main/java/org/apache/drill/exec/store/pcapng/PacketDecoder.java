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

import org.apache.drill.exec.store.pcap.decoder.Packet;
import org.apache.drill.exec.store.pcap.decoder.PacketConstants;

import static org.apache.drill.exec.store.pcap.PcapFormatUtils.getByte;
import static org.apache.drill.exec.store.pcap.PcapFormatUtils.getShort;

public class PacketDecoder extends Packet {

  @SuppressWarnings("WeakerAccess")
  public boolean readPcapng(final byte[] raw) {
    this.raw = raw;
    return decodeEtherPacket();
  }

  private boolean decodeEtherPacket() {
    etherProtocol = getShort(raw, PacketConstants.PACKET_PROTOCOL_OFFSET);
    ipOffset = PacketConstants.IP_OFFSET;
    if (isIpV4Packet()) {
      protocol = processIpV4Packet();
      return true;
    } else if (isIpV6Packet()) {
      int tmp = processIpV6Packet();
      if (tmp != -1) {
        protocol = tmp;
      }
      return true;
    } else if (isPPPoV6Packet()) {
      protocol = getByte(raw, 48);
      return true;
    }
    return false;
  }

  @Override
  protected int processIpV6Packet() {
    try {
      return super.processIpV6Packet();
    } catch (IllegalStateException | ArrayIndexOutOfBoundsException e) {
      return -1;
    }
  }
}
