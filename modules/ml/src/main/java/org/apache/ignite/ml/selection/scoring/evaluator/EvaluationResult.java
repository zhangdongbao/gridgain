/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.ml.selection.scoring.evaluator;

import java.io.PrintStream;
import java.util.Map;
import org.apache.ignite.internal.util.typedef.internal.A;
import org.apache.ignite.ml.selection.scoring.metric.MetricName;

/**
 * Class represents an aggregation of metrics evaluation results.
 */
public class EvaluationResult {
    /** Default precision. */
    private static final int DEFAULT_PRECISION = 3;

    /** Estimated values with metric names. */
    private final Map<MetricName, Double> values;

    /**
     * Creates an instance of EvaluationResult class.
     *
     * @param values Values.
     */
    public EvaluationResult(Map<MetricName, Double> values) {
        this.values = values;
    }

    /**
     * Returns metric value by its name.
     *
     * @param name Name.
     * @return Metric value.
     */
    public double get(MetricName name) {
        return values.getOrDefault(name, Double.NaN);
    }

    /**
     * Returns any computed metric value. This method is useful in case of computing just one metric.
     *
     * @return Metric value.
     */
    public double get() {
        return values.values().stream().findFirst().orElse(Double.NaN);
    }

    /**
     * Returns a collection of metric name and value pairs.
     *
     * @return Collection of metric name and value pairs.
     */
    public Iterable<Map.Entry<MetricName, Double>> getAll() {
        return values.entrySet();
    }

    /**
     * Print metric values to stdout.
     */
    public void print() {
        print(DEFAULT_PRECISION , System.out);
    }

    /**
     * Print metric values to user provided stream.
     *
     * @param out Out.
     */
    public void print(PrintStream out) {
        print(DEFAULT_PRECISION , out);
    }

    /**
     * Print metric values to stdout.
     *
     * @param precition Metric printing precision.
     */
    public void print(int precition) {
        print(precition, System.out);
    }

    /**
     * Print metric values to user provided stream.
     *
     * @param precition Metric printing precision.
     * @param out Out.
     */
    public void print(int precition, PrintStream out) {
        A.ensure(precition > 0, "precition > 0");
        out.println(toString(precition));
    }

    /**
     * Returns string representation of model estimation.
     *
     * @param precition Precition.
     * @return String representation of model estimation.
     */
    public String toString(int precition) {
        StringBuilder sb = new StringBuilder();
        values.forEach((k, v) -> {
            sb.append(String.format("%s = %." + precition + "f\n", k.getPrettyName(), v));
        });
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return toString(DEFAULT_PRECISION);
    }
}
