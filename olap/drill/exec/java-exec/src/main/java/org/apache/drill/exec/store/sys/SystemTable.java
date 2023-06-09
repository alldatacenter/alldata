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
package org.apache.drill.exec.store.sys;

import java.util.Iterator;

import org.apache.drill.exec.alias.AliasTarget;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.planner.logical.DrillTableSelection;
import org.apache.drill.exec.store.sys.OptionIterator.OptionValueWrapper;

/**
 * An enumeration of all tables in Drill's system ("sys") schema.
 * <p>
 *   OPTION, DRILLBITS and VERSION are local tables available on every Drillbit.
 *   MEMORY and THREADS are distributed tables with one record on every Drillbit.
 *   PROFILES and PROFILES_JSON are stored in local / distributed storage.
 * </p>
 */
public enum SystemTable implements DrillTableSelection {
  OPTIONS_OLD("options_old", false, OptionValueWrapper.class) {
    @Deprecated
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new OptionIterator(context, OptionIterator.Mode.SYS_SESS_PUBLIC);
    }
  },

  OPTIONS("options", false, ExtendedOptionIterator.ExtendedOptionValueWrapper.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new ExtendedOptionIterator(context, false);
    }
  },

  INTERNAL_OPTIONS_OLD("internal_options_old", false, OptionValueWrapper.class) {
    @Deprecated
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new OptionIterator(context, OptionIterator.Mode.SYS_SESS_INTERNAL);
    }
  },

  INTERNAL_OPTIONS("internal_options", false, ExtendedOptionIterator.ExtendedOptionValueWrapper.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new ExtendedOptionIterator(context, true);
    }
  },

  BOOT("boot", false, OptionValueWrapper.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new OptionIterator(context, OptionIterator.Mode.BOOT);
    }
  },

  DRILLBITS("drillbits", false,DrillbitIterator.DrillbitInstance.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new DrillbitIterator(context);
    }
  },

  VERSION("version", false, VersionIterator.VersionInfo.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new VersionIterator();
    }
  },

  MEMORY("memory", true, MemoryIterator.MemoryInfo.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new MemoryIterator(context);
    }
  },

  CONNECTIONS("connections", true, BitToUserConnectionIterator.ConnectionInfo.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new BitToUserConnectionIterator(context);
    }
  },

  PROFILES("profiles", false, ProfileInfoIterator.ProfileInfo.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new ProfileInfoIterator(context, maxRecords);
    }
  },

  PROFILES_JSON("profiles_json", false, ProfileJsonIterator.ProfileJson.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new ProfileJsonIterator(context, maxRecords);
    }
  },

  THREADS("threads", true, ThreadsIterator.ThreadsInfo.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new ThreadsIterator(context);
    }
  },

  FUNCTIONS("functions", false, FunctionsIterator.FunctionInfo.class) {
    @Override
    public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
      return new FunctionsIterator(context);
    }
  },

  STORAGE_ALIASES("storage_aliases", false, AliasesIterator.AliasInfo.class) {
    @Override
    public Iterator<Object> getIterator(ExecutorFragmentContext context, int maxRecords) {
      return new AliasesIterator(context, AliasTarget.STORAGE, maxRecords);
    }
  },

  TABLE_ALIASES("table_aliases", false, AliasesIterator.AliasInfo.class) {
    @Override
    public Iterator<Object> getIterator(ExecutorFragmentContext context, int maxRecords) {
      return new AliasesIterator(context, AliasTarget.TABLE, maxRecords);
    }
  };

  private final String tableName;
  private final boolean distributed;
  private final Class<?> pojoClass;

  SystemTable(final String tableName, final boolean distributed, final Class<?> pojoClass) {
    this.tableName = tableName;
    this.distributed = distributed;
    this.pojoClass = pojoClass;
  }

  public Iterator<Object> getIterator(final ExecutorFragmentContext context, final int maxRecords) {
    throw new UnsupportedOperationException(tableName + " must override this method.");
  }

  public String getTableName() {
    return tableName;
  }

  public boolean isDistributed() {
    return distributed;
  }

  public Class<?> getPojoClass() {
    return pojoClass;
  }

  @Override
  public String digest() {
    return toString();
  }
}
