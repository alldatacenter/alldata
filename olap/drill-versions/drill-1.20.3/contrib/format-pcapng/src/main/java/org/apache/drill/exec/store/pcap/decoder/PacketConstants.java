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

@SuppressWarnings("WeakerAccess")
public final class PacketConstants {

  public static final int PCAP_HEADER_SIZE = 4 * 4;   // packet header, not file header

  public static final int TIMESTAMP_OFFSET = 0;
  public static final int TIMESTAMP_MICRO_OFFSET = 4;
  public static final int ORIGINAL_LENGTH_OFFSET = 8;
  public static final int ACTUAL_LENGTH_OFFSET = 12;

  public static final int PACKET_PROTOCOL_OFFSET = 12;

  public static final byte ARP_PROTOCOL = 0;
  public static final byte ICMP_PROTOCOL = 1;
  public static final byte TCP_PROTOCOL = 6;
  public static final byte UDP_PROTOCOL = 17;

  public static final int HOP_BY_HOP_EXTENSION_V6 = 0;
  public static final int DESTINATION_OPTIONS_V6 = 60;
  public static final int ROUTING_V6 = 43;
  public static final int FRAGMENT_V6 = 44;
  public static final int AUTHENTICATION_V6 = 51;
  public static final int ENCAPSULATING_SECURITY_V6 = 50;
  public static final int MOBILITY_EXTENSION_V6 = 135;
  public static final int HOST_IDENTITY_PROTOCOL = 139;
  public static final int SHIM6_PROTOCOL = 140;

  public static final int NO_NEXT_HEADER = 59;
  public static final int UDP_HEADER_LENGTH = 8;
  public static final int VER_IHL_OFFSET = 14;

  public static final int ETHER_HEADER_LENGTH = 14;
  public static final int ETHER_TYPE_OFFSET = 12;

  public static final int IPv4_TYPE = 0x800;
  public static final int IPv6_TYPE = 0x86dd;
  public static final int PPPoV6_TYPE = 0x8864;


  public static final int IP_OFFSET = 14;

  public static final int IP4_SRC_OFFSET = IP_OFFSET + 12;
  public static final int IP4_DST_OFFSET = IP_OFFSET + 16;

  public static final int IP6_SRC_OFFSET = IP_OFFSET + 8;
  public static final int IP6_DST_OFFSET = IP_OFFSET + 24;
  public static final int ETHER_DST_OFFSET = 0;
  public static final int ETHER_SRC_OFFSET = 6;

  public static final int PPPoV6_IP_OFFSET = 28;

  public static final int TCP_SEQUENCE_OFFSET = 4;
  public static final int TCP_ACK_OFFSET = 8;
  public static final int TCP_FLAG_OFFSET = 12;
}
