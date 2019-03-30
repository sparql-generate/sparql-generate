#!/bin/bash

finalName="sparql-generate"
port="8106"


echo "run maven"
mvn clean package -P docs
rc=$?
if [[ $rc -ne 0 ]] ; then
  echo 'could not run maven'; exit $rc
fi

# prepare
echo "prepare"

ssh -t ci.emse.fr "sudo mkdir -p /var/www/$finalName/log && sudo chown -R jetty:jetty /var/www/$finalName/log/
cd /var/www/$finalName
sudo rm $finalName-old.war
sudo mv $finalName.war $finalName-old.war"

scp sparql-generate-website/target/$finalName.war ci.emse.fr:~/$finalName.war
ssh -t ci.emse.fr "sudo mv ~/$finalName.war /var/www/$finalName/$finalName.war
# clean 
sudo docker stop $finalName 
sudo docker rm $finalName 
# run
sudo docker run -d -p $port:8080 -v /var/www/$finalName/log:/var/lib/jetty/log --restart unless-stopped --name $finalName jetty --module=websocket
sudo docker cp /var/www/$finalName/$finalName.war $finalName:/var/lib/jetty/webapps/$finalName.war"
