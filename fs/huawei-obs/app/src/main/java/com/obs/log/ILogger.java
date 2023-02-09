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


public interface ILogger {
    public boolean isInfoEnabled();
    
    public void info(CharSequence msg);
    
    public void info(Object obj);
    
    public void info(Object obj, Throwable e);

    public boolean isWarnEnabled();

    public void warn(CharSequence msg);
    
    public void warn(Object obj);
    
    public void warn(Object obj, Throwable e);

    public boolean isErrorEnabled();

    public void error(CharSequence msg);
    
    public void error(Object obj);
    
    public void error(Object obj, Throwable e);
    
    public boolean isDebugEnabled();

    public void debug(CharSequence msg);
    
    public void debug(Object obj);
    
    public void debug(Object obj, Throwable e);

    public boolean isTraceEnabled();

    public void trace(CharSequence msg);
    
    public void trace(Object obj);
    
    public void trace(Object obj, Throwable e);

    public void accessRecord(Object object);
}
