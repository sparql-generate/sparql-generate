function clean {
  mvn -B clean --file sparql-generate-parent/pom.xml
  rm -rf sparql-generate-website/public
}

function echoredirect {
  path=${3%/*}
  javafile=${3##.*/}
  basename=${javafile%\.java}
  localname=${basename#$2_}
  url="/sparql-generate/$1/${path#*main/java/}/${basename}.html"
  echo "RewriteRule $1/${localname//_/-} \"$url\" [R=302,NC]";
}

function build {
  mvn -B install -P docs --file sparql-generate-parent/pom.xml
  cd sparql-generate-website 
  npm i
  gulp
  cd ..
  javadoc $(find -name 'ITER_*.java' | paste -sd " ") -d sparql-generate-website/public/iter -notree -nohelp -nonavbar  -public -nodeprecated -nodeprecatedlist -encoding utf-8 -windowtitle 'SPARQL-Generate - Iterator functions' -header 'SPARQL-Generate - Iterator functions' -bottom '<p class="legalCopy"><small>Copyright &#169; 2016&#x2013;2020 <a href="http://www.mines-stetienne.fr/">MINES Saint-Étienne</a>. All rights reserved.</small></p>' 
  javadoc $(find -name 'FUN_*.java' | paste -sd " ") -d sparql-generate-website/public/fn -notree -nohelp -nonavbar  -public -nodeprecated -nodeprecatedlist -encoding utf-8 -windowtitle 'SPARQL-Generate - Binding functions' -header 'SPARQL-Generate - Binding functions' -bottom '<p class="legalCopy"><small>Copyright &#169; 2016&#x2013;2020 <a href="http://www.mines-stetienne.fr/">MINES Saint-Étienne</a>. All rights reserved.</small></p>' 
  export -f echoredirect
  find -name 'ITER_*.java' -exec bash -c 'echoredirect iter ITER "$0"' {} \; >> sparql-generate-website/public/.htaccess
  find -name 'FUN_*.java' -exec bash -c 'echoredirect fn FUN "$0"' {} \; >> sparql-generate-website/public/.htaccess
}

function push {
  scp -i ~/.ssh/sparql-generate sparql-generate-server/target/sparql-generate.war sparql-generate@ci.mines-stetienne.fr:~/sparql-generate.war
  scp -r -i ~/.ssh/sparql-generate sparql-generate-website/public/* sparql-generate@ci.mines-stetienne.fr:~/website
  scp -r -i ~/.ssh/sparql-generate sparql-generate-website/public/.htaccess sparql-generate@ci.mines-stetienne.fr:~/website/.htaccess
  scp -r -i ~/.ssh/sparql-generate sparql-generate-all/target/apidocs sparql-generate@ci.mines-stetienne.fr:~/website
  ssh -i ~/.ssh/sparql-generate sparql-generate@ci.mines-stetienne.fr "sudo ./update-app"
}

if [[ $# == 0 ]]; then
  echo "usage ./update-app.sh [clean] [build] [push]";
fi

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
