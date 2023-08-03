 /* Copyright (C) 2004-2011 Scott Dunbar (scott@xigole.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.util.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.util.outputformatter.JisqlFormatter;

/**
 * A simple utility to provide an interactive session with a SQL server. This
 * application is conceptually modeled on the Sybase 'isql' program with,
 * obviously, strong similarities to Microsoft SQL/Server isql and osql (as
 * Microsoft got SQL Server from Sybase).
 * <p>
 *
 * The program can act in a similar way to Oracle's sqlplus and PostgreSQL's
 * psql.
 * <p>
 *
 * A simple command line might look like (this should be all on one line) is: <br>
 * <code>
 *  java -classpath lib/jisql.jar:&lt;file containing native driver&gt;
 *       org.apache.util.sql.Jisql  -user scott -password blah -driver postgresql
 *       -cstring jdbc:postgresql://localhost:5432/scott -c \;
 * </code>
 * <p>
 *
 * This logs into a PostgreSQL database as the user "scott", password "blah". It
 * connects to the database named "scott". It uses the command terminator of
 * ";", just like psql or sqlplus (which is escaped in the example so that it
 * will not be interpreted by the Unix shell). If you do not use this the
 * default is the term "go" on a single line like Sybase's isql or MS/SQL's
 * isql/osql. Note that there is a dependency on <a
 * href="http://jopt-simple.sourceforge.net/">JOpt Simple</a> in for the base
 * configuration.
 *
 *
 * Options:
 * <ul>
 * <li><b>-driver </b> This option allows you to specify the JDBC driver class
 * name of the driver. There are several shortcuts that can be used:
 * <ul>
 * <li><b>jconnect4 </b>- short for <code>com.sybase.jdbc.SybDriver</code></li>
 * <li><b>jconnect5 </b>- short for <code>com.sybase.jdbc2.jdbc.SybDriver</code>
 * <li><b>jconnect6 </b>- short for <code>com.sybase.jdbc3.jdbc.SybDriver</code>
 * </li>
 * <li><b>oraclethin </b>- short for
 * <code>oracle.jdbc.driver.OracleDriver</code></li>
 * <li><b>db2app </b>- the DB2 &quot;App&quot; driver -
 * <code>COM.ibm.db2.jdbc.app.DB2Driver</code></li>
 * <li><b>db2net </b>- the DB2 &quot;Net&quot; driver -
 * <code>COM.ibm.db2.jdbc.net.DB2Driver</code></li>
 * <li><b>mssql </b>- short for
 * <code>com.microsoft.jdbc.sqlserver.SQLServerDriver</code></li>
 * <li><b>cloudscape </b>- short for <code>COM.cloudscape.core.JDBCDriver</code>
 * </li>
 * <li><b>pointbase </b>- short for
 * <code>com.pointbase.jdbc.jdbcUniversalDriver</code></li>
 * <li><b>postgresql </b>- short for <code>org.postgresql.Driver</code></li>
 * <li><b>mysqlconj </b>- short for <code>com.mysql.jdbc.Driver</code>- the
 * Connector/J driver for MySQL</li>
 * <li><b>mysqlcaucho </b>- short for <code>com.caucho.jdbc.mysql.Driver</code>-
 * the Caucho driver for MySQL</li>
 * </ul>
 *
 * Alternatively, any class name can be specified here. The shortcuts only exist
 * for those of us who generate more typos than real text :)</li>
 *
 * <li><b>-cstring </b> This option allows you to specify the connection string
 * to the database. This string is driver specific but almost always starts with
 * &quot;jdbc:&quot;. Connection strings for the drivers I have tested look
 * like:
 * <ul>
 * <li><b>jconnect4, jconnect5, jconnect6 </b>- Sybase connection strings take
 * the form &quot;jdbc:sybase:Tds:[hostname]:[port]/[db_name]&quot;</li>
 * <li><b>oraclethin </b>- The Oracle &quot;thin&quot; driver connection string
 * looks like &quot;jdbc:oracle:thin:@[hostname]:[port]:[oracle sid]&quot;</li>
 * <li><b>db2app </b>- The DB2 &quot;App&quot; driver connection string looks
 * like &quot;jdbc:db2:[db_name]&quot;</li>
 * <li><b>db2net </b>- The DB2 &quot;Net&quot; driver connection string looks
 * like &quot;jdbc:db2://[hostname]:[port]/[db_name]&quot;</li>
 * <li><b>mssql </b>- The MS/SQL driver connection string looks like
 * &quot;jdbc:microsoft:sqlserver://[hostname]:[port]/[db_name]&quot;</li>
 * <li><b>cloudscape </b>- The Cloudscape driver connection string looks like
 * &quot;jdbc:cloudscape:[db_name];create=true;autocommit=false&quot;</li>
 * <li><b>pointbase </b>- The Pointbase driver connection string looks like
 * &quot;jdbc:pointbase:server://[hostname]:[port]/[db_name]&quot;</li>
 * <li><b>postgresql </b>- The PostgreSQL driver connection string looks like
 * &quot;jdbc:postgresql://[hostname]:[port]/[db_name]&quot;</li>
 * <li><b>mysqlconj </b>- The MySQL Connector/J driver connection string looks
 * like &quot;jdbc:mysql://[hostname]:[port]/[db_name]&quot;</li>
 * <li><b>mysqlcaucho </b>- The MySQL Cahcho driver connection string looks like
 * &quot;jdbc:mysql-caucho://[hostname]:[port]/[db_name]&quot;</li>
 * </ul>
 *
 * <b>Important </b>- each JDBC vendor has other flags and parameters that can
 * be passed on the connection string. You should look at the documentation for
 * your JDBC driver for more information. The strings listed are just a sample
 * and may change with a new release of the driver. None of these strings are
 * coded within the application - the list is provided for reference only.</li>
 *
 * <li><b>-user or -u </b> The user name to use to log into the database with.</li>
 * <li><b>-password or -p </b> The password to use to log into the database
 * with. If this option is missing then the program asks for the password.</li>
 * <li><b>-c </b> The &quot;command terminator&quot; to use. By default this
 * application uses the string &quot;go&quot; (case insensitive) on a line by
 * itself to determine when to send the string buffer to the database. You may
 * specify something else with the -c option. For example, users of Oracle may
 * prefer either the &quot;;&quot; (semi-colon) character or the &quot;/&quot;
 * (forwardslash) character as that is what sqlplus uses. This string may occur
 * as a standalone line or at the end of a particular line.</li>
 * <li><b>-pf</b> Optional file to specify the password. This prevents having to
 * have it visible when looking at a process status. The first line of the file
 * is read and used as the password. If both the command line password and this
 * option are specified the command line password is used.</li>
 * <li><b>-input </b> The name of a file to read commands from instead of
 * System.in.</li>
 * <li><b>-query </b> An optional single query to run instead of interacting
 * with the command line or a file. <b>Note</b> - the command <i>must</i> have a
 * command terminator. So, for example, your command line may be something like
 * &quot;-c \; -query "select * from blah;". If you do not include the command
 * terminator then the command will hang, waiting for you to enter the default
 * "go".</li>
 * <li><b>-debug </b> This turns on some internal debugging code. Not generally
 * useful.</li>
 * <li><b>-driverinfo </b> Allows you to print some information that the driver
 * returns. Generally not very useful in all but a few cases.</li>
 * <li><b>-formatter</b> Optionally specify a class name or short cut to format
 * the output. There are three built in short cuts:
 * <ul>
 * <li><b>csv</b> output the data in CSV format.</li>
 * <li><b>xml</b> output the data in XML format.</li>
 * <li><b>default</b> (does not have to be specified) - output the format in the
 * &quot;normal&quot; format.</li>
 * </ul>
 * Otherwise, this is a class name that implements
 * org.apache.util.outputformatter.JisqlFormatter. See the code for more
 * information on implementing your own output formatter.</li>
 * </ul>
 * <p>
 * &nbsp;
 * </p>
 * The included default formatter supports the following command line options:
 * <ul>
 * <li><b>-noheader</b> do not print the header column info.</li>
 * <li><b>-spacer</b> The character to use for &quot;empty&quot; space. This
 * defaults to the space character. From mrider - &quot;I added the ability to
 * specify the spacer for columns - which used to be the single char ' '. I did
 * this because of brain-dead Windows' command line copy/paste. It seems that
 * when a line of text ends in space, copy does not copy that space. Which makes
 * it difficult to copy/paste into another program. This can probably be ignored
 * most of the time.&quot;</li>
 * <li><b>-w</b>Specifies the maximum field width for a column. By default jisql
 * defaults columns to a maximum width of 2048. By specifying a value
 * for this jisql with truncate the output of columns that are wider than this
 * parameter.</li>
 * <li><b>-delimiter</b> Specify a single character delimiter for columns.</li>
 * <li><b>-trim</b> trim the spaces from columns.</li>
 * <li><b>-nonull</b> print an empty string instead of the word &quot;NULL&quot;
 * when there is a null value.
 * <li>
 * <li><b>-left</b> left justify the output</li>
 * <li><b>-debug</b> print debugging information about the result set.</li>
 * </ul>
 * <p>
 * &nbsp;
 * </p>
 * The include CSV formatter supports the following command line options:
 * <ul>
 * <li><b>-delimiter</b> specifies the delimiter to use. By default a comma is
 * used</li>
 * <li><b>-colnames</b> if included then column names are printed as the first
 * line of output. By default they are not included</li>
 * </ul>
 * <p>
 * &nbsp;
 * </p>
 * The included XML formatter does not have any additional output options.
 * <p>
 * &nbsp;
 * </p>
 */
public class Jisql {
	//Sybase SQL Anywhere JDBC4-Type2 (Native) Driver
	private static final String sapJDBC4SqlAnywhereDriverName= "sap.jdbc4.sqlanywhere.IDriver";
	private static final String sybaseJDBC4SqlAnywhereDriverName= "sybase.jdbc4.sqlanywhere.IDriver";
    private static final String sybaseJConnect6DriverName = "com.sybase.jdbc3.jdbc.SybDriver";
    private static final String sybaseJConnect5DriverName = "com.sybase.jdbc2.jdbc.SybDriver";
    private static final String sybaseJConnect4DriverName = "com.sybase.jdbc.SybDriver";
    private static final String oracleThinDriverName = "oracle.jdbc.driver.OracleDriver";
    private static final String db2AppDriverName = "COM.ibm.db2.jdbc.app.DB2Driver";
    private static final String db2NetDriverName = "COM.ibm.db2.jdbc.net.DB2Driver";
    private static final String cloudscapeDriverName = "COM.cloudscape.core.JDBCDriver";
    private static final String msqlDriverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String pointbaseDriverName = "com.pointbase.jdbc.jdbcUniversalDriver";
    private static final String postgresqlDriverName = "org.postgresql.Driver";
    private static final String mySQLConnectJDriverName = "com.mysql.jdbc.Driver";
    private static final String mySQLCauchoDriverName = "com.caucho.jdbc.mysql.Driver";

    private static final String defaultFormatterClassName = "org.apache.util.outputformatter.DefaultFormatter";
    private static final String csvFormatterClassName = "org.apache.util.outputformatter.CSVFormatter";
    private static final String xmlFormatterClassName = "org.apache.util.outputformatter.XMLFormatter";

    private String driverName = null;
    private String connectString = null;
    private String userName = null;
    private String password = null;
    private String passwordFileName = null;
    private String formatterClassName = defaultFormatterClassName;

    private JisqlFormatter formatter = null;

    private Connection connection = null;
    private boolean printDebug = false;
    private boolean printDriverDetails = false;
    private Driver driver = null;
    private Properties props = null;
    private String inputFileName = null;
    private String commandTerminator = "go";
    private String inputQuery = null;

    /**
     * Runs Jisql with the command line arguments provided.
     *
     */
    public static void main(String argv[]) {
        Jisql jisql = new Jisql();

        try {
            jisql.parseArgs(argv);
        }
        catch (Throwable t) {
            t.printStackTrace();
            jisql.usage();
            System.exit(1);
        }

        try {
            jisql.run();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    public void run() throws Exception {
    	boolean isExit=false;
        try {
            driver = (Driver) Class.forName(driverName).newInstance();
            props = new Properties();

            props.put("user", userName);
            if (password != null)
                props.put("password", password);

            connection = DriverManager.getConnection(connectString, props);
            if (printDriverDetails) {
                printDriverInfo();
            }
            else {
            	if(connectString.toLowerCase().startsWith("jdbc:mysql") && inputFileName!=null){
            		MySQLPLRunner scriptRunner = new MySQLPLRunner(connection, false, true,printDebug);
            		scriptRunner.setDelimiter(commandTerminator,false);
            		FileReader reader = new FileReader(inputFileName);
            		try {
                	scriptRunner.runScript(reader);
            		}
            		finally {
            			if (reader != null) {
            				try {
								reader.close();
							} catch (IOException ioe) {
								// Ignore error during closing of the reader stream
							}
            			}
            		}
            	}else{
            		doIsql();
            	}
            }
        }
        catch (SQLException sqle) {
        	printAllExceptions(sqle);
        	isExit=true;
        }
        catch (IOException ie) {
        	isExit=true;
        }
        catch (ClassNotFoundException cnfe) {
        	isExit=true;
            System.err.println("Cannot find the driver class \"" + driverName + "\" in the current classpath.");
        }
        catch (InstantiationException ie) {
        	isExit=true;
            System.err.println("Cannot instantiate the driver class \"" + driverName + "\"");
            ie.printStackTrace(System.err);
        }
        catch (IllegalAccessException iae) {
        	isExit=true;
            System.err.println("Cannot instantiate the driver class \"" + driverName + "\" because of an IllegalAccessException");
            iae.printStackTrace(System.err);
        }
        finally {
            if (connection != null) {
                try {
                    connection.close();
                }
                catch (SQLException ignore) {
                    /* ignored */
                }
                if(isExit){
                	System.exit(1);
                }
            }
        }
    }

    /**
     * The main loop for the Jisql program. This method handles the input from
     * either a command line or from a file. Output is handled through the
     * Formatter.
     *
     * @throws SQLException
     *             if an exception occurs.
     *
     */
    public void doIsql() throws IOException, SQLException {
        BufferedReader reader = null;
        Statement statement = null;
        ResultSet resultSet = null;
        ResultSetMetaData resultSetMetaData = null;
        StringBuilder query = null;

        if (inputFileName != null) {
            try {
                reader = new BufferedReader(new FileReader(inputFileName));
            }
            catch (FileNotFoundException fnfe) {
            	System.err.println("Unable to open file \"" + inputFileName + "\"");
                fnfe.printStackTrace(System.err);
                throw fnfe;
            }
        }
        else {
            reader = new BufferedReader(new InputStreamReader(System.in));
        }
        if(printDebug)
        	printAllExceptions(connection.getWarnings());
        statement = connection.createStatement();
        connection.clearWarnings();
        String trimmedLine=null;

        try {

        while (true) {
            int linecount = 1;
            query = new StringBuilder();

            try {
                if ((inputFileName == null) && (inputQuery == null))
                    System.out.print("\nEnter a query:\n");

                while (true) {
                    if ((inputFileName == null) && (inputQuery == null)) {
                        System.out.print(linecount++ + " > ");
                        System.out.flush();
                    }

                    String line = null;
                    if (inputQuery == null)
                        line = reader.readLine();
                    else
                        line = inputQuery.toString();

                    if (line == null || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")){
                    	if ((inputFileName != null) && (inputQuery != null)) {
                    		break;
                    	}else{
                    		 return;
                    	}
                    }

                    if (line.equals("reset")) {
                        query = new StringBuilder();
                        break;
                    }
                    trimmedLine=line.trim();
                    if (trimmedLine.startsWith("--") ||trimmedLine.length()<1) {
                        continue;
                    }
                    if(connectString.toLowerCase().startsWith("jdbc:oracle") && inputFileName!=null){
	                    if (trimmedLine.startsWith("/") ||trimmedLine.length()<2) {
	                        commandTerminator=";";
	                        continue;
	                    }
	                    if (trimmedLine.toUpperCase().startsWith("DECLARE")) {
	                        commandTerminator="/";
	                    }
	                    if ((trimmedLine.toUpperCase().startsWith("CREATE OR REPLACE PROCEDURE")) || (trimmedLine.toUpperCase().startsWith("CREATE OR REPLACE FUNCTION"))) {
	                        commandTerminator="/";
	                    }
                    }
                    if(connectString.toLowerCase().startsWith("jdbc:postgresql") && inputFileName!=null){
	                    if (trimmedLine.toLowerCase().startsWith("select 'delimiter start';")) {
	                        commandTerminator="select 'delimiter end';";
	                        continue;
	                    }
                    }

                    if (line.trim().equalsIgnoreCase(commandTerminator) || line.trim().endsWith(commandTerminator)) {
                        if (line.trim().endsWith(commandTerminator)) {
                            line = line.substring(0, line.length() - commandTerminator.length());
                            query.append("\n");
                            query.append(line);
                        }
                        break;
                    }

                    query.append("\n");
                    query.append(line);
                }

                if (query.toString().length() == 0)
                    continue;

                if (printDebug)
                    System.out.println("executing: " + query.toString());

                boolean moreResults = statement.execute(query.toString());
                int rowsAffected = 0;
                do {
                	if(printDebug)
                		printAllExceptions(statement.getWarnings());
                    statement.clearWarnings();
                    if (moreResults) {
                        resultSet = statement.getResultSet();
                        if(printDebug)
                        	printAllExceptions(resultSet.getWarnings());
                        resultSet.clearWarnings();
                        resultSetMetaData = resultSet.getMetaData();

                        formatter.formatHeader(System.out, resultSetMetaData);
                        formatter.formatData(System.out, resultSet, resultSetMetaData);
                        formatter.formatFooter(System.out, resultSetMetaData);

                        int rowsSelected = statement.getUpdateCount();

                        if (rowsSelected >= 0 && printDebug) {
                            System.out.println(rowsSelected + " rows affected.");
                        }
                    }
                    else {
                        rowsAffected = statement.getUpdateCount();
                        if (printDebug)
                        	printAllExceptions(statement.getWarnings());
                        statement.clearWarnings();
                        if (rowsAffected >= 0 && printDebug) {
                            System.out.println(rowsAffected + " rows affected.");
                        }
                    }

                    //
                    // I was having problems with the PostgreSQL driver throwing
                    // a NullPointerException here so I just catch it and tell
                    // the loop that it is done if it happens.
                    //
                    try {
                        moreResults = statement.getMoreResults();
                    }
                    catch (NullPointerException npe) {
                        moreResults = false;
                    }
                }

                while (moreResults || rowsAffected != -1);
            }
            catch (SQLException sqle) {
                printAllExceptions(sqle);
                statement.cancel();
                statement.clearWarnings();
                throw sqle;
            }
            catch (Exception e) {
                e.printStackTrace(System.err);
            }

            if (inputQuery != null)
                return;
        }
        }
        finally {
        	if (reader != null) {
        		try {
        			reader.close();
        		}
        		catch(IOException ioe) {
        			// Ignore IOE when closing streams
        		}
        	}
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException sqle) {
			}
		}
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException sqle) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Prints some information about the JDBC driver in use.
     *
     * @throws SQLException
     *             if one of the methods called does.
     *
     */
    private void printDriverInfo() throws SQLException {
        System.out.println("driver.getMajorVersion() is " + driver.getMajorVersion());
        System.out.println("driver.getMinorVersion() is " + driver.getMinorVersion());
        System.out.println("driver is " + (driver.jdbcCompliant() ? "" : "not ") + "JDBC compliant");

        DriverPropertyInfo[] infos = driver.getPropertyInfo(connectString, props);

        for (DriverPropertyInfo info : infos) {
            System.out.println("driver property named \"" + info.name + "\"");
            if (info.choices != null) {
                System.out.println("choices:");
                for (int j = 0; j < info.choices.length; j++)
                    System.out.println("\tchoice " + j + ": \"" + info.choices[j] + "\"");
            }
            System.out.println("description: \"" + info.description + "\"");
            System.out.println("required parameter?: \"" + info.required + "\"");
            System.out.println("current value: \"" + info.value + "\"\n");
        }

        DatabaseMetaData metaData = connection.getMetaData();

        System.out.println("metaData.getDatabaseProductName(): \"" + metaData.getDatabaseProductName() + "\"");
        System.out.println("metaData.getDatabaseProductVersion(): \"" + metaData.getDatabaseProductVersion() + "\"");

        System.out.println("metaData.getDriverName(): \"" + metaData.getDriverName() + "\"");
        System.out.println("metaData.getDriverVersion(): \"" + metaData.getDriverVersion() + "\"");
    }

    /**
     * Parse the command line arguments. This method parses what is needed for
     * the Jisql driver program and lets the configured formatter do the same.
     *
     * @param argv  the command line arguments.
     *
     * @throws Exception if there are any errors parsing the command line arguments.
     *
     */
    public void parseArgs(String argv[]) throws Throwable {
        //
        // I'm sure that there has to be a better way but I couldn't find a
        // command lineparser that would let me ignore unknown arguments. so
        // walk through the list once to find the formatter. then, use the
        // command line parser to do it "for real"
        //
    	String passwordValue=null;
    	for (int argumentIndex = 0; argumentIndex < argv.length; argumentIndex++) {
    		 if ("-p".equalsIgnoreCase(argv[argumentIndex]) || "-password".equalsIgnoreCase(argv[argumentIndex]) ) {
    			 if(argv.length>argumentIndex + 1){
    				 passwordValue=argv[argumentIndex + 1];
    				 argv[argumentIndex + 1]="";
    				 break;
    			 }
    		 }
    	}
        for (int argumentIndex = 0; argumentIndex < argv.length; argumentIndex++) {
            if (argv[argumentIndex].equals("-formatter")) {
                formatterClassName = argv[argumentIndex + 1];
                break;
            }
        }

        if (formatterClassName.compareToIgnoreCase("csv") == 0)
            formatterClassName = csvFormatterClassName;
        else if (formatterClassName.compareToIgnoreCase("xml") == 0)
            formatterClassName = xmlFormatterClassName;
        else if (formatterClassName.compareToIgnoreCase("default") == 0)
            formatterClassName = defaultFormatterClassName;

        formatter = (JisqlFormatter) Class.forName(formatterClassName).newInstance();

        OptionParser parser = new OptionParser();
        parser.posixlyCorrect(false);

        parser.accepts("c").withRequiredArg().ofType(String.class);
        parser.accepts("cstring").withRequiredArg().ofType(String.class);
        parser.accepts("debug");
        parser.accepts("driver").withRequiredArg().ofType(String.class);
        parser.accepts("driverinfo");
        parser.accepts("formatter").withRequiredArg().ofType(String.class);
        parser.accepts("help");
        parser.accepts("input").withRequiredArg().ofType(String.class);
        parser.accepts("password").withOptionalArg().ofType(String.class);
        parser.accepts("p").withOptionalArg().ofType(String.class);
        parser.accepts("pf").withRequiredArg().ofType(String.class);
        parser.accepts("query").withRequiredArg().ofType(String.class);
        parser.accepts("user").withRequiredArg().ofType(String.class);
        parser.accepts("u").withRequiredArg().ofType(String.class);

        formatter.setSupportedOptions(parser);

        OptionSet options = parser.parse(argv);

        if (options.has("help")) {
            usage();
            System.exit(1);
        }

        if (options.has("driver")) {
            driverName = (String) options.valueOf("driver");

            if (driverName.compareToIgnoreCase("jconnect4") == 0)
                driverName = sybaseJConnect4DriverName;
            else if (driverName.compareToIgnoreCase("jconnect5") == 0)
                driverName = sybaseJConnect5DriverName;
            else if (driverName.compareToIgnoreCase("jconnect6") == 0)
                driverName = sybaseJConnect6DriverName;
            else if (driverName.compareToIgnoreCase("oraclethin") == 0)
                driverName = oracleThinDriverName;
            else if (driverName.compareToIgnoreCase("db2app") == 0)
                driverName = db2AppDriverName;
            else if (driverName.compareToIgnoreCase("db2net") == 0)
                driverName = db2NetDriverName;
            else if (driverName.compareToIgnoreCase("cloudscape") == 0)
                driverName = cloudscapeDriverName;
            else if (driverName.compareToIgnoreCase("mssql") == 0)
                driverName = msqlDriverName;
            else if (driverName.compareToIgnoreCase("pointbase") == 0)
                driverName = pointbaseDriverName;
            else if (driverName.compareToIgnoreCase("postgresql") == 0)
                driverName = postgresqlDriverName;
            else if (driverName.compareToIgnoreCase("mysqlconj") == 0)
                driverName = mySQLConnectJDriverName;
            else if (driverName.compareToIgnoreCase("mysqlcaucho") == 0)
                driverName = mySQLCauchoDriverName;
            else if (driverName.compareToIgnoreCase("sapsajdbc4") == 0)
                driverName = sapJDBC4SqlAnywhereDriverName;
            else if (driverName.compareToIgnoreCase("sybasesajdbc4") == 0)
                driverName = sybaseJDBC4SqlAnywhereDriverName;
        }

        connectString = (String) options.valueOf("cstring");

        if (options.has("c"))
            commandTerminator = (String) options.valueOf("c");

        if (options.has("debug"))
            printDebug = true;

        if (options.has("user"))
            userName = (String) options.valueOf("user");
        else if (options.has("u"))
            userName = (String) options.valueOf("u");

        password=passwordValue;

        if (options.has("driverinfo"))
            printDriverDetails = true;

        if (options.has("input"))
            inputFileName = (String) options.valueOf("input");

        if (options.has("pf"))
            passwordFileName = (String) options.valueOf("pf");

        if (options.has("query"))
            inputQuery = (String) options.valueOf("query");

        if (driverName == null)
            throw new Exception("driver name must exist");

        if (connectString == null)
            throw new Exception("connect string must exist");

        if (userName == null)
            throw new Exception("user name must exist");

        if ((password == null) && (passwordFileName == null)) {
            password="";
        }
        else if (password == null) {
            File passwordFile = null;
            BufferedReader reader = null;

            passwordFile = new File(passwordFileName);
            if (!passwordFile.exists())
                throw new Exception("the password file \"" + passwordFileName + "\" does not exist");

            if (!passwordFile.isFile())
                throw new Exception("the password file \"" + passwordFileName + "\" is not a normal file");

            if (!passwordFile.canRead())
                throw new Exception("the password file \"" + passwordFileName + "\" is not readable");

            try {
                reader = new BufferedReader(new FileReader(passwordFile));
                password = reader.readLine().trim();
            }
            catch (Exception e) {
                throw new Exception("An error occured reading the password file", e);
            }
            finally {
                if (reader != null) {
                    try {
                        reader.close();
                    }
                    catch (Exception ignore) { /* ignored */
                    }
                }
            }
        }

        formatter.consumeOptions(options);
    }

    /**
     * Walks through a SQLException and prints out every exception.
     *
     * @param sqle the Exception to print
     *
     */
    private void printAllExceptions(SQLException sqle) {
        while (sqle != null) {
            System.err.println("SQLException : " + "SQL state: " + sqle.getSQLState() + " " + sqle.toString() + " ErrorCode: "
                    + sqle.getErrorCode());
            sqle = sqle.getNextException();
        }
    }

    /**
     * Prints out the usage message for the Jisql driver and the configured
     * formatter.
     *
     */
    private void usage() {
        System.err.println();
        System.err.println("usage: java " + getClass().getName() +
                           " -driver driver -cstring connect_string -user|-u username -password|-p password [-pf password_file] " +
                           "[-c command_term] [-input file_name] [-debug] [-driverinfo] [-formatter formatter]");
        System.err.println("where:");
        System.err
                .println("\t-driver specifies the JDBC driver to use.  There are several builtin shortcuts - see the docs for details.");
        System.err.println("\t-cstring specifies the connection string to use.  These are driver specific.");
        System.err.println("\t-user specifies a user name to log into a database server with.");
        System.err.println("\t-password specifies the user name to log into a database server with.");
        System.err.println("\t-pf specifies the name of a file that contains the password to log into a database server with.");
        System.err.println("\t    The first line of file should contain the password and nothing else.");
        System.err.println("\t-c specifies the command terminator.  The default is \"" + commandTerminator + "\"");
        System.err.println("\t-input specifies a file name to read commands from.");
        System.err
                .println("\t-query specifies an optional single query to run instead of interacting with the command line or a file.");
        System.err.println("\t       Note that the command must include a command terminator or the command will hang");
        System.err.println("\t-debug prints to stdout (System.out) debugging information");
        System.err.println("\t-driverinfo prints to stdout (System.out) detailed driver information and then exits");
        System.err
                .println("\t-formatter specifies either a class name or a pre-configured output formatter.  See the docs for details.");

        if (formatter != null) {
            System.err.println("Additional command line arguments of the " + formatter.getClass().getName() + " class are");
            formatter.usage(System.err);
        }

        System.err.println();
    }
}
