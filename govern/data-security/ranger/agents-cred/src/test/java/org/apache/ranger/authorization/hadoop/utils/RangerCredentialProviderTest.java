/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.hadoop.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.alias.CredentialProvider;
import org.apache.hadoop.security.alias.CredentialShell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RangerCredentialProviderTest {
	
	private final File ksFile;
	private final String keystoreFile;
	private final String[] argsCreate;
	private final String[] argsDelete;
	private final String url;
	RangerCredentialProvider cp = null;
	List<CredentialProvider> providers = null;
	
	
	public RangerCredentialProviderTest() throws IOException {
		ksFile = File.createTempFile("testkeystore", "jceks");
		keystoreFile = ksFile.toURI().getPath();
		url = "jceks://file@/" + keystoreFile;
		
		if (isCredentialShellInteractiveEnabled()) {
			argsCreate = new String[] {"create", "TestCredential001", "-f",  "-value", "PassworD123", "-provider", "jceks://file@/" + keystoreFile};
			argsDelete = new String[] {"delete", "TestCredential001", "-f" , "-provider", "jceks://file@/" + keystoreFile};
		} else {
			argsCreate = new String[] {"create", "TestCredential001", "-value", "PassworD123", "-provider", "jceks://file@/" + keystoreFile};
			argsDelete = new String[] {"delete", "TestCredential001", "-provider", "jceks://file@/" + keystoreFile};
		}
	}
	
	
	@Before
	public void setup() throws Exception {
		int ret;
		//
		// adding a delete before creating a keystore
		//
		try {
			if (ksFile != null) {
				if (ksFile.exists()) {
					System.out.println("Keystore File [" + ksFile.getAbsolutePath() + "] is available - and deleting");
					ksFile.delete();
					System.out.println("Keystore File [" + ksFile.getAbsolutePath() + "] is deleted.");
				} else {
					System.out.println("Keystore File [" + ksFile.getAbsolutePath() + "] is not available");
				}
			} else {
				System.out.println("Keystore File is NULL");
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
		
		Configuration conf = new Configuration();
		CredentialShell cs = new CredentialShell();
		cs.setConf(conf);
		try {
			 ret = cs.run(argsCreate);
		} catch (Exception e) {
			throw e;
		}
		assertEquals(0, ret);
		System.out.println("(1) Number of active Threads : " + Thread.activeCount());
		listThreads();
	}
	
	@After
	public void cleanup() throws Exception {
		if (ksFile != null && ksFile.exists()) {
			ksFile.delete();
		}
	}
	
	@Test
	public void testCredentialProvider() {
		//test credential provider is registered and return credential providers.
		cp = new RangerCredentialProvider();
		providers = cp.getCredentialProviders(url);
		if (providers != null) {
			assertTrue(url.equals(providers.get(0).toString()));
		}
		System.out.println("(2) Number of active Threads : " + Thread.activeCount());
		listThreads();
	}
	
	@Test
	public void testCredentialString() {
		//test credential provider created is returning the correct credential string.
		cp = new RangerCredentialProvider();
		providers = cp.getCredentialProviders(url);
		if (providers != null) {
			assertTrue("PassworD123".equals(cp.getCredentialString(url, "TestCredential001")));
		}
		System.out.println("(3) Number of active Threads : " + Thread.activeCount());
		listThreads();
	}

	
	@After
	public void teardown() throws Exception {
		System.out.println("In teardown : Number of active Threads : " + Thread.activeCount() );
		int ret;
		Configuration conf = new Configuration();
		CredentialShell cs = new CredentialShell();
		cs.setConf(conf);
		try {
			 ret = cs.run(argsDelete);
		} catch (Exception e) {
			throw e;
		}
		assertEquals(0, ret);
		listThreads();
	}
	
	private static void listThreads() {
		int ac = Thread.activeCount();
		if (ac > 0) {
			Thread[] tlist = new Thread[ac];
			Thread.enumerate(tlist);
			for (Thread t : tlist) {
				System.out.println("Thread [" + t + "] => {" + t.getClass().getName() + "}");
			}
		}
	}
	
	private static boolean isCredentialShellInteractiveEnabled() {
		boolean ret = false;
		
		String fieldName = "interactive";
		
		CredentialShell cs = new CredentialShell();
		
		try {
			Field interactiveField = cs.getClass().getDeclaredField(fieldName);
			
			if (interactiveField != null) {
				interactiveField.setAccessible(true);
				ret = interactiveField.getBoolean(cs);
				System.out.println("FOUND value of [" + fieldName + "] field in the Class [" + cs.getClass().getName() + "] = [" + ret + "]");
			}
		} catch (Throwable e) {
			System.out.println("Unable to find the value of [" + fieldName + "] field in the Class [" + cs.getClass().getName() + "]. Skiping -f option");
			e.printStackTrace();
			ret = false;
		}
		
		return ret;
		
		
	}

}

