grammar Decaf;

program
    : methodDecl* EOF
    ;

methodDecl
    : VOID ID '(' ')' block
    ;

block
    : '{' fieldDecl* statement* '}'
    ;

fieldDecl
    : type ID (',' ID)* ';'
    ;

statement
    : ID assignExpr ';'
    ;

assignExpr
    : assignOp expr
    | increment
    ;

assignOp
    : '='
    | '+='
    | '-='
    | '*='
    | '/='
    | '%='
    ;

increment
    : '++'
    | '--'
    ;

expr
    : equalityExpr
    ;

equalityExpr
    : relationalExpr (('==' | '!=') relationalExpr)*
    ;

relationalExpr
    : additiveExpr (('<' | '<=' | '>' | '>=') additiveExpr)*
    ;

additiveExpr
    : term (('+' | '-') term)*
    ;

term
    : unaryExpr (('*' | '/' | '%') unaryExpr)*
    ;

unaryExpr
    : '-' unaryExpr
    | factor
    ;

factor
    : INT_LITERAL
    | TRUE
    | FALSE
    | ID
    | '(' expr ')'
    ;

type
    : INT
    | LONG
    | BOOL
    ;

VOID : 'void';
INT  : 'int';
LONG : 'long';
BOOL : 'bool';

ID : [a-zA-Z_] [a-zA-Z_0-9]*;
WS : [ \t\r\n\f]+ -> skip;
INT_LITERAL : [0-9]+;
TRUE : 'true';
FALSE : 'false';
