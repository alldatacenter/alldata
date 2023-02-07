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
package org.apache.drill.exec.server.options;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.server.options.OptionValue.AccessibleScopes;
import org.apache.drill.exec.server.options.OptionValue.OptionScope;

import com.typesafe.config.ConfigValue;

public class DrillConfigIterator implements Iterable<OptionValue> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillConfigIterator.class);

  DrillConfig c;
  public DrillConfigIterator(DrillConfig c){
    this.c = c;
  }

  @Override
  public Iterator<OptionValue> iterator() {
    return new Iter(c);
  }

  public class Iter implements Iterator<OptionValue>{

    Iterator<Entry<String, ConfigValue>> entries;
    public Iter(DrillConfig c){
      entries = c.entrySet().iterator();
    }
    @Override
    public boolean hasNext() {
      return entries.hasNext();
    }

    @Override
    public OptionValue next() {
      final Entry<String, ConfigValue> e = entries.next();
      final ConfigValue cv = e.getValue();
      final String name = e.getKey();
      OptionValue optionValue = null;
      switch(cv.valueType()) {
      case BOOLEAN:
        optionValue = OptionValue.create(AccessibleScopes.BOOT, name, (Boolean) cv.unwrapped(), OptionScope.BOOT);
        break;

      case LIST:
      case OBJECT:
      case STRING:
        optionValue = OptionValue.create(AccessibleScopes.BOOT, name, cv.render(),OptionScope.BOOT);
        break;

      case NUMBER:
        optionValue = OptionValue.create(OptionValue.AccessibleScopes.BOOT, name, ((Number) cv.unwrapped()).longValue(),OptionScope.BOOT);
        break;

      case NULL:
        throw new IllegalStateException("Config value \"" + name + "\" has NULL type");

      default:
        throw new IllegalStateException("Unknown type: " + cv.valueType());
      }

      return optionValue;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

}
