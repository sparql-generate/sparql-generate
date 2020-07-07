# this script must be run in the main directory of sparql-generate. it:
# - deletes the tag online
# - commits and push the tag to github
# - releases the SNAPSHOT version on maven central

if [ -z "$(git status --porcelain)" ]; then 
  # Working directory clean
  cd sparql-generate-parent && \
  snapshotVersion=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec) && \
  git commit -am "new snapshot $snapshotVersion" && \
  git tag -s "v$snapshotVersion" -m "snapshot $snapshotVersion" && \
  git push --delete origin v$snapshotVersion && \
  git push origin v$snapshotVersion && \
  mvn deploy -P deploy
  cd ..
else 
 echo "working directory is not clean. Commit first.
fi