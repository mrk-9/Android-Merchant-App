package info.blockchain.merchant;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import info.blockchain.merchant.util.PrefsUtil;
import info.blockchain.merchant.util.ToastCustom;

public class SettingsActivity extends PreferenceActivity	{

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setTheme(android.R.style.Theme_Holo);
        }

        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        addPreferencesFromResource(R.xml.settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        toolbar.setTitle(R.string.action_settings);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        root.addView(toolbar, 0);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "").length() == 0)    {
                    ToastCustom.makeText(SettingsActivity.this, getString(R.string.obligatory_receiver), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
                }
                else    {
                    finish();
                }
            }
        });

        final Preference receivePref = (Preference) findPreference("receiveAPI");
        final boolean status = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "").length() == 0 ? false : true;
        receivePref.setSummary(status ? (String)SettingsActivity.this.getText(R.string.on) : (String)SettingsActivity.this.getText(R.string.off));
        receivePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                intent.putExtra("status", status);
                startActivity(intent);

                return true;
            }
        });

        final Preference namePref = (Preference) findPreference("name");
        namePref.setSummary(PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_NAME, ""));
        namePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                final EditText etName = new EditText(SettingsActivity.this);
                etName.setSingleLine(true);
                int maxLength = 70;
                InputFilter[] fArray = new InputFilter[1];
                fArray[0] = new InputFilter.LengthFilter(maxLength);
                etName.setFilters(fArray);
                etName.setText(PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_NAME, ""));

                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.receive_coins_fragment_name)
                        .setView(etName)
                        .setCancelable(false)
                        .setPositiveButton(R.string.prompt_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                                String name = etName.getText().toString();

                                if (name != null && name.length() > 0) {
                                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.MERCHANT_KEY_MERCHANT_NAME, name);
                                    namePref.setSummary(name);
                                }

                            }

                        }).setNegativeButton(R.string.prompt_ko, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        ;

                    }
                }).show();

                return true;
            }
        });

        final Preference fiatPref = (Preference) findPreference("fiat");
        fiatPref.setSummary(PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.MERCHANT_KEY_CURRENCY, "USD"));
        fiatPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                final String[] currencies = getResources().getStringArray(R.array.currencies);
                String strCurrency = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.MERCHANT_KEY_CURRENCY, "USD");
                int sel = -1;
                for (int i = 0; i < currencies.length; i++) {
                    if (currencies[i].endsWith(strCurrency)) {
                        sel = i;
                        break;
                    }
                }
                if (sel == -1) {
                    sel = currencies.length - 1;    // set to USD
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(R.string.options_local_currency);
                builder.setSingleChoiceItems(currencies, sel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.MERCHANT_KEY_CURRENCY, currencies[which].substring(currencies[which].length() - 3));
                            fiatPref.setSummary(PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.MERCHANT_KEY_CURRENCY, currencies[which].substring(currencies[which].length() - 3)));
                        }
                    });
                AlertDialog alert = builder.create();
                alert.show();

                return true;
            }
        });

        Preference pinPref = (Preference) findPreference("pin");
        pinPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(SettingsActivity.this, PinActivity.class);
                intent.putExtra("create", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);

                return false;
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        final Preference receivePref = (Preference) findPreference("receiveAPI");
        final boolean status = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "").length() == 0 ? false : true;
        receivePref.setSummary(status ? (String) SettingsActivity.this.getText(R.string.on) : (String) SettingsActivity.this.getText(R.string.off));
        receivePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                Intent intent = new Intent(SettingsActivity.this, SettingsActivity2.class);
                intent.putExtra("status", status);
                startActivity(intent);

                return true;
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK) {

            if(PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "").length() == 0)    {
                ToastCustom.makeText(this, getString(R.string.obligatory_receiver), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR);
            }
            else    {
                finish();
            }

        }

        return false;
    }

}
