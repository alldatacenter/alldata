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
package org.apache.drill.common.exceptions;

import static java.lang.Thread.sleep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;

import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Base class for all user exception. The goal is to separate out common error conditions where we can give users
 * useful feedback.
 * <p>Throwing a user exception will guarantee it's message will be displayed to the user, along with any context
 * information added to the exception at various levels while being sent to the client.
 * <p>A specific class of user exceptions are system exception. They represent system level errors that don't display
 * any specific error message to the user apart from "A system error has occurred" along with information to retrieve
 * the details of the exception from the logs.
 * <p>Although system exception should only display a generic message to the user, for now they will display the root
 * error message, until all user errors are properly sent from the server side.
 * <p>Any thrown exception that is not wrapped inside a user exception will automatically be converted to a system
 * exception before being sent to the client.
 *
 * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType
 */
public class UserException extends DrillRuntimeException {
  private static final long serialVersionUID = -6720929331624621840L;
  private static final Logger logger = LoggerFactory.getLogger(UserException.class);

  public static final String MEMORY_ERROR_MSG = "One or more nodes ran out of memory while executing the query.";

  /**
   * Creates a RESOURCE error with a prebuilt message for out of memory exceptions
   *
   * @param cause exception that will be wrapped inside a memory error
   * @return resource error builder
   */
  public static Builder memoryError(final Throwable cause) {
    return UserException.resourceError(cause)
      .message(MEMORY_ERROR_MSG).addContext(cause.getMessage());
  }

  public static Builder memoryError(final String format, final Object... args) {
    final Builder builder =  UserException.resourceError();
    builder.message(MEMORY_ERROR_MSG);
    if (!format.isEmpty()) {
      builder.addContext(String.format(format, args));
    }
    return builder;
  }

  /**
   * Creates a RESOURCE error with a prebuilt message for out of memory exceptions
   *
   * @return resource error builder
   */
  public static Builder memoryError() {
    return memoryError("");
  }

  /**
   * Wraps the passed exception inside a system error.
   * <p>The cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>If the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#SYSTEM
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */

  public static Builder systemError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.SYSTEM, cause);
  }

  /**
   * Creates a new user exception builder.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#CONNECTION
   * @return user exception builder
   */
  public static Builder connectionError() {
    return connectionError(null);
  }

  /**
   * Wraps the passed exception inside a connection error.
   * <p>The cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>If the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#CONNECTION
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder connectionError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.CONNECTION, cause);
  }

  /**
   * Creates a new user exception builder.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#DATA_READ
   * @return user exception builder
   */
  public static Builder dataReadError() {
    return dataReadError(null);
  }

  /**
   * Wraps the passed exception inside a data read error.
   * <p>The cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>If the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#DATA_READ
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder dataReadError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.DATA_READ, cause);
  }

  /**
   * Creates a new user exception builder .
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#DATA_WRITE
   * @return user exception builder
   */
  public static Builder dataWriteError() {
    return dataWriteError(null);
  }

  /**
   * Wraps the passed exception inside a data write error.
   * <p>The cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>If the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#DATA_WRITE
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder dataWriteError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.DATA_WRITE, cause);
  }

  /**
   * Creates a new user exception builder .
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#FUNCTION
   * @return user exception builder
   */
  public static Builder functionError() {
    return functionError(null);
  }

  /**
   * Wraps the passed exception inside a function error.
   * <p>The cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>If the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#FUNCTION
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder functionError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.FUNCTION, cause);
  }

  /**
   * Creates a new user exception builder .
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#PARSE
   * @return user exception builder
   */
  public static Builder parseError() {
    return parseError(null);
  }

  /**
   * Wraps the passed exception inside a parse error.
   * <p>The cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>If the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#PARSE
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder parseError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.PARSE, cause);
  }

  /**
   * Creates a new user exception builder .
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#VALIDATION
   * @return user exception builder
   */
  public static Builder validationError() {
    return validationError(null);
  }

  /**
   * wraps the passed exception inside a validation error.
   * <p>the cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>if the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#VALIDATION
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder validationError(Throwable cause) {
    return new Builder(DrillPBError.ErrorType.VALIDATION, cause);
  }

  /**
   * creates a new user exception builder .
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#PERMISSION
   * @return user exception builder
   */
  public static Builder permissionError() {
    return permissionError(null);
  }

  /**
   * Wraps the passed exception inside a permission error.
   * <p>The cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>If the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#PERMISSION
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder permissionError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.PERMISSION, cause);
  }

  /**
   * Creates a new user exception builder .
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#PLAN
   * @return user exception builder
   */
  public static Builder planError() {
    return planError(null);
  }

  /**
   * Wraps the passed exception inside a plan error.
   * <p>The cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>If the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#PLAN
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder planError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.PLAN, cause);
  }

  /**
   * Creates a new user exception builder .
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#RESOURCE
   * @return user exception builder
   */
  public static Builder resourceError() {
    return resourceError(null);
  }

  /**
   * Wraps the passed exception inside a resource error.
   * <p>The cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>If the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#RESOURCE
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder resourceError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.RESOURCE, cause);
  }

  /**
   * Creates a new user exception builder .
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#UNSUPPORTED_OPERATION
   * @return user exception builder
   */
  public static Builder unsupportedError() {
    return unsupportedError(null);
  }

  /**
   * Wraps the passed exception inside a unsupported error.
   * <p>The cause message will be used unless {@link Builder#message(String, Object...)} is called.
   * <p>If the wrapped exception is, or wraps, a user exception it will be returned by {@link Builder#build(Logger)}
   * instead of creating a new exception. Any added context will be added to the user exception as well.
   *
   * @see org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType#UNSUPPORTED_OPERATION
   *
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder unsupportedError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.UNSUPPORTED_OPERATION, cause);
  }

  /**
   * Report an unsupported schema change.
   *
   * @param cause the <tt>SchemaChangeException</tt>. (Not typed because that
   * class is not visible to this package.)
   * @return user exception builder.
   */
  public static Builder schemaChangeError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.UNSUPPORTED_OPERATION, cause)
        .addContext("Unsupported schema change");
  }

  public static Builder schemaChangeError() {
    return schemaChangeError(null);
  }

  /**
   * Wraps an error that arises from execution due to issues in the query, in
   * the environment and so on -- anything other than "this should never occur"
   * type checks.
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */

  public static Builder executionError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.EXECUTION_ERROR, cause);
  }

  /**
   * Indicates an internal validation failed or similar unexpected error. Indicates
   * the problem is likely within Drill itself rather than due to the environment,
   * query, etc.
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */

  public static Builder internalError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.INTERNAL_ERROR, cause);
  }

  public static Builder internalError() {
    return new Builder(DrillPBError.ErrorType.INTERNAL_ERROR, null);
  }

  /**
   * Indicates an unspecified error: code caught the exception, but does not have
   * visibility into the cause well enough to pick one of the more specific
   * error types. In practice, using this exception indicates that error handling
   * should be moved closer to the source of the exception so we can provide the
   * user with a better explanation than "something went wrong."
   * @param cause exception we want the user exception to wrap. If cause is, or wrap, a user exception it will be
   *              returned by the builder instead of creating a new user exception
   * @return user exception builder
   */
  public static Builder unspecifiedError(final Throwable cause) {
    return new Builder(DrillPBError.ErrorType.UNSPECIFIED_ERROR, cause);
  }

  /**
   * Builder class for DrillUserException. You can wrap an existing exception, in this case it will first check if
   * this exception is, or wraps, a DrillUserException. If it does then the builder will use the user exception as it is
   * (it will ignore the message passed to the constructor) and will add any additional context information to the
   * exception's context
   */
  public static class Builder {

    private final Throwable cause;
    private final DrillPBError.ErrorType errorType;
    private final UserException uex;
    private final UserExceptionContext context;

    private String message;

    /**
     * Wraps an existing exception inside a user exception.
     *
     * @param errorType user exception type that should be created if the passed exception isn't,
     *                  or doesn't wrap a user exception
     * @param cause exception to wrap inside a user exception. Can be null
     */
    private Builder(final DrillPBError.ErrorType errorType, final Throwable cause) {
      this.cause = cause;

      //TODO handle the improbable case where cause is a SYSTEM exception ?
      uex = ErrorHelper.findWrappedUserException(cause);
      if (uex != null) {
        this.errorType = null;
        this.context = uex.context;
      } else {
        // we will create a new user exception
        this.errorType = errorType;
        this.context = new UserExceptionContext();
        this.message = cause != null ? cause.getMessage() : null;
      }
    }

    private Builder(UserException uex) {
      this.uex = uex;
      cause = uex.getCause();
      errorType = uex.errorType;
      context = uex.context;
      message = uex.getOriginalMessage();
    }

    /**
     * sets or replaces the error message.
     * <p>This will be ignored if this builder is wrapping a user exception
     *
     * @see String#format(String, Object...)
     *
     * @param format format string
     * @param args Arguments referenced by the format specifiers in the format string
     * @return this builder
     */
    public Builder message(final String format, final Object... args) {
      // we can't replace the message of a user exception
      if (uex == null && format != null) {
        if (args.length == 0) {
          message = format;
        } else {
          message = String.format(format, args);
        }
      }
      return this;
    }

    /**
     * add DrillbitEndpoint identity to the context.
     * <p>if the context already has a drillbitEndpoint identity, the new identity will be ignored
     *
     * @param endpoint drillbit endpoint identity
     */
    public Builder addIdentity(final CoordinationProtos.DrillbitEndpoint endpoint) {
      context.add(endpoint);
      return this;
    }

    /**
     * add a string line to the bottom of the context
     * @param value string line
     * @return this builder
     */
    public Builder addContext(final String value) {
      context.add(value);
      return this;
    }

    /**
     * add a string line to the bottom of the context
     * @param value string line
     * @return this builder
     */
    public Builder addContext(final String value, Object... args) {
      context.add(String.format(value, args));
      return this;
    }

    /**
     * add a string value to the bottom of the context
     *
     * @param name context name
     * @param value context value
     * @return this builder
     */
    public Builder addContext(final String name, final String value) {
      context.add(name, value);
      return this;
    }

    /**
     * add a long value to the bottom of the context
     *
     * @param name context name
     * @param value context value
     * @return this builder
     */
    public Builder addContext(final String name, final long value) {
      context.add(name, value);
      return this;
    }

    /**
     * add a double value to the bottom of the context
     *
     * @param name context name
     * @param value context value
     * @return this builder
     */
    public Builder addContext(final String name, final double value) {
      context.add(name, value);
      return this;
    }

    public Builder addContext(CustomErrorContext context) {
      if (context != null) {
        context.addContext(this);
      }
      return this;
    }

    /**
     * pushes a string value to the top of the context
     *
     * @param value context value
     * @return this builder
     */
    public Builder pushContext(final String value) {
      context.push(value);
      return this;
    }

    /**
     * pushes a string value to the top of the context
     *
     * @param name context name
     * @param value context value
     * @return this builder
     */
    public Builder pushContext(final String name, final String value) {
      context.push(name, value);
      return this;
    }

    /**
     * pushes a long value to the top of the context
     *
     * @param name context name
     * @param value context value
     * @return this builder
     */
    public Builder pushContext(final String name, final long value) {
      context.push(name, value);
      return this;
    }

    /**
     * pushes a double value to the top of the context
     *
     * @param name context name
     * @param value context value
     * @return this builder
     */
    public Builder pushContext(final String name, final double value) {
      context.push(name, value);
      return this;
    }

    private static final File SPIN_FILE =  new File("/tmp/drill/spin");
    /**
     * builds a user exception or returns the wrapped one. If the error is a system error, the error message is logged
     * to the given {@link Logger}.
     *
     * @param logger the logger to write to
     * @return user exception
     */
    public UserException build(final Logger logger) {

      // To allow for debugging:
      //
      // A spinner code to make the execution stop here while the file '/tmp/drill/spin' exists
      // Can be used to attach a debugger, use jstack, etc
      // (do "clush -a touch /tmp/drill/spin" to turn this on across all the cluster nodes, and to
      // release the spinning threads do "clush -a rm /tmp/drill/spin")
      // The processID of the spinning thread (along with the error message) should then be found
      // in a file like  /tmp/drill/spin4148663301172491613.tmp
      if (SPIN_FILE.exists()) {
        final File tmpDir = new File("/tmp/drill");
        File outErr = null;
        try {
          outErr = File.createTempFile("spin", ".tmp", tmpDir);
          final BufferedWriter bw = new BufferedWriter(new FileWriter(outErr));
          bw.write("Spinning process: " + ManagementFactory.getRuntimeMXBean().getName()
          /* After upgrading to JDK 9 - replace with: ProcessHandle.current().getPid() */);
          bw.write("\nError cause: " +
            (errorType == DrillPBError.ErrorType.SYSTEM ? ("SYSTEM ERROR: " + ErrorHelper.getRootMessage(cause)) : message));
          bw.close();
        } catch (final Exception ex) {
          logger.warn("Failed creating a spinner tmp message file: {}", ex);
        }
        while (SPIN_FILE.exists()) {
          try { sleep(1_000); } catch (final Exception ex) { /* ignore interruptions */ }
        }
        try { outErr.delete(); } catch (final Exception ex) { } // cleanup - remove err msg file
      }

      if (uex != null) {
        return uex;
      }

      final boolean isSystemError = errorType == DrillPBError.ErrorType.SYSTEM;

      // make sure system errors use the root error message and display the root cause class name
      if (isSystemError) {
        message = ErrorHelper.getRootMessage(cause);
      }

      final UserException newException = new UserException(this);

      // since we just created a new exception, we should log it for later reference. If this is a system error, this is
      // an issue that the Drill admin should pay attention to and we should log as ERROR. However, if this is a user
      // mistake or data read issue, the system admin should not be concerned about these and thus we'll log this
      // as an INFO message.
      if (isSystemError) {
        logger.error(newException.getMessage(), newException);
      } else {
        final StringBuilder buf = new StringBuilder();
        buf.append("User Error Occurred");
        if (message != null) {
          buf.append(": ").append(message);
        }
        if (cause != null) {
          buf.append(" (").append(cause.getMessage()).append(")");
        }
        logger.info(buf.toString(), newException);
      }

      return newException;
    }

    /**
     * Builds a user exception or returns the wrapped one.
     *
     * @return user exception
     * @deprecated Use {@link #build(Logger)} instead. If the error is a system error, the error message is logged to
     * this {@link UserException#logger}.
     */
    @Deprecated
    public UserException build() {
      return build(logger);
    }
  }

  private final DrillPBError.ErrorType errorType;

  private final UserExceptionContext context;

  protected UserException(final DrillPBError.ErrorType errorType, final String message, final Throwable cause) {
    super(message, cause);

    this.errorType = errorType;
    this.context = new UserExceptionContext();
  }

  private UserException(final Builder builder) {
    super(builder.message, builder.cause);
    this.errorType = builder.errorType;
    this.context = builder.context;
  }

  public Builder rebuild() {
    return new Builder(this);
  }

  /**
   * generates the message that will be displayed to the client without the stack trace.
   *
   * @return non verbose error message
   */
  @Override
  public String getMessage() {
    return generateMessage(true);
  }

  public String getMessage(boolean includeErrorIdAndIdentity) {
    return generateMessage(includeErrorIdAndIdentity);
  }

  /**
   *
   * @return the error message that was passed to the builder
   */
  public String getOriginalMessage() {
    return super.getMessage();
  }

  /**
   * generates the message that will be displayed to the client. The message also contains the stack trace.
   *
   * @return verbose error message
   */
  public String getVerboseMessage() {
    return getVerboseMessage(true);
  }

  public String getVerboseMessage(boolean includeErrorIdAndIdentity) {
    return generateMessage(includeErrorIdAndIdentity) + "\n\n" + ErrorHelper.buildCausesMessage(getCause());
  }

  /**
   * returns or creates a DrillPBError object corresponding to this user exception.
   *
   * @param verbose should the error object contain the verbose error message ?
   * @return protobuf error object
   */
  public DrillPBError getOrCreatePBError(final boolean verbose) {
    final String message = verbose ? getVerboseMessage() : getMessage();

    final DrillPBError.Builder builder = DrillPBError.newBuilder();
    builder.setErrorType(errorType);
    builder.setErrorId(context.getErrorId());
    if (context.getEndpoint() != null) {
      builder.setEndpoint(context.getEndpoint());
    }
    builder.setMessage(message);

    if (getCause() != null) {
      // some unit tests use this information to make sure a specific exception was thrown in the server
      builder.setException(ErrorHelper.getWrapper(getCause()));
    } else {
      // not a wrapper exception
      builder.setException(ErrorHelper.getWrapper(this));
    }
    return builder.build();
  }

  public String getErrorId() {
    return context.getErrorId();
  }

  public DrillPBError.ErrorType getErrorType() {
    return errorType;
  }

  public String getErrorLocation() {
    final DrillbitEndpoint ep = context.getEndpoint();
    if (ep != null) {
      return ep.getAddress() + ":" + ep.getUserPort();
    } else {
      return null;
    }
  }
  /**
   * Generates a user error message that has the following structure:
   * ERROR TYPE: ERROR_MESSAGE
   * CONTEXT
   * [ERROR_ID on DRILLBIT_IP:DRILLBIT_USER_PORT]
   *
   * @return generated user error message
   */
  private String generateMessage(boolean includeErrorIdAndIdentity) {
    final boolean seeLogsMessage = errorType == DrillPBError.ErrorType.INTERNAL_ERROR
        || errorType == DrillPBError.ErrorType.SYSTEM;
    return errorType + " ERROR: " + super.getMessage() + "\n\n" +
        context.generateContextMessage(includeErrorIdAndIdentity, seeLogsMessage);
  }

}
