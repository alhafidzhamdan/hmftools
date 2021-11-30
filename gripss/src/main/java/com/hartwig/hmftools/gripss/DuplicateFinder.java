package com.hartwig.hmftools.gripss;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.gripss.common.Breakend;
import com.hartwig.hmftools.gripss.common.SvData;
import com.hartwig.hmftools.gripss.links.AlternatePath;
import com.hartwig.hmftools.gripss.links.Link;
import com.hartwig.hmftools.gripss.links.LinkStore;

public class DuplicateFinder
{
    private final FilterCache mFilterCache;
    private final SvDataCache mDataCache;
    
    private final Set<Breakend> mDuplicateBreakends;
    private final Set<Breakend> mRescueBreakends;
    private final Set<Breakend> mSingleDuplicates; // SGLs which are duplicates or duplicates of SGLs

    private static final int MAX_DEDUP_SGL_SEEK_DISTANCE = 1000;
    private static final int MAX_DEDUP_SGL_ADDITIONAL_DISTANCE = 0;

    public DuplicateFinder(final SvDataCache dataCache, final FilterCache filterCache)
    {
        mDataCache = dataCache;
        mFilterCache = filterCache;
        
        mDuplicateBreakends = Sets.newHashSet();
        mRescueBreakends = Sets.newHashSet();
        mSingleDuplicates = Sets.newHashSet();
    }

    public Set<Breakend> duplicateBreakends() { return mDuplicateBreakends; }
    public Set<Breakend> rescueBreakends() { return mRescueBreakends; }
    public Set<Breakend> duplicateSglBreakends() { return mSingleDuplicates; };

    public void findDuplicateSVs(final List<AlternatePath> alternatePaths)
    {
        for(AlternatePath altPath : alternatePaths)
        {
            boolean firstIsPass = !mFilterCache.hasFilters(altPath.First);

            boolean anyInAltPathPasses = altPath.Links.stream()
                    .anyMatch(x -> !mFilterCache.hasFilters(x.breakendStart()) || !mFilterCache.hasFilters(x.breakendEnd()));

            if(altPath.Links.size() == 1)
            {
                Breakend first = altPath.First;
                Breakend second = altPath.Links.get(0).breakendEnd();

                // Favour PRECISE, PASSING, then QUAL
                if(!keepOriginal(first, second, firstIsPass, anyInAltPathPasses))
                {
                    mDuplicateBreakends.add(first);
                }
            }
            else
            {
                mDuplicateBreakends.add(altPath.First);
                mDuplicateBreakends.add(altPath.Second);

                if(firstIsPass || anyInAltPathPasses)
                {
                    for(Link link : altPath.Links)
                    {
                        mRescueBreakends.add(link.breakendStart());
                        mRescueBreakends.add(link.breakendEnd());
                    }
                }
            }
        }

        // duplicates supersede rescues
        mDuplicateBreakends.forEach(x -> mRescueBreakends.remove(x));
    }

    private static boolean keepOriginal(final Breakend original, final Breakend other, boolean originalIsPass, boolean otherIsPass)
    {
        if(original.imprecise() != other.imprecise())
            return !original.imprecise();

        if(originalIsPass != otherIsPass)
            return originalIsPass;

        return original.Qual > other.Qual;
    }

    public void findDuplicateSingles(final LinkStore linkStore)
    {
        for(SvData sv : mDataCache.getSvList())
        {
            if(!sv.isSgl())
                continue;
            
            Breakend breakend = sv.breakendStart();
            
            boolean isPass = mFilterCache.hasFilters(breakend);

            List<Breakend> nearbyBreakends = mDataCache.selectOthersNearby(
                    breakend, MAX_DEDUP_SGL_ADDITIONAL_DISTANCE, MAX_DEDUP_SGL_SEEK_DISTANCE);

            // look through duplicate breakends in the vacinity
            // if none of them require keeping the single, then mark it as a duplicate
            boolean keepSingle = true;

            for(Breakend otherBreakend : nearbyBreakends)
            {
                if(!isDuplicateCandidate(breakend, otherBreakend))
                    continue;

                if(!keepSingle(isPass, breakend, otherBreakend, linkStore))
                {
                    keepSingle = false;
                    break;
                }
            }

            if(!keepSingle)
            {
                mSingleDuplicates.add(breakend);
            }
            else
            {
                for(Breakend otherBreakend : nearbyBreakends)
                {
                    if(!isDuplicateCandidate(breakend, otherBreakend))
                        continue;

                    mSingleDuplicates.add(otherBreakend);

                    if(!otherBreakend.isSgl())
                        mSingleDuplicates.add(otherBreakend.otherBreakend());
                }
            }

            /*
            val exactPositionFilter = { other: StructuralVariantContext -> other.start >= sgl.minStart && other.start <= sgl.maxStart }
            val duplicateFilter = { other: StructuralVariantContext -> other.orientation == sgl.orientation && (other.precise || exactPositionFilter(other)) }
            val others = variantStore.selectOthersNearby(sgl, MAX_DEDUP_SGL_ADDITIONAL_DISTANCE, MAX_DEDUP_SGL_SEEK_DISTANCE, duplicateFilter)
            if (!others.all { x -> keepSingle(sglPasses, sgl, x, softFilterStore, linkStore) }) {
                duplicates.add(sgl.vcfId)
            } else {
                others.forEach { x ->
                    x.vcfId.let { duplicates.add(it) }
                    x.mateId?.let { duplicates.add(it) }
                }
            }

             */

        }
        
    }


    
    private static boolean isDuplicateCandidate(final Breakend breakend, final Breakend otherBreakend)
    {
        return breakend.Orientation == otherBreakend.Orientation && (!otherBreakend.imprecise() || isExactPosition(otherBreakend));
    }

    private static boolean isExactPosition(final Breakend breakend)
    {
        return breakend.Position >= breakend.minPosition() && breakend.Position <= breakend.maxPosition(); 
    }

    private boolean keepSingle(
            boolean originalIsPass, final Breakend original, final Breakend alternative, final LinkStore linkStore)
    {
        if(linkStore.getBreakendLinks(alternative) != null)
            return false;
        
        boolean altIsPass = !mFilterCache.hasFilters(alternative);

        if(originalIsPass != altIsPass)
            return originalIsPass;

        return original.Qual > alternative.Qual;
    }
    
    /*

        class DedupSingle(val duplicates: Set<String>) {

    companion object {

        operator fun invoke(variantStore: VariantStore, softFilterStore: SoftFilterStore, linkStore: LinkStore): DedupSingle {
            val duplicates = mutableSetOf<String>()

            for (sgl in variantStore.selectAll().filter { x -> x.isSingle }) {
                val sglPasses = softFilterStore.isPassing(sgl.vcfId)

                val exactPositionFilter = { other: StructuralVariantContext -> other.start >= sgl.minStart && other.start <= sgl.maxStart }
                val duplicateFilter = { other: StructuralVariantContext -> other.orientation == sgl.orientation && (other.precise || exactPositionFilter(other)) }
                val others = variantStore.selectOthersNearby(sgl, MAX_DEDUP_SGL_ADDITIONAL_DISTANCE, MAX_DEDUP_SGL_SEEK_DISTANCE, duplicateFilter)
                if (!others.all { x -> keepSingle(sglPasses, sgl, x, softFilterStore, linkStore) }) {
                    duplicates.add(sgl.vcfId)
                } else {
                    others.forEach { x ->
                        x.vcfId.let { duplicates.add(it) }
                        x.mateId?.let { duplicates.add(it) }
                    }
                }
            }
            return DedupSingle(duplicates)
        }

        private fun keepSingle(originalPass: Boolean, original: StructuralVariantContext, alternative: StructuralVariantContext, softFilterStore: SoftFilterStore, linkStore: LinkStore): Boolean {
            if (linkStore.linkedVariants(alternative.vcfId).isNotEmpty()) {
                return false
            }

            val alternativePass = softFilterStore.isPassing(alternative.vcfId)
            if (originalPass != alternativePass) {
                return originalPass
            }

            return original.tumorQual > alternative.tumorQual
        }
    }

     */
}
