# Grammar

The grammar of SPARQL-Generate extends the grammar of SPARQL 1.1. Its normative source is the JavaCC grammar of the engine, [`spargl.jj`](https://github.com/sparql-generate/sparql-generate/blob/master/sparql-generate-jena/src/main/javacc/spargl.jj). The website gives it [in EBNF](https://ci.mines-stetienne.fr/sparql-generate/grammar.html). The ESWC 2017 paper (see [Publications](../publications.md)) defines the grammar and the semantics of the first version of the language.

The main productions added to SPARQL 1.1 are the following.

```text
ExtQuery        ::= Prologue ( ( SelectQuery | GenerateQuery | TemplateQuery | PerformQuery ) ValuesClause | Function )
GenerateQuery   ::= 'GENERATE' ( iri VarList? )? GenerateClause DatasetClause* BindingClauses*
                    WhereClause? SolutionModifier PostSelectClause?
TemplateQuery   ::= 'TEMPLATE' ( iri VarList? )? TemplateClause DatasetClause* BindingClauses*
                    ( ( 'DISTINCT' | 'REDUCED' )? WhereClause )? SolutionModifier
SelectQuery     ::= 'SELECT' ( iri VarList? )? ( 'DISTINCT' | 'REDUCED' )? ( ( Var | '(' Expression 'AS' Var ')' )+ | '*' )
                    DatasetClause* BindingClauses* WhereClause SolutionModifier
Function        ::= 'FUNCTION' iri VarList '{' PrimaryExpression '}'
VarList         ::= NIL | '(' Var ( ',' Var )* ')'
BindingClauses  ::= IteratorClause | SourceClause | Bind
IteratorClause  ::= 'ITERATOR' FunctionCall 'AS' Var+
SourceClause    ::= 'SOURCE' VarOrXIri ( 'ACCEPT' VarOrXIri )? 'AS' Var
GenerateClause  ::= '{' ConstructTriples? ( SubGenerateQuery '.' ConstructTriples? )* '}'
SubGenerateQuery ::= 'GENERATE' ( VarOrXIri ArgList? | GenerateClause ) DatasetClause* BindingClauses*
                    ( 'WHERE' GroupGraphPattern )? SolutionModifier PostSelectClause?
TemplateClause  ::= '{' ( 'BEFORE' '=' Expression ';' )? TExpression*
                    ( ';' ( 'SEPARATOR' '=' Expression ( ';' 'AFTER' '=' Expression )? | 'AFTER' '=' Expression ) )? '}'
TExpression     ::= PrimaryExpressionNotBracketted | Box | Format | Group | SubTemplateQuery '.'
Box             ::= 'BOX' '{' TExpression* '}'
Format          ::= 'FORMAT' '{' PrimaryExpressionNotBracketted TExpression+ '}'
Group           ::= 'GROUP' 'DISTINCT'? '{' ( PrimaryExpressionNotBracketted | Box | Format )* ( ';' 'SEPARATOR' '=' String )? '}'
DatasetClause   ::= 'FROM' ( Expression | 'NAMED' Expression | SubGenerateQuery ( 'NAMED' Expression )? '.' )
PostSelectClause ::= 'EXPRESSIONS' ( '(' Expression 'AS' Var ')' )+
ListNode        ::= 'LIST' '(' Expression ')'
XExpr           ::= ( '?{' | '${' ) Expression '}'
```

In templates, IRIs and literals can contain expressions between `{` and `}` (`XIri`, `XRDFLiteral` in the JavaCC grammar).
