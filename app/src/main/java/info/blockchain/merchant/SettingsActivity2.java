package info.blockchain.merchant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;

import net.sourceforge.zbar.Symbol;

import info.blockchain.merchant.util.PrefsUtil;
import info.blockchain.merchant.util.ToastCustom;
import info.blockchain.wallet.util.FormatsUtil;

public class SettingsActivity2 extends PreferenceActivity	{

    private static int ZBAR_SCANNER_REQUEST = 2026;

    private Preference newPref = null;
    private Preference walletPref = null;

    private boolean pausedForIntent = false;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setTheme(android.R.style.Theme_Holo);
        }

        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);

        boolean status = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey("status"))	{
            status = extras.getBoolean("status");
        }
        addPreferencesFromResource(status ? R.xml.settings_with_receive : R.xml.settings_no_receive);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        root.addView(toolbar, 0);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        newPref = (Preference) findPreference("address");

        if(!status)    {
            walletPref = (Preference) findPreference("wallet");
            if(walletPref != null)    {
                walletPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {

                        pausedForIntent = true;
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://blockchain.info/wallet-beta"));
                        startActivity(intent);

                        return true;
                    }
                });
            }
        }

        doPopUp(status);

    }

    @Override
    protected void onPause() {
        super.onPause();

        if(!pausedForIntent)  {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        pausedForIntent = false;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK && requestCode == ZBAR_SCANNER_REQUEST)	{

            String scanResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

            if(scanResult.startsWith("bitcoin:"))    {
                scanResult = scanResult.substring(8);
            }

            if(FormatsUtil.getInstance().isValidXpub(scanResult) || FormatsUtil.getInstance().isValidBitcoinAddress(scanResult))    {
                PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, scanResult);
                newPref.setSummary(scanResult);
            }
            else{
                ToastCustom.makeText(this, getString(R.string.unrecognized_xpub), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }

            pausedForIntent = false;

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == android.R.id.home) {
            finish();
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

    private void doPopUp(boolean hasReceiver)  {

        newPref.setSummary(PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, ""));

        if(!hasReceiver)    {

            newPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {

                    final TextView tvReceiverHelp = new TextView(SettingsActivity2.this);
                    tvReceiverHelp.setText(SettingsActivity2.this.getText(R.string.options_add_payment_address_text));
                    tvReceiverHelp.setPadding(50, 10, 50, 10);

                    new AlertDialog.Builder(SettingsActivity2.this)
                            .setTitle(R.string.options_add_payment_address)
                            .setView(tvReceiverHelp)
                            .setCancelable(true)
                            .setPositiveButton(R.string.paste, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    final EditText etReceiver = new EditText(SettingsActivity2.this);
                                    etReceiver.setSingleLine(true);
                                    etReceiver.setText(PrefsUtil.getInstance(SettingsActivity2.this).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, ""));

                                    new AlertDialog.Builder(SettingsActivity2.this)
                                            .setTitle(R.string.options_add_payment_address)
                                            .setView(etReceiver)
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.prompt_ok, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {

                                                    String receiver = etReceiver.getText().toString().trim();
                                                    if (receiver != null && receiver.length() > 0 && (FormatsUtil.getInstance().isValidBitcoinAddress(receiver) || FormatsUtil.getInstance().isValidXpub(receiver))) {
                                                        newPref.setSummary(receiver);
                                                        PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, receiver);
                                                    } else {
                                                        ToastCustom.makeText(SettingsActivity2.this, getString(R.string.unrecognized_xpub), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                                                    }

                                                }

                                            }).setNegativeButton(R.string.prompt_ko, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            ;

                                        }
                                    }).show();

                                }

                            }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            pausedForIntent = true;
                            Intent intent = new Intent(SettingsActivity2.this, ZBarScannerActivity.class);
                            intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
                            startActivityForResult(intent, ZBAR_SCANNER_REQUEST);

                        }
                    }).show();

                    return true;

                }
            });

        }
        else    {

            final Preference forgetPref = (Preference) findPreference("forget");
            forgetPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {

                    final TextView tvForgetHelp = new TextView(SettingsActivity2.this);
                    tvForgetHelp.setText(SettingsActivity2.this.getText(R.string.options_forget_payment_address_text));
                    tvForgetHelp.setPadding(50, 10, 50, 10);

                    new AlertDialog.Builder(SettingsActivity2.this)
                            .setTitle(R.string.options_forget_payment_address)
                            .setView(tvForgetHelp)
                            .setCancelable(false)
                            .setPositiveButton(R.string.prompt_ok, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {

                                    dialog.dismiss();

                                    PrefsUtil.getInstance(SettingsActivity2.this).setValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "");

                                    SettingsActivity2.this.finish();

                                }

                            }).setNegativeButton(R.string.prompt_ko, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            ;

                        }
                    }).show();

                    return true;
                }
            });

        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }

        return false;
    }

}
