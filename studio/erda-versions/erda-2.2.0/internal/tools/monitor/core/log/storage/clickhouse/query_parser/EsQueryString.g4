grammar EsQueryString;

GROUP_BEGIN : '(';
GROUP_END : ')';
WHITESPACE: [ \r\n\t]+ -> skip;

FIELD: [a-zA-Z0-9_] [-a-zA-Z0-9_.]+ ':';
TERM: ~[ \r\n\t"():]+;
PHRASE: '"' ~('"')+ '"';

query
    : queryExpression? EOF
    ;

queryExpression
    : GROUP_BEGIN queryExpression GROUP_END                         # GroupExpression
    | op=('NOT'|'not') queryExpression                              # NotExpression
    | queryExpression op=('AND'|'and') queryExpression              # AndExpression
    | queryExpression op=('OR'|'or') queryExpression                # OrExpression
    | queryExpression queryExpression                               # DefaultOpExpression
    | fieldQuery                                                    # FieldExpression
    ;

fieldQuery
    : FIELD PHRASE                                                  # NamedPhraseFieldQuery
    | FIELD TERM                                                    # NamedTermFieldQuery
    | PHRASE                                                        # PhraseFieldQuery
    | TERM                                                          # TermFieldQuery
    ;