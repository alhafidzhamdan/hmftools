package com.hartwig.hmftools.common.cuppa;

import static java.lang.String.format;

import static com.hartwig.hmftools.common.cuppa.CategoryType.COMBINED;
import static com.hartwig.hmftools.common.utils.FileReaderUtils.createFieldsIndexMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CuppaDataFile
{
    public final CategoryType Category;
    public final ResultType Result;
    public final String DataType;
    public final String Value;

    public final Map<String,Double> CancerTypeValues;

    private static final String DELIMITER = ",";
    public static final String FLD_CATEGORY = "Category";
    public static final String FLD_RESULT_TYPE = "ResultType";
    public static final String FLD_DATA_TYPE = "DataType";
    public static final String FLD_VALUE = "Value";
    public static final String FLD_REF_CANCER_TYPE = "RefCancerType";
    public static final String FLD_REF_VALUE = "RefValue";

    public static final String CUPPA_DATAFILE = ".cup.data.csv";

    public CuppaDataFile(
            final CategoryType category, final ResultType resultType,
            final String dataType, final String value, final Map<String,Double> cancerTypeValues)
    {
        Category = category;
        DataType = dataType;
        Result = resultType;
        Value = value;
        CancerTypeValues = cancerTypeValues;
    }

    public static String generateFilename(final String basePath, final String sample)
    {
        return basePath + File.separator + sample + CUPPA_DATAFILE;
    }

    public static String header()
    {
        StringJoiner sj = new StringJoiner(DELIMITER);
        sj.add(FLD_CATEGORY);
        sj.add(FLD_RESULT_TYPE);
        sj.add(FLD_DATA_TYPE);
        sj.add(FLD_VALUE);
        sj.add(FLD_REF_CANCER_TYPE);
        sj.add(FLD_REF_VALUE);
        return sj.toString();
    }

    public void write(final BufferedWriter writer, final String sampleId) throws IOException
    {
        String commonValues = format("%s,%s,%s,%s", Category, Result, DataType, Value);

        for(Map.Entry<String,Double> cancerValues : CancerTypeValues.entrySet())
        {
            if(sampleId != null)
                writer.write(format("%s,", sampleId));

            writer.write(format("%s,%s,%.3g", commonValues, cancerValues.getKey(), cancerValues.getValue()));
            writer.newLine();
        }
    }

    public static List<CuppaDataFile> read(final String filename) throws IOException
    {
        List<CuppaDataFile> results = Lists.newArrayList();

        BufferedReader fileReader = new BufferedReader(new FileReader(filename));

        String line = fileReader.readLine();
        final Map<String, Integer> fieldsMap = createFieldsIndexMap(line, DELIMITER);

        int categoryIndex = fieldsMap.get(FLD_CATEGORY);
        int resultTypeIndex = fieldsMap.get(FLD_RESULT_TYPE);
        int dataTypeIndex = fieldsMap.get(FLD_DATA_TYPE);
        int valueIndex = fieldsMap.get(FLD_VALUE);
        int refCancerTypeIndex = fieldsMap.get(FLD_REF_CANCER_TYPE);
        int refValueIndex = fieldsMap.get(FLD_REF_VALUE);

        CuppaDataFile currentResult = null;

        while((line = fileReader.readLine()) != null)
        {
            String[] values = line.split(DELIMITER, -1);

            String dataType = values[dataTypeIndex];

            String categoryStr = values[categoryIndex];

            CategoryType category = CategoryType.valueOf(categoryStr);
            ResultType resultType = ResultType.valueOf(values[resultTypeIndex]);

            if(category == COMBINED)
                resultType = ResultType.CLASSIFIER;

            String value = values[valueIndex];
            String refCancerType = values[refCancerTypeIndex];
            double refValue = Double.parseDouble(values[refValueIndex]);

            if(currentResult == null || currentResult.CancerTypeValues.containsKey(refCancerType))
            {
                currentResult = new CuppaDataFile(category, resultType, dataType, value, Maps.newHashMap());
                results.add(currentResult);
            }

            currentResult.CancerTypeValues.put(refCancerType, refValue);
        }

        return results;
    }

    public static List<String> getRankedCancerTypes(final Map<String,Double> cancerTypeValues)
    {
        List<String> cancerTypes = Lists.newArrayList();
        List<Double> scores = Lists.newArrayList();

        for(Map.Entry<String,Double> entry : cancerTypeValues.entrySet())
        {
            int index = 0;
            double score = entry.getValue();

            while(index < scores.size())
            {
                if(score > scores.get(index))
                    break;

                ++index;
            }

            scores.add(index, score);
            cancerTypes.add(index, entry.getKey());
        }

        return cancerTypes;
    }

}
