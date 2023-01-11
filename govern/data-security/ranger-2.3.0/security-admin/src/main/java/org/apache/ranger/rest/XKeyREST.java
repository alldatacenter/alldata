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
package org.apache.ranger.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.ranger.biz.KmsKeyMgr;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.SearchUtil;
import org.apache.ranger.common.annotation.RangerAnnotationJSMgrName;
import org.apache.ranger.security.context.RangerAPIList;
import org.apache.ranger.view.VXKmsKey;
import org.apache.ranger.view.VXKmsKeyList;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sun.jersey.api.client.UniformInterfaceException;


@Path("keys")
@Component
@Scope("request")
@RangerAnnotationJSMgrName("KeyMgr")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class XKeyREST {
	private static final Logger logger = LoggerFactory.getLogger(XKeyREST.class);

	private static String UNAUTHENTICATED_MSG = "Unauthenticated : Please check the permission in the policy for the user";
	
	@Autowired
	KmsKeyMgr keyMgr;
		
	@Autowired
	SearchUtil searchUtil;
	
	@Autowired
	RESTErrorUtil restErrorUtil;
	
	/**
	 * Implements the traditional search functionalities for Keys
	 *
	 * @param request
	 * @return
	 */
	@GET
	@Path("/keys")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.SEARCH_KEYS + "\")")
	public VXKmsKeyList searchKeys(@Context HttpServletRequest request, @QueryParam("provider") String provider) {
		VXKmsKeyList vxKmsKeyList = new VXKmsKeyList();
		try{
			vxKmsKeyList = keyMgr.searchKeys(request, provider);
		}catch(Exception e){
			handleError(e);						
		}
		return vxKmsKeyList;
	}
	
	/**
	 * Implements the Rollover key functionality
	 * @param vXKey
	 * @return
	 */
	@PUT
	@Path("/key")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.ROLLOVER_KEYS + "\")")
	public VXKmsKey rolloverKey(@QueryParam("provider") String provider, VXKmsKey vXKey) {
		VXKmsKey vxKmsKey = new VXKmsKey();
		try{
			String name = vXKey.getName();
			if (name == null || name.isEmpty()) {
				throw restErrorUtil.createRESTException("Please provide a valid "
						+ "alias.", MessageEnums.INVALID_INPUT_DATA);
			}
			if(vXKey.getCipher() == null || vXKey.getCipher().trim().isEmpty()){
				vXKey.setCipher(null);
			}
			vxKmsKey = keyMgr.rolloverKey(provider, vXKey);
		}catch(Exception e){
			handleError(e);
		}
		return vxKmsKey;
	}	
	
	/**
	 * Implements the delete key functionality
	 * @param name
	 * @param request
	 */
	@DELETE
	@Path("/key/{alias}")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.DELETE_KEY + "\")")
	public void deleteKey(@PathParam("alias") String name, @QueryParam("provider") String provider, @Context HttpServletRequest request) {
		try{
			if (name == null || name.isEmpty()) {
				throw restErrorUtil.createRESTException("Please provide a valid "
						+ "alias.", MessageEnums.INVALID_INPUT_DATA);
			}
			keyMgr.deleteKey(provider, name);
		}catch(Exception e){
			handleError(e);
		}
	}
	
	/**
	 * Implements the create key functionality
	 * @param vXKey
	 * @return
	 */
	@POST
	@Path("/key")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.CREATE_KEY + "\")")
	public VXKmsKey createKey(@QueryParam("provider") String provider, VXKmsKey vXKey) {
		VXKmsKey vxKmsKey = new VXKmsKey();
		try{
			String name = vXKey.getName();
			if (name == null || name.isEmpty()) {
				throw restErrorUtil.createRESTException("Please provide a valid "
						+ "alias.", MessageEnums.INVALID_INPUT_DATA);
			}
			if(vXKey.getCipher() == null || vXKey.getCipher().trim().isEmpty()){
				vXKey.setCipher(null);
			}
			vxKmsKey = keyMgr.createKey(provider, vXKey);
		}catch(Exception e){
			handleError(e);
		}
		return vxKmsKey;
	}
	
	/**
	 *
	 * @param name
	 * @param provider
	 * @return
	 */
	@GET
	@Path("/key/{alias}")
	@Produces({ "application/json", "application/xml" })
	@PreAuthorize("@rangerPreAuthSecurityHandler.isAPIAccessible(\"" + RangerAPIList.GET_KEY + "\")")
	public VXKmsKey getKey(@PathParam("alias") String name,@QueryParam("provider") String provider){
		VXKmsKey vxKmsKey = new VXKmsKey();
		try{
			if (name == null || name.isEmpty()) {
				throw restErrorUtil.createRESTException("Please provide a valid "
						+ "alias.", MessageEnums.INVALID_INPUT_DATA);
			}
			vxKmsKey = keyMgr.getKey(provider, name);
		}catch(Exception e){
			handleError(e);
		}
		return vxKmsKey;
	}
	
	private void handleError(Exception e) {
		String message = e.getMessage();
		if (e instanceof UniformInterfaceException){
			 UniformInterfaceException uie=(UniformInterfaceException)e;
			 message = uie.getResponse().getEntity(String.class);
			 logger.error(message);
			 try {
				JSONObject objRE = new JSONObject(message);
				message = objRE.getString("RemoteException");
				JSONObject obj = new JSONObject(message);
				message = obj.getString("message");
			} catch (JSONException e1) {
				logger.error("Unable to parse the error message, So sending error message as it is - Error : " + e1.getMessage());
			}
		}
		if (!(message==null) && !(message.isEmpty()) && message.contains("Connection refused")){
			message = "Connection refused : Please check the KMS provider URL and whether the Ranger KMS is running";
		} else if (!(message==null) && !(message.isEmpty()) && (message.contains("response status of 403") || message.contains("HTTP Status 403"))){
			message = UNAUTHENTICATED_MSG;
		} else if (!(message==null) && !(message.isEmpty()) && (message.contains("response status of 401") || message.contains("HTTP Status 401 - Authentication required"))){
			message = UNAUTHENTICATED_MSG;
		} else if (message == null) {
		    message = UNAUTHENTICATED_MSG;
		}
		throw restErrorUtil.createRESTException(message, MessageEnums.ERROR_SYSTEM);
	}	
}
