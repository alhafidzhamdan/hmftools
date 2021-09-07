package com.hartwig.hmftools.sage.evidence;

import static com.hartwig.hmftools.sage.SageCommon.SG_LOGGER;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.utils.sv.ChrBaseRegion;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.sage.config.SageConfig;
import com.hartwig.hmftools.sage.context.AltContext;
import com.hartwig.hmftools.sage.context.RefContextConsumer;
import com.hartwig.hmftools.sage.context.RefContextFactory;
import com.hartwig.hmftools.sage.coverage.Coverage;
import com.hartwig.hmftools.sage.coverage.GeneCoverage;
import com.hartwig.hmftools.sage.ref.RefSequence;
import com.hartwig.hmftools.sage.sam.SamSlicer;
import com.hartwig.hmftools.sage.sam.SamSlicerFactory;

import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.reference.ReferenceSequenceFile;

public class CandidateEvidence
{
    private final SageConfig mConfig;
    private final List<VariantHotspot> mHotspots;
    private final List<ChrBaseRegion> mPanel;
    private final ReferenceSequenceFile mRefGenome;
    private final SamSlicerFactory mSamSlicerFactory;
    private final Coverage mCoverage;

    public CandidateEvidence(
            final SageConfig config, final List<VariantHotspot> hotspots, final List<ChrBaseRegion> panel,
            final SamSlicerFactory samSlicerFactory, final ReferenceSequenceFile refGenome, final Coverage coverage)
    {
        mConfig = config;
        mPanel = panel;
        mSamSlicerFactory = samSlicerFactory;
        mHotspots = hotspots;
        mRefGenome = refGenome;
        mCoverage = coverage;
    }

    @NotNull
    public List<AltContext> readBam(
            final String sample, final String bamFile, final RefSequence refSequence, final ChrBaseRegion bounds)
    {
        SG_LOGGER.trace("variant candidates {} position {}:{}", sample, bounds.Chromosome, bounds.start());
        final List<GeneCoverage> geneCoverage = mCoverage.coverage(sample, bounds.Chromosome);
        final RefContextFactory candidates = new RefContextFactory(mConfig, sample, mHotspots, mPanel);
        final RefContextConsumer refContextConsumer = new RefContextConsumer(mConfig, bounds, refSequence, candidates);

        final Consumer<SAMRecord> consumer = record ->
        {
            refContextConsumer.accept(record);
            if(!geneCoverage.isEmpty())
            {
                ChrBaseRegion alignment = new ChrBaseRegion(record.getContig(), record.getAlignmentStart(), record.getAlignmentEnd());
                geneCoverage.forEach(x -> x.accept(alignment));
            }
        };

        return readBam(bamFile, bounds, consumer, candidates);
    }

    @NotNull
    private List<AltContext> readBam(
            final String bamFile, final ChrBaseRegion bounds, final Consumer<SAMRecord> recordConsumer, final RefContextFactory candidates)
    {
        final List<AltContext> altContexts = Lists.newArrayList();

        final SamSlicer slicer = mSamSlicerFactory.create(bounds);

        try(final SamReader tumorReader = SamReaderFactory.makeDefault()
                .validationStringency(mConfig.Stringency)
                .referenceSource(new ReferenceSource(mRefGenome))
                .open(new File(bamFile)))
        {
            // First parse
            slicer.slice(tumorReader, recordConsumer);

            // Add all valid alt contexts
            altContexts.addAll(candidates.altContexts());
        }
        catch(Exception e)
        {
            throw new CompletionException(e);
        }

        return altContexts;
    }
}
