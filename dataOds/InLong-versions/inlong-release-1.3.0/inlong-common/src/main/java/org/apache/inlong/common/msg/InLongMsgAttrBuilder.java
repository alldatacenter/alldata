/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.common.msg;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class InLongMsgAttrBuilder {

    public enum PartitionUnit {
        DAY("d"), HOUR("h"), HALFHOUR("n"),
        QUARTER("q"), TENMINS("t"), FIVEMINS("f");
        private static final Map<String, PartitionUnit> STRING_TO_TYPE_MAP =
                new HashMap<String, PartitionUnit>();

        static {
            for (PartitionUnit type : PartitionUnit.values()) {
                STRING_TO_TYPE_MAP.put(type.value, type);
            }
        }

        private final String value;

        private PartitionUnit(String value) {
            this.value = value;
        }

        public static PartitionUnit of(String p) {
            PartitionUnit type = STRING_TO_TYPE_MAP.get(p);
            if (type == null) {
                return PartitionUnit.HOUR;
            }
            return type;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum TimeType {
        MS("#ms"), S("#s"),
        STANDARD("#")/* yyyy-MM-dd HH:mm:ss */,
        NORMAL("#n")/* yyyyMMddHH */;
        private static final Map<String, TimeType> STRING_TO_TYPE_MAP =
                new HashMap<String, TimeType>();

        static {
            for (TimeType type : TimeType.values()) {
                STRING_TO_TYPE_MAP.put(type.value, type);
            }
        }

        private final String value;

        private TimeType(String value) {
            this.value = value;
        }

        public static TimeType of(String tt) {
            TimeType type = STRING_TO_TYPE_MAP.get(tt);
            if (type == null) {
                return TimeType.STANDARD;
            }
            return type;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static class MsgAttrProtocolM0 {
        private final StringBuffer attrBuffer;
        private String id = null;
        private String t = null;
        private TimeType tt = TimeType.NORMAL;
        private PartitionUnit p = PartitionUnit.HOUR;

        public MsgAttrProtocolM0() {
            attrBuffer = new StringBuffer();
            attrBuffer.append("m=0");
        }

        public MsgAttrProtocolM0 setId(String id) {
            this.id = id;
            return this;
        }

        public MsgAttrProtocolM0 setSpliter(String s) {
            attrBuffer.append("&s=").append(s);
            return this;
        }

        public MsgAttrProtocolM0 setTime(String t) {
            this.t = t;
            return this;
        }

        public MsgAttrProtocolM0 setTime(long t) {
            return this.setTime(String.valueOf(t));
        }

        public MsgAttrProtocolM0 setTimeType(String tt) {
            this.tt = TimeType.of(tt);
            return this;
        }

        public MsgAttrProtocolM0 setTimeType(TimeType tt) {
            this.tt = tt;
            return this;
        }

        public MsgAttrProtocolM0 setPartitionUnit(String p) {
            this.p = PartitionUnit.of(p);
            return this;
        }

        public MsgAttrProtocolM0 setPartitionUnit(PartitionUnit p) {
            this.p = p;
            return this;
        }

        /**
         * buildAttr
         * @return
         *
         * @throws Exception
         */
        public String buildAttr() throws Exception {
            if (id == null) {
                throw new Exception("id is null");
            }

            if (t == null) {
                throw new Exception("t is null");
            }

            attrBuffer.append("&iname=").append(id);

            Date d = transData(this.tt, t);
            String tstr = null;
            if (this.p == PartitionUnit.DAY) {
                SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
                tstr = f.format(d);
            } else if (this.p == PartitionUnit.HOUR) {
                SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHH");
                tstr = f.format(d);
            } else if (this.p == PartitionUnit.QUARTER) {
                int idx =
                        (int) ((d.getTime() % (60L * 60 * 1000)) / (15L * 60 * 1000));
                SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHH");
                tstr = f.format(d) + "q" + idx;
            }

            return attrBuffer.append("&t=").append(tstr).toString();
        }
    }

    public static class MsgAttrProtocolM100 {

        private final StringBuffer attrBuffer;
        private String id = null;
        private String t = null;

        private int idp = -1;
        private int tp = -1;
        private TimeType tt = null;
        private PartitionUnit p = null;

        public MsgAttrProtocolM100() {
            attrBuffer = new StringBuffer();
            attrBuffer.append("m=100");
        }

        public MsgAttrProtocolM100 setSpliter(String s) {
            attrBuffer.append("&s=").append(s);
            return this;
        }

        public MsgAttrProtocolM100 setId(String id) {
            this.id = id;
            return this;
        }

        public MsgAttrProtocolM100 setTime(String t) {
            this.t = t;
            return this;
        }

        public MsgAttrProtocolM100 setTime(long t) {
            return this.setTime(String.valueOf(t));
        }

        public MsgAttrProtocolM100 setIdPos(int idp) {
            this.idp = idp;
            return this;
        }

        public MsgAttrProtocolM100 setTimePos(int tp) {
            this.tp = tp;
            return this;
        }

        public MsgAttrProtocolM100 setTimeType(String tt) {
            return this.setTimeType(TimeType.of(tt));
        }

        public MsgAttrProtocolM100 setTimeType(TimeType tt) {
            this.tt = tt;
            return this;
        }

        public MsgAttrProtocolM100 setPartitionUnit(String p) {
            return this.setPartitionUnit(PartitionUnit.of(p));
        }

        public MsgAttrProtocolM100 setPartitionUnit(PartitionUnit p) {
            this.p = p;
            return this;
        }

        /**
         * buildAttr
         * @return
         *
         * @throws Exception
         */
        public String buildAttr() throws Exception {
            // #lizard forgives
            if (id != null) {
                attrBuffer.append("&iname=").append(id);
            } else if (idp >= 0) {
                attrBuffer.append("&idp=").append(idp);
            }
            if (t != null) {
                String tstr = null;
                if (tt != null && tt == TimeType.NORMAL) {
                    if (makeSureTimeNormal(t)) {
                        tstr = t;
                    }
                } else {
                    if (this.p == null) {
                        this.p = PartitionUnit.HOUR;
                    }
                    Date d = transData(tt, t);
                    if (this.p == PartitionUnit.DAY) {
                        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
                        tstr = f.format(d);
                    } else if (this.p == PartitionUnit.HOUR) {
                        SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHH");
                        tstr = f.format(d);
                    } else if (this.p == PartitionUnit.HALFHOUR) {
                        int idx =
                                (int) ((d.getTime() % (60L * 60 * 1000)) / (30L * 60 * 1000));
                        SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHH");
                        tstr = f.format(d) + "n" + idx;
                    } else if (this.p == PartitionUnit.QUARTER) {
                        int idx =
                                (int) ((d.getTime() % (60L * 60 * 1000)) / (15L * 60 * 1000));
                        SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHH");
                        tstr = f.format(d) + "q" + idx;
                    } else if (this.p == PartitionUnit.TENMINS) {
                        int idx =
                                (int) ((d.getTime() % (60L * 60 * 1000)) / (10L * 60 * 1000));
                        SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHH");
                        tstr = f.format(d) + "t" + idx;
                    } else if (this.p == PartitionUnit.FIVEMINS) {
                        int idx =
                                (int) ((d.getTime() % (60L * 60 * 1000)) / (5L * 60 * 1000));
                        SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHH");
                        tstr = f.format(d) + "f" + idx;
                    }
                }

                if (tstr != null) {
                    attrBuffer.append("&t=").append(tstr);
                }

            } else if (tp >= 0) {
                attrBuffer.append("&tp=").append(tp);
                if (this.tt != null) {
                    attrBuffer.append("&tt=").append(tt);
                }
                if (this.p != null) {
                    attrBuffer.append("&p=").append(p);
                }
            }
            return attrBuffer.toString();
        }

        private boolean makeSureTimeNormal(String time) {
            int len = time.length();
            return len == 8 || len == 10
                    || (len == 12 && time.charAt(10) == 'p');
        }

    }

    public static MsgAttrProtocolM0 getProtocolM0() {
        return new MsgAttrProtocolM0();
    }

    public static MsgAttrProtocolM100 getProtocolM100() {
        return new MsgAttrProtocolM100();
    }

    /**
     *  main
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        SimpleDateFormat f =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat f1 =
                new SimpleDateFormat("yyyyMMddHH");

        System.out.println(InLongMsgAttrBuilder.getProtocolM0()
                .setId("interfaceid").setTimeType(TimeType.S)
                .setTime(String.valueOf(System.currentTimeMillis() / 1000))
                .buildAttr());
        System.out.println(InLongMsgAttrBuilder.getProtocolM0()
                .setId("interfaceid").setTime(System.currentTimeMillis())
                .setTimeType(TimeType.MS).buildAttr());
        System.out.println(InLongMsgAttrBuilder.getProtocolM0()
                .setId("interfaceid")
                .setTime(f1.format(new Date(System.currentTimeMillis())))
                .buildAttr());
        System.out.println(InLongMsgAttrBuilder.getProtocolM0()
                .setId("interfaceid")
                .setTime(f.format(new Date(System.currentTimeMillis())))
                .setTimeType(TimeType.STANDARD).buildAttr());
        System.out.println(InLongMsgAttrBuilder.getProtocolM0()
                .setId("interfaceid")
                .setTime(f1.format(new Date(System.currentTimeMillis())))
                .setTimeType(TimeType.NORMAL).buildAttr());
        System.out.println(InLongMsgAttrBuilder.getProtocolM0()
                .setId("interfaceid").setTimeType(TimeType.S)
                .setTime(String.valueOf(System.currentTimeMillis() / 1000))
                .setPartitionUnit(PartitionUnit.DAY).buildAttr());
        System.out.println(InLongMsgAttrBuilder.getProtocolM0()
                .setId("interfaceid").setTimeType(TimeType.S)
                .setTime(String.valueOf(System.currentTimeMillis() / 1000))
                .setPartitionUnit(PartitionUnit.HOUR).buildAttr());
        System.out.println(InLongMsgAttrBuilder.getProtocolM0()
                .setId("interfaceid").setTimeType(TimeType.S)
                .setTime(String.valueOf(System.currentTimeMillis() / 1000))
                .setPartitionUnit(PartitionUnit.QUARTER).buildAttr());
        System.out.println();

        System.out.print(InLongMsgAttrBuilder.getProtocolM100().buildAttr());
        System.out.println("\t\t\t\t\t\t// ---- all the param is "
                + "default : s=\\t, idp=0, tp=1, tt=#ms, p=h ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100().setSpliter(",")
                .buildAttr());
        System.out.println("\t\t\t\t\t// ---- : idp=0, tp=1, tt=#ms, p=h ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100().setSpliter(",")
                .setIdPos(0).buildAttr());
        System.out.println("\t\t\t\t\t// ---- : tp=1, tt=#ms, p=h ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100().setSpliter(",")
                .setTimePos(1).buildAttr());
        System.out.println("\t\t\t\t\t// ---- : idp=0, tt=#ms, p=h ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100().setSpliter(",")
                .setIdPos(0).setTimePos(1).buildAttr());
        System.out.println("\t\t\t\t// ---- : tt=#ms, p=h ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100().setSpliter(",")
                .setIdPos(0).setTimePos(1).setTimeType(TimeType.S).buildAttr());
        System.out.println("\t\t\t// ---- : p=h ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100().setSpliter(",")
                .setIdPos(0).setTimePos(1)
                .setPartitionUnit(PartitionUnit.QUARTER).buildAttr());
        System.out.println("\t\t\t// ---- : tt=#s ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100().setSpliter(",")
                .setIdPos(0).setTimePos(1).setTimeType(TimeType.MS)
                .setPartitionUnit(PartitionUnit.QUARTER).buildAttr());
        System.out.println("\t\t\t// ---- : all ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100()
                .setId("interfaceid").setSpliter(",").setIdPos(0).setTimePos(1)
                .setTimeType(TimeType.MS)
                .setPartitionUnit(PartitionUnit.QUARTER).buildAttr());
        System.out.println("\t// ---- : id is set so idp is ignored ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100().setSpliter(",")
                .setIdPos(0).setTimePos(1).setTime(System.currentTimeMillis())
                .setTimeType(TimeType.MS)
                .setPartitionUnit(PartitionUnit.QUARTER).buildAttr());
        System.out.println("\t\t\t// ---- : t is set so tp is ignored ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100()
                .setId("interfaceid").setSpliter(",").setIdPos(0).setTimePos(1)
                .setTime(System.currentTimeMillis()).setTimeType(TimeType.MS)
                .setPartitionUnit(PartitionUnit.QUARTER).buildAttr());
        System.out
                .println("\t\t// ---- : id and t are all set so idpos and tp are all ignored ");

        System.out.print(InLongMsgAttrBuilder.getProtocolM100()
                .setId("interfaceid").setSpliter(",").setIdPos(0).setTimePos(1)
                .setTime(f1.format(new Date(System.currentTimeMillis())))
                .setTimeType(TimeType.NORMAL)
                .setPartitionUnit(PartitionUnit.QUARTER).buildAttr());
        System.out.println("\t\t// ---- : TimeType.NORMAL ");

        System.out
                .println("\nAttention !!!! m=0 is contained by m=100, so just use m=100");

        // long time = System.currentTimeMillis();
        // byte[] data0 = ("id," + time + ",other,data").getBytes();
        // String attr = InLongMsgProtocolFactory.getProtololM100().setSpliter(",")
        // .setIdPos(0).setTimePos(1).setTimeType(TimeType.MS)
        // .setPartitionUnit(PartitionUnit.QUARTER).buildAttr();
        // InLongMsg inLongMsg = InLongMsg.newInLongMsg();
        // inLongMsg.addMsg(attr, data0);
        // byte[] result = inLongMsg.buildArray();
        // System.out.println(new String(result));
        // attr = InLongMsgProtocolFactory.getProtololM0().setSpliter(",")
        // .setTime(System.currentTimeMillis()).setTimeType(TimeType.MS)
        // .setPartitionUnit(PartitionUnit.QUARTER).buildAttr();
        // inLongMsg = InLongMsg.newInLongMsg();
        // inLongMsg.addMsg(attr, data0);
        // result = inLongMsg.buildArray();
        // System.out.println(new String(result));
        System.out.print(InLongMsgAttrBuilder.getProtocolM100().setSpliter(",")
                .setIdPos(0).setTimePos(1)

                .setTimeType(TimeType.STANDARD)
                .setPartitionUnit(PartitionUnit.QUARTER).buildAttr());

    }

    private static Date transData(TimeType tt, String timeStr) throws Exception {
        Date d = null;
        if (tt == TimeType.MS) {
            d = new Date(Long.valueOf(timeStr));
        } else if (tt == TimeType.S) {
            d = new Date(Long.valueOf(timeStr) * 1000);
        } else if (tt == TimeType.STANDARD) {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            d = f.parse(timeStr);
        } else if (tt == TimeType.NORMAL) {
            SimpleDateFormat f = new SimpleDateFormat("yyyyMMddHH");
            d = f.parse(timeStr);
        }
        return d;
    }
}
