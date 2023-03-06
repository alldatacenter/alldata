# Generated from /Users/m1n0/dev/soda/soda-sql/soda-core/soda/core/soda/sodacl/antlr/SodaCLAntlr.g4 by ANTLR 4.11.1
from antlr4 import *
if __name__ is not None and "." in __name__:
    from .SodaCLAntlrParser import SodaCLAntlrParser
else:
    from SodaCLAntlrParser import SodaCLAntlrParser

# This class defines a complete generic visitor for a parse tree produced by SodaCLAntlrParser.

class SodaCLAntlrVisitor(ParseTreeVisitor):

    # Visit a parse tree produced by SodaCLAntlrParser#check.
    def visitCheck(self, ctx:SodaCLAntlrParser.CheckContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#freshness_check.
    def visitFreshness_check(self, ctx:SodaCLAntlrParser.Freshness_checkContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#freshness_variable.
    def visitFreshness_variable(self, ctx:SodaCLAntlrParser.Freshness_variableContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#warn_qualifier.
    def visitWarn_qualifier(self, ctx:SodaCLAntlrParser.Warn_qualifierContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#failed_rows_check.
    def visitFailed_rows_check(self, ctx:SodaCLAntlrParser.Failed_rows_checkContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#row_count_comparison_check.
    def visitRow_count_comparison_check(self, ctx:SodaCLAntlrParser.Row_count_comparison_checkContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#metric_check.
    def visitMetric_check(self, ctx:SodaCLAntlrParser.Metric_checkContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#default_anomaly_threshold.
    def visitDefault_anomaly_threshold(self, ctx:SodaCLAntlrParser.Default_anomaly_thresholdContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#change_over_time.
    def visitChange_over_time(self, ctx:SodaCLAntlrParser.Change_over_timeContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#change_over_time_config.
    def visitChange_over_time_config(self, ctx:SodaCLAntlrParser.Change_over_time_configContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#change_aggregation.
    def visitChange_aggregation(self, ctx:SodaCLAntlrParser.Change_aggregationContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#same_day_last_week.
    def visitSame_day_last_week(self, ctx:SodaCLAntlrParser.Same_day_last_weekContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#percent.
    def visitPercent(self, ctx:SodaCLAntlrParser.PercentContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#anomaly_score.
    def visitAnomaly_score(self, ctx:SodaCLAntlrParser.Anomaly_scoreContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#metric.
    def visitMetric(self, ctx:SodaCLAntlrParser.MetricContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#metric_name.
    def visitMetric_name(self, ctx:SodaCLAntlrParser.Metric_nameContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#metric_args.
    def visitMetric_args(self, ctx:SodaCLAntlrParser.Metric_argsContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#metric_arg.
    def visitMetric_arg(self, ctx:SodaCLAntlrParser.Metric_argContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#threshold.
    def visitThreshold(self, ctx:SodaCLAntlrParser.ThresholdContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#between_threshold.
    def visitBetween_threshold(self, ctx:SodaCLAntlrParser.Between_thresholdContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#comparator_threshold.
    def visitComparator_threshold(self, ctx:SodaCLAntlrParser.Comparator_thresholdContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#zones_threshold.
    def visitZones_threshold(self, ctx:SodaCLAntlrParser.Zones_thresholdContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#outcome.
    def visitOutcome(self, ctx:SodaCLAntlrParser.OutcomeContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#zone_comparator.
    def visitZone_comparator(self, ctx:SodaCLAntlrParser.Zone_comparatorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#comparator.
    def visitComparator(self, ctx:SodaCLAntlrParser.ComparatorContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#threshold_value.
    def visitThreshold_value(self, ctx:SodaCLAntlrParser.Threshold_valueContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#freshness_threshold_value.
    def visitFreshness_threshold_value(self, ctx:SodaCLAntlrParser.Freshness_threshold_valueContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#reference_check.
    def visitReference_check(self, ctx:SodaCLAntlrParser.Reference_checkContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#source_column_name.
    def visitSource_column_name(self, ctx:SodaCLAntlrParser.Source_column_nameContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#target_column_name.
    def visitTarget_column_name(self, ctx:SodaCLAntlrParser.Target_column_nameContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#section_header.
    def visitSection_header(self, ctx:SodaCLAntlrParser.Section_headerContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#table_checks_header.
    def visitTable_checks_header(self, ctx:SodaCLAntlrParser.Table_checks_headerContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#partition_name.
    def visitPartition_name(self, ctx:SodaCLAntlrParser.Partition_nameContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#table_filter_header.
    def visitTable_filter_header(self, ctx:SodaCLAntlrParser.Table_filter_headerContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#column_configurations_header.
    def visitColumn_configurations_header(self, ctx:SodaCLAntlrParser.Column_configurations_headerContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#checks_for_each_dataset_header.
    def visitChecks_for_each_dataset_header(self, ctx:SodaCLAntlrParser.Checks_for_each_dataset_headerContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#checks_for_each_column_header.
    def visitChecks_for_each_column_header(self, ctx:SodaCLAntlrParser.Checks_for_each_column_headerContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#signed_number.
    def visitSigned_number(self, ctx:SodaCLAntlrParser.Signed_numberContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#number.
    def visitNumber(self, ctx:SodaCLAntlrParser.NumberContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#integer.
    def visitInteger(self, ctx:SodaCLAntlrParser.IntegerContext):
        return self.visitChildren(ctx)


    # Visit a parse tree produced by SodaCLAntlrParser#identifier.
    def visitIdentifier(self, ctx:SodaCLAntlrParser.IdentifierContext):
        return self.visitChildren(ctx)



del SodaCLAntlrParser