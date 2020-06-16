package com.quarkonium.qpocket.crypto;

import com.quarkonium.qpocket.crypto.rlp.RlpDecoder;
import com.quarkonium.qpocket.crypto.rlp.RlpList;
import com.quarkonium.qpocket.crypto.rlp.RlpString;
import com.quarkonium.qpocket.crypto.utils.Numeric;


import java.math.BigInteger;

public class TransactionDecoder {

    public static RawTransaction decode(String hexTransaction) {
        byte[] transaction = Numeric.hexStringToByteArray(hexTransaction);
        RlpList rlpList = RlpDecoder.decode(transaction);
        RlpList values = (RlpList) rlpList.getValues().get(0);
        BigInteger nonce = ((RlpString) values.getValues().get(0)).asPositiveBigInteger();
        BigInteger gasPrice = ((RlpString) values.getValues().get(1)).asPositiveBigInteger();
        BigInteger gasLimit = ((RlpString) values.getValues().get(2)).asPositiveBigInteger();
        String to = ((RlpString) values.getValues().get(3)).asString();
        BigInteger value = ((RlpString) values.getValues().get(4)).asPositiveBigInteger();
        String data = ((RlpString) values.getValues().get(5)).asString();

        BigInteger networkId = ((RlpString) values.getValues().get(6)).asPositiveBigInteger();
        BigInteger fromShard = ((RlpString) values.getValues().get(7)).asPositiveBigInteger();
        BigInteger toShard = ((RlpString) values.getValues().get(8)).asPositiveBigInteger();

        BigInteger gasTokenId = ((RlpString) values.getValues().get(9)).asPositiveBigInteger();
        BigInteger transferTokenId = ((RlpString) values.getValues().get(10)).asPositiveBigInteger();
        if (values.getValues().size() > 11) {
            byte v = ((RlpString) values.getValues().get(12)).getBytes()[0];
            byte[] r = Numeric.toBytesPadded(
                    Numeric.toBigInt(((RlpString) values.getValues().get(13)).getBytes()), 32);
            byte[] s = Numeric.toBytesPadded(
                    Numeric.toBigInt(((RlpString) values.getValues().get(14)).getBytes()), 32);
            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);

            return new SignedRawTransaction(nonce, gasPrice, gasLimit,
                    to, value, data, fromShard, toShard, networkId, signatureData, transferTokenId, gasTokenId);
        } else {
            return RawTransaction.createTransaction(nonce,
                    gasPrice, gasLimit, to, value, fromShard, toShard, data, networkId, transferTokenId, gasTokenId);
        }
    }
}
