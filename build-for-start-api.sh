#!/bin/sh
echo 'execute `gradle clean build` and `copy zip file to ./`;'
cd LambdaForZaim && gradle clean build && cp build/distributions/LambdaForZaim.zip ../lambdaForZaim.zip