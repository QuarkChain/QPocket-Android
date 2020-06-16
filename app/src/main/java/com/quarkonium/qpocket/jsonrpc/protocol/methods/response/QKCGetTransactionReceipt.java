package com.quarkonium.qpocket.jsonrpc.protocol.methods.response;

import com.quarkonium.qpocket.jsonrpc.protocol.core.Response;

/**
 * transaction
 */
public class QKCGetTransactionReceipt extends Response<TransactionReceipt> {

    public TransactionReceipt getTransaction() {
        return getResult();
    }
}
