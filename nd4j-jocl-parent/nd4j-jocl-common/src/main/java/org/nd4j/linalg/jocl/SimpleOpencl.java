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

package org.nd4j.linalg.jocl;


import org.jocl.CL;
import org.jocl.Pointer;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.complex.IComplexDouble;
import org.nd4j.linalg.api.complex.IComplexFloat;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.arithmetic.CopyOp;
import org.nd4j.linalg.factory.DataTypeValidation;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.jocl.buffer.OpenclBuffer;
import org.nd4j.linalg.jocl.kernel.KernelFunctionLoader;

/**
 * Simple abstraction for jocl operations
 *
 * @author mjk
 * @author Adam Gibson
 */
public class SimpleOpencl {

    private static boolean init = false;
   

    static {
        init();
    }


    public static void assertCudaBuffer(INDArray... buffer) {
        for (INDArray b1 : buffer)
            if (!(b1.data() instanceof OpenclBuffer))
                throw new IllegalArgumentException("Unable to allocate pointer for buffer of type " + buffer.getClass().toString());
    }


    public static void assertCudaBuffer(DataBuffer... buffer) {
        for (DataBuffer b1 : buffer)
            if (!(b1 instanceof OpenclBuffer))
                throw new IllegalArgumentException("Unable to allocate pointer for buffer of type " + buffer.getClass().toString());
    }

    public static Pointer getPointer(INDArray array) {
        OpenclBuffer buffer = (OpenclBuffer) array.data();
        return buffer.pointer().withByteOffset(buffer.elementSize() * array.offset());
    }


    /**
     * Initialize jocl only called once
     */
    public static void init() {
        if (init)
            return;

        CL.setExceptionsEnabled(true);

        try {
            KernelFunctionLoader.getInstance().load();
        } catch (Exception e) {
            e.printStackTrace();
        }
        init = true;
    }




    /**
     * General matrix vector multiplication
     *
     * @param A
     * @param B
     * @param C
     * @param alpha
     * @param beta
     * @return
     */
    public static INDArray gemv(INDArray A, INDArray B, INDArray C, double alpha, double beta) {

        DataTypeValidation.assertDouble(A, B, C);
        assertCudaBuffer(A.data(), B.data(), C.data());
       

        Pointer cAPointer = getPointer(A);
        Pointer cBPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);


        jocl.cublasDgemv(
                'N',
                A.rows(),
                A.columns(),
                alpha,
                cAPointer,
                A.rows(),
                cBPointer,
                1,
                beta,
                cCPointer,
                1);

       
        return C;
    }

    /**
     * General matrix vector multiplication
     *
     * @param A
     * @param B
     * @param C
     * @param alpha
     * @param beta
     * @return
     */
    public static INDArray gemv(INDArray A, INDArray B, INDArray C, float alpha, float beta) {

        DataTypeValidation.assertFloat(A, B, C);
       

        Pointer cAPointer = getPointer(A);
        Pointer cBPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);



        jocl.cublasSgemv('N',
                A.rows(),
                A.columns(),
                alpha,
                cAPointer,
                A.rows(),
                cBPointer,
                1,
                beta,
                cCPointer,
                1);

       

        return C;
    }


    /**
     * General matrix vector
     *
     * @param A
     * @param B
     * @param a
     * @param C
     * @param b
     * @return
     */
    public static IComplexNDArray gemv(IComplexNDArray A, IComplexNDArray B, IComplexDouble a, IComplexNDArray C
            , IComplexDouble b) {
        DataTypeValidation.assertSameDataType(A, B, C);
       


        Pointer cAPointer = getPointer(A);
        Pointer cBPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);


        cuDoubleComplex alpha = cuDoubleComplex.cuCmplx(a.realComponent().doubleValue(), b.imaginaryComponent().doubleValue());
        cuDoubleComplex beta = cuDoubleComplex.cuCmplx(b.realComponent().doubleValue(), b.imaginaryComponent().doubleValue());

        jocl.cublasZgemv(
                'n', //trans
                A.rows(),  // m
                A.rows(), // n
                alpha,
                cAPointer, // A
                A.rows(),  // lda
                cBPointer, // x
                B.secondaryStride(), // ldb
                beta,  // beta
                cCPointer, // y
                C.secondaryStride()); // ldc

       

        return C;

    }

    /**
     * General matrix vector
     *
     * @param A
     * @param B
     * @param a
     * @param C
     * @param b
     * @return
     */
    public static IComplexNDArray gemv(IComplexNDArray A, IComplexNDArray B, IComplexFloat a, IComplexNDArray C
            , IComplexFloat b) {
        DataTypeValidation.assertFloat(A, B, C);
        assertCudaBuffer(A, B, C);
       

        Pointer cAPointer = getPointer(A);
        Pointer cBPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);


        cuComplex alpha = cuComplex.cuCmplx(a.realComponent().floatValue(), b.imaginaryComponent().floatValue());
        cuComplex beta = cuComplex.cuCmplx(b.realComponent().floatValue(), b.imaginaryComponent().floatValue());

        jocl.cublasCgemv(
                'n', //trans
                A.rows(),  // m
                A.columns(), // n
                alpha,
                cAPointer, // A
                A.rows(),  // lda
                cBPointer, // x
                B.secondaryStride(), // ldb
                beta,  // beta
                cCPointer, // y
                C.secondaryStride()); // ldc

       

        return C;

    }


    /**
     * General matrix multiply
     *
     * @param A
     * @param B
     * @param a
     * @param C
     * @param b
     * @return
     */
    public static IComplexNDArray gemm(IComplexNDArray A, IComplexNDArray B, IComplexDouble a, IComplexNDArray C
            , IComplexDouble b) {
        DataTypeValidation.assertSameDataType(A, B, C);
       

        Pointer cAPointer = getPointer(A);
        Pointer cBPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);



        cuDoubleComplex alpha = cuDoubleComplex.cuCmplx(a.realComponent().doubleValue(), b.imaginaryComponent().doubleValue());
        cuDoubleComplex beta = cuDoubleComplex.cuCmplx(b.realComponent().doubleValue(), b.imaginaryComponent().doubleValue());

        jocl.cublasZgemm(
                'n', //trans
                'n',
                C.rows(),  // m
                C.columns(), // n
                A.columns(), //k,
                alpha,
                cAPointer, // A
                A.rows(),  // lda
                cBPointer, // x
                B.rows(), // ldb
                beta,  // beta
                cCPointer, // y
                C.rows()); // ldc

       

        return C;

    }

    /**
     * General matrix multiply
     *
     * @param A
     * @param B
     * @param a
     * @param C
     * @param b
     * @return
     */
    public static IComplexNDArray gemm(IComplexNDArray A, IComplexNDArray B, IComplexFloat a, IComplexNDArray C
            , IComplexFloat b) {
        DataTypeValidation.assertFloat(A, B, C);

       

        Pointer cAPointer = getPointer(A);
        Pointer cBPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);


        cuComplex alpha = cuComplex.cuCmplx(a.realComponent().floatValue(), b.imaginaryComponent().floatValue());
        cuComplex beta = cuComplex.cuCmplx(b.realComponent().floatValue(), b.imaginaryComponent().floatValue());

        jocl.cublasCgemm(
                'n', //trans
                'n',
                C.rows(),  // m
                C.columns(), // n
                A.columns(), //k,
                alpha,
                cAPointer, // A
                A.rows(),  // lda
                cBPointer, // x
                B.rows(), // ldb
                beta,  // beta
                cCPointer, // y
                C.rows()); // ldc

       

        return C;

    }

    /**
     * General matrix multiply
     *
     * @param A
     * @param B
     * @param C
     * @param alpha
     * @param beta
     * @return
     */
    public static INDArray gemm(INDArray A, INDArray B, INDArray C,
                                double alpha, double beta) {

        DataTypeValidation.assertDouble(A, B, C);

       

        OpenClNDArray cA = (OpenClNDArray) A;
        OpenClNDArray cB = (OpenClNDArray) B;
        OpenClNDArray cC = (OpenClNDArray) C;

        Pointer cAPointer = getPointer(cA);
        Pointer cBPointer = getPointer(cB);
        Pointer cCPointer = getPointer(cC);


        jocl.cublasDgemm(
                'n', //trans
                'n',
                C.rows(),  // m
                C.columns(), // n
                A.columns(), //k,
                alpha,
                cAPointer, // A
                A.rows(),  // lda
                cBPointer, // x
                B.rows(), // ldb
                beta,  // beta
                cCPointer, // y
                C.rows()); // incy

       

        return C;

    }

    /**
     * General matrix multiply
     *
     * @param A
     * @param B
     * @param C
     * @param alpha
     * @param beta
     * @return
     */
    public static INDArray gemm(INDArray A, INDArray B, INDArray C,
                                float alpha, float beta) {
        DataTypeValidation.assertFloat(A, B, C);
       


        Pointer cAPointer = getPointer(A);
        Pointer cBPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);

        jocl.cublasSgemm(
                'n', //trans
                'n',
                C.rows(),  // m
                C.columns(), // n
                A.columns(), //k,
                alpha,
                cAPointer, // A
                A.rows(),  // lda
                cBPointer, // x
                B.rows(), // ldb
                beta,  // beta
                cCPointer, // y
                C.rows()); // incy
       

        return C;

    }


    /**
     * Calculate the 2 norm of the ndarray.
     * Note that this is a standin for
     * no complex ndarray. It will treat this as a normal ndarray
     * with a stride of 2.
     *
     * @param A the ndarray to calculate the norm2 of
     * @return the ndarray to calculate the norm2 of
     */
    public static double nrm2(IComplexNDArray A) {

       

        Pointer cAPointer = getPointer(A);
        if (A.data().dataType() == DataBuffer.FLOAT) {
            float s = jocl.cublasSnrm2(A.length(), cAPointer, 2);
            return s;
        } else {
            double s = jocl.cublasDnrm2(A.length(), cAPointer, 2);
            return s;
        }

    }

    /**
     * Copy x to y
     *
     * @param x the origin
     * @param y the destination
     */
    public static void copy(IComplexNDArray x, IComplexNDArray y) {
        DataTypeValidation.assertSameDataType(x, y);

       

        Pointer xCPointer = getPointer(x);
        Pointer yCPointer = getPointer(y);


        OpenclBuffer buff = (OpenclBuffer) x.data();
        if (x.majorStride() == 2 && y.majorStride() == 2)
            JCuda.cudaMemcpy(
                    yCPointer
                    , xCPointer
                    , x.length() * buff.elementSize() * 2
                    , cudaMemcpyKind.cudaMemcpyDeviceToDevice);
        else
            Nd4j.getExecutioner().exec(new CopyOp(x, y, y, x.length()));
       


    }


    /**
     * Return the index of the max in the given ndarray
     *
     * @param x the ndarray to ge the max for
     * @return the max index of the given ndarray
     */
    public static int iamax(IComplexNDArray x) {
       

        Pointer xCPointer = getPointer(x);
        if (x.data().dataType() == DataBuffer.FLOAT) {
            int max = jocl.cublasIsamax(x.length(), xCPointer, 1);
            return max;
        } else {
            int max = jocl.cublasIzamax(x.length(), xCPointer, 1);
            return max;
        }

    }

    /**
     * @param x
     * @return
     */
    public static float asum(IComplexNDArray x) {
        Pointer xCPointer = getPointer(x);
        float sum = jocl.cublasScasum(x.length(), xCPointer, 1);
        return sum;
    }


    /**
     * Swap the elements in each ndarray
     *
     * @param x
     * @param y
     */
    public static void swap(INDArray x, INDArray y) {


        DataTypeValidation.assertSameDataType(x, y);

        Pointer xCPointer = getPointer(x);
        Pointer yCPointer = getPointer(y);
       

        if (x.data().dataType() == DataBuffer.FLOAT) {
            jocl.cublasSswap(
                    x.length(),
                    xCPointer,
                    1,
                    yCPointer,
                    1);

        } else {
            jocl.cublasDswap(
                    x.length(),
                    xCPointer,
                    1,
                    yCPointer,
                    1);

        }
       


    }

    /**
     * @param x
     * @return
     */
    public static double asum(INDArray x) {


        Pointer xCPointer = getPointer(x);
        if (x.data().dataType() == DataBuffer.FLOAT) {
            float sum = jocl.cublasSasum(x.length(), xCPointer, 1);
            return sum;
        } else {
            double sum = jocl.cublasDasum(x.length(), xCPointer, 1);
            return sum;
        }

    }

    /**
     * Returns the norm2 of the given ndarray
     *
     * @param x
     * @return
     */
    public static double nrm2(INDArray x) {


        if (x.data().dataType() == DataBuffer.FLOAT) {
            Pointer xCPointer = getPointer(x);


            float normal2 = jocl.cublasSnrm2(x.length(), xCPointer, 1);
            return normal2;
        } else if (x.data().dataType() == DataBuffer.DOUBLE) {
            Pointer xCPointer = getPointer(x);
            double normal2 = jocl.cublasDnrm2(x.length(), xCPointer, 1);
            return normal2;
        }
        throw new IllegalStateException("Illegal data type on array ");


    }

    /**
     * Returns the index of the max element
     * in the given ndarray
     *
     * @param x
     * @return
     */
    public static int iamax(INDArray x) {



        Pointer xCPointer = getPointer(x);

        if (x.data().dataType() == DataBuffer.FLOAT) {
            int max = jocl.cublasIsamax(
                    x.length(),
                    xCPointer,
                    x.majorStride());

            return max - 1;
        } else if (x.data().dataType() == DataBuffer.DOUBLE) {
            int max = jocl.cublasIdamax(
                    x.length(),
                    xCPointer,
                    x.majorStride());

            return max - 1;
        }

        throw new IllegalStateException("Illegal data type on array ");
    }


    /**
     * And and scale by the given scalar da
     *
     * @param da alpha
     * @param A  the element to add
     * @param B  the matrix to add to
     */
    public static void axpy(float da, INDArray A, INDArray B) {


        DataTypeValidation.assertFloat(A, B);

        Pointer xAPointer = getPointer(A);
        Pointer xBPointer = getPointer(B);
       
        jocl.cublasSaxpy(
                A.length(),
                da,
                xAPointer,
                A.majorStride(),
                xBPointer,
                B.majorStride());
       


    }

    /**
     * @param da
     * @param A
     * @param B
     */
    public static void axpy(IComplexFloat da, IComplexNDArray A, IComplexNDArray B) {
        DataTypeValidation.assertFloat(A, B);



        Pointer aCPointer = getPointer(A);
        Pointer bCPointer = getPointer(B);
       

        jocl.cublasCaxpy(
                A.length(),
                jcuda.cuComplex.cuCmplx(da.realComponent().floatValue(), da.imaginaryComponent().floatValue()),
                aCPointer,
                1,
                bCPointer,
                1
        );
       


    }

    /**
     * @param da
     * @param A
     * @param B
     */
    public static void axpy(IComplexDouble da, IComplexNDArray A, IComplexNDArray B) {
        DataTypeValidation.assertDouble(A, B);



        Pointer aCPointer = getPointer(A);
        Pointer bCPointer = getPointer(B);
       

        jocl.cublasZaxpy(
                A.length(),
                jcuda.cuDoubleComplex.cuCmplx(da.realComponent().floatValue(), da.imaginaryComponent().floatValue()),
                aCPointer,
                A.majorStride(),
                bCPointer,
                B.majorStride()
        );
       


    }


    /**
     * Multiply the given ndarray
     * by alpha
     *
     * @param alpha
     * @param x
     * @return
     */
    public static INDArray scal(double alpha, INDArray x) {
        DataTypeValidation.assertDouble(x);

       

        Pointer xCPointer = getPointer(x);
        jocl.cublasDscal(
                x.length(),
                alpha,
                xCPointer,
                x.majorStride());
       

        return x;

    }

    /**
     * Multiply the given ndarray
     * by alpha
     *
     * @param alpha
     * @param x
     * @return
     */
    public static INDArray scal(float alpha, INDArray x) {


        DataTypeValidation.assertFloat(x);
       

        Pointer xCPointer = getPointer(x);
        jocl.cublasSscal(
                x.length(),
                alpha,
                xCPointer,
                x.majorStride());
       

        return x;

    }

    /**
     * Copy x to y
     *
     * @param x the src
     * @param y the destination
     */
    public static void copy(INDArray x, INDArray y) {
       
        DataTypeValidation.assertSameDataType(x, y);
        Pointer xCPointer = getPointer(x);
        Pointer yCPointer = getPointer(y);
        if(x.data().dataType() == DataBuffer.DOUBLE)
            jocl.cublasDcopy(x.length(),xCPointer,x.majorStride(),yCPointer,y.majorStride());
        if(x.data().dataType() == DataBuffer.FLOAT)
            jocl.cublasScopy(x.length(),xCPointer,x.majorStride(),yCPointer,y.majorStride());
       

    }

    /**
     * Dot product between 2 ndarrays
     *
     * @param x the first ndarray
     * @param y the second ndarray
     * @return the dot product between the two ndarrays
     */
    public static double dot(INDArray x, INDArray y) {
        DataTypeValidation.assertSameDataType(x, y);

       

        Pointer xCPointer = getPointer(x);
        Pointer yCPointer = getPointer(y);

        if (x.data().dataType() == (DataBuffer.FLOAT)) {
            float ret = jocl.cublasSdot(
                    x.length(),
                    xCPointer,
                    x.majorStride()
                    , yCPointer,
                    y.majorStride());
           

            return ret;
        } else {
            double ret = jocl.cublasDdot(
                    x.length(),
                    xCPointer,
                    y.majorStride()
                    , yCPointer,
                    y.majorStride());
           

            return ret;
        }

    }


    public static IComplexDouble dot(IComplexNDArray x, IComplexNDArray y) {
        DataTypeValidation.assertSameDataType(x, y);

       

        Pointer aCPointer = getPointer(x);
        Pointer bCPointer = getPointer(y);


        jcuda.cuDoubleComplex dott = jocl.cublasZdotc(
                x.length(),
                aCPointer,
                x.majorStride(),
                bCPointer,
                y.majorStride());

        IComplexDouble ret = Nd4j.createDouble(dott.x, dott.y);
       


        return ret;
    }


    public static INDArray ger(INDArray A, INDArray B, INDArray C, double alpha) {
        DataTypeValidation.assertDouble(A, B, C);
       

        // = alpha * A * transpose(B) + C
        Pointer aCPointer = getPointer(A);
        Pointer bCPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);


        jocl.cublasDger(
                A.rows(),   // m
                A.columns(),// n
                alpha,      // alpha
                aCPointer,        // d_A or x
                A.rows(),   // incx
                bCPointer,        // dB or y
                B.rows(),   // incy
                cCPointer,        // dC or A
                C.rows()    // lda
        );

       

        return C;
    }


    public static INDArray ger(INDArray A, INDArray B, INDArray C, float alpha) {
        DataTypeValidation.assertFloat(A, B, C);

       
        // = alpha * A * transpose(B) + C

        Pointer aCPointer = getPointer(A);
        Pointer bCPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);


        jocl.cublasSger(
                A.rows(),   // m
                A.columns(),// n
                alpha,      // alpha
                aCPointer,        // d_A or x
                A.rows(),   // incx
                bCPointer,        // dB or y
                B.rows(),   // incy
                cCPointer,        // dC or A
                C.rows()    // lda
        );
       


        return C;
    }


    /**
     * Complex multiplication of an ndarray
     *
     * @param alpha
     * @param x
     * @return
     */
    public static IComplexNDArray scal(IComplexFloat alpha, IComplexNDArray x) {
        DataTypeValidation.assertFloat(x);

       

        Pointer xCPointer = getPointer(x);

        jocl.cublasCscal(
                x.length(),
                jcuda.cuComplex.cuCmplx(alpha.realComponent(), alpha.imaginaryComponent()),
                xCPointer,
                x.majorStride()
        );
       


        return x;
    }

    /**
     * Complex multiplication of an ndarray
     *
     * @param alpha
     * @param x
     * @return
     */
    public static IComplexNDArray scal(IComplexDouble alpha, IComplexNDArray x) {
        DataTypeValidation.assertDouble(x);
       


        Pointer xCPointer = getPointer(x);

        jocl.cublasZscal(
                x.length(),
                jcuda.cuDoubleComplex.cuCmplx(alpha.realComponent(), alpha.imaginaryComponent()),
                xCPointer,
                x.majorStride()
        );
       


        return x;
    }

    /**
     * Complex dot product
     *
     * @param x
     * @param y
     * @return
     */
    public static IComplexDouble dotu(IComplexNDArray x, IComplexNDArray y) {

        DataTypeValidation.assertSameDataType(x, y);
       

        Pointer xCPointer = getPointer(x);
        Pointer yCPointer = getPointer(y);
        IComplexDouble ret = null;
        if (x.data().dataType() == DataBuffer.DOUBLE) {
            jcuda.cuDoubleComplex dott = jocl.cublasZdotu(x.length(), xCPointer, x.majorStride(), yCPointer, y.majorStride());
            ret = Nd4j.createDouble(dott.x, dott.y);
        } else {
            jcuda.cuComplex dott = jocl.cublasCdotu(x.length(), xCPointer, x.majorStride(), yCPointer, y.majorStride());
            ret = Nd4j.createDouble(dott.x, dott.y);
        }
       

        return ret;
    }


    /**
     * @param A
     * @param B
     * @param C
     * @param Alpha
     * @return
     */
    public static IComplexNDArray geru(IComplexNDArray A,
                                       IComplexNDArray B,
                                       IComplexNDArray C, IComplexDouble Alpha) {
        // = alpha * A * tranpose(B) + C
       
        DataTypeValidation.assertDouble(A, B, C);



        Pointer aCPointer = getPointer(A);
        Pointer bCPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);

        cuDoubleComplex alpha = cuDoubleComplex.cuCmplx(Alpha.realComponent(), Alpha.imaginaryComponent());

        jocl.cublasZgeru(
                A.rows(),   // m
                A.columns(),// n
                alpha,      // alpha
                aCPointer,        // d_A or x
                A.rows(),   // incx
                bCPointer,        // d_B or y
                B.rows(),   // incy
                cCPointer,        // d_C or A
                C.rows()    // lda
        );
       


        return C;
    }

    /**
     * @param A
     * @param B
     * @param C
     * @param Alpha
     * @return
     */
    public static IComplexNDArray gerc(IComplexNDArray A, IComplexNDArray B, IComplexNDArray C,
                                       IComplexFloat Alpha) {
        DataTypeValidation.assertFloat(A, B, C);
        // = alpha * A * tranpose(B) + C

       
        Pointer aCPointer = getPointer(A);
        Pointer bCPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);


        cuComplex alpha = cuComplex.cuCmplx(Alpha.realComponent(), Alpha.imaginaryComponent());


        jocl.cublasCgerc(
                A.rows(),   // m
                A.columns(),// n
                alpha,      // alpha
                aCPointer,        // dA or x
                A.rows(),   // incx
                bCPointer,        // dB or y
                B.rows(),   // incy
                cCPointer,        // dC or A
                C.rows()    // lda
        );
       

        return C;
    }

    /**
     * @param A
     * @param B
     * @param C
     * @param Alpha
     * @return
     */
    public static IComplexNDArray geru(IComplexNDArray A,
                                       IComplexNDArray B,
                                       IComplexNDArray C, IComplexFloat Alpha) {

        DataTypeValidation.assertFloat(A, B, C);
        // = alpha * A * tranpose(B) + C
       

        Pointer aCPointer = getPointer(A);
        Pointer bCPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);

        cuDoubleComplex alpha = cuDoubleComplex.cuCmplx(Alpha.realComponent(), Alpha.imaginaryComponent());

        jocl.cublasZgeru(
                A.rows(),   // m
                A.columns(),// n
                alpha,      // alpha
                aCPointer,        // d_A or x
                A.rows(),   // incx
                bCPointer,        // d_B or y
                B.rows(),   // incy
                cCPointer,        // d_C or A
                C.rows()    // lda
        );

       

        return C;
    }

    /**
     * @param A
     * @param B
     * @param C
     * @param Alpha
     * @return
     */
    public static IComplexNDArray gerc(IComplexNDArray A, IComplexNDArray B, IComplexNDArray C,
                                       IComplexDouble Alpha) {

        DataTypeValidation.assertDouble(A, B, C);
        // = alpha * A * tranpose(B) + C

       

        Pointer aCPointer = getPointer(A);
        Pointer bCPointer = getPointer(B);
        Pointer cCPointer = getPointer(C);


        cuDoubleComplex alpha = cuDoubleComplex.cuCmplx(Alpha.realComponent(), Alpha.imaginaryComponent());


        jocl.cublasZgerc(
                A.rows(),   // m
                A.columns(),// n
                alpha,      // alpha
                aCPointer,        // dA or x
                A.rows(),   // incx
                bCPointer,        // dB or y
                B.rows(),   // incy
                cCPointer,        // dC or A
                C.rows()    // lda
        );

       

        return C;
    }

    /**
     * Simpler version of saxpy
     * taking in to account the parameters of the ndarray
     *
     * @param alpha the alpha to scale by
     * @param x     the x
     * @param y     the y
     */
    public static void axpy(double alpha, INDArray x, INDArray y) {
        DataTypeValidation.assertDouble(x, y);

       

        Pointer xCPointer = getPointer(x);
        Pointer yCPointer = getPointer(y);

        jocl.cublasDaxpy(x.length(), alpha, xCPointer, x.majorStride(), yCPointer, y.majorStride());

       

    }

    /**
     * Simpler version of saxpy
     * taking in to account the parameters of the ndarray
     *
     * @param alpha the alpha to scale by
     * @param x     the x
     * @param y     the y
     */
    public static void saxpy(float alpha, INDArray x, INDArray y) {
        DataTypeValidation.assertFloat(x, y);
       

        Pointer xCPointer = getPointer(x);
        Pointer yCPointer = getPointer(y);

        jocl.cublasSaxpy(x.length(), alpha, xCPointer, x.majorStride(), yCPointer, y.majorStride());
       


    }
}
