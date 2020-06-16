package com.quarkonium.qpocket.jsonrpc.protocol.methods.response;

import com.quarkonium.qpocket.jsonrpc.protocol.core.Response;

/**
 * transaction
 */
public class QKCGetTransaction extends Response<TransactionDetail> {

    public TransactionDetail getTransaction() {
        return getResult();
    }
}
