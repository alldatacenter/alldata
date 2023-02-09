/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */


package com.obs.log;

import java.util.logging.Logger;

import com.obs.services.internal.utils.AccessLoggerUtils;

public abstract class AbstractLog4jLogger {
    private static final Logger ILOG = Logger.getLogger(AbstractLog4jLogger.class.getName());
    
    protected final Object logger;
    
    AbstractLog4jLogger(Object logger) {
        this.logger = logger;
    }
    
    public void info(CharSequence msg) {
        if (this.logger != null && LoggerMethodHolder.info != null) {
            try {
                LoggerMethodHolder.info.invoke(this.logger, msg, null);
                AccessLoggerUtils.appendLog(msg, "info");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }

    public void info(Object obj) {
        if (this.logger != null && LoggerMethodHolder.info != null) {
            try {
                LoggerMethodHolder.info.invoke(this.logger, obj, null);
                AccessLoggerUtils.appendLog(obj, "info");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }

    public void info(Object obj, Throwable e) {
        if (this.logger != null && LoggerMethodHolder.info != null) {
            try {
                LoggerMethodHolder.info.invoke(this.logger, obj, e);
                AccessLoggerUtils.appendLog(obj, "info");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }
    
    public void warn(CharSequence msg) {
        if (this.logger != null && LoggerMethodHolder.warn != null) {
            try {
                LoggerMethodHolder.warn.invoke(this.logger, msg, null);
                AccessLoggerUtils.appendLog(msg, "warn");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }

    public void warn(Object obj) {
        if (this.logger != null && LoggerMethodHolder.warn != null) {
            try {
                LoggerMethodHolder.warn.invoke(this.logger, obj, null);
                AccessLoggerUtils.appendLog(obj, "warn");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }

    public void warn(Object obj, Throwable e) {
        if (this.logger != null && LoggerMethodHolder.warn != null) {
            try {
                LoggerMethodHolder.warn.invoke(this.logger, obj, e);
                AccessLoggerUtils.appendLog(obj, "warn");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }
    
    public void error(CharSequence msg) {
        if (this.logger != null && LoggerMethodHolder.error != null) {
            try {
                LoggerMethodHolder.error.invoke(this.logger, msg, null);
                AccessLoggerUtils.appendLog(msg, "error");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }

    public void error(Object obj) {
        if (this.logger != null && LoggerMethodHolder.error != null) {
            try {
                LoggerMethodHolder.error.invoke(this.logger, obj, null);
                AccessLoggerUtils.appendLog(obj, "error");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }

    public void error(Object obj, Throwable e) {
        if (this.logger != null && LoggerMethodHolder.error != null) {
            try {
                LoggerMethodHolder.error.invoke(this.logger, obj, e);
                AccessLoggerUtils.appendLog(obj, "error");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }
    
    public void debug(CharSequence msg) {
        if (this.logger != null && LoggerMethodHolder.debug != null) {
            try {
                LoggerMethodHolder.debug.invoke(this.logger, msg, null);
                AccessLoggerUtils.appendLog(msg, "debug");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }

    public void debug(Object obj) {
        if (this.logger != null && LoggerMethodHolder.debug != null) {
            try {
                LoggerMethodHolder.debug.invoke(this.logger, obj, null);
                AccessLoggerUtils.appendLog(obj, "debug");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }

    public void debug(Object obj, Throwable e) {
        if (this.logger != null && LoggerMethodHolder.debug != null) {
            try {
                LoggerMethodHolder.debug.invoke(this.logger, obj, e);
                AccessLoggerUtils.appendLog(obj, "debug");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }
    
    public void trace(CharSequence msg) {
        if (this.logger != null && LoggerMethodHolder.trace != null) {
            try {
                LoggerMethodHolder.trace.invoke(this.logger, msg, null);
                AccessLoggerUtils.appendLog(msg, "trace");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }

    public void trace(Object obj) {
        if (this.logger != null && LoggerMethodHolder.trace != null) {
            try {
                LoggerMethodHolder.trace.invoke(this.logger, obj, null);
                AccessLoggerUtils.appendLog(obj, "trace");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }

    public void trace(Object obj, Throwable e) {
        if (this.logger != null && LoggerMethodHolder.trace != null) {
            try {
                LoggerMethodHolder.trace.invoke(this.logger, obj, e);
                AccessLoggerUtils.appendLog(obj, "trace");
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }
    
    public void accessRecord(Object obj) {
        if (this.logger != null && LoggerMethodHolder.info != null) {
            try {
                LoggerMethodHolder.info.invoke(this.logger, obj, null);
            } catch (Exception ex) {
                ILOG.warning(ex.getMessage());
            }
        }
    }
}
