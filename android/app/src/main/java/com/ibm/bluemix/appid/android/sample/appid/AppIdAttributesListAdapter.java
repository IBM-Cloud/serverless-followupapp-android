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
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ibm.bluemix.appid.android.api.AppID;
import com.ibm.bluemix.appid.android.api.userattributes.UserAttributeResponseListener;
import com.ibm.bluemix.appid.android.api.userattributes.UserAttributesException;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The list adapter accesses the user profile attributes and uses the App ID SDK to fetch them
 * so they could be presented at the clickable list at the UI.
 * Note that this sample uses a single App ID profile attribute for "food_selection" to store a list of items
 * in a JSON array format
 */
public class AppIdAttributesListAdapter extends ArrayAdapter<String> {
    private final static Logger logger = Logger.getLogger(AppIdAttributesListAdapter.class.getName());

    private static String ATTR_FOOD_SELECTION = "food_selection";
    private AppID appID = AppID.getInstance();

    private static final String[] values = new String[] {
            "Burgers", "Sandwiches", "Pizza"
    };

    private final Activity context;

    private JSONArray jaFoodSelection = new JSONArray();

    public AppIdAttributesListAdapter(Activity context) {
        super(context, -1, values);
        this.context = context;
        loadSelection();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.listview_food_item, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.item);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        textView.setText(values[position]);
        // change the icon for Windows and iPhone
        String s = values[position];

//        This will show or hide the green "selected" icon at the list row
        if (Utils.isItemInJsonArray(jaFoodSelection, s)) {
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.INVISIBLE);
        }

        return rowView;
    }

    /**
     * Invoked when clicking on an item in the foods selection list
     * @return
     */
    public AdapterView.OnItemClickListener getOnClickListener(){
        return new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                view.animate().setDuration(500).alpha(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (Utils.isItemInJsonArray(jaFoodSelection, item)) {
                                        Utils.removeValueFromJsonArray(jaFoodSelection, item);
                                    }else{
                                        jaFoodSelection.put(item);
                                    }
                                    storeSelectionToProfile();
                                    notifyDataSetChanged();
                                    view.setAlpha(1);

                                } catch (JSONException e) {
                                    logger.error("Failed selecting an item. ",e);
                                }
                            }
                        });
            }

        };
    }

    /**
     * Loading selection stored at App ID profile.
     */
    private void loadSelection() {
        appID.getUserAttributeManager().getAllAttributes(new UserAttributeResponseListener() {
            @Override
            public void onSuccess(JSONObject attributes) {
                JSONArray storedSelection = null;
                try {
                    String list = attributes.optString(ATTR_FOOD_SELECTION);
                    if( list != null && list.length() > 0) {
                        storedSelection = new JSONArray(list);
                    }else{
                        storedSelection = new JSONArray();
                    }
                    jaFoodSelection = storedSelection == null ? new JSONArray() : storedSelection;
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                } catch (JSONException e) {
                    logger.error("Failed to load selection from profile. ",e);
                }
            }

            @Override
            public void onFailure(UserAttributesException e) {
                handleAttributesLoadFailure(e);
            }
        });
    }

    private void handleAttributesLoadFailure(Exception e) {
        logger.error("Failed to access profile attributes.", e);
    }

    /**
     * Updating the selection attribute at the App ID profile
     */
    private void storeSelectionToProfile() {
        appID.getUserAttributeManager().setAttribute(ATTR_FOOD_SELECTION, jaFoodSelection.toString(), new UserAttributeResponseListener() {
            @Override
            public void onSuccess(JSONObject attributes) {
                logger.info("Stored selection to profile");
            }

            @Override
            public void onFailure(UserAttributesException e) {
                logger.error("Failed to Store selection to profile. ",e);
            }
        });
    }

}
