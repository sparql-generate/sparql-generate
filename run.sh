#!/bin/bash

cd sparql-generate-jena && mvn install -Dskip
cd ../sparql-generate-website && mvn jetty:run

