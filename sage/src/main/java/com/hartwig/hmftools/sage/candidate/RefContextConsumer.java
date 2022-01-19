package com.hartwig.hmftools.sage.candidate;

import static com.hartwig.hmftools.common.utils.sv.BaseRegion.positionsOverlap;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import com.hartwig.hmftools.common.samtools.CigarHandler;
import com.hartwig.hmftools.common.samtools.CigarTraversal;
import com.hartwig.hmftools.common.utils.sv.ChrBaseRegion;
import com.hartwig.hmftools.sage.common.RefSequence;
import com.hartwig.hmftools.sage.config.SageConfig;
import com.hartwig.hmftools.sage.common.IndexedBases;
import com.hartwig.hmftools.sage.common.ReadContext;
import com.hartwig.hmftools.sage.read.ReadContextFactory;
import com.hartwig.hmftools.sage.read.NumberEvents;

import org.apache.commons.compress.utils.Lists;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SAMRecord;

public class RefContextConsumer implements Consumer<SAMRecord>
{
    private final SageConfig mConfig;
    private final ChrBaseRegion mBounds;
    private final RefSequence mRefGenome;
    private final RefContextCache mRefContextCache;
    private final ReadContextFactory mReadContextFactory;

    public RefContextConsumer(
            final SageConfig config, final ChrBaseRegion bounds, final RefSequence refGenome, final RefContextCache refContextCache)
    {
        mBounds = bounds;
        mRefGenome = refGenome;
        mRefContextCache = refContextCache;
        mReadContextFactory = new ReadContextFactory(config.ReadContextFlankSize);

        mConfig = config;
    }

    @Override
    public void accept(final SAMRecord record)
    {
        if(!inBounds(record) || reachedDepthLimit(record))
            return;

        final List<AltRead> altReads = Lists.newArrayList();
        final IndexedBases refBases = mRefGenome.alignment();

        int numberOfEvents = NumberEvents.numberOfEvents(record, mRefGenome);

        final CigarHandler handler = new CigarHandler()
        {
            @Override
            public void handleAlignment(final SAMRecord record, final CigarElement element, final int readIndex,
                    final int refPosition)
            {
                altReads.addAll(processAlignment(record, readIndex, refPosition, element.getLength(), refBases, numberOfEvents));
            }

            @Override
            public void handleInsert(final SAMRecord record, final CigarElement element, final int readIndex,
                    final int refPosition)
            {
                Optional.ofNullable(processInsert(element, record, readIndex, refPosition, refBases, numberOfEvents))
                        .ifPresent(altReads::add);
            }

            @Override
            public void handleDelete(final SAMRecord record, final CigarElement element, final int readIndex,
                    final int refPosition)
            {
                Optional.ofNullable(processDel(element, record, readIndex, refPosition, refBases, numberOfEvents))
                        .ifPresent(altReads::add);
            }
        };

        CigarTraversal.traverseCigar(record, handler);

        checkCoreExtension(altReads);

        altReads.forEach(AltRead::updateRefContext);
    }

    private AltRead processInsert(
            final CigarElement element, final SAMRecord record, int readIndex, int refPosition,
            final IndexedBases refBases, int numberOfEvents)
    {
        int refIndex = refBases.index(refPosition);
        boolean sufficientMapQuality = record.getMappingQuality() >= mConfig.MinMapQuality;

        if(refPosition <= mBounds.end() && refPosition >= mBounds.start())
        {
            final String ref = new String(refBases.Bases, refIndex, 1);
            final String alt = new String(record.getReadBases(), readIndex, element.getLength() + 1);
            boolean findReadContext = withinReadContext(readIndex, record);

            final RefContext refContext = mRefContextCache.getOrCreateRefContext(record.getContig(), refPosition);
            if(!reachedDepthLimit(refContext))
            {
                final int baseQuality = baseQuality(readIndex, record, alt.length());
                final ReadContext readContext =
                        findReadContext ? mReadContextFactory.createInsertContext(alt, refPosition, readIndex, record, refBases) : null;
                return new AltRead(refContext, ref, alt, baseQuality, numberOfEvents, sufficientMapQuality, readContext);
            }
        }

        return null;
    }

    private AltRead processDel(
            final CigarElement element, final SAMRecord record, int readIndex, int refPosition,
            final IndexedBases refBases, int numberOfEvents)
    {
        int refIndex = refBases.index(refPosition);
        boolean sufficientMapQuality = record.getMappingQuality() >= mConfig.MinMapQuality;

        if(refPosition <= mBounds.end() && refPosition >= mBounds.start())
        {
            final String ref = new String(refBases.Bases, refIndex, element.getLength() + 1);
            final String alt = new String(record.getReadBases(), readIndex, 1);
            boolean findReadContext = withinReadContext(readIndex, record);

            final RefContext refContext = mRefContextCache.getOrCreateRefContext(record.getContig(), refPosition);
            if(refContext != null && !reachedDepthLimit(refContext))
            {
                final int baseQuality = baseQuality(readIndex, record, 2);
                final ReadContext readContext =
                        findReadContext ? mReadContextFactory.createDelContext(ref, refPosition, readIndex, record, refBases) : null;
                return new AltRead(refContext, ref, alt, baseQuality, numberOfEvents, sufficientMapQuality, readContext);
            }
        }

        return null;
    }

    private List<AltRead> processAlignment(
            final SAMRecord record, int readBasesStartIndex, int refPositionStart,
            int alignmentLength, final IndexedBases refBases, int numberOfEvents)
    {
        final List<AltRead> result = Lists.newArrayList();
        boolean sufficientMapQuality = record.getMappingQuality() >= mConfig.MinMapQuality;

        int refIndex = refBases.index(refPositionStart);

        for(int i = 0; i < alignmentLength; i++)
        {
            int refPosition = refPositionStart + i;
            int readBaseIndex = readBasesStartIndex + i;
            int refBaseIndex = refIndex + i;

            if(!mBounds.containsPosition(refPosition))
                continue;

            final byte refByte = refBases.Bases[refBaseIndex];
            final String ref = String.valueOf((char) refByte);
            final byte readByte = record.getReadBases()[readBaseIndex];
            boolean isWithinReadContext = withinReadContext(readBaseIndex, record);

            final RefContext refContext = mRefContextCache.getOrCreateRefContext(record.getContig(), refPosition);
            if(refContext != null && !reachedDepthLimit(refContext))
            {
                if(readByte != refByte)
                {
                    int baseQuality = record.getBaseQualities()[readBaseIndex];
                    final String alt = String.valueOf((char) readByte);
                    final ReadContext readContext = isWithinReadContext ?
                            mReadContextFactory.createSNVContext(refPosition, readBaseIndex, record, refBases) : null;

                    result.add(new AltRead(refContext, ref, alt, baseQuality, numberOfEvents, sufficientMapQuality, readContext));

                    if(mConfig.MnvEnabled)
                    {
                        int mnvMaxLength = mnvLength(readBaseIndex, refBaseIndex, record.getReadBases(), refBases.Bases);
                        for(int mnvLength = 2; mnvLength <= mnvMaxLength; mnvLength++)
                        {
                            final String mnvRef = new String(refBases.Bases, refBaseIndex, mnvLength);
                            final String mnvAlt = new String(record.getReadBases(), readBaseIndex, mnvLength);

                            // Only check last base because some subsets may not be valid,
                            // ie CA > TA is not a valid subset of CAC > TAT
                            if(mnvRef.charAt(mnvLength - 1) != mnvAlt.charAt(mnvLength - 1))
                            {
                                final ReadContext mnvReadContext = isWithinReadContext ? mReadContextFactory.createMNVContext(refPosition,
                                        readBaseIndex,
                                        mnvLength,
                                        record,
                                        refBases) : null;

                                result.add(new AltRead(refContext,
                                        mnvRef,
                                        mnvAlt,
                                        baseQuality,
                                        NumberEvents.numberOfEventsWithMNV(numberOfEvents, mnvRef, mnvAlt),
                                        sufficientMapQuality,
                                        mnvReadContext));
                            }
                        }
                    }
                }
                else
                {
                    refContext.refRead(sufficientMapQuality);
                }
            }
        }

        return result;
    }

    private boolean withinReadContext(int readIndex, final SAMRecord record)
    {
        return readIndex >= mConfig.ReadContextFlankSize && readIndex < record.getReadLength() - mConfig.ReadContextFlankSize;
    }

    @VisibleForTesting
    static int mnvLength(int readIndex, int refIndex, byte[] readBases, byte[] refBases)
    {
        final Function<Integer, Boolean> isDifferent =
                i -> refIndex + i < refBases.length && readIndex + i < readBases.length && refBases[refIndex + i] != readBases[readIndex
                        + i];

        if(isDifferent.apply(2))
            return 3;

        return isDifferent.apply((1)) ? 2 : 1;
    }

    private int baseQuality(int readIndex, final SAMRecord record, int length)
    {
        int maxIndex = Math.min(readIndex + length, record.getBaseQualities().length) - 1;
        int quality = Integer.MAX_VALUE;
        for(int i = readIndex; i <= maxIndex; i++)
        {
            quality = Math.min(quality, record.getBaseQualities()[i]);
        }
        return quality;
    }

    private boolean inBounds(final SAMRecord record)
    {
        return positionsOverlap(record.getStart(), record.getEnd(), mBounds.start(), mBounds.end());
    }

    private boolean reachedDepthLimit(final RefContext refContext)
    {
        return refContext.exceedsDepthLimit(mConfig.MaxReadDepth, mConfig.MaxReadDepthPanel);
    }

    private boolean reachedDepthLimit(final SAMRecord record)
    {
        RefContext startRefContext = mRefContextCache.getOrCreateRefContext(mBounds.Chromosome, record.getAlignmentStart());

        if(reachedDepthLimit(startRefContext))
            return true;

        RefContext endRefContext = mRefContextCache.getOrCreateRefContext(mBounds.Chromosome, record.getAlignmentEnd());

        return reachedDepthLimit(endRefContext);
    }

    private void checkCoreExtension(final List<AltRead> altReads)
    {
        if(altReads.size() < 2)
            return;

        // if an SNV core overlaps an indel core, then extend the cores of both
        for(int i = 0; i < altReads.size(); i++)
        {
            final AltRead snv = altReads.get(i);
            if(!snv.isIndel() && snv.containsReadContext())
            {
                for(int j = altReads.size() - 1; j > i; j--)
                {
                    final AltRead nextIndel = altReads.get(j);
                    if(nextIndel != null && nextIndel.isIndel() && nextIndel.containsReadContext())
                    {
                        if(nextIndel.leftCoreIndex() - nextIndel.length() <= snv.rightCoreIndex())
                        {
                            snv.extend(nextIndel);
                            nextIndel.extend(snv);
                        }
                    }
                }
            }
        }

        for(int i = altReads.size() - 1; i >= 0; i--)
        {
            final AltRead snv = altReads.get(i);

            if(!snv.isIndel() && snv.containsReadContext())
            {
                for(int j = 0; j < i; j++)
                {
                    final AltRead previousIndel = altReads.get(j);
                    if(previousIndel != null && previousIndel.isIndel() && previousIndel.containsReadContext())
                    {
                        if(previousIndel.rightCoreIndex() + previousIndel.length() >= snv.leftCoreIndex())
                        {
                            previousIndel.extend(snv);
                            snv.extend(previousIndel);
                        }
                    }
                }

            }
        }
    }
}