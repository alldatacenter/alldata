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
package org.apache.util.outputformatter;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


/**
 * This is the definition of what a JisqlFormatter does.
 *
 */
public interface JisqlFormatter {

	/**
     * Sets a the option list for this formatter.
     *
     * @param parser - the OptionParser to use.
     */
    void setSupportedOptions( OptionParser parser );

    /**
     * Consumes any options that were specified on the command line.
     *
     * @param options the OptionSet that the main driver is using.  Implementing
     *                classes should add their supported parameters to the list.
     *
     * @throws Exception if there is a problem parsing the command line arguments.
     *                   Note that Jisql includes jopt-simple so you can use that
     *                   to parse your command line.  See
     *                   <a href="http://jopt-simple.sourceforge.net/">http://jopt-simple.sourceforge.net/</a>
     *                   for more information.
     *
     */
    void consumeOptions( OptionSet options ) throws Exception;

    /**
     * Called to output a usage message to the command line window.  This
     * message should contain information on how to call the formatter.
     *
     * @param out where to put the usage message.
     *
     */
    void usage( PrintStream out );


    /**
     * Outputs a header for a query.  This is called before any data is
     * output.
     *
     * @param out where to put header output.
     * @param metaData the ResultSetMetaData for the output.
     *
     */
    void formatHeader( PrintStream out, ResultSetMetaData metaData ) throws Exception;

    /**
     * Called to output the data.
     *
     * @param out where to put output data.
     * @param resultSet the ResultSet for the row.
     * @param metaData the ResultSetMetaData for the row.
     *
     */
    void formatData( PrintStream out, ResultSet resultSet, ResultSetMetaData metaData ) throws Exception;

    /**
     * Outputs a footer for a query.  This is called after all data has been
     * exhausted.
     *
     * @param out where to put footer output.
     * @param metaData the ResultSetMetaData for the output.
     *
     */
    void formatFooter( PrintStream out, ResultSetMetaData metaData ) throws Exception;
}
