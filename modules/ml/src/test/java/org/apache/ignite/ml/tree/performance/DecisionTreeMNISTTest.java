/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.ml.tree.performance;

import org.apache.ignite.ml.tree.DecisionTreeClassificationTrainer;

/**
 * Tests {@link DecisionTreeClassificationTrainer} on the MNIST dataset using locally stored data. For manual run.
 */
public class DecisionTreeMNISTTest {
    /** Tests on the MNIST dataset. For manual run. */
/*    @Test
    public void testMNIST() throws IOException {
        Map<Integer, MnistUtils.MnistLabeledImage> trainingSet = new HashMap<>();

        int i = 0;
        for (MnistUtils.MnistLabeledImage e : MnistMLPTestUtil.loadTrainingSet(60_000))
            trainingSet.put(i++, e);

        DecisionTreeClassificationTrainer trainer = new DecisionTreeClassificationTrainer(
            8,
            0,
            new SimpleStepFunctionCompressor<>());

        DecisionTreeNode mdl = trainer.fit(
            trainingSet,
            10, FeatureLabelExtractorWrapper.wrap(
                (k, v) -> VectorUtils.of(v.getPixels()),
                (k, v) -> (double)v.getLabel()
            )
        );

        int correctAnswers = 0;
        int incorrectAnswers = 0;

        for (MnistUtils.MnistLabeledImage e : MnistMLPTestUtil.loadTestSet(10_000)) {
            double res = mdl.predict(new DenseVector(e.getPixels()));

            if (res == e.getLabel())
                correctAnswers++;
            else
                incorrectAnswers++;
        }

        double accuracy = 1.0 * correctAnswers / (correctAnswers + incorrectAnswers);

        assertTrue(accuracy > 0.8);
    }*/
}
