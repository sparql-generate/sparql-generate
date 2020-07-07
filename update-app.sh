function clean {
  mvn -B clean --file sparql-generate-parent/pom.xml
  rm -rf sparql-generate-website/public
}

function build {
  mvn -B package -P docs --file sparql-generate-parent/pom.xml
  cd sparql-generate-website 
  npm i
  gulp
  cd ..
}

function push {
  scp -i ~/.ssh/sparql-generate sparql-generate-server/target/sparql-generate.war sparql-generate@ci.mines-stetienne.fr:~/sparql-generate.war
  scp -r -i ~/.ssh/sparql-generate sparql-generate-website/public/* sparql-generate@ci.mines-stetienne.fr:~/website
  scp -r -i ~/.ssh/sparql-generate sparql-generate-all/target/apidocs sparql-generate@ci.mines-stetienne.fr:~/website/apidocs
  ssh -i ~/.ssh/sparql-generate sparql-generate@ci.mines-stetienne.fr "sudo ./update-app"
}


while [[ $# > 0 ]]
do
  case $1 in
    clean)
      clean
      ;;
    build)
      build
      ;;
    push)
      push
      ;;
    *)
      echo "unknown option: $1";;
  esac
  shift
done
