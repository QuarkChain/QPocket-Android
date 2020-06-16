package com.quarkonium.qpocket.tron;

import android.text.TextUtils;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.tron.crypto.ECKey;
import com.quarkonium.qpocket.tron.crypto.Hash;
import com.quarkonium.qpocket.tron.keystore.Wallet;
import com.quarkonium.qpocket.tron.utils.ByteArray;
import com.quarkonium.qpocket.tron.keystore.CipherException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.math.ec.ECPoint;
import org.tron.api.GrpcAPI;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.math.BigInteger;
import java.util.Arrays;

public class TronWalletClient {

    //私钥转为地址
    public static String privateKeyToAddress(String privateKey) {
        BigInteger priK = new BigInteger(privateKey, 16);

        //获取public key
        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        ECDomainParameters CURVE = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
        ECPoint pubPoint = CURVE.getG().multiply(priK);
        byte[] pubBytes = pubPoint.getEncoded(/* uncompressed */ false);

        byte[] pubKey = Hash.sha3omit12(Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
        return encode58Check(pubKey);
    }

    //解析地址
    public static String encode58Check(byte[] input) {
        byte[] hash0 = Hash.sha256(input);
        byte[] hash1 = Hash.sha256(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

    //地址转为数组
    private static byte[] decode58Check(String address) {
        try {
            byte[] decodeCheck = Base58.decode(address);
            if (decodeCheck.length <= 4) {
                return null;
            }
            byte[] decodeData = new byte[decodeCheck.length - 4];
            System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
            byte[] hash0 = Hash.sha256(decodeData);
            byte[] hash1 = Hash.sha256(hash0);
            if (hash1[0] == decodeCheck[decodeData.length] &&
                    hash1[1] == decodeCheck[decodeData.length + 1] &&
                    hash1[2] == decodeCheck[decodeData.length + 2] &&
                    hash1[3] == decodeCheck[decodeData.length + 3]) {
                return decodeData;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    //是否有效tron地址
    public static boolean isTronAddressValid(String address) {
        byte[] data = decodeFromBase58Check(address);
        return data != null;
    }

    public static boolean isTronErc10TokenAddressValid(String address) {
        try {
            Integer.parseInt(address);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static byte[] decodeFromBase58Check(String addressBase58) {
        if (TextUtils.isEmpty(addressBase58)) {
            return null;
        }
        byte[] address = decode58Check(addressBase58);
        if (!isAddressValid(address)) {
            return null;
        }
        return address;
    }

    public static String decodeBase58checkToHexString(String addressBase58) {
        return ByteArray.toHexString(decodeFromBase58Check(addressBase58));
    }

    private static boolean isAddressValid(byte[] address) {
        if (address == null || address.length == 0) {
            return false;
        }
        if (address.length != Parameter.CommonConstant.ADDRESS_SIZE) {
            return false;
        }
        byte preFixbyte = address[0];
        if (preFixbyte != Parameter.CommonConstant.ADD_PRE_FIX_BYTE) {
            return false;
        }
        return true;
    }

    public static synchronized Protocol.Transaction parseTransactionByJson(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        Protocol.Transaction.Builder builder = Protocol.Transaction.newBuilder();
        try {
            JSONObject object = new JSONObject(json);
            object = object.getJSONObject("raw_data");

            Protocol.Transaction.raw.Builder raw = Protocol.Transaction.raw.newBuilder();
            String refBlockBytes = object.getString("ref_block_bytes");
            raw.setRefBlockBytes(ByteString.copyFrom(Numeric.hexStringToByteArray(refBlockBytes)));

            String refBlockHash = object.getString("ref_block_hash");
            raw.setRefBlockHash(ByteString.copyFrom(Numeric.hexStringToByteArray(refBlockHash)));

            raw.setExpiration(object.getLong("expiration"));
            raw.setTimestamp(object.getLong("timestamp"));
            raw.setFeeLimit(object.getLong("fee_limit"));

            if (object.has("contract")) {
                JSONArray jsonArray = object.getJSONArray("contract");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject value = jsonArray.getJSONObject(i);
                    Protocol.Transaction.Contract.Builder contract = Protocol.Transaction.Contract.newBuilder();

                    Any.Builder any = Any.newBuilder();
                    any.setTypeUrl(value.getJSONObject("parameter").getString("type_url"));

                    Contract.TriggerSmartContract.Builder smartContract = Contract.TriggerSmartContract.newBuilder();
                    JSONObject smartContractJson = value.getJSONObject("parameter").getJSONObject("value");
                    String data = smartContractJson.getString("data");
                    smartContract.setData(ByteString.copyFrom(Numeric.hexStringToByteArray(data)));

                    String ownerAddress = smartContractJson.getString("owner_address");
                    smartContract.setOwnerAddress(ByteString.copyFrom(Numeric.hexStringToByteArray(ownerAddress)));

                    String contractAddress = smartContractJson.getString("contract_address");
                    smartContract.setContractAddress(ByteString.copyFrom(Numeric.hexStringToByteArray(contractAddress)));

                    //trx转账金额
                    if (smartContractJson.has("call_value")) {
                        smartContract.setCallValue(smartContractJson.getLong("call_value"));
                    }

                    //trc10 token转账
                    if (smartContractJson.has("call_token_value")) {
                        //trc10 token
                        smartContract.setCallTokenValue(smartContractJson.getLong("call_token_value"));
                    }
                    if (smartContractJson.has("token_id")) {
                        smartContract.setTokenId(smartContractJson.getLong("token_id"));
                    }

                    any.setValue(smartContract.build().toByteString());
                    contract.setParameter(any);

                    Protocol.Transaction.Contract.ContractType[] types = Protocol.Transaction.Contract.ContractType.values();
                    String type = value.getString("type");
                    for (Protocol.Transaction.Contract.ContractType temp : types) {
                        if (TextUtils.equals(type, temp.name())) {
                            contract.setType(temp);
                            break;
                        }
                    }
                    raw.addContract(contract);
                }
            }
            builder.setRawData(raw);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    private static String buildMethodId(String methodSignature) {
        byte[] input = methodSignature.getBytes();
        byte[] hash = com.quarkonium.qpocket.crypto.Hash.sha3(input);
        return Numeric.toHexString(hash).substring(2, 10);
    }

    private static Contract.TriggerSmartContract triggerCallContract(byte[] address, byte[] contractAddress,
                                                                     long callValue, byte[] data, long tokenValue, String tokenId) {
        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(address));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(data));
        builder.setCallValue(callValue);

        if (!TextUtils.isEmpty(tokenId)) {
            builder.setCallTokenValue(tokenValue);
            builder.setTokenId(Long.parseLong(tokenId));
        }
        return builder.build();
    }

    //创建交易
    public static synchronized Contract.TransferContract createTransferContract(byte[] to, byte[] owner,
                                                                                long amount) {
        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    //创建Token交易
    public static synchronized Contract.TransferAssetContract createTransferAssetContract(byte[] to,
                                                                                          byte[] assertName, byte[] owner,
                                                                                          long amount) {
        Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }


    private static synchronized Contract.FreezeBalanceContract createFreezeBalanceContract(byte[] owner, long frozenBalance, long frozenDuration, Contract.ResourceCode resource) {
        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(owner);
        builder.setOwnerAddress(byteAddreess)
                .setFrozenBalance(frozenBalance)
                .setFrozenDuration(frozenDuration)
                .setResource(resource);
        return builder.build();
    }

    private static synchronized Contract.UnfreezeBalanceContract createUnfreezeBalanceContract(byte[] owner, Contract.ResourceCode resourceCode) {
        Contract.UnfreezeBalanceContract.Builder builder = Contract.UnfreezeBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(owner);

        builder.setOwnerAddress(byteAddreess);
        builder.setResource(resourceCode);

        return builder.build();
    }

    //获取私钥 长度为64
    public static String getPrivateKeyByKeyStore(String password, String keyStore) throws CipherException {
        ECKey ecKeyPair = Wallet.decrypt(password, keyStore);
        return Numeric.toHexStringNoPrefixZeroPadded(ecKeyPair.getPrivKey(), 64);
    }

    private GrpcClient rpcCli;

    public TronWalletClient() {
        rpcCli = GrpcClient.getInstance();
    }

    //获取钱包balance
    public Protocol.Account queryAccount(byte[] address, boolean useSolidity) {
        return rpcCli.queryAccount(address, useSolidity);
    }

    //获取带宽
    public GrpcAPI.AccountNetMessage getAccountNet(byte[] address) {
        return rpcCli.getAccountNet(address);
    }

    //获取能量
    public GrpcAPI.AccountResourceMessage getAccountRes(byte[] address) {
        return rpcCli.getAccountRes(address);
    }

    //获取from交易记录
    public GrpcAPI.TransactionListExtention getTransactionsFromThis(byte[] address, int offset, int limit) {
        return rpcCli.getTransactionsFromThis(address, offset, limit);
    }

    //获取to交易记录
    public GrpcAPI.TransactionListExtention getTransactionsToThis(byte[] address, int offset, int limit) {
        return rpcCli.getTransactionsToThis(address, offset, limit);
    }

    //获取详情
    public Protocol.TransactionInfo getTransactionInfo(String txID) {
        return rpcCli.getTransactionInfo(txID);
    }

    public synchronized Protocol.Transaction createTransaction4Transfer(Contract.TransferContract contract) {
        return rpcCli.createTransaction(contract);
    }

    public synchronized Protocol.Transaction createTransferAssetTransaction(Contract.TransferAssetContract contract) {
        return rpcCli.createTransferAssetTransaction(contract);
    }

    //发送交易
    public GrpcAPI.Return broadcastTransaction(Protocol.Transaction transaction) {
        return rpcCli.broadcastTransaction(transaction);
    }

    //创建冻结交易
    public synchronized Protocol.Transaction createFreezeBalanceTransaction(byte[] owner, long frozenBalance, long frozenDuration, Contract.ResourceCode resource) {
        Contract.FreezeBalanceContract contract = createFreezeBalanceContract(owner, frozenBalance, frozenDuration, resource);
        return rpcCli.createTransaction(contract);
    }

    //创建解冻结交易
    public synchronized Protocol.Transaction createUnfreezeBalanceTransaction(byte[] owner, Contract.ResourceCode resourceCode) {
        Contract.UnfreezeBalanceContract contract = createUnfreezeBalanceContract(owner, resourceCode);
        return rpcCli.createTransaction(contract);
    }

    public synchronized Contract.AssetIssueContract getAssetIssueById(String id) {
        return rpcCli.getAssetIssueById(id);
    }

    public synchronized GrpcAPI.TransactionExtention triggerContract(byte[] addressBytes, byte[] contractAddress, long callValue, byte[] input, long tokenCallValue, String tokenId) {
        Contract.TriggerSmartContract triggerContract = triggerCallContract(addressBytes, contractAddress, callValue, input, tokenCallValue, tokenId);
        return rpcCli.triggerContract(triggerContract);
    }
}
