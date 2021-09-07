package com.hartwig.hmftools.sage.sam;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.samtools.BamSlicer;
import com.hartwig.hmftools.common.utils.sv.ChrBaseRegion;

import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class SamSlicer
{
    private final List<ChrBaseRegion> mRegions;

    private final BamSlicer mBamSlicer;

    public SamSlicer(final int minMappingQuality, final ChrBaseRegion slice)
    {
        mBamSlicer = new BamSlicer(minMappingQuality);
        mRegions = Collections.singletonList(slice);
    }

    public SamSlicer(final int minMappingQuality, final ChrBaseRegion slice, final List<ChrBaseRegion> panel)
    {
        mBamSlicer = new BamSlicer(minMappingQuality);
        mRegions = Lists.newArrayList();

        for(final ChrBaseRegion panelRegion : panel)
        {
            if(slice.Chromosome.equals(panelRegion.Chromosome) && panelRegion.start() <= slice.end()
                    && panelRegion.end() >= slice.start())
            {
                ChrBaseRegion overlap = new ChrBaseRegion(slice.Chromosome,
                        Math.max(panelRegion.start(), slice.start()),
                        Math.min(panelRegion.end(), slice.end()));

                mRegions.add(overlap);
            }
        }
    }

    public void slice(final SamReader samReader, @NotNull final Consumer<SAMRecord> consumer)
    {
        mBamSlicer.slice(samReader, mRegions, consumer);

        // TODO: is this required or are regions already exclusive??
        // return QueryInterval.optimizeIntervals(queryIntervals.toArray(new QueryInterval[queryIntervals.size()]));


        /*
        final QueryInterval[] queryIntervals = createIntervals(mRegions, samReader.getFileHeader());

        try(final SAMRecordIterator iterator = samReader.queryOverlapping(queryIntervals))
        {
            while(iterator.hasNext())
            {
                final SAMRecord record = iterator.next();
                if(samRecordMeetsQualityRequirements(record))
                {
                    consumer.accept(record);
                }
            }
        }
        */
    }
}
