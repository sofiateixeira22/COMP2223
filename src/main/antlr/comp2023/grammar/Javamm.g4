grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;

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
    : 'public'? type ID '(' (type ID ( ',' type ID)* )? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}'
    | 'public'? 'static' 'void' 'main' '(' type '[' ']' ID ')' '{' (varDeclaration)* (statement)* '}'
    ;

type
    : t='int' '[' ']'
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
    | ID '=' expression ';'
    | ID '[' expression ']' '=' expression ';'
    ;

expression
    : expression ('++' | '--') #UnaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('<' | '>' | '<=' | '>=') expression #BinaryOp
    | expression op=('==' | '!=') #BinaryOp
    | expression '&' expression #BinaryOp
    | expression '|' expression #BinaryOp
    | expression '&&' expression #BinaryOp
    | expression '||' expression #BinaryOp
    | expression '[' expression ']' #BinaryOp
    | expression '.' 'length' #UnaryOp
    | expression '.' ID '(' ( expression ( ',' expression )* )? ')' #TernaryOp
    | 'new' 'int' '[' expression ']' #ArrayNew
    | 'new' ID '('')' #ClassNew
    | '!' expression #UnaryOp
    | '(' expression ')' #UnaryOp
    | value=INTEGER #Integer
    | 'true' #Boolean
    | 'false' #Boolean
    | value=ID #IdentifierExpr
    | 'this' #ThisExpr
    ;

