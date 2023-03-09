package com.linkedin.feathr.offline.util

import org.apache.commons.cli.{GnuParser, Options, Option => CmdOption}

/**
 * Commandline parser class, used by FeatureJoinJob and FeatureGenJob to handle input commandline parameters
 * @param args commanlind argumnents
 * @param optionParams map of Options params, each map key is the long name of the option, and map value is a 4-tuple,
 *                     which are short name, description, arg name (null means not argument), default value (null means required),
 *                     respectively
 * @param extraOptions extra options that do not have the long names
 */
private[feathr] class CmdLineParser(args: Array[String], optionParams: Map[String, OptionParam], extraOptions: List[CmdOption] = List()) {

  // all available options
  val options = new Options
  optionParams.foreach {
    case (longOpt, OptionParam(opt, desc, argName, defVal)) =>
      val option = new CmdOption(opt, desc)
      if (defVal == null) {
        option.setRequired(true)
      }
      option.setLongOpt(longOpt)
      if (argName != null) {
        option.setArgs(1)
        option.setArgName(argName)
      }
      options.addOption(option)
  }

  // extra options is added to the 'options'
  extraOptions.foreach(opt => options.addOption(opt))

  val commandLine = new GnuParser().parse(options, args)

  /**
   * extractor the value of an option
   * @param optName option name
   * @return value of the option, throw exception if optName does not exist
   */
  def extractRequiredValue(optName: String): String =
    commandLine.getOptionValue(optName, optionParams(optName).defaultValue)

  /**
   * extract optional value as Option[String]
   * will return None if value of optName is empty string
   * @param optName
   * @return optional string value of the optName
   */
  def extractOptionalValue(optName: String): Option[String] = {
    try{
      commandLine.getOptionValue(optName, optionParams(optName).defaultValue) match {
        case "" => None
        case value: String => Some(value)
        case _ => None
      }
    }
    catch{
      case _: Exception => None
    }
  }
}
/**
 * Each map key is the long name of the option, and map value is a 4-tuple,
 * which are short name, description, arg name (null means not argument), default value (null means required),
 * respectively
 */
private[offline] case class OptionParam(shortName: String, description: String, argName: String, defaultValue: String)
