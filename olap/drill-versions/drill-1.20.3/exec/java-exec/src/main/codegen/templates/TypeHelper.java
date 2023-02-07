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
import org.apache.drill.exec.vector.complex.UnionVector;

<@pp.dropOutputFile />
<@pp.changeOutputFile name="/org/apache/drill/exec/expr/TypeHelper.java" />

<#include "/@includes/license.ftl" />

package org.apache.drill.exec.expr;

<#include "/@includes/vv_imports.ftl" />
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.accessor.*;
import org.apache.drill.exec.vector.complex.RepeatedMapVector;
import org.apache.drill.exec.util.CallBack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */
public class TypeHelper extends BasicTypeHelper {
  static final Logger logger = LoggerFactory.getLogger(TypeHelper.class);

  public static SqlAccessor getSqlAccessor(ValueVector vector){
    final MajorType type = vector.getField().getType();
    switch(type.getMinorType()){
    case UNION:
      return new UnionSqlAccessor((UnionVector) vector);
    <#list vv.types as type>
    <#list type.minor as minor>
    case ${minor.class?upper_case}:
      switch (type.getMode()) {
        case REQUIRED:
          return new ${minor.class}Accessor((${minor.class}Vector) vector);
        case OPTIONAL:
          return new Nullable${minor.class}Accessor((Nullable${minor.class}Vector) vector);
        case REPEATED:
          return new GenericAccessor(vector);
      }
    </#list>
    </#list>
    case MAP:
    case DICT:
    case LIST:
    case NULL:
      return new GenericAccessor(vector);
    default:
      throw new UnsupportedOperationException(buildErrorMessage("find sql accessor", type));
    }
  }

  public static JType getHolderType(JCodeModel model, MinorType type, DataMode mode){
    switch (type) {
    case UNION:
      return model._ref(UnionHolder.class);
    case DICT:
      switch (mode) {
        case REQUIRED:
        case OPTIONAL:
          return model._ref(DictHolder.class);
        case REPEATED:
          return model._ref(RepeatedDictHolder.class);
      }
    case MAP:
    case LIST:
      return model._ref(ComplexHolder.class);

<#list vv.types as type>
  <#list type.minor as minor>
    case ${minor.class?upper_case}:
      switch (mode) {
        case REQUIRED:
          return model._ref(${minor.class}Holder.class);
        case OPTIONAL:
          return model._ref(Nullable${minor.class}Holder.class);
        case REPEATED:
          return model._ref(Repeated${minor.class}Holder.class);
      }
  </#list>
</#list>
    case GENERIC_OBJECT:
      return model._ref(ObjectHolder.class);
    case NULL:
      return model._ref(UntypedNullHolder.class);
    default:
      throw new UnsupportedOperationException(buildErrorMessage("get holder type", type, mode));
    }
  }

  public static JType getComplexHolderType(JCodeModel model, MinorType type, DataMode mode) {
    switch (type) {
      case DICT:
        switch (mode) {
          case REQUIRED:
          case OPTIONAL:
            return model._ref(DictHolder.class);
          case REPEATED:
            return model._ref(RepeatedDictHolder.class);
        }
      case MAP:
        switch (mode) {
          case REQUIRED:
          case OPTIONAL:
            return model._ref(MapHolder.class);
          case REPEATED:
            return model._ref(RepeatedMapHolder.class);
        }
      case LIST:
        switch (mode) {
          case REQUIRED:
          case OPTIONAL:
            return model._ref(ListHolder.class);
          case REPEATED:
            return model._ref(RepeatedListHolder.class);
        }
      default:
        throw new IllegalArgumentException("Complex type expected. Found: " + type);
    }
  }
}
