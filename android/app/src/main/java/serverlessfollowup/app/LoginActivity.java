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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.ibm.cloud.appid.android.api.AppID;
import com.ibm.cloud.appid.android.api.AppIDAuthorizationManager;
import com.ibm.cloud.appid.android.api.LoginWidget;
import com.vlonjatg.progressactivity.ProgressRelativeLayout;

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
public class LoginActivity extends AppCompatActivity {

  private final static String region = AppID.REGION_US_SOUTH;

  private AppID appId;
  private AppIDAuthorizationManager appIDAuthorizationManager;
  private TokensPersistenceManager tokensPersistenceManager;
  private ProgressRelativeLayout progressActivity;

  private LoginAndRegistrationListener registrationListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    ServerlessAPI.initialize(getString(R.string.serverlessBackendUrl));

    appId = AppID.getInstance();
    appId.initialize(this, getString(R.string.authTenantId), region);

    this.appIDAuthorizationManager = new AppIDAuthorizationManager(this.appId);
    tokensPersistenceManager = new TokensPersistenceManager(this, appIDAuthorizationManager);

    progressActivity = (ProgressRelativeLayout) findViewById(R.id.activity_main);
    progressActivity.showContent();

    registrationListener = new LoginAndRegistrationListener(this, tokensPersistenceManager) {
      @Override
      protected void onRegistrationSuccess() {
        super.onRegistrationSuccess();

        // move to the feedback screen
        Log.d(logTag("onRegistrationSuccess"), "Opening Feedback view...");
        Intent intent = new Intent(LoginActivity.this, FeedbackActivity.class);
        LoginActivity.this.startActivity(intent);
        LoginActivity.this.finish();
      }

      @Override
      protected void onRegistrationCanceled() {
        super.onRegistrationCanceled();
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            progressActivity.showContent();
          }
        });
      }

      @Override
      protected void onRegistrationFailure(Throwable th) {
        super.onRegistrationFailure(th);
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            progressActivity.showContent();
          }
        });
      }
    };
  }

  /**
   * Continue as guest action
   */
  public void onAnonymousClick(View v) {
    progressActivity.showLoading();

    Log.d(logTag("onAnonymousClick"), "Attempting anonymous authorization");
    appId.signinAnonymously(getApplicationContext(), registrationListener);
  }

  /**
   * Log in with identity provider authentication action
   */
  public void onLoginClick(View v) {
    progressActivity.showLoading();

    Log.d(logTag("onLoginClick"), "Attempting identified authorization");
    LoginWidget loginWidget = appId.getLoginWidget();
    loginWidget.launch(this, registrationListener);
  }

  private String logTag(String methodName) {
    return getClass().getCanonicalName() + "." + methodName;
  }
}
