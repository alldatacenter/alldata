/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.query;

import com.google.common.annotations.VisibleForTesting;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenStream;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.query.antlr4.AtlasDSLLexer;
import org.apache.atlas.query.antlr4.AtlasDSLParser;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AtlasDSL {

    public static class Parser {
        private static final Logger LOG = LoggerFactory.getLogger(Parser.class);

        private static final Set<String> RESERVED_KEYWORDS =
                new HashSet<>(Arrays.asList("[", "]", "(", ")", "=", "<", ">", "!=", "<=", ">=", ",", "and", "or", "+", "-",
                                            "*", "/", ".", "select", "from", "where", "groupby", "loop", "isa", "is", "has",
                                            "as", "times", "withPath", "limit", "offset", "orderby", "count", "max", "min",
                                            "sum", "by", "order", "like"));

        public static boolean isKeyword(String word) {
            return RESERVED_KEYWORDS.contains(word);
        }

        @VisibleForTesting
        static AtlasDSLParser.QueryContext parse(String queryStr) throws AtlasBaseException {
            AtlasDSLParser.QueryContext ret;
            try {
                InputStream    stream           = new ByteArrayInputStream(queryStr.getBytes());
                AtlasDSLLexer  lexer            = new AtlasDSLLexer(CharStreams.fromStream(stream));
                Validator      validator        = new Validator();
                TokenStream    inputTokenStream = new CommonTokenStream(lexer);
                AtlasDSLParser parser           = new AtlasDSLParser(inputTokenStream);

                parser.removeErrorListeners();
                parser.addErrorListener(validator);

                // Validate the syntax of the query here
                ret = parser.query();
                if (!validator.isValid()) {
                    LOG.error("Invalid DSL: {} Reason: {}", queryStr, validator.getErrorMsg());
                    throw new AtlasBaseException(AtlasErrorCode.INVALID_DSL_QUERY, queryStr, validator.getErrorMsg());
                }

            } catch (IOException e) {
                throw new AtlasBaseException(e);
            }

            return ret;
        }
    }

    static class Validator extends BaseErrorListener {
        private boolean isValid  = true;
        private String  errorMsg = "";

        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line, final int charPositionInLine, final String msg, final RecognitionException e) {
            isValid = false;
            errorMsg = msg;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMsg() {
            return errorMsg;
        }
    }

    public static class Translator {
        private static final Logger LOG = LoggerFactory.getLogger(Translator.class);

        private final AtlasDSLParser.QueryContext queryContext;
        private final AtlasTypeRegistry           typeRegistry;
        private final int                         offset;
        private final int                         limit;
        private final String                      query;

        public Translator(String query, AtlasTypeRegistry typeRegistry, int offset, int limit) throws AtlasBaseException {
            this.query        = query;
            this.queryContext = Parser.parse(query);
            this.typeRegistry = typeRegistry;
            this.offset       = offset;
            this.limit        = limit;
        }

        public GremlinQuery translate() throws AtlasBaseException {
            QueryMetadata        queryMetadata = new QueryMetadata(queryContext);
            GremlinQueryComposer queryComposer = new GremlinQueryComposer(typeRegistry, queryMetadata, limit, offset);

            queryContext.accept(new DSLVisitor(queryComposer));

            processErrorList(queryComposer);

            return new GremlinQuery(queryComposer.get(), queryMetadata, queryComposer.clauses(), queryComposer.getSelectComposer());
        }

        private void processErrorList(GremlinQueryComposer gremlinQueryComposer) throws AtlasBaseException {
            if (CollectionUtils.isNotEmpty(gremlinQueryComposer.getErrorList())) {
                final String errorMessage = StringUtils.join(gremlinQueryComposer.getErrorList(), ", ");

                LOG.warn("DSL Errors: {}", errorMessage);

                throw new AtlasBaseException(AtlasErrorCode.INVALID_DSL_QUERY, this.query, errorMessage);
            }
        }
    }

    public static class QueryMetadata {
        private final boolean hasSelect;
        private final boolean hasGroupBy;
        private final boolean hasOrderBy;
        private final boolean hasLimitOffset;
        private final int     resolvedLimit;
        private final int     resolvedOffset;

        public QueryMetadata(AtlasDSLParser.QueryContext queryContext) {
            hasSelect      = queryContext != null && queryContext.selectClause() != null;
            hasGroupBy     = queryContext != null && queryContext.groupByExpression() != null;
            hasOrderBy     = queryContext != null && queryContext.orderByExpr() != null;
            hasLimitOffset = queryContext != null && queryContext.limitOffset() != null;

            if (hasLimitOffset) {
                AtlasDSLParser.LimitOffsetContext  limitOffsetContext = queryContext.limitOffset();
                AtlasDSLParser.LimitClauseContext  limitClause        = limitOffsetContext.limitClause();
                AtlasDSLParser.OffsetClauseContext offsetClause       = limitOffsetContext.offsetClause();

                resolvedLimit = (limitClause != null) ? Integer.parseInt(limitClause.NUMBER().getText()) : 0;
                resolvedOffset = (offsetClause != null) ? Integer.parseInt(offsetClause.NUMBER().getText()) : 0;
            } else {
                resolvedLimit = 0;
                resolvedOffset = 0;
            }
        }

        public boolean hasSelect() {
            return hasSelect;
        }

        public boolean hasGroupBy() {
            return hasGroupBy;
        }

        public boolean hasOrderBy() {
            return hasOrderBy;
        }

        public boolean hasLimitOffset() {
            return hasLimitOffset;
        }

        public boolean needTransformation() {
            return (hasGroupBy && hasSelect && hasOrderBy) || hasSelect;
        }

        public int getResolvedLimit() {
            return resolvedLimit;
        }

        public int getResolvedOffset() {
            return resolvedOffset;
        }
    }
}
