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
package org.apache.drill.exec.store.pcap.schema;

import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.pcap.dto.ColumnDto;
import org.apache.drill.common.types.TypeProtos.MinorType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Schema {

  private final List<ColumnDto> columns = new ArrayList<>();
  private final MinorType typeMap[] = new MinorType[PcapTypes.values().length];
  private final boolean sessionizeTCPStreams;

  public Schema(boolean sessionSchema) {
    this.sessionizeTCPStreams = sessionSchema;
    setupStructure();
  }

  private void setupStructure() {
    typeMap[PcapTypes.BOOLEAN.ordinal()] = MinorType.BIT;
    typeMap[PcapTypes.INTEGER.ordinal()] = MinorType.INT;
    typeMap[PcapTypes.STRING.ordinal()] = MinorType.VARCHAR;
    typeMap[PcapTypes.LONG.ordinal()] = MinorType.BIGINT;
    typeMap[PcapTypes.TIMESTAMP.ordinal()] = MinorType.TIMESTAMP;
    typeMap[PcapTypes.DURATION.ordinal()] = MinorType.INTERVAL;

    // Common columns
    columns.add(new ColumnDto("src_ip", PcapTypes.STRING));
    columns.add(new ColumnDto("dst_ip", PcapTypes.STRING));
    columns.add(new ColumnDto("src_port", PcapTypes.INTEGER));
    columns.add(new ColumnDto("dst_port", PcapTypes.INTEGER));
    columns.add(new ColumnDto("src_mac_address", PcapTypes.STRING));
    columns.add(new ColumnDto("dst_mac_address", PcapTypes.STRING));

    // Columns specific for Sessionized TCP Sessions
    if (sessionizeTCPStreams) {
      columns.add(new ColumnDto("session_start_time", PcapTypes.TIMESTAMP));
      columns.add(new ColumnDto("session_end_time", PcapTypes.TIMESTAMP));
      columns.add(new ColumnDto("session_duration", PcapTypes.DURATION));
      columns.add(new ColumnDto("total_packet_count", PcapTypes.INTEGER));
      columns.add(new ColumnDto("data_volume_from_origin", PcapTypes.INTEGER));
      columns.add(new ColumnDto("data_volume_from_remote", PcapTypes.INTEGER));
      columns.add(new ColumnDto("packet_count_from_origin", PcapTypes.INTEGER));
      columns.add(new ColumnDto("packet_count_from_remote", PcapTypes.INTEGER));

      columns.add(new ColumnDto("connection_time", PcapTypes.DURATION));
      columns.add(new ColumnDto("tcp_session", PcapTypes.LONG));
      columns.add(new ColumnDto("is_corrupt", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("data_from_originator", PcapTypes.STRING));
      columns.add(new ColumnDto("data_from_remote", PcapTypes.STRING));
    } else {
      // Columns for Regular Packets
      columns.add(new ColumnDto("type", PcapTypes.STRING));
      columns.add(new ColumnDto("network", PcapTypes.INTEGER));
      columns.add(new ColumnDto("packet_timestamp", PcapTypes.TIMESTAMP));
      columns.add(new ColumnDto("timestamp_micro", PcapTypes.LONG));
      columns.add(new ColumnDto("tcp_session", PcapTypes.LONG));
      columns.add(new ColumnDto("tcp_sequence", PcapTypes.INTEGER));
      columns.add(new ColumnDto("tcp_ack", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags", PcapTypes.INTEGER));
      columns.add(new ColumnDto("tcp_flags_ns", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags_cwr", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags_ece", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags_ece_ecn_capable", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags_ece_congestion_experienced", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags_urg", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags_ack", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags_psh", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags_rst", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags_syn", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_flags_fin", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("tcp_parsed_flags", PcapTypes.STRING));
      columns.add(new ColumnDto("packet_length", PcapTypes.INTEGER));
      columns.add(new ColumnDto("is_corrupt", PcapTypes.BOOLEAN));
      columns.add(new ColumnDto("data", PcapTypes.STRING));
    }
  }

  /**
   * Return list with all columns names and its types
   *
   * @return List<ColumnDto>
   */
  public List<ColumnDto> getColumns() {
    return Collections.unmodifiableList(columns);
  }

  public ColumnDto getColumnByIndex(int i) {
    return columns.get(i);
  }

  public int getNumberOfColumns() {
    return columns.size();
  }

  public TupleMetadata buildSchema(SchemaBuilder builder) {
    for (ColumnDto column : columns) {
      builder.addNullable(column.getColumnName(), typeMap[column.getColumnType().ordinal()]);
    }

    return builder.buildSchema();
  }
}
