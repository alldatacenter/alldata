grammar SodaCLAntlr;

// Checks

check
 : failed_rows_check
 | row_count_comparison_check
 | metric_check
 | reference_check
 | freshness_check
 ;

freshness_check
 : 'freshness using' S identifier freshness_variable? (S LT S freshness_threshold_value)? EOF
 ;

freshness_variable
 : S 'with' S identifier
 ;

warn_qualifier
 : S 'warn'
 ;

failed_rows_check: IDENTIFIER_DOUBLE_QUOTE EOF;

row_count_comparison_check: 'row_count same as' S identifier (S partition_name)? (S IN S identifier)? EOF;

metric_check
 :  (change_over_time|anomaly_score)? metric (S (threshold|default_anomaly_threshold))? EOF
 ;

default_anomaly_threshold
 : LT S 'default'
 ;

change_over_time
 : CHANGE S (change_over_time_config S)? percent? FOR S
 ;

change_over_time_config
 : change_aggregation S LAST S integer
 | same_day_last_week
 ;

change_aggregation
 : (AVG|MIN|MAX)
 ;

same_day_last_week
: 'same day last week'
;

percent
 : 'percent' S
 ;

anomaly_score
 : 'anomaly score for '
 ;

metric
 : metric_name metric_args?
 ;

metric_name
 : identifier
 ;

metric_args
 : ROUND_LEFT metric_arg (COMMA S metric_arg)* ROUND_RIGHT
 ;

metric_arg
 : signed_number
 | identifier
 ;

threshold
 : comparator_threshold
 | between_threshold
 ;

between_threshold
 : (NOT S)? BETWEEN S (SQUARE_LEFT|ROUND_LEFT)? threshold_value S AND S threshold_value (SQUARE_RIGHT|ROUND_RIGHT)?
 ;

comparator_threshold
 : comparator S threshold_value
 ;

zones_threshold
 : (outcome S zone_comparator S threshold_value S zone_comparator S)+ outcome
 ;

outcome
 : WARN | FAIL | PASS
 ;

zone_comparator
 : LT | LTE
 ;

comparator
 : LT | LTE | EQUAL | GTE | GT | NOT_EQUAL | NOT_EQUAL_SQL
 ;

threshold_value
 : signed_number (S? PERCENT)?
 | freshness_threshold_value
 | IDENTIFIER_UNQUOTED
 ;


freshness_threshold_value
 : (integer ('d'|'h'|'m'))+ integer?
 ;

reference_check
 : 'values in' S source_column_name S 'must exist in' S identifier S target_column_name
 | 'values in' S ROUND_LEFT source_column_name (COMMA S source_column_name)* ROUND_RIGHT S 'must exist in' S identifier S ROUND_LEFT target_column_name (COMMA S target_column_name)* ROUND_RIGHT
 ;

source_column_name
 : identifier
 ;

target_column_name
 : identifier
 ;

// Sections headers

section_header
 : table_checks_header
 | column_configurations_header
 | table_filter_header
 | checks_for_each_dataset_header
 | checks_for_each_column_header
 ;

table_checks_header
 : 'checks for' S identifier (S partition_name)? EOF
 ;

partition_name
 : SQUARE_LEFT identifier SQUARE_RIGHT
 ;

table_filter_header
 : 'filter' S identifier S partition_name EOF
 ;

column_configurations_header
 : 'configurations for' S identifier EOF
 ;

checks_for_each_dataset_header
 : 'for each dataset' S identifier EOF
 | 'for each table' S identifier EOF
 ;

checks_for_each_column_header
 : 'for each column' S identifier EOF
 ;

signed_number
 : (PLUS|MINUS)? number
 ;

number
 : integer
 | DIGITS '.' DIGITS?
 | DIGITS? '.' DIGITS
 ;

integer
 : DIGITS
 ;

identifier
 : IDENTIFIER_UNQUOTED
 | IDENTIFIER_DOUBLE_QUOTE
 | IDENTIFIER_BACKTICK
 | MIN
 | MAX
 | AVG
 ;

FOR: 'for';
AND: 'and';
BETWEEN: 'between';
NOT: 'not';
IN: 'in';
WARN: 'warn';
FAIL: 'fail';
PASS: 'pass';

CHANGE: 'change';
LAST: 'last';
AVG: 'avg';
MIN: 'min';
MAX: 'max';

SQUARE_LEFT: '[';
SQUARE_RIGHT: ']';
CURLY_LEFT: '{';
CURLY_RIGHT: '}';
ROUND_LEFT: '(';
ROUND_RIGHT: ')';

COMMA: ',';
PERCENT: '%';

PLUS: '+';
MINUS: '-';

NOT_EQUAL: '!=';
NOT_EQUAL_SQL: '<>';
LTE: '<=';
GTE: '>=';
EQUAL: '=';
LT: '<';
GT: '>';

IDENTIFIER_DOUBLE_QUOTE: '"' ( ~'"' | '\\"')+ '"';
IDENTIFIER_BACKTICK: '`' ( ~'`' | '\\`')+ '`';
IDENTIFIER_UNQUOTED: [a-zA-Z_$] ~(' ' | '<' | '=' | '>' | '(' | ')' | '[' | ']' | ',')*;
STRING: [a-z]+;
DIGITS: [0-9]+;

S: ' ';
