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
package com.qlangtech.tis.extension.util;


import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public class VersionNumber implements Comparable<VersionNumber> {

    private static final Pattern SNAPSHOT = Pattern.compile("^.*((?:-\\d{8}\\.\\d{6}-\\d+)|-SNAPSHOT)( \\(.*\\))?$");

    private String value;

    private String snapshot;

    private String canonical;

    private VersionNumber.ListItem items;

    public static final Comparator<VersionNumber> DESCENDING = new Comparator<VersionNumber>() {

        public int compare(VersionNumber o1, VersionNumber o2) {
            return o2.compareTo(o1);
        }
    };

    public VersionNumber(String version) {
        this.parseVersion(version);
    }

    private void parseVersion(String version) {
        this.value = version;
        this.items = new VersionNumber.ListItem();
        Matcher matcher = SNAPSHOT.matcher(version);
        if (matcher.matches()) {
            this.snapshot = matcher.group(1);
            version = version.substring(0, matcher.start(1)) + "-SNAPSHOT";
        }
        version = version.toLowerCase(Locale.ENGLISH);
        VersionNumber.ListItem list = this.items;
        Stack<VersionNumber.Item> stack = new Stack();
        stack.push(list);
        boolean isDigit = false;
        int startIndex = 0;
        for (int i = 0; i < version.length(); ++i) {
            char c = version.charAt(i);
            if (c == '.') {
                if (i == startIndex) {
                    list.add(VersionNumber.IntegerItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;
            } else if (c == '-') {
                if (i == startIndex) {
                    list.add(VersionNumber.IntegerItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;
                if (isDigit) {
                    list.normalize();
                    if (i + 1 < version.length() && Character.isDigit(version.charAt(i + 1))) {
                        list.add(list = new VersionNumber.ListItem());
                        stack.push(list);
                    }
                }
            } else if (c == '*') {
                list.add(new VersionNumber.WildCardItem());
                startIndex = i + 1;
            } else if (Character.isDigit(c)) {
                if (!isDigit && i > startIndex) {
                    list.add(new VersionNumber.StringItem(version.substring(startIndex, i), true));
                    startIndex = i;
                }
                isDigit = true;
            } else if (Character.isWhitespace(c)) {
                if (i > startIndex) {
                    if (isDigit) {
                        list.add(parseItem(true, version.substring(startIndex, i)));
                    } else {
                        list.add(new VersionNumber.StringItem(version.substring(startIndex, i), true));
                    }
                    startIndex = i;
                }
                isDigit = false;
            } else {
                if (isDigit && i > startIndex) {
                    list.add(parseItem(true, version.substring(startIndex, i)));
                    startIndex = i;
                }
                isDigit = false;
            }
        }
        if (version.length() > startIndex) {
            list.add(parseItem(isDigit, version.substring(startIndex)));
        }
        while (!stack.isEmpty()) {
            list = (VersionNumber.ListItem) stack.pop();
            list.normalize();
        }
        this.canonical = this.items.toString();
    }

    private static VersionNumber.Item parseItem(boolean isDigit, String buf) {
        return (VersionNumber.Item) (isDigit ? new VersionNumber.IntegerItem(buf) : new VersionNumber.StringItem(buf, false));
    }

    public int compareTo(VersionNumber o) {
        int result = this.items.compareTo(o.items);
        if (result != 0) {
            return result;
        } else if (this.snapshot == null) {
            return o.snapshot == null ? 0 : -1;
        } else if (o.snapshot == null) {
            return 1;
        } else if (!"-SNAPSHOT".equals(this.snapshot) && !"-SNAPSHOT".equals(o.snapshot)) {
            result = this.snapshot.substring(1, 16).compareTo(o.snapshot.substring(1, 16));
            if (result != 0) {
                return result;
            } else {
                int i1 = Integer.parseInt(this.snapshot.substring(17));
                int i2 = Integer.parseInt(o.snapshot.substring(17));
                return i1 < i2 ? -1 : (i1 == i2 ? 0 : 1);
            }
        } else {
            return 0;
        }
    }

    public String toString() {
        return this.value;
    }

    public boolean equals(Object o) {
        if (!(o instanceof VersionNumber)) {
            return false;
        } else {
            VersionNumber that = (VersionNumber) o;
            if (!this.canonical.equals(that.canonical)) {
                return false;
            } else if (this.snapshot == null) {
                return that.snapshot == null;
            } else {
                return !"-SNAPSHOT".equals(this.snapshot) && !"-SNAPSHOT".equals(that.snapshot) ? this.snapshot.equals(that.snapshot) : true;
            }
        }
    }

    public int hashCode() {
        return this.canonical.hashCode();
    }

    public boolean isOlderThan(VersionNumber rhs) {
        return this.compareTo(rhs) < 0;
    }

    public boolean isNewerThan(VersionNumber rhs) {
        return this.compareTo(rhs) > 0;
    }

    public boolean isOlderThanOrEqualTo(VersionNumber rhs) {
        return this.compareTo(rhs) <= 0;
    }

    public boolean isNewerThanOrEqualTo(VersionNumber rhs) {
        return this.compareTo(rhs) >= 0;
    }

    /**
     * @deprecated
     */
    public int digit(int idx) {
        Iterator i = this.items.iterator();
        VersionNumber.Item item;
        for (item = (VersionNumber.Item) i.next(); idx > 0 && i.hasNext(); i.next()) {
            if (item instanceof VersionNumber.IntegerItem) {
                --idx;
            }
        }
        return ((VersionNumber.IntegerItem) item).value.intValue();
    }

    public int getDigitAt(int idx) {
        if (idx < 0) {
            return -1;
        } else {
            Iterator it = this.items.iterator();
            int i = 0;
            VersionNumber.Item item;
            for (item = null; i <= idx && it.hasNext(); ++i) {
                item = (VersionNumber.Item) it.next();
                if (!(item instanceof VersionNumber.IntegerItem)) {
                    return -1;
                }
            }
            return idx - i >= 0 ? -1 : ((VersionNumber.IntegerItem) item).value.intValue();
        }
    }

    private static class ListItem extends ArrayList<VersionNumber.Item> implements VersionNumber.Item {

        private ListItem() {
        }

        public int getType() {
            return 2;
        }

        public boolean isNull() {
            return this.size() == 0;
        }

        void normalize() {
            ListIterator iterator = this.listIterator(this.size());
            while (iterator.hasPrevious()) {
                VersionNumber.Item item = (VersionNumber.Item) iterator.previous();
                if (!item.isNull()) {
                    break;
                }
                iterator.remove();
            }
        }

        public int compareTo(VersionNumber.Item item) {
            if (item == null) {
                if (this.size() == 0) {
                    return 0;
                } else {
                    VersionNumber.Item first = (VersionNumber.Item) this.get(0);
                    return first.compareTo((VersionNumber.Item) null);
                }
            } else {
                switch(item.getType()) {
                    case 0:
                        return -1;
                    case 1:
                        return 1;
                    case 2:
                        Iterator left = this.iterator();
                        Iterator right = ((VersionNumber.ListItem) item).iterator();
                        int result;
                        do {
                            if (!left.hasNext() && !right.hasNext()) {
                                return 0;
                            }
                            VersionNumber.Item l = left.hasNext() ? (VersionNumber.Item) left.next() : null;
                            VersionNumber.Item r = right.hasNext() ? (VersionNumber.Item) right.next() : null;
                            if (l == null) {
                                if (r == null) {
                                    result = 0;
                                } else {
                                    result = -1 * r.compareTo((VersionNumber.Item) null);
                                }
                            } else {
                                result = l.compareTo(r);
                            }
                        } while (result == 0);
                        return result;
                    case 3:
                        return -1;
                    default:
                        throw new RuntimeException("invalid item: " + item.getClass());
                }
            }
        }

        public String toString() {
            StringBuilder buffer = new StringBuilder("(");
            Iterator iter = this.iterator();
            while (iter.hasNext()) {
                buffer.append(iter.next());
                if (iter.hasNext()) {
                    buffer.append(',');
                }
            }
            buffer.append(')');
            return buffer.toString();
        }
    }

    private static class StringItem implements VersionNumber.Item {

        private static final String[] QUALIFIERS = new String[] { "snapshot", "alpha", "beta", "milestone", "rc", "", "sp" };

        private static final List<String> _QUALIFIERS;

        private static final Properties ALIASES;

        private static String RELEASE_VERSION_INDEX;

        private String value;

        public StringItem(String value, boolean followedByDigit) {
            if (followedByDigit && value.length() == 1) {
                switch(value.charAt(0)) {
                    case 'a':
                        value = "alpha";
                        break;
                    case 'b':
                        value = "beta";
                        break;
                    case 'm':
                        value = "milestone";
                }
            }
            this.value = ALIASES.getProperty(value, value);
        }

        public int getType() {
            return 1;
        }

        public boolean isNull() {
            return comparableQualifier(this.value).compareTo(RELEASE_VERSION_INDEX) == 0;
        }

        public static String comparableQualifier(String qualifier) {
            int i = _QUALIFIERS.indexOf(qualifier);
            return i == -1 ? _QUALIFIERS.size() + "-" + qualifier : String.valueOf(i);
        }

        public int compareTo(VersionNumber.Item item) {
            if (item == null) {
                return comparableQualifier(this.value).compareTo(RELEASE_VERSION_INDEX);
            } else {
                switch(item.getType()) {
                    case 0:
                        return -1;
                    case 1:
                        return comparableQualifier(this.value).compareTo(comparableQualifier(((VersionNumber.StringItem) item).value));
                    case 2:
                        return -1;
                    case 3:
                        return -1;
                    default:
                        throw new RuntimeException("invalid item: " + item.getClass());
                }
            }
        }

        public String toString() {
            return this.value;
        }

        static {
            _QUALIFIERS = Arrays.asList(QUALIFIERS);
            ALIASES = new Properties();
            ALIASES.put("ga", "");
            ALIASES.put("final", "");
            ALIASES.put("cr", "rc");
            ALIASES.put("ea", "rc");
            RELEASE_VERSION_INDEX = String.valueOf(_QUALIFIERS.indexOf(""));
        }
    }

    private static class IntegerItem implements VersionNumber.Item {

        private static final BigInteger BigInteger_ZERO = new BigInteger("0");

        private final BigInteger value;

        public static final VersionNumber.IntegerItem ZERO = new VersionNumber.IntegerItem();

        private IntegerItem() {
            this.value = BigInteger_ZERO;
        }

        public IntegerItem(String str) {
            this.value = new BigInteger(str);
        }

        public int getType() {
            return 0;
        }

        public boolean isNull() {
            return BigInteger_ZERO.equals(this.value);
        }

        public int compareTo(VersionNumber.Item item) {
            if (item == null) {
                return BigInteger_ZERO.equals(this.value) ? 0 : 1;
            } else {
                switch(item.getType()) {
                    case 0:
                        return this.value.compareTo(((VersionNumber.IntegerItem) item).value);
                    case 1:
                        return 1;
                    case 2:
                        return 1;
                    case 3:
                        return 0;
                    default:
                        throw new RuntimeException("invalid item: " + item.getClass());
                }
            }
        }

        public String toString() {
            return this.value.toString();
        }
    }

    private static class WildCardItem implements VersionNumber.Item {

        private WildCardItem() {
        }

        public int compareTo(VersionNumber.Item item) {
            if (item == null) {
                return 1;
            } else {
                switch(item.getType()) {
                    case 0:
                    case 1:
                    case 2:
                        return 1;
                    case 3:
                        return 0;
                    default:
                        return 1;
                }
            }
        }

        public int getType() {
            return 3;
        }

        public boolean isNull() {
            return false;
        }

        public String toString() {
            return "*";
        }
    }

    private interface Item {

        int INTEGER_ITEM = 0;

        int STRING_ITEM = 1;

        int LIST_ITEM = 2;

        int WILDCARD_ITEM = 3;

        int compareTo(VersionNumber.Item var1);

        int getType();

        boolean isNull();
    }
}
