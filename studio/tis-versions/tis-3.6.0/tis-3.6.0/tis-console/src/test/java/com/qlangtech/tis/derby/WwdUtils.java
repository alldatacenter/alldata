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

package com.qlangtech.tis.derby;/*
     Derby - WwdUtils.java - utilitity methods used by WwdEmbedded.java

        Licensed to the Apache Software Foundation (ASF) under one
           or more contributor license agreements.  See the NOTICE file
           distributed with this work for additional information
           regarding copyright ownership.  The ASF licenses this file
           to you under the Apache License, Version 2.0 (the
           "License"); you may not use this file except in compliance
           with the License.  You may obtain a copy of the License at

             http://www.apache.org/licenses/LICENSE-2.0

           Unless required by applicable law or agreed to in writing,
           software distributed under the License is distributed on an
           "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
           KIND, either express or implied.  See the License for the
           specific language governing permissions and limitations
           under the License.

*/

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class WwdUtils {

  /*****************
   **  Asks user to enter a wish list item or 'exit' to exit the loop - returns
   **       the string entered - loop should exit when the string 'exit' is returned
   ******************/
  static int i = 0;
  static String[] ans = new String[]{"hello", "ok", "dabao", "xiaobao", "exit"};

  public static String getWishItem() {
    return ans[i++];
//    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//    String ans = "";
//    try {
//      while (ans.length() == 0) {
//        System.out.println("Enter wish-list item (enter exit to end): ");
//        ans = br.readLine();
//        if (ans.length() == 0)
//          System.out.print("Nothing entered: ");
//      }
//    } catch (java.io.IOException e) {
//      System.out.println("Could not read response from stdin");
//    }
//    return ans;
  }  /**  END  getWishItem  ***/

  /***      Check for  WISH_LIST table    ****/
  public static boolean wwdChk4Table(Connection conTst) throws SQLException {
    boolean chk = true;
    boolean doCreate = false;
    try {
      Statement s = conTst.createStatement();
      s.execute("update WISH_LIST set ENTRY_DATE = CURRENT_TIMESTAMP, WISH_ITEM = 'TEST ENTRY' where 1=3");
    } catch (SQLException sqle) {
      String theError = (sqle).getSQLState();
      //   System.out.println("  Utils GOT:  " + theError);
      /** If table exists will get -  WARNING 02000: No row was found **/
      if (theError.equals("42X05"))   // Table does not exist
      {
        return false;
      } else if (theError.equals("42X14") || theError.equals("42821")) {
        System.out.println("WwdChk4Table: Incorrect table definition. Drop table WISH_LIST and rerun this program");
        throw sqle;
      } else {
        System.out.println("WwdChk4Table: Unhandled SQLException");
        throw sqle;
      }
    }
    //  System.out.println("Just got the warning - table exists OK ");
    return true;
  }

  /*** END wwdInitTable  **/


  public static void main(String[] args) {
    // This method allows stand-alone testing of the getWishItem method
    String answer;
    do {
      answer = getWishItem();
      if (!answer.equals("exit")) {
        System.out.println("You said: " + answer);
      }
    } while (!answer.equals("exit"));
  }

}
