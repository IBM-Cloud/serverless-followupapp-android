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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ibm.bluemix.appid.android.api.AppID;
import com.ibm.bluemix.appid.android.api.AppIDAuthorizationManager;
import com.ibm.bluemix.appid.android.api.AuthorizationListener;
import com.ibm.bluemix.appid.android.api.LoginWidget;
import com.ibm.bluemix.appid.android.api.tokens.AccessToken;
import com.ibm.bluemix.appid.android.api.tokens.IdentityToken;
import com.ibm.bluemix.appid.android.api.userattributes.UserAttributeResponseListener;
import com.ibm.bluemix.appid.android.api.userattributes.UserAttributesException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

/**
 * This Activity starts after pressing on "login" or "continue as guest" buttons.
 */
public class AfterLoginActivity extends AppCompatActivity {

    private static String ATTR_BONUS_POINTS = "bonus_points";
    private static int LOGGING_IN_BONUS = 150;

    private AppID appID;

    private AppIDAuthorizationManager appIDAuthorizationManager;
    private TokensPersistenceManager tokensPersistenceManager;
    private NoticeHelper noticeHelper;

    private JSONArray jaFoodSelection;
    private NoticeHelper.AuthState authState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_login);

        appID = AppID.getInstance();

        appIDAuthorizationManager = new AppIDAuthorizationManager(appID);
        tokensPersistenceManager = new TokensPersistenceManager(this, appIDAuthorizationManager);

        noticeHelper = new NoticeHelper(this, appIDAuthorizationManager, tokensPersistenceManager);
        IdentityToken idt = appIDAuthorizationManager.getIdentityToken();

        //Getting information from identity token. This is information that is coming from the identity provider.
        String userName = idt.getEmail() != null ? idt.getEmail().split("@")[0] : "Guest";
        if(idt.getName() != null)
            userName = idt.getName();

        String profilePhotoUrl = idt.getPicture();

        //Setting identity data to UI
        ((TextView) findViewById(R.id.userName)).setText(getString(R.string.greet) +" "+ userName);
        setProfilePhoto(profilePhotoUrl);


        authState = (NoticeHelper.AuthState)getIntent().getSerializableExtra("auth-state");

        if ( !appIDAuthorizationManager.getAccessToken().isAnonymous()) {
            //Identified users get bonus points the first time the first they login.
            getPoints();
        }

        //Creating the attributes list view with adapter
        final ListView listview = (ListView) findViewById(R.id.listviewFood);
        final AppIdAttributesListAdapter adapter = new AppIdAttributesListAdapter(this);

        listview.setAdapter(adapter);
        listview.setOnItemClickListener(adapter.getOnClickListener());

        //Creating a notice to developer
        updateNotice(noticeHelper.getNoticeForState(authState));
        getSupportActionBar().hide();
    }

    /**
     * Present the appropriate developer helper notice
     * @param notice
     */
    private void updateNotice(final String notice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final SpannableStringBuilder str = new SpannableStringBuilder(getString(R.string.notice) +" "+ notice);
                str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView tvNote = (TextView)findViewById(R.id.note);
                tvNote.setText(str);
            }
        });
    }


    /**
     * Show textual representation of Access and Identity tokens
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

    /**
     * Present bonus points attribute value at UI
     * @param points
     * @param isNewIdentifiedUser
     */
    private void showBonusPoints(final String points, final boolean isNewIdentifiedUser) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tvGift = (TextView)findViewById(R.id.textViewGift);
                TextView tvGiftImg = (TextView)findViewById(R.id.textViewGiftImg);
                if(isNewIdentifiedUser){
                    tvGift.setText(getString(R.string.new_bonus_points, points));
                    tvGiftImg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gift,0,0,0);
                }else {
                    tvGift.setText(getString(R.string.bonus_points, points));
                    tvGiftImg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_icecream,0,0,0);
                }
                tvGiftImg.setText("");
            }
        });
    }

    /**
     * Reading profiles bonus points value
     */
    private void getPoints() {
        appID.getUserAttributeManager().getAttribute(ATTR_BONUS_POINTS, new UserAttributeResponseListener() {
            @Override
            public void onSuccess(JSONObject attributes) {

                showBonusPoints(attributes.optString(ATTR_BONUS_POINTS), false);
            }

            @Override
            public void onFailure(UserAttributesException e) {
                if(e.getError() != UserAttributesException.Error.NOT_FOUND){
                    handleAppIdError(e);
                    return;
                }
                //The bonus_points attribute was not found
                Log.i(logTag("getPoints"), "User never got bonus points, new identified user gets: " + LOGGING_IN_BONUS);

                //adjust the notice to developer
                authState = authState == NoticeHelper.AuthState.logged_in_again ? NoticeHelper.AuthState.logged_in_new : authState;

                //We identify the case of a user loging in for the very first time, and give him the bonus points.
                givePoints();
            }
        });
    }

    /**
     * Giving bonus points to user
     */
    private void givePoints(){
        appID.getUserAttributeManager().setAttribute(ATTR_BONUS_POINTS, String.valueOf(LOGGING_IN_BONUS), new UserAttributeResponseListener() {
            @Override
            public void onSuccess(JSONObject attributes) {
                showBonusPoints(attributes.optString(ATTR_BONUS_POINTS), true);
                updateNotice(noticeHelper.getNoticeForState(authState));
            }

            @Override
            public void onFailure(UserAttributesException e) {
                handleAppIdError(e);
            }
        });
    }

    private void handleAppIdError(UserAttributesException e) {
        switch(e.getError()){
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
                            profilePicture.getLayoutParams().height = 300;
                            profilePicture.getLayoutParams().width = 300;
                            profilePicture.setScaleType(ImageView.ScaleType.FIT_XY);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Logging in action, only available for an anonymous user.
     * @param v
     */
    public void onGiftClick(View v) {
        if(appIDAuthorizationManager.getAccessToken().isAnonymous()){
            LoginWidget loginWidget = appID.getLoginWidget();

            AuthorizationListener loginAuthorization = new AppIdSampleAuthorizationListener(this,appIDAuthorizationManager,false);
            loginWidget.launch(this,loginAuthorization);
        }
    }

    private String logTag(String methodName){
        return getClass().getCanonicalName() + "." + methodName;
    }
}
