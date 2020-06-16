package com.quarkonium.qpocket.tron;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigInteger;

/**
 * Transaction request object used the below methods.
 * <ol>
 * <li>eth_call</li>
 * <li>eth_sendTransaction</li>
 * <li>eth_estimateGas</li>
 * </ol>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrxRequest {

    private String contract_address;
    private String owner_address;
    private String function_selector;
    private String parameter;
    private BigInteger fee_limit;
    private BigInteger call_value;

    public TrxRequest(String contract_address,
                      String owner_address,
                      String function_selector,
                      String parameter,
                      BigInteger fee_limit,
                      BigInteger call_value) {
        this.contract_address = contract_address;
        this.owner_address = owner_address;
        this.function_selector = function_selector;
        this.parameter = parameter;
        this.fee_limit = fee_limit;
        this.call_value = call_value;

    }

    public static TrxRequest createContractTransaction(
            String from, String contract, String function, String parameter,
            BigInteger fee, BigInteger callValue) {
        return new TrxRequest(contract, from, function, parameter, fee, callValue);
    }

    public String getContract_address() {
        return contract_address;
    }

    public void setContract_address(String contract_address) {
        this.contract_address = contract_address;
    }

    public String getOwner_address() {
        return owner_address;
    }

    public void setOwner_address(String owner_address) {
        this.owner_address = owner_address;
    }

    public String getFunction_selector() {
        return function_selector;
    }

    public void setFunction_selector(String function_selector) {
        this.function_selector = function_selector;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public BigInteger getFee_limit() {
        return fee_limit;
    }

    public void setFee_limit(BigInteger fee_limit) {
        this.fee_limit = fee_limit;
    }

    public BigInteger getCall_value() {
        return call_value;
    }

    public void setCall_value(BigInteger call_value) {
        this.call_value = call_value;
    }
}