#!/bin/sh
source .env
sam build
array1=("ConsumerKey" "ConsumerSecret" "AccessToken" "AccessTokenSecret" "SlackBotUserAccessToken" "SlackChannelId")
array2=($CONSUMER_KEY $CONSUMER_SECRET $ACCESS_TOKEN $ACCESS_TOKEN_SECRET $SLACK_BOT_USER_ACCESS_TOKEN $SLACK_CHANNEL_ID)
forNum=$((${#array1[@]}-1))
for i in `seq 0 ${forNum}`
do
  # params="$params ParameterKey=${array1[$i]},ParameterValue=${array2[$i]}"
  params="$params ${array1[$i]}=${array2[$i]}"
done
echo $params
sam deploy \
  --region ap-northeast-1 \
  --stack-name ZaimFunction \
  --debug \
  --parameter-overrides \
  ${params}
