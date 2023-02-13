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
package com.qlangtech.tis.db.parser;

import com.qlangtech.tis.db.parser.ScannerPatterns.TokenTypes;
import com.qlangtech.tis.plugin.ds.DBConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DBConfigParser {

    private final TokenBuffer tokenBuffer;

    public final DBConfig dbConfigResult = new DBConfig();

    // 反序列化的时候需要将db描述信息通过token再拼接起来
    public final StringBuffer hostDesc = new StringBuffer();

    public DBConfigParser(TokenBuffer tokenBuffer) {
        this.tokenBuffer = tokenBuffer;
        this.dbConfigResult.setHostDesc(hostDesc);
    }

    /**
     * Parse the dbnode description to struct Object
     *
     * @param dbName
     * @param dbNodeDesc
     * @return
     */
    public static Map<String, List<String>> parseDBEnum(String dbName, String dbNodeDesc) {
        if (StringUtils.isEmpty(dbNodeDesc)) {
            throw new IllegalArgumentException("param dbNodeDesc can not be null");
        }
        if (StringUtils.isEmpty(dbName)) {
            throw new IllegalArgumentException("param dbName can not be null");
        }
        try {
            DBTokenizer tokenizer = new DBTokenizer(ScannerPatterns.HOST_KEY + ":" + dbNodeDesc);
            tokenizer.parse();
            DBConfigParser parser = new DBConfigParser(tokenizer.getTokenBuffer());
            parser.dbConfigResult.setName(dbName);
            parser.parseHostDesc();
            DBConfig db = parser.dbConfigResult;
            return db.getDbEnum();
        } catch (Throwable e) {
            throw new RuntimeException("dbNodeDesc:" + dbNodeDesc, e);
        }
    }


    public static void main(String[] args) throws Exception {
        File f = new File("./db_config.txt");
        String content = FileUtils.readFileToString(f, "utf8");
        // System.out.println(content);
        DBTokenizer tokenizer = new DBTokenizer(content);
        tokenizer.parse();
        // for (Token t : ) {
        // System.out.println(t.getContent() + " "
        // + t.getToken());
        // }
        DBConfigParser parser = new DBConfigParser(tokenizer.getTokenBuffer());
        DBConfig db = parser.startParser();
        System.out.println("hostDesc:" + parser.hostDesc);
        System.out.println("type:" + db.getDbType());
        System.out.println("name:" + db.getName());
//        System.out.println("getPassword:" + db.getPassword());
//        System.out.println("getPort:" + db.getPort());
//        System.out.println("UserName:" + db.getUserName());
        StringBuffer dbdesc = new StringBuffer();
        for (Map.Entry<String, List<String>> e : db.getDbEnum().entrySet()) {
            dbdesc.append(e.getKey()).append(":");
            for (String dbName : e.getValue()) {
                dbdesc.append(dbName).append(",");
            }
            dbdesc.append("\n");
        }
        System.out.println(dbdesc.toString());
    }

    public DBConfig startParser() {
        if (dbConfig()) {
        } else {
            throw new IllegalStateException("parse dbconfig file falid");
        }
        return dbConfigResult;
    }

    private boolean dbConfig() {
        boolean parseSuccess = false;
        if (mysqlBlock()) {
            parseSuccess = true;
        }
        return parseSuccess;
    }

    public boolean parseHostDesc() {
        boolean parseSuccess = true;
        // final int save = tokenBuffer.getCurrentPosition();
        Token t = tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_HOST_DESC)) {
            // hostDesc.append(t.getContent());
            tokenBuffer.popToken();
            t = tokenBuffer.nextToken();
            if (t.isTokenType(TokenTypes.TT_COLON)) {
                // hostDesc.append(t.getContent());
                tokenBuffer.popToken();
                if (dbHostEnum()) {
                    // parseSuccess = true;
                } else {
                    parseSuccess = false;
                }
            } else {
                parseSuccess = false;
            }
        } else {
            parseSuccess = false;
        }
        return parseSuccess;
    }

    /**
     * host:127.0.0.1[00-31],127.0.0.2[32-63],127.0.0.3,127.0.0.4[9],baisui.com[
     * 0-9] <br>
     * username:root <br>
     * password:root@123<br>
     * 解析db的主要配置项
     *
     * @return
     */
    private boolean dbPrimaryInfo() {
        final int save = tokenBuffer.getCurrentPosition();
        boolean parseSuccess = this.parseHostDesc();

        Token t = tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_USERNAME)) {
            tokenBuffer.popToken();
            t = tokenBuffer.nextToken();
            if (t.isTokenType(TokenTypes.TT_COLON)) {
                tokenBuffer.popToken();
                t = tokenBuffer.nextToken();
                if (t.isTokenType(TokenTypes.TT_IDENTIFIER)) {
                    tokenBuffer.popToken();
                    //  this.dbConfigResult.setUserName(t.getContent());
                } else {
                    parseSuccess = false;
                }
            } else {
                parseSuccess = false;
            }
        } else {
            parseSuccess = false;
        }
        // password
        t = tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_PASSWORD)) {
            tokenBuffer.popToken();
            t = tokenBuffer.nextToken();
            if (t.isTokenType(TokenTypes.TT_COLON)) {
                tokenBuffer.popToken();
                t = tokenBuffer.nextToken();
                if (t.isTokenType(TokenTypes.TT_IDENTIFIER)) {
                    tokenBuffer.popToken();
                    //  this.dbConfigResult.setPassword(t.getContent());
                } else {
                    parseSuccess = false;
                }
            } else {
                parseSuccess = false;
            }
        } else {
            parseSuccess = false;
        }
        if (!parseSuccess) {
            tokenBuffer.resetCurrentPosition(save);
        }
        return parseSuccess;
    }

    private boolean dbHostEnum() {
        boolean parseResult = dbHostParse();
        Token t = null;
        while (true) {
            if (parseResult) {
                t = tokenBuffer.nextToken();
                if (t != null && t.isTokenType(TokenTypes.TT_DBDESC_SPLIT)) {
                    tokenBuffer.popToken();
                    hostDesc.append(t.getContent());
                } else {
                    break;
                }
                parseResult = dbHostParse();
            }
        }
        return parseResult;
    }

    // (IP|Host)(Range)?
    private boolean dbHostParse() {
        String hostIp = this.ipORHost();
        if (hostIp != null) {
            if (!parseDBRange(hostIp)) {
                if (!parseDBSingle(hostIp)) {
                    this.dbConfigResult.addDbName(hostIp, this.dbConfigResult.getName());
                }
            }
            return true;
        } else {
            return false;
        }
        // return hostIp != null;
    }

    // '['NUM']'
    private boolean parseDBSingle(String hostIp) {
        boolean parseResult = true;
        final int save = this.tokenBuffer.getCurrentPosition();
        Token t = this.tokenBuffer.nextToken();
        if (t == null) {
            return false;
        }
        if (t.isTokenType(TokenTypes.TT_RANGE_LEFT)) {
            tokenBuffer.popToken();
        } else {
            parseResult = false;
        }
        NameRange range = null;
        String dbSuffix = null;
        t = this.tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_RANGE_NUMBER)) {
            tokenBuffer.popToken();
            dbSuffix = t.getContent();
            range = NameRange.loadInput(t.getContent());
        } else {
            parseResult = false;
        }
        t = this.tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_RANGE_RIGHT)) {
            tokenBuffer.popToken();
        } else {
            parseResult = false;
        }
        if (!parseResult) {
            tokenBuffer.resetCurrentPosition(save);
        } else {
            if (range == null) {
                throw new IllegalStateException("parse success but object range is null");
            }
            for (String dbOffset : range.list()) {
                this.dbConfigResult.addDbName(hostIp, this.dbConfigResult.getName() + dbOffset);
            }
            // 解析成功
            hostDesc.append("[" + dbSuffix + "]");
        }
        return parseResult;
    }

    // '[' NUM '-' NUM ']'
    private boolean parseDBRange(String hostIp) {
        boolean parseResult = true;
        final int save = tokenBuffer.getCurrentPosition();
        Token t = this.tokenBuffer.nextToken();
        if (t == null) {
            return false;
        }
        String from = null;
        String to = null;
        if (t.isTokenType(TokenTypes.TT_RANGE_LEFT)) {
            tokenBuffer.popToken();
        } else {
            parseResult = false;
        }
        t = this.tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_RANGE_NUMBER)) {
            tokenBuffer.popToken();
            from = t.getContent();
        } else {
            parseResult = false;
        }
        t = this.tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_RANGE_MINUS)) {
            tokenBuffer.popToken();
        } else {
            parseResult = false;
        }
        t = this.tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_RANGE_NUMBER)) {
            tokenBuffer.popToken();
            to = t.getContent();
        } else {
            parseResult = false;
        }
        t = this.tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_RANGE_RIGHT)) {
            tokenBuffer.popToken();
        } else {
            parseResult = false;
        }
        if (!parseResult) {
            // 设置初始状态
            tokenBuffer.resetCurrentPosition(save);
        } else {
            NameRange range = NameRange.loadInput(from + "-" + to);
            hostDesc.append("[" + from + "-" + to + "]");
            for (String dbOffset : range.list()) {
                this.dbConfigResult.addDbName(hostIp, this.dbConfigResult.getName() + dbOffset);
            }
        }
        return parseResult;
    }

    private String ipORHost() {
        boolean praseResult = false;
        String hostip = null;
        final int save = tokenBuffer.getCurrentPosition();
        Token t = tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_IP)) {
            tokenBuffer.popToken();
            hostDesc.append(t.getContent());
            praseResult = true;
            hostip = t.getContent();
        }
        if (!praseResult) {
            t = tokenBuffer.nextToken();
            if (t.isTokenType(TokenTypes.TT_HOST)) {
                hostDesc.append(t.getContent());
                tokenBuffer.popToken();
                praseResult = true;
                hostip = t.getContent();
            }
        }
        if (!praseResult) {
            tokenBuffer.resetCurrentPosition(save);
        }
        return hostip;
        // return praseResult;
    }

    private boolean dbPort() {
        boolean praseResult = false;
        final int save = tokenBuffer.getCurrentPosition();
        // port:3306
        Token token = tokenBuffer.nextToken();
        if (token.isTokenType(TokenTypes.TT_PORT)) {
            tokenBuffer.popToken();
            token = tokenBuffer.nextToken();
            if (token.isTokenType(TokenTypes.TT_COLON)) {
                tokenBuffer.popToken();
                // tokenBuffer.nextToken();
                token = tokenBuffer.nextToken();
                if (token.isTokenType(TokenTypes.TT_RANGE_NUMBER)) {
                    tokenBuffer.popToken();
                    praseResult = true;
                    // dbConfigResult.setPort(Integer.parseInt(token.getContent()));
                }
            }
        }
        if (!praseResult) {
            tokenBuffer.resetCurrentPosition(save);
        }
        return praseResult;
    }

    private boolean mysqlBlock() {
        boolean parseSuccess = true;
        final int save = tokenBuffer.getCurrentPosition();
        Token t = tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_MYSQL)) {
            dbConfigResult.setDbType(t.getContent());
            tokenBuffer.popToken();
        } else {
            parseSuccess = false;
        }
        t = tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_IDENTIFIER)) {
            dbConfigResult.setName(t.getContent());
            tokenBuffer.popToken();
        } else {
            parseSuccess = false;
        }
        t = tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_LEFT)) {
            tokenBuffer.popToken();
        } else {
            parseSuccess = false;
        }
        // 解析db的主要配置项
        if (dbPrimaryInfo()) {
        } else {
            parseSuccess = false;
        }
        dbPort();
        // /////////////////////////////////////
        t = tokenBuffer.nextToken();
        if (t.isTokenType(TokenTypes.TT_RIGHT)) {
            tokenBuffer.popToken();
        } else {
            parseSuccess = false;
        }
        if (!parseSuccess) {
            tokenBuffer.resetCurrentPosition(save);
        }
        return parseSuccess;
    }
}
