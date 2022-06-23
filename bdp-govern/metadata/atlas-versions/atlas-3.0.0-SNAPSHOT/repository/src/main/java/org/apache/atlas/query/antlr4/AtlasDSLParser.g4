/**
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

parser grammar AtlasDSLParser;

options { tokenVocab=AtlasDSLLexer; }

// Core rules
identifier: ID ;

operator: (K_LT | K_LTE | K_EQ | K_NEQ | K_GT | K_GTE | K_LIKE) ;

sortOrder: K_ASC | K_DESC ;

valueArray: K_LBRACKET ID (K_COMMA ID)* K_RBRACKET ;

literal: BOOL | NUMBER | FLOATING_NUMBER | (ID | valueArray) ;

// Composite rules
limitClause: K_LIMIT NUMBER ;

offsetClause: K_OFFSET NUMBER ;

atomE: (identifier | literal) | K_LPAREN expr K_RPAREN ;

multiERight: (K_STAR | K_DIV) atomE ;

multiE: atomE multiERight* ;

arithERight: (K_PLUS | K_MINUS) multiE ;

arithE: multiE arithERight* ;

comparisonClause: arithE operator arithE ;

isClause: arithE (K_ISA | K_IS) (identifier | expr ) ;

hasTermClause: arithE K_HASTERM (identifier | expr );

hasClause: arithE K_HAS identifier ;

countClause: K_COUNT K_LPAREN K_RPAREN ;

maxClause: K_MAX K_LPAREN expr K_RPAREN ;

minClause: K_MIN K_LPAREN expr K_RPAREN ;

sumClause: K_SUM K_LPAREN expr K_RPAREN ;

exprRight: (K_AND | K_OR) compE ;

compE: comparisonClause
    | isClause
    | hasClause
    | arithE
    | countClause
    | maxClause
    | minClause
    | sumClause
    | hasTermClause
    ;

expr: compE exprRight* ;

limitOffset: limitClause offsetClause? ;

selectExpression: expr (K_AS identifier)? ;

selectExpr: selectExpression (K_COMMA selectExpression)* ;

aliasExpr: (identifier | literal) K_AS identifier ;

orderByExpr: K_ORDERBY expr sortOrder? ;

fromSrc: aliasExpr | (identifier | literal) ;

whereClause: K_WHERE expr ;

fromExpression: fromSrc whereClause? ;

fromClause: K_FROM fromExpression ;

selectClause: K_SELECT selectExpr ;

singleQrySrc: fromClause | whereClause | fromExpression | expr ;

groupByExpression: K_GROUPBY K_LPAREN selectExpr K_RPAREN ;

commaDelimitedQueries: singleQrySrc (K_COMMA singleQrySrc)* ;

spaceDelimitedQueries: singleQrySrc singleQrySrc* ;

querySrc: commaDelimitedQueries | spaceDelimitedQueries ;

query: querySrc groupByExpression?
                selectClause?
                orderByExpr?
                limitOffset? EOF;