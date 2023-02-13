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
package scala.tools.scala_maven_executions;

import org.apache.commons.lang.StringUtils;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class MainHelper {

    static final String argFilePrefix = "scala-maven-";

    static final String argFileSuffix = ".args";

    public static String toMultiPath(Set<String> paths) {
        return StringUtils.join(paths.iterator(), File.pathSeparator);
    }

    public static String toMultiPath(String[] paths) {
        return StringUtils.join(paths, File.pathSeparator);
    }

    // public static String[] findFiles(File dir, String[] includes, String[] excludes) {
    // DirectoryScanner scanner = new DirectoryScanner();
    // scanner.setBasedir(dir);
    // scanner.setIncludes(includes);
    // scanner.setExcludes(excludes);
    // scanner.addDefaultExcludes();
    // scanner.scan();
    // return scanner.getIncludedFiles();
    // }
    public static String toClasspathString(ClassLoader cl) {
        StringBuilder back = new StringBuilder();
        List<String> cps = new LinkedList<>();
        appendUrlToClasspathCollection(cl, cps);
        for (String cp : cps) {
            if (back.length() != 0) {
                back.append(File.pathSeparatorChar);
            }
            back.append(cp);
        }
        return back.toString();
    }

    private static void appendUrlToClasspathCollection(ClassLoader cl, Collection<String> classpath) {
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        while (cl != null) {
            if (cl instanceof URLClassLoader) {
                URLClassLoader ucl = (URLClassLoader) cl;
                URL[] urls = ucl.getURLs();
                for (URL url : urls) {
                    classpath.add(url.getFile());
                }
            }
            cl = cl.getParent();
        }
    }

    /**
     * Escapes arguments as necessary so the StringTokenizer for scala arguments
     * pulls in filenames with spaces correctly.
     *
     * @param arg
     * @return
     */
    private static String escapeArgumentForScalacArgumentFile(String arg) {
        if (arg.matches(".*\\s.*")) {
            return '"' + arg + '"';
        }
        return arg;
    }

    /**
     * UnEscapes arguments as necessary so the StringTokenizer for scala arguments
     * pulls in filenames with spaces correctly.
     *
     * @param arg
     * @return
     */
    private static String unescapeArgumentForScalacArgumentFile(String arg) {
        if (arg.charAt(0) == '"' && arg.charAt(arg.length() - 1) == '"') {
            return arg.substring(1, arg.length() - 1);
        }
        return arg;
    }

    /**
     * Creates a file containing all the arguments. This file has a very simple
     * format of argument (white-space argument).
     *
     * @return
     * @throws IOException
     */
    static File createArgFile(List<String> args) throws IOException {
        final File argFile = File.createTempFile(argFilePrefix, argFileSuffix);
        // argFile.deleteOnExit();
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(argFile)))) {
            for (String arg : args) {
                out.println(escapeArgumentForScalacArgumentFile(arg));
            }
        }
        return argFile;
    }

    /**
     * Creates a file containing all the arguments. This file has a very simple
     * format of argument (white-space argument).
     *
     * @return
     * @throws IOException
     */
    static List<String> readArgFile(File argFile) throws IOException {
        ArrayList<String> back = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(argFile))) {
            String line;
            while ((line = in.readLine()) != null) {
                back.add(unescapeArgumentForScalacArgumentFile(line));
            }
        }
        return back;
    }

    /**
     * Runs the main method of a java class
     */
    static void runMain(String mainClassName, List<String> args, ClassLoader cl) throws Exception {
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        Class<?> mainClass = cl.loadClass(mainClassName);
        // Method mainMethod = mainClass.getMethod("main", String[].class);
        // process
        Method mainMethod = mainClass.getMethod("process", String[].class);
        int mods = mainMethod.getModifiers();
        if (!Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
            throw new NoSuchMethodException("process");
        }
        String[] argArray = args.toArray(new String[] {});
        // TODO - Redirect System.in System.err and System.out
        mainMethod.invoke(null, new Object[] { argArray });
    }

    static String locateJar(Class<?> c) throws Exception {
        final URL location;
        final String classLocation = c.getName().replace('.', '/') + ".class";
        final ClassLoader loader = c.getClassLoader();
        if (loader == null) {
            location = ClassLoader.getSystemResource(classLocation);
        } else {
            location = loader.getResource(classLocation);
        }
        if (location != null) {
            Pattern p = Pattern.compile("^.*file:(.*)!.*$");
            Matcher m = p.matcher(location.toString());
            if (m.find()) {
                return URLDecoder.decode(m.group(1), "UTF-8");
            }
            throw new ClassNotFoundException("Cannot parse location of '" + location + "'.  Probably not loaded from a Jar");
        }
        throw new ClassNotFoundException("Cannot find class '" + c.getName() + " using the classloader");
    }
}
