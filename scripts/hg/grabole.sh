#!/bin/bash
source /etc/bs-tester.conf

REPO=$INSTALL_DIR/repo
USER=`ls -l $REPO | awk '{split($0,a," ")} END{print a[3]}'`
CMD_PREFIX="sudo -u $USER"

pushd $REPO
TEAM_A=$1
TEAM_B=$2
$CMD_PREFIX hg revert -a
# Get team a
rm -rf teams/old_team_a
$CMD_PREFIX hg checkout $TEAM_A
pushd teams
cp -r $TEAM old_team_a
cd old_team_a
find -name '*.java' | xargs sed -i -e 's/package team[0-9]\{3\}/package old_team_a/g'
find -name '*.java' | xargs sed -i -e 's/import team[0-9]\{3\}/import old_team_a/g'
popd

# Get team b
rm -rf teams/old_team_b
$CMD_PREFIX hg checkout $TEAM_B
pushd teams
cp -r $TEAM old_team_b
cd old_team_b
find -name '*.java' | xargs sed -i -e 's/package team[0-9]\{3\}/package old_team_b/g'
find -name '*.java' | xargs sed -i -e 's/import team[0-9]\{3\}/import old_team_b/g'
popd
$CMD_PREFIX hg checkout tip

popd
