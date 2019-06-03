/*
 * Copyright 2016, 2017 IBM Corp.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package serverlessfollowup.app;

import android.app.Activity;
import android.util.Log;

import com.ibm.cloud.appid.android.api.AuthorizationException;
import com.ibm.cloud.appid.android.api.AuthorizationListener;
import com.ibm.cloud.appid.android.api.tokens.AccessToken;
import com.ibm.cloud.appid.android.api.tokens.IdentityToken;
import com.ibm.cloud.appid.android.api.tokens.RefreshToken;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;

import org.json.JSONObject;

/**
 * Listens for a successful authentication and then triggers the registration of the device
 * with the push notifications service and with the backend API.
 */
public class LoginAndRegistrationListener implements AuthorizationListener {

  private final static String region = BMSClient.REGION_US_SOUTH;

  private TokensPersistenceManager tokensPersistenceManager;
  private Activity activity;

  public LoginAndRegistrationListener(Activity activity, TokensPersistenceManager tokensPersistenceManager) {
    this.tokensPersistenceManager = tokensPersistenceManager;
    this.activity = activity;
  }

  @Override
  public final void onAuthorizationFailure(AuthorizationException exception) {
    Log.e(logTag("onAuthorizationFailure"), "Authorization failed", exception);
    onRegistrationFailure(exception);
  }

  @Override
  public final void onAuthorizationCanceled() {
    Log.w(logTag("onAuthorizationCanceled"), "Authorization canceled");
    onRegistrationCanceled();
  }

  @Override
  public void onAuthorizationSuccess(final AccessToken accessToken, final IdentityToken identityToken, RefreshToken refreshToken) {
    Log.i(logTag("onAuthorizationCanceled"), "Authorization succeeded");

    // keep track of the new tokens
    tokensPersistenceManager.persistTokensOnDevice();

    // initialize the Mobile SDK
    BMSClient.getInstance().initialize(activity, region);

    // initialize the client Push Notifications SDK
    final MFPPush push = MFPPush.getInstance();
    push.initialize(activity.getApplicationContext(),
      activity.getString(R.string.pushAppGuid),
      activity.getString(R.string.pushClientSecret));

    // register this device
    push.registerDevice(new MFPPushResponseListener<String>() {
      @Override
      public void onSuccess(String response) {
        // handle successful device registration here
        Log.i(LoginActivity.class.getName(), "Registered device for push notifications: " + response);

        try {
          // extract the deviceId from the response
          String jsonResponse = response.substring(response.indexOf("Response Text: ") + ("Response Text: ").length(),
            response.length());
          JSONObject jsonOBJ = new JSONObject(jsonResponse);
          String deviceId = jsonOBJ.getString("deviceId");

          // register the user with the backend
          ServerlessAPI.instance().register(accessToken, identityToken, deviceId);

          // move to the next screen
          onRegistrationSuccess();
        } catch (Exception e) {
          onRegistrationFailure(e);
        }
      }

      @Override
      public void onFailure(MFPPushException ex) {
        //handle failure in device registration here
        Log.i(LoginActivity.class.getName(), "Failed to register device for push notifications", ex);
        onRegistrationFailure(ex);
      }
    });
  }

  protected void onRegistrationSuccess() {
  }

  protected void onRegistrationFailure(Throwable th) {
  }

  protected void onRegistrationCanceled() {
  }

  private String logTag(String methodName) {
    return this.getClass().getCanonicalName() + "." + methodName;
  }

}
