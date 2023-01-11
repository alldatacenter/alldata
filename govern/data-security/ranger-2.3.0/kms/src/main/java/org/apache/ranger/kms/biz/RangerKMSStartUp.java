/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.kms.biz;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.crypto.key.RangerKeyStoreProvider;
import org.apache.hadoop.crypto.key.RangerMasterKey;
import org.springframework.stereotype.Component;

@SuppressWarnings("serial")
@Component
public class RangerKMSStartUp extends HttpServlet
{	
	public static final String ENCRYPTION_KEY = "ranger.db.encrypt.key.password";
	private static final Logger LOG = LoggerFactory.getLogger(RangerKMSStartUp.class);
	
	@PostConstruct
	public void initRangerMasterKey() {
		LOG.info("Ranger KMSStartUp");
		RangerMasterKey rangerMasterKey = new RangerMasterKey();
		try {
				Configuration conf = RangerKeyStoreProvider.getDBKSConf();
				String password = conf.get(ENCRYPTION_KEY);
				boolean check = rangerMasterKey.generateMasterKey(password);
				if(check){
					LOG.info("MasterKey Generated..");
				}
		} catch (Throwable e) {
			e.printStackTrace();
		}				
	}	
}
