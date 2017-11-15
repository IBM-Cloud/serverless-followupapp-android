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

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.ibm.bluemix.appid.android.api.AppID;
import com.ibm.bluemix.appid.android.api.AppIDAuthorizationManager;
import com.ibm.bluemix.appid.android.api.AuthorizationException;
import com.ibm.bluemix.appid.android.api.LoginWidget;
import com.ibm.bluemix.appid.android.api.tokens.AccessToken;
import com.ibm.bluemix.appid.android.api.tokens.IdentityToken;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPSimplePushNotification;
import com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants;
import com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushUtils;
import com.vlonjatg.progressactivity.ProgressRelativeLayout;

import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.DEVICE_ID;

/**
 * This is the App front page activity.
 * It demonstrates the use of {@link AppID} for two forms of authorization:
 * 1. loginAnonymously for creating a guest user profile.
 * 2. Using the loginWidget to log in through identity providers authentication. This could create a new user profile or
 * provide access to an existing one.
 * In both cases App Id generates and returns Access and Identity tokens. The Identity token provides information
 * about the user which could come from the Identity Provider (e.g. facebook, google...) and the access token can be used
 * to access the profile attributes.
 * <p>
 * This sample also demonstrates how a token can be stored on the device and reused when coming back to the app.
 */
public class MainActivity extends AppCompatActivity {

    private final static String region = AppID.REGION_US_SOUTH;

    private AppID appId;
    private AppIDAuthorizationManager appIDAuthorizationManager;
    private TokensPersistenceManager tokensPersistenceManager;
    private ProgressRelativeLayout progressActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ServerlessAPI.initialize(getString(R.string.serverlessBackendUrl));

        appId = AppID.getInstance();
        appId.initialize(this, getString(R.string.authTenantId), region);

        this.appIDAuthorizationManager = new AppIDAuthorizationManager(this.appId);
        tokensPersistenceManager = new TokensPersistenceManager(this, appIDAuthorizationManager);

        progressActivity = (ProgressRelativeLayout) findViewById(R.id.activity_main);
        progressActivity.showLoading();

        // Initialize the SDK
        BMSClient.getInstance().initialize(this, BMSClient.REGION_US_SOUTH);

        //Initialize client Push SDK
        final MFPPush push = MFPPush.getInstance();
        push.initialize(getApplicationContext(), getString(R.string.pushAppGuid), getString(R.string.pushClientSecret));

        //Register Android devices
        push.registerDevice(new MFPPushResponseListener<String>() {
            @Override
            public void onSuccess(String response) {
                //handle successful device registration here
                Log.i(MainActivity.class.getName(), "Registered device for push notifications: " + response);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressActivity.showContent();
                    }
                });
            }

            @Override
            public void onFailure(MFPPushException ex) {
                //handle failure in device registration here
                Log.i(MainActivity.class.getName(), "Failed to register device for push notifications", ex);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressActivity.showContent();
                    }
                });
            }
        });

    }

    /**
     * Continue as guest action
     *
     * @param v
     */
    public void onAnonymousClick(View v) {
        progressActivity.showLoading();

        Log.d(logTag("onAnonymousClick"), "Attempting anonymous authorization");

        final String storedAccessToken = tokensPersistenceManager.getStoredAnonymousAccessToken();
        AppIdSampleAuthorizationListener appIdSampleAuthorizationListener =
                new AppIdSampleAuthorizationListener(this, appIDAuthorizationManager, true) {
                    @Override
                    public void onAuthorizationFailure(AuthorizationException exception) {
                        super.onAuthorizationFailure(exception);
                        progressActivity.showContent();
                    }

                    @Override
                    public void onAuthorizationCanceled() {
                        super.onAuthorizationCanceled();
                        progressActivity.showContent();
                    }
                };
        appId.loginAnonymously(getApplicationContext(), storedAccessToken, appIdSampleAuthorizationListener);
    }

    /**
     * Log in with identity provider authentication action
     *
     * @param v
     */
    public void onLoginClick(View v) {
        progressActivity.showLoading();

        Log.d(logTag("onLoginClick"), "Attempting identified authorization");
        LoginWidget loginWidget = appId.getLoginWidget();
        final String storedAccessToken;
        storedAccessToken = tokensPersistenceManager.getStoredAccessToken();

        AppIdSampleAuthorizationListener appIdSampleAuthorizationListener =
                new AppIdSampleAuthorizationListener(this, appIDAuthorizationManager, false) {
                    @Override
                    public void onAuthorizationFailure(AuthorizationException exception) {
                        super.onAuthorizationFailure(exception);
                        progressActivity.showContent();
                    }

                    @Override
                    public void onAuthorizationCanceled() {
                        super.onAuthorizationCanceled();
                        progressActivity.showContent();
                    }
                };

        loginWidget.launch(this, appIdSampleAuthorizationListener, storedAccessToken);
    }

    private String logTag(String methodName) {
        return getClass().getCanonicalName() + "." + methodName;
    }
}
