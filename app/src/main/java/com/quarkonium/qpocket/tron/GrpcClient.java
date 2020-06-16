package com.quarkonium.qpocket.tron;

import android.text.TextUtils;

import com.google.protobuf.ByteString;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.tron.utils.ByteArray;

import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountPaginated;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockLimit;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.api.WalletExtensionGrpc;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClient {

    private static GrpcClient rpcCli;

    public synchronized static GrpcClient getInstance() {
        if (rpcCli == null || rpcCli.isShutDown()) {
            String ip = Constant.TRON_MAIN_NET_GRPC_PATH;
            String ip_sol = Constant.TRON_MAIN_NET_GRPC_SOLIDITY_PATH;
            rpcCli = new GrpcClient(ip, ip_sol);
        }
        return rpcCli;
    }

    private ManagedChannel channelFull = null;
    private ManagedChannel channelSolidity = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
    private WalletExtensionGrpc.WalletExtensionBlockingStub blockingStubExtension = null;


    private GrpcClient(String fullnode, String soliditynode) {
        if (!TextUtils.isEmpty(fullnode)) {
            channelFull = ManagedChannelBuilder.forTarget(fullnode)
                    .usePlaintext(true)
                    .build();
            blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        }
        if (!TextUtils.isEmpty(soliditynode)) {
            channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
                    .usePlaintext(true)
                    .build();
            blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
            blockingStubExtension = WalletExtensionGrpc.newBlockingStub(channelSolidity);
        }
    }

    public boolean isShutDown() {
        return channelFull == null || channelSolidity == null || channelFull.isShutdown() || channelSolidity.isShutdown();
    }

    public void shutdown() {
        if (channelFull != null) {
            channelFull.shutdown();
        }
        if (channelSolidity != null) {
            channelSolidity.shutdown();
        }
    }

    public Account queryAccount(byte[] address, boolean useSolidity) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        if (blockingStubSolidity != null && useSolidity) {
            return blockingStubSolidity.getAccount(request);
        } else {
            return blockingStubFull.getAccount(request);
        }
    }

    public Transaction createTransaction(Contract.AccountUpdateContract contract) {
        return blockingStubFull.updateAccount(contract);
    }

    public Transaction createTransaction(Contract.TransferContract contract) {
        return blockingStubFull.createTransaction(contract);
    }

    public Transaction createTransaction(Contract.FreezeBalanceContract contract) {
        return blockingStubFull.freezeBalance(contract);
    }

    public Transaction createTransaction(Contract.WithdrawBalanceContract contract) {
        return blockingStubFull.withdrawBalance(contract);
    }

    public Transaction createTransaction(Contract.UnfreezeBalanceContract contract) {
        return blockingStubFull.unfreezeBalance(contract);
    }

    public Transaction createTransferAssetTransaction(Contract.TransferAssetContract contract) {
        return blockingStubFull.transferAsset(contract);
    }

    public Transaction createParticipateAssetIssueTransaction(Contract.ParticipateAssetIssueContract contract) {
        return blockingStubFull.participateAssetIssue(contract);
    }

    public Transaction createAccount(Contract.AccountCreateContract contract) {
        return blockingStubFull.createAccount(contract);
    }

    public Transaction createAssetIssue(Contract.AssetIssueContract contract) {
        return blockingStubFull.createAssetIssue(contract);
    }

    public Transaction voteWitnessAccount(Contract.VoteWitnessContract contract) {
        return blockingStubFull.voteWitnessAccount(contract);
    }

    public Transaction createWitness(Contract.WitnessCreateContract contract) {
        return blockingStubFull.createWitness(contract);
    }

    public GrpcAPI.Return broadcastTransaction(Transaction signaturedTransaction) {
        int i = 10;
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(signaturedTransaction);
        while (response.getResult() == false && response.getCode() == response_code.SERVER_BUSY
                && i > 0) {
            i--;
            response = blockingStubFull.broadcastTransaction(signaturedTransaction);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    public GrpcAPI.BlockExtention getBlock(long blockNum, boolean useSolidity) {
        if (blockNum < 0) {
            if (blockingStubSolidity != null && useSolidity) {
                return blockingStubSolidity.getNowBlock2(EmptyMessage.newBuilder().build());
            } else {
                return blockingStubFull.getNowBlock2(EmptyMessage.newBuilder().build());
            }
        }
        NumberMessage.Builder builder = NumberMessage.newBuilder();
        builder.setNum(blockNum);
        if (blockingStubSolidity != null && useSolidity) {
            return blockingStubSolidity.getBlockByNum2(builder.build());
        } else {
            return blockingStubFull.getBlockByNum2(builder.build());
        }
    }

//  public Optional<AccountList> listAccounts() {
//    AccountList accountList = blockingStubSolidity
//        .listAccounts(EmptyMessage.newBuilder().build());
//    return Optional.ofNullable(accountList);
//
//  }

    public WitnessList listWitnesses(boolean useSolidity) {
        if (blockingStubSolidity != null && useSolidity) {
            WitnessList witnessList = blockingStubSolidity.listWitnesses(EmptyMessage.newBuilder().build());
            return witnessList;
        } else {
            WitnessList witnessList = blockingStubFull.listWitnesses(EmptyMessage.newBuilder().build());
            return witnessList;
        }
    }

    public AssetIssueList getAssetIssueList(long offset, long limit) {
        GrpcAPI.PaginatedMessage.Builder pageMessageBuilder = GrpcAPI.PaginatedMessage.newBuilder();
        pageMessageBuilder.setOffset(offset);
        pageMessageBuilder.setLimit(limit);
        if (blockingStubSolidity != null) {
            AssetIssueList assetIssueList = blockingStubSolidity
                    .getPaginatedAssetIssueList(pageMessageBuilder.build());
            return assetIssueList;
        } else {
            AssetIssueList assetIssueList = blockingStubFull
                    .getPaginatedAssetIssueList(pageMessageBuilder.build());
            return assetIssueList;
        }
    }

    public AssetIssueList getAssetIssueList(boolean useSolidity) {
        if (blockingStubSolidity != null && useSolidity) {
            AssetIssueList assetIssueList = blockingStubSolidity
                    .getAssetIssueList(EmptyMessage.newBuilder().build());
            return (assetIssueList);
        } else {
            AssetIssueList assetIssueList = blockingStubFull
                    .getAssetIssueList(EmptyMessage.newBuilder().build());
            return (assetIssueList);
        }
    }

    public NodeList listNodes() {
        NodeList nodeList = blockingStubFull.listNodes(EmptyMessage.newBuilder().build());
        return (nodeList);
    }

    public AssetIssueList getAssetIssueByAccount(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        AssetIssueList assetIssueList = blockingStubFull.getAssetIssueByAccount(request);
        return (assetIssueList);
    }

    public AccountNetMessage getAccountNet(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccountNet(request);
    }

    public GrpcAPI.AccountResourceMessage getAccountRes(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccountResource(request);
    }

    public Contract.AssetIssueContract getAssetIssueByName(String assetName) {
        ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
        BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
        return blockingStubFull.getAssetIssueByName(request);
    }

    public Contract.AssetIssueContract getAssetIssueById(String id) {
        ByteString assetNameBs = ByteString.copyFrom(id.getBytes());
        BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
        return blockingStubFull.getAssetIssueById(request);
    }

    public NumberMessage getTotalTransaction() {
        return blockingStubFull.totalTransaction(EmptyMessage.newBuilder().build());
    }

    public NumberMessage getNextMaintenanceTime() {
        return blockingStubFull.getNextMaintenanceTime(EmptyMessage.newBuilder().build());
    }

    public GrpcAPI.TransactionListExtention getTransactionsFromThis(byte[] address, int offset, int limit) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account account = Account.newBuilder().setAddress(addressBS).build();
        AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
        accountPaginated.setAccount(account);
        accountPaginated.setOffset(offset);
        accountPaginated.setLimit(limit);
        GrpcAPI.TransactionListExtention transactionList = blockingStubExtension
                .getTransactionsFromThis2(accountPaginated.build());
        return (transactionList);
    }

    public GrpcAPI.TransactionListExtention getTransactionsToThis(byte[] address, int offset, int limit) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account account = Account.newBuilder().setAddress(addressBS).build();
        AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
        accountPaginated.setAccount(account);
        accountPaginated.setOffset(offset);
        accountPaginated.setLimit(limit);
        GrpcAPI.TransactionListExtention transactionList = blockingStubExtension
                .getTransactionsToThis2(accountPaginated.build());
        return (transactionList);
    }

    public Transaction getTransactionById(String txID, boolean useSolidity) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txID));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        if (blockingStubSolidity != null && useSolidity) {
            Transaction transaction = blockingStubSolidity.getTransactionById(request);
            return (transaction);
        } else {
            Transaction transaction = blockingStubFull.getTransactionById(request);
            return (transaction);
        }
    }

    public Block getBlockById(String blockID) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(blockID));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        Block block = blockingStubFull.getBlockById(request);
        return (block);
    }

    public GrpcAPI.BlockListExtention getBlockByLimitNext(long start, long end) {
        BlockLimit.Builder builder = BlockLimit.newBuilder();
        builder.setStartNum(start);
        builder.setEndNum(end);
        GrpcAPI.BlockListExtention blockList = blockingStubFull.getBlockByLimitNext2(builder.build());
        return (blockList);
    }

    public GrpcAPI.BlockListExtention getBlockByLatestNum(long num) {
        NumberMessage numberMessage = NumberMessage.newBuilder().setNum(num).build();
        GrpcAPI.BlockListExtention blockList = blockingStubFull.getBlockByLatestNum2(numberMessage);
        return (blockList);
    }

    public Protocol.TransactionInfo getTransactionInfo(String txID) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txID));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        if (blockingStubSolidity != null) {
            return blockingStubSolidity.getTransactionInfoById(request);
        }
        return Protocol.TransactionInfo.getDefaultInstance();
    }

    public GrpcAPI.TransactionExtention triggerContract(Contract.TriggerSmartContract triggerContract) {
        return blockingStubFull.triggerContract(triggerContract);
    }
}
