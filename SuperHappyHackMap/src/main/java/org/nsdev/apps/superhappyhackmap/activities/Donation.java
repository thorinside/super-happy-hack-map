package org.nsdev.apps.superhappyhackmap.activities;

import android.graphics.drawable.Drawable;

import com.android_s14.rvh.DataModel;

/**
 * Created by neal on 15-07-30.
 */
public class Donation implements DataModel {

    private final String mProductName;
    private final String mProductPrice;

    public Donation(String productName, String productPrice) {
        mProductName = productName;
        mProductPrice = productPrice;
    }

    @Override
    public String getTextField(int i) {
        switch (i) {
            case 0:
                return mProductName;
            case 1:
                return mProductPrice;
        }
        return null;
    }

    @Override
    public int getTextFieldsNumber() {
        return 2;
    }

    @Override
    public Drawable getImageField(int i) {
        return null;
    }

    @Override
    public int getImageFieldsNumber() {
        return 0;
    }
}
