/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.pig.persistence.utils;

import org.apache.ambari.view.ViewContext;
import org.apache.commons.configuration.Configuration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Persistence API to Apache Configuration adapter
 */
@Deprecated
public class ContextConfigurationAdapter implements Configuration {
  private ViewContext context;

  /**
   * Constructor of adapter
   * @param context View Context
   */
  public ContextConfigurationAdapter(ViewContext context) {
    this.context = context;
  }

  @Override
  public Configuration subset(String prefix) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmpty() {
    return context.getInstanceData().isEmpty();
  }

  @Override
  public boolean containsKey(String s) {
    Map<String, String> data = context.getInstanceData();
    return data.containsKey(s);
  }

  @Override
  public void addProperty(String s, Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setProperty(String s, Object o) {
    context.putInstanceData(s, o.toString());
  }

  @Override
  public void clearProperty(String key) {
    context.removeInstanceData(key);
  }

  @Override
  public void clear() {
    for (String key : context.getInstanceData().keySet())
      context.removeInstanceData(key);
  }

  @Override
  public Object getProperty(String key) {
    return context.getInstanceData(key);
  }

  @Override
  public Iterator getKeys(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator getKeys() {
    return context.getInstanceData().keySet().iterator();
  }

  @Override
  public Properties getProperties(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean getBoolean(String s) {
    return getBoolean(s, null);
  }

  @Override
  public boolean getBoolean(String s, boolean b) {
    return getBoolean(s, (Boolean)b);
  }

  @Override
  public Boolean getBoolean(String s, Boolean aBoolean) {
    String data = context.getInstanceData(s);
    return (data != null)?Boolean.parseBoolean(data):aBoolean;
  }

  @Override
  public byte getByte(String s) {
    return getByte(s, null);
  }

  @Override
  public byte getByte(String s, byte b) {
    return getByte(s, (Byte)b);
  }

  @Override
  public Byte getByte(String s, Byte aByte) {
    String data = context.getInstanceData(s);
    return (data != null)?Byte.parseByte(data):aByte;
  }

  @Override
  public double getDouble(String s) {
    return getDouble(s, null);
  }

  @Override
  public double getDouble(String s, double v) {
    return getDouble(s, (Double)v);
  }

  @Override
  public Double getDouble(String s, Double aDouble) {
    String data = context.getInstanceData(s);
    return (data != null)?Double.parseDouble(data):aDouble;
  }

  @Override
  public float getFloat(String s) {
    return getFloat(s, null);
  }

  @Override
  public float getFloat(String s, float v) {
    return getFloat(s, (Float)v);
  }

  @Override
  public Float getFloat(String s, Float aFloat) {
    String data = context.getInstanceData(s);
    return (data != null)?Float.parseFloat(data):aFloat;
  }

  @Override
  public int getInt(String s) {
    return getInteger(s, null);
  }

  @Override
  public int getInt(String s, int i) {
    return getInteger(s, i);
  }

  @Override
  public Integer getInteger(String s, Integer integer) {
    String data = context.getInstanceData(s);
    return (data != null)?Integer.parseInt(data):integer;
  }

  @Override
  public long getLong(String s) {
    return getLong(s, null);
  }

  @Override
  public long getLong(String s, long l) {
    return getLong(s, (Long)l);
  }

  @Override
  public Long getLong(String s, Long aLong) {
    String data = context.getInstanceData(s);
    return (data != null)?Long.parseLong(data):aLong;
  }

  @Override
  public short getShort(String s) {
    return getShort(s, null);
  }

  @Override
  public short getShort(String s, short i) {
    return getShort(s, (Short)i);
  }

  @Override
  public Short getShort(String s, Short aShort) {
    String data = context.getInstanceData(s);
    return (data != null)?Short.parseShort(data):aShort;
  }

  @Override
  public BigDecimal getBigDecimal(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public BigDecimal getBigDecimal(String s, BigDecimal bigDecimal) {
    throw new UnsupportedOperationException();
  }

  @Override
  public BigInteger getBigInteger(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public BigInteger getBigInteger(String s, BigInteger bigInteger) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getString(String s) {
    return context.getInstanceData(s);
  }

  @Override
  public String getString(String s, String s2) {
    String data = getString(s);
    return (data != null)?data:s2;
  }

  @Override
  public String[] getStringArray(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List getList(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List getList(String s, List list) {
    throw new UnsupportedOperationException();
  }
}
