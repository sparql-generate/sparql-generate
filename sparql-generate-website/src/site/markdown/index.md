# What is SPARQL-Generate ?

RDF aims at being the universal abstract data model for structured data on the Web. While there is effort to convert data in RDF, the vast majority of data available on the Web does not conform to RDF. Indeed, exposing data in RDF, either natively or through wrappers, can be very costly. Furthermore, in the emerging Web of Things, resource constraints of devices prevent from processing RDF graphs. Hence one cannot expect that all the data on the Web be available as RDF anytime soon. 

SPARQL-Generate is an extension of SPARQL for querying not only RDF datasets but also documents in arbitrary formats.

SPARQL-Generate has a first reference implementation on top of Apache Jena, which currently enables to query and transform web documents in XML, JSON, CSV, HTML, CBOR, and plain text with regular expressions.

**To cite our work:**

> Maxime Lefrançois, Antoine Zimmermann, Noorani Bakerally _A SPARQL extension for generating RDF from heterogeneous formats_, In Proc. Extended Semantic Web Conference, ESWC, May 2017, Portoroz, Slovenia (long paper - [PDF](http://www.maxime-lefrancois.info/docs/LefrancoisZimmermannBakerally-ESWC2017-Generate.pdf) - [BibTeX](LefrancoisZimmermannBakerally-ESWC2017-SPARQL.bib))

> Maxime Lefrançois, Antoine Zimmermann, Noorani Bakerally _Flexible RDF generation from RDF and heterogeneous data sources with SPARQL-Generate_, In Proc. the 20th International Conference on Knowledge Engineering and Knowledge Management, EKAW, Nov 2016, Bologna, Italy (demo track - [PDF](http://www.maxime-lefrancois.info/docs/LefrancoisZimmermannBakerally-EKAW2016-Flexible.pdf) - [BibTeX](LefrancoisZimmermannBakerally-EKAW2016-Flexible.bib))

> Maxime Lefrançois, Antoine Zimmermann, Noorani Bakerally _Génération de RDF à partir de sources de données aux formats hétérogènes_, Actes de la 17ème conférence Extraction et Gestion des Connaissances, EGC, Jan 2017, Grenoble, France - ([PDF](http://www.maxime-lefrancois.info/docs/LefrancoisZimmermannBakerally-EGC2017-Generation.pdf) - [BibTeX](LefrancoisZimmermannBakerally-EGC2017-Generation.bib))


## A language to generate RDF from RDF datasets and documents in arbitrary formats

SPARQL-Generate is an extension of SPARQL 1.1 for querying not only RDF datasets but also documents in arbitrary formats. It offers a simple template-based option to generate RDF Graphs from documents, and presents the following advantages:

- Anyone familiar with SPARQL can easily learn SPARQL-Generate;
- SPARQL-Generate leverages the expressivity of SPARQL 1.1: Aggregates, Solution Sequences and Modifiers, SPARQL functions and their extension mechanism.
- It integrates seamlessly with existing standards for consuming Semantic Web data, such as SPARQL or Semantic Web programming frameworks.

See also:

* [the language specification](language.html);
* [a form to test SPARQL-Generate online](language-form.html);

## A first implementation on top of Apache Jena

Since we leverage the expressiveness of SPARQL and its function extension mechanism, its implementation on top of a SPARQL engine is straightforward. This website describes a first implementation over Apache Jena, which currently enables to query and transform web documents in XML, JSON, CSV, HTML, CBOR, and plain text with regular expressions.

All these formats are supported thanks to [our predefined SPARQL binding functions and SPARQL-Generate iterator functions](functions.html), but of course you can leverage the SPARQL 1.1 extension mechanism and implement your own functions to support any other format.

You can start using SPARQL-Generate as:

* [an executable JAR](language-cli.html);
* [a Java library](get-started.html) with its [reference Java documentation](apidocs/index.html);
* [a Web API](language-api.html).
* [a comparative evaluation with the RML reference implementation](evaluation.html).

Our [tests report](tests-reports.html) contains tests from related work and more. They are automatically as examples to the [online form](language-form.html). You can request a new unit test, via the [mailing list](mail-lists.html) or the [issue tracker](issue-tracking.html)


## Applications

SPARQL-Generate is already in use in the following projects:

* In the ITEA2 12004 SEAS project:
    * with the [CNR](www.cnr.tm.fr), their [Electric Vehicle Smart Charging Provider API](http://cnr-seas.cloudapp.net/scp) exposes optimized charge plan in XML, but also sends a link to a SPARQL-Generate query, so that the client can interpret the response as RDF.
    * ongoing work with [GECAD ](http://gecad.isep.ipp.pt): serve the consumption data of the Instituto Politecnico de Porto microgrid as CBOR, and point to the SPARQL-Generate `GENERATE` query that enables to interpret it as RDF. 
    * some modules of the [SEAS ontology](https://w3id.org/seas/) are generated out of JSON configuration files. SPARQL-Generate is used to generate a whole part of a vocabulary from a configuration file, and ensure that specific naming conventions are respected for resource URIs, labels and comments;
* In the [OpenSensingCity French ANR 14-CE24-0029 project](http://opensensingcity.emse.fr/):
    * ongoing work: generate RDF from several open data sources for the [Grand Lyon](www.grandlyon.com/).

## Contribute

Don't hesitate to request for any binding function or iterator function addition, via the [mailing list](mail-lists.html) or the [issue tracker](issue-tracking.html).

## Acknowledgments

This work has been partly funded by the ITEA2 12004 SEAS (Smart Energy Aware Systems) project, the ANR 14-CE24-0029 OpenSensingCity project, and a bilateral research convention with ENGIE R&D.
