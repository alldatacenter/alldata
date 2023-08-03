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
package org.apache.util.sql;


import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MySQLPLRunner {

    private static final String DEFAULT_DELIMITER = ";";
    private Connection connection;
    private boolean stopOnError;
    private boolean autoCommit;
    private PrintWriter logWriter = new PrintWriter(System.out);
    private PrintWriter errorLogWriter = new PrintWriter(System.err);
    private String delimiter = DEFAULT_DELIMITER;
    private boolean fullLineDelimiter = false;
    private static final String DELIMITER_LINE_REGEX = "(?i)delimiter.+";
    private static final String DELIMITER_LINE_SPLIT_REGEX = "(?i)delimiter";
    private boolean printDebug = false;
    /**
     * Default constructor
     */
    public MySQLPLRunner(Connection connection, boolean autoCommit,
            boolean stopOnError,boolean printDebug) {
        this.connection = connection;
        this.autoCommit = autoCommit;
        this.stopOnError = stopOnError;
        this.printDebug=printDebug;
    }

    public void setDelimiter(String delimiter, boolean fullLineDelimiter) {
        this.delimiter = delimiter;
        this.fullLineDelimiter = fullLineDelimiter;
    }

    /**
     * Setter for logWriter property
     *
     * @param logWriter
     *            - the new value of the logWriter property
     */
    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    /**
     * Setter for errorLogWriter property
     *
     * @param errorLogWriter
     *            - the new value of the errorLogWriter property
     */
    public void setErrorLogWriter(PrintWriter errorLogWriter) {
        this.errorLogWriter = errorLogWriter;
    }

    /**
     * Runs an SQL script (read in using the Reader parameter)
     *
     * @param reader
     *            - the source of the script
     */
    public void runScript(Reader reader) throws IOException, SQLException {
        try {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                if (originalAutoCommit != this.autoCommit) {
                    connection.setAutoCommit(this.autoCommit);
                }
                runScript(connection, reader);
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (IOException e) {
            throw e;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error running script.  Cause: " + e, e);
        }
    }

    /**
     * Runs an SQL script (read in using the Reader parameter) using the
     * connection passed in
     *
     * @param conn
     *            - the connection to use for the script
     * @param reader
     *            - the source of the script
     * @throws SQLException
     *             if any SQL errors occur
     * @throws IOException
     *             if there is an error reading from the Reader
     */
    private void runScript(Connection conn, Reader reader) throws IOException,
            SQLException {
        StringBuilder command = null;
        try {
            LineNumberReader lineReader = new LineNumberReader(reader);
            String line = null;
            while ((line = lineReader.readLine()) != null) {
                if (command == null) {
                    command = new StringBuilder();
                }
                String trimmedLine = line.trim();

                if (trimmedLine.length() < 1 || trimmedLine.startsWith("--")
                    || trimmedLine.startsWith("//")) { //NOPMD
                    //println(trimmedLine);
                    // Do nothing
                } else if (!fullLineDelimiter
                        && trimmedLine.endsWith(getDelimiter())
                        || fullLineDelimiter
                        && trimmedLine.equals(getDelimiter())) {


                    Pattern pattern = Pattern.compile(DELIMITER_LINE_REGEX);
                    Matcher matcher = pattern.matcher(trimmedLine);
                    if (matcher.matches()) {
                        setDelimiter(trimmedLine.split(DELIMITER_LINE_SPLIT_REGEX)[1].trim(), fullLineDelimiter);
                        continue;
                        /*line = lineReader.readLine();
                        if (line == null) {
                            break;
                        }
                        trimmedLine = line.trim();*/
                    }

                    if(line!=null && line.endsWith(getDelimiter()) && !DEFAULT_DELIMITER.equalsIgnoreCase(getDelimiter())){
                    	 command.append(line.substring(0, line.lastIndexOf(getDelimiter())));
                    }else{
                    	command.append(line);
                    }

                    command.append(" ");
                    Statement statement = conn.createStatement();
                    if(printDebug)
                    	println(command);
                    //System.out.println(getDelimiter());
                    boolean hasResults = false;
                    if (stopOnError) {
                        hasResults = statement.execute(command.toString());
                    } else {
                        try {
                            statement.execute(command.toString());
                        } catch (SQLException e) {
                            e.fillInStackTrace();
                            printlnError("Error executing: " + command);
                            printlnError(e);
                        }
                    }

                    if (autoCommit && !conn.getAutoCommit()) {
                        conn.commit();
                    }

                    ResultSet rs = statement.getResultSet();
                    if (hasResults && rs != null) {
                        ResultSetMetaData md = rs.getMetaData();
                        int cols = md.getColumnCount();
                        for (int i = 1; i <= cols; i++) {
                            String name = md.getColumnLabel(i);
                            print(name + "\t");
                        }
                        println("");
                        while (rs.next()) {
                            for (int i = 1; i <= cols; i++) {
                                String value = rs.getString(i);
                                print(value + "\t");
                            }
                            println("");
                        }
                    }

                    command = null;
                    try {
                    	if(rs!=null){
                        rs.close();
                    	}
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        statement.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Ignore to workaround a bug in Jakarta DBCP
                    }
                    Thread.yield();
                } else {
                    Pattern pattern = Pattern.compile(DELIMITER_LINE_REGEX);
                    Matcher matcher = pattern.matcher(trimmedLine);
                    if (matcher.matches()) {
                        setDelimiter(trimmedLine.split(DELIMITER_LINE_SPLIT_REGEX)[1].trim(), fullLineDelimiter);
                        continue;
                        /*line = lineReader.readLine();
                        if (line == null) {
                            break;
                        }
                        trimmedLine = line.trim();*/
                    }
                    command.append(line);
                    command.append(" ");
                }
            }
            if (!autoCommit) {
                conn.commit();
            }
        } catch (SQLException e) {
            e.fillInStackTrace();
            printlnError("Error executing: " + command);
            printlnError(e);
            throw e;
        } catch (IOException e) {
            e.fillInStackTrace();
            printlnError("Error executing: " + command);
            printlnError(e);
            throw e;
        } finally {
            conn.rollback();
            flush();
        }
    }

    private String getDelimiter() {
        return delimiter;
    }

    private void print(Object o) {
        if (logWriter != null) {
            System.out.print(o);
        }
    }

    private void println(Object o) {
        if (logWriter != null) {
            logWriter.println(o);
        }
    }

    private void printlnError(Object o) {
        if (errorLogWriter != null) {
            errorLogWriter.println(o);
        }
    }

    private void flush() {
        if (logWriter != null) {
            logWriter.flush();
        }
        if (errorLogWriter != null) {
            errorLogWriter.flush();
        }
    }

    public static void main(String args[]){
    	// Creating object of ScriptRunner class
    	  Connection con = null;
    	  String driverName = "com.mysql.jdbc.Driver";
    	  Properties props = null;
    	  try {
              Class.forName(driverName).newInstance();
              props = new Properties();

              props.put("user", "root");

              props.put("password", "root");
              String connectString = "jdbc:mysql://localhost:3306/ranger";
              con = DriverManager.getConnection(connectString, props);


    	MySQLPLRunner scriptRunner = new MySQLPLRunner(con, false, true,true);
     	String aSQLScriptFilePath = "/disk1/zero/jisql-2.0.11/xa_core_db.sql";
    	
    	
    	// Executing SQL Script
     	FileReader reader = new FileReader(aSQLScriptFilePath);
     	
     	try {
     		scriptRunner.runScript(reader);
     	}
     	finally {
     		if (reader != null) {
     			try {
     			reader.close();
     			}
     			catch(IOException ioe) {
     				// Ignore IOException when reader is getting closed
     			}
     		}
     	}

    	
    	  }
          catch (SQLException sqle) {
        	  sqle.printStackTrace();
          }
          catch (ClassNotFoundException cnfe) {
              System.err.println("Cannot find the driver class \"" + driverName + "\" in the current classpath.");
          }
          catch (InstantiationException ie) {
              System.err.println("Cannot instantiate the driver class \"" + driverName + "\"");
              ie.printStackTrace(System.err);
          }
          catch (IllegalAccessException iae) {
              System.err.println("Cannot instantiate the driver class \"" + driverName + "\" because of an IllegalAccessException");
              iae.printStackTrace(System.err);
          }catch (Exception sqle) {
        	  sqle.printStackTrace();
          }
          finally {
              if (con != null) {
                  try {
                	  con.close();
                  }
                  catch (SQLException ignore) {
                      /* ignored */
                  }
              }
          }
    }
}
