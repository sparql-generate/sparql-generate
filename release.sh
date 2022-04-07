# this script must be run in the main directory of sparql-generate. it:
# - removes the -SNAPSHOT from the version of all repositories
# - commits and push the tag to github
# - releases the version on maven central
# - increments the patch version and adds -SNAPSHOT
# - pushes the new development version
# - install the new version to get ready to develop

if [ -z "$(git status --porcelain)" ]; then 
  # Working directory clean
  mvn -B versions:set -DremoveSnapshot --file 'sparql-generate-parent/pom.xml' && \
  releaseVersion=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec --file 'sparql-generate-parent/pom.xml') && \
  git commit -am "release $releaseVersion" && \
  git tag -d v$releaseVersion
  git tag -s "v$releaseVersion" -m "release $releaseVersion" && \
  git push --delete origin v$releaseVersion
  git push
  git push origin v$releaseVersion && \
  mvn deploy -P deploy
  # nextVersion=$(semver -i patch $releaseVersion)-SNAPSHOT && \
  # mvn -B versions:set -DnewVersion=$nextVersion --file 'sparql-generate-parent/pom.xml' && \
  # git commit -am "prepare next version $nextVersion" && \
  # mvn -B install --file 'sparql-generate-parent/pom.xml'
else 
 echo "working directory is not clean. Commit first."
fi
