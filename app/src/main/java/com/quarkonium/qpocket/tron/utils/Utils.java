package com.quarkonium.qpocket.tron.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.quarkonium.qpocket.R;

import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    public static void saveAccountNet(Context context, String walletAddress, GrpcAPI.AccountNetMessage accountNet) {
        if (context != null && accountNet != null && !TextUtils.isEmpty(walletAddress)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(walletAddress, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putLong(context.getString(R.string.net_limit_key), accountNet.getNetLimit());
            editor.putLong(context.getString(R.string.net_used_key), accountNet.getNetUsed());
            editor.putLong(context.getString(R.string.net_free_limit_key), accountNet.getFreeNetLimit());
            editor.putLong(context.getString(R.string.net_free_used_key), accountNet.getFreeNetUsed());

            editor.apply();
        }
    }

    public static void saveAccountRes(Context context, String walletAddress, GrpcAPI.AccountResourceMessage accountRes) {
        if (context != null && accountRes != null && !TextUtils.isEmpty(walletAddress)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(walletAddress, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putLong(context.getString(R.string.energy_limit_key), accountRes.getEnergyLimit());
            editor.putLong(context.getString(R.string.energy_used_key), accountRes.getEnergyUsed());

            editor.apply();
        }
    }

    public static GrpcAPI.AccountResourceMessage getAccountRes(Context context, String walletAddress) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(walletAddress, Context.MODE_PRIVATE);

        GrpcAPI.AccountResourceMessage.Builder accountResMessage = GrpcAPI.AccountResourceMessage.newBuilder();

        accountResMessage.setEnergyLimit(sharedPreferences.getLong(context.getString(R.string.energy_limit_key), 0));
        accountResMessage.setEnergyUsed(sharedPreferences.getLong(context.getString(R.string.energy_used_key), 0));

        GrpcAPI.AccountResourceMessage message = accountResMessage.build();
        if (message != null) {
            return message;
        }
        return GrpcAPI.AccountResourceMessage.getDefaultInstance();
    }


    public static GrpcAPI.AccountNetMessage getAccountNet(Context context, String walletAddress) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(walletAddress, Context.MODE_PRIVATE);

        GrpcAPI.AccountNetMessage.Builder accountNetMessage = GrpcAPI.AccountNetMessage.newBuilder();

        accountNetMessage.setNetLimit(sharedPreferences.getLong(context.getString(R.string.net_limit_key), 0));
        accountNetMessage.setNetUsed(sharedPreferences.getLong(context.getString(R.string.net_used_key), 0));
        accountNetMessage.setFreeNetLimit(sharedPreferences.getLong(context.getString(R.string.net_free_limit_key), 0));
        accountNetMessage.setFreeNetUsed(sharedPreferences.getLong(context.getString(R.string.net_free_used_key), 0));

        GrpcAPI.AccountNetMessage message = accountNetMessage.build();
        if (message != null) {
            return message;
        }
        return GrpcAPI.AccountNetMessage.getDefaultInstance();
    }

    public static long getAccountAssetAmount(Protocol.Account account, String assetName) {
        Map<String, Long> assets = account.getAssetMap();
        return assets.containsKey(assetName) ? assets.get(assetName) : 0;
    }

    public static double round(double value, int places, RoundingMode mode) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, mode);
        return bd.doubleValue();
    }


    public static String getContractName(Protocol.Transaction.Contract contract) {
        if (contract == null)
            return "";

        switch (contract.getType()) {
            case AccountCreateContract:
                return "AccountCreateContract";
            case TransferContract:
                return "TransferContract";
            case TransferAssetContract:
                return "TransferAssetContract";
            case VoteAssetContract:
                return "VoteAssetContract";
            case VoteWitnessContract:
                return "VoteWitnessContract";
            case WitnessCreateContract:
                return "WitnessCreateContract";
            case AssetIssueContract:
                return "AssetIssueContract";
            case WitnessUpdateContract:
                return "WitnessUpdateContract";
            case ParticipateAssetIssueContract:
                return "ParticipateAssetIssueContract";
            case AccountUpdateContract:
                return "AccountUpdateContract";
            case FreezeBalanceContract:
                return "FreezeBalanceContract";
            case UnfreezeBalanceContract:
                return "UnfreezeBalanceContract";
            case WithdrawBalanceContract:
                return "WithdrawBalanceContract";
            case UnfreezeAssetContract:
                return "UnfreezeAssetContract";
            case UpdateAssetContract:
                return "UpdateAssetContract";
            case CustomContract:
                return "CustomContract";
            case UNRECOGNIZED:
                return "UNRECOGNIZED";
        }
        return "";
    }


    public static void saveAccount(Context context, String walletAddress, Protocol.Account account) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(walletAddress, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

//        List<Protocol.Vote> votesList = account.getVotesList();
//        Map<String, Long> votesMap = new HashMap<>();
//        for (Protocol.Vote vote : votesList) {
//            String voteAddress = WalletManager.encode58Check(vote.getVoteAddress().toByteArray());
//            if (!voteAddress.equals(""))
//                votesMap.put(voteAddress, vote.getVoteCount());
//        }
//        editor.putString(context.getString(R.string.votes_key), new Gson().toJson(votesMap));

//        editor.putLong(context.getString(R.string.bandwidth_key), account.getNetUsage());

        List<Protocol.Account.Frozen> frozenList = account.getFrozenList();
        HashMap<Long, Long> frozenMap = new HashMap<>();
        for (Protocol.Account.Frozen frozen : frozenList) {
            long balance = frozen.getFrozenBalance();
            if (frozenMap.containsKey(frozen.getExpireTime())) {
                Long value = frozenMap.get(frozen.getExpireTime());
                balance += value != null ? value : 0;
            }
            frozenMap.put(frozen.getExpireTime(), balance);
        }
        editor.putString(context.getString(R.string.frozen_key), new Gson().toJson(frozenMap));

        frozenMap = new HashMap<>();
        Protocol.Account.Frozen frozen = account.getAccountResource().getFrozenBalanceForEnergy();
        frozenMap.put(frozen.getExpireTime(), frozen.getFrozenBalance());
        editor.putString(context.getString(R.string.frozen_energy_key), new Gson().toJson(frozenMap));
        editor.apply();
    }

    public static List<Protocol.Account.Frozen> getFrozenBandWidth(Context context, String walletAddress) {
        List<Protocol.Account.Frozen> frozen = new ArrayList<>();

        SharedPreferences sharedPreferences = context.getSharedPreferences(walletAddress, Context.MODE_PRIVATE);
        Map<Long, Long> frozenMap = new Gson()
                .fromJson(
                        sharedPreferences.getString(context.getString(R.string.frozen_key), ""),
                        new TypeToken<Map<Long, Long>>() {
                        }.getType());
        if (frozenMap != null) {
            for (Map.Entry<Long, Long> entry : frozenMap.entrySet()) {
                Protocol.Account.Frozen.Builder frozenBuilder = Protocol.Account.Frozen.newBuilder();
                frozenBuilder.setExpireTime(entry.getKey());
                frozenBuilder.setFrozenBalance(entry.getValue());
                frozen.add(frozenBuilder.build());
            }
        }

        return frozen;
    }

    public static Protocol.Account.Frozen getFrozenEnergy(Context context, String walletAddress) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(walletAddress, Context.MODE_PRIVATE);
        Map<Long, Long> frozenMap = new Gson()
                .fromJson(
                        sharedPreferences.getString(context.getString(R.string.frozen_energy_key), ""),
                        new TypeToken<Map<Long, Long>>() {
                        }.getType());
        if (frozenMap != null) {
            for (Map.Entry<Long, Long> entry : frozenMap.entrySet()) {
                Protocol.Account.Frozen.Builder frozenBuilder = Protocol.Account.Frozen.newBuilder();
                frozenBuilder.setExpireTime(entry.getKey());
                frozenBuilder.setFrozenBalance(entry.getValue());
                return frozenBuilder.build();
            }
        }

        return null;
    }
}
