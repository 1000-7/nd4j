/*
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 *
 */

package org.nd4j.linalg.api.ops.executioner;

import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.*;
import org.nd4j.linalg.api.parallel.ParallelExecutioner;
import org.nd4j.linalg.api.parallel.bufferops.*;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic op executioner. Knows how to iterate over
 * the buffers of each respective ndarray and apply transformations
 *
 * @author Adam Gibson
 */
public class DefaultOpExecutioner implements OpExecutioner {
    public static final String PARALLEL_THRESHOLD = "org.nd4j.parallel.threshold";
    private static Logger log = LoggerFactory.getLogger(DefaultOpExecutioner.class);
    protected int parallelThreshold = 8192;

    protected ExecutionMode executionMode = ExecutionMode.JAVA;

    public DefaultOpExecutioner() {

        //Check if user has specified a parallel threshold via VM argument:
        String thresholdString = System.getProperty(PARALLEL_THRESHOLD,null);
        if(thresholdString != null) {
            int threshold = -1;
            try{
                threshold = Integer.parseInt(thresholdString);
            }catch(NumberFormatException e ){
                log.warn("Error parsing OpExecutioner parallel threshold: \"" + thresholdString + "\"");
                log.warn("OpExecutioner parallel threshold set to default: " + parallelThreshold);
            }
            if(threshold != -1) {
                if(threshold <= 0) {
                    log.warn("Invalid OpExecutioner parallel threshold; using default: " + parallelThreshold);
                } else {
                    setParallelThreshold(threshold);
                }
            }
        }
    }

    public int getParallelThreshold() {
        return parallelThreshold;
    }

    public void setParallelThreshold(int threshold) {
        if(threshold <= 0) throw new IllegalArgumentException("Threshold must be > 0 (is: " + threshold + ")");
        parallelThreshold = threshold;
    }

    @Override
    public ParallelExecutioner parallelExecutioner() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Op exec(Op op) {
        if (op.isPassThrough()) {
            op.exec();
            return op;
        }

        if (op instanceof TransformOp) {
            doTransformOp((TransformOp) op);
        } else if (op instanceof Accumulation) {
            doAccumulationOp((Accumulation) op);
        } else if (op instanceof ScalarOp) {
            doScalarOp((ScalarOp) op);
        } else if (op instanceof IndexAccumulation) {
            doIndexAccumulationOp((IndexAccumulation) op);
        }
        return op;
    }

    @Override
    public INDArray execAndReturn(Op op) {
        if (op instanceof TransformOp) {
            return execAndReturn((TransformOp) op);
        } else if (op instanceof ScalarOp) {
            return execAndReturn((ScalarOp) op);
        } else if (op instanceof Accumulation) {
            return Nd4j.scalar(execAndReturn((Accumulation) op).getFinalResult());
        } else if (op instanceof IndexAccumulation) {
            return Nd4j.scalar(execAndReturn((IndexAccumulation) op).getFinalResult());
        }

        throw new IllegalArgumentException("Illegal type of op: " + op.getClass());
    }

    @Override
    public void iterateOverAllRows(Op op) {
        //column and row vectors should be treated the same
        if (op.x().isVector()) {
            //reset the op in case
            op.setX(op.x());
            if (op.y() != null)
                op.setY(op.y());
            op.setZ(op.z());
            exec(op);
        }
        //execute row wise
        else if (op.x().isMatrix()) {
            if (op.x() instanceof IComplexNDArray) {
                IComplexNDArray original = (IComplexNDArray) op.x();
                IComplexNDArray originalZ = (IComplexNDArray) op.z();
                IComplexNDArray y = (IComplexNDArray) op.y();

                for (int i = 0; i < original.rows(); i++) {
                    IComplexNDArray row = original.slice(i);
                    IComplexNDArray zRow = originalZ.slice(i);
                    op.setX(row.dup());
                    op.setZ(zRow.dup());
                    if (y != null)
                        op.setY(y.slice(i));
                    exec(op);
                    originalZ.slice(i).assign(op.z());

                }
            } else {
                INDArray original = op.x();
                INDArray originalZ = op.z();
                INDArray y = op.y();

                for (int i = 0; i < original.rows(); i++) {
                    INDArray row = original.getRow(i);
                    INDArray zRow = originalZ.getRow(i);
                    op.setX(row.dup());
                    op.setZ(zRow.dup());
                    if (y != null)
                        op.setY(y.getRow(i).dup());
                    exec(op);
                    zRow.assign(op.z());
                }
            }
        } else {
            INDArray originalX = op.x();
            INDArray originalZ = op.z();
            for (int i = 0; i < originalX.slices(); i++) {
                INDArray slice = originalX.slice(i);
                INDArray zSlice = originalZ.slice(i);
                op.setX(slice);
                op.setZ(zSlice);
                iterateOverAllRows(op);
            }
        }
    }

    @Override
    public void iterateOverAllColumns(Op op) {
        if (op.x().isVector()) {
            exec(op);
        }
        //execute row wise
        else if (op.x().isMatrix() || op.x().isColumnVector()) {
            exec(op, 1);
        } else {
            if (op.x() instanceof IComplexNDArray) {
                IComplexNDArray originalX = (IComplexNDArray) op.x();
                IComplexNDArray originalZ = (IComplexNDArray) op.z();
                IComplexNDArray y = (IComplexNDArray) op.y();
                for (int i = 0; i < op.x().slices(); i++) {
                    op.setX(originalX.getColumn(i));
                    op.setZ(originalZ.getColumn(i));
                    if (y != null)
                        op.setY(y.getColumn(i));
                    iterateOverAllColumns(op);
                }
            } else {
                INDArray originalX = op.x();
                INDArray originalZ = op.z();
                INDArray y = op.y();
                for (int i = 0; i < op.x().slices(); i++) {
                    op.setX(originalX.getColumn(i));
                    op.setZ(originalZ.getColumn(i));
                    if (y != null)
                        op.setY(y.getColumn(i));
                    iterateOverAllColumns(op);
                }
            }
        }
    }


    @Override
    public INDArray execAndReturn(TransformOp op) {
        Op result = exec(op);
        TransformOp t = (TransformOp) result;
        return t.z();
    }


    @Override
    public Accumulation execAndReturn(Accumulation op) {
        return (Accumulation) exec(op);
    }

    @Override
    public INDArray execAndReturn(ScalarOp op) {
        return exec(op).z();
    }

    @Override
    public IndexAccumulation execAndReturn(IndexAccumulation op) {
        return (IndexAccumulation) exec(op);
    }

    @Override
    public Op exec(Op op, int... dimension) {
        //do op along all dimensions
        if (dimension.length == op.x().rank())
            dimension = new int[]{Integer.MAX_VALUE};

        if (op.isPassThrough()) {
            op.exec(dimension);
            return op;
        }

        if (op instanceof Accumulation || op instanceof IndexAccumulation) {
            //Overloaded exec(Accumulation,int...) and exec(IndexAccumulation,int...) should always be called instead of this
            throw new IllegalStateException("exec(Op,int...) should never be invoked for Accumulation/IndexAccumulation");
        } else if (op instanceof TransformOp) {
            execAndReturn((TransformOp) op);
            return op;
        } else if (op instanceof ScalarOp) {
            //Scalar op along dimension should be same as on the entire NDArray
            doScalarOp((ScalarOp) op);
            return op;
        } else {
            throw new UnsupportedOperationException("Unknown op type");
        }
    }

    @Override
    public INDArray exec(Accumulation op, int... dimension) {
        //do op along all dimensions
        if (dimension.length == op.x().rank())
            dimension = new int[]{Integer.MAX_VALUE};

        if (op.isPassThrough()) {
            op.exec(dimension);
            return op.z();
        }


        if (dimension[0] == Integer.MAX_VALUE) {
            if (op.x() instanceof IComplexNDArray)
                return Nd4j.scalar(execAndReturn(op).getFinalResultComplex());
            return Nd4j.scalar(execAndReturn(op).getFinalResult().doubleValue());
        }

        if (op instanceof IComplexNDArray) {
            int[] retShape = ArrayUtil.removeIndex(op.x().shape(), dimension);
            //ensure vector is proper shape
            if (retShape.length == 1) {
                if (dimension[0] == 0)
                    retShape = new int[]{1, retShape[0]};
                else
                    retShape = new int[]{retShape[0], 1};
            } else if (retShape.length == 0) {
                retShape = new int[]{1, 1};
            }

            IComplexNDArray ret = Nd4j.createComplex(retShape);
            for (int i = 0; i < op.x().tensorssAlongDimension(dimension); i++) {
                Op op2 = op.opForDimension(i, dimension);
                IComplexNumber result = execAndReturn((Accumulation) op2).getFinalResultComplex();
                ret.putScalar(i, result);
            }

            if (ret.ordering() == 'c')
                ret.setStride(ArrayUtil.reverseCopy(ret.stride()));

            return ret;
        } else {
            return new AccumulationAlongDimensionDataBufferTask(op, parallelThreshold, dimension).invoke();
        }
    }

    @Override
    public INDArray exec(IndexAccumulation op, int... dimension) {
        //do op along all dimensions
        if (dimension.length == op.x().rank())
            dimension = new int[]{Integer.MAX_VALUE};


        if (op.isPassThrough()) {
            op.exec(dimension);
            return op.z();
        }


        if (dimension[0] == Integer.MAX_VALUE) {
            return Nd4j.scalar(execAndReturn(op).getFinalResult());
        }

        if (op.x() instanceof IComplexNDArray) {
            int[] retShape = ArrayUtil.removeIndex(op.x().shape(), dimension);
            //ensure vector is proper shape
            if (retShape.length == 1) {
                if (dimension[0] == 0)
                    retShape = new int[]{1, retShape[0]};
                else
                    retShape = new int[]{retShape[0], 1};
            } else if (retShape.length == 0) {
                retShape = new int[]{1, 1};
            }

            IComplexNDArray ret = Nd4j.createComplex(retShape);
            for (int i = 0; i < op.x().tensorssAlongDimension(dimension); i++) {
                Op op2 = op.opForDimension(i, dimension);
                int result = execAndReturn((IndexAccumulation) op2).getFinalResult();
                ret.putScalar(i, result);
            }

            if (ret.ordering() == 'c')
                ret.setStride(ArrayUtil.reverseCopy(ret.stride()));

            return ret;
        } else {
            return new IndexAccumulationAlongDimensionDataBufferTask(op, parallelThreshold, dimension).invoke();
        }
    }

    @Override
    public INDArray execAndReturn(final TransformOp op, int... dimension) {
        if (dimension.length == op.x().rank()) {
            dimension = new int[]{Integer.MAX_VALUE};
        }

        new TransformAlongDimensionDataBufferTask(op, parallelThreshold, dimension).invoke();
        return op.z();
    }

    @Override
    public INDArray execAndReturn(ScalarOp op, int... dimension) {
        return exec(op, dimension).z();
    }

    @Override
    public ExecutionMode executionMode() {
        return executionMode;
    }

    @Override
    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    private void doTransformOp(TransformOp op) {
        INDArray x = op.x();
        INDArray y = op.y();
        INDArray z = op.z();

        if (y != null) {
            //Ops with 2 inputs - AddOp, MulOp, GreaterThan, etc
            if (!(x instanceof IComplexNDArray) && !(z instanceof IComplexNDArray)) {
                boolean canDoDirectly;
                if (x == z) {
                    canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x, y);
                } else {
                    canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x, y, z);
                }

                if (canDoDirectly) {
                    //Do parallelism via fork-join, directly on buffer
                    op.getTransformOpDataBufferAction(parallelThreshold, op.n(), x.data(), y.data(), z.data(),
                            x.offset(), y.offset(), z.offset(), x.elementWiseStride(), y.elementWiseStride(), z.elementWiseStride()).invoke();
                } else {
                    //Do parallelism after splitting into tensors first
                    new TransformViaTensorDataBufferAction(op, parallelThreshold, x, y, z).invoke();
                }
            } else {
                //Complex: x, y and/or z are complex
                if (z instanceof IComplexNDArray) {
                    IComplexNDArray cz = (IComplexNDArray) z;
                    if (x instanceof IComplexNDArray) {
                        IComplexNDArray cx = (IComplexNDArray) x;
                        if (y instanceof IComplexNDArray) {
                            IComplexNDArray cy = (IComplexNDArray) y;
                            //x,y,z all complex
                            for (int i = 0; i < op.n(); i++) {
                                cz.putScalar(i, op.op(cx.getComplex(i), cy.getComplex(i)));
                            }
                        } else {
                            //x,z complex, y real
                            for (int i = 0; i < op.n(); i++) {
                                cz.putScalar(i, op.op(cx.getComplex(i), y.getDouble(i)));
                            }
                        }
                    }
                } else {
                    //IComplexNDArray in, but real out
                    throw new UnsupportedOperationException("Invalid op: z is real but x.class=" + x.getClass().getName() + ", y.class=" + y.getClass().getName());
                }
            }
        } else {
            //Ops with 1 input - Tanh, Sin, etc. X=OP(X) or Z=OP(X)
            if (!(x instanceof IComplexNDArray) && !(z instanceof IComplexNDArray)) {
                boolean canDoDirectly;
                if (x == z) canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x);
                else canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x, z);

                if (canDoDirectly) {
                    op.getTransformOpDataBufferAction(parallelThreshold, x.length(), x.data(), null, z.data(), x.offset(),
                            0, z.offset(), x.elementWiseStride(), 0, z.elementWiseStride()).invoke();
                } else {
                    //Do parallelism after splitting into tensors first
                    new TransformViaTensorDataBufferAction(op, parallelThreshold, x, null, z).invoke();
                }
            } else {
                //Complex
                if (z instanceof IComplexNDArray) {
                    IComplexNDArray cz = (IComplexNDArray) z;
                    if (x instanceof IComplexNDArray) {
                        IComplexNDArray cx = (IComplexNDArray) x;
                        for (int i = 0; i < op.n(); i++) {
                            cz.putScalar(i, op.op(cx.getComplex(i)));
                        }
                    } else {
                        for (int i = 0; i < op.n(); i++) {
                            cz.putScalar(i, op.op(x.getDouble(i)));
                        }
                    }
                }
            }
        }
    }


    private void doAccumulationOp(Accumulation op) {
        INDArray x = op.x();
        INDArray y = op.y();
        if (!(x instanceof IComplexNDArray) && !(y instanceof IComplexNDArray)) {
            boolean canDoDirectly;
            if (y == null) canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x);
            else canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x, y);

            if (canDoDirectly) {
                if (y == null) {
                    op.getAccumulationOpDataBufferTask(parallelThreshold, x.length(), x.data(), null,
                            x.offset(), 0, x.elementWiseStride(), 0, true).invoke();
                } else {
                    op.getAccumulationOpDataBufferTask(parallelThreshold, x.length(), x.data(), y.data(),
                            x.offset(), y.offset(), x.elementWiseStride(), y.elementWiseStride(), true).invoke();
                }
            } else {
                //Need to break the accumulation into tensors first
                new AccumulationViaTensorDataBufferTask(op, parallelThreshold, x, y).invoke();
            }

        } else {
            //Complex
            if (y == null) {
                //Accumulation(x)
                //x must be complex
                IComplexNDArray cx = (IComplexNDArray) x;
                IComplexNumber accum = op.zeroComplex();
                for (int i = 0; i < op.n(); i++) {
                    accum = op.update(accum, cx.getComplex(i), i);
                }
                op.setFinalResultComplex(accum);
            } else {
                //Accumulation(x,y)
                if (!(x instanceof IComplexNDArray) || !(y instanceof IComplexNDArray)) {
                    throw new UnsupportedOperationException("Invalid input for accumulation op: x.class=" + x.getClass().getName() + ", y.class=" + y.getClass().getName());
                }
                IComplexNDArray cx = (IComplexNDArray) x;
                IComplexNDArray cy = (IComplexNDArray) y;
                IComplexNumber accum = op.zeroComplex();
                for (int i = 0; i < op.n(); i++) {
                    accum = op.update(accum, cx.getComplex(i), cy.getComplex(i));
                }
                op.setFinalResultComplex(accum);
            }
        }
    }

    private void doScalarOp(ScalarOp op) {
        INDArray x = op.x();
        INDArray z = op.z();

        if (!(x instanceof IComplexNDArray) && !(z instanceof IComplexNDArray)) {
            boolean canDoDirectly;
            if (x == z) canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x);     //X=OP(X)
            else canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x, z);       //Z=OP(X)

            if (canDoDirectly) {
                op.getScalarOpDataBufferAction(parallelThreshold, op.n(), x.data(), z.data(), x.offset(), z.offset(),
                        x.elementWiseStride(), z.elementWiseStride()).invoke();
            } else {
                //Break into tensors
                new ScalarViaTensorDataBufferAction(op, parallelThreshold, x, z).invoke();
            }
        } else {
            //Complex
            if (z instanceof IComplexNDArray) {
                IComplexNDArray cz = (IComplexNDArray) z;
                if (x instanceof IComplexNDArray) {
                    IComplexNDArray cx = (IComplexNDArray) x;
                    for (int i = 0; i < op.n(); i++) cz.putScalar(i, op.op(cx.getComplex(i)));
                } else {
                    for (int i = 0; i < op.n(); i++) cz.putScalar(i, op.op(x.getDouble(i)));
                }
            } else {
                //Put complex into real -> not supported
                throw new UnsupportedOperationException("Scalar op with complex x but real z: not supported");
            }
        }
    }

    private void doIndexAccumulationOp(IndexAccumulation op) {
        INDArray x = op.x();
        INDArray y = op.y();

        if (!(x instanceof IComplexNDArray) && !(y instanceof IComplexNDArray)) {
            boolean canDoDirectly;
            if (y == null) canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x);
            else canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x, y);

            if (canDoDirectly) {
                if (y == null) {
                    op.getIndexAccumulationOpDataBufferTask(parallelThreshold, x.length(), x.data(), null, x.offset(), 0,
                            x.elementWiseStride(), 0, 0, true).invoke();
                } else {
                    op.getIndexAccumulationOpDataBufferTask(parallelThreshold, x.length(), x.data(), y.data(), x.offset(), y.offset(),
                            x.elementWiseStride(), y.elementWiseStride(), 0, true).invoke();
                }
            } else {
                //Need to break the accumulation into tensors first
                new IndexAccumulationViaTensorDataBufferTask(op, parallelThreshold, x, y).invoke();
            }
        } else {
            //Complex
            if (y == null) {
                //IndexAccumulation(x)
                //x must be complex
                int accumIdx = -1;
                IComplexNDArray cx = (IComplexNDArray) x;
                IComplexNumber accum = op.zeroComplex();
                for (int i = 0; i < op.n(); i++) {
                    accumIdx = op.update(accum, accumIdx, cx.getComplex(i), i);
                    if (accumIdx == i) accum = op.op(cx.getComplex(i));
                }
                op.setFinalResult(accumIdx);
            } else {
                //IndexAccumulation(x,y)
                if (!(x instanceof IComplexNDArray) || !(y instanceof IComplexNDArray)) {
                    throw new UnsupportedOperationException("Invalid input for index accumulation op: x.class=" + x.getClass().getName() + ", y.class=" + y.getClass().getName());
                }
                int accumIdx = -1;
                IComplexNDArray cx = (IComplexNDArray) x;
                IComplexNDArray cy = (IComplexNDArray) y;
                IComplexNumber accum = op.zeroComplex();
                for (int i = 0; i < op.n(); i++) {
                    accumIdx = op.update(accum, accumIdx, cx.getComplex(i), cy.getComplex(i), i);
                    if (accumIdx == i) accum = op.op(cx.getComplex(i), cy.getComplex(i));
                }
                op.setFinalResult(accumIdx);
            }
        }
    }
}
