package info.blockchain.merchant.tabsswipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
//import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import info.blockchain.merchant.NotificationData;
import info.blockchain.merchant.R;
import info.blockchain.merchant.db.DBControllerV2;
import info.blockchain.merchant.util.DateUtil;
import info.blockchain.merchant.util.MonetaryUtil;
import info.blockchain.merchant.util.PrefsUtil;
import info.blockchain.merchant.util.TypefaceUtil;

public class TransactionsFragment extends Fragment {

    private static String merchantXpub = null;
    private List<ContentValues> mListItems;
    private TransactionAdapter adapter = null;
    private NotificationData notification = null;
    private boolean push_notifications = false;
    private Timer timer = null;
    private ListView listView = null;
    private Typeface btc_font = null;

    private boolean doBTC = false;
    private SwipeRefreshLayout swipeLayout = null;

    private Activity thisActivity = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(getResources().getLayout(R.layout.fragment_transaction), container, false);

        initListView(rootView);

        merchantXpub = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "");
        push_notifications = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.MERCHANT_KEY_PUSH_NOTIFS, false);
        doBTC = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.MERCHANT_KEY_CURRENCY_DISPLAY, false);

        btc_font = TypefaceUtil.getInstance(getActivity()).getTypeface();

        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        swipeLayout.setProgressViewEndTarget(false, (int) (getResources().getDisplayMetrics().density * (72 + 20)));
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new GetDataTask().execute();
            }
        });
        swipeLayout.setColorScheme(R.color.blockchain_blue,
                R.color.blockchain_green,
                R.color.blockchain_dark_blue);

        thisActivity = getActivity();

        return rootView;
    }

    private void initListView(View rootView){

        listView = (ListView)rootView.findViewById(R.id.txList);
        mListItems = new ArrayList<ContentValues>();
        adapter = new TransactionAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                doBTC = !doBTC;
                adapter.notifyDataSetChanged();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                doTxTap(id);
                return true;
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {

            merchantXpub = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "");
            push_notifications = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.MERCHANT_KEY_PUSH_NOTIFS, false);
            doBTC = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.MERCHANT_KEY_CURRENCY_DISPLAY, false);

            if(push_notifications) {
                if(timer == null) {
                    timer = new Timer();
                    try {
                        timer.scheduleAtFixedRate(new TimerTask() {
                            public void run() {
                                new GetDataTask().execute();
                            }
                        }, 500L, 1000L * 60L * 2L);
                    }
                    catch(IllegalStateException ise) {
                        ;
                    }
                    catch(IllegalArgumentException iae) {
                        ;
                    }
                }
            }

        }
        else {
            ;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        merchantXpub = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.MERCHANT_KEY_MERCHANT_RECEIVER, "");
        push_notifications = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.MERCHANT_KEY_PUSH_NOTIFS, false);
        doBTC = PrefsUtil.getInstance(getActivity()).getValue(PrefsUtil.MERCHANT_KEY_CURRENCY_DISPLAY, false);
        new GetDataTask().execute();
    }

    private class GetDataTask extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... params) {

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(true);
                }
            });

            if(merchantXpub != null && merchantXpub.length() > 0) {

                // get updated list from database
                DBControllerV2 pdb = new DBControllerV2(getActivity());
                ArrayList<ContentValues> vals = pdb.getAllPayments();

                if(vals.size() > 0) {
                    mListItems.clear();
                    mListItems.addAll(vals);

                    thisActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }

            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {

            thisActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(false);
                }
            });

            super.onPostExecute(result);
        }
    }

    private class TransactionAdapter extends BaseAdapter {

        private LayoutInflater inflater = null;
        private SimpleDateFormat sdf = null;

        TransactionAdapter() {
            inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mListItems.size();
        }

        @Override
        public String getItem(int position) {
            return mListItems.get(position).getAsString("iad");
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view;

            if (convertView == null) {
                view = inflater.inflate(R.layout.list_item_transaction, parent, false);
            } else {
                view = convertView;
            }

            ContentValues vals = mListItems.get(position);

            String date_str = DateUtil.getInstance().formatted(vals.getAsLong("ts"));
            SpannableStringBuilder ds = new SpannableStringBuilder(date_str);
            if(date_str.indexOf("@") != -1) {
                int idx = date_str.indexOf("@");
                ds.setSpan(new StyleSpan(Typeface.NORMAL), 0, idx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ds.setSpan(new RelativeSizeSpan(0.75f), idx, date_str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            TextView tvDate = (TextView)view.findViewById(R.id.tv_date);
            tvDate.setText(ds);
            tvDate.setAlpha(0.7f);

            TextView tvAmount = (TextView)view.findViewById(R.id.tv_amount);
            if(doBTC) {
                String displayValue = null;
                long amount = Math.abs(vals.getAsLong("amt"));
                displayValue = MonetaryUtil.getInstance(getActivity()).getDisplayAmountWithFormatting(amount);

                SpannableStringBuilder cs = new SpannableStringBuilder(getActivity().getResources().getString(R.string.bitcoin_currency_symbol));
                cs.setSpan(new RelativeSizeSpan((float) 0.75), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvAmount.setText(displayValue + " " + cs);
            }
            else {
                SpannableStringBuilder cs = new SpannableStringBuilder(vals.getAsString("famt").subSequence(0, 1));
                cs.setSpan(new RelativeSizeSpan((float) 0.75), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvAmount.setText(cs + " " + vals.getAsString("famt").substring(1));
            }

            if(vals.getAsLong("amt") < 0L)    {
                ImageView ivStatus = (ImageView)view.findViewById(R.id.iv_status);
                ivStatus.setImageResource(R.drawable.ic_warning_black_18dp);
            }

            return view;
        }

    }

    private void doTxTap(final long item)	{

        final ContentValues val = mListItems.get((int) item);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.drawable.ic_launcher);
        builder.setItems(new CharSequence[]
                        { "View transaction" },
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Intent intent = new Intent( Intent.ACTION_VIEW , Uri.parse("https://blockchain.info/address/" + val.getAsString("iad")));
                                startActivity(intent);
                                break;
                        }
                    }
                });
        builder.create().show();
    }

}
