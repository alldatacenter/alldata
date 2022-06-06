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

package org.apache.ambari.view.phonelist;

import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.PersistenceException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet for phone list view.
 */
public class PhoneListServlet extends HttpServlet {

  /**
   * The view context.
   */
  private ViewContext viewContext;

  /**
   * The view data store.
   * <code>null</code> indicates that the view properties should be used instead of the data store.
   */
  private DataStore dataStore = null;


  // ----- GenericServlet ----------------------------------------------------

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ServletContext context = config.getServletContext();
    viewContext = (ViewContext) context.getAttribute(ViewContext.CONTEXT_ATTRIBUTE);
    dataStore = Boolean.parseBoolean(viewContext.getProperties().get("data.store.enabled")) ?
        viewContext.getDataStore() : null;
  }


  // ----- HttpServlet -------------------------------------------------------

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String name = request.getParameter("name");
    String phone = request.getParameter("phone");

    try {
      if (name != null && name.length() > 0 && phone != null && phone.length() > 0) {
        if (request.getParameter("add") != null) {
          addUser(name, phone);
        } else if (request.getParameter("update") != null) {
          updateUser(name, phone);
        } else if (request.getParameter("delete") != null) {
          removeUser(name, phone);
        }
      }
      listAll(request, response);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);

    PrintWriter writer = response.getWriter();

    try {
      String name = request.getParameter("name");
      String phone = getPhone(name);

      if (phone != null) {
        editNumber(writer, name, phone, request);
      } else {
        listAll(request, response);
      }
    } catch (PersistenceException e) {
      throw new ServletException(e);
    }
  }


  // ----- helper methods ----------------------------------------------------

  // form to add new user
  private void enterNumber(PrintWriter writer, HttpServletRequest request) {
    writer.println("<form name=\"input\" action = \""+ request.getRequestURI() +"\" method=\"POST\">");
    writer.println("<table>");
    writer.println("<tr>");
    writer.println("<td>Name:</td><td><input type=\"text\" name=\"name\"></td><br/>");
    writer.println("</tr>");
    writer.println("<tr>");
    writer.println("<td>Phone Number:</td><td><input type=\"text\" name=\"phone\"></td><br/><br/>");
    writer.println("</tr>");
    writer.println("</table>");
    writer.println("<input type=\"submit\" value=\"Add\" name=\"add\">");
    writer.println("</form>");
  }

  // for to update / delete existing user
  private void editNumber(PrintWriter writer, String name, String phone, HttpServletRequest request) {
    writer.println("<form name=\"input\" action = \""+ request.getRequestURI() +"\" method=\"POST\">");
    writer.println("<table>");
    writer.println("<tr>");
    writer.println("<td>Name:</td><td><input type=\"text\" name=\"name\" value=\"" + name + "\" readonly></td><br/>");
    writer.println("</tr>");
    writer.println("<tr>");
    writer.println("<td>Phone Number:</td><td><input type=\"text\" name=\"phone\" value=\"" + phone + "\"></td><br/><br/>");
    writer.println("</tr>");
    writer.println("</table>");
    writer.println("<input type=\"submit\" value=\"Update\" name=\"update\">");
    writer.println("<input type=\"submit\" value=\"Delete\" name=\"delete\">");
    writer.println("</form>");
  }

  // list all of the users
  private void listAll(HttpServletRequest request, HttpServletResponse response) throws IOException, PersistenceException {

    PrintWriter writer = response.getWriter();

    writer.println("<h1>Phone List :" + viewContext.getInstanceName() + "</h1>");

    writer.println("<table border=\"1\" style=\"width:300px\">");
    writer.println("<tr>");
    writer.println("<td>Name</td>");
    writer.println("<td>Phone Number</td>");
    writer.println("</tr>");

    Collection<PhoneUser> phoneUsers = getAllUsers();
    for (PhoneUser phoneUser : phoneUsers) {
      String name = phoneUser.getName();
      writer.println("<tr>");
      writer.println("<td><A href=" + request.getRequestURI() + "?name=" + name + ">" + name + "</A></td>");
      writer.println("<td>" + phoneUser.getPhone() + "</td>");
      writer.println("</tr>");
    }

    writer.println("</table><br/><hr/>");

    enterNumber(writer, request);
  }

  // determine whether a user has been persisted
  private boolean userExists(String name) throws PersistenceException {
    return dataStore != null &&
        dataStore.find(PhoneUser.class, name) != null || viewContext.getInstanceData(name) != null;
  }

  // persist a new user
  private void addUser(String name, String phone) throws PersistenceException {
    if (userExists(name)) {
      throw new IllegalArgumentException("A number for " + name + " already exists.");
    }
    updateUser(name, phone);
  }

  // update an existing user
  private void updateUser(String name, String phone) throws PersistenceException {
    if (dataStore != null) {
      dataStore.store(new PhoneUser(name, phone));
    } else {
      viewContext.putInstanceData(name, phone);
    }
  }

  // remove an existing user
  private void removeUser(String name, String phone) throws PersistenceException {
    if (dataStore != null) {
      dataStore.remove(new PhoneUser(name, phone));
    } else {
      viewContext.removeInstanceData(name);
    }
  }

  // get the phone for the given name
  private String getPhone(String name) throws PersistenceException {
    if (name != null && name.length() > 0) {
      if (dataStore != null) {
        PhoneUser phoneUser = dataStore.find(PhoneUser.class, name);
        if (phoneUser != null) {
          return phoneUser.getPhone();
        }
      } else {
        return viewContext.getInstanceData(name);
      }
    }
    return null;
  }

  // get all of the phone users
  private Collection<PhoneUser> getAllUsers() throws PersistenceException {
    if (dataStore != null) {
      return dataStore.findAll(PhoneUser.class, null);
    }
    Map<String, String> data = new LinkedHashMap<String, String>(viewContext.getInstanceData());
    Collection<PhoneUser> users = new HashSet<PhoneUser>();

    for (Map.Entry<String,String> entry : data.entrySet()) {
      users.add(new PhoneUser(entry.getKey(), entry.getValue()));
      entry.getKey();
    }
    return users;
  }
}

