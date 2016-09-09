response=`curl https://api.travis-ci.org/repos/sharethrough/Android-SDK/branches/master`

state=`echo $response | jq '.branch.state'`

if [ "$state" = '"passed"' ]
	then
	echo build has passed
	exit 0
elif [ "$state" = '"created"' ]
	then
	echo build is pending
    exit 0
else 
	echo failed
	exit 1
fi
