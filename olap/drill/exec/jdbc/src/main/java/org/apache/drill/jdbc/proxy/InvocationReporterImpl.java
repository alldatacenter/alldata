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
package org.apache.drill.jdbc.proxy;

import java.lang.reflect.Method;
import java.sql.DriverPropertyInfo;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Implementation of InvocationReporter.
 * <p>
 *   Currently, just writes to System.err.
 * </p>
 */
class InvocationReporterImpl implements InvocationReporter
{
  private static final String LINE_PREFIX = "TRACER: ";
  private static final String SETUP_LINE_PREFIX   = LINE_PREFIX + "SETUP: ";
  private static final String WARNING_LINE_PREFIX = LINE_PREFIX + "WARNING: ";
  private static final String CALL_LINE_PREFIX    = LINE_PREFIX + "CALL:   ";
  private static final String RETURN_LINE_PREFIX  = LINE_PREFIX + "RETURN: ";
  private static final String THROW_LINE_PREFIX   = LINE_PREFIX + "THROW:  ";

  private static final Set<Package> JDBC_PACKAGES;
  static {
    // Load some class in each JDBC package below so package exists for
    // getPackage():
    // Suppressed because we intentionally are only assigning to the variable.
    @SuppressWarnings("unused")
    Class<?> someReference;
    someReference = java.sql.Connection.class;
    someReference = javax.sql.PooledConnection.class;
    someReference = javax.sql.rowset.BaseRowSet.class;
    someReference = javax.sql.rowset.serial.SerialJavaObject.class;
    someReference = javax.sql.rowset.spi.SyncFactory.class;

    Set<Package> set = new HashSet<>();
    set.add( Package.getPackage( "java.sql" ) );
    set.add( Package.getPackage( "javax.sql" ) );
    set.add( Package.getPackage( "javax.sql.rowset" ) );
    set.add( Package.getPackage( "javax.sql.rowset.serial" ) );
    set.add( Package.getPackage( "javax.sql.rowset.spi" ) );
    for ( Package p : set ) {
      assert null != p
          : "null Package; missing reference to class in that package?";
    }
    JDBC_PACKAGES = Collections.unmodifiableSet( set );
  }

  /** Common packages whose names to suppress in rendered type names. */
  // (Is sorted for abbreviated-packages message to user.)
  private static final SortedSet<Package> PACKAGES_TO_ABBREVIATE;
  static {
    SortedSet<Package> set = new TreeSet<Package>(
        new Comparator<Package>() {
          @Override
          public int compare( Package o1, Package o2 ) {
            return
                null == o1 ? -1
                    : null == o2 ? 1
                        : o1.getName().compareTo( o2.getName() );
          }
        } );
    set.addAll( JDBC_PACKAGES );
    set.add( Package.getPackage( "java.util" ) );
    set.add( Package.getPackage( "java.lang" ) );
    PACKAGES_TO_ABBREVIATE = Collections.unmodifiableSortedSet( set );
  }

  private int lastObjNum = 0;
  private Map<Object, String> objectsToIdsMap = new IdentityHashMap<>();


  void reportAbbreviatedPackages() {
    final List<String> names = new ArrayList<>();
    for ( Package p : PACKAGES_TO_ABBREVIATE ) {
      names.add( p.getName() );
    }
    setupMessage( "Abbreviating (unique) class names in packages "
                  + StringUtils.join( names, ", " ) + "." );
  }

  ////////////////////
  // Line-output output methods:

  // TODO:  When needed, allow output to something other then System.err
  // (e.g., appending to file, System.out).  Decide control--probably parameter
  // in proxy URL and/or JVM system property (checked higher up and configuring
  // this class).

  /**
   * Prints a line to the tracing log.
   * <p>
   *   Is it intended that all tracing output goes through this method.
   * </p>
   */
  private void printTraceLine( final String line ) {
    System.err.println( line );
  }


  /**
   * For warnings such as warning about encountering a type that for which
   * rendering isn't known to show values well.
   */
  private void printWarningLine( final String line ) {
    printTraceLine( WARNING_LINE_PREFIX + line );
  }


  ////////////////////
  // Type, value, exception, and arguments formatting methods:

  private String getObjectId( final Object object )
  {
    String id;
    if ( null == object ) {
      id = "n/a";
    }
    else {
      id = objectsToIdsMap.get( object );
      if ( null == id ) {
        ++lastObjNum;
        id = Integer.toString( lastObjNum );
        objectsToIdsMap.put( object, id );
      }
    }
    return id;
  }

  /**
   * Renders a type name.  Uses simple names for common types (JDBC interfaces
   * and {code java.lang.*}).
   */
  private String formatType( final Class<?> type ) {
    final String result;
    if ( type.isArray() ) {
      result = formatType( type.getComponentType() ) + "[]";
    } else {
      // Suppress package name for common (JDBC and java.lang) types, except
      // when would be ambiguous (e.g., java.sql.Date vs. java.util.Date).
      if ( PACKAGES_TO_ABBREVIATE.contains( type.getPackage() ) ) {
        int sameSimpleNameCount = 0;
        for ( Package p : PACKAGES_TO_ABBREVIATE ) {
          try {
            Class.forName( p.getName() + "." + type.getSimpleName() );
            sameSimpleNameCount++;
          }
          catch ( ClassNotFoundException e ) {
            // Nothing to do.
          }
        }
        if ( 1 == sameSimpleNameCount ) {
          result = type.getSimpleName();
        }
        else {
          // Multiple classes with same simple name, so would be ambiguous to
          // abbreviate, so use fully qualified name.
          result = type.getName();
        }
      }
      else {
        result = type.getName();
      }
    }
    return result;
  }

  private String formatString( final String value ) {
    return
        "\""
        + ( ((String) value)
             .replace( "\\", "\\\\" )  // first encode backslashes (esc. char.)
             .replace( "\"", "\\\"" )  // then encode quotes (via backslash)
             .replace( "\n", "\\n" )   // then encode newlines
             // Anything else?
            )
        + "\"";
  }

  private String formatDriverPropertyInfo( final DriverPropertyInfo info ) {
    return
        "[ "
        + "name = " + formatValue( info.name )
        + ", value = " + formatValue( info.value )
        + ", required = " + info.required
        + ", choices = " + formatValue( info.choices )
        + ", description = " + formatValue( info.description )
        + " ]";
  }

  private String formatValue( final Object value ) {
    final String result;
    if ( null == value ) {
      result = "null";
    }
    else {
      final Class<?> rawActualType = value.getClass();
      if ( String.class == rawActualType ) {
        result = formatString( (String) value );
      }
      else if ( rawActualType.isArray()
                && ! rawActualType.getComponentType().isPrimitive() ) {
        // Array of non-primitive type

        final StringBuilder buffer = new StringBuilder();
        /* Decide whether to includes this:
        buffer.append( formatType( elemType ) );
        buffer.append( "[] " );
        */
        buffer.append( "{ " );
        boolean first = true;
        for ( Object elemVal : (Object[]) value ) {
          if ( ! first ) {
            buffer.append( ", " );
          }
          first = false;
          buffer.append( formatValue( elemVal ) );
        }
        buffer.append( " }" );
        result = buffer.toString();
      }
      else if ( DriverPropertyInfo.class == rawActualType ) {
        result = formatDriverPropertyInfo( (DriverPropertyInfo) value );
      }
      else if (
          // Is type seen and whose toString() renders value well.
          false
          || rawActualType == java.lang.Boolean.class
          || rawActualType == java.lang.Byte.class
          || rawActualType == java.lang.Double.class
          || rawActualType == java.lang.Float.class
          || rawActualType == java.lang.Integer.class
          || rawActualType == java.lang.Long.class
          || rawActualType == java.lang.Short.class
          || rawActualType == java.math.BigDecimal.class
          || rawActualType == java.lang.Class.class
          || rawActualType == java.sql.Date.class
          || rawActualType == java.sql.Timestamp.class
          ) {
        result = value.toString();
      }
      else if (
          // Is type seen and whose toString() has rendered value well--in cases
          // seen so far.
          false
          || rawActualType == java.util.Properties.class
          || rawActualType.isEnum()
          ) {
        result = value.toString();
      }
      else if (
          // Is type to warn about (one case).
          false
          || rawActualType == org.apache.drill.jdbc.DrillResultSet.class
          ) {
        printWarningLine(
            "Class " + rawActualType.getName() + " should be an interface."
            + " (While it's a class, it can't be proxied, and some methods can't"
            + " be traced.)" );
        result = value.toString();
      }
      else if (
          // Is type to warn about (second case).
          false
          // Note:  Using strings rather than compiled-in class references to
          // avoid failing when run using JDBC-all Jar, which excludes
          // org.apache.hadoop.io.Text.
          // Note:  org.apache.hadoop.io.Text should no longer appear (see
          // DRILL-3347, but leaving warning in for now in case Text returns).
          || rawActualType.getName().equals( "org.apache.hadoop.io.Text" )
          || rawActualType.getName().equals( "org.joda.time.Period" )
          || rawActualType ==
             org.apache.drill.exec.vector.accessor.sql.TimePrintMillis.class
          ) {
        printWarningLine( "Should " + rawActualType
                          + " be appearing at JDBC interface?" );
        result = value.toString();
      }
      else {
        // Is other type--unknown whether it already formats well.
        // (No handled yet: byte[].)
        printWarningLine( "Unnoted type encountered in formatting (value might"
                          + " not render well): " + rawActualType + "." );
        result = value.toString();
      }
    }
    return result;
  }

  /**
   * Renders a value with its corresponding <em>declared</em> type.
   *
   * @param  declaredType
   *         the corresponding declared method parameter or return type
   * @value  value
   *         the value to render
   */
  private String formatTypeAndValue( Class<?> declaredType, Object value ) {
    final String declaredTypePart = "(" + formatType( declaredType ) + ") ";

    final String actualTypePart;
    final String actualValuePart;
    if ( null == value ) {
      // Null--show no actual type or object ID.
      actualTypePart = "";
      actualValuePart = formatValue( value );
    }
    else {
      // Non-null value--show at least some representation of value.
      Class<?> rawActualType = value.getClass();
      Class<?> origActualType =
          declaredType.isPrimitive() ? declaredType : rawActualType;
      if ( String.class == rawActualType ) {
        // String--show no actual type or object ID.
        actualTypePart = "";
        actualValuePart = formatValue( value );
      }
      else if ( origActualType.isPrimitive() ) {
        // Primitive type--show no actual type or object ID.
        actualTypePart = "";
        // (Remember--primitive type is wrapped here.)
        actualValuePart = value.toString();
      }
      else {
        // Non-primitive, non-String value--include object ID.
        final String idPrefix = "<id=" + getObjectId( value ) + "> ";
        if ( declaredType.isInterface()
             && JDBC_PACKAGES.contains( declaredType.getPackage() ) ) {
          // JDBC interface implementation class--show no actual type or value
          // (because object is proxied and therefore all uses will be traced).
          actualTypePart = "";
          actualValuePart = idPrefix + "...";
        }
        else if ( origActualType == declaredType ) {
          // Actual type is same as declared--don't show redundant actual type.
          actualTypePart = "";
          actualValuePart = idPrefix + formatValue( value );
        }
        else {
          // Other--show actual type and (try to) show value.
          actualTypePart = "(" + formatType( rawActualType) + ") ";
          actualValuePart = idPrefix + formatValue( value );
        }
      }
    }
    final String result = declaredTypePart + actualTypePart + actualValuePart;
    return result;
  }

  /**
   * Renders given type and value of target (receiver) of method call.
   */
  private String formatTargetTypeAndValue( Class<?> declaredType, Object value ) {
    return formatTypeAndValue( declaredType, value );
  }

  /**
   * Renders given type and value of method call argument.
   */
  // Expect JDBC interface types; (mostly) doesn't expect other JDBC types;
  // expect mostly primitives and String.
  private String formatArgTypeAndValue( Class<?> declaredType, Object value ) {
    return formatTypeAndValue( declaredType, value );
  }

  /**
   * Renders given type and value of method return value.
   */
  // Expect declared type Object (need actual type); expect primitive types
  // (maybe wrapper classes too?)
  private String formatReturnTypeAndValue( Class<?> declaredType, Object value ) {
    return formatTypeAndValue( declaredType, value );
  }

  /**
   * Renders given exception.
   * Includes test of chained exceptions.
   */
  private String formatThrowable( final Throwable thrown ) {
    final StringBuffer s = new StringBuffer();
    boolean first = true;
    Throwable current = thrown;
    while ( null != current ) {
      if ( ! first ) {
        s.append( " ==> ");
      }
      first = false;

      s.append( "(" );
      s.append( formatType( current.getClass() ) );
      s.append( ") ");
      s.append( formatString( current.toString() ) );
      current = current.getCause();
    }
    final String result = s.toString();
    return result;
  }

  /**
   * Renders corresponding given sequence of declared types and given sequence
   * of values from method call.
   */
  private String formatArgs( Class<?>[] declaredTypes, Object[] argValues )
  {
    final String result;
    if ( null == argValues ) {
      result = "()";
    }
    else {
      final StringBuilder s = new StringBuilder();
      s.append( "( " );
      for ( int ax = 0; ax < argValues.length; ax++ ) {
        if ( ax > 0 ) {
          s.append( ", " );
        }
        s.append( formatArgTypeAndValue( declaredTypes[ ax ], argValues[ ax ] ) );
      }
      s.append( " )" );
      result = s.toString();
    }
    return result;
  }

  /**
   * Renders the call part for a method call, method return, or exception-thrown
   * event.
   * @param target
   *        the target (receiver) of the method call
   * @param targetType
   *        the interface containing called method
   * @param method
   *        the called method
   * @param argValues
   *        the argument values (represented as for {@link Method#invoke})
   *
   */
  private String formatCallPart( final Object target,
                                 final Class<?> targetType,
                                 final Method method,
                                 final Object[] argValues ) {
    return
        "(" + formatTargetTypeAndValue( targetType, target ) + ") . "
        + method.getName() + formatArgs( method.getParameterTypes(), argValues );
  }


  ////////////////////
  // Invocation-level methods:

  @Override
  public void setupMessage( final String message ) {
    printTraceLine( SETUP_LINE_PREFIX + message );
  }

  @Override
  public void methodCalled( final Object target,
                            final Class<?> targetType,
                            final Method method,
                            final Object[] args ) {
    printTraceLine( CALL_LINE_PREFIX
                    + formatCallPart( target, targetType, method, args ) );
  }

  @Override
  public void methodReturned( final Object target,
                              final Class<?> targetType,
                              final Method method,
                              final Object[] args, final
                              Object result ) {
    final String callPart =
        RETURN_LINE_PREFIX + formatCallPart( target, targetType, method, args );
    if ( void.class == method.getReturnType() ) {
      assert null == result
          : "unexpected non-null result value " + result
            + " for method returning " + method.getReturnType();
      printTraceLine( callPart + ", RESULT: (none--void) " );
    } else {
      printTraceLine(
          callPart + ", RESULT: "
          + formatReturnTypeAndValue( method.getReturnType(), result ) );
    }
  }

  @Override
  public void methodThrew( final Object target,
                           final Class<?> targetType,
                           final Method method,
                           final Object[] args,
                           final Throwable exception ) {
    printTraceLine( THROW_LINE_PREFIX
                    + formatCallPart( target, targetType, method, args )
                    + ", threw: " + formatThrowable( exception ) );
  }

} // class SimpleInvocationReporter
