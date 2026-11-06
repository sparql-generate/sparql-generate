#!/bin/sh
# Run from the root of the repository, on a clean working tree, on master.
#   1. removes -SNAPSHOT from the version of every module;
#   2. commits "release X.Y.Z", creates the signed tag vX.Y.Z and pushes both;
#      the tag triggers .github/workflows/release.yml, which publishes to Maven
#      Central (Central Portal), creates the GitHub release and updates the
#      playground server;
#   3. sets the next development version X.Y.(Z+1)-SNAPSHOT and pushes it.
set -e

if [ -n "$(git status --porcelain)" ]; then
  echo "working directory is not clean. Commit first." >&2
  exit 1
fi

POM=sparql-generate-parent/pom.xml
mvn -B -q versions:set -DremoveSnapshot -DgenerateBackupPoms=false --file $POM
version=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec --file $POM)
git commit -qam "release $version"
git tag -s "v$version" -m "release $version"
git push origin HEAD "v$version"

next=$(echo "$version" | awk -F. '{ printf "%d.%d.%d-SNAPSHOT", $1, $2, $3 + 1 }')
mvn -B -q versions:set -DnewVersion="$next" -DgenerateBackupPoms=false --file $POM
git commit -qam "prepare next version $next"
git push origin HEAD
echo "released $version; the Release workflow publishes it: gh run watch"
