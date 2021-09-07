package com.hartwig.hmftools.common.samtools;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.genome.position.GenomePosition;
import com.hartwig.hmftools.common.genome.region.GenomeRegion;
import com.hartwig.hmftools.common.utils.sv.ChrBaseRegion;

import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;

public class BamSlicer
{
    private final int mMinMappingQuality;
    private boolean mKeepDuplicates;
    private boolean mKeepSupplementaries;
    private boolean mKeepSecondaries;
    private boolean mConsumerHalt; // allow consumer to halt processing

    public BamSlicer(int minMappingQuality)
    {
        this(minMappingQuality, false, false, false);
    }

    public BamSlicer(int minMappingQuality, boolean keepDuplicates, boolean keepSupplementaries, boolean keepSecondaries)
    {
        mMinMappingQuality = minMappingQuality;
        mKeepDuplicates = keepDuplicates;
        mKeepSupplementaries = keepSupplementaries;
        mKeepSecondaries = keepSecondaries;
        mConsumerHalt = false;
    }

    public void haltProcessing() { mConsumerHalt = true; }

    public void slice(@NotNull final SamReader samReader, final List<ChrBaseRegion> regions, @NotNull final Consumer<SAMRecord> consumer)
    {
        mConsumerHalt = false;

        final QueryInterval[] queryIntervals = createIntervals(regions, samReader.getFileHeader());

        try (final SAMRecordIterator iterator = samReader.queryOverlapping(queryIntervals))
        {
            while (!mConsumerHalt && iterator.hasNext())
            {
                final SAMRecord record = iterator.next();

                if (passesFilters(record))
                {
                    consumer.accept(record);
                }
            }
        }
    }

    public List<SAMRecord> slice(@NotNull final SamReader samReader, final ChrBaseRegion region)
    {
        return slice(samReader, createIntervals(Lists.newArrayList(region), samReader.getFileHeader()));
    }

    public List<SAMRecord> slice(@NotNull final SamReader samReader, final GenomePosition variantRegion)
    {
        int position = (int)variantRegion.position();

        final QueryInterval[] queryIntervals = createIntervals(Lists.newArrayList(
                new ChrBaseRegion(variantRegion.chromosome(), position, position)), samReader.getFileHeader());

        return slice(samReader, queryIntervals);
    }

    public List<SAMRecord> slice(@NotNull final SamReader samReader, final QueryInterval[] queryIntervals)
    {
        final List<SAMRecord> records = Lists.newArrayList();

        try (final SAMRecordIterator iterator = samReader.queryOverlapping(queryIntervals))
        {
            while (iterator.hasNext())
            {
                final SAMRecord record = iterator.next();

                if(passesFilters(record))
                {
                    records.add(record);
                }
            }
        }

        return records;
    }

    public void sliceNoDups(@NotNull final SamReader samReader, final List<GenomeRegion> regions, final Consumer<SAMRecord> consumer)
    {
        // skips duplicate reads
        List<ChrBaseRegion> baseRegions = regions.stream().map(x -> ChrBaseRegion.from(x)).collect(Collectors.toList());

        final QueryInterval[] queryIntervals = QueryInterval.optimizeIntervals(createIntervals(baseRegions, samReader.getFileHeader()));

        Set<String> processed = Sets.newHashSet();
        try (final SAMRecordIterator iterator = samReader.queryOverlapping(queryIntervals))
        {
            while (iterator.hasNext())
            {
                final SAMRecord record = iterator.next();

                if(passesFilters(record))
                {
                    if (processed.add(record.toString()))
                    {
                        consumer.accept(record);
                    }
                }
            }
        }
    }

    public List<SAMRecord> queryMates(final SamReader samReader, final List<SAMRecord> records)
    {
        return records.stream().map(x -> samReader.queryMate(x))
                .filter(x -> x != null)
                .filter(x -> passesFilters(x))
                .collect(Collectors.toList());
    }

    private static QueryInterval[] createIntervals(final List<ChrBaseRegion> regions, final SAMFileHeader header)
    {
        final QueryInterval[] queryIntervals = new QueryInterval[regions.size()];

        for (int i = 0; i < regions.size(); ++i)
        {
            final ChrBaseRegion region = regions.get(i);
            int sequenceIndex = header.getSequenceIndex(region.Chromosome);

            if (sequenceIndex < 0)
                return null;


            queryIntervals[i] = new QueryInterval(sequenceIndex, region.start(), region.end());
        }

        return queryIntervals;
    }

    private boolean passesFilters(final SAMRecord record)
    {
        if(record.getMappingQuality() < mMinMappingQuality || record.getReadUnmappedFlag())
            return false;

        if(record.isSecondaryAlignment() && !mKeepSecondaries)
            return false;

        if(record.getSupplementaryAlignmentFlag() && !mKeepSupplementaries)
            return false;

        if(record.getDuplicateReadFlag() && !mKeepDuplicates)
            return false;

        return true;
    }
}
