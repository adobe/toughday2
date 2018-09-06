/*
Copyright 2015 Adobe. All rights reserved.
This file is licensed to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
OF ANY KIND, either express or implied. See the License for the specific language
governing permissions and limitations under the License.
*/
package com.adobe.qe.toughday.api.core.benchmark;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * Helpers for implementing proxies
 */
public final class ProxyHelpers {
    private ProxyHelpers() {
        //no-op
    }

    /**
     * If you are using Mockito for writing a proxy, the arguments of the original call will be passed unrolled (the varargs
     * will be not be in an array, but directly in the root array)
     * E.g. for the method
     * <pre>
     *     <code>
     *         myMethod(String s, long p, int... q)
     *     </code>
     * </pre>
     * The following call myMethod("A string", 10L, 1, 2, 3) will result in ["A string", 10L, 1, 2, 3], instead of ["A string", 10L, new int[]{ 1, 2, 3 }]
     * This helper method rolls back the arguments so that they can be used to redirect the call via reflection. Sample implementation
     * for simple method call redirect proxy.
     * <pre>
     *     <code>
     *          private <T> T createDummyProxy(T object) {
     *              return Mockito.mock((Class<T>)object.getClass(), new Answer() {
     *                  @Override
     *                  public Object answer(InvocationOnMock invocation) throws Throwable {
     *                      return invocation.getMethod().invoke(object, canonicArguments(invocation.getMethod(), invocation.getArguments()));
     *                  }
     *              }
     *          }
     *     </code>
     * </pre>
     * @param method The method that needs to be called. The method on the target must be the exact same one as the one from proxy.
     * @param arguments The unrolled arguments
     * @return The rolled back arguments
     */
    public static Object[] canonicArguments(Method method, Object[] arguments) {
        if(!method.isVarArgs())
            return arguments;
        /*
         * Important distinction:
         * parameter = declared class + identifier in the method's signature
         * argument  = object given to a function at call time
         * parameters length != arguments length
         * arguments length = parameters length - 1 + var args length
         */
        int paramsLength = method.getParameterTypes().length;
        int nonVarParamsLength = paramsLength - 1;
        int varArgsLength = arguments.length - nonVarParamsLength;

        Object[] result = new Object[method.getParameterTypes().length];
        Class varArgsClass = method.getParameterTypes()[nonVarParamsLength];
        Object varArgs = Array.newInstance(varArgsClass.getComponentType(), varArgsLength);
        result[result.length -1] = varArgs;

        for(int i = 0; i < nonVarParamsLength; i++) {
            result[i] = arguments[i];
        }
        if(varArgsLength == 0) return result;

        /* ---- Sometimes I hate Java :( ----- */
        if(varArgsClass.getComponentType().isPrimitive()) {
            if (varArgs.getClass() == int[].class) {
                int[] varArgsType = (int[]) varArgs;
                for (int i = 0; i < varArgsLength; i++) {
                    varArgsType[i] = (int) arguments[nonVarParamsLength + i];
                }
            } else if (varArgs.getClass() == float[].class) {
                float[] varArgsType = (float[]) varArgs;
                for (int i = 0; i < varArgsLength; i++) {
                    varArgsType[i] = (float) arguments[nonVarParamsLength + i];
                }
            } else if (varArgs.getClass() == boolean[].class) {
                boolean[] varArgsType = (boolean[]) varArgs;
                for (int i = 0; i < varArgsLength; i++) {
                    varArgsType[i] = (boolean) arguments[nonVarParamsLength + i];
                }
            } else if (varArgs.getClass() == short[].class) {
                short[] varArgsType = (short[]) varArgs;
                for (int i = 0; i < varArgsLength; i++) {
                    varArgsType[i] = (short) arguments[nonVarParamsLength + i];
                }
            } else if (varArgs.getClass() == double[].class) {
                double[] varArgsType = (double[]) varArgs;
                for (int i = 0; i < varArgsLength; i++) {
                    varArgsType[i] = (double) arguments[nonVarParamsLength + i];
                }
            } else if (varArgs.getClass() == long[].class) {
                long[] varArgsType = (long[]) varArgs;
                for (int i = 0; i < varArgsLength; i++) {
                    varArgsType[i] = (long) arguments[nonVarParamsLength + i];
                }
            } else if (varArgs.getClass() == byte[].class) {
                byte[] varArgsType = (byte[]) varArgs;
                for (int i = 0; i < varArgsLength; i++) {
                    varArgsType[i] = (byte) arguments[nonVarParamsLength + i];
                }
            } else if (varArgs.getClass() == char[].class) {
                char[] varArgsType = (char[]) varArgs;
                for (int i = 0; i < varArgsLength; i++) {
                    varArgsType[i] = (char) arguments[nonVarParamsLength + i];
                }
            }
        } else {
            Object[] varArgsType = (Object[]) varArgs;
            for(int i = 0; i < varArgsLength; i++) {
                varArgsType[i] = arguments[nonVarParamsLength + i];
            }
        }
        /* ---- Sometimes I hate Java :( ----- */

        return result;
    }

}
