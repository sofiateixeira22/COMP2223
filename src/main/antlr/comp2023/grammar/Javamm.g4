grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;
COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDeclaration EOF
    | statement+ EOF
    ;

importDeclaration
    : 'import' identifier ('.'identifier)* ';'
    ;

classDeclaration
    : 'class' identifier ( extend='extends' identifier)? '{' (varDeclaration)* (methodDeclaration)* '}'
    ;

varDeclaration
    : type identifier ';'
    ;


methodDeclaration
    : 'public'? type identifier '(' (type identifier ( ',' type identifier)* )? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}'
    | 'public'? 'static' type identifier '(' type '[' ']' identifier ')' '{' (varDeclaration)* (statement)* '}'
    ;

type
    : t='int[]'
    | t='boolean'
    | t='int'
    | t=ID
    ;

identifier
    : value=ID
    ;

statement
    : '{'(statement)*'}'
    | 'if' '(' expression ')' statement 'else' statement
    | 'while' '(' expression ')' statement
    | expression ';'
    | identifier '=' expression ';'
    | identifier '[' expression ']' '=' expression ';'
    ;

expression
    : expression '[' expression ']' #BinaryOp
    | expression '.' 'length' #UnaryOp
    | expression '.' ID '(' ( expression ( ',' expression )* )? ')' #TernaryOp
    | '(' expression ')' #UnaryOp
    | expression ('++' | '--') #UnaryOp
    | ('+' | '-' | '!' | '~' | '++' | '--') expression #UnaryOp
    | 'new' type '[' expression ']' #ArrayNew
    | 'new' ID '('')' #ClassNew
    | expression op=('*' | '/' | '%') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('<' | '>' | '<=' | '>=') expression #BinaryOp
    | expression op=('==' | '!=') #BinaryOp
    | expression '&' expression #BinaryOp
    | expression '^' expression #BinaryOp
    | expression '|' expression #BinaryOp
    | expression '&&' expression #BinaryOp
    | expression '||' expression #BinaryOp
    | expression ('=' | '+=' | '-=' | '*=' | '/=' | '%=' | '&=' | '^=' | '|=' | '<<=' | '>>=' | '>>>=' ) expression #BinaryOp
    | value=INTEGER #Integer
    | 'true' #Boolean
    | 'false' #Boolean
    | value=ID #IdentifierExpr
    | 'this' #ThisExpr
    ;

