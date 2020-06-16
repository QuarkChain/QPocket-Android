package com.quarkonium.qpocket.jsonrpc.protocol.methods.response;

import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.jsonrpc.protocol.core.Response;

import java.math.BigInteger;

public class EthEstimateGas extends Response<String> {
    public BigInteger getAmountUsed() {
        return Numeric.decodeQuantity(getResult());
    }
}
