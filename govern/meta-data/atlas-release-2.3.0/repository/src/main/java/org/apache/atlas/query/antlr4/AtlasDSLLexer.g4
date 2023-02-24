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

lexer grammar AtlasDSLLexer;

fragment A: ('A'|'a');
fragment B: ('B'|'b');
fragment C: ('C'|'c');
fragment D: ('D'|'d');
fragment E: ('E'|'e');
fragment F: ('F'|'f');
fragment G: ('G'|'g');
fragment H: ('H'|'h');
fragment I: ('I'|'i');
fragment J: ('J'|'j');
fragment K: ('K'|'k');
fragment L: ('L'|'l');
fragment M: ('M'|'m');
fragment N: ('N'|'n');
fragment O: ('O'|'o');
fragment P: ('P'|'p');
fragment Q: ('Q'|'q');
fragment R: ('R'|'r');
fragment S: ('S'|'s');
fragment T: ('T'|'t');
fragment U: ('U'|'u');
fragment V: ('V'|'v');
fragment W: ('W'|'w');
fragment X: ('X'|'x');
fragment Y: ('Y'|'y');
fragment Z: ('Z'|'z');

fragment DIGIT: [0-9];

fragment LETTER: 'a'..'z'| 'A'..'Z' | '_';

// Comment skipping
SINGLE_LINE_COMMENT: '--' ~[\r\n]* -> channel(HIDDEN) ;

MULTILINE_COMMENT : '/*' .*? ( '*/' | EOF ) -> channel(HIDDEN) ;

WS: (' ' ' '* | [ \n\t\r]+) -> channel(HIDDEN) ;

// Lexer rules
NUMBER: (K_PLUS | K_MINUS)? DIGIT DIGIT* (E (K_PLUS | K_MINUS)? DIGIT DIGIT*)? ;

FLOATING_NUMBER: (K_PLUS | K_MINUS)? DIGIT+ K_DOT DIGIT+ (E (K_PLUS | K_MINUS)? DIGIT DIGIT*)? ;

BOOL: K_TRUE | K_FALSE ;

K_COMMA: ',' ;

K_PLUS: '+' ;

K_MINUS: '-' ;

K_STAR: '*' ;

K_DIV: '/' ;

K_DOT: '.' ;

K_LIKE: L I K E ;

K_AND: A N D ;

K_OR: O R ;

K_LPAREN: '(' ;

K_LBRACKET: '[' ;

K_RPAREN: ')' ;

K_RBRACKET: ']' ;

K_LT: '<' | L T ;

K_LTE: '<=' | L T E ;

K_EQ: '=' | E Q ;

K_NEQ: '!=' | N E Q ;

K_GT: '>' | G T ;

K_GTE: '>=' | G T E ;

K_FROM: F R O M ;

K_WHERE: W H E R E ;

K_ORDERBY: O R D E R B Y ;

K_GROUPBY: G R O U P B Y ;

K_LIMIT: L I M I T ;

K_SELECT: S E L E C T ;

K_MAX: M A X ;

K_MIN: M I N ;

K_SUM: S U M ;

K_COUNT: C O U N T ;

K_OFFSET: O F F S E T ;

K_AS: A S ;

K_ISA: I S A ;

K_IS: I S ;

K_HAS: H A S ;

K_ASC: A S C ;

K_DESC: D E S C ;

K_TRUE: T R U E ;

K_FALSE: F A L S E ;

K_HASTERM: H A S T E R M;

KEYWORD: K_LIKE
        | K_DOT
        | K_SELECT
        | K_AS
        | K_HAS
        | K_IS
        | K_ISA
        | K_WHERE
        | K_LIMIT
        | K_TRUE
        | K_FALSE
        | K_AND
        | K_OR
        | K_GROUPBY
        | K_ORDERBY
        | K_SUM
        | K_MIN
        | K_MAX
        | K_OFFSET
        | K_FROM
        | K_DESC
        | K_ASC
        | K_COUNT
        | K_HASTERM
        ;

ID: STRING
    |LETTER (LETTER|DIGIT)*
    | LETTER (LETTER|DIGIT)* KEYWORD KEYWORD*
    | KEYWORD KEYWORD* LETTER (LETTER|DIGIT)*
    | LETTER (LETTER|DIGIT)* KEYWORD KEYWORD* LETTER (LETTER|DIGIT)*
    ;

STRING: '"' ~('"')* '"' | '\'' ~('\'')* '\'' | '`' ~('`')* '`';