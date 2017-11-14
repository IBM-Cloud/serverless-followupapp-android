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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.ibm.bluemix.appid.android.api.AppID;
import com.ibm.bluemix.appid.android.api.AppIDAuthorizationManager;
import com.ibm.bluemix.appid.android.api.tokens.AccessToken;
import com.ibm.bluemix.appid.android.api.tokens.IdentityToken;
import com.ibm.bluemix.appid.android.api.userattributes.UserAttributesException;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.URL;

/**
 * This Activity starts after pressing on "login" or "continue as guest" buttons.
 */
public class AfterLoginActivity extends AppCompatActivity {

    private AppID appID;

    private AppIDAuthorizationManager appIDAuthorizationManager;
    private TokensPersistenceManager tokensPersistenceManager;

    private JSONArray jaFoodSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_login);

        appID = AppID.getInstance();

        appIDAuthorizationManager = new AppIDAuthorizationManager(appID);
        tokensPersistenceManager = new TokensPersistenceManager(this, appIDAuthorizationManager);

        IdentityToken idt = appIDAuthorizationManager.getIdentityToken();

        //Getting information from identity token. This is information that is coming from the identity provider.
        String userName = idt.getEmail() != null ? idt.getEmail().split("@")[0] : "Guest";
        if (idt.getName() != null)
            userName = idt.getName();

        String profilePhotoUrl = idt.getPicture();

        //Setting identity data to UI
        ((TextView) findViewById(R.id.userName)).setText(getString(R.string.greet) + " " + userName);
        setProfilePhoto(profilePhotoUrl);

        getSupportActionBar().hide();
    }


    public void onSubmitClick(View v) {
        final EditText feedbackTextView = (EditText) findViewById(R.id.yourFeedbackText);
        final String feedbackText = feedbackTextView.getText().toString();
        System.out.println("Send feedback " + feedbackText);
        new DoWithProgress(this) {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    ServerlessAPI.instance().sendFeedback(appIDAuthorizationManager.getAccessToken(), feedbackText);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                feedbackTextView.setText(null);
            }
        }.execute();
    }

    /**
     * Show textual representation of Access and Identity tokens
     *
     * @param v
     */
    public void onTokenViewClick(View v) {
        Intent intent = new Intent(this, TokenActivity.class);

        IdentityToken idt = appIDAuthorizationManager.getIdentityToken();
        AccessToken at = appIDAuthorizationManager.getAccessToken();
        try {
            intent.putExtra("idToken", idt.getPayload().toString(2));
            intent.putExtra("accessToken", at.getPayload().toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        startActivity(intent);
    }

    private void handleAppIdError(UserAttributesException e) {
        switch (e.getError()) {
            case FAILED_TO_CONNECT:
                throw new RuntimeException("Failed to connect to App ID to access profile attributes", e);
            case UNAUTHORIZED:
                throw new RuntimeException("Not authorized to access profile attributes at App ID", e);
        }
    }

    private void setProfilePhoto(final String photoUrl) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bmp = photoUrl == null || photoUrl.length() == 0 ? null :
                            BitmapFactory.decodeStream(new URL(photoUrl).openConnection().getInputStream());
                    //run on main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ImageView profilePicture = (ImageView) findViewById(R.id.profilePic);
                            if (bmp == null) {
                                profilePicture.setImageResource(R.mipmap.ic_anon);
                            } else {
                                profilePicture.setImageBitmap(Utils.getRoundedCornerBitmap(bmp, 100));
                            }

                            profilePicture.requestLayout();
                            profilePicture.setScaleType(ImageView.ScaleType.FIT_XY);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String logTag(String methodName) {
        return getClass().getCanonicalName() + "." + methodName;
    }
}
