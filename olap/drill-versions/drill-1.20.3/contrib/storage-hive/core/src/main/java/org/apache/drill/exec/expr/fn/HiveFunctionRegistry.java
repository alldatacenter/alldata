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
package org.apache.drill.exec.expr.fn;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.fun.OracleSqlOperatorTable;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.fn.impl.hive.ObjectInspectorHelper;
import org.apache.drill.exec.planner.sql.DrillOperatorTable;
import org.apache.drill.exec.planner.sql.HiveUDFOperator;
import org.apache.drill.exec.planner.sql.HiveUDFOperatorWithoutInference;
import org.apache.drill.exec.planner.sql.TypeInferenceUtils;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.hive.ql.udf.UDFType;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDFBridge;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

public class HiveFunctionRegistry implements PluggableFunctionRegistry {
  private static final Logger logger = LoggerFactory.getLogger(HiveFunctionRegistry.class);

  /**
   * Map for renaming UDFs. Keys of the map represent UDF names which should be replaced
   * and its values represent target UDF names.
   */
  private static final Map<String, String> FUNCTION_REPLACE_MAP = ImmutableMap.<String, String> builder()
      // renames Hive's TRANSLATE UDF to TRANSLATE3 due to CALCITE-1115
      .put(SqlStdOperatorTable.TRANSLATE.getName().toLowerCase(),
          OracleSqlOperatorTable.TRANSLATE3.getName().toLowerCase())
      .build();

  private final Multimap<String, Class<? extends GenericUDF>> methodsGenericUDF = ArrayListMultimap.create();
  private final Multimap<String, Class<? extends UDF>> methodsUDF = ArrayListMultimap.create();
  private final Set<Class<?>> nonDeterministicUDFs = new HashSet<>();

  /**
   * Scan the classpath for implementation of GenericUDF/UDF interfaces,
   * extracts function annotation and store the
   * (function name) --> (implementation class) mappings.
   * @param config
   */
  public HiveFunctionRegistry(DrillConfig config) {
    // TODO: see if we can avoid this. We can't change the constructor right now.
    ScanResult classpathScan = ClassPathScanner.fromPrescan(config);
    Set<Class<? extends GenericUDF>> genericUDFClasses = classpathScan.getImplementations(GenericUDF.class);
    for (Class<? extends GenericUDF> clazz : genericUDFClasses) {
      register(clazz, methodsGenericUDF);
    }

    Set<Class<? extends UDF>> udfClasses = classpathScan.getImplementations(UDF.class);
    for (Class<? extends UDF> clazz : udfClasses) {
      register(clazz, methodsUDF);
    }

    if (logger.isTraceEnabled()) {
      StringBuilder allHiveFunctions = new StringBuilder();
      for (Map.Entry<String, Class<? extends GenericUDF>> method : methodsGenericUDF.entries()) {
        allHiveFunctions.append(method.toString()).append("\n");
      }
      logger.trace("Registered Hive GenericUDFs: [\n{}]", allHiveFunctions);

      StringBuilder allUDFs = new StringBuilder();
      for (Map.Entry<String, Class<? extends UDF>> method : methodsUDF.entries()) {
        allUDFs.append(method.toString()).append("\n");
      }
      logger.trace("Registered Hive UDFs: [\n{}]", allUDFs);
      StringBuilder allNonDeterministic = new StringBuilder();
      for (Class<?> clz : nonDeterministicUDFs) {
        allNonDeterministic.append(clz.toString()).append("\n");
      }
      logger.trace("Registered Hive nonDeterministicUDFs: [\n{}]", allNonDeterministic);
    }
  }

  @Override
  public void register(DrillOperatorTable operatorTable) {
    for (String name : Sets.union(methodsGenericUDF.asMap().keySet(), methodsUDF.asMap().keySet())) {
      operatorTable.addOperatorWithoutInference(name, new HiveUDFOperatorWithoutInference(name.toUpperCase()));
      operatorTable.addOperatorWithInference(name, new HiveUDFOperator(name.toUpperCase(), new HiveSqlReturnTypeInference()));
    }
  }

  private <I> void register(Class<? extends I> clazz, Multimap<String, Class<? extends I>> methods) {
    Description desc = clazz.getAnnotation(Description.class);
    Stream<String> namesStream;
    if (desc != null) {
      namesStream = Stream.of(desc.name().split(","))
          .map(String::trim);
    } else {
      namesStream = Stream.of(clazz)
          .map(Class::getName)
          .map(name -> name.replace('.', '_'));
    }

    // Checks specified array of function names whether they should be replaced
    // using FUNCTION_REPLACE_MAP map.
    namesStream.map(String::toLowerCase)
        .map(functionName -> FUNCTION_REPLACE_MAP.getOrDefault(functionName, functionName))
        .forEach(name -> methods.put(name, clazz));

    UDFType type = clazz.getAnnotation(UDFType.class);
    if (type != null && !type.deterministic()) {
      nonDeterministicUDFs.add(clazz);
    }
  }

  /**
   * Find the UDF class for given function name and check if it accepts the given input argument
   * types. If a match is found, create a holder and return
   * @param call
   * @return
   */
  @Override
  public HiveFuncHolder getFunction(FunctionCall call) {
    HiveFuncHolder h;

    h = resolveFunction(call, false);
    if (h != null) {
      return h;
    }

    return resolveFunction(call, true);
  }

  /**
   * Helper method which resolves the given function call to a Hive UDF. It takes an argument
   * <i>varCharToStringReplacement</i> which tells to use hive STRING(true) or VARCHAR(false) type for drill VARCHAR type
   * and search Hive UDF registry using this replacement.
   *
   * TODO: This is a rudimentary function resolver. Need to include more implicit casting such as DECIMAL28 to
   * DECIMAL38 as Hive UDFs can accept only DECIMAL38 type.
   */
  private HiveFuncHolder resolveFunction(FunctionCall call, boolean varCharToStringReplacement) {
    HiveFuncHolder holder;
    MajorType[] argTypes = new MajorType[call.argCount()];
    ObjectInspector[] argOIs = new ObjectInspector[call.argCount()];
    for (int i=0; i<call.argCount(); i++) {
      try {
        argTypes[i] = call.arg(i).getMajorType();
        argOIs[i] = ObjectInspectorHelper.getDrillObjectInspector(argTypes[i].getMode(), argTypes[i].getMinorType(),
            varCharToStringReplacement);
      } catch(Exception e) {
        // Hive throws errors if there are unsupported types. Consider there is no hive UDF supporting the
        // given argument types
        logger.trace("Failed to find a hive function for given FunctionCall: '{}'", call.toString(), e);
        return null;
      }
    }

    String funcName = call.getName().toLowerCase();

    // search in GenericUDF list
    for (Class<? extends GenericUDF> clazz: methodsGenericUDF.get(funcName)) {
      holder = matchAndCreateGenericUDFHolder(clazz, argTypes, argOIs);
      if (holder != null) {
        return holder;
      }
    }

    // search in UDF list
    for (Class<? extends UDF> clazz : methodsUDF.get(funcName)) {
      holder = matchAndCreateUDFHolder(call.getName(), clazz, argTypes, argOIs);
      if (holder != null) {
        return holder;
      }
    }

    return null;
  }

  private HiveFuncHolder matchAndCreateGenericUDFHolder(Class<? extends GenericUDF> udfClazz,
                                              MajorType[] argTypes,
                                              ObjectInspector[] argOIs) {
    // probe UDF to find if the arg types and acceptable
    // if acceptable create a holder object
    try {
      GenericUDF udfInstance = udfClazz.newInstance();
      ObjectInspector returnOI = udfInstance.initialize(argOIs);
      return new HiveFuncHolder(
        udfClazz,
        argTypes,
        returnOI,
        Types.optional(ObjectInspectorHelper.getDrillType(returnOI)),
        nonDeterministicUDFs.contains(udfClazz));
    } catch (IllegalAccessException | InstantiationException e) {
      logger.debug("Failed to instantiate class", e);
    } catch (Exception e) { /*ignore this*/ }

    return null;
  }

  private HiveFuncHolder matchAndCreateUDFHolder(String udfName,
                                                 Class<? extends UDF> udfClazz,
                                                 MajorType[] argTypes,
                                                 ObjectInspector[] argOIs) {
    try {
      GenericUDF udfInstance = new GenericUDFBridge(udfName, false/* is operator */, udfClazz.getName());
      ObjectInspector returnOI = udfInstance.initialize(argOIs);

      return new HiveFuncHolder(
        udfName,
        udfClazz,
        argTypes,
        returnOI,
        Types.optional(ObjectInspectorHelper.getDrillType(returnOI)),
        nonDeterministicUDFs.contains(udfClazz));
    } catch (Exception e) { /*ignore this*/ }

    return null;
  }

  public class HiveSqlReturnTypeInference implements SqlReturnTypeInference {
    private HiveSqlReturnTypeInference() {

    }

    @Override
    public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
      for (RelDataType type : opBinding.collectOperandTypes()) {
        final TypeProtos.MinorType minorType = TypeInferenceUtils.getDrillTypeFromCalciteType(type);
        if(minorType == TypeProtos.MinorType.LATE) {
          return opBinding.getTypeFactory()
              .createTypeWithNullability(
                  opBinding.getTypeFactory().createSqlType(SqlTypeName.ANY),
                  true);
        }
      }

      final FunctionCall functionCall = TypeInferenceUtils.convertSqlOperatorBindingToFunctionCall(opBinding);
      final HiveFuncHolder hiveFuncHolder = getFunction(functionCall);
      if (hiveFuncHolder == null) {
        final StringBuilder operandTypes = new StringBuilder();
        for (int j = 0; j < opBinding.getOperandCount(); ++j) {
          RelDataType operandType = opBinding.getOperandType(j);
          operandTypes.append(operandType.getSqlTypeName());
          if (operandType.isNullable()) {
            operandTypes.append(":OPTIONAL");
          }
          if (j < opBinding.getOperandCount() - 1) {
            operandTypes.append(",");
          }
        }

        throw UserException
            .functionError()
            .message(String.format("%s does not support operand types (%s)",
                opBinding.getOperator().getName(),
                operandTypes))
            .build(logger);
      }

      return TypeInferenceUtils.convertToCalciteType(
          opBinding.getTypeFactory(),
          hiveFuncHolder.getReturnType(),
          hiveFuncHolder.getReturnType().getMode() != TypeProtos.DataMode.REQUIRED);
    }
  }
}
