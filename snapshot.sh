# this script must be run in the main directory of sparql-generate. it:
# - deletes the tag online
# - commits and push the tag to github
# - releases the SNAPSHOT version on maven central

if [ -z "$(git status --porcelain)" ]; then 
  # Working directory clean
  snapshotVersion=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec --file 'sparql-generate-parent/pom.xml') && \
  git tag -d v$snapshotVersion
  git tag -s "v$snapshotVersion" -m "snapshot $snapshotVersion"
  git push --delete origin v$snapshotVersion
  git push origin v$snapshotVersion
#  mvn -B deploy -P deploy --file 'sparql-generate-parent/pom.xml'
else 
 echo "working directory is not clean. Commit first."
fi