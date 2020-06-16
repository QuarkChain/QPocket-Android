package com.quarkonium.qpocket.crypto;

import com.quarkonium.qpocket.crypto.rlp.RlpEncoder;
import com.quarkonium.qpocket.crypto.rlp.RlpList;
import com.quarkonium.qpocket.crypto.rlp.RlpString;
import com.quarkonium.qpocket.crypto.rlp.RlpType;
import com.quarkonium.qpocket.crypto.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Smart Contract utility functions.
 */
public class ContractUtils {

    /**
     * Generate a smart contract address. This enables you to identify what address a
     * smart contract will be deployed to on the network.
     *
     * @param address of sender
     * @param nonce   of transaction
     * @return the generated smart contract address
     */
    public static byte[] generateContractAddress(byte[] address, BigInteger nonce) {
        List<RlpType> values = new ArrayList<>();

        values.add(RlpString.create(address));
        values.add(RlpString.create(nonce));
        RlpList rlpList = new RlpList(values);

        byte[] encoded = RlpEncoder.encode(rlpList);
        byte[] hashed = Hash.sha3(encoded);
        return Arrays.copyOfRange(hashed, 12, hashed.length);
    }

    public static String generateContractAddress(String address, BigInteger nonce) {
        byte[] result = generateContractAddress(Numeric.hexStringToByteArray(address), nonce);
        return Numeric.toHexString(result);
    }
}
