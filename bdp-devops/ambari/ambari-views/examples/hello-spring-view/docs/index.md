<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Hello Spring View Example
========
Description
-----
The Hello Spring view is a very simple view example that uses Spring MVC.  Like the HelloWorld view example, it demonstrates the basics of how to write and deploy a view in Ambari but also shows a simple web application using the Spring MVC framework.  The Hello Spring view displays a Hello message that is customized based on the currently logged in user.

Package
-----

All views are packaged as a view archive.  The view archive contains the configuration file and various optional components of the view.

#####view.xml

The view.xml file is the only required file for a view archive.  The view.xml is the configuration that describes the view and view instances for Ambari.

      <view>
        <name>HELLO_SPRING</name>
        <label>The Hello Spring View</label>
        <version>1.0.0</version>
        <instance>
          <name>INSTANCE</name>
        </instance>
      </view>

The configuration in this case defines a view named HELLO_SPRING that has a single instance.


#####HelloController.java

The HelloController class is the controller for the Spring MVC app.

Notice that we can access the view context through the servlet context.

    // get the view context from the servlet context
    ViewContext viewContext = (ViewContext) request.getSession().getServletContext().getAttribute(ViewContext.CONTEXT_ATTRIBUTE);

    // get the current user name from the view context
    String userName = viewContext.getUsername();


For this app, the controller saves a customized greeting to a model attribute.

    // add the greeting message attribute
    model.addAttribute("greeting", "Hello " + (userName == null ? "unknown user" : userName) + "!");



#####WEB-INF/web.xml
The web.xml is the deployment descriptor used to deploy the view as a web app.  The Java EE standards apply for the descriptor.  We can see that for this example a single servlet is mapped to the root context path.  The class for the servlet is the Spring DispatcherServlet.

        <servlet>
          <servlet-name>Hello</servlet-name>
          <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
          <load-on-startup>0</load-on-startup>
        </servlet>
        <servlet-mapping>
          <servlet-name>Hello</servlet-name>
          <url-pattern>/</url-pattern>
        </servlet-mapping>
      </web-app>

#####WEB-INF/hello-spring.xml

This hello-spring.xml file contains the bean configuration.

      <context:component-scan base-package="org.apache.ambari.view.hello" />

      <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/jsp/" />
        <property name="suffix" value=".jsp" />
      </bean>

In the above servlet.xml file, we have defined a tag <context:component-scan> . This will direct Spring to load all the components from the org.apache.ambari.view.hello package.

Note that in the HelloController class we return the bean name "hello" which will resolve to the view /WEB-INF/jsp/hello.jsp.


#####WEB-INF/jsp/hello.jsp

The Spring MVC app view for this example will display the customized greeting message saved in the HelloController class.

      <%@ page contentType="text/html; charset=UTF-8" %>
      <html>
        <head>
          <title>Hello</title>
        </head>
        <body>
          <h2>${greeting}</h2>
        </body>
      </html>

Build
-----

The view can be built as a maven project.

    cd ambari-views/examples/hello-spring-view
    mvn clean package

The build will produce the view archive.

    ambari-views/examples/hello-spring-view/target/hello-spring-view-x.x.x.war


Deploy
-----
To deploy a view we simply place the view archive in the views folder of the ambari-server machine.  By default the views folder is located at ...

    /var/lib/ambari-server/resources/views

To deploy the Hello Spring view simply copy the hello-spring-view jar to the ambari-server views folder and restart the ambari server.

Use
-----

After deploying a view you should see it as a view resource in the Ambari REST API.  If we request all views, we should see the HELLO_SPRING view.

      http://<server>:8080/api/v1/views

      {
        "href" : "http://<server>:8080/api/v1/views",
        "items" : [
          {
            "href" : "http://c6401.ambari.apache.org:8080/api/v1/views/HELLO_SPRING",
            "ViewInfo" : {
              "view_name" : "HELLO_SPRING"
            }
          },...
        ]
      }


We can access access the view at ...
      
    http://c6401.ambari.apache.org:8080/views/HELLO_SPRING/1.0.0/INSTANCE/

This should display a greeting customized to for the logged in user.


