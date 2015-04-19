package org.nd4j.linalg.jcublas.context;

import com.google.common.collect.*;
import jcuda.CudaException;
import jcuda.driver.CUcontext;
import jcuda.driver.CUdevice;
import jcuda.driver.CUresult;
import jcuda.driver.JCudaDriver;
import org.nd4j.linalg.jcublas.SimpleJCublas;


import java.util.concurrent.atomic.AtomicInteger;

import static jcuda.driver.JCudaDriver.*;

/**
 * A multithreaded version derived from the cuda launcher util
 * by the authors of jcuda.
 *
 * This class handles managing cuda contexts
 * across multiple devices and threads.
 *
 *
 * @author Adam Gibson
 */
public class ContextHolder {
    private Table<Integer,String,CUdevice> devices = HashBasedTable.create();
    private Table<Integer,String,CUcontext> deviceToThreadAndContext = HashBasedTable.create();
    private int numDevices = 0;
    private static ContextHolder INSTANCE;
    private AtomicInteger deviceTouse = new AtomicInteger(0);
    private ContextHolder(){
        getNumDevices();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
               for(Table.Cell<Integer,String,CUcontext> cell : deviceToThreadAndContext.cellSet()) {
                   JCudaDriver.cuCtxDestroy(cell.getValue());
               }
            }
        }));
    }

    /**
     * Returns the device to use for launching
     * kernels
     * @return the device to use for launching kernels
     */
    public int device() {
        return deviceTouse.get();
    }

    /**
     * Sets the device for launching kernels
     * @param device the device to use
     *               to launch kernels
     */
    public void setDevice(int device) {
        deviceTouse.set(device);
    }


    public static ContextHolder getInstance() {
        if(INSTANCE == null)
            INSTANCE = new ContextHolder();
        return INSTANCE;
    }


    private void getNumDevices() {
        int count[] = new int[1];
        cuDeviceGetCount(count);
        numDevices = count[0];
        if(numDevices < 1)
           numDevices = 1;
    }

    /**
     * Retrieve a context for use with the current thread
     * and the given device
     * @return the t
     */
    public  synchronized CUcontext getContext() {
        return getContext(0);
    }

    /**
     * Retrieve a context for use with the current thread
     * and the given device
     * @param deviceToUse the device to use
     * @return the t
     */
    public  synchronized CUcontext getContext(int deviceToUse) {
        Thread currentThread = Thread.currentThread();
        CUcontext ctx = deviceToThreadAndContext.get(deviceToUse, currentThread.getName());
        if(ctx == null) {
            ctx = new CUcontext();
            for(int device = 0; device < numDevices; device++) {
                initialize(ctx,device);
                CUdevice currDevice = createDevice(ctx, device);
                devices.put(device,currentThread.getName(),currDevice);
                deviceToThreadAndContext.put(device,currentThread.getName(),ctx);


            }

        }

        return ctx;
    }


    /**
     * Initializes this KernelLauncher. This method will try to
     * initialize the JCuda driver API. Then it will try to
     * attach to the current CUDA context. If no active CUDA
     * context exists, then it will try to create one, for
     * the device which is specified by the current
     * deviceNumber.
     *
     * @throws CudaException If it is neither possible to
     * attach to an existing context, nor to create a new
     * context.
     */
    private void initialize(CUcontext context,int deviceNumber) {
        int result = cuInit(deviceNumber);
        if (result != CUresult.CUDA_SUCCESS)
        {
            throw new CudaException(
                    "Failed to initialize the driver: "+
                            CUresult.stringFor(result));
        }

        // Try to obtain the current context
        result = cuCtxGetCurrent(context);
        if (result != CUresult.CUDA_SUCCESS) {
            throw new CudaException(
                    "Failed to obtain the current context: "+
                            CUresult.stringFor(result));
        }

        // If the context is 'null', then a new context
        // has to be created.
        CUcontext nullContext = new CUcontext();
        if (context.equals(nullContext))
        {
            createContext(context,deviceNumber);
        }
    }

    /**
     * Tries to create a context for device 'deviceNumber'.
     *
     * @throws CudaException If the device can not be
     * accessed or the context can not be created
     */
    private void createContext(CUcontext context,int deviceNumber) {
        CUdevice device = new CUdevice();
        int result = cuDeviceGet(device, deviceNumber);
        if (result != CUresult.CUDA_SUCCESS) {
            throw new CudaException(
                    "Failed to obtain a device: "+
                            CUresult.stringFor(result));
        }

        result = cuCtxCreate(context, 0, device);
        if (result != CUresult.CUDA_SUCCESS) {
            throw new CudaException(
                    "Failed to create a context: "+
                            CUresult.stringFor(result));
        }

    }


    public static CUdevice createDevice(CUcontext context,int deviceNumber) {
        CUdevice device = new CUdevice();
        int result = cuDeviceGet(device, deviceNumber);
        if (result != CUresult.CUDA_SUCCESS) {
            throw new CudaException(
                    "Failed to obtain a device: "+
                            CUresult.stringFor(result));
        }

        result = cuCtxCreate(context, 0, device);
        if (result != CUresult.CUDA_SUCCESS) {
            throw new CudaException(
                    "Failed to create a context: "+
                            CUresult.stringFor(result));
        }

        return device;
    }

    /**
     * Returns the available devices
     * delimited by device,thread
     * @return the available devices
     */
    public Table<Integer, String, CUdevice> getDevices() {
        return devices;
    }

    /**
     * Returns the available contexts
     * based on device and thread name
     * @return the context
     */
    public Table<Integer, String, CUcontext> getDeviceToThreadAndContext() {
        return deviceToThreadAndContext;
    }


}
