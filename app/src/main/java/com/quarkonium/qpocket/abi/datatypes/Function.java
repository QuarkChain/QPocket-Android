package com.quarkonium.qpocket.abi.datatypes;


import java.util.List;

import com.quarkonium.qpocket.abi.TypeReference;

import static com.quarkonium.qpocket.abi.Utils.convert;

/**
 * Function type.
 */
public class Function {
    private String name;
    private List<Type> inputParameters;
    private List<TypeReference<Type>> outputParameters;

    public Function(String name, List<Type> inputParameters,
                    List<TypeReference<?>> outputParameters) {
        this.name = name;
        this.inputParameters = inputParameters;
        this.outputParameters = convert(outputParameters);
    }

    public String getName() {
        return name;
    }

    public List<Type> getInputParameters() {
        return inputParameters;
    }

    public List<TypeReference<Type>> getOutputParameters() {
        return outputParameters;
    }
}
