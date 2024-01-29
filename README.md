# Z-ordering issue
In the process of using `Composition.java` to mix multiple `MediaItem` instances from different sequences that overlap in time, the expected behavior is for the z-order of these items to be determined by the order of `EditedMediaItemSequence` inserted into the Composition's builder, as per the default implementation of `DefaultVideoCompositor`. However, in practice, the z-order appears to be inconsistent and changes randomly with each execution.

## Steps to reproduce
1. Checkout this repository
2. Run project
3. Wait for initial Transformation to complete
4. Re-run transformation using the Run Transformer button
5. Repeat it a few times
6. Expected: The z-order of the items should be consistent with the order of the sequences in the builder
7. Actual: The z-order of the items is inconsistent and changes randomly with each execution

## Recording
[![Bug](./img.png)](./bug.mp4)

