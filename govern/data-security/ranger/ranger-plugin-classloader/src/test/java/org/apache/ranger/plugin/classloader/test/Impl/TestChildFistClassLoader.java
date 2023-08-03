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

package org.apache.ranger.plugin.classloader.test.Impl;

import java.io.File;
import java.net.URL;

import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.apache.ranger.plugin.classloader.test.TestPlugin;
import org.apache.ranger.plugin.classloader.test.TestPrintParent;

public class TestChildFistClassLoader {

	public static void main(String [] args){
		TestPrintParent testPrint = new TestPrintParent();
		System.out.println(testPrint.getString());
		File   file = null;
		URL[]  urls = null;
		try {
			file = new File(".." + File.separatorChar + "TestPluginImpl.class");
		    URL url = file.toPath().toUri().toURL();		
		    urls = new URL[] {url};
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String[] libdirs = new String[] { file.getAbsolutePath() };
	
		try {
			@SuppressWarnings("resource")
			RangerPluginClassLoader rangerPluginClassLoader = new RangerPluginClassLoader("hdfs", TestChildFistClassLoader.class);
			TestPlugin testPlugin = (TestPlugin) rangerPluginClassLoader.loadClass("org.apache.ranger.plugin.classloader.test.Impl.TestPluginImpl").newInstance();
			System.out.println(testPlugin.print());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
