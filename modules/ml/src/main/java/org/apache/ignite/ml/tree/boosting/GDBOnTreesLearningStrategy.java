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

package org.apache.ignite.ml.tree.boosting;

import java.util.Arrays;
import java.util.List;
import org.apache.ignite.ml.IgniteModel;
import org.apache.ignite.ml.composition.ModelsComposition;
import org.apache.ignite.ml.composition.boosting.GDBLearningStrategy;
import org.apache.ignite.ml.composition.boosting.GDBTrainer;
import org.apache.ignite.ml.composition.boosting.convergence.ConvergenceChecker;
import org.apache.ignite.ml.composition.predictionsaggregator.WeightedPredictionsAggregator;
import org.apache.ignite.ml.dataset.Dataset;
import org.apache.ignite.ml.dataset.DatasetBuilder;
import org.apache.ignite.ml.dataset.primitive.builder.context.EmptyContextBuilder;
import org.apache.ignite.ml.dataset.primitive.context.EmptyContext;
import org.apache.ignite.ml.environment.LearningEnvironment;
import org.apache.ignite.ml.environment.logging.MLLogger;
import org.apache.ignite.ml.math.primitives.vector.Vector;
import org.apache.ignite.ml.math.primitives.vector.VectorUtils;
import org.apache.ignite.ml.preprocessing.Preprocessor;
import org.apache.ignite.ml.trainers.DatasetTrainer;
import org.apache.ignite.ml.tree.DecisionTree;
import org.apache.ignite.ml.tree.data.DecisionTreeData;
import org.apache.ignite.ml.tree.data.DecisionTreeDataBuilder;

/**
 * Gradient boosting on trees specific learning strategy reusing learning dataset with index between several learning
 * iterations.
 */
public class GDBOnTreesLearningStrategy extends GDBLearningStrategy {
    /** Use index. */
    private boolean useIdx;

    /**
     * Create an instance of learning strategy.
     *
     * @param useIdx Use index.
     */
    public GDBOnTreesLearningStrategy(boolean useIdx) {
        this.useIdx = useIdx;
    }

    /** {@inheritDoc} */
    @Override public <K, V> List<IgniteModel<Vector, Double>> update(GDBTrainer.GDBModel mdlToUpdate,
                                                                     DatasetBuilder<K, V> datasetBuilder, Preprocessor<K, V> vectorizer) {

        LearningEnvironment environment = envBuilder.buildForTrainer();
        environment.initDeployingContext(vectorizer);

        DatasetTrainer<? extends IgniteModel<Vector, Double>, Double> trainer = baseMdlTrainerBuilder.get();
        assert trainer instanceof DecisionTree;
        DecisionTree decisionTreeTrainer = (DecisionTree)trainer;

        List<IgniteModel<Vector, Double>> models = initLearningState(mdlToUpdate);

        ConvergenceChecker<K, V> convCheck = checkConvergenceStgyFactory.create(sampleSize,
            externalLbToInternalMapping, loss, datasetBuilder, vectorizer);

        try (Dataset<EmptyContext, DecisionTreeData> dataset = datasetBuilder.build(
            envBuilder,
            new EmptyContextBuilder<>(),
            new DecisionTreeDataBuilder<>(vectorizer, useIdx),
            environment
        )) {
            for (int i = 0; i < cntOfIterations; i++) {
                double[] weights = Arrays.copyOf(compositionWeights, models.size());
                WeightedPredictionsAggregator aggregator = new WeightedPredictionsAggregator(weights, meanLbVal);
                ModelsComposition currComposition = new ModelsComposition(models, aggregator);

                if (convCheck.isConverged(dataset, currComposition))
                    break;

                dataset.compute(part -> {
                    if (part.getCopiedOriginalLabels() == null)
                        part.setCopiedOriginalLabels(Arrays.copyOf(part.getLabels(), part.getLabels().length));

                    for (int j = 0; j < part.getLabels().length; j++) {
                        double mdlAnswer = currComposition.predict(VectorUtils.of(part.getFeatures()[j]));
                        double originalLbVal = externalLbToInternalMapping.apply(part.getCopiedOriginalLabels()[j]);
                        part.getLabels()[j] = -loss.gradient(sampleSize, originalLbVal, mdlAnswer);
                    }
                });

                long startTs = System.currentTimeMillis();
                models.add(decisionTreeTrainer.fit(dataset));
                double learningTime = (double)(System.currentTimeMillis() - startTs) / 1000.0;
                trainerEnvironment.logger(getClass()).log(MLLogger.VerboseLevel.LOW, "One model training time was %.2fs", learningTime);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        compositionWeights = Arrays.copyOf(compositionWeights, models.size());
        return models;
    }
}
