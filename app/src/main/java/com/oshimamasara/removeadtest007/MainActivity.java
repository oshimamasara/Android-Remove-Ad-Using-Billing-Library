package com.oshimamasara.removeadtest007;



import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.List;

public class MainActivity extends AppCompatActivity implements PurchasesUpdatedListener {
    private static final String TAG = "MainActivity";
    static final String ITEM_SKU_ADREMOVAL = "ad_remove_item";
    private Button mBuyButton;
    private SharedPreferences mSharedPreferences;///////アプリ設定を保存するために必要
    private BillingClient mBillingClient;
    private AdView mAdView;
    WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        myWebView = findViewById(R.id.webview);
        myWebView.getSettings().setDefaultTextEncodingName("utf-8");
        myWebView.loadUrl("https://blog.codecamp.jp");
        myWebView.setWebViewClient(new MyWebViewClient());


        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");
        mAdView = findViewById(R.id.adView);


        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);  // SharedPreferences() 購入の有無に関係するレイアウトを保存するため

        mBillingClient = BillingClient.newBuilder(MainActivity.this).setListener(this).build();
        Log.i(TAG,"mBillingClient：:" + mBillingClient); //ok

        mBillingClient.startConnection(new BillingClientStateListener() {

            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                Log.i(TAG,"onBillingSetupFinished" + mBillingClient); //ok
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    Log.i(TAG,"BillingSetUpFinished:" + billingResponseCode); // Logcatに出力　0  接続OK
                    Log.i(TAG,"アイテム：" + ITEM_SKU_ADREMOVAL);
                }
                else

                    Log.i(TAG,"onBillingSetupFinished else：" );
                    Log.i(TAG,"onBillingSetupFinished else billingResponseCode:：" + billingResponseCode);
                    //2019.1026 :  billingResponseCode// emulator:: 3 , Real Mobile ::0 //
                    // billingResponseCode = 3  -> BILLING_UNAVAILABLE -> Billing API version is not supported for the type requested
                    /*
                    This state seems to be unable to read the user account information.
                    This may happen because you are not logged in when using the emulator.
                    Please test with actual machine.
                    */
            }

            @Override
            public void onBillingServiceDisconnected() {
                Toast.makeText(MainActivity.this,  getResources().getString(R.string.billing_connection_failure), Toast.LENGTH_SHORT);
            }
        });



        mBuyButton = (Button) findViewById(R.id.buyButton);
        mBuyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setSku(ITEM_SKU_ADREMOVAL)
                        .setType(BillingClient.SkuType.INAPP)
                        .build();
                int responseCode = mBillingClient.launchBillingFlow(MainActivity.this, flowParams);
                // launchBillingFlow() -> Start purchase process
                // This package "com.oshimamasara.removeadtest007;" is not registered with the Google Play Console.
                // You can't proceed with the purchase process because you haven't registered any billing items
                Log.i(TAG,"After Tap button： ITEM_SKU_ADREMOVAL::"+ITEM_SKU_ADREMOVAL );
                Log.i(TAG,"After Tap button： responseCode::"+responseCode );
            }
        });
        queryPrefPurchases();
    }


    // WebView
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }
    }

    // back function
    @Override
    public void onBackPressed() {
        if(myWebView!= null && myWebView.canGoBack())
            myWebView.goBack();// if there is previous page open it
        else
            super.onBackPressed();//if there is no previous page, close app
    }





    private void queryPrefPurchases() {
        Boolean adFree = mSharedPreferences.getBoolean(getResources().getString(R.string.pref_remove_ads_key), false);

        if (adFree) {
            // adFree = true の処理内容
            mBuyButton.setText(getResources().getString(R.string.pref_ad_removal_purchased));
            mBuyButton.setEnabled(false); // setEnabled　ボタンイベント無効に
            mAdView.setVisibility(View.GONE);  // バナー広告非表示
            mBuyButton.setVisibility(View.GONE);  // ボタン非表示
        } else {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);  //広告表示
        }
    }


    //購入後のデータ書き換え onPurchasesUpdated() の処理に必要なプログラム
    private void handlePurchase(Purchase purchase) {
        if (purchase.getSku().equals(ITEM_SKU_ADREMOVAL)) {
            mSharedPreferences.edit().putBoolean(getResources().getString(R.string.pref_remove_ads_key), true).commit();
            mBuyButton.setVisibility(View.GONE);
            mAdView.setVisibility(View.GONE);
            mBuyButton.setText(getResources().getString(R.string.pref_ad_removal_purchased));   //ボタンの文字切り替え　購入済み
            mBuyButton.setEnabled(false);
        }
    }

    //購入手続き後のデータ書き換え
    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<com.android.billingclient.api.Purchase> purchases) {
        Log.d(TAG, "if前：：" + responseCode);
        if (responseCode == BillingClient.BillingResponse.OK   //最初の購入フロー
                && purchases != null) {
            Log.i(TAG,"購入手続き完了後の処理：：" + responseCode);
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            Log.d(TAG, "キャンセル時の処理：：" + responseCode);
        } else if (responseCode == BillingClient.BillingResponse.ITEM_ALREADY_OWNED) {
            Log.i(TAG,"購入済みの場合の処理：：" + responseCode);
            mSharedPreferences.edit().putBoolean(getResources().getString(R.string.pref_remove_ads_key), true).commit();
            mBuyButton.setVisibility(View.GONE);
            mAdView.setVisibility(View.GONE);mBuyButton.setText(getResources().getString(R.string.pref_ad_removal_purchased));
            mBuyButton.setEnabled(false);
        } else {
            Log.d(TAG, "例外時：：" + responseCode);
        }
    }
}


/*
error history:
From YouTube
    https://www.youtube.com/watch?v=yBMwpeU0DdI&lc=Ugx7sbzwBWbHpMpJuTF4AaABAg
    "New iap purchase dependencies showing ERROR in yours codes!!! Please provide update code for one time in app purchase"

Checked
    Emulator
    billingResponseCode = 3  -> BILLING_UNAVAILABLE -> Billing API version is not supported for the type requested
    Test with actual machine.

    Actual Machine
    Current Activity:false
    Because it is not registered in google play console.
    Apps with the same content are running.
        https://play.google.com/store/apps/details?id=com.oshimamasara.removead000


 */

