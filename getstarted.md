---
layout: page
title: "Getting Started"
description: ""
---
{% include JB/setup %}

* ND4J requires [Java 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html).

* You can install ND4J either from source or from Maven central. Either way you do it, you need to run this git command and cd into an ND4J working directory. 

         git clone https://github.com/SkymindIO/nd4j.git
         cd nd4j

### IntelliJ

* To work with ND4J code, you should download the Java IDE IntelliJ. A [free, community edition](http://www.jetbrains.com/idea/download/) is available at:

         http://www.jetbrains.com/idea/download/

* Unzip the download, move it to your applications folder, and open the application. Upon opening, you may be prompted to install a Java SE 6 runtime. If so, install it. 

* As you open IntelliJ, you will have to choose whether to create or open a project. Choose "Open Project" from the menu, and then select the working directory for ND4J. Click the open button for that folder. (It will take a while for all the dependencies to be resolved, during which time you will not be able to run your examples.)

![Alt text](../img/open_project.png) 

* You'll need to make sure the Maven 2 Integration plugin is installed. On Macs, go to Preferences and then click on Plugins. (On Linux, you'll find the plugins in Settings.) Then choose "Browse Repositories" and search for "Maven 2 Integration." Install that plugin and restart IntelliJ. Restarting should take you back to your ND4J project. 

* Click through the folder names to the examples folder -- nd4j/tree/master/nd4j-examples/src/main/java/org/nd4j/examples -- and then right-click on the dataset you're interested in. (ElementwiseOperations is where most users start.) There, you will find a number of elementwise operations you can apply to matrices. Right click on ElementWiseOperationExample. In the menu that appears, look for the green arrow and choose "Run." 

* Any warning messages will appear at the top of the screen in a colored bar. If IntelliJ prompts you to add an SDK, choose JDK.

### The Maven Alternative to Source

* To check if you have Maven on your machine, type this in the terminal/cmd:

         mvn --version

* If you have Maven, you'll see the particular version on your computer, as well as the file path to where it lives. On a Windows PC, my file path was:

         c:\Programs\maven\bin\..

* If you don't have Maven, you can follow the installation instructions on Maven's ["getting started" page](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html). Finally, run this command:

         mvn clean install -DskipTests

* After you run "mvn clean", you will find ND4J jar files the local folder, which is where the compiling happens.
	
* Include the following in your pom.xml:


       <dependency>
        <artifactId>nd4j</artifactId>
        <groupId>org.nd4j</groupId>
        <artifactId>nd4j-api</artifactId>
        <version>0.0.1-SNAPSHOT</version>
       </dependency>

From here, you need to pick an implementation suitable for your needs. This can be either jblas for native or cuda for GPUs.


Jblas:

             <dependency>
                <artifactId>nd4j</artifactId>
                <groupId>org.nd4j</groupId>
                <artifactId>nd4j-jblas</artifactId>
                <version>0.0.1-SNAPSHOT</version>
               </dependency>

Jcuda:

                    <dependency>
                       <artifactId>nd4j</artifactId>
                       <groupId>org.nd4j</groupId>
                       <artifactId>nd4j-jcublas</artifactId>
                       <version>0.0.1-SNAPSHOT</version>
                      </dependency>

For Jcuda, we are still in the process of streamlining the release for this one. For now, please do the following:


                  git clone https://github.com/SkymindIO/mavenized-jcuda
                  cd mavenized-jcuda
                  mvn clean install


This will install the Jcuda jar files.

You need to specify a version of Jcuda to use as well. The version will depend on your GPU. Amazon supports 0.5.5.

**NEXT STEP**: Now you're ready to run the examples cited in our [documentation](../elementwise.html).

**The curious** will want to examine our [Github repo](https://github.com/SkymindIO/nd4j) or access the core through [Maven](http://maven.apache.org/download.cgi).
