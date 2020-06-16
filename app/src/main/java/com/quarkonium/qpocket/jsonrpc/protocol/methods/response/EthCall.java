package com.quarkonium.qpocket.jsonrpc.protocol.methods.response;

import com.quarkonium.qpocket.jsonrpc.protocol.core.Response;

public class EthCall extends Response<String> {
    public String getValue() {
        return getResult();
    }
}