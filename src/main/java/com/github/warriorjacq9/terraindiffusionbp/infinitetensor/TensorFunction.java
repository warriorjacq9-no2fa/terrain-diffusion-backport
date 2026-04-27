package com.github.warriorjacq9.terraindiffusionbp.infinitetensor;

import java.util.List;

/**
 * Function that computes a window of an InfiniteTensor.
 *
 */
@FunctionalInterface
public interface TensorFunction {
    /**
     *
     * @param windowIndex the N-dimensional index of the window being computed
     * @param args        slices from each upstream dependency tensor, in the order declared
     * @return the computed FloatTensor with shape matching the output TensorWindow size
     */
    FloatTensor apply(int[] windowIndex, List<FloatTensor> args);
}