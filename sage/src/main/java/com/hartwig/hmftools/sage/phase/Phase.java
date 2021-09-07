package com.hartwig.hmftools.sage.phase;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.hartwig.hmftools.common.genome.region.HmfTranscriptRegion;
import com.hartwig.hmftools.sage.config.SageConfig;
import com.hartwig.hmftools.sage.variant.SageVariant;

import org.jetbrains.annotations.NotNull;

public class Phase implements Consumer<SageVariant>
{
    public static final int PHASE_BUFFER = 150;

    private final DedupRealign mDedupRealign;
    private final DedupMnv mDedupMnv;
    private final LocalPhaseSet mLocalPhaseSet;
    private final LocalRealignSet mLocalRealignSet;
    private final DedupIndel mDedupIndel;
    private final MixedSomaticGermlineIdentifier mMixedSomaticGermlineIdentifier;
    private final MixedSomaticGermlineDedup mMixedSomaticGermlineDedup;
    private final PhasedInframeIndel mPhasedInframeIndel;
    private final RightAlignMicrohomology mRrightAlignMicrohomology;

    public Phase(final List<HmfTranscriptRegion> transcripts, final Consumer<SageVariant> consumer)
    {
        mDedupRealign = new DedupRealign(consumer);
        mDedupIndel = new DedupIndel(mDedupRealign);
        mDedupMnv = new DedupMnv(mDedupIndel);
        mMixedSomaticGermlineDedup = new MixedSomaticGermlineDedup(mDedupMnv, transcripts);
        mMixedSomaticGermlineIdentifier = new MixedSomaticGermlineIdentifier(mMixedSomaticGermlineDedup);
        mPhasedInframeIndel = new PhasedInframeIndel(mMixedSomaticGermlineIdentifier, transcripts);
        mRrightAlignMicrohomology = new RightAlignMicrohomology(mPhasedInframeIndel, transcripts);
        mLocalRealignSet = new LocalRealignSet(mRrightAlignMicrohomology);
        mLocalPhaseSet = new LocalPhaseSet(mLocalRealignSet);
    }

    @NotNull
    public Set<Integer> passingPhaseSets()
    {
        return mLocalPhaseSet.passingPhaseSets();
    }

    @Override
    public void accept(final SageVariant sageVariant)
    {
        mLocalPhaseSet.accept(sageVariant);
    }

    public void flush()
    {
        mLocalPhaseSet.flush();
        mLocalRealignSet.flush();
        mRrightAlignMicrohomology.flush();
        mPhasedInframeIndel.flush();
        mMixedSomaticGermlineIdentifier.flush();
        mMixedSomaticGermlineDedup.flush();
        mDedupMnv.flush();
        mDedupIndel.flush();
        mDedupRealign.flush();
    }
}
