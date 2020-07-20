package com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver;

import com.adobe.qe.toughday.internal.core.engine.Engine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Queue;

/**
 * Contains all the information needed for updating the state of a new driver when joining the cluster.
 */
public class DriverUpdateInfo {
    private int driverId;
    private DriverState.Role role;
    private Queue<Integer> invalidCandidates;
    private Queue<String> registeredAgents;
    private String yamlConfig;
    private String currentPhaseName;
    protected static final Logger LOG = LogManager.getLogger(Engine.class);


    // TODO: add registeredAgents currently running tasks
    // TODO: add registeredAgents which finished running the current phase


    // dummy constructor used to dump the class
    public DriverUpdateInfo() {}

    /**
     * Constructor.
     * @param driverId : the id of the driver which sent this updates.
     * @param role : the status of the driver which sent this updates (Master or Candidate).
     * @param invalidCandidates : list of drivers that are not eligible to become the new master if the current one dies
     * @param registeredAgents : list of active agents running in the cluster.
     * @param yamlConfig : TD configuration to be executed in distributed mode. This string is empty if the
     *                   configuration was not received yet.
     * @param currentPhaseName : the name of the current phase being executed by the agents running in the cluster. This
     *                         is empty if the execution did not started yet.
     */
    public DriverUpdateInfo(int driverId, DriverState.Role role, Queue<Integer> invalidCandidates,
                            Queue<String> registeredAgents, String yamlConfig, String currentPhaseName) {
        this.driverId = driverId;
        this.role = role;
        this.invalidCandidates = invalidCandidates;
        this.registeredAgents = registeredAgents;
        this.yamlConfig = yamlConfig;
        this.currentPhaseName = currentPhaseName;
    }

    /**
     * Getter for the id of the driver sending the updates.
     */
    public int getDriverId() {
        return this.driverId;
    }

    /**
     * Setter for the driver sending the updates.
     */
    public void setDriverId(int driverId) {
        this.driverId = driverId;
    }

    /**
     * Setter for the status of the driver sending the updates.
     */
    public void setRole(DriverState.Role driverState) {
        this.role = driverState;
    }

    /**
     * Getter for the status of the driver sending the updates.
     * @return
     */
    public DriverState.Role getRole() {
        return this.role;
    }

    /**
     * Setter for the list of candidates to be excluded when electing a new master.
     */
    public void setInvalidCandidates(Queue<Integer> invalidCandidates) {
        this.invalidCandidates = invalidCandidates;
    }

    /**
     * Getter for the list of candidates to be excluded when electing a new master.
     * @return
     */
    public Queue<Integer> getInvalidCandidates() {
        return this.invalidCandidates;
    }

    /**
     * Setter for the list of agents which are currently running in the cluster.
     */
    public void setRegisteredAgents(Queue<String> registeredAgents) {
        this.registeredAgents = registeredAgents;
    }

    /**
     * Getter for the list of agents which are currently running in the cluster.
     */
    public Queue<String> getRegisteredAgents() {
        return this.registeredAgents;
    }

    /**
     * Setter for the YAML representation of the TD configuration to be executed in distributed mode.
     */
    public void setYamlConfig(String yamlConfig) {
        this.yamlConfig = yamlConfig;
    }

    /**
     * Getter for the YAML representation of the TD configuration to be executed in distributed mode.
     */
    public String getYamlConfig() {
        return this.yamlConfig;
    }

    /**
     * Getter for the name of the phase being executed by the agents.
     */
    public String getCurrentPhaseName() {
        return this.currentPhaseName;
    }

    /**
     * Setter for the name of the phase being executed by the agents.
     */
    public void setCurrentPhaseName(String currentPhaseName) {
        this.currentPhaseName = currentPhaseName;
    }

}
