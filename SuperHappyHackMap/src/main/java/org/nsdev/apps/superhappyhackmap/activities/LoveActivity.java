package org.nsdev.apps.superhappyhackmap.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.view.View;

import com.android_s14.rvh.DataModel;
import com.android_s14.rvh.RecyclerViewBuilder;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import org.nsdev.apps.superhappyhackmap.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by neal on 15-07-30.
 */
public class LoveActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler {

    private BillingProcessor mBillingProcessor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View layout = getLayoutInflater().inflate(R.layout.activity_love, null);

        List<DataModel> data = new ArrayList<>();
        data.add(new Donation("Super Love Pack", "$10.00"));
        data.add(new Donation("Moderate Love Pack", "$5.00"));
        data.add(new Donation("Tickle Me Love Pack", "$1.00"));

        new RecyclerViewBuilder(this)
                .using(R.id.recycler_view, layout)
                .setData(data)
                .setCardCornerRadii(1)
                .setCardPadding(32, 32, 32, 32)
                .setCardMargins(8, 8, 8, 8)
                .setRowLayout(R.layout.row_donation)
                .setLayoutManager(new GridLayoutManager(this, 2))
                .build();
        setContentView(layout);

        mBillingProcessor = new BillingProcessor(this, "", this);
    }

    @Override
    public void onProductPurchased(String s, TransactionDetails transactionDetails) {

    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int i, Throwable throwable) {

    }

    @Override
    public void onBillingInitialized() {

    }
}
