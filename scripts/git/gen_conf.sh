#!/bin/bash
if [ -e /etc/battlecode.conf ]; then
	source /etc/battlecode.conf
else
	source etc/battlecode.conf
fi
MAP=$1
cd $REPO
cat bc.conf | sed -e 's/bc.game.maps=.*/bc.game.maps='"$MAP"'/' -e 's/bc.game.team-a=.*/bc.game.team-a=old_team_a/' -e 's/bc.game.team-b=.*/bc.game.team-b=old_team_b/' -e 's/bc.server.save-file=.*/bc.server.save-file='"$MAP"'.rms/' > tmp.conf
mv tmp.conf bc.conf
