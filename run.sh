#!/bin/bash

cd sparql-generate && mvn install -Dskip
cd ../sparql-generate-website && mvn jetty:run

