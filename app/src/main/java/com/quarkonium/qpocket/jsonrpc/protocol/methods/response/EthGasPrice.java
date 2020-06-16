package com.quarkonium.qpocket.jsonrpc.protocol.methods.response;

import com.quarkonium.qpocket.crypto.utils.Numeric;
import com.quarkonium.qpocket.jsonrpc.protocol.core.Response;

import java.math.BigInteger;

public class EthGasPrice extends Response<String> {
    public BigInteger getGasPrice() {
        return Numeric.decodeQuantity(getResult());
    }
}
