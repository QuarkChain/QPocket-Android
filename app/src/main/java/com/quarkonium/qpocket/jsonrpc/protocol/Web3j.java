package com.quarkonium.qpocket.jsonrpc.protocol;

import com.quarkonium.qpocket.jsonrpc.protocol.rx.Web3jRx;

/**
 * JSON-RPC Request object building factory.
 */
public interface Web3j extends QuarkChain, Ethereum, Web3jRx {

}
