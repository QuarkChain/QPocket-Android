package com.quarkonium.qpocket.abi.datatypes.generated;

import com.quarkonium.qpocket.abi.datatypes.StaticArray;
import com.quarkonium.qpocket.abi.datatypes.Type;

import java.util.List;

/**
 * Auto generated code.
 * <p><strong>Do not modifiy!</strong>
 * <p>Please use org.web3j.codegen.AbiTypesGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 */
public class StaticArray2<T extends Type> extends StaticArray<T> {
    public StaticArray2(List<T> values) {
        super(2, values);
    }

    public StaticArray2(T... values) {
        super(2, values);
    }
}
