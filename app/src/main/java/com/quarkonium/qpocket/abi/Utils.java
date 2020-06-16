package com.quarkonium.qpocket.abi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import com.quarkonium.qpocket.abi.datatypes.DynamicArray;
import com.quarkonium.qpocket.abi.datatypes.DynamicBytes;
import com.quarkonium.qpocket.abi.datatypes.Fixed;
import com.quarkonium.qpocket.abi.datatypes.Int;
import com.quarkonium.qpocket.abi.datatypes.StaticArray;
import com.quarkonium.qpocket.abi.datatypes.Type;
import com.quarkonium.qpocket.abi.datatypes.Ufixed;
import com.quarkonium.qpocket.abi.datatypes.Uint;
import com.quarkonium.qpocket.abi.datatypes.Utf8String;

/**
 * Utility functions.
 */
public class Utils {
    private Utils() {}

    static <T extends Type> String getTypeName(TypeReference<T> typeReference) {
        try {
            java.lang.reflect.Type reflectedType = typeReference.getType();

            Class<?> type;
            if (reflectedType instanceof ParameterizedType) {
                type = (Class<?>) ((ParameterizedType) reflectedType).getRawType();
                return getParameterizedTypeName(typeReference, type);
            } else {
                type = Class.forName(((Class) reflectedType).getName());
                return getSimpleTypeName(type);
            }
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Invalid class reference provided", e);
        }
    }

    static String getSimpleTypeName(Class<?> type) {
        String simpleName = type.getSimpleName().toLowerCase();

        if (type.equals(Uint.class) || type.equals(Int.class)
                || type.equals(Ufixed.class) || type.equals(Fixed.class)) {
            return simpleName + "256";
        } else if (type.equals(Utf8String.class)) {
            return "string";
        } else if (type.equals(DynamicBytes.class)) {
            return "bytes";
        } else {
            return simpleName;
        }
    }

    static <T extends Type, U extends Type> String getParameterizedTypeName(
            TypeReference<T> typeReference, Class<?> type) {

        try {
            if (type.equals(DynamicArray.class)) {
                Class<U> parameterizedType = getParameterizedTypeFromArray(typeReference);
                String parameterizedTypeName = getSimpleTypeName(parameterizedType);
                return parameterizedTypeName + "[]";
            } else if (type.equals(StaticArray.class)) {
                Class<U> parameterizedType = getParameterizedTypeFromArray(typeReference);
                String parameterizedTypeName = getSimpleTypeName(parameterizedType);
                return parameterizedTypeName
                        + "["
                        + ((TypeReference.StaticArrayTypeReference) typeReference).getSize()
                        + "]";
            } else {
                throw new UnsupportedOperationException("Invalid type provided " + type.getName());
            }
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Invalid class reference provided", e);
        }
    }

    static <T extends Type> Class<T> getParameterizedTypeFromArray(
            TypeReference typeReference) throws ClassNotFoundException {

        java.lang.reflect.Type type = typeReference.getType();
        java.lang.reflect.Type[] typeArguments =
                ((ParameterizedType) type).getActualTypeArguments();

        String parameterizedTypeName = ((Class) typeArguments[0]).getName();
        return (Class<T>) Class.forName(parameterizedTypeName);
    }

    @SuppressWarnings("unchecked")
    public static List<TypeReference<Type>> convert(List<TypeReference<?>> input) {
        List<TypeReference<Type>> result = new ArrayList<TypeReference<Type>>(input.size());

        for (TypeReference<?> typeReference:input) {
            result.add((TypeReference<Type>) typeReference);
        }

        return result;
    }

    public static <T, R extends Type<T>> List<R> typeMap(List<T> input, Class<R> destType)
            throws TypeMappingException {

        List<R> result = new ArrayList<R>(input.size());

        if (!input.isEmpty()) {
            try {
                Constructor<R> constructor = destType.getDeclaredConstructor(
                        input.get(0).getClass());
                for (T value : input) {
                    result.add(constructor.newInstance(value));
                }
            } catch (NoSuchMethodException e) {
                throw new TypeMappingException(e);
            } catch (IllegalAccessException e) {
                throw new TypeMappingException(e);
            } catch (InvocationTargetException e) {
                throw new TypeMappingException(e);
            } catch (InstantiationException e) {
                throw new TypeMappingException(e);
            }
        }

        return result;
    }
}
