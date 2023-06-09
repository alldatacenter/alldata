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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.easy.text.reader;

import com.univocity.parsers.common.TextParsingException;
import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/*******************************************************************************
 * Portions Copyright 2014 uniVocity Software Pty Ltd
 ******************************************************************************/

/**
 * A byte-based Text parser implementation. Builds heavily upon the uniVocity parsers. Customized for UTF8 parsing and
 * DrillBuf support.
 */
public final class TextReader {

  private static final Logger logger = LoggerFactory.getLogger(TextReader.class);

  private static final byte NULL_BYTE = (byte) TextFormatPlugin.NULL_CHAR;

  private final TextParsingContext context;

  private final TextParsingSettings settings;

  private final TextInput input;
  private final TextOutput output;

  // TODO: Remove this; it is a vestige of the "V2" implementation
  // and appears to be used only for white-space handling, which is
  // overkill.
  private final DrillBuf workBuf;

  private byte ch;

  // index of the field within this record
  private int fieldIndex;

  /** Behavior settings **/
  private final boolean ignoreTrailingWhitespace;
  private final boolean ignoreLeadingWhitespace;
  private final boolean parseUnescapedQuotes;

  /** Key Characters **/
  private final byte comment;
  private final byte delimiter;
  private final byte quote;
  private final byte quoteEscape;
  private final byte newLine;

  /**
   * The CsvParser supports all settings provided by {@link TextParsingSettings},
   * and requires this configuration to be properly initialized.
   * @param settings  the parser configuration
   * @param input  input stream
   * @param output  interface to produce output record batch
   * @param workBuf  working buffer to handle whitespace
   */
  public TextReader(TextParsingSettings settings, TextInput input, TextOutput output, DrillBuf workBuf) {
    this.context = new TextParsingContext(input, output);
    this.workBuf = workBuf;
    this.settings = settings;

    this.ignoreTrailingWhitespace = settings.ignoreTrailingWhitespace();
    this.ignoreLeadingWhitespace = settings.ignoreLeadingWhitespace();
    this.parseUnescapedQuotes = settings.parseUnescapedQuotes();
    this.delimiter = settings.getDelimiter();
    this.quote = settings.getQuote();
    this.quoteEscape = settings.getQuoteEscape();
    this.newLine = settings.getNormalizedNewLine();
    this.comment = settings.getComment();

    this.input = input;
    this.output = output;
  }

  public TextOutput getOutput() { return output; }

  /**
   * Check if the given byte is a white space. As per the univocity text reader
   * any ASCII <= ' ' is considered a white space. However since byte in JAVA is signed
   * we have an additional check to make sure its not negative
   */
  static boolean isWhite(byte b){
    return b <= ' ' && b > -1;
  }

  /**
   * Inform the output interface to indicate we are starting a new record batch
   */
  public void resetForNextBatch() { }

  public long getPos() { return input.getPos(); }

  /**
   * Function encapsulates parsing an entire record, delegates parsing of the
   * fields to parseField() function.
   * We mark the start of the record and if there are any failures encountered (OOM for eg)
   * then we reset the input stream to the marked position
   * @return  true if parsing this record was successful; false otherwise
   * @throws IOException for input file read errors
   */
  private boolean parseRecord() throws IOException {
    final byte newLine = this.newLine;
    final TextInput input = this.input;

    input.mark();

    fieldIndex = 0;
    if (ignoreLeadingWhitespace && isWhite(ch)) {
      skipWhitespace();
    }

    output.startRecord();
    int fieldsWritten = 0;
    try {
      while (ch != newLine) {
        parseField();
        fieldsWritten++;
        if (ch != newLine) {
          ch = input.nextChar();
          if (ch == newLine) {
            output.startField(fieldsWritten++);
            output.endEmptyField();
            break;
          }
        }
      }
      output.finishRecord();
    } catch (StreamFinishedPseudoException e) {

      // if we've written part of a field or all of a field, we should send this row.

      if (fieldsWritten == 0) {
        throw e;
      } else {
        output.finishRecord();
      }
    }
    return true;
  }

  /**
   * Function parses an individual field and ignores any white spaces encountered
   * by not appending it to the output vector
   * @throws IOException for input file read errors
   */
  private void parseValueIgnore() throws IOException {
    final byte newLine = this.newLine;
    final byte delimiter = this.delimiter;
    final TextInput input = this.input;

    byte ch = this.ch;
    while (ch != delimiter && ch != newLine) {
      appendIgnoringWhitespace(ch);
      ch = input.nextChar();
    }
    this.ch = ch;
  }

  public void appendIgnoringWhitespace(byte data) {
    if (! isWhite(data)) {
      output.append(data);
    }
  }

  /**
   * Function parses an individual field and appends all characters till the delimeter (or newline)
   * to the output, including white spaces
   * @throws IOException for input file read errors
   */
  private void parseValueAll() throws IOException {
    final byte newLine = this.newLine;
    final byte delimiter = this.delimiter;
    final TextOutput output = this.output;
    final TextInput input = this.input;

    byte ch = this.ch;
    while (ch != delimiter && ch != newLine) {
      output.append(ch);
      ch = input.nextChar();
    }
    this.ch = ch;
  }

  /**
   * Function simply delegates the parsing of a single field to the actual
   * implementation based on parsing config
   *
   * @throws IOException
   *           for input file read errors
   */
  private void parseValue() throws IOException {
    if (ignoreTrailingWhitespace) {
      parseValueIgnore();
    } else {
      parseValueAll();
    }
  }

  /**
   * Recursive function invoked when a quote is encountered. Function also
   * handles the case when there are non-white space characters in the field
   * after the quoted value.
   * <p>
   * Handles quotes and quote escapes:
   * <ul>
   * <li>[escape][quote] - escapes the quote</li>
   * <li>[escape][! quote] - emits both the escape and
   * the next char</li>
   * <li>escape = quote, [quote][quote] - escapes the
   * quote.</li>
   * </ul>
   * @param prev  previous byte read
   * @throws IOException for input file read errors
   */
  private void parseQuotedValue(byte prev) throws IOException {
    final byte newLine = this.newLine;
    final byte delimiter = this.delimiter;
    final TextOutput output = this.output;
    final TextInput input = this.input;
    final byte quote = this.quote;
    final byte quoteEscape = this.quoteEscape;

    ch = input.nextCharNoNewLineCheck();

    while (!(prev == quote && (ch == delimiter || ch == newLine || isWhite(ch)))) {
      if (ch == quote) {
        if (prev == quoteEscape) {
          output.append(ch);
          prev = NULL_BYTE;
        } else {
          prev = ch;
          // read next char taking into account it can be new line indicator
          // to ensure that custom new line will be replaced with normalized one
          ch = input.nextChar();
          continue;
        }
      } else {
        if (prev == quoteEscape) {
          output.append(prev);
        } else if (prev == quote) { // unescaped quote detected
          if (parseUnescapedQuotes) {
            output.append(prev);
            break;
          } else {
            throw new TextParsingException(
                context,
                "Unescaped quote character '"
                    + quote
                    + "' inside quoted value of CSV field. To allow unescaped quotes, "
                    + "set 'parseUnescapedQuotes' to 'true' in the CSV parser settings. "
                    + "Cannot parse CSV input.");
          }
        }
        if (ch == quoteEscape) {
          prev = ch;
        } else {
          output.append(ch);
          prev = ch;
        }
      }
      ch = input.nextCharNoNewLineCheck();
    }

    // Handles whitespace after quoted value:
    // Whitespace are ignored (i.e., ch <= ' ') if they are not used as delimiters (i.e., ch != ' ')
    // For example, in tab-separated files (TSV files), '\t' is used as delimiter and should not be ignored
    // Content after whitespace may be parsed if 'parseUnescapedQuotes' is enabled.
    if (ch != newLine && ch <= ' ' && ch != delimiter) {
      final DrillBuf workBuf = this.workBuf;
      workBuf.resetWriterIndex();
      do {
        // saves whitespace after value
        workBuf.writeByte(ch);
        ch = input.nextChar();
        // found a new line, go to next record.
        if (ch == newLine) {
          return;
        }
      } while (ch <= ' ' && ch != delimiter);

      // there's more stuff after the quoted value, not only empty spaces.
      if (!(ch == delimiter || ch == newLine) && parseUnescapedQuotes) {

        output.append(quote);
        for(int i =0; i < workBuf.writerIndex(); i++){
          output.append(workBuf.getByte(i));
        }
        // the next character is not the escape character, put it there
        if (ch != quoteEscape) {
          output.append(ch);
        }
        // sets this character as the previous character (may be escaping)
        // calls recursively to keep parsing potentially quoted content
        parseQuotedValue(ch);
      }
    }

    if (!(ch == delimiter || ch == newLine)) {
      throw new TextParsingException(context, "Unexpected character '" + ch
          + "' following quoted value of CSV field. Expecting '" + delimiter + "'. Cannot parse CSV input.");
    }
  }

  /**
   * Captures the entirety of parsing a single field and based on the input delegates to the appropriate function
   * @return true if more rows can be read, false if not
   * @throws IOException for input file read errors
   */
  private boolean parseField() throws IOException {

    output.startField(fieldIndex++);

    if (isWhite(ch) && ignoreLeadingWhitespace) {
      skipWhitespace();
    }

    // Delimiter? Then this is an empty field.

    if (ch == delimiter) {
      return output.endEmptyField();
    }

    // Have the first character of the field. Parse and save the
    // field, even if we hit EOF. (An EOF identifies a last line
    // that contains data, but is not terminated with a newline.)

    try {
      if (ch == quote) {
        parseQuotedValue(NULL_BYTE);
      } else {
        parseValue();
      }
      return output.endField();
    } catch (StreamFinishedPseudoException e) {
      return output.endField();
    }
  }

  /**
   * Helper function to skip white spaces occurring at the current input stream.
   * @throws IOException for input file read errors
   */
  private void skipWhitespace() throws IOException {
    final byte delimiter = this.delimiter;
    final byte newLine = this.newLine;
    final TextInput input = this.input;

    while (isWhite(ch) && ch != delimiter && ch != newLine) {
      ch = input.nextChar();
    }
  }

  /**
   * Starting point for the reader. Sets up the input interface.
   * @throws IOException for input file read errors
   */
  public final void start() throws IOException {
    context.stop(false);
    input.start();
  }

  /**
   * Parses the next record from the input. Will skip the line if its a comment,
   * this is required when the file contains headers
   * @throws IOException for input file read errors
   */
  public final boolean parseNext() throws IOException {
    try {
      while (!context.isStopped()) {
        ch = input.nextChar();
        if (ch == comment) {
          input.skipLines(1);
          continue;
        }
        break;
      }
      final long initialLineNumber = input.lineCount();
      boolean success = parseRecord();
      if (initialLineNumber + 1 < input.lineCount()) {
        throw new TextParsingException(context, "Cannot use newline character within quoted string");
      }

      return success;
    } catch (UserException ex) {
      stopParsing();
      throw ex;
    } catch (StreamFinishedPseudoException ex) {
      stopParsing();
      return false;
    } catch (Exception ex) {
      try {
        throw handleException(ex);
      } finally {
        stopParsing();
      }
    }
  }

  private void stopParsing() { }

  private String displayLineSeparators(String str, boolean addNewLine) {
    if (addNewLine) {
      if (str.contains("\r\n")) {
        str = str.replaceAll("\\r\\n", "[\\\\r\\\\n]\r\n\t");
      } else if (str.contains("\n")) {
        str = str.replaceAll("\\n", "[\\\\n]\n\t");
      } else {
        str = str.replaceAll("\\r", "[\\\\r]\r\t");
      }
    } else {
      str = str.replaceAll("\\n", "\\\\n");
      str = str.replaceAll("\\r", "\\\\r");
    }
    return str;
  }

  /**
   * Helper method to handle exceptions caught while processing text files and generate better error messages associated with
   * the exception.
   * @param ex  Exception raised
   * @throws IOException for input file read errors
   */
  private TextParsingException handleException(Exception ex) throws IOException {

    if (ex instanceof TextParsingException) {
      throw (TextParsingException) ex;
    }

    String message = null;
    String tmp = input.getStringSinceMarkForError();
    char[] chars = tmp.toCharArray();
    if (chars != null) {
      int length = chars.length;
      if (length > settings.getMaxCharsPerColumn()) {
        message = "Length of parsed input (" + length
            + ") exceeds the maximum number of characters defined in your parser settings ("
            + settings.getMaxCharsPerColumn() + "). ";
      }

      if (tmp.contains("\n") || tmp.contains("\r")) {
        tmp = displayLineSeparators(tmp, true);
        String lineSeparator = displayLineSeparators(settings.getLineSeparatorString(), false);
        message += "\nIdentified line separator characters in the parsed content. This may be the cause of the error. The line separator in your parser settings is set to '"
            + lineSeparator + "'. Parsed content:\n\t" + tmp;
      }

      int nullCharacterCount = 0;
      // ensuring the StringBuilder won't grow over Integer.MAX_VALUE to avoid OutOfMemoryError
      int maxLength = length > Integer.MAX_VALUE / 2 ? Integer.MAX_VALUE / 2 - 1 : length;
      StringBuilder s = new StringBuilder(maxLength);
      for (int i = 0; i < maxLength; i++) {
        if (chars[i] == '\0') {
          s.append('\\');
          s.append('0');
          nullCharacterCount++;
        } else {
          s.append(chars[i]);
        }
      }
      tmp = s.toString();

      if (nullCharacterCount > 0) {
        message += "\nIdentified "
            + nullCharacterCount
            + " null characters ('\0') on parsed content. This may indicate the data is corrupt or its encoding is invalid. Parsed content:\n\t"
            + tmp;
      }
    }

    UserException.Builder builder;
    if (ex instanceof UserException) {
      builder = ((UserException) ex).rebuild();
    } else {
      builder = UserException
        .dataReadError(ex)
        .message(message);
    }
    throw builder
      .addContext("Line", context.currentLine())
      .addContext("Record", context.currentRecord())
      .build(logger);
  }

  /**
   * Finish the processing of a batch, indicates to the output
   * interface to wrap up the batch
   */
  public void finishBatch() { }

  /**
   * Invoked once there are no more records and we are done with the
   * current record reader to clean up state.
   * @throws IOException for input file read errors
   */
  public void close() throws IOException {
    input.close();
  }
}
