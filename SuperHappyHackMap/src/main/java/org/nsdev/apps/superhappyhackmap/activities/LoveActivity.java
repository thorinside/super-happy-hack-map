package org.nsdev.apps.superhappyhackmap.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.view.View;

import com.android_s14.rvh.DataModel;
import com.android_s14.rvh.OnClickListener;
import com.android_s14.rvh.RecyclerViewBuilder;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;

import org.nsdev.apps.superhappyhackmap.R;
import org.nsdev.apps.superhappyhackmap.utils.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by neal on 15-07-30.
 */
public class LoveActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler {

    private BillingProcessor mBillingProcessor;
    private View mLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayout = getLayoutInflater().inflate(R.layout.activity_love, null);

        mBillingProcessor = new BillingProcessor(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnws7sK1+a3Qykxld3t3cwW2wjRo65Rpgl8EV4l4E0RHqqj2eK2wUkcLPQoDJJ7+OcXUzyDYeV6gWVj5HczZBEhWRP5ac9k/ZMXR8DcghWgwbPXIjTegQMaAgHd/d23wacDl3O0BHVUBA+Sm94YjlHstped0iIVpEbiZMdN5hQHs6Xfh/7PW4e8RhzSMuq+eW8xtYpp93Bcp618Eo/jkG45TrFGl67XTB+nh6f2RI8vAsvxoWHh8edG5xMBLT+mI1qLqpsDvNMJzLO71x0H1tsTufYiDDn3Ev+34QDczM8cpUlYH2RiHQdxpahglVpUGVDtOomdkZIsY6qYDAjtsSBQIDAQAB", this);
    }

    @Override
    public void onProductPurchased(String s, TransactionDetails transactionDetails) {
        mBillingProcessor.consumePurchase(transactionDetails.productId);
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int i, Throwable throwable) {
        Log.e("NAS", "Billing error: " + i + " : " + throwable.getMessage());
    }

    @Override
    public void onBillingInitialized() {

        ArrayList<String> skus = new ArrayList<>(Arrays.asList("donation_1", "donation_5", "donation_10", "donation_20"));
        List<SkuDetails> purchaseListingDetails = mBillingProcessor.getPurchaseListingDetails(skus);
        final List<DataModel> data = new ArrayList<>();
        if (purchaseListingDetails.size() > 0) {
            for (SkuDetails skuDetails : purchaseListingDetails) {
                data.add(new Donation(skuDetails.productId, skuDetails.title.replace(" (Super Happy Hack Map)", ""), skuDetails.priceText));
            }
        } else {
            data.add(new Donation("android.test.item_unavailable", "Test Item", "$1,000,000.00"));
            data.add(new Donation("android.test.purchased", "Test Item 2", "$5.00"));
        }

        new RecyclerViewBuilder(this)
                .using(R.id.recycler_view, mLayout)
                .setData(data)
                .setRowLayout(R.layout.row_donation)
                .setLayoutManager(new GridLayoutManager(this, 2))
                .setListener(new OnClickListener() {
                    @Override
                    public void onClick(View view, int i) {
                        DataModel dataModel = data.get(i);
                        mBillingProcessor.purchase(LoveActivity.this, ((Donation) dataModel).getProducId());
                    }
                })
                .setItemAnimator(new DefaultItemAnimator())
                .build();
        setContentView(mLayout);


    }
}
