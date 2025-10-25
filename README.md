# ChaTravel
Chat travel app created for Udemy course "Master the Android System Design Interview"

## Downloading GraphQL schema from server
Run the ChaTravel server on localhost, and then execute this gradle command to download the latest schema:
./gradlew downloadApolloSchema \
--endpoint="http://0.0.0.0:8080/graphql" \
--schema="app/src/main/graphql/chatravel/schema.json"
