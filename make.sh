#!/bin/bash

websitepath="sparql-generate-website/target/sparql-generate.war"
echo "websitepath is " $websitepath
echo ""

echo "run maven"
mvn clean package -P docs
rc=$?
if [[ $rc -ne 0 ]] ; then
  echo 'could not run maven'; exit $rc
fi

echo ""
echo "delete existing sparql-generate.war on the server"
ssh ci.emse.fr "cd /var/www/glassfish-apps/ 
rm sparql-generate-old.war
mv sparql-generate.war sparql-generate-old.war"

echo "send sparql-generate.war on the server"
scp ${websitepath} "root@ci.emse.fr:/var/www/glassfish-apps/sparql-generate.war"

echo ""
echo "deploy sparql-generate.war on the server"
ssh ci.emse.fr "/opt/glassfish4/glassfish/bin/asadmin login && /opt/glassfish4/glassfish/bin/asadmin deploy --force /var/www/glassfish-apps/sparql-generate.war"

echo "ok"
echo
