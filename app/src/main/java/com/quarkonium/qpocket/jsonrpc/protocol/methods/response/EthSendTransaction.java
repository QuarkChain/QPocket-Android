package com.quarkonium.qpocket.jsonrpc.protocol.methods.response;

import com.quarkonium.qpocket.jsonrpc.protocol.core.Response;

public class EthSendTransaction extends Response<String> {
    public String getTransactionHash() {
        return getResult();
    }
}
