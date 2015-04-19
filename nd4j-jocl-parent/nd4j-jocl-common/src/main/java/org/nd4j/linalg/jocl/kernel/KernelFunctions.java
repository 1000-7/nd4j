/*
 * Copyright 2015 Skymind,Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.nd4j.linalg.jocl.kernel;



import org.jocl.Sizeof;
import org.nd4j.linalg.jocl.buffer.OpenClDoubleDataBuffer;
import org.nd4j.linalg.jocl.buffer.OpenClFloatDataBuffer;
import org.nd4j.linalg.jocl.buffer.OpenclBuffer;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;



/**
 * Kernel functions.
 * <p/>
 * Derived from:
 * http://www.jcuda.org/samples/JCudaVectorAdd.java
 *
 * @author Adam Gibson
 */
public class KernelFunctions {

    public final static String NAME_SPACE = "org.nd4j.linalg.jocl";
    public final static String DOUBLE = NAME_SPACE + ".double.functions";
    public final static String FLOAT = NAME_SPACE + ".float.functions";
    public final static String REDUCE = NAME_SPACE + ".reducefunctions";
    public final static String SHARED_MEM_KEY = NAME_SPACE + ".sharedmem";
    public final static String THREADS_KEY = NAME_SPACE + ".threads";
    public final static String BLOCKS_KEY = NAME_SPACE + ".blocks";
    public static int SHARED_MEM = 512;
    public static int THREADS = 128;
    public static int BLOCKS = 512;
    private static Set<String> reduceFunctions = new ConcurrentSkipListSet<>();


    private KernelFunctions() {}


    static {
        try {
            register();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called at initialization in the static context.
     * Registers cuda functions based on
     * the cudafunctions.properties in the classpath
     *
     * @throws java.io.IOException
     */
    public static void register() throws Exception {
        ClassPathResource res = new ClassPathResource("/cudafunctions.properties");
        if (!res.exists())
            throw new IllegalStateException("Please put a cudafunctions.properties in your class path");
        Properties props = new Properties();
        props.load(res.getInputStream());
        KernelFunctionLoader.getInstance().load();

        String reduceFunctionsList = props.getProperty(REDUCE);
        for (String function : reduceFunctionsList.split(","))
            reduceFunctions.add(function);

        SHARED_MEM = Integer.parseInt(props.getProperty(SHARED_MEM_KEY, "512"));
        THREADS = Integer.parseInt(props.getProperty(THREADS_KEY, "128"));
        BLOCKS = Integer.parseInt(props.getProperty(BLOCKS_KEY, "64"));

    }



    /**
     * Invoke a function with the given number of parameters
    * @param kernelParameters the parameters
     * @param dataType         the data type ot use
     */
    public static   void invoke(String functionName,String dataType,Object...kernelParameters) {
        // Call the kernel function.
        KernelFunctionLoader.launcher(functionName,dataType).forFunction(functionName + "_" + dataType)
                .call(kernelParameters);

    }


    /**
     * Allocate a pointer of a given data type
     *
     * @param data the data for the pointer
     * @return the pointer
     */
    public static OpenclBuffer alloc(double[] data) {
        // Allocate the device input data, and copy the
        // host input data to the device
        OpenclBuffer doubleBuffer = new OpenClDoubleDataBuffer(data);
        return doubleBuffer;
    }

    /**
     * Allocate a pointer of a given data type
     *
     * @param data the data for the pointer
     * @return the pointer
     */
    public static OpenclBuffer alloc(float[] data) {
        // Allocate the device input data, and copy the
        // host input data to the device
        OpenclBuffer floatBuffer = new OpenClFloatDataBuffer(data);
        return floatBuffer;
    }

}
