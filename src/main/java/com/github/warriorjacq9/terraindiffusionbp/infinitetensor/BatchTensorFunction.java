package com.github.warriorjacq9.terraindiffusionbp.infinitetensor;

import java.util.List;

/**
 * Batched variant of TensorFunction.
 */
@FunctionalInterface
public interface BatchTensorFunction {
    /**
     *
     * @param windowIndices the window indices for the batch
     * @param args          args.get(depIdx) is the list of dependency slices — one per window in the batch
     * @return list of output tensors, one per window in the batch
     */
    List<FloatTensor> apply(List<int[]> windowIndices, List<List<FloatTensor>> args);
}
