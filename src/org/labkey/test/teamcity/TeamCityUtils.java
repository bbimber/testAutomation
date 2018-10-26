package org.labkey.test.teamcity;

import org.apache.commons.collections.map.UnmodifiableMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.util.FileUtil;
import org.labkey.api.writer.PrintWriters;
import org.labkey.test.TestFileUtils;
import org.labkey.test.util.Maps;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TeamCityUtils
{
    private static final Map<String, List<Number>> buildStatistics = new HashMap<>();

    public static Map<String, List<Number>> getBuildStatistics()
    {
        return UnmodifiableMap.decorate(buildStatistics);
    }

    public static void reportBuildStatisticValue(String key, Number value)
    {
        reportBuildStatisticValue(key, value, true);
    }

    public static void reportBuildStatisticValue(String key, Number value, boolean combine)
    {
        List<Number> allValues = storeBuildStatistic(key, value);
        if (combine && allValues.size() > 1)
        {
            value = allValues.stream().mapToDouble(Number::doubleValue).sum() / allValues.size();
        }
        else if (allValues.size() > 1)
        {
            TestLogger.log("WARNING: Overwriting previous build statistic [" + key + "]: " + allValues.get(0) + " => " + value);
        }

        String valString = new DecimalFormat("#.######").format(value);
        if (valString.contains("."))
            valString = StringUtils.strip(valString, "0");
        else
            valString = StringUtils.stripStart(valString, "0");

        reportBuildStatisticValue(key, valString);
    }

    private static List<Number> storeBuildStatistic(String key, Number value)
    {
        final List<Number> statisticValues;
        if (buildStatistics.containsKey(key))
        {
            statisticValues = buildStatistics.get(key);
        }
        else
        {
            statisticValues = new ArrayList<>();
            buildStatistics.put(key, statisticValues);
        }
        statisticValues.add(value);
        return statisticValues;
    }

    // https://confluence.jetbrains.com/display/TCD18/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-ReportingBuildStatistics
    private static void reportBuildStatisticValue(String key, String value)
    {
        // https://confluence.jetbrains.com/display/TCD18/Customizing+Statistics+Charts#CustomizingStatisticsCharts-predefinedStatisticsKeys
        final List<String> reservedStatistics = Arrays.asList("coverage-graph", "duplicates-graph", "inspections-graph");
        if (reservedStatistics.contains(key))
            throw new IllegalArgumentException("'" + key + "' is a reserved statistic name");

        int valDigits = value.length();
        if (value.contains("."))
            valDigits -= value.substring(value.indexOf(".")).length();
        if (value.startsWith("-"))
            valDigits--;

        if (valDigits > 13)
            throw new IllegalArgumentException("Statistic value is too extreme: " + value);

        serviceMessage("buildStatisticValue", Maps.of("key", key, "value", value));
    }

    // Publish artifacts while the build is still in progress:
    // http://www.jetbrains.net/confluence/display/TCD18/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-PublishingArtifactswhiletheBuildisStillinProgress
    public static void publishArtifact(File file, @Nullable String destination)
    {
        if (file != null && file.exists())
        {
            String labkeyRoot = new File(TestFileUtils.getLabKeyRoot()).getAbsolutePath();
            String filePath = FileUtil.getAbsoluteCaseSensitiveFile(file).getAbsolutePath();
            if (filePath.startsWith(labkeyRoot))
            {
                // relativize path to labkey project root
                String path = filePath.substring(labkeyRoot.length());
                path = StringUtils.stripStart(path, File.separator);
                StringBuilder message = new StringBuilder();
                message.append(path);
                if (destination != null)
                {
                    message.append(" => ");
                    message.append(destination);
                }

                serviceMessage("publishArtifacts", message.toString());
            }
            else
            {
                TestLogger.log("Refusing to publish file from outside the LabKey enlistment: " + filePath);
            }
        }
    }

    // https://confluence.jetbrains.com/display/TCD18/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-Escapedvalues
    public static void serviceMessage(String messageName, Map<String, String> attributes)
    {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("##teamcity[");
        messageBuilder.append(validateId(messageName));
        // Certain message types take a single unkeyed value rather than key/value pairs (e.g. 'publishArtifacts')
        if (attributes.size() == 1 && attributes.containsKey(null))
        {
            messageBuilder.append(" ");
            appendValue(messageBuilder, attributes.values().iterator().next());
        }
        else if (attributes.containsKey(null) || attributes.containsValue(null))
        {
            throw new IllegalArgumentException("Provide keys and values for all service message attributes: " + attributes);
        }
        else
        {
            for (String key : attributes.keySet())
            {
                String value = attributes.get(key);
                messageBuilder.append(" ");
                messageBuilder.append(validateId(key));
                messageBuilder.append("=");
                appendValue(messageBuilder, value);
            }
        }
        messageBuilder.append("]");
        System.out.println(messageBuilder.toString());
    }

    public static void serviceMessage(String messageName, String value)
    {
        serviceMessage(messageName, Maps.of(null, value));
    }

    // https://confluence.jetbrains.com/display/TCD18/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-Escapedvalues
    private static void appendValue(StringBuilder sb, String value)
    {
        value = value
                .replace("|", "||")
                .replace("'", "|'")
                .replace("\n", "|n")
                .replace("\r", "|r")
                .replace("[", "|[")
                .replace("]", "|]");
        sb.append("'").append(value).append("'");
    }

    private static String validateId(String id)
    {
        Pattern idPattern = Pattern.compile("[A-Za-z][0-9A-Za-z-]*");
        Matcher matcher = idPattern.matcher(id);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + id + "' is not a valid message or attribute name");
        return id;
    }

    private void writeActionStatistics(int totalActions, int coveredActions, Double actionCoveragePercent)
    {
        // TODO: Create static class for managing teamcity-info.xml file.
        File xmlFile = new File(TestFileUtils.getLabKeyRoot(), "teamcity-info.xml");
        try (Writer writer = PrintWriters.getPrintWriter(xmlFile))
        {
            xmlFile.createNewFile();

            writer.write("<build>\n");
            writer.write("\t<statisticValue key=\"totalActions\" value=\"" + totalActions + "\"/>\n");
            writer.write("\t<statisticValue key=\"coveredActions\" value=\"" + coveredActions + "\"/>\n");
            writer.write("\t<statisticValue key=\"actionCoveragePercent\" value=\"" + actionCoveragePercent + "\"/>\n");
            writer.write("</build>");
        }
        catch (IOException ignore){}
    }

}