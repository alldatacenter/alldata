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

 package org.apache.ranger.unixusersync.poc;

import java.io.BufferedReader;
import java.io.FileReader;


public class ListUserTest
{
 public static String strLine;

 public static void main(String args[])
  {

  try{

	  FileReader file = new FileReader("C:\\git\\xa_server\\conf\\client\\passwd");
      BufferedReader br = new BufferedReader(file);

	  while ((strLine = br.readLine()) != null)   {
		 ListRangerUser userList = ListRangerUser.parseUser(strLine);
     if (userList != null) {
		   System.out.println(userList.getName() + " " + userList.getUid() + " " + userList.getGid());
     } else {
		   System.out.println("userList is null");
     }
	  }

	  file.close();
    }catch (Exception e){//Catch exception if any
	System.err.println("Error: " + e.getMessage());
    }
  }
}
