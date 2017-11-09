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

import android.content.Context;

import com.ibm.bluemix.appid.android.api.AppIDAuthorizationManager;

/**
 * A helper class for providing notifications to the sample users about the authorization process and
 * profile state at App ID
 *
 */
public class NoticeHelper {
    private Context ctx;
    private AppIDAuthorizationManager authorizationManager;
    private TokensPersistenceManager persistenceManager;

    enum AuthState {
        progressive_auth, switch_to_identified, logged_in_new, logged_in_again, new_guest, returning_guest, login_back, login_back_and_change_login
    }

    public NoticeHelper(Context ctx, AppIDAuthorizationManager authorizationManager, TokensPersistenceManager persistenceManager) {
        this.ctx = ctx;
        this.authorizationManager = authorizationManager;
        this.persistenceManager = persistenceManager;
    }

    public String getNoticeForState(AuthState state){
        switch(state){
            case logged_in_again:
                return ctx.getString(R.string.logged_in_again_notice);
            case login_back:
                return ctx.getString(R.string.login_back_notice);
            case logged_in_new:
                return ctx.getString(R.string.logged_in_new_notice);
            case login_back_and_change_login:
                return ctx.getString(R.string.login_back_and_change_login_notice);
            case new_guest:
                return ctx.getString(R.string.new_guest_notice);
            case progressive_auth:
                return ctx.getString(R.string.progressive_auth_notice);
            case returning_guest:
                return ctx.getString(R.string.returning_guest_notice);
            case switch_to_identified:
                return ctx.getString(R.string.switch_to_identified_notice);
            default:
                return null;
        }
    }

    /**
     * Calculating the authorization state based on stored token and new authorization data
     * @param isAnonLogin true means last action is anonymousLogin, otherwise its an identity login
     * @return
     */
    public AuthState determineAuthState(boolean isAnonLogin){
        String lastAuthorizedUserId = authorizationManager.getAccessToken().getSubject();

        switch(persistenceManager.getStoreTokenState()){
            case empty:
                if(isAnonLogin){
                    return AuthState.new_guest;
                }else{
                    return AuthState.logged_in_again;
                }

            case anonymous:
                if(isAnonLogin){
                    return AuthState.returning_guest;
                }else{
                    if( lastAuthorizedUserId.equals(persistenceManager.getStoredUserID())) {
                        return AuthState.progressive_auth;
                    }else{
                        return AuthState.switch_to_identified;
                    }
                }

            case identified:
                if(isAnonLogin){
                    return AuthState.new_guest;
                }else{
                    if( lastAuthorizedUserId.equals(persistenceManager.getStoredUserID())) {
                        return AuthState.login_back;
                    }else{
                        return AuthState.login_back_and_change_login;
                    }
                }

            default: throw new RuntimeException("Invalid token persistence state");
        }
    }

}
