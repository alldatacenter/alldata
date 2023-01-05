/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

/**
 *
 */
package com.qlangtech.tis.trigger;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * @date 2012-7-30
 */
public interface TriggerJobConsole extends Remote {

	public List<JobDesc> getAllJobsInServer() throws RemoteException;

	public List<JobDesc> getJob(String indexName, Long jobid)
			throws RemoteException;

	public boolean isServing(String coreName) throws RemoteException;

	/**
	 * 停止执行
	 *
	 * @param
	 * @throws RemoteException
	 */
	public void pause(String coreName) throws RemoteException;

	/**
	 * core是否是任务终止状态
	 *
	 * @param coreName
	 * @return
	 * @throws RemoteException
	 */
	public boolean isPause(String coreName) throws RemoteException;

	/**
	 * 重新启动
	 *
	 * @param coreName
	 * @throws RemoteException
	 */
	public void resume(String coreName) throws RemoteException;
}
