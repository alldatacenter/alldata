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
 * This is the default formatter for Jisql.  It outputs data in a &quot;normal&quot;
 * format that is similar to most other database command line formatters.
 *
 */
public class CSVFormatter implements JisqlFormatter {
    private char delimiter = ',';
    private boolean includeColumnNames = false;


    /**
     * Sets a the supported option list for this formatter.  This formatter accepts
     * the following options:
     *
     * <p>&nbsp;</p>
     *
     * <ul>
     * <li><<b>delimiter</b> specifies the delimiter to use.  By default a comma is
     * used</li>
     * <li><b>colnames</b> if included then column names are printed as the first
     * line of output.  By default they are not included</li>
     * </ul>
     *
     * @param parser the OptionParser to use.
     *
     */
    public void setSupportedOptions( OptionParser parser ) {
        parser.accepts( "delimiter" ).withRequiredArg().ofType( String.class );
        parser.accepts( "colnames" );
    }

    /**
     * Consumes any options that were specified on the command line.
     *
     * @param options the OptionSet that the main driver is using.
     *
     * @throws Exception if there is a problem parsing the command line arguments.
     *
     */
    public void consumeOptions( OptionSet options ) throws Exception {
        if( options.has( "delimiter" ) )
            delimiter = ((String)(options.valueOf( "delimiter" ))).charAt( 0 );

        if( options.has( "colnames" ) )
        	includeColumnNames = true;
    }

    /**
     * Called to output a usage message to the command line window.  This
     * message should contain information on how to call the formatter.
     *
     * @param out the PrintStream to display the usage message on.
     *
     */
    public void usage( PrintStream out ) {
        out.println("\t-delimiter specifies the character to use as the delimiter.  This defaults to \"" + delimiter + "\"" );
        out.println("\t-colnames outputs column names.  By default there are no column names." );
    }

    /**
     * Outputs an optional header for the CSV data.  This header is only enabled
     * if the &quot;colnames&quot; parameter is included.
     *
     * @param out a PrintStream to send any output to.
     * @param metaData the ResultSetMetaData for the output.
     *
     */
    public void formatHeader( PrintStream out, ResultSetMetaData metaData ) throws Exception {
        if( includeColumnNames ) {
            int numColumns = metaData.getColumnCount();

            //
            // output the column names
            //
            for (int i = 1; i <= numColumns; i++) {
                out.print( metaData.getColumnName(i).trim() );
                if( (i + 1) <= numColumns )
                	out.print( delimiter );
            }

            out.println();
        }
    }


    /**
     * Called to output the data.
     *
     * @param out the PrintStream to output data to.
     * @param resultSet the ResultSet for the row.
     * @param metaData the ResultSetMetaData for the row.
     *
     *
     */
    public void formatData( PrintStream out, ResultSet resultSet, ResultSetMetaData metaData ) throws Exception{
    }


    /**
     * Outputs a footer for a query.  For the CSVFormatter this method does nothing.
     *
     * @param out the PrintStream to output data to.
     * @param metaData the ResultSetMetaData for the output.
     *
     */
    public void formatFooter( PrintStream out, ResultSetMetaData metaData ) throws Exception {
    }
}
