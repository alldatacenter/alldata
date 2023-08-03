// Copyright 2017 JanusGraph Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.janusgraph.diskstorage.hbase2;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.io.compress.Compression;

import java.io.IOException;

public class HBaseCompat2_0 implements HBaseCompat {

    @Override
    public ColumnFamilyDescriptor setCompression(ColumnFamilyDescriptor cd, String algo) {
        return ColumnFamilyDescriptorBuilder.newBuilder(cd).setCompressionType(Compression.Algorithm.valueOf(algo)).build();
    }

    @Override
    public TableDescriptor newTableDescriptor(String tableName) {
        TableName tn = TableName.valueOf(tableName);

        return TableDescriptorBuilder.newBuilder(tn).build();
    }

    @Override
    public ConnectionMask createConnection(Configuration conf) throws IOException
    {
        return new HConnection2_0(ConnectionFactory.createConnection(conf));
    }

    @Override
    public TableDescriptor addColumnFamilyToTableDescriptor(TableDescriptor tdesc, ColumnFamilyDescriptor cdesc)
    {
        return TableDescriptorBuilder.newBuilder(tdesc).addColumnFamily(cdesc).build();
    }

    @Override
    public void setTimestamp(Delete d, long timestamp)
    {
        d.setTimestamp(timestamp);
    }

}
