grammar Decaf;

@lexer::members {
    private void reportLexicalError(String message) {
        getErrorListenerDispatch().syntaxError(
            this, null, _tokenStartLine, _tokenStartCharPositionInLine, message, null);
    }
}

program
    : importDecl* fieldDecl* methodDecl* EOF
    ;

importDecl
    : IMPORT ID SEMICOLON
    ;

fieldDecl
    : type field (COMMA field)* SEMICOLON
    ;

field
    : ID (LBRACKET INT_LITERAL RBRACKET)?
    ;

methodDecl
    : (type | VOID) ID LPAREN parameters? RPAREN block
    ;

parameters
    : type ID (COMMA type ID)*
    ;

block
    : LBRACE fieldDecl* statement* RBRACE
    ;

statement
    : location assignExpr SEMICOLON
    | methodCall SEMICOLON
    | IF LPAREN expr RPAREN block (ELSE block)?
    | WHILE LPAREN expr RPAREN block
    | FOR LPAREN ID ASSIGN expr SEMICOLON expr SEMICOLON forUpdate RPAREN block
    | RETURN expr? SEMICOLON
    | BREAK SEMICOLON
    | CONTINUE SEMICOLON
    ;

forUpdate
    : location assignExpr
    ;

assignExpr
    : assignOp expr
    | increment
    ;

assignOp
    : ASSIGN | PLUS_ASSIGN | MINUS_ASSIGN | STAR_ASSIGN | SLASH_ASSIGN | PERCENT_ASSIGN
    ;

increment
    : INCREMENT | DECREMENT
    ;

location
    : ID (LBRACKET expr RBRACKET)?
    ;

// Strings are allowed as call arguments. Whether the callee is imported is a stage 2 check.
methodCall
    : ID LPAREN (externArg (COMMA externArg)*)? RPAREN
    ;

externArg
    : expr | STRING_LITERAL
    ;

expr
    : logicalOrExpr
    ;

logicalOrExpr
    : logicalAndExpr (OR logicalAndExpr)*
    ;

logicalAndExpr
    : equalityExpr (AND equalityExpr)*
    ;

equalityExpr
    : relationalExpr ((EQUAL | NOT_EQUAL) relationalExpr)*
    ;

relationalExpr
    : additiveExpr ((LESS | LESS_EQUAL | GREATER | GREATER_EQUAL) additiveExpr)*
    ;

additiveExpr
    : term ((PLUS | MINUS) term)*
    ;

term
    : unaryExpr ((STAR | SLASH | PERCENT) unaryExpr)*
    ;

// Prefix operators nest right-to-left. Cast operands are delimited by parentheses.
unaryExpr
    : (MINUS | NOT) unaryExpr
    | factor
    ;

factor
    : literal
    | methodCall
    | location
    | (INT | LONG) LPAREN expr RPAREN
    | LEN LPAREN ID RPAREN
    | LPAREN expr RPAREN
    ;

literal
    : INT_LITERAL | LONG_LITERAL | CHAR_LITERAL | TRUE | FALSE
    ;

type
    : INT | LONG | BOOL
    ;

// Keywords must precede ID for equal-length matches.
IMPORT   : 'import';
VOID     : 'void';
INT      : 'int';
LONG     : 'long';
BOOL     : 'bool';
IF       : 'if';
ELSE     : 'else';
WHILE    : 'while';
FOR      : 'for';
RETURN   : 'return';
BREAK    : 'break';
CONTINUE : 'continue';
LEN      : 'len';
TRUE     : 'true';
FALSE    : 'false';

PLUS_ASSIGN    : '+=';
MINUS_ASSIGN   : '-=';
STAR_ASSIGN    : '*=';
SLASH_ASSIGN   : '/=';
PERCENT_ASSIGN : '%=';
INCREMENT      : '++';
DECREMENT      : '--';
EQUAL          : '==';
NOT_EQUAL      : '!=';
LESS_EQUAL     : '<=';
GREATER_EQUAL  : '>=';
AND            : '&&';
OR             : '||';
ASSIGN         : '=';
LESS           : '<';
GREATER        : '>';
PLUS           : '+';
MINUS          : '-';
STAR           : '*';
SLASH          : '/';
PERCENT        : '%';
NOT            : '!';
LPAREN         : '(';
RPAREN         : ')';
LBRACE         : '{';
RBRACE         : '}';
LBRACKET       : '[';
RBRACKET       : ']';
COMMA          : ',';
SEMICOLON      : ';';

ID           : [a-zA-Z_] [a-zA-Z_0-9]*;
LONG_LITERAL : NUMBER 'L';
INT_LITERAL  : NUMBER;
CHAR_LITERAL : '\'' CHARACTER '\'';
STRING_LITERAL : '"' CHARACTER* '"';

fragment NUMBER : '0x' [0-9a-fA-F]+ | [0-9]+;
fragment CHARACTER
    : [\u0020-\u0021\u0023-\u0026\u0028-\u005B\u005D-\u007E]
    | '\\' ["'\\tnrf]
    ;

LINE_COMMENT : '//' ~[\r\n]* -> skip;
BLOCK_COMMENT
    : '/*' .*? ('*/' | EOF {reportLexicalError("unterminated block comment");}) -> skip
    ;
WS : [ \t\r\n\f]+ -> skip;

// Valid literals win equal-length matches. These rules report malformed ones
// once, including invalid escapes, non-ASCII characters and missing delimiters.
INVALID_STRING
    : '"' (~["\\\r\n] | '\\' ~[\r\n])* ('"' | [\r\n] | EOF)
      {reportLexicalError("invalid or unterminated string literal");} -> skip
    ;
INVALID_CHAR
    : '\'' (~['\\\r\n] | '\\' ~[\r\n])* ('\'' | [\r\n] | EOF)
      {reportLexicalError("invalid or unterminated character literal");} -> skip
    ;
INVALID_HEX
    : '0x' {reportLexicalError("hexadecimal literal requires at least one hex digit");} -> skip
    ;
