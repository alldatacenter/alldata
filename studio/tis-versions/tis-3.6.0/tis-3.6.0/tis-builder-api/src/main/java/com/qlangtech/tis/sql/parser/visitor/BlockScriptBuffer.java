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

package com.qlangtech.tis.sql.parser.visitor;

import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-19 22:43
 **/
public class BlockScriptBuffer implements IBlockToString {

    public static final int INDENT_STEP = 4;
    private static final Pattern PATTERN_LAST_RETURN = Pattern.compile("\n\\s*$");
    private final List<Object> format = new ArrayList<>();
    private int indent;
    private boolean lastReturnChar = false;

    public BlockScriptBuffer() {
        this(0);
    }

    public BlockScriptBuffer(int indent) {
        this.indent = indent;
    }

    private void addIndent() {
        this.indent += INDENT_STEP;
    }

    private void decreaseIndent() {
        this.indent -= INDENT_STEP;
    }

    public BlockScriptBuffer methodBody(Object method, IFuncFormatCall call) {
        return methodBody(true, method, call);
    }


    public BlockScriptBuffer methodBody(boolean returnLine, Object method, IFuncFormatCall call) {
        return blockBody(returnLine, new String[]{"{", "}"}, method, call);
    }

    public BlockScriptBuffer block(Object method, IFuncFormatCall call) {
        return block(false, method, call);
    }

    public BlockScriptBuffer block(boolean returnLine, Object method, IFuncFormatCall call) {
        return blockBody(returnLine, new String[]{"(", ")"}, method, call);
    }

    public void buildRowMapTraverseLiteria(EntityName entity, IFuncFormatCall call) {
        this.methodBody("for ( ( r:" + EntityName.ROW_MAP_CLASS_NAME + ") <- " + entity.entities() + ".asScala)", call);
    }

    private BlockScriptBuffer blockBody(boolean returnLine, String[] brackets, Object method, IFuncFormatCall call) {
        if (returnLine) {
            this.returnLine();
            this.appendIndent();
        }
        this.append(method).append(brackets[0]).returnLine();
        this.addIndent();
        try {
            call.execute(this);
            this.returnLine();
        } finally {
            this.decreaseIndent();
        }
        this.appendIndent().append(brackets[1]);
        if (returnLine) {
            // this.returnLine();
            this.returnLine();
        }
        return this;
    }

    public BlockScriptBuffer append(Object val) {
        if (val instanceof String) {
            Matcher matcher = PATTERN_LAST_RETURN.matcher((String) val);
            this.lastReturnChar = matcher.find();
        } else {
            this.lastReturnChar = false;
        }
        format.add(val);
        return this;
    }

    public BlockScriptBuffer appendLine(Object val) {
        return startLine(val).appendIndent();
    }

    public BlockScriptBuffer appendLine(String val, Object... params) {
        String[] p = new String[params.length];
        Object o = null;
        for (int i = 0; i < params.length; i++) {
            o = params[i];
            if (o instanceof String) {
                p[i] = "\"" + o + "\"";
            } else {
                p[i] = String.valueOf(o);
            }
        }
        return startLine(String.format(val, p)).appendIndent();
    }

    public BlockScriptBuffer startLine(Object val) {
        if (!lastReturnChar) {
            this.returnLine();
        }
        appendIndent();
        return this.append(val);
    }

    private BlockScriptBuffer appendIndent() {
        if (indent > 0) {
            for (int i = 0; i < indent; i++) {
                format.add(" ");
            }
        }
        return this;
    }

    public BlockScriptBuffer append(long val) {
        // appendIndent();
        format.add(val);
        return this;
    }

    public BlockScriptBuffer returnLine() {
        this.append("\n");
        return this;
    }

    @Override
    public String toString() {
        StringBuffer buffer = getContent();
        return buffer.toString();
    }

    public StringBuffer getContent() {
        StringBuffer buffer = new StringBuffer();
        for (Object o : format) {
            buffer.append(o);
        }
        return buffer;
    }

    public interface IFuncFormatCall {

        public void execute(BlockScriptBuffer f);
    }

}
