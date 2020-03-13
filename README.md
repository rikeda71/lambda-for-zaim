# Lambda-for-Zaim

WIP

## Requirements

- AWS Account
- aws-sam-cli

## Usage

### local test

1. create `.env.json` from `.env.json.sample`

```shell script
$ cp .env.json.sample .env.json
```

2. edit `.env.json`

```
CONSUMER_KEY": Consumer key of Zaim
CONSUMER_SECRET": Consumer secret key of Zaim
ACCESS_TOKEN": Access token of Zaim
ACCESS_TOKEN_SECRET": Secret access token of Zaim
```

3. execute following commands

```shell script
$ sam build
$ sam local invoke -e events/event1.json -n .env.json
```
