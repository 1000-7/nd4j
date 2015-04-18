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



import org.apache.commons.io.IOUtils;
import org.jocl.cl_kernel;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Kernel function loader:
 *
 * @author Adam Gibson
 */
public class KernelFunctionLoader {

    public final static String NAME_SPACE = "org.nd4j.linalg.jocl";
    public final static String DOUBLE = NAME_SPACE + ".double.functions";
    public final static String FLOAT = NAME_SPACE + ".float.functions";
    public final static String IMPORTS_FLOAT = NAME_SPACE + ".float.imports";
    public final static String IMPORTS_DOUBLE = NAME_SPACE + ".double.imports";
    private Map<String,String> paths = new HashMap<>();
    private static Logger log = LoggerFactory.getLogger(KernelFunctionLoader.class);
    private static KernelFunctionLoader INSTANCE;
    private boolean init = false;

    private KernelFunctionLoader() {}

    /**
     * Singleton pattern
     *
     * @return
     */
    public static synchronized KernelFunctionLoader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new KernelFunctionLoader();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    INSTANCE.unload();
                }
            }));
            try {
                INSTANCE.load();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return INSTANCE;
    }

    private static String dataFolder(int type) {
        return "/kernels/" + (type == DataBuffer.FLOAT ? "float" : "double");
    }


    public  static KernelLauncher launcher(String functionName,String dataType) {
        return KernelFunctionLoader.getInstance().get(functionName,dataType);
    }


    public KernelLauncher get(String functionName,String dataType) {
        String path = paths.get(functionName + "_" + dataType);
        if(path == null) {
            throw new IllegalArgumentException("Unable to find " + functionName + "_" + dataType);
        }
        return KernelLauncher.load(path, functionName + "_" + dataType);
    }


    /**
     * Clean up all the modules
     */
    public void unload() {
        init = false;
    }



    /**
     * Load the appropriate functions from the class
     * path in to one module
     *
     * @return the module associated with this
     * @throws Exception
     */
    public void load() throws Exception {
        if (init)
            return;
        StringBuffer sb = new StringBuffer();
        sb.append("nvcc -g -G -ptx");

        ClassPathResource res = new ClassPathResource("/cudafunctions.properties");
        if (!res.exists())
            throw new IllegalStateException("Please put a cudafunctions.properties in your class path");
        Properties props = new Properties();
        props.load(res.getInputStream());
        log.info("Registering cuda functions...");
        //ensure imports for each file before compiling
        ensureImports(props, "float");
        ensureImports(props, "double");
        compileAndLoad(props, FLOAT, "float");
        compileAndLoad(props, DOUBLE, "double");

        init = true;
    }


    /**
     * The extension of the given file name is replaced with "ptx".
     * If the file with the resulting name does not exist, it is
     * compiled from the given file using NVCC. The name of the
     * PTX file is returned.
     * <p/>
     * <p/>
     * Note that you may run in to an error akin to:
     * Unsupported GCC version
     * <p/>
     * At your own risk, comment the lines under:
     * /usr/local/cuda-$VERSION/include/host_config.h
     * <p/>
     * #if defined(__GNUC__)
     * <p/>
     * if __GNUC__ > 4 || (__GNUC__ == 4 && __GNUC_MINOR__ > 8)
     * #error -- unsupported GNU version! gcc 4.9 and up are not supported!
     * <p/>
     * #endif /* __GNUC__> 4 || (__GNUC__ == 4 && __GNUC_MINOR__ > 8)
     * <p/>
     * #endif  __GNUC__
     * <p/>
     * This will allow you to bypass the compiler restrictions. Again, do so at your own risk.
     *
     * @return The name of the PTX file
     * @throws java.io.IOException If an I/O error occurs
     */
    private void compileAndLoad(Properties props, String key,String dataType) throws IOException {
        String f = props.getProperty(key);
        String[] split = f.split(",");
        for(String module : split) {
            Resource resource = new ClassPathResource(module);
            String source = IOUtils.toString(resource.getURI());
            paths.put(module + "_" + dataType,source);
        }

    }



    private void ensureImports(Properties props, String dataType) throws IOException {
        if (dataType.equals("float")) {
            String[] imports = props.getProperty(IMPORTS_FLOAT).split(",");
            for (String import1 : imports) {
                loadFile("/kernels/" + dataType + "/" + import1);
            }
        } else {
            String[] imports = props.getProperty(IMPORTS_DOUBLE).split(",");
            for (String import1 : imports) {
                loadFile("/kernels/" + dataType + "/" + import1);
            }
        }

    }


    private String loadFile(String file) throws IOException {
        ClassPathResource resource = new ClassPathResource(file);
        String tmpDir = System.getProperty("java.io.tmpdir");

        if (!resource.exists())
            throw new IllegalStateException("Unable to find file " + resource);
        File out = new File(tmpDir, file);
        if (!out.getParentFile().exists())
            out.getParentFile().mkdirs();
        if (out.exists())
            out.delete();
        out.createNewFile();
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out));
        IOUtils.copy(resource.getInputStream(), bos);
        bos.flush();
        bos.close();

        out.deleteOnExit();
        return out.getAbsolutePath();

    }


}
