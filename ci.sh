response=`curl https://api.travis-ci.org/repos/sharethrough/Android-SDK/branches/master`

state=`echo $response | jq '.branch.state'`
echo $state

if [ "$state" = '"failed"' ]
	then
	echo build has failed
	exit 1
else 
	echo build has passed or is pending
	exit 0
fi
