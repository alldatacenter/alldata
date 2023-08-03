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

 package org.apache.ranger.credentialapi;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.alias.CredentialProvider;
import org.apache.hadoop.security.alias.CredentialProviderFactory;
import org.apache.hadoop.security.alias.JavaKeyStoreProvider;
public class CredentialReader {

	public static String getDecryptedString(String CrendentialProviderPath,String alias, String storeType) {
		  String credential=null;
		  try{
			  if(CrendentialProviderPath==null || alias==null||CrendentialProviderPath.trim().isEmpty()||alias.trim().isEmpty()){
				  return null;
			  }
			  char[] pass = null;
			  Configuration conf = new Configuration();
			  String crendentialProviderPrefixJceks=JavaKeyStoreProvider.SCHEME_NAME + "://file";
			  String crendentialProviderPrefixLocalJceks="localjceks://file";
			  crendentialProviderPrefixJceks=crendentialProviderPrefixJceks.toLowerCase();

			  String crendentialProviderPrefixBcfks= "bcfks" + "://file";
			  String crendentialProviderPrefixLocalBcfks= "localbcfks" + "://file";
			  crendentialProviderPrefixBcfks=crendentialProviderPrefixBcfks.toLowerCase();
			  crendentialProviderPrefixLocalBcfks=crendentialProviderPrefixLocalBcfks.toLowerCase();

			  CrendentialProviderPath=CrendentialProviderPath.trim();
			  alias=alias.trim();
			  if(CrendentialProviderPath.toLowerCase().startsWith(crendentialProviderPrefixJceks) ||
					  CrendentialProviderPath.toLowerCase().startsWith(crendentialProviderPrefixLocalJceks) ||
					  CrendentialProviderPath.toLowerCase().startsWith(crendentialProviderPrefixBcfks) ||
					  CrendentialProviderPath.toLowerCase().startsWith(crendentialProviderPrefixLocalBcfks)){
				  conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH,
						  //UserProvider.SCHEME_NAME + ":///," +
						  CrendentialProviderPath);
			  }else{
				  if(CrendentialProviderPath.startsWith("/")){
					  if(StringUtils.equalsIgnoreCase(storeType, "bcfks")) {
						  conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, CrendentialProviderPath);
					  } else {
						  conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH,
								  //UserProvider.SCHEME_NAME + ":///," +
								  JavaKeyStoreProvider.SCHEME_NAME + "://file" + CrendentialProviderPath);
					  }

				  }else{
					  conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH,
							  //UserProvider.SCHEME_NAME + ":///," +
							  JavaKeyStoreProvider.SCHEME_NAME + "://file/" + CrendentialProviderPath);
				  }
			  }
			  List<CredentialProvider> providers = CredentialProviderFactory.getProviders(conf);
			  List<String> aliasesList=new ArrayList<String>();
			  CredentialProvider.CredentialEntry credEntry=null;
			  for(CredentialProvider provider: providers) {
	              //System.out.println("Credential Provider :" + provider);
				  aliasesList=provider.getAliases();
				  if(aliasesList!=null && aliasesList.contains(alias.toLowerCase())){
					  credEntry=null;
					  credEntry= provider.getCredentialEntry(alias.toLowerCase());
					  pass = credEntry.getCredential();
					  if(pass!=null && pass.length>0){
						  credential=String.valueOf(pass);
						  break;
					  }				
				  }
			  }
		  }catch(Exception ex){
			  ex.printStackTrace();
			  credential=null;
		  }
		  return credential;
	  }
}
