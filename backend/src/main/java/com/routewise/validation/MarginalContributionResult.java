package com.routewise.validation;

/**
 * Result of testing whether a candidate new cost indicator adds value to the
 * existing model, is redundant with it, or is negligible.
 *
 * @param totalScenarios          number of scenarios evaluated
 * @param pctRoutesDiffer         % of scenarios where adding the candidate changed the chosen route
 * @param gapReaisMean            mean cost (R$) "left on the table" by ignoring the candidate
 * @param gapPercentMean          same, as % of the base route's cost
 * @param wilcoxonPValue          paired significance test on the resulting cost gap
 * @param correlationWithBaseCost Spearman correlation between the candidate's own cost total
 *                                and the existing model's cost total — high means redundant
 * @param verdict                 human-readable conclusion: negligible / redundant / adds value
 */
public record MarginalContributionResult(
  int totalScenarios,
  double pctRoutesDiffer,
  double gapReaisMean,
  double gapPercentMean,
  double wilcoxonPValue,
  double correlationWithBaseCost,
  String verdict
) {}
