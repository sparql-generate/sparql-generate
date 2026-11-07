# Media type

The media type of a SPARQL-Generate query is `application/vnd.sparql-generate`, with file extension `.rqg`. The engine looks named queries up with this media type.

```text
Type name:            application
Subtype name:         vnd.sparql-generate
Required parameters:  none
Optional parameters:  none

Encoding considerations:
   The syntax of the SPARQL-Generate language is expressed over code points in Unicode.
   The encoding is always UTF-8. Unicode code points may also be expressed using an
   \uXXXX (U+0 to U+FFFF) or \UXXXXXXXX syntax (for U+10000 onwards), where X is a
   hexadecimal digit [0-9A-F].

Security considerations:
   See SPARQL 1.1 Query Language, appendix C, Security Considerations, and RFC 3629,
   section 7, Security Considerations.

Interoperability considerations:  none known
Published specification:          https://w3id.org/sparql-generate/language
Fragment identifier considerations: none

Additional information:
   Magic number(s):   a SPARQL-Generate query may have the string 'PREFIX'
                      (case independent) near the beginning of the document.
   File extension(s): .rqg
   Macintosh file type code(s): TEXT

Person and email address to contact for further information:
   Maxime Lefrançois <maxime.lefrancois@emse.fr>
Intended usage:          COMMON
Restrictions on usage:   none
Author/Change controller: Maxime Lefrançois <maxime.lefrancois@emse.fr>
```
