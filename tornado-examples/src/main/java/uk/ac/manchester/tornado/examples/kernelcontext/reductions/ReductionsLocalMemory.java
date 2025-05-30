/*
 * Copyright (c) 2021-2022 APT Group, Department of Computer Science,
 * The University of Manchester.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.manchester.tornado.examples.kernelcontext.reductions;

import java.util.stream.IntStream;

import uk.ac.manchester.tornado.api.GridScheduler;
import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.KernelContext;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.WorkerGrid;
import uk.ac.manchester.tornado.api.WorkerGrid1D;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.types.arrays.FloatArray;

/**
 * <p>
 * How to run?
 * </p>
 * <code>
 * $ tornado --threadInfo -m tornado.examples/uk.ac.manchester.tornado.examples.kernelcontext.reductions.ReductionsLocalMemory
 * </code>
 */
public class ReductionsLocalMemory {

    // Reduction in Local memory using KernelContext
    public static void reductionLocal(FloatArray a, FloatArray b, int localSize, KernelContext context) {
        int globalIdx = context.globalIdx;
        int localIdx = context.localIdx;
        int localGroupSize = context.localGroupSizeX;
        int groupID = context.groupIdx; // Expose Group ID

        float[] localA = context.allocateFloatLocalArray(256);
        localA[localIdx] = a.get(globalIdx);
        for (int stride = (localGroupSize / 2); stride > 0; stride /= 2) {
            context.localBarrier();
            if (localIdx < stride) {
                localA[localIdx] += localA[localIdx + stride];
            }
        }
        if (localIdx == 0) {
            b.set(groupID, localA[0]);
        }
    }

    public static float computeSequential(FloatArray input) {
        float acc = 0;
        for (int i = 0; i < input.getSize(); i++) {
            acc += input.get(i);
        }
        return acc;
    }

    public static void rAdd(final FloatArray array, int size) {
        float acc = array.get(0);
        for (int i = 1; i < array.getSize(); i++) {
            acc += array.get(i);
        }
        array.set(0, acc);
    }

    public static void main(String[] args) {
        final int size = 1024;
        final int localSize = 256;
        FloatArray input = new FloatArray(size);
        FloatArray reduce = new FloatArray(size / localSize);
        IntStream.range(0, input.getSize()).sequential().forEach(i -> input.set(i, i));
        float sequential = computeSequential(input);

        WorkerGrid worker = new WorkerGrid1D(size);
        GridScheduler gridScheduler = new GridScheduler();
        gridScheduler.addWorkerGrid("s0.t0", worker);
        KernelContext context = new KernelContext();

        TaskGraph taskGraph = new TaskGraph("s0") //
                .transferToDevice(DataTransferMode.EVERY_EXECUTION, input, localSize)//
                .task("t0", ReductionsLocalMemory::reductionLocal, input, reduce, localSize, context) //
                .task("t1", ReductionsLocalMemory::rAdd, reduce, (size / localSize)) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, reduce);

        // Change the Grid
        worker.setLocalWork(localSize, 1, 1);

        ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();
        TornadoExecutionPlan executor = new TornadoExecutionPlan(immutableTaskGraph);
        executor.withGridScheduler(gridScheduler).execute();

        // Final SUM
        float finalSum = reduce.get(0);

        System.out.println("Final SUM = " + finalSum + " vs seq= " + sequential);
        if ((sequential - finalSum) == 0) {
            System.out.println("Result is correct");
        } else {
            System.out.println("Result is wrong");
        }
    }
}
