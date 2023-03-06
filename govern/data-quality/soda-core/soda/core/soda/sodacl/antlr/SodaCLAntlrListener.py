# Generated from /Users/m1n0/dev/soda/soda-sql/soda-core/soda/core/soda/sodacl/antlr/SodaCLAntlr.g4 by ANTLR 4.11.1
from antlr4 import *
if __name__ is not None and "." in __name__:
    from .SodaCLAntlrParser import SodaCLAntlrParser
else:
    from SodaCLAntlrParser import SodaCLAntlrParser

# This class defines a complete listener for a parse tree produced by SodaCLAntlrParser.
class SodaCLAntlrListener(ParseTreeListener):

    # Enter a parse tree produced by SodaCLAntlrParser#check.
    def enterCheck(self, ctx:SodaCLAntlrParser.CheckContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#check.
    def exitCheck(self, ctx:SodaCLAntlrParser.CheckContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#freshness_check.
    def enterFreshness_check(self, ctx:SodaCLAntlrParser.Freshness_checkContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#freshness_check.
    def exitFreshness_check(self, ctx:SodaCLAntlrParser.Freshness_checkContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#freshness_variable.
    def enterFreshness_variable(self, ctx:SodaCLAntlrParser.Freshness_variableContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#freshness_variable.
    def exitFreshness_variable(self, ctx:SodaCLAntlrParser.Freshness_variableContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#warn_qualifier.
    def enterWarn_qualifier(self, ctx:SodaCLAntlrParser.Warn_qualifierContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#warn_qualifier.
    def exitWarn_qualifier(self, ctx:SodaCLAntlrParser.Warn_qualifierContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#failed_rows_check.
    def enterFailed_rows_check(self, ctx:SodaCLAntlrParser.Failed_rows_checkContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#failed_rows_check.
    def exitFailed_rows_check(self, ctx:SodaCLAntlrParser.Failed_rows_checkContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#row_count_comparison_check.
    def enterRow_count_comparison_check(self, ctx:SodaCLAntlrParser.Row_count_comparison_checkContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#row_count_comparison_check.
    def exitRow_count_comparison_check(self, ctx:SodaCLAntlrParser.Row_count_comparison_checkContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#metric_check.
    def enterMetric_check(self, ctx:SodaCLAntlrParser.Metric_checkContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#metric_check.
    def exitMetric_check(self, ctx:SodaCLAntlrParser.Metric_checkContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#default_anomaly_threshold.
    def enterDefault_anomaly_threshold(self, ctx:SodaCLAntlrParser.Default_anomaly_thresholdContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#default_anomaly_threshold.
    def exitDefault_anomaly_threshold(self, ctx:SodaCLAntlrParser.Default_anomaly_thresholdContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#change_over_time.
    def enterChange_over_time(self, ctx:SodaCLAntlrParser.Change_over_timeContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#change_over_time.
    def exitChange_over_time(self, ctx:SodaCLAntlrParser.Change_over_timeContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#change_over_time_config.
    def enterChange_over_time_config(self, ctx:SodaCLAntlrParser.Change_over_time_configContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#change_over_time_config.
    def exitChange_over_time_config(self, ctx:SodaCLAntlrParser.Change_over_time_configContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#change_aggregation.
    def enterChange_aggregation(self, ctx:SodaCLAntlrParser.Change_aggregationContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#change_aggregation.
    def exitChange_aggregation(self, ctx:SodaCLAntlrParser.Change_aggregationContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#same_day_last_week.
    def enterSame_day_last_week(self, ctx:SodaCLAntlrParser.Same_day_last_weekContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#same_day_last_week.
    def exitSame_day_last_week(self, ctx:SodaCLAntlrParser.Same_day_last_weekContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#percent.
    def enterPercent(self, ctx:SodaCLAntlrParser.PercentContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#percent.
    def exitPercent(self, ctx:SodaCLAntlrParser.PercentContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#anomaly_score.
    def enterAnomaly_score(self, ctx:SodaCLAntlrParser.Anomaly_scoreContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#anomaly_score.
    def exitAnomaly_score(self, ctx:SodaCLAntlrParser.Anomaly_scoreContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#metric.
    def enterMetric(self, ctx:SodaCLAntlrParser.MetricContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#metric.
    def exitMetric(self, ctx:SodaCLAntlrParser.MetricContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#metric_name.
    def enterMetric_name(self, ctx:SodaCLAntlrParser.Metric_nameContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#metric_name.
    def exitMetric_name(self, ctx:SodaCLAntlrParser.Metric_nameContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#metric_args.
    def enterMetric_args(self, ctx:SodaCLAntlrParser.Metric_argsContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#metric_args.
    def exitMetric_args(self, ctx:SodaCLAntlrParser.Metric_argsContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#metric_arg.
    def enterMetric_arg(self, ctx:SodaCLAntlrParser.Metric_argContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#metric_arg.
    def exitMetric_arg(self, ctx:SodaCLAntlrParser.Metric_argContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#threshold.
    def enterThreshold(self, ctx:SodaCLAntlrParser.ThresholdContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#threshold.
    def exitThreshold(self, ctx:SodaCLAntlrParser.ThresholdContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#between_threshold.
    def enterBetween_threshold(self, ctx:SodaCLAntlrParser.Between_thresholdContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#between_threshold.
    def exitBetween_threshold(self, ctx:SodaCLAntlrParser.Between_thresholdContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#comparator_threshold.
    def enterComparator_threshold(self, ctx:SodaCLAntlrParser.Comparator_thresholdContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#comparator_threshold.
    def exitComparator_threshold(self, ctx:SodaCLAntlrParser.Comparator_thresholdContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#zones_threshold.
    def enterZones_threshold(self, ctx:SodaCLAntlrParser.Zones_thresholdContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#zones_threshold.
    def exitZones_threshold(self, ctx:SodaCLAntlrParser.Zones_thresholdContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#outcome.
    def enterOutcome(self, ctx:SodaCLAntlrParser.OutcomeContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#outcome.
    def exitOutcome(self, ctx:SodaCLAntlrParser.OutcomeContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#zone_comparator.
    def enterZone_comparator(self, ctx:SodaCLAntlrParser.Zone_comparatorContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#zone_comparator.
    def exitZone_comparator(self, ctx:SodaCLAntlrParser.Zone_comparatorContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#comparator.
    def enterComparator(self, ctx:SodaCLAntlrParser.ComparatorContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#comparator.
    def exitComparator(self, ctx:SodaCLAntlrParser.ComparatorContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#threshold_value.
    def enterThreshold_value(self, ctx:SodaCLAntlrParser.Threshold_valueContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#threshold_value.
    def exitThreshold_value(self, ctx:SodaCLAntlrParser.Threshold_valueContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#freshness_threshold_value.
    def enterFreshness_threshold_value(self, ctx:SodaCLAntlrParser.Freshness_threshold_valueContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#freshness_threshold_value.
    def exitFreshness_threshold_value(self, ctx:SodaCLAntlrParser.Freshness_threshold_valueContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#reference_check.
    def enterReference_check(self, ctx:SodaCLAntlrParser.Reference_checkContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#reference_check.
    def exitReference_check(self, ctx:SodaCLAntlrParser.Reference_checkContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#source_column_name.
    def enterSource_column_name(self, ctx:SodaCLAntlrParser.Source_column_nameContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#source_column_name.
    def exitSource_column_name(self, ctx:SodaCLAntlrParser.Source_column_nameContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#target_column_name.
    def enterTarget_column_name(self, ctx:SodaCLAntlrParser.Target_column_nameContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#target_column_name.
    def exitTarget_column_name(self, ctx:SodaCLAntlrParser.Target_column_nameContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#section_header.
    def enterSection_header(self, ctx:SodaCLAntlrParser.Section_headerContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#section_header.
    def exitSection_header(self, ctx:SodaCLAntlrParser.Section_headerContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#table_checks_header.
    def enterTable_checks_header(self, ctx:SodaCLAntlrParser.Table_checks_headerContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#table_checks_header.
    def exitTable_checks_header(self, ctx:SodaCLAntlrParser.Table_checks_headerContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#partition_name.
    def enterPartition_name(self, ctx:SodaCLAntlrParser.Partition_nameContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#partition_name.
    def exitPartition_name(self, ctx:SodaCLAntlrParser.Partition_nameContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#table_filter_header.
    def enterTable_filter_header(self, ctx:SodaCLAntlrParser.Table_filter_headerContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#table_filter_header.
    def exitTable_filter_header(self, ctx:SodaCLAntlrParser.Table_filter_headerContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#column_configurations_header.
    def enterColumn_configurations_header(self, ctx:SodaCLAntlrParser.Column_configurations_headerContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#column_configurations_header.
    def exitColumn_configurations_header(self, ctx:SodaCLAntlrParser.Column_configurations_headerContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#checks_for_each_dataset_header.
    def enterChecks_for_each_dataset_header(self, ctx:SodaCLAntlrParser.Checks_for_each_dataset_headerContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#checks_for_each_dataset_header.
    def exitChecks_for_each_dataset_header(self, ctx:SodaCLAntlrParser.Checks_for_each_dataset_headerContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#checks_for_each_column_header.
    def enterChecks_for_each_column_header(self, ctx:SodaCLAntlrParser.Checks_for_each_column_headerContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#checks_for_each_column_header.
    def exitChecks_for_each_column_header(self, ctx:SodaCLAntlrParser.Checks_for_each_column_headerContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#signed_number.
    def enterSigned_number(self, ctx:SodaCLAntlrParser.Signed_numberContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#signed_number.
    def exitSigned_number(self, ctx:SodaCLAntlrParser.Signed_numberContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#number.
    def enterNumber(self, ctx:SodaCLAntlrParser.NumberContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#number.
    def exitNumber(self, ctx:SodaCLAntlrParser.NumberContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#integer.
    def enterInteger(self, ctx:SodaCLAntlrParser.IntegerContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#integer.
    def exitInteger(self, ctx:SodaCLAntlrParser.IntegerContext):
        pass


    # Enter a parse tree produced by SodaCLAntlrParser#identifier.
    def enterIdentifier(self, ctx:SodaCLAntlrParser.IdentifierContext):
        pass

    # Exit a parse tree produced by SodaCLAntlrParser#identifier.
    def exitIdentifier(self, ctx:SodaCLAntlrParser.IdentifierContext):
        pass



del SodaCLAntlrParser