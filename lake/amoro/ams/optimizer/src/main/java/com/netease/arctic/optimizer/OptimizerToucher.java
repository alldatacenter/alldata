package com.netease.arctic.optimizer;

import com.netease.arctic.ams.api.ArcticException;
import com.netease.arctic.ams.api.ErrorCodes;
import com.netease.arctic.ams.api.OptimizerRegisterInfo;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class OptimizerToucher extends AbstractOptimizerOperator {
  private static final Logger LOG = LoggerFactory.getLogger(OptimizerToucher.class);

  private OptimizerToucher.TokenChangeListener tokenChangeListener;
  private final Map<String, String> registerProperties = Maps.newHashMap();
  private long startTime;

  public OptimizerToucher(OptimizerConfig config) {
    super(config);
    this.startTime = System.currentTimeMillis();
  }

  public OptimizerToucher withTokenChangeListener(OptimizerToucher.TokenChangeListener tokenChangeListener) {
    this.tokenChangeListener = tokenChangeListener;
    return this;
  }

  public OptimizerToucher withRegisterProperty(String name, String value) {
    registerProperties.put(name, value);
    LOG.info("Adding register property {}:{} into optimizer", name, value);
    return this;
  }

  public void start() {
    LOG.info("Starting optimizer toucher with configuration:{}", getConfig());
    while (isStarted()) {
      try {
        if (checkToken()) {
          touch();
        }
        waitAShortTime(getConfig().getHeartBeat());
      } catch (Throwable t) {
        LOG.error("Optimizer toucher got an unexpected error", t);
      }
    }
    LOG.info("Optimizer toucher stopped");
  }

  private boolean checkToken() {
    if (!tokenIsReady()) {
      try {
        String token = callAms(client -> {
          OptimizerRegisterInfo registerInfo = new OptimizerRegisterInfo();
          registerInfo.setThreadCount(getConfig().getExecutionParallel());
          registerInfo.setMemoryMb(getConfig().getMemorySize());
          registerInfo.setGroupName(getConfig().getGroupName());
          registerInfo.setProperties(registerProperties);
          registerInfo.setResourceId(getConfig().getResourceId());
          registerInfo.setStartTime(startTime);
          return client.authenticate(registerInfo);
        });
        setToken(token);
        if (tokenChangeListener != null) {
          tokenChangeListener.tokenChange(token);
        }
        LOG.info("Registered optimizer to ams with token:{}", token);
        return true;
      } catch (TException e) {
        LOG.error("Register optimizer to ams failed", e);
        return false;
      }
    }
    return true;
  }

  private void touch() {
    try {
      callAms(client -> {
        client.touch(getToken());
        return null;
      });
      if (LOG.isDebugEnabled()) {
        LOG.debug("Optimizer[{}] touch ams", getToken());
      }
    } catch (TException e) {
      if (e instanceof ArcticException &&
          ErrorCodes.PLUGIN_RETRY_AUTH_ERROR_CODE == ((ArcticException)e).getErrorCode()) {
        setToken(null);
        LOG.error("Got authorization error from ams, try to register later", e);
      } else {
        LOG.error("Touch ams failed", e);
      }
    }
  }

  public interface TokenChangeListener {
    void tokenChange(String newToken);
  }

}
