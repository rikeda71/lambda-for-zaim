import os
import shutil

from collections import defaultdict
from requests_oauthlib import OAuth1Session
from dotenv import load_dotenv

load_dotenv(".env")

CONSUMER_KEY = os.environ.get("CONSUMER_KEY")
CONSUMER_SECRET = os.environ.get("CONSUMER_SECRET")
REQUEST_TOKEN_URL = "https://api.zaim.net/v2/auth/request"
AUTHORIZATION_URL = "https://auth.zaim.net/users/auth"
ACCESS_TOKEN_URL = "https://api.zaim.net/v2/auth/access"
CALLBACK_URL = "https://zaim.net/"

if __name__ == "__main__":
    oauth = OAuth1Session(
        client_key=CONSUMER_KEY,
        client_secret=CONSUMER_SECRET,
        callback_uri=CALLBACK_URL,
    )

    oauth.fetch_request_token(REQUEST_TOKEN_URL)
    authorization_url = oauth.authorization_url(AUTHORIZATION_URL)
    print("Please jump this link and authoriza: ", authorization_url)

    # The user input oauth verifier to command line
    verifier = input("Please input `oauth verifier`: ")
    access_token_response = oauth.fetch_access_token(
        url=ACCESS_TOKEN_URL, verifier=verifier
    )
    access_token = access_token_response.get("oauth_token")
    access_token_secret = access_token_response.get("oauth_token_secret")

    print("access token:\t", access_token)
    print("access token secret:\t", access_token_secret)

    # update `.env`
    if not os.path.exists(".env"):
        shutil.copy(".env.sample", ".env")
    with open(".env", "r") as f:
        lines = [line.replace("\n", "").split("=") for line in f.readlines()]
    dic = defaultdict(str)
    for line in lines:
        dic[line[0]] = line[1]
    dic["ACCESS_TOKEN"] = access_token
    dic["ACCESS_TOKEN_SECRET"] = access_token_secret
    with open(".env", "w") as f:
        f.write("\n".join([f"{k}={v}" for k, v in dic.items()]))
    print("updated `.env`")
