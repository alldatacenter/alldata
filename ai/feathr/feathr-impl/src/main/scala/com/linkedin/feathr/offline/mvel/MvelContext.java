package com.linkedin.feathr.offline.mvel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.linkedin.feathr.common.FeatureValue;
import com.linkedin.feathr.common.util.MvelContextUDFs;
import com.linkedin.feathr.offline.mvel.plugins.FeathrExpressionExecutionContext;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.integration.PropertyHandler;
import org.mvel2.integration.PropertyHandlerFactory;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.OptimizerFactory;

import java.util.*;


/**
 * Some MVEL hackery to enable use in Feathr.
 *
 * MVEL is an open-source expression language and runtime that makes it easy to write concise statements that operate
 * on structured data objects (such as Avro records), among other things.
 */
public class MvelContext {
  private MvelContext() { }

  private static final ParserConfiguration PARSER_CONFIG = new ParserConfiguration();

  // External UDF register class
  public static Optional<Broadcast<Class<?>>> mvelAlienUDFRegisterClazz = Optional.empty();
  // Flag to avoid init external UDF everytime
  public static Boolean alienUDFInitialized = false;
  static {

    MvelContextUDFs.registerUDFs(MvelContextUDFs.class, PARSER_CONFIG);

    loadJavaClasses();

    // There is a bug in MVEL (jira.codehaus.org/browse/MVEL-291) which causes sporadic runtime errors when
    // custom PropertyHandlers are used with MVEL's default "asm" optimizer if the PropertyHandlers don't also
    // implement ProducesBytecode, which apparently tricky to do and requires knowledge of JVM assembly.
    // As a workaround, we can use MVEL's reflective optimizer, which is not supposed to be as fast.
    // Someday later we can implement the bytecode to try to improve performance.
    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);

    // register GenericRecordPropertyHandler, so that MVEL could get variables from Avro GenericRecord.
    PropertyHandlerFactory.registerPropertyHandler(GenericRecord.class, GenericRecordPropertyHandler.INSTANCE);

    // register GenericRowWithSchemaPropertyHandler, so that MVEL could get variables from SparkSql GenericRowWithSchema.
    PropertyHandlerFactory.registerPropertyHandler(GenericRowWithSchema.class, GenericRowWithSchemaPropertyHandler.INSTANCE);

  }

  /**
   * Load Java Class Library explicitly into ParserConfiguration.
   *
   * These classes have been loaded by MVEL in AbstractParser; however, a bug in MVEL
   * ReflectiveOptimizerAccessor.getBeanProperty prevents names to be resolved against
   * them when a custom PropertyHandler is used. Thus we load them here again to go
   * around that code path.
   */
  private static void loadJavaClasses() {
    PARSER_CONFIG.addImport("System", System.class);
    PARSER_CONFIG.addImport("String", String.class);
    PARSER_CONFIG.addImport("CharSequence", CharSequence.class);

    PARSER_CONFIG.addImport("Integer", Integer.class);
    PARSER_CONFIG.addImport("int", int.class);

    PARSER_CONFIG.addImport("Long", Long.class);
    PARSER_CONFIG.addImport("long", long.class);

    PARSER_CONFIG.addImport("Boolean", Boolean.class);
    PARSER_CONFIG.addImport("boolean", boolean.class);

    PARSER_CONFIG.addImport("Short", Short.class);
    PARSER_CONFIG.addImport("short", short.class);

    PARSER_CONFIG.addImport("Character", Character.class);
    PARSER_CONFIG.addImport("char", char.class);

    PARSER_CONFIG.addImport("Double", Double.class);
    PARSER_CONFIG.addImport("double", double.class);

    PARSER_CONFIG.addImport("Float", Float.class);
    PARSER_CONFIG.addImport("float", float.class);

    PARSER_CONFIG.addImport("Byte", Byte.class);
    PARSER_CONFIG.addImport("byte", byte.class);

    PARSER_CONFIG.addImport("Math", Math.class);
    PARSER_CONFIG.addImport("Void", Void.class);
    PARSER_CONFIG.addImport("Object", Object.class);
    PARSER_CONFIG.addImport("Number", Number.class);
  }

  // ensure the class is loaded in memory.
  public static void ensureInitialized() {
    if (!alienUDFInitialized && mvelAlienUDFRegisterClazz.isPresent()) {
      MvelContextUDFs.registerUDFs(mvelAlienUDFRegisterClazz.get().getValue(), PARSER_CONFIG);
      alienUDFInitialized = true;
    }
  }

  /**
   * Gives access to our helper methods from within an MVEL expression.
   * @return an MVEL {@link ParserContext} in which our helper UDFs are available
   */
  public static ParserContext newParserContext() {
    return new ParserContext(PARSER_CONFIG);
  }

  /**
   * Evaluate MVEL expression as per {@link MVEL#executeExpression(Object, Object)}, with added support for
   * {@link com.linkedin.feathr.offline.mvel.plugins.FeathrMvelPluginContext}. (Output objects that can be converted
   * to {@link FeatureValue} via plugins, will be converted after MVEL returns.)
   */
  public static Object executeExpressionWithPluginSupport(Object compiledExpression, Object ctx, FeathrExpressionExecutionContext mvelContext) {
    Object output = MVEL.executeExpression(compiledExpression, ctx);
    return coerceToFeatureValueViaMvelDataConversionPlugins(output, mvelContext);
  }

  /**
   * Evaluate MVEL expression as per {@link MVEL#executeExpression(Object, Object, VariableResolverFactory)}, with added support for
   * {@link com.linkedin.feathr.offline.mvel.plugins.FeathrMvelPluginContext}. (Output objects that can be converted
   * to {@link FeatureValue} via plugins, will be converted after MVEL returns.)
   */
  public static Object executeExpressionWithPluginSupportWithFactory(Object compiledExpression,
                                                                     Object ctx,
                                                                     VariableResolverFactory variableResolverFactory,
                                                                     FeathrExpressionExecutionContext mvelContext) {
    Object newCtx = coerceToFeatureValueViaMvelDataConversionPlugins(ctx, mvelContext);
    Object output = MVEL.executeExpression(compiledExpression, newCtx, variableResolverFactory);
    return coerceToFeatureValueViaMvelDataConversionPlugins(output, mvelContext);
  }

  private static Object coerceToFeatureValueViaMvelDataConversionPlugins(Object input, FeathrExpressionExecutionContext mvelContext) {
    // Convert the input to feature value using the given MvelContext if possible
    if (input != null && mvelContext!= null && mvelContext.canConvert(FeatureValue.class, input.getClass())) {
      return mvelContext.convert(input, FeatureValue.class);
    } else {
      return input;
    }
  }

  /**
   * Allows easy access to the properties of GenericRecord object from MVEL.
   */
  public static final class GenericRecordPropertyHandler implements PropertyHandler {
    public static final GenericRecordPropertyHandler INSTANCE = new GenericRecordPropertyHandler();

    private GenericRecordPropertyHandler() { }

    @Override
    public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory) {
      Object output = ((GenericRecord) contextObj).get(name);
      return sanitize(output);
    }

    @Override
    public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Allows easy access to the properties of GenericRowWithSchema(generated from spark SQL) object from MVEL.
   */
  private static final class GenericRowWithSchemaPropertyHandler implements PropertyHandler {
    public static final GenericRowWithSchemaPropertyHandler INSTANCE = new GenericRowWithSchemaPropertyHandler();

    private GenericRowWithSchemaPropertyHandler() { }

    @Override
    public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory) {
      GenericRowWithSchema row = (GenericRowWithSchema) contextObj;
      int index;
      try {
        index = row.fieldIndex(name);
      } catch (IllegalArgumentException e1) {
        // if the field doesn't exist, try again with lowercase field name.
        try {
          index = row.fieldIndex(name.toLowerCase());
        } catch (IllegalArgumentException e2) {
          // if the field doesn't exist, we throw the original exception with the original field name.
          throw e1;
        }
      }
      Object output = ((GenericRowWithSchema) contextObj).get(index);
      if (output == null) {
        return output;
      }
      //Convert scala map type to java map type as MVEL only supports java map type.
      if (output instanceof scala.collection.immutable.Map) {
        return scala.collection.JavaConverters.mapAsJavaMapConverter((scala.collection.immutable.Map) output).asJava();
      }
      // mvel cannot handle wrappedArray, need to convert to java list
      // simply do .asJava will not work here
      if (output instanceof scala.collection.mutable.WrappedArray) {
        scala.collection.mutable.WrappedArray arr = (scala.collection.mutable.WrappedArray) output;
        ArrayList<Object> newOutput = new ArrayList<Object>();
        for (int i = 0; i < arr.length(); i++) {
          newOutput.add(arr.apply(i));
        }
        return newOutput;
      }
      return output;
    }

    @Override
    public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * "Sanitize" objects extracted from a GenericRecord into more convenient formats.  Convenient formats are important
   * because we want to make it easy for our clients to manipulate these things via simple MVEL expressions.<p>
   *
   * Utf8 and GenericEnumSymbol will be converted to Strings for easier handling.<p>
   *
   * Maps will be wrapped in a decorator which similarly sanitizes keys and values appropriately.
   *
   * @param input null is allowed; will result in null being returned.
   * @return the sanitized object
   */
  @SuppressWarnings("unchecked")
  private static Object sanitize(Object input) {
    if (input instanceof Map) {
      // For cases where the AvroMap have key represented as "String" instead of Utf8
      // there is not need to sanitize it to Utf8
      Map<?, ?> map = (Map<?, ?>) input;
      if (!map.isEmpty() && (map.keySet().iterator().next() instanceof Utf8)) {
        return new AvroDecoratorMap((Map<Utf8, Object>) input);
      } else {
        return map;
      }
    }
    if (input instanceof Utf8 || input instanceof GenericEnumSymbol || input instanceof Enum) {
      return input.toString();
    }
    return input;
  }

  /**
   * Maps extracted from GenericRecords are inconvenient for a few reasons:<ul>
   *  <li>their keys are Utf8s instead of Strings</li>
   *  <li>their values are Utf8s instead of Strings</li>
   *  <li>they can contain values of type GenericEnumSymbol which is hard to work using MVEL; we'd prefer a String</li>
   *  <li>they can contain values of type Map which have these same inconveniences</li>
   *</ul>
   * Unfortunately I'm not able to find these features well-documented in some specification separate from the
   * actual Avro implementation.  Consequently this class's behavior may be incompatible with future versions of Avro.
   * To fix this, we might look into using SpecificRecord instead of GenericRecord in StatServer, but that will be a
   * considerable change we will leave for another day.<p>
   *
   * Implementation note: I don't think there is a use case for the keySet(), entrySet(), and value() methods, but I
   * implemented them "just in case" and for completeness.  They will generally not be used and are somewhat expensive
   * to compute, so they are lazily loaded when needed.
   */
  private static class AvroDecoratorMap implements Map<String, Object> {
    private final Map<Utf8, Object> _underlying;

    private Set<String> _keys = null;
    private Collection<Object> _values = null;
    private Set<Entry<String, Object>> _entries = null;

    /**
     * Wrap a Utf8-keyed map we got from an Avro record
     * @param map the map to wrap
     */
    public AvroDecoratorMap(Map<Utf8, Object> map) {
      _underlying = map;
    }

    @Override
    public boolean containsKey(Object key) {
      if (!(key instanceof String)) {
        return false;
      }
      return _underlying.containsKey(new Utf8((String) key));
    }

    @Override
    public Object get(Object key) {
      return sanitize(_underlying.get(new Utf8((String) key)));
    }

    @Override
    public Set<String> keySet() {
      if (_keys != null) {
        return _keys;
      }
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (Utf8 key : _underlying.keySet()) {
        builder.add(key.toString()); // we could have potential issues here if more than one Utf8 mapped to the same String. is that possible?
      }
      _keys = builder.build();
      return _keys;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      if (_entries != null) {
        return _entries;
      }
      ImmutableSet.Builder<Entry<String, Object>> output = ImmutableSet.builder();
      for (Entry<Utf8, Object> entry : _underlying.entrySet()) {
        output.add(new AbstractMap.SimpleImmutableEntry<String, Object>(entry.getKey().toString(),
            sanitize(entry.getValue())));
      }
      _entries = output.build();
      return _entries;
    }

    @Override
    public Collection<Object> values() {
      if (_values != null) {
        return _values;
      }
      ImmutableList.Builder<Object> builder = ImmutableList.builder();
      for (Object value : _underlying.values()) {
        builder.add(sanitize(value));
      }
      _values = builder.build();
      return _values;
    }

    @Override
    public boolean containsValue(Object value) {
      return values().contains(value);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof AvroDecoratorMap) {
        AvroDecoratorMap other = (AvroDecoratorMap) o;
        if (_underlying == other._underlying) {
          return true;
        } else {
          return _underlying.equals(other._underlying);
        }
      } else if (o instanceof Map) {
        return entrySet().equals(((Map<?, ?>) o).entrySet());
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return entrySet().hashCode();
    }

    @Override
    public int size() {
      return _underlying.size();
    }

    @Override
    public boolean isEmpty() {
      return _underlying.isEmpty();
    }

    @Override
    public String toString() {
      return _underlying.toString();
    }

    @Override
    public Object put(String key, Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }
}
