package com.netease.arctic.optimizer;

import com.netease.arctic.ams.api.ArcticException;
import com.netease.arctic.ams.api.ErrorCodes;
import com.netease.arctic.ams.api.OptimizingService;
import com.netease.arctic.ams.api.client.OptimizingClientPools;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractOptimizerOperator implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractOptimizerOperator.class);
  private static long CALL_AMS_INTERVAL = 5000;//5s

  private final OptimizerConfig config;
  private final AtomicReference<String> token = new AtomicReference<>();
  private boolean stopped = false;

  public AbstractOptimizerOperator(OptimizerConfig config) {
    Preconditions.checkNotNull(config);
    this.config = config;
  }

  protected <T> T callAms(AmsCallOperation<T> operation) throws TException {
    while (isStarted()) {
      try {
        return operation.call(OptimizingClientPools.getClient(config.getAmsUrl()));
      } catch (Throwable t) {
        if (shouldReturnNull(t)) {
          return null;
        } else if (shouldRetryLater(t)) {
          LOG.error("Call ams got an error and will try again later", t);
          waitAShortTime();
        } else {
          throw t;
        }
      }
    }
    throw new IllegalStateException("Operator is stopped");
  }

  private boolean shouldRetryLater(Throwable t) {
    if (t instanceof ArcticException) {
      ArcticException arcticException = (ArcticException) t;
      //Call ams again when got a persistence/undefined error
      return ErrorCodes.PERSISTENCE_ERROR_CODE == arcticException.getErrorCode() ||
          ErrorCodes.UNDEFINED_ERROR_CODE == arcticException.getErrorCode();
    } else {
      //Call ams again when got an unexpected error
      return true;
    }
  }

  // Return null if got MISSING_RESULT error
  private boolean shouldReturnNull(Throwable t) {
    if (t instanceof TApplicationException) {
      TApplicationException applicationException = (TApplicationException) t;
      return applicationException.getType() == TApplicationException.MISSING_RESULT;
    }
    return false;
  }

  protected <T> T callAuthenticatedAms(AmsAuthenticatedCallOperation<T> operation) throws TException {
    while (isStarted()) {
      if (tokenIsReady()) {
        String token = getToken();
        try {
          return operation.call(OptimizingClientPools.getClient(config.getAmsUrl()), token);
        } catch (Throwable t) {
          if (t instanceof ArcticException &&
              ErrorCodes.PLUGIN_RETRY_AUTH_ERROR_CODE == ((ArcticException) (t)).getErrorCode()) {
            //Reset the token when got a authorization error
            LOG.error("Got a authorization error while calling ams, reset token and wait for a new one", t);
            resetToken(token);
          } else if (shouldReturnNull(t)) {
            return null;
          } else if (shouldRetryLater(t)) {
            LOG.error("Call ams got an error and will try again later", t);
            waitAShortTime();
          } else {
            throw t;
          }
        }
      } else {
        LOG.debug("Optimizer wait for token is ready");
        waitAShortTime();
      }
    }
    throw new IllegalStateException("Operator is stopped");
  }

  protected OptimizerConfig getConfig() {
    return config;
  }

  protected String getToken() {
    return token.get();
  }

  protected boolean tokenIsReady() {
    return token.get() != null;
  }

  protected void resetToken(String oldToken) {
    token.compareAndSet(oldToken, null);
  }

  public void setToken(String newToken) {
    token.set(newToken);
  }

  public boolean isStarted() {
    return !stopped;
  }

  public void stop() {
    this.stopped = true;
  }

  protected void waitAShortTime() {
    waitAShortTime(CALL_AMS_INTERVAL);
  }

  protected void waitAShortTime(long waitTime) {
    try {
      TimeUnit.MILLISECONDS.sleep(waitTime);
    } catch (InterruptedException e) {
      // ignore
    }
  }

  interface AmsCallOperation<T> {
    T call(OptimizingService.Iface client) throws TException;
  }

  interface AmsAuthenticatedCallOperation<T> {
    T call(OptimizingService.Iface client, String token) throws TException;
  }
}
