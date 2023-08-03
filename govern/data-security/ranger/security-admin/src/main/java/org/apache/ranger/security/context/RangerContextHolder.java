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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 /**
 *
 */
package org.apache.ranger.security.context;

public class RangerContextHolder {

    private static final ThreadLocal<RangerSecurityContext> securityContextThreadLocal = new ThreadLocal<RangerSecurityContext>();

    private static final ThreadLocal<RangerAdminOpContext> operationContextThreadLocal = new ThreadLocal<RangerAdminOpContext>();

    private RangerContextHolder() {

    }

    public static RangerSecurityContext getSecurityContext(){
	return securityContextThreadLocal.get();
    }

    public static void setSecurityContext(RangerSecurityContext context){
	securityContextThreadLocal.set(context);
    }

    public static void resetSecurityContext(){
	securityContextThreadLocal.remove();
    }

	public static RangerAdminOpContext getOpContext() {
		return operationContextThreadLocal.get();
	}

	public static void setOpContext(RangerAdminOpContext context) {
		operationContextThreadLocal.set(context);
	}

	public static void resetOpContext() {
		operationContextThreadLocal.remove();
	}
}
