# serverless-followupapp-android

:warning: work in progress

A mobile follow-up app using a serverless backend in Java


## Provision a NoSQL Service and Create databases.

1. Create service instance

   ```
   bx cf create-service cloudantNoSQLDB Lite serverless-followupapp-db
   ```

1. Create credentials to use the service with Cloud Functions

   ```
   bx cf create-service-key serverless-followupapp-db for-cli
   ```

1. Retrieve the credentials

   ```
   bx cf service-key serverless-followupapp-db for-cli
   ```

1. Create databases

   ```
   curl -X PUT https://account:password@acccount-bluemix.cloudant.com/feedback
   curl -X PUT https://account:password@acccount-bluemix.cloudant.com/moods
   curl -X PUT https://account:password@acccount-bluemix.cloudant.com/users
   ```

## Provision a Cognitive Service.

1. Create instance

   ```
   bx cf create-service tone_analyzer standard serverless-followupapp-tone
   ```

1. Create credentials to use the service with Cloud Functions

   ```
   bx cf create-service-key serverless-followupapp-tone for-cli
   ```

## Provision App ID Service

1. Create instance

   ```
   bx cf create-service AppID "Graduated tier" serverless-followupapp-appid
   ```

1. Create credentials to use the service with Cloud Functions

   ```
   bx cf create-service-key serverless-followupapp-appid for-cli
   ```

## Provision a Push Notifications Service

1. Create instance

   ```
   bx cf create-service imfpush lite serverless-followupapp-mobilepush
   ```

1. Create credentials to use the service with Cloud Functions

   ```
   bx cf create-service-key serverless-followupapp-mobilepush for-cli
   ```

## Configure Push Notifications Service.

## Clone the mobile app project

## Create Serverless actions.

### A package for all actions

-> has access to all service credentials

### Register the user as a feedback submitter

a sequence exposed as a web action PUT verb
  input has header Authorization: Bearer {accessToken} {idToken}
  input has device ID to be used by push notifications

action **validate_token**
  retrieve accessToken and idToken from Authorization header
  verify accessToken and idToken either by using the public key for the App ID instance
  output the input args + decoded tokens

action **create_user**
  insert or update the user record in the database
  save the user device ID for push notifications
  use the "sub" parameter of the tokens as user identifier
  output the created/updated user

### Post feedback

a sequence exposed as a web action PUT verb
  required Authorization: Bearer {accessToken}

action **validate_token**
action **put_feedback**
  retrieve the user associated with the feedback by looking at the sub of the Authorization token
  store a new feedback document, setting the user_id

### Analyze feedback

with a trigger in response to a new document in the feedback database
  load the feedback
  load the user
  call tone analysis
  find the associated mood
  send a push notification to the user

