package org.nd4j.linalg.api.ops.impl.shape.tensorops;

import onnx.OnnxProto3;
import org.nd4j.autodiff.samediff.SameDiff;

import java.util.Map;

public class TensorArrayScatterV3 extends BaseTensorOp {

    @Override
    public String tensorflowName() {
        return "TensorArrayScatterV3";
    }



    @Override
    public String toString() {
        return opName();
    }

    @Override
    public String opName() {
        return "tensorarrayscatterv3";
    }


    @Override
    public void initFromOnnx(OnnxProto3.NodeProto node, SameDiff initWith, Map<String, OnnxProto3.AttributeProto> attributesForNode, OnnxProto3.GraphProto graph) {
    }
}
