/*
   IniEditor is Copyright (c) 2003-2013, Nik Haldimann
   All rights reserved. Distributed under a BSD-style license.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:

   * Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
   OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.aliyun.oss.common.utils;

import java.io.*;
import java.util.*;

/**
 * Loads, edits and saves INI-style configuration files. While loading from and
 * saving to streams and files, <code>IniEditor</code> preserves comments and
 * blank lines as well as the order of sections and lines in general.
 * <p>
 * <code>IniEditor</code> assumes configuration files to be split in sections. A
 * section starts out with a header, which consists of the section name enclosed
 * in brackets (<code>'['</code> and <code>']'</code>). Everything before the
 * first section header is ignored when loading from a stream or file. The
 * <code>{@link IniEditor.Section}</code> class can be used to load
 * configuration files without sections (ie Java-style properties).
 * <p>
 * A "common section" may be named. All sections inherit the options of this
 * section but can overwrite them.
 * <p>
 * <code>IniEditor</code> represents an INI file (or rather, its sections) line
 * by line, as comment, blank and option lines. A comment is a line which has a
 * comment delimiter as its first non-white space character. The default comment
 * delimiters, which may be overwritten, are <code>'#'</code> and
 * <code>';'</code>.
 * <p>
 * A blank line is any line that consists only of white space.
 * <p>
 * Everything else is an option line. Option names and values are separated by
 * option delimiters <code>'='</code>, <code>':'</code> or white space (spaces
 * and tabs).
 * <p>
 * Here's a minimal example. Suppose, we have this in a file called
 * <code>users.ini</code>:
 * 
 * <pre>
 *   [root]
 *   role = administrator
 *   last_login = 2003-05-04
 *
 *   [joe]
 *   role = author
 *   last_login = 2003-05-13
 * </pre>
 * 
 * Let's load that file, add something to it and save the changes:
 * 
 * <pre>
 * IniEditor users = new IniEditor();
 * users.load("users.ini");
 * users.set("root", "last_login", "2003-05-16");
 * users.addComment("root", "Must change password often");
 * users.set("root", "change_pwd", "10 days");
 * users.addBlankLine("root");
 * users.save("users.ini");
 * </pre>
 * 
 * Now, the file looks like this:
 * 
 * <pre>
 *   [root]
 *   role = administrator
 *   last_login = 2003-05-16
 *
 *   # Must change password often
 *   change_pwd = 10 days
 *
 *   [joe]
 *   role = author
 *   last_login = 2003-05-13
 * </pre>
 * <p>
 * IniEditor provides services simliar to the standard Java API class
 * <code>java.util.Properties</code>. It uses its own parser, though, which
 * differs in these respects from that of <code>Properties</code>:
 * <ul>
 * <li>Line continuations (backslashes at the end of an option line) are not
 * supported.</li>
 * <li>No kind of character escaping is performed or recognized. Characters are
 * read and written in in the default character encoding. If you want to use a
 * different character encoding, use the {@link #load(InputStreamReader)} and
 * {@link #save(OutputStreamWriter)} methods with a reader and writer tuned to
 * the desired character encoding.</li>
 * <li>As a consequence, option names may not contain option/value separators
 * (normally <code>'='</code>, <code>':'</code> and white space).</li>
 * </ul>
 *
 * @author Nik Haldimann, nhaldimann at gmail dot com
 * @version r5 (3/4/2013)
 */
public class IniEditor {

    private static boolean DEFAULT_CASE_SENSITIVITY = false;

    private Map<String, Section> sections;
    private List<String> sectionOrder;
    private String commonName;
    private char[] commentDelims;
    private boolean isCaseSensitive;
    private OptionFormat optionFormat;

    /**
     * Constructs new bare IniEditor instance.
     */
    public IniEditor() {
        this(null, null);
    }

    /**
     * Constructs new bare IniEditor instance specifying case-sensitivity.
     *
     * @param isCaseSensitive
     *            section and option names are case-sensitive if this is true
     */
    public IniEditor(boolean isCaseSensitive) {
        this(null, null, isCaseSensitive);
    }

    /**
     * Constructs new IniEditor instance with a common section. Options in the
     * common section are used as defaults for all other sections.
     *
     * @param commonName
     *            name of the common section
     */
    public IniEditor(String commonName) {
        this(commonName, null);
    }

    /**
     * Constructs new IniEditor instance with a common section. Options in the
     * common section are used as defaults for all other sections.
     *
     * @param commonName
     *            name of the common section
     * @param isCaseSensitive
     *            section and option names are case-sensitive if this is true
     */
    public IniEditor(String commonName, boolean isCaseSensitive) {
        this(commonName, null, isCaseSensitive);
    }

    /**
     * Constructs new IniEditor defining comment delimiters.
     *
     * @param delims
     *            an array of characters to be recognized as starters of comment
     *            lines; the first of them will be used for newly created
     *            comments
     */
    public IniEditor(char[] delims) {
        this(null, delims);
    }

    /**
     * Constructs new IniEditor defining comment delimiters.
     *
     * @param delims
     *            an array of characters to be recognized as starters of comment
     *            lines; the first of them will be used for newly created
     *            comments
     * @param isCaseSensitive
     *            section and option names are case-sensitive if this is true
     */
    public IniEditor(char[] delims, boolean isCaseSensitive) {
        this(null, delims, isCaseSensitive);
    }

    /**
     * Constructs new IniEditor instance with a common section, defining comment
     * delimiters. Options in the common section are used as defaults for all
     * other sections.
     *
     * @param commonName
     *            name of the common section
     * @param delims
     *            an array of characters to be recognized as starters of comment
     *            lines; the first of them will be used for newly created
     *            comments
     */
    public IniEditor(String commonName, char[] delims) {
        this(commonName, delims, DEFAULT_CASE_SENSITIVITY);
    }

    /**
     * Constructs new IniEditor instance with a common section, defining comment
     * delimiters. Options in the common section are used as defaults for all
     * other sections.
     *
     * @param commonName
     *            name of the common section
     * @param delims
     *            an array of characters to be recognized as starters of comment
     *            lines; the first of them will be used for newly created
     *            comments
     * @param isCaseSensitive
     *            option names are case-sensitive if this is true
     */
    public IniEditor(String commonName, char[] delims, boolean isCaseSensitive) {
        this.sections = new HashMap<String, Section>();
        this.sectionOrder = new LinkedList<String>();
        this.isCaseSensitive = isCaseSensitive;
        if (commonName != null) {
            this.commonName = commonName;
            addSection(this.commonName);
        }
        this.commentDelims = delims;
        this.optionFormat = new OptionFormat(Section.DEFAULT_OPTION_FORMAT);
    }

    /**
     * Sets the option format for this instance to the given string. Options
     * will be rendered according to the given format string when printed. The
     * string must contain <code>%s</code> three times, these will be replaced
     * with the option name, the option separator and the option value in this
     * order. Literal percentage signs must be escaped by preceding them with
     * another percentage sign (i.e., <code>%%</code> corresponds to one
     * percentage sign). The default format string is <code>"%s %s %s"</code>.
     *
     * Option formats may look like format strings as supported by Java 1.5, but
     * the string is in fact parsed in a custom fashion to guarantee backwards
     * compatibility. So don't try clever stuff like using format conversion
     * types other than <code>%s</code>.
     *
     * @param formatString
     *            a format string, containing <code>%s</code> exactly three
     *            times
     * @throws IllegalArgumentException
     *             if the format string is illegal
     */
    public void setOptionFormatString(String formatString) {
        this.optionFormat = new OptionFormat(formatString);
    }

    /**
     * Returns the value of a given option in a given section or null if either
     * the section or the option don't exist. If a common section was defined
     * options are also looked up there if they're not present in the specific
     * section.
     *
     * @param section
     *            the section's name
     * @param option
     *            the option's name
     * @return the option's value
     * @throws NullPointerException
     *             any of the arguments is <code>null</code>
     */
    public String get(String section, String option) {
        if (hasSection(section)) {
            Section sect = getSection(section);
            if (sect.hasOption(option)) {
                return sect.get(option);
            }
            if (this.commonName != null) {
                return getSection(this.commonName).get(option);
            }
        }
        return null;
    }

    /**
     * Returns a map of a given section with option/value pairs or null if the
     * section doesn't exist.
     *
     * @param section
     *            the section's name
     * @return HashMap of option/value pairs from the section
     * @throws NullPointerException
     *             if section is <code>null</code>
     */
    public Map<String, String> getSectionMap(String section) {
        Map<String, String> sectionMap = new HashMap<String, String>();
        if (hasSection(section)) {
            Section sect = getSection(section);
            for (String key : sect.options.keySet()) {
                sectionMap.put(key, sect.options.get(key).value);
            }
            return sectionMap;
        }
        return null;
    }

    /**
     * Sets the value of an option in a section, if the option exist, otherwise
     * adds the option to the section. Trims white space from the start and the
     * end of the value and deletes newline characters it might contain.
     *
     * @param section
     *            the section's name
     * @param option
     *            the option's name
     * @param value
     *            the option's value
     * @throws IniEditor.NoSuchSectionException
     *             no section with the given name exists
     * @throws IllegalArgumentException
     *             the option name is illegal, ie contains a '=' character or
     *             consists only of white space
     * @throws NullPointerException
     *             section or option are <code>null</code>
     */
    public void set(String section, String option, String value) {
        if (hasSection(section)) {
            getSection(section).set(option, value);
        } else {
            throw new NoSuchSectionException(section);
        }
    }

    /**
     * Removes an option from a section if it exists. Will not remove options
     * from the common section if it's not directly addressed.
     *
     * @param section
     *            the section's name
     * @param option
     *            the option's name
     * @return <code>true</code> if the option was actually removed
     * @throws IniEditor.NoSuchSectionException
     *             no section with the given name exists
     */
    public boolean remove(String section, String option) {
        if (hasSection(section)) {
            return getSection(section).remove(option);
        } else {
            throw new NoSuchSectionException(section);
        }
    }

    /**
     * Checks whether an option exists in a given section. Options in the common
     * section are assumed to not exist in particular sections, unless they're
     * overwritten.
     *
     * @param section
     *            the section's name
     * @param option
     *            the option's name
     * @return true if the given section has the option
     */
    public boolean hasOption(String section, String option) {
        return hasSection(section) && getSection(section).hasOption(option);
    }

    /**
     * Checks whether a section with a particular name exists in this instance.
     *
     * @param name
     *            the name of the section
     * @return true if the section exists
     */
    public boolean hasSection(String name) {
        return this.sections.containsKey(normSection(name));
    }

    /**
     * Adds a section if it doesn't exist yet.
     *
     * @param name
     *            the name of the section
     * @return <code>true</code> if the section didn't already exist
     * @throws IllegalArgumentException
     *             the name is illegal, ie contains one of the characters '['
     *             and ']' or consists only of white space
     */
    public boolean addSection(String name) {
        String normName = normSection(name);
        if (!hasSection(normName)) {
            // Section constructor might throw IllegalArgumentException
            Section section = new Section(normName, this.commentDelims, this.isCaseSensitive);
            section.setOptionFormat(this.optionFormat);
            this.sections.put(normName, section);
            this.sectionOrder.add(normName);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes a section if it exists.
     *
     * @param name
     *            the section's name
     * @return <code>true</code> if the section actually existed
     * @throws IllegalArgumentException
     *             when trying to remove the common section
     */
    public boolean removeSection(String name) {
        String normName = normSection(name);
        if (this.commonName != null && this.commonName.equals(normName)) {
            throw new IllegalArgumentException("Can't remove common section");
        }
        if (hasSection(normName)) {
            this.sections.remove(normName);
            this.sectionOrder.remove(normName);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns all section names in this instance minus the common section if
     * one was defined.
     *
     * @return list of the section names in original/insertion order
     */
    public List<String> sectionNames() {
        List<String> sectList = new ArrayList<String>(this.sectionOrder);
        if (this.commonName != null) {
            sectList.remove(this.commonName);
        }
        return sectList;
    }

    /**
     * Returns all option names of a section, not including options from the
     * common section.
     *
     * @param section
     *            the section's name
     * @return list of option names
     * @throws IniEditor.NoSuchSectionException
     *             no section with the given name exists
     */
    public List<String> optionNames(String section) {
        if (hasSection(section)) {
            return getSection(section).optionNames();
        } else {
            throw new NoSuchSectionException(section);
        }
    }

    /**
     * Adds a comment line to the end of a section. A comment spanning several
     * lines (ie with line breaks) will be split up, one comment line for each
     * line.
     *
     * @param section
     *            the section's name
     * @param comment
     *            the comment
     * @throws IniEditor.NoSuchSectionException
     *             no section with the given name exists
     */
    public void addComment(String section, String comment) {
        if (hasSection(section)) {
            getSection(section).addComment(comment);
        } else {
            throw new NoSuchSectionException(section);
        }
    }

    /**
     * Adds a blank line to the end of a section.
     *
     * @param section
     *            the section's name
     * @throws IniEditor.NoSuchSectionException
     *             no section with the given name exists
     */
    public void addBlankLine(String section) {
        if (hasSection(section)) {
            getSection(section).addBlankLine();
        } else {
            throw new NoSuchSectionException(section);
        }
    }

    /**
     * Writes this instance in INI format to a file.
     *
     * @param filename
     *            the file to write to
     * @throws IOException
     *             at an I/O problem
     */
    public void save(String filename) throws IOException {
        save(new File(filename));
    }

    /**
     * Writes this instance in INI format to a file.
     *
     * @param file
     *            where to save to
     * @throws IOException
     *             at an I/O problem
     */
    public void save(File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        save(out);
        out.close();
    }

    /**
     * Writes this instance in INI format to an output stream. This method takes
     * an <code>OutputStream</code> for maximum flexibility, internally it does
     * of course use a writer for character based output.
     *
     * @param stream
     *            where to write
     * @throws IOException
     *             at an I/O problem
     */
    public void save(OutputStream stream) throws IOException {
        save(new OutputStreamWriter(stream));
    }

    /**
     * Writes this instance in INI format to an output stream writer.
     *
     * @param streamWriter
     *            where to write
     * @throws IOException
     *             at an I/O problem
     */
    public void save(OutputStreamWriter streamWriter) throws IOException {
        Iterator<String> it = this.sectionOrder.iterator();
        PrintWriter writer = new PrintWriter(streamWriter, true);
        while (it.hasNext()) {
            Section sect = getSection(it.next());
            writer.println(sect.header());
            sect.save(writer);
        }
    }

    /**
     * Loads INI formatted input from a file into this instance, using the
     * default character encoding. Everything in the file before the first
     * section header is ignored.
     *
     * @param filename
     *            file to read from
     * @throws IOException
     *             at an I/O problem
     */
    public void load(String filename) throws IOException {
        load(new File(filename));
    }

    /**
     * Loads INI formatted input from a file into this instance, using the
     * default character encoding. Everything in the file before the first
     * section header is ignored.
     *
     * @param file
     *            file to read from
     * @throws IOException
     *             at an I/O problem
     */
    public void load(File file) throws IOException {
        InputStream in = null;
        try {
        	in = new FileInputStream(file);
        	load(in);
        } finally {
        	if (in != null) {
        		in.close();
        	}
        }
    }

    /**
     * Loads INI formatted input from a stream into this instance, using the
     * default character encoding. This method takes an <code>InputStream</code>
     * for maximum flexibility, internally it does use a reader (using the
     * default character encoding) for character based input. Everything in the
     * stream before the first section header is ignored.
     *
     * @param stream
     *            where to read from
     * @throws IOException
     *             at an I/O problem
     */
    public void load(InputStream stream) throws IOException {
        load(new InputStreamReader(stream));
    }

    /**
     * Loads INI formatted input from a stream reader into this instance.
     * Everything in the stream before the first section header is ignored.
     *
     * @param streamReader
     *            where to read from
     * @throws IOException
     *             at an I/O problem
     */
    public void load(InputStreamReader streamReader) throws IOException {
        BufferedReader reader = new BufferedReader(streamReader);
        String curSection = null;
        String line = null;

        while (reader.ready()) {
            line = reader.readLine().trim();
            if (line.length() > 0 && line.charAt(0) == Section.HEADER_START) {
                int endIndex = line.indexOf(Section.HEADER_END);
                if (endIndex >= 0) {
                    curSection = line.substring(1, endIndex);
                    addSection(curSection);
                }
            }
            if (curSection != null) {
                Section sect = getSection(curSection);
                sect.load(reader);
            }
        }
    }

    /**
     * Returns a section by name or <code>null</code> if not found.
     *
     * @param name
     *            the section's name
     * @return the section
     */
    private Section getSection(String name) {
        return sections.get(normSection(name));
    }

    /**
     * Normalizes an arbitrary string for use as a section name. Currently only
     * makes the string lower-case (provided this instance isn't case-
     * sensitive) and trims leading and trailing white space. Note that
     * normalization isn't enforced by the Section class.
     *
     * @param name
     *            the string to be used as section name
     * @return a normalized section name
     */
    private String normSection(String name) {
        if (!this.isCaseSensitive) {
            name = name.toLowerCase();
        }
        return name.trim();
    }

    /**
     * Loads, edits and saves a section of an INI-style configuration file. This
     * class does actually belong to the internals of {@link IniEditor} and
     * should rarely ever be used directly. It's exposed because it can be
     * useful for plain, section-less configuration files (Java-style
     * properties, for example).
     */
    public static class Section {

        private String name;
        private Map<String, Option> options;
        private List<Line> lines;
        private char[] optionDelims;
        private char[] optionDelimsSorted;
        private char[] commentDelims;
        private char[] commentDelimsSorted;
        private boolean isCaseSensitive;
        private OptionFormat optionFormat;

        private static final char[] DEFAULT_OPTION_DELIMS = new char[] { '=', ':' };
        private static final char[] DEFAULT_COMMENT_DELIMS = new char[] { '#', ';' };
        private static final char[] OPTION_DELIMS_WHITESPACE = new char[] { ' ', '\t' };
        private static final boolean DEFAULT_CASE_SENSITIVITY = false;
        public static final String DEFAULT_OPTION_FORMAT = "%s %s %s";

        public static final char HEADER_START = '[';
        public static final char HEADER_END = ']';
        private static final int NAME_MAXLENGTH = 1024;
        private static final char[] INVALID_NAME_CHARS = { HEADER_START, HEADER_END };

        /**
         * Constructs a new section.
         *
         * @param name
         *            the section's name
         * @throws IllegalArgumentException
         *             the section's name is illegal
         */
        public Section(String name) {
            this(name, null);
        }

        /**
         * Constructs a new section, specifying case-sensitivity.
         *
         * @param name
         *            the section's name
         * @param isCaseSensitive
         *            option names are case-sensitive if this is true
         * @throws IllegalArgumentException
         *             the section's name is illegal
         */
        public Section(String name, boolean isCaseSensitive) {
            this(name, null, isCaseSensitive);
        }

        /**
         * Constructs a new section, defining comment delimiters.
         *
         * @param name
         *            the section's name
         * @param delims
         *            an array of characters to be recognized as starters of
         *            comment lines; the first of them will be used for newly
         *            created comments
         * @throws IllegalArgumentException
         *             the section's name is illegal
         */
        public Section(String name, char[] delims) {
            this(name, delims, DEFAULT_CASE_SENSITIVITY);
        }

        /**
         * Constructs a new section, defining comment delimiters.
         *
         * @param name
         *            the section's name
         * @param delims
         *            an array of characters to be recognized as starters of
         *            comment lines; the first of them will be used for newly
         *            created comments
         * @param isCaseSensitive
         *            option names are case-sensitive if this is true
         * @throws IllegalArgumentException
         *             the section's name is illegal
         */
        public Section(String name, char[] delims, boolean isCaseSensitive) {
            if (!validName(name)) {
                throw new IllegalArgumentException("Illegal section name:" + name);
            }
            this.name = name;
            this.isCaseSensitive = isCaseSensitive;
            this.options = new HashMap<String, Option>();
            this.lines = new LinkedList<Line>();
            this.optionDelims = DEFAULT_OPTION_DELIMS;
            this.commentDelims = (delims == null ? DEFAULT_COMMENT_DELIMS : delims);
            this.optionFormat = new OptionFormat(DEFAULT_OPTION_FORMAT);
            // sorting so we can later use binary search
            this.optionDelimsSorted = new char[this.optionDelims.length];
            System.arraycopy(this.optionDelims, 0, this.optionDelimsSorted, 0, this.optionDelims.length);
            this.commentDelimsSorted = new char[this.commentDelims.length];
            System.arraycopy(this.commentDelims, 0, this.commentDelimsSorted, 0, this.commentDelims.length);
            Arrays.sort(this.optionDelimsSorted);
            Arrays.sort(this.commentDelimsSorted);
        }

        /**
         * Sets the option format for this section to the given string. Options
         * in this section will be rendered according to the given format
         * string. The string must contain <code>%s</code> three times, these
         * will be replaced with the option name, the option separator and the
         * option value in this order. Literal percentage signs must be escaped
         * by preceding them with another percentage sign (i.e., <code>%%</code>
         * corresponds to one percentage sign). The default format string is
         * <code>"%s %s %s"</code>.
         *
         * Option formats may look like format strings as supported by Java 1.5,
         * but the string is in fact parsed in a custom fashion to guarantee
         * backwards compatibility. So don't try clever stuff like using format
         * conversion types other than <code>%s</code>.
         *
         * @param formatString
         *            a format string, containing <code>%s</code> exactly three
         *            times
         * @throws IllegalArgumentException
         *             if the format string is illegal
         */
        public void setOptionFormatString(String formatString) {
            this.setOptionFormat(new OptionFormat(formatString));
        }

        /**
         * Sets the option format for this section. Options will be rendered
         * according to the given format when printed.
         *
         * @param format
         *            a compiled option format
         */
        public void setOptionFormat(OptionFormat format) {
            this.optionFormat = format;
        }

        /**
         * Returns the names of all options in this section.
         *
         * @return list of names of this section's options in original/insertion
         *         order
         */
        public List<String> optionNames() {
            List<String> optNames = new LinkedList<String>();
            Iterator<Line> it = this.lines.iterator();
            while (it.hasNext()) {
                Line line = it.next();
                if (line instanceof Option) {
                    optNames.add(((Option) line).name());
                }
            }
            return optNames;
        }

        /**
         * Checks whether a given option exists in this section.
         *
         * @param name
         *            the name of the option to test for
         * @return true if the option exists in this section
         */
        public boolean hasOption(String name) {
            return this.options.containsKey(normOption(name));
        }

        /**
         * Returns an option's value.
         *
         * @param option
         *            the name of the option
         * @return the requested option's value or <code>null</code> if no
         *         option with the specified name exists
         */
        public String get(String option) {
            String normed = normOption(option);
            if (hasOption(normed)) {
                return getOption(normed).value();
            }
            return null;
        }

        /**
         * Sets an option's value and creates the option if it doesn't exist.
         *
         * @param option
         *            the option's name
         * @param value
         *            the option's value
         * @throws IllegalArgumentException
         *             the option name is illegal, ie contains a '=' character
         *             or consists only of white space
         */
        public void set(String option, String value) {
            set(option, value, this.optionDelims[0]);
        }

        /**
         * Sets an option's value and creates the option if it doesn't exist.
         *
         * @param option
         *            the option's name
         * @param value
         *            the option's value
         * @param delim
         *            the delimiter between name and value for this option
         * @throws IllegalArgumentException
         *             the option name is illegal, ie contains a '=' character
         *             or consists only of white space
         */
        public void set(String option, String value, char delim) {
            String normed = normOption(option);
            if (hasOption(normed)) {
                getOption(normed).set(value);
            } else {
                // Option constructor might throw IllegalArgumentException
                Option opt = new Option(normed, value, delim, this.optionFormat);
                this.options.put(normed, opt);
                this.lines.add(opt);
            }
        }

        /**
         * Removes an option if it exists.
         *
         * @param option
         *            the name of the option
         * @return <code>true</code> if the option was actually removed
         */
        public boolean remove(String option) {
            String normed = normOption(option);
            if (hasOption(normed)) {
                this.lines.remove(getOption(normed));
                this.options.remove(normed);
                return true;
            } else {
                return false;
            }
        }

        /**
         * Adds a comment line to the end of this section. A comment spanning
         * several lines (ie with line breaks) will be split up, one comment
         * line for each line.
         *
         * @param comment
         *            the comment
         */
        public void addComment(String comment) {
            addComment(comment, this.commentDelims[0]);
        }

        /**
         * Adds a comment line to the end of this section. A comment spanning
         * several lines (ie with line breaks) will be split up, one comment
         * line for each line.
         *
         * @param comment
         *            the comment
         * @param delim
         *            the delimiter used to mark the start of this comment
         */
        public void addComment(String comment, char delim) {
            StringTokenizer st = new StringTokenizer(comment.trim(), NEWLINE_CHARS);
            while (st.hasMoreTokens()) {
                this.lines.add(new Comment(st.nextToken(), delim));
            }
        }

        private static final String NEWLINE_CHARS = "\n\r";

        /**
         * Adds a blank line to the end of this section.
         */
        public void addBlankLine() {
            this.lines.add(BLANK_LINE);
        }

        /**
         * Loads options from a reader into this instance. Will read from the
         * stream until it hits a section header, ie a '[' character, and resets
         * the reader to point to this character.
         *
         * @param reader
         *            where to read from
         * @throws IOException
         *             at an I/O problem
         */
        public void load(BufferedReader reader) throws IOException {
            while (reader.ready()) {
                reader.mark(NAME_MAXLENGTH);
                String line = reader.readLine().trim();

                // Check for section header
                if (line.length() > 0 && line.charAt(0) == HEADER_START) {
                    reader.reset();
                    return;
                }

                int delimIndex = -1;
                // blank line
                if (line.equals("")) {
                    this.addBlankLine();
                }
                // comment line
                else if ((delimIndex = Arrays.binarySearch(this.commentDelimsSorted, line.charAt(0))) >= 0) {
                    addComment(line.substring(1), this.commentDelimsSorted[delimIndex]);
                }
                // option line
                else {
                    delimIndex = -1;
                    int delimNum = -1;
                    int lastSpaceIndex = -1;
                    for (int i = 0, l = line.length(); i < l && delimIndex < 0; i++) {
                        delimNum = Arrays.binarySearch(this.optionDelimsSorted, line.charAt(i));
                        if (delimNum >= 0) {
                            delimIndex = i;
                        } else {
                            boolean isSpace = Arrays.binarySearch(Section.OPTION_DELIMS_WHITESPACE, line.charAt(i)) >= 0;
                            if (!isSpace && lastSpaceIndex >= 0) {
                                break;
                            } else if (isSpace) {
                                lastSpaceIndex = i;
                            }
                        }
                    }
                    // delimiter at start of line
                    if (delimIndex == 0) {
                        // XXX what's a man got to do?
                    }
                    // no delimiter found
                    else if (delimIndex < 0) {
                        if (lastSpaceIndex < 0) {
                            this.set(line, "");
                        } else {
                            this.set(line.substring(0, lastSpaceIndex), line.substring(lastSpaceIndex + 1));
                        }
                    }
                    // delimiter found
                    else {
                        this.set(line.substring(0, delimIndex), line.substring(delimIndex + 1),
                                line.charAt(delimIndex));
                    }
                }
            }
        }

        /**
         * Prints this section to a print writer.
         *
         * @param writer
         *            where to write
         * @throws IOException
         *             at an I/O problem
         */
        public void save(PrintWriter writer) throws IOException {
            Iterator<Line> it = this.lines.iterator();
            while (it.hasNext()) {
                writer.println(it.next().toString());
            }
            if (writer.checkError()) {
                throw new IOException();
            }
        }

        /**
         * Returns an actual Option instance.
         *
         * @param option
         *            the name of the option, assumed to be normed already (!)
         * @return the requested Option instance
         * @throws NullPointerException
         *             if no option with the specified name exists
         */
        private Option getOption(String name) {
            return this.options.get(name);
        }

        /**
         * Returns the bracketed header of this section as appearing in an
         * actual INI file.
         *
         * @return the section's name in brackets
         */
        private String header() {
            return HEADER_START + this.name + HEADER_END;
        }

        /**
         * Checks a string for validity as a section name. It can't contain the
         * characters '[' and ']'. An empty string or one consisting only of
         * white space isn't allowed either.
         *
         * @param name
         *            the name to validate
         * @return true if the name validates as a section name
         */
        private static boolean validName(String name) {
            if (name.trim().equals("")) {
                return false;
            }
            for (int i = 0; i < INVALID_NAME_CHARS.length; i++) {
                if (name.indexOf(INVALID_NAME_CHARS[i]) >= 0) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Normalizes an arbitrary string for use as an option name, ie makes it
         * lower-case (provided this section isn't case-sensitive) and trims
         * leading and trailing white space.
         *
         * @param name
         *            the string to be used as option name
         * @return a normalized option name
         */
        private String normOption(String name) {
            if (!this.isCaseSensitive) {
                name = name.toLowerCase();
            }
            return name.trim();
        }

    }

    private interface Line {
        public String toString();
    }

    private static final Line BLANK_LINE = new Line() {
        public String toString() {
            return "";
        }
    };

    private static class Option implements Line {

        private String name;
        private String value;
        private char separator;
        private OptionFormat format;

        private static final String ILLEGAL_VALUE_CHARS = "\n\r";

        public Option(String name, String value, char separator, OptionFormat format) {
            if (!validName(name, separator)) {
                throw new IllegalArgumentException("Illegal option name:" + name);
            }
            this.name = name;
            this.separator = separator;
            this.format = format;
            set(value);
        }

        public String name() {
            return this.name;
        }

        public String value() {
            return this.value;
        }

        public void set(String value) {
            if (value == null) {
                this.value = value;
            } else {
                StringTokenizer st = new StringTokenizer(value.trim(), ILLEGAL_VALUE_CHARS);
                StringBuffer sb = new StringBuffer();
                // XXX this might not be particularly efficient
                while (st.hasMoreTokens()) {
                    sb.append(st.nextToken());
                }
                this.value = sb.toString();
            }
        }

        public String toString() {
            return this.format.format(this.name, this.value, this.separator);
        }

        private static boolean validName(String name, char separator) {
            if (name.trim().equals("")) {
                return false;
            }
            if (name.indexOf(separator) >= 0) {
                return false;
            }
            return true;
        }

    }

    private static class Comment implements Line {

        private String comment;
        private char delimiter;

        public Comment(String comment, char delimiter) {
            this.comment = comment.trim();
            this.delimiter = delimiter;
        }

        public String toString() {
            return this.delimiter + " " + this.comment;
        }

    }

    private static class OptionFormat {

        private static final int EXPECTED_TOKENS = 4;

        private String[] formatTokens;

        public OptionFormat(String formatString) {
            this.formatTokens = this.compileFormat(formatString);
        }

        public String format(String name, String value, char separator) {
            String[] t = this.formatTokens;
            return t[0] + name + t[1] + separator + t[2] + value + t[3];
        }

        private String[] compileFormat(String formatString) {
            String[] tokens = { "", "", "", "" };
            int tokenCount = 0;
            boolean seenPercent = false;
            StringBuffer token = new StringBuffer();
            for (int i = 0; i < formatString.length(); i++) {
                switch (formatString.charAt(i)) {
                case '%':
                    if (seenPercent) {
                        token.append("%");
                        seenPercent = false;
                    } else {
                        seenPercent = true;
                    }
                    break;
                case 's':
                    if (seenPercent) {
                        if (tokenCount >= EXPECTED_TOKENS) {
                            throw new IllegalArgumentException("Illegal option format. Too many %s placeholders.");
                        }
                        tokens[tokenCount] = token.toString();
                        tokenCount++;
                        token = new StringBuffer();
                        seenPercent = false;
                    } else {
                        token.append("s");
                    }
                    break;
                default:
                    if (seenPercent) {
                        throw new IllegalArgumentException("Illegal option format. Unknown format specifier.");
                    }
                    token.append(formatString.charAt(i));
                    break;
                }
            }
            if (tokenCount != EXPECTED_TOKENS - 1) {
                throw new IllegalArgumentException("Illegal option format. Not enough %s placeholders.");
            }
            tokens[tokenCount] = token.toString();
            return tokens;
        }

    }

    /**
     * Thrown when an non-existent section is addressed.
     */
    public static class NoSuchSectionException extends RuntimeException {
        private static final long serialVersionUID = 5680119666410718328L;

        public NoSuchSectionException() {
            super();
        }

        public NoSuchSectionException(String msg) {
            super(msg);
        }
    }

}
