package info.blockchain.merchant;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import info.blockchain.merchant.db.MigrateDBUtil;
import info.blockchain.merchant.service.WebSocketHandler;
import info.blockchain.merchant.service.WebSocketListener;
import info.blockchain.merchant.tabsswipe.TabsPagerAdapter;
import info.blockchain.merchant.util.AppUtil;
import info.blockchain.merchant.util.PrefsUtil;
import info.blockchain.merchant.util.SSLVerifierThreadUtil;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback, WebSocketListener, NavigationView.OnNavigationItemSelectedListener {

    public static int SETTINGS_ACTIVITY 	= 1;
    private static int PIN_ACTIVITY 		= 2;
    private static int RESET_PIN_ACTIVITY 	= 3;
    private static int ABOUT_ACTIVITY 	    = 4;

    private WebSocketHandler webSocketHandler = null;

    public static final String ACTION_INTENT_SUBSCRIBE_TO_ADDRESS = "info.blockchain.merchant.MainActivity.SUBSCRIBE_TO_ADDRESS";
    public static final String ACTION_INTENT_INCOMING_TX = "info.blockchain.merchant.MainActivity.ACTION_INTENT_INCOMING_TX";
    public static final String ACTION_INTENT_RECONNECT = "info.blockchain.merchant.MainActivity.ACTION_INTENT_RECONNECT";

    //Navigation Drawer
    private Toolbar toolbar = null;
    DrawerLayout mDrawerLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_main);

        setToolbar();
        setNavigtionDrawer();

        initTableLayout();

        SSLVerifierThreadUtil.getInstance(MainActivity.this).validateSSLThread();

        // no PIN ?, then create one
		String pin = PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.MERCHANT_KEY_PIN, "");
        if(pin.equals("")) {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            TextView title = new TextView(MainActivity.this);
            title.setPadding(20, 60, 20, 20);
            title.setText(R.string.app_name);
            title.setGravity(Gravity.CENTER);
            title.setTextSize(20);
            builder.setCustomTitle(title);
            builder.setMessage(R.string.please_create_pin).setCancelable(false);
            AlertDialog alert = builder.create();

            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.prompt_ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    doPIN();
                }});

            alert.show();

        }
        else if(PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "").length() == 0)    {
            doSettings(false);
        }
		else	{

			//
			// test for v1
			//
			if(AppUtil.getInstance(MainActivity.this).isLegacy())	{
				String strCurrency = PrefsUtil.getInstance(MainActivity.this).getValue("currency", "");
				if(strCurrency.equals("ZZZ"))	{
					PrefsUtil.getInstance(MainActivity.this).setValue(PrefsUtil.MERCHANT_KEY_CURRENCY, "USD");
				}
				PrefsUtil.getInstance(MainActivity.this).removeValue("ocurrency");
			}

            MigrateDBUtil.getInstance(MainActivity.this).migrate();

		}

        //
        // test for v1
        //
        if(AppUtil.getInstance(MainActivity.this).isLegacy())	{
            String strCurrency = PrefsUtil.getInstance(MainActivity.this).getValue("currency", "");
            if(strCurrency.equals("ZZZ"))	{
                PrefsUtil.getInstance(MainActivity.this).setValue(PrefsUtil.MERCHANT_KEY_CURRENCY, "USD");
            }
            PrefsUtil.getInstance(MainActivity.this).removeValue("ocurrency");
        }

        MigrateDBUtil.getInstance(MainActivity.this).migrate();

        //Start websockets
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_INTENT_SUBSCRIBE_TO_ADDRESS);
        filter.addAction(ACTION_INTENT_RECONNECT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        webSocketHandler = new WebSocketHandler();
        webSocketHandler.addListener(this);
        webSocketHandler.start();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setMerchantName();
    }

    @Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	@Override
	public void onNdefPushComplete(NfcEvent event) {

		if(Build.VERSION.SDK_INT < 16){
			return;
		}

        final String eventString = "onNdefPushComplete\n" + event.toString();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), eventString, Toast.LENGTH_SHORT).show();
            }
        });

	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {

		if(Build.VERSION.SDK_INT < 16){
			return null;
		}

		NdefRecord rtdUriRecord = NdefRecord.createUri("market://details?id=info.blockchain.merchant");
		NdefMessage ndefMessageout = new NdefMessage(rtdUriRecord);
		return ndefMessageout;
	}

	@Override
    protected void onDestroy() {
        //Stop websockets
        webSocketHandler.stop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);

        super.onDestroy();
    }

    private void initTableLayout(){

        String[] tabs = new String[]{getResources().getString(R.string.tab_payment),getResources().getString(R.string.tab_history)};

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter mAdapter = new TabsPagerAdapter(getSupportFragmentManager(), tabs);
		viewPager.setAdapter(mAdapter);

        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabTextColors(getResources().getColor(R.color.white_50), getResources().getColor(R.color.white));

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setTabsFromPagerAdapter(mAdapter);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

	    // Handle item selection
	    switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(requestCode == SETTINGS_ACTIVITY && resultCode == RESULT_OK) {
			;
		}
		else if(requestCode == PIN_ACTIVITY && resultCode == RESULT_OK) {

            if(PrefsUtil.getInstance(MainActivity.this).getValue("popup_" + getResources().getString(R.string.version_name), false) == false)	{

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                TextView title = new TextView(MainActivity.this);
                title.setPadding(20, 60, 20, 20);
                title.setText(R.string.app_name);
                title.setGravity(Gravity.CENTER);
                title.setTextSize(20);
                builder.setCustomTitle(title);
                builder.setMessage(R.string.new_version_message).setCancelable(false);
                AlertDialog alert = builder.create();

                alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.prompt_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PrefsUtil.getInstance(MainActivity.this).setValue("popup_" + getResources().getString(R.string.version_name), true);
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivityForResult(intent, SETTINGS_ACTIVITY);
                    }});

                alert.show();

            }
            else    {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, SETTINGS_ACTIVITY);
            }

		}
		else if(requestCode == RESET_PIN_ACTIVITY && resultCode == RESULT_OK) {
			doPIN();
		}
		else {
            super.onActivityResult(requestCode, resultCode, data);
		}
		
	}

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean ret = super.dispatchTouchEvent(event);

        View view = this.getCurrentFocus();

        if (view instanceof EditText) {
            View w = this.getCurrentFocus();
            int scrcoords[] = new int[2];
            w.getLocationOnScreen(scrcoords);
            float x = event.getRawX() + w.getLeft() - scrcoords[0];
            float y = event.getRawY() + w.getTop() - scrcoords[1];
            
            if (event.getAction() == MotionEvent.ACTION_UP && (x < w.getLeft() || x >= w.getRight() || y < w.getTop() || y > w.getBottom()) ) { 
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
            }
        }

        return ret;
    }

    private void doSettings(final boolean create)	{
    	if(create)	{
    		Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
    		startActivityForResult(intent, SETTINGS_ACTIVITY);
    	}
    	else	{
    		enterPIN();
    	}
    }

    private void doPIN()	{

        Intent intent = new Intent(MainActivity.this, PinActivity.class);
        intent.putExtra("create", true);
        startActivityForResult(intent, PIN_ACTIVITY);

    }

    private void enterPIN()	{
		Intent intent = new Intent(MainActivity.this, PinActivity.class);
		intent.putExtra("create", false);
		startActivityForResult(intent, PIN_ACTIVITY);
    }

    private void doAbout()	{
    	Intent intent = new Intent(MainActivity.this, AboutActivity.class);
		startActivityForResult(intent, ABOUT_ACTIVITY);
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {

            if (ACTION_INTENT_SUBSCRIBE_TO_ADDRESS.equals(intent.getAction())) {
                webSocketHandler.subscribeToAddress(intent.getStringExtra("address"));
            }

            //Connection re-established
            if (ACTION_INTENT_RECONNECT.equals(intent.getAction())) {
                if(webSocketHandler != null && !webSocketHandler.isConnected()){
                    webSocketHandler.start();
				}
            }
        }
    };

    private void setMerchantName(){
        //Update Merchant name
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        TextView tvName = (TextView)navigationView.findViewById(R.id.drawer_title);
        tvName.setText(PrefsUtil.getInstance(this).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_NAME, getResources().getString(R.string.app_name)));
    }

    @Override
    public void onIncomingPayment(String addr, long paymentAmount) {

        //New incoming payment - broadcast message
        Intent intent = new Intent(MainActivity.ACTION_INTENT_INCOMING_TX);
        intent.putExtra("payment_address",addr);
        intent.putExtra("payment_amount",paymentAmount);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void setToolbar() {

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_menu_white_24dp));
        setSupportActionBar(toolbar);
    }

    private void setNavigtionDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        setMerchantName();
        // listen for navigation events
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        if (isNavDrawerOpen()) {
            closeNavDrawer();
        } else {
            super.onBackPressed();
        }
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    protected void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem menuItem) {

        // allow some time after closing the drawer before performing real navigation
        // so the user can see what is happening
        mDrawerLayout.closeDrawer(GravityCompat.START);
        Handler mDrawerActionHandler = new Handler();
        mDrawerActionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (menuItem.getItemId()) {
                    case R.id.action_settings:
                        doSettings(false);
                        break;
                    case R.id.action_about:
                        doAbout();
                        break;
                }
            }
        }, 250);

        return false;
    }
}
