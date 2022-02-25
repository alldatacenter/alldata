---
name: Eclipse Setup
route: /EclipseSetup
menu: For Developers
submenu: Eclipse Setup
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';

# Tools required to build and run Apache Atlas on Eclipse

These instructions are provided as-is. They worked at a point in time; other variants of software may work. These instructions may become stale if the build dependencies change.

They have been shown to work on 19th of December 2016.

To build, run tests, and debug Apache Atlas, the following software is required:

**Java**
   * Download and install a 1.8 Java SDK
   * Set JAVA_HOME system environment variable to the installed JDK home directory
   * Add JAVA_HOME/bin directory to system PATH

**Python**
Atlas command line tools are written in Python.
   * Download and install Python version 2.7.7
   * For Mac, we used 2.7.11
   * Add Python home directory to system PATH

**Maven**
   * Download and install Maven 3.3.9
   * Set the environment variable M2_HOME to point to the maven install directory
   * Add M2_HOME/bin directory to system PATH e.g. C:\Users\IBM_ADMIN\Documents\Software\apache-maven-3.3.9\bin

**Git**
   * Install Git
   * Add git bin directory to the system PATH e.g. C:\Program Files (x86)\Git\bin

**Eclipse**
* Install Eclipse Neon (4.6)
* The non-EE Neon for iOS from eclipse.org has been proven to work here.
* Install the Scala IDE, TestNG, and m2eclipse-scala features/plugins as described below.

**Scala IDE Eclipse feature**
Some of the Atlas source code is written in the Scala programming language. The Scala IDE feature is required to compile Scala source code in Eclipse.
   * In Eclipse, choose Help - Install New Software..
   * Click Add... to add an update site, and set Location to http://download.scala-ide.org/sdk/lithium/e44/scala211/stable/site
   * Select Scala IDE for Eclipse from the list of available features
   * Restart Eclipse after install
   * Set the Scala compiler to target the 1.7 JVM: Window - Preferences - Scala - Compiler, change target to 1.7
*TestNG Eclipse plug-in*

Atlas tests use the [TestNG framework](http://testng.org/doc/documentation-main.html), which is similar to JUnit. The TestNG plug-in is required to run TestNG tests from Eclipse.
   * In Eclipse, choose Help - Install New Software..
   * Click Add... to add an update site, and set Location to http://beust.com/eclipse-old/eclipse_6.9.9.201510270734
      * Choose TestNG and continue with install
      * Restart Eclipse after installing the plugin
      * In Window - Preferences - TestNG, <b>un</b>check "Use project TestNG jar"
*m2eclipse-scala Eclipse plugin*
   * In Eclipse, choose Help - Install New Software..
   * Click Add... to add an update site, and set Location to http://alchim31.free.fr/m2e-scala/update-site/
   * Choose Maven Integration for Scala IDE, and continue with install
   * Restart Eclipse after install
   * In Window - Preferences -Maven - Errors/Warnings, set Plugin execution not covered by lifecycle configuration to Warning
*Import Atlas maven projects into Eclipse:*

a. File - Import - Maven - Existing Maven Projects b. Browse to your Atlas folder c. Uncheck the root project and non-Java projects such as dashboardv2, docs and distro, then click Finish

On the Mac, the Maven import fails with message

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`"Cannot complete the install because one or more required items could not be found.
Software being installed: Maven Integration for AJDT (Optional) 0.14.0.201506231302 (org.maven.ide.eclipse.ajdt.feature.feature.group 0.14.0.201506231302)
Missing requirement: Maven Integration for AJDT (Optional) 0.14.0.201506231302 (org.maven.ide.eclipse.ajdt.feature.feature.group 0.14.0.201506231302) requires 'org.eclipse.ajdt.core 1.5.0' but it could not be found".`}
</SyntaxHighlighter>

Install http://download.eclipse.org/tools/ajdt/46/dev/update and rerun. The Maven AspectJ should plugin install - allowing the references to Aspects in Maven to be resolved.

 d. In the atlas-typesystem, atlas-repository, hdfs-model, and storm-bridge projects, add the src/main/scala and src/test/scala (if available) directories as source folders. Note: the hdfs-model and storm-bridge projects do not have the src/test/scala folder.

Right-click on the project, and choose *Properties*.

Click the *Java Build Path* in the left-hand panel, and choose the *Source* tab.

Click *Add Folder*, and select the src/main/scala and src/test/scala directories.

Only the atlas-repository and atlas-type system projects have Scala source folders to update.

e. Select atlas-typesystem, atlas-repository, hdfs-model, and storm-bridge projects, right-click, go to the Scala menu, and choose ‘Set the Scala Installation’.

f. Choose Fixed Scala Installation: 2.11.8 (bundled) , and click OK.

g. Restart Eclipse

h. Choose Project - Clean, select Clean all projects, and click OK.

Some projects may not pick up the Scala library – if this occurs, quick fix on those projects to add in the Scala library – projects atlas-typesystem, atlas-repository, hdfs-model, storm-bridge and altas-webapp.

You should now have a clean workspace.

*Sample Bash scripts to help mac users*

You will need to change some of these scripts to point to your installation targets.
   * Run this script to setup your command line build environment

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`#!/bin/bash # export JAVA_HOME=/Library/Java/JavaVirtualMachines/macosxx6480sr3fp10hybrid-20160719_01-sdk
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_101.jdk/Contents/Home
export M2_HOME=/Applications/apache-maven-3.3.9 # Git is installed in the system path
export PYTHON_HOME='/Applications/Python 2.7'
export PATH=$PYTHON_HOME:$M2_HOME/bin:$JAVA_HOME/bin:$PATH
export MAVEN_OPTS="-Xmx1536m -Drat.numUnapprovedLicenses=100"`}
</SyntaxHighlighter>

   * If you do  not want to set Java 8 as your system java, you can use this  bash script to setup the environment and run Eclipse (which you can drop in Applications and rename to neon).

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`#!/bin/bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_101.jdk/Contents/Home
export M2_HOME=/Applications/apache-maven-3.3.9
# Git is installed in the system path
export PYTHON_HOME='/Applications/Python 2.7'
export PATH=$PYTHON_HOME:$M2_HOME/bin:$JAVA_HOME/bin:$PATH/Applications/neon.app/Contents/MacOS/eclipse`}
</SyntaxHighlighter>
