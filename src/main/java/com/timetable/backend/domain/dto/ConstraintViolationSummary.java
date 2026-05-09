package com.timetable.backend.domain.dto;

/**
 * Summary of a single constraint's contribution to the solver score.
 *
 * @param constraintName the name defined in {@code .asConstraint(...)}
 * @param hardScore      hard score impact (negative = hard violation)
 * @param softScore      soft score impact (negative = soft penalty, positive = reward)
 * @param matchCount     number of fact tuples that triggered this constraint
 */
public record ConstraintViolationSummary(
    String constraintName,
    int hardScore,
    int softScore,
    int matchCount
) {}

