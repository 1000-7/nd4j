---
layout: page
title: "ND4J Backends: What They Are, How They Work"
description: "Linear Algebra Backends"
---
{% include JB/setup %}

ND4J works atop so-called backends, or linear-algebra libraries, such as Jblas, Netlib Blas and Jcublas (GPUs), which you can select by pasting the [right dependency into your project's POM.xml file](http://nd4j.org/dependencies.html). 

A Java [ServiceLoader](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html), which is baked into the language itself, tells Java that the backend exists. It's not necessary to concern yourself with how ND4J backends load and perform other basic functions, but you can explore [how ND4J loads and selects backends, according to your OS, here](https://github.com/deeplearning4j/nd4j/blob/master/nd4j-api/src/main/java/org/nd4j/linalg/factory/Nd4jBackend.java).

The core configurations for each backend are specified in a properties file. Using Jblas as an example, this post will quickly explain each line in the [*nd4j-jblas.properties* file here](https://github.com/deeplearning4j/nd4j/blob/master/nd4j-jblas/src/main/resources/nd4j-jblas.properties).

<script src="http://gist-it.appspot.com/https://github.com/deeplearning4j/nd4j/blob/master/nd4j-jblas/src/main/resources/nd4j-jblas.properties?slice=18:35"></script>

Let's step through this line by line with some brief comments:

    //points to array class for real numbers
    real.class.double = org.nd4j.linalg.jblas.NDArray 
    
    //points to array class for complex numbers
    complex.class.double = org.nd4j.linalg.jblas.complex.ComplexNDArray 
    
    //defines the default data type as a float
    dtype = float  
    
    //again points to appropriate class
    complex.double.class = org.nd4j.linalg.jblas.complex.ComplexDouble 
    
    // Blas wrappers let you speak to the underlying linalg subprocesses.
    blas.ops = org.nd4j.linalg.jblas.BlasWrapper 
    
    //this factory creates NDArrays for Jblas
    ndarrayfactory.class = org.nd4j.linalg.jblas.JblasNDArrayFactory  
    
    //f stands for fortran, with column-major arrays. 
    //the alternative is c, which is row-major
    ndarray.order = f 
    resourcemanager_state = false 
    
    // the databufferfactory will differ if you use of cpus or gpus...
    databufferfactory = org.nd4j.linalg.api.buffer.factory.DefaultDataBufferFactory 
    
    //memory allocation can use arrays for storage with "heap".
    //it uses raw byte buffers/netty with "direct".
    alloc = heap 
    
    //fft specifies which fast-fourier transform impl to use
    fft = org.nd4j.linalg.fft.DefaultFFTInstance

You can see most of the classes the properties config file lines map to in this snapshot of nd4j-jblas's unfurled directory structure. 

![Alt text](../img/nd4j_backend_config.png)

A few more points:

* Regardless of the backend you choose, you use the same ND4J API for everything, including GPUs and distributed systems. 
* You can override all properties in the above file from the command line using *mvn -D$your_parameter_here*.
* You can include multiple backends on the classpath. If you do, ND4J will run on as many as GPUs asa you have available, exhaust them, and then start adding CPUs, allowing you to operate on mixed hardware. 
