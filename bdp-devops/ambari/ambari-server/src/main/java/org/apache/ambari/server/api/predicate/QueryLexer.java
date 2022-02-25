/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.predicate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans a query expression and generates an array of tokens.
 * Each token contains type and value information.
 *
 * First, the query expression is broken down into string tokens using
 * a regular expression which splits on a set of deliminators which includes
 * operators and brackets.
 *
 * Second, each string token is converted into a Token with type and value information.
 */
public class QueryLexer {

  /**
   * Query string constants.
   */
  public static final String QUERY_FIELDS    = "fields";
  public static final String QUERY_FORMAT    = "format";
  public static final String QUERY_PAGE_SIZE = "page_size";
  public static final String QUERY_TO        = "to";
  public static final String QUERY_FROM      = "from";
  public static final String QUERY_MINIMAL   = "minimal_response";
  public static final String QUERY_SORT      = "sortBy";
  public static final String QUERY_DOAS      = "doAs";

  /**
   * All valid deliminators.
   */
  private static final String[] ALL_DELIMS =
      {".matches\\(",".in\\(",".isEmpty\\(","<=",">=","!=","=","<",">","&","|","!","(", ")"};

  /**
   * Map of token type to list of valid handlers for next token.
   */
  private static final Map<Token.TYPE, List<TokenHandler>> TOKEN_HANDLERS =
    new HashMap<>();

  /**
   * Static set of property names to ignore.
   */
  private static final Set<String> SET_IGNORE = new HashSet<>();


  /**
   * Constructor.
   * Register token handlers.
   */
  public QueryLexer() {
    //todo: refactor handler registration
    List<TokenHandler> listHandlers = new ArrayList<>();
    listHandlers.add(new LogicalUnaryOperatorTokenHandler());
    listHandlers.add(new OpenBracketTokenHandler());
    listHandlers.add(new PropertyOperandTokenHandler());

    TOKEN_HANDLERS.put(Token.TYPE.BRACKET_OPEN, listHandlers);
    TOKEN_HANDLERS.put(Token.TYPE.LOGICAL_OPERATOR, listHandlers);
    TOKEN_HANDLERS.put(Token.TYPE.LOGICAL_UNARY_OPERATOR, listHandlers);

    listHandlers= new ArrayList<>();
    listHandlers.add(new RelationalOperatorTokenHandler());
    listHandlers.add(new RelationalOperatorFuncTokenHandler());
    TOKEN_HANDLERS.put(Token.TYPE.PROPERTY_OPERAND, listHandlers);

    listHandlers = new ArrayList<>();
    listHandlers.add(new ValueOperandTokenHandler());
    TOKEN_HANDLERS.put(Token.TYPE.RELATIONAL_OPERATOR, listHandlers);

    listHandlers = new ArrayList<>();
    listHandlers.add(new CloseBracketTokenHandler());
    listHandlers.add(new ComplexValueOperandTokenHandler());
    TOKEN_HANDLERS.put(Token.TYPE.RELATIONAL_OPERATOR_FUNC, listHandlers);

    listHandlers = new ArrayList<>();
    listHandlers.add(new CloseBracketTokenHandler());
    listHandlers.add(new LogicalOperatorTokenHandler());
    TOKEN_HANDLERS.put(Token.TYPE.BRACKET_CLOSE, listHandlers);

    listHandlers = new ArrayList<>(listHandlers);
    // complex value operands can span multiple tokens
    listHandlers.add(0, new ComplexValueOperandTokenHandler());
    TOKEN_HANDLERS.put(Token.TYPE.VALUE_OPERAND, listHandlers);
  }


  /**
   * Scan the provided query and generate a token stream to be used by the query parser.
   *
   * @param exp  the query expression to scan
   *
   * @return an array of tokens
   * @throws InvalidQueryException if the query is invalid
   */
  public Token[] tokens(String exp) throws InvalidQueryException {
    return tokens(exp, Collections.emptySet());
  }

  /**
   * Scan the provided query and generate a token stream to be used by the query parser.
   *
   * @param exp               the query expression to scan
   * @param ignoreProperties  property names which should be ignored
   *
   * @return an array of tokens
   * @throws InvalidQueryException if the query is invalid
   */
  public Token[] tokens(String exp, Collection<String> ignoreProperties) throws InvalidQueryException {
    ScanContext ctx = new ScanContext();
    ctx.addPropertiesToIgnore(SET_IGNORE);
    ctx.addPropertiesToIgnore(ignoreProperties);

    for (String tok : parseStringTokens(exp)) {
      List<TokenHandler> listHandlers = TOKEN_HANDLERS.get(ctx.getLastTokenType());
      boolean            processed    = false;
      int                idx          = 0;

      while (!processed && idx < listHandlers.size()) {
        processed = listHandlers.get(idx++).handleToken(tok, ctx);
      }

      if (! processed) {
        throw new InvalidQueryException("Invalid Query Token: token='" +
            tok + "\', previous token type=" + ctx.getLastTokenType());
      }
    }

    ctx.validateEndState();

    return ctx.getTokenList().toArray(new Token[ctx.getTokenList().size()]);
  }

  /**
   * Uses a regular expression to scan a query expression and produce a list of string tokens.
   * These tokens are the exact strings that exist in the original syntax.
   *
   * @param exp  the query expression
   *
   * @return list of string tokens from the query expression
   */
  private List<String> parseStringTokens(String exp) {
    Pattern      pattern       = generatePattern();
    Matcher      matcher       = pattern.matcher(exp);
    List<String> listStrTokens = new ArrayList<>();
    int pos = 0;

    while (matcher.find()) { // while there's a delimiter in the string
      if (pos != matcher.start()) {
        // add anything between the current and previous delimiter to the tokens list
        listStrTokens.add(exp.substring(pos, matcher.start()));
      }
      listStrTokens.add(matcher.group()); // add the delimiter
      pos = matcher.end(); // Remember end of delimiter
    }
    if (pos != exp.length()) {
      // Add any chars remaining in the string after last delimiter
      listStrTokens.add(exp.substring(pos));
    }
    return listStrTokens;
  }

  /**
   * Generate the regex pattern to tokenize the query expression.
   *
   * @return the regex pattern
   */
  private Pattern generatePattern() {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    for (String delim : ALL_DELIMS) { // For each delimiter
      if (sb.length() != 1) sb.append('|');
      sb.append('\\');
      sb.append(delim);
    }
    sb.append(')');

    return Pattern.compile(sb.toString());
  }

  // Add property names that the lexer should ignore.
  static {
    // ignore values
    SET_IGNORE.add(QUERY_FIELDS);
    SET_IGNORE.add(QUERY_FORMAT);
    SET_IGNORE.add(QUERY_PAGE_SIZE);
    SET_IGNORE.add(QUERY_TO);
    SET_IGNORE.add(QUERY_FROM);
    SET_IGNORE.add(QUERY_MINIMAL);
    SET_IGNORE.add(QUERY_SORT);
    SET_IGNORE.add(QUERY_DOAS);
    SET_IGNORE.add("_");
  }

  /**
   * Scan context.  Provides contextual information related to the current scan.
   */
  private class ScanContext {
    /**
     * The last token type scanned.
     */
    private Token.TYPE m_lastType;

    /**
     * The last property operand value
     */
    private String m_propertyName;

    /**
     * List of tokens generated by the scan
     */
    private List<Token> m_listTokens = new ArrayList<>();

    /**
     * If non-null, ignore all tokens up to and including this token type.
     */
    private Token.TYPE m_ignoreSegmentEndToken = null;

    /**
     * Property names which are to be ignored.
     */
    private Set<String> m_propertiesToIgnore = new HashSet<>();

    /**
     * Bracket score.  This score is the difference between the number of
     * opening brackets and the number of closing brackets processed by
     * a handler.  Only handlers which process values containing brackets
     * will be interested in this information.
     */
    private int bracketScore = 0;

    /**
     * Intermediate tokens are tokens which are used by a handler which may
     * process several adjacent tokens.  A handler might push intermediate
     * tokens and then in subsequent invocations combine/alter/remove/etc
     * these tokens prior to adding them to the context tokens.
     */
    private Deque<Token> m_intermediateTokens = new ArrayDeque<>();


    /**
     * Constructor.
     */
    private ScanContext() {
      //init last type to the logical op type
      m_lastType = Token.TYPE.LOGICAL_OPERATOR;
    }

    /**
     * Ignore all subsequent tokens up to and including the provided token.
     *
     * @param type  the last token type of the ignore segment
     */
    public void setIgnoreSegmentEndToken(Token.TYPE type) {
      m_ignoreSegmentEndToken = type;
    }

    /**
     * Get the type of the last token.
     *
     * @return the type of the last token
     */
    public Token.TYPE getLastTokenType() {
      return m_lastType;
    }

    /**
     * Set the type of the last token.
     *
     * @param lastType  the type of the last token
     */
    public void setLastTokenType(Token.TYPE lastType) {
      m_lastType = lastType;
    }

    /**
     * Get the current property operand value.
     * This is used to hold the property operand name until it is added since,
     * the following relational operator token is added first.
     *
     * @return the current property operand value
     */
    public String getPropertyOperand() {
      return m_propertyName;
    }

    /**
     * Set the current property operand value.
     * This is used to hold the property operand name until it is added since,
     * the following relational operator token is added first.
     */
    public void setPropertyOperand(String prop) {
      m_propertyName = prop;
    }

    /**
     * Add a token.
     *
     * @param token  the token to add
     */
    public void addToken(Token token) {
      if (m_ignoreSegmentEndToken == null) {
        m_listTokens.add(token);
      } else if (token.getType() == m_ignoreSegmentEndToken) {
        m_ignoreSegmentEndToken = null;
      }
    }

    /**
     * Get the list of generated tokens.
     *
     * @return the list of generated tokens
     */
    public List<Token> getTokenList() {
      return m_listTokens;
    }

    /**
     * Get the set of property names that are to be ignored.
     *
     * @return set of property names to ignore
     */
    public Set<String> getPropertiesToIgnore() {
      return m_propertiesToIgnore;
    }

    /**
     * Add property names to the set of property names to ignore.
     *
     * @param ignoredProperties set of property names to ignore
     */
    public void addPropertiesToIgnore(Collection<String> ignoredProperties) {
      if (ignoredProperties != null) {
        m_propertiesToIgnore.addAll(ignoredProperties);
      }
    }

    /**
     * Add an intermediate token.
     *
     * @param token  the token to add
     */
    public void pushIntermediateToken(Token token) {
      if (m_ignoreSegmentEndToken == null) {
        m_intermediateTokens.add(token);
      } else if (token.getType() == m_ignoreSegmentEndToken) {
        m_ignoreSegmentEndToken = null;
      }
    }

    /**
     * Return the intermediate tokens if any.
     *
     * @return the intermediate tokens.  Will never return null.
     */
    public Deque<Token> getIntermediateTokens() {
      return m_intermediateTokens;
    }

    /**
     * Move all intermediate tokens to the context tokens.
     */
    public void addIntermediateTokens() {
      m_listTokens.addAll(m_intermediateTokens);
      m_intermediateTokens.clear();
    }

    /**
     * Obtain the bracket score.  This count is the number of outstanding opening brackets.
     * A value of 0 indicates all opening and closing brackets are matched
     * @return the current bracket score
     */
    public int getBracketScore() {
      return bracketScore;
    }

    /**
     * Increment the bracket score by n.  This indicates that n unmatched opening brackets
     * have been encountered.
     *
     * @param n amount to increment
     * @return the new bracket score after incrementing
     */
    public int incrementBracketScore(int n) {
      return bracketScore += n;
    }

    /**
     * Decrement the bracket score.  This is done when matching a closing bracket with a previously encountered
     * opening bracket.  If the requested decrement would result in a negative number an exception is thrown
     * as this isn't a valid state.
     *
     * @param decValue  amount to decrement
     * @return the new bracket score after decrementing
     * @throws InvalidQueryException if the decrement operation will result in a negative value
     */
    public int decrementBracketScore(int decValue) throws InvalidQueryException {
      bracketScore -= decValue;
      if (bracketScore < 0) {
        throw new InvalidQueryException("Unexpected closing bracket.  Last token type: " + getLastTokenType() +
            ", Current property operand: " + getPropertyOperand() + ", tokens: " + getTokenList());
      }
      return bracketScore;
    }

    //todo: most handlers should implement this
    /**
     * Validate the end state of the scan context.
     * Iterates over each handler associated with the final token type and asks it to validate the context.
     * @throws InvalidQueryException  if the context is determined to in an invalid end state
     */
    public void validateEndState() throws InvalidQueryException {
      for (TokenHandler handler : TOKEN_HANDLERS.get(getLastTokenType())) {
        handler.validateEndState(this);
      }
    }
  }

  /**
   * Token handler base class.
   * Token handlers are responsible for processing specific token type.
   */
  private abstract class TokenHandler {
    /**
     * Provides base token handler functionality then delegates to the individual concrete handlers.
     *
     * @param token   the token to process
     * @param ctx     the scan context
     *
     * @return true if this handler processed the token; false otherwise
     * @throws InvalidQueryException  if an invalid token is encountered
     */
    public boolean handleToken(String token, ScanContext ctx) throws InvalidQueryException {
      if (handles(token, ctx)) {
        _handleToken(token, ctx);
        ctx.setLastTokenType(getType());
        return true;
      } else {
        return false;
      }
    }

    public void validateEndState(ScanContext ctx) throws InvalidQueryException {
      if (! ctx.getIntermediateTokens().isEmpty()) {
        throw new InvalidQueryException("Unexpected end of expression.");
      }
    }

    /**
     * Process a token.
     *
     * @param token  the token to process
     * @param ctx    the current scan context
     * @throws InvalidQueryException if an invalid token is encountered
     */
    public abstract void _handleToken(String token, ScanContext ctx) throws InvalidQueryException;

    /**
     * Get the token handler type.
     *
     * @return the token handler type
     */
    public abstract Token.TYPE getType();

    /**
     * Determine if a handler handles a specific token type.
     *
     *
     * @param token  the token type
     * @param ctx    scan context
     * @return true if the handler handles the specified type; false otherwise
     */
    public abstract boolean handles(String token, ScanContext ctx);
  }

  /**
   * Property Operand token handler.
   */
  private class PropertyOperandTokenHandler extends TokenHandler {

    @Override
    public void _handleToken(String token, ScanContext ctx) throws InvalidQueryException {
      //don't add prop name token until after operator token
      if (! ctx.getPropertiesToIgnore().contains(token)) {
        ctx.setPropertyOperand(token);
      } else {
        if (!ctx.getTokenList().isEmpty() ) {
          // ignore through next value operand
          ctx.setIgnoreSegmentEndToken(Token.TYPE.VALUE_OPERAND);
          // remove preceding '&' token
          ctx.getTokenList().remove(ctx.getTokenList().size() -1);
        } else {
          // first expression.  Ignore and strip out next '&'
          ctx.setIgnoreSegmentEndToken(Token.TYPE.LOGICAL_OPERATOR);
        }
      }
    }

    @Override
    public Token.TYPE getType() {
      return Token.TYPE.PROPERTY_OPERAND;
    }

    @Override
    public boolean handles(String token, ScanContext ctx) {
      return token.matches("[^!&\\|<=|>=|!=|=|<|>\\(\\)]+");
    }
  }

  /**
   * Value Operand token handler.
   */
  private class ValueOperandTokenHandler extends TokenHandler {
    @Override
    public void _handleToken(String token, ScanContext ctx) throws InvalidQueryException {
      ctx.addToken(new Token(Token.TYPE.VALUE_OPERAND, token));
    }

    @Override
    public Token.TYPE getType() {
      return Token.TYPE.VALUE_OPERAND;
    }

    @Override
    public boolean handles(String token, ScanContext ctx) {
      return token.matches("[^!&\\|<=|>=|!=|=|<|>]+");
    }
  }

  /**
   * Open Bracket token handler.
   */
  private class OpenBracketTokenHandler extends TokenHandler {
    @Override
    public void _handleToken(String token, ScanContext ctx) throws InvalidQueryException {
      ctx.addToken(new Token(Token.TYPE.BRACKET_OPEN, token));
    }

    @Override
    public Token.TYPE getType() {
      return Token.TYPE.BRACKET_OPEN;
    }

    @Override
    public boolean handles(String token, ScanContext ctx) {
      return token.matches("\\(");
    }
  }

  /**
   * Close Bracket token handler.
   */
  private class CloseBracketTokenHandler extends TokenHandler {
    @Override
    public void _handleToken(String token, ScanContext ctx) throws InvalidQueryException {
      ctx.addToken(new Token(Token.TYPE.BRACKET_CLOSE, token));
    }

    @Override
    public Token.TYPE getType() {
      return Token.TYPE.BRACKET_CLOSE;
    }

    @Override
    public boolean handles(String token, ScanContext ctx) {
      return token.matches("\\)");
    }
  }

  /**
   * Relational Operator token handler.
   */
  private class RelationalOperatorTokenHandler extends TokenHandler {
    @Override
    public void _handleToken(String token, ScanContext ctx) throws InvalidQueryException {
      ctx.addToken(new Token(Token.TYPE.RELATIONAL_OPERATOR, token));
      ctx.addToken(new Token(Token.TYPE.PROPERTY_OPERAND, ctx.getPropertyOperand()));
    }

    @Override
    public Token.TYPE getType() {
      return Token.TYPE.RELATIONAL_OPERATOR;
    }

    @Override
    public boolean handles(String token, ScanContext ctx) {
      return token.matches("<=|>=|!=|=|<|>");
    }
  }

  /**
   * Relational Operator function token handler.
   */
  private class RelationalOperatorFuncTokenHandler extends TokenHandler {
    @Override
    public void _handleToken(String token, ScanContext ctx) throws InvalidQueryException {
      ctx.addToken(new Token(Token.TYPE.RELATIONAL_OPERATOR_FUNC, token));
      ctx.addToken(new Token(Token.TYPE.PROPERTY_OPERAND, ctx.getPropertyOperand()));
    }

    @Override
    public Token.TYPE getType() {
      return Token.TYPE.RELATIONAL_OPERATOR_FUNC;
    }

    //todo: add a unary relational operator func
    @Override
    public boolean handles(String token, ScanContext ctx) {
      return token.matches("\\.[a-zA-Z]+\\(");
    }
  }

  /**
   * Complex Value Operand token handler.
   * Supports values that span multiple tokens.
   */
  private class ComplexValueOperandTokenHandler extends TokenHandler {
    @Override
    public void _handleToken(String token, ScanContext ctx) throws InvalidQueryException {
      if (token.equals(")")) {
        ctx.decrementBracketScore(1);
      } else if (token.endsWith("(")) {
        // .endsWith() is used because of tokens ".matches(",".in(" and".isEmpty("
        ctx.incrementBracketScore(1);
      }

      String tokenValue = token;
      if (ctx.getBracketScore() > 0) {
        Deque<Token> intermediateTokens = ctx.getIntermediateTokens();
        if (intermediateTokens !=null && !intermediateTokens.isEmpty()) {
          Token lastToken = intermediateTokens.peek();
          if (lastToken.getType() == Token.TYPE.VALUE_OPERAND) {
            intermediateTokens.pop();
            tokenValue = lastToken.getValue() + token;
          }
        }
        ctx.pushIntermediateToken(new Token(Token.TYPE.VALUE_OPERAND, tokenValue));
      }

      if (ctx.getBracketScore() == 0) {
        ctx.addIntermediateTokens();
        ctx.addToken(new Token(Token.TYPE.BRACKET_CLOSE, ")"));
      }
    }

    @Override
    public Token.TYPE getType() {
      return Token.TYPE.VALUE_OPERAND;
    }

    @Override
    public boolean handles(String token, ScanContext ctx) {
      Token.TYPE lastTokenType = ctx.getLastTokenType();
      if (lastTokenType == Token.TYPE.RELATIONAL_OPERATOR_FUNC) {
        ctx.incrementBracketScore(1);
        return true;
      } else {
        return ctx.getBracketScore() > 0;
      }
    }

    @Override
    public void validateEndState(ScanContext ctx) throws InvalidQueryException {
      if (ctx.getBracketScore() > 0) {
        throw new InvalidQueryException("Missing closing bracket for function: " + ctx.getTokenList());
      }
    }
  }

  /**
   * Logical Operator token handler.
   */
  private class LogicalOperatorTokenHandler extends TokenHandler {
    @Override
    public void _handleToken(String token, ScanContext ctx) throws InvalidQueryException {
      ctx.addToken(new Token(Token.TYPE.LOGICAL_OPERATOR, token));
    }

    @Override
    public Token.TYPE getType() {
      return Token.TYPE.LOGICAL_OPERATOR;
    }

    @Override
    public boolean handles(String token, ScanContext ctx) {
      return token.matches("[!&\\|]");
    }
  }

  /**
   * Logical Unary Operator token handler.
   */
  private class LogicalUnaryOperatorTokenHandler extends TokenHandler {
    @Override
    public void _handleToken(String token, ScanContext ctx) throws InvalidQueryException {
      ctx.addToken(new Token(Token.TYPE.LOGICAL_UNARY_OPERATOR, token));
    }

    @Override
    public Token.TYPE getType() {
      return Token.TYPE.LOGICAL_UNARY_OPERATOR;
    }

    @Override
    public boolean handles(String token, ScanContext ctx) {
      return "!".equals(token);
    }
  }
}
