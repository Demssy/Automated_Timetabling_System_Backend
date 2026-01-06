package com.timetable.backend.service;

import ai.timefold.solver.core.api.solver.SolverStatus;
import com.timetable.backend.solver.domain.TimetableSolution;

/**
 * Service interface for Timefold Solver operations.
 * Handles asynchronous schedule optimization and result persistence.
 */
public interface ISolverService {

    /**
     * Starts solving the schedule optimization problem asynchronously.
     *
     * @param scheduleId unique identifier for this solving session
     */
    void solve(Long scheduleId);

    /**
     * Loads the planning problem from the database.
     *
     * @param scheduleId the schedule identifier
     * @return TimetableSolution ready for optimization
     */
    TimetableSolution loadProblem(Long scheduleId);

    /**
     * Saves the optimized solution back to the database.
     *
     * @param solution the solved TimetableSolution
     */
    void saveSolution(TimetableSolution solution);

    /**
     * Gets the current status of the solver for a given schedule.
     *
     * @param scheduleId the schedule identifier
     * @return SolverStatus (NOT_SOLVING, SOLVING_SCHEDULED, SOLVING_ACTIVE)
     */
    SolverStatus getSolverStatus(Long scheduleId);

    /**
     * Terminates solving early for a given schedule.
     *
     * @param scheduleId the schedule identifier
     * @return true if termination was successful
     */
    boolean terminateEarly(Long scheduleId);

    /**
     * Retrieves the current solution from the database.
     *
     * @param scheduleId the schedule identifier
     * @return TimetableSolution with current state from database
     */
    TimetableSolution getCurrentSolutionFromDatabase(Long scheduleId);
}

