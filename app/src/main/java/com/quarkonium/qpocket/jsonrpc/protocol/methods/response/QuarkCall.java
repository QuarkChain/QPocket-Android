package com.quarkonium.qpocket.jsonrpc.protocol.methods.response;


import com.quarkonium.qpocket.jsonrpc.protocol.core.Response;

/**
 * eth_call.
 */
public class QuarkCall extends Response<String> {
    public String getValue() {
        return getResult();
    }
}
