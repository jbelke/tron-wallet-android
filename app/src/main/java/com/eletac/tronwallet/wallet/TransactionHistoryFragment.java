package com.eletac.tronwallet.wallet;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.eletac.tronwallet.CaptureActivityPortrait;
import com.eletac.tronwallet.InputFilterMinMax;
import com.eletac.tronwallet.R;
import com.eletac.tronwallet.TronWalletApplication;
import com.eletac.tronwallet.Utils;
import com.eletac.tronwallet.WrapContentLinearLayoutManager;
import com.eletac.tronwallet.block_explorer.BlockExplorerUpdater;
import com.eletac.tronwallet.block_explorer.TransactionItemListAdapter;
import com.eletac.tronwallet.database.Transaction;
import com.eletac.tronwallet.wallet.confirm_transaction.ConfirmTransactionActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;

import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.Wallet;
import org.tron.walletserver.WalletManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionHistoryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView mTransactions_RecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private LinearLayoutManager mLayoutManager;
    private TransactionItemListAdapter mTransactionsItemListAdapter;

    private List<Protocol.Transaction> mTransactions;
    private Wallet mWallet;

    private TransactionSentBroadcastReceiver mTransactionSentBroadcastReceiver;

    public TransactionHistoryFragment() {
        // Required empty public constructor
    }

    public static TransactionHistoryFragment newInstance() {
        TransactionHistoryFragment fragment = new TransactionHistoryFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWallet = WalletManager.getSelectedWallet();
        mTransactions = new ArrayList<>();
        loadTransactions();
        mTransactionsItemListAdapter = new TransactionItemListAdapter(getContext(), mTransactions);

        mTransactionSentBroadcastReceiver = new TransactionSentBroadcastReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mTransactionSentBroadcastReceiver, new IntentFilter(ConfirmTransactionActivity.TRANSACTION_SENT));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_transaction_history, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mTransactionSentBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        mWallet = WalletManager.getSelectedWallet();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTransactions_RecyclerView = view.findViewById(R.id.TransactionHistory_transactions_recyclerView);
        //mSwipeRefreshLayout = view.findViewById(R.id.TransactionHistory_swipe_container);

        //mSwipeRefreshLayout.setOnRefreshListener(this);

        mLayoutManager = new WrapContentLinearLayoutManager(getContext());
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);

        mTransactions_RecyclerView.setHasFixedSize(true);
        mTransactions_RecyclerView.setLayoutManager(mLayoutManager);
        mTransactions_RecyclerView.setAdapter(mTransactionsItemListAdapter);
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
    }

    private void loadTransactions() {
        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                List<Transaction> dbTransactions = TronWalletApplication.getDatabase().transactionDao().getAllTransactions();

                // TODO load and compare with transactions from node if possible

                List<Protocol.Transaction> transactions = new ArrayList<>();
                for(Transaction dbTransaction : dbTransactions) {
                    if(dbTransaction.senderAddress.equals(mWallet.getAddress())) {
                        transactions.add(dbTransaction.transaction);
                    }
                }
                AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                    @Override
                    public void doInUIThread() {
                        mTransactions.clear();
                        mTransactions.addAll(transactions);
                        mTransactionsItemListAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }


    private class TransactionSentBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            loadTransactions();
        }
    }
}
