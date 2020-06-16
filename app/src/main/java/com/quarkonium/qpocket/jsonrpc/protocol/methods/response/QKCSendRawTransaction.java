package com.quarkonium.qpocket.jsonrpc.protocol.methods.response;


import com.quarkonium.qpocket.jsonrpc.protocol.core.Response;

/**
 * qkc_sendTransaction.
 */
public class QKCSendRawTransaction extends Response<String> {

    public String getTransactionHash() {
        return getResult();
    }
}
