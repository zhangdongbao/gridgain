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

package org.apache.ignite.internal.processors.cluster;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.util.tostring.GridToStringExclude;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;

/**
 * A pojo-object representing current cluster global state. The state includes cluster active flag and cluster
 * baseline topology.
 * <p>
 * This object also captures a transitional cluster state, when one or more fields are changing. In this case,
 * a {@code transitionReqId} field is set to a non-null value and {@code previousBaselineTopology} captures previous cluster state.
 * A joining node catching the cluster in an intermediate state will observe {@code transitionReqId} field to be
 * non-null, however the {@code previousBaselineTopology} will not be sent to the joining node.
 *
 * TODO https://issues.apache.org/jira/browse/IGNITE-7640 This class must be immutable, transitionRes must be set by calling finish().
 */
public class DiscoveryDataClusterState implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Flag indicating if the cluster in in active state. */
    private final boolean active;

    /** Flag indicating if the cluster in read-only mode. */
    private final boolean readOnly;

    /** Last cluster activation time. */
    private final long activationTime;

    /** Read-only mode change time. Correctly work's only for enabling read-only mode. */
    private final long readOnlyChangeTime;

    /** Current cluster baseline topology. */
    @Nullable private final BaselineTopology baselineTopology;

    /**
     * Transition request ID. Set to a non-null value if the cluster is changing it's state.
     * The ID is assigned on the initiating node.
     */
    private final UUID transitionReqId;

    /**
     * Topology version in the cluster when state change request was received by the coordinator.
     * The exchange fired for the cluster state change will be on version {@code transitionTopVer.nextMinorVersion()}.
     */
    @GridToStringInclude
    private final AffinityTopologyVersion transitionTopVer;

    /** Nodes participating in state change exchange. */
    @GridToStringExclude
    private final Set<UUID> transitionNodes;

    /**
     * Local flag for state transition active state result (global state is updated asynchronously by custom message),
     * {@code null} means that state change is not completed yet.
     */
    private transient volatile Boolean transitionRes;

    /**
     * Previous cluster state if this state is a transition state and it was not received by a joining node.
     */
    private transient DiscoveryDataClusterState prevState;

    /** Transition result error. */
    private transient volatile Exception transitionError;

    /** Local baseline autoadjustment flag. */
    private transient volatile boolean locBaselineAutoAdjustment;

    /**
     * @param active Current status.
     * @return State instance.
     */
    static DiscoveryDataClusterState createState(
        boolean active,
        boolean readOnly,
        @Nullable BaselineTopology baselineTopology,
        long activationTime
    ) {
        return new DiscoveryDataClusterState(null, active, readOnly, baselineTopology, null, null, activationTime, null);
    }

    /**
     * @param active New status.
     * @param readOnly New read-only mode.
     * @param transitionReqId State change request ID.
     * @param transitionTopVer State change topology version.
     * @param activationTime Cluster activation time.
     * @param transitionNodes Nodes participating in state change exchange.
     * @return State instance.
     */
    static DiscoveryDataClusterState createTransitionState(
        DiscoveryDataClusterState prevState,
        boolean active,
        boolean readOnly,
        @Nullable BaselineTopology baselineTopology,
        UUID transitionReqId,
        AffinityTopologyVersion transitionTopVer,
        long activationTime,
        Set<UUID> transitionNodes
    ) {
        assert transitionReqId != null;
        assert transitionTopVer != null;
        assert !F.isEmpty(transitionNodes) : transitionNodes;
        assert prevState != null;

        return new DiscoveryDataClusterState(
            prevState,
            active,
            readOnly,
            baselineTopology,
            transitionReqId,
            transitionTopVer,
            activationTime,
            transitionNodes
        );
    }

    /**
     * @param prevState Previous state. May be non-null only for transitional states.
     * @param active New state.
     * @param readOnly New read-only mode.
     * @param transitionReqId State change request ID.
     * @param transitionTopVer State change topology version.
     * @param activationTime Cluster activation time.
     * @param transitionNodes Nodes participating in state change exchange.
     */
    private DiscoveryDataClusterState(
        DiscoveryDataClusterState prevState,
        boolean active,
        boolean readOnly,
        @Nullable BaselineTopology baselineTopology,
        @Nullable UUID transitionReqId,
        @Nullable AffinityTopologyVersion transitionTopVer,
        long activationTime,
        @Nullable Set<UUID> transitionNodes
    ) {
        this.prevState = prevState;
        this.active = active;
        this.readOnly = readOnly;
        this.activationTime = activationTime;
        this.readOnlyChangeTime = U.currentTimeMillis();
        this.baselineTopology = baselineTopology;
        this.transitionReqId = transitionReqId;
        this.transitionTopVer = transitionTopVer;
        this.transitionNodes = transitionNodes;
    }

    /**
     * @return Local flag for state transition result (global state is updated asynchronously by custom message).
     */
    @Nullable public Boolean transitionResult() {
        return transitionRes;
    }

    /**
     * Discovery cluster state is changed asynchronously by discovery message, this methods changes local status
     * for public API calls.
     *
     * @param reqId Request ID.
     * @param active New cluster state.
     */
    public void setTransitionResult(UUID reqId, boolean active) {
        if (reqId.equals(transitionReqId))
            transitionRes = active;
    }

    /**
     * @return State change request ID.
     */
    public UUID transitionRequestId() {
        return transitionReqId;
    }

    /**
     * @return {@code True} if any cluster state change is in progress (e.g. active state change, baseline change).
     */
    public boolean transition() {
        return transitionReqId != null;
    }

    /**
     * @return {@code True} if transition is baseline change.
     */
    public boolean baselineChanging() {
        return transition() && prevState != null && prevState.active() && active();
    }

    /**
     * @return State change exchange version.
     */
    public AffinityTopologyVersion transitionTopologyVersion() {
        return transitionTopVer;
    }

    /**
     * @return Current cluster state (or new state in case when transition is in progress).
     */
    public boolean active() {
        return active;
    }

    /**
     * @return Cluster activation time.
     */
    public long activationTime() {
        return activationTime;
    }

    /**
     * @return Read only mode enabled flag.
     */
    public boolean readOnly() {
        return readOnly;
    }

    /**
     * @return Change time read-only mode.
     */
    public long readOnlyModeChangeTime() {
        return readOnlyChangeTime;
    }

    /**
     * @return Baseline topology.
     */
    @Nullable public BaselineTopology baselineTopology() {
        return baselineTopology;
    }

    /**
     * @return Previous Baseline topology.
     */
    @Nullable public BaselineTopology previousBaselineTopology() {
        return prevState != null ? prevState.baselineTopology() : null;
    }

    /**
     * @return Previous "active" flag value during transition.
     */
    public boolean previouslyActive() {
        assert transitionReqId != null;

        if (prevState != null)
            return prevState.active;

        return !active;
    }

    /**
     * @return {@code True} if baseline topology is set in the cluster. {@code False} otherwise.
     */
    public boolean hasBaselineTopology() {
        return baselineTopology != null;
    }

    /**
     * @return Nodes participating in state change exchange.
     */
    public Set<UUID> transitionNodes() {
        return transitionNodes;
    }

    /**
     * @return Transition error.
     */
    @Nullable public Exception transitionError() {
        return transitionError;
    }

    /**
     * @param ex Exception
     */
    public void transitionError(Exception ex) {
        transitionError = ex;
    }

    /**
     * @return {@code true} if current state was created as a result of local baseline autoadjustment with zero timeout
     *      on in-memory cluster.
     */
    public boolean localBaselineAutoAdjustment() {
        return locBaselineAutoAdjustment;
    }

    /**
     * Set local baseline autoadjustment flag.
     *
     * @param adjusted Flag value.
     * @see #localBaselineAutoAdjustment()
     */
    public void localBaselineAutoAdjustment(boolean adjusted) {
        locBaselineAutoAdjustment = adjusted;
    }

    /**
     * Creates a non-transitional cluster state. This method effectively cleans all fields identifying the
     * state as transitional and creates a new state with the state transition result.
     *
     * @param success Transition success status.
     * @return Cluster state that finished transition.
     */
    public DiscoveryDataClusterState finish(boolean success) {
        return success ?
            new DiscoveryDataClusterState(
                null,
                active,
                readOnly,
                baselineTopology,
                null,
                null,
                activationTime,
                null
            ) :
            prevState != null ? prevState : createState(false, false, null, 0);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(DiscoveryDataClusterState.class, this);
    }
}
