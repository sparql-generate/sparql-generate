@title SPARQL-Generate | IANA Considerations

# IANA considerations.

```
Type name:
   application

Subtype name:
   vnd.sparql-generate

Required parameters:
   None

Optional parameters:
   None

Encoding considerations:
   The syntax of the SPARQL-Generate Language is expressed over code points in Unicode [UNICODE]. The encoding is always UTF-8 [RFC3629].
   Unicode code points may also be expressed using an \uXXXX (U+0 to U+FFFF) or \UXXXXXXXX syntax (for U+10000 onwards) where X is a hexadecimal digit [0-9A-F]

Security considerations:
   See SPARQL Query appendix C, Security Considerations as well as RFC 3629 [RFC3629] section 7, Security Considerations.

Interoperability considerations:
   There are no known interoperability issues.

Published specification:
   https://w3id.org/sparql-generate/language

Fragment identifier considerations:
   None

Additional information:

Magic number(s):
   A SPARQL-Generate query may have the string 'PREFIX' (case independent) near the beginning of the document.

File extension(s): 
   ".rqg"

Macintosh file type code(s): 
   TEXT

Person & email address to contact for further information:
   Maxime Lefrançois <maxime.lefrancois@emse.fr>

Intended usage:
   COMMON

Restrictions on usage:
   None

Author/Change controller:
   Maxime Lefrançois <maxime.lefrancois@emse.com>

The Internet Media type of a SPARQL-Generate query is 'application/vnd.sparql-generate', with file extension '*.rqg'.
```