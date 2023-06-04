/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.hadoop.split;

import lombok.val;
import org.apache.flink.core.io.LocatableInputSplit;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.io.WritableFactories;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Similar with{@link org.apache.flink.api.java.hadoop.mapred.wrapper.HadoopInputSplit}
 * this class remove job conf to improve the performance.
 */
public class OptimizedHadoopInputSplit extends LocatableInputSplit {
  private static final long serialVersionUID = -6990336376163226161L;
  private final Class<? extends InputSplit> splitType;
  private transient InputSplit hadoopInputSplit;

  private byte[] hadoopInputSplitByteArray;

  public OptimizedHadoopInputSplit(int splitNumber, InputSplit inputSplit) {
    super(splitNumber, (String) null);
    if (inputSplit == null) {
      throw new NullPointerException("Hadoop input split must not be null");
    }

    this.splitType = inputSplit.getClass();
    this.hadoopInputSplit = inputSplit;
  }

  public String[] getHostnames() {
    try {
      return this.hadoopInputSplit.getLocations();
    } catch (IOException var2) {
      return new String[0];
    }
  }

  public InputSplit getHadoopInputSplit() {
    return this.hadoopInputSplit;
  }

  public void initInputSplit(JobConf jobConf) {
    if (this.hadoopInputSplit != null) {
      return;
    }

    checkNotNull(hadoopInputSplitByteArray);

    try {
      this.hadoopInputSplit = (InputSplit) WritableFactories.newInstance(splitType);

      if (this.hadoopInputSplit instanceof Configurable) {
        ((Configurable) this.hadoopInputSplit).setConf(jobConf);
      } else if (this.hadoopInputSplit instanceof JobConfigurable) {
        ((JobConfigurable) this.hadoopInputSplit).configure(jobConf);
      }

      if (hadoopInputSplitByteArray != null) {
        try (val oInput = new ObjectInputStream(new ByteArrayInputStream(hadoopInputSplitByteArray))) {
          this.hadoopInputSplit.readFields(oInput);
        }

        this.hadoopInputSplitByteArray = null;
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to instantiate Hadoop InputSplit", e);
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {

    if (hadoopInputSplit != null) {
      try (
          ByteArrayOutputStream bout = new ByteArrayOutputStream();
          ObjectOutputStream oout = new ObjectOutputStream(bout)
      ) {
        this.hadoopInputSplit.write(oout);
        oout.flush();
        this.hadoopInputSplitByteArray = bout.toByteArray();
      }
    }

    out.defaultWriteObject();
  }
}

