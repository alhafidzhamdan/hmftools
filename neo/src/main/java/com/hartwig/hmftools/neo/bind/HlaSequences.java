package com.hartwig.hmftools.neo.bind;

import static com.hartwig.hmftools.common.utils.FileWriterUtils.createFieldsIndexMap;
import static com.hartwig.hmftools.neo.NeoCommon.NE_LOGGER;
import static com.hartwig.hmftools.neo.bind.BindCommon.FLD_ALLELE;
import static com.hartwig.hmftools.neo.bind.BindCommon.DELIM;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeCoordinates;

public class HlaSequences
{
    private final List<List<Integer>> mPositionHlaAminoAcids;

    private final Map<String,List<String>> mAllelePositionSequences;

    private boolean mIsValid;

    public static final String HLA_DEFINITIONS_FILE = "hla_defintions_file";

    public HlaSequences()
    {
        mPositionHlaAminoAcids = Lists.newArrayList();
        mAllelePositionSequences = Maps.newHashMap();
        mIsValid = false;
    }

    public boolean isValid() { return mIsValid; }

    public boolean hasAlleleDefinition(final String allele) { return mAllelePositionSequences.containsKey(allele); }

    public String getSequence(final String allele, int peptidePosition)
    {
        List<String> posSequences = mAllelePositionSequences.get(allele);
        return posSequences != null && posSequences.size() > peptidePosition ? posSequences.get(peptidePosition) : null;
    }

    public Map<String,List<String>> getAllelePositionSequences() { return mAllelePositionSequences; }

    public void load(final String hlaDefinitionsFile)
    {
        loadHlaPositions();
        mIsValid &= loadHlaDefinitions(hlaDefinitionsFile);
    }

    private void loadHlaPositions()
    {
        final List<String> lines = new BufferedReader(new InputStreamReader(
                HlaSequences.class.getResourceAsStream("/ref/hla_allele_bind_positions.csv")))
                .lines().collect(Collectors.toList());

        String[] columns = lines.get(0).split(DELIM);
        lines.remove(0);

        int positionCount = columns.length - 1;

        for(int i = 0; i < positionCount; ++i)
        {
            mPositionHlaAminoAcids.add(Lists.newArrayList());
        }

        for(String line : lines)
        {
            String[] items = line.split(DELIM);

            if(items.length != positionCount + 1)
            {
                mIsValid = false;
                return;
            }

            int aminoAcidPosition = Integer.parseInt(items[0]);

            for(int i = 1; i < items.length; ++i)
            {
                boolean applies = Boolean.parseBoolean(items[i]);

                if(applies)
                {
                    int pos = i - 1;
                    List<Integer> positions = mPositionHlaAminoAcids.get(pos);
                    positions.add(aminoAcidPosition);
                }
            }
        }
    }

    private boolean loadHlaDefinitions(final String filename)
    {
        if(filename == null || !Files.exists(Paths.get(filename)))
            return false;

        try
        {
            final List<String> lines = Files.readAllLines(new File(filename).toPath());

            final Map<String,Integer> fieldsIndexMap = createFieldsIndexMap(lines.get(0), DELIM);
            lines.remove(0);

            int alleleIndex = fieldsIndexMap.get(FLD_ALLELE);
            int seqIndex = fieldsIndexMap.get("Sequence");

            for(String line : lines)
            {
                String[] items = line.split(DELIM);

                String allele = items[alleleIndex];
                String sequence = items[seqIndex];

                List<String> posSequences = Lists.newArrayList();
                mAllelePositionSequences.put(allele, posSequences);

                for(int pos = 0; pos < mPositionHlaAminoAcids.size(); ++pos)
                {
                    List<Integer> posAminoAcids = mPositionHlaAminoAcids.get(pos);

                    StringBuilder posSequence = new StringBuilder();

                    for(Integer aaPos : posAminoAcids)
                    {
                        // the positions are 1-based vs the sequence indexed from 0
                        String alleleAA = sequence.substring(aaPos - 1, aaPos);
                        posSequence.append(alleleAA);
                    }

                    posSequences.add(posSequence.toString());
                }
            }

            NE_LOGGER.info("loaded {} HLA allele definitions from {}", mAllelePositionSequences.size(), filename);
        }
        catch(IOException e)
        {
            NE_LOGGER.error("failed to load HLA definitions file({}): {}", filename, e.toString());
            return false;
        }

        return true;
    }

}
