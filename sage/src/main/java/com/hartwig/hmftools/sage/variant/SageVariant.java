package com.hartwig.hmftools.sage.variant;

import java.util.List;
import java.util.Set;

import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.sage.candidate.Candidate;
import com.hartwig.hmftools.sage.read.ReadContext;
import com.hartwig.hmftools.sage.read.ReadContextCounter;

import org.jetbrains.annotations.NotNull;

public class SageVariant
{
    private final Candidate mCandidate;
    private final Set<String> mFilters;
    private final List<ReadContextCounter> mNormalAltContexts;
    private final List<ReadContextCounter> mTumorAltContexts;

    private int mLocalPhaseSet;
    private int mLocalRealignSet;
    private int mMixedImpact;
    private int mPhasedInframeIndel;
    private boolean mRealigned;

    public SageVariant(
            final Candidate candidate, final Set<String> filters,
            final List<ReadContextCounter> normal, final List<ReadContextCounter> tumorAltContexts)
    {
        mCandidate = candidate;
        mNormalAltContexts = normal;
        mTumorAltContexts = tumorAltContexts;
        mFilters = filters;
    }

    public Candidate candidate()
    {
        return mCandidate;
    }

    public String ref()
    {
        return variant().ref();
    }

    public String alt()
    {
        return variant().alt();
    }

    public boolean isRealigned()
    {
        return mRealigned;
    }

    public void realigned(final boolean realigned)
    {
        mRealigned = realigned;
    }

    public long end()
    {
        return position() + ref().length() - 1;
    }

    public boolean isIndel()
    {
        return variant().ref().length() != variant().alt().length();
    }

    public int phasedInframeIndel()
    {
        return mPhasedInframeIndel;
    }

    public void phasedInframeIndel(final int phasedInframeIndel)
    {
        mPhasedInframeIndel = phasedInframeIndel;
    }

    public boolean isMnv()
    {
        return variant().ref().length() >= 1 && variant().ref().length() == variant().alt().length();
    }

    public boolean isSnv()
    {
        return variant().ref().length() == 1 && variant().alt().length() == 1;
    }

    public boolean isInsert()
    {
        return variant().ref().length() < variant().alt().length();
    }

    public boolean isDelete()
    {
        return variant().ref().length() > variant().alt().length();
    }

    public int localPhaseSet()
    {
        return mLocalPhaseSet;
    }

    public void localPhaseSet(int localPhaseSet)
    {
        mLocalPhaseSet = localPhaseSet;
    }

    public int localRealignSet()
    {
        return mLocalRealignSet;
    }

    public void localRealignSet(int localRealignSet)
    {
        mLocalRealignSet = localRealignSet;
    }

    public int mixedGermlineImpact()
    {
        return mMixedImpact;
    }

    public void mixedGermlineImpact(final int mixedImpact)
    {
        mMixedImpact = mixedImpact;
    }

    public boolean isPassing()
    {
        return mFilters.isEmpty();
    }

    public boolean isTumorEmpty()
    {
        return mTumorAltContexts.isEmpty();
    }

    public boolean isNormalEmpty()
    {
        return mNormalAltContexts.isEmpty();
    }

    @NotNull
    public VariantHotspot variant()
    {
        return mCandidate.variant();
    }

    @NotNull
    public VariantTier tier()
    {
        return mCandidate.tier();
    }

    @NotNull
    public Set<String> filters()
    {
        return mFilters;
    }

    @NotNull
    public ReadContext readContext()
    {
        return mTumorAltContexts.get(0).ReadContext;
    }

    @NotNull
    public String microhomology()
    {
        return readContext().microhomology();
    }

    @NotNull
    public List<ReadContextCounter> normalAltContexts()
    {
        return mNormalAltContexts;
    }

    @NotNull
    public List<ReadContextCounter> tumorAltContexts()
    {
        return mTumorAltContexts;
    }

    @NotNull
    public String chromosome()
    {
        return variant().chromosome();
    }

    public long position()
    {
        return variant().position();
    }

    public int totalQuality()
    {
        return mTumorAltContexts.stream().mapToInt(ReadContextCounter::tumorQuality).sum();
    }

    public int maxQuality()
    {
        return mTumorAltContexts.stream().mapToInt(ReadContextCounter::tumorQuality).max().orElse(0);
    }

}
