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
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

/**
 * Simple test to verify that the sample works, for internal use only
 */
@RunWith(AndroidJUnit4.class)
public class AndroidCheck {

    private UiDevice device;
    private static final String PACKAGE = "com.ibm.bluemix.appid";

    @Before
    public void setUp() throws IOException, UiObjectNotFoundException {
        startApp();
    }

    @Test
    public void runTest() throws UiObjectNotFoundException, InterruptedException {

        clickOnObject("loginButton");
        UiObject google_login = device.findObject(new UiSelector().resourceId("google_login"));
        google_login.clickAndWaitForNewWindow(10000);

        String displayName = getDisplayName();
        assertThat(displayName, containsString("Lon Don"));
    }

    private void startApp(){
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Start from the home screen
        device.pressHome();

        // Wait for launcher
        final String launcherPackage = device.getLauncherPackageName();

        //assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), 5000);

        // Launch the app
        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(PACKAGE);

        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        context.startActivity(intent);

        // Wait for the app to appear
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), 5000);
    }

    private void clickOnObject(String id) throws UiObjectNotFoundException {
        UiObject object = getObjectById(id);
        object.click();
    }

    private UiObject getObjectById(String id){
        return device.findObject(new UiSelector().resourceId(PACKAGE + ":id/" + id));
    }

    protected String getDisplayName() throws UiObjectNotFoundException {
        try{
            device.wait(Until.findObject(By.res(PACKAGE,"userName")),25000);
            UiObject result = getObjectById("userName");
            return result.getText();
        }
        catch (Throwable t){
            return "";
        }
    }
}




