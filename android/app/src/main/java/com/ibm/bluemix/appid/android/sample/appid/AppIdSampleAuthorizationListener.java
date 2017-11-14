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
package com.ibm.bluemix.appid.android.sample.appid;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.ibm.bluemix.appid.android.api.AppIDAuthorizationManager;
import com.ibm.bluemix.appid.android.api.AuthorizationException;
import com.ibm.bluemix.appid.android.api.AuthorizationListener;
import com.ibm.bluemix.appid.android.api.tokens.AccessToken;
import com.ibm.bluemix.appid.android.api.tokens.IdentityToken;

/**
 * This listener provides the callback methods that are called at the end of App ID
 * authorization process when using the {@link com.ibm.bluemix.appid.android.api.AppID} login APIs
 */
public class AppIdSampleAuthorizationListener implements AuthorizationListener {
    private TokensPersistenceManager tokensPersistenceManager;
    private boolean isAnonymous;
    private Activity activity;

    public AppIdSampleAuthorizationListener(Activity activity, AppIDAuthorizationManager authorizationManager, boolean isAnonymous) {
        tokensPersistenceManager = new TokensPersistenceManager(activity, authorizationManager);
        this.isAnonymous = isAnonymous;
        this.activity = activity;
    }

    @Override
    public void onAuthorizationFailure(AuthorizationException exception) {
        Log.e(logTag("onAuthorizationFailure"),"Authorization failed", exception);
    }

    @Override
    public void onAuthorizationCanceled() {
        Log.w(logTag("onAuthorizationCanceled"),"Authorization canceled");
    }

    @Override
    public void onAuthorizationSuccess(AccessToken accessToken, IdentityToken identityToken) {
        Log.i(logTag("onAuthorizationCanceled"),"Authorization succeeded");

        Intent intent = new Intent(activity, AfterLoginActivity.class);
        //storing the new token
        tokensPersistenceManager.persistTokensOnDevice();

        //register the user with the backend
        try {
            ServerlessAPI.instance().register(accessToken, identityToken, "DEVICE_ID");
        } catch (Exception e) {
            e.printStackTrace();
        }

        activity.startActivity(intent);
        activity.finish();
    }

    private String logTag(String methodName){
        return this.getClass().getCanonicalName() + "." + methodName;
    }
}
