package com.hartwig.hmftools.linx.analysis;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.BND;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.DEL;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.DUP;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.INF;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.INS;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.INV;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.SGL;
import static com.hartwig.hmftools.linx.analysis.ClusterAnalyser.SMALL_CLUSTER_SIZE;
import static com.hartwig.hmftools.linx.analysis.PairResolution.classifyPairClusters;
import static com.hartwig.hmftools.linx.analysis.PairResolution.isClusterPairType;
import static com.hartwig.hmftools.linx.analysis.SvUtilities.NO_LENGTH;
import static com.hartwig.hmftools.linx.analysis.SvUtilities.copyNumbersEqual;
import static com.hartwig.hmftools.linx.chaining.ChainPloidyLimits.ploidyMatch;
import static com.hartwig.hmftools.linx.chaining.LinkFinder.getMinTemplatedInsertionLength;
import static com.hartwig.hmftools.linx.types.ResolvedType.COMPLEX;
import static com.hartwig.hmftools.linx.types.ResolvedType.DEL_TI;
import static com.hartwig.hmftools.linx.types.ResolvedType.DUP_BE;
import static com.hartwig.hmftools.linx.types.ResolvedType.DUP_TI;
import static com.hartwig.hmftools.linx.types.ResolvedType.LINE;
import static com.hartwig.hmftools.linx.types.ResolvedType.LOW_VAF;
import static com.hartwig.hmftools.linx.types.ResolvedType.NONE;
import static com.hartwig.hmftools.linx.types.ResolvedType.PAIR_INF;
import static com.hartwig.hmftools.linx.types.ResolvedType.PAIR_OTHER;
import static com.hartwig.hmftools.linx.types.ResolvedType.SGL_PAIR_DEL;
import static com.hartwig.hmftools.linx.types.ResolvedType.SGL_PAIR_DUP;
import static com.hartwig.hmftools.linx.types.ResolvedType.SGL_PAIR_INS;
import static com.hartwig.hmftools.linx.types.ResolvedType.SIMPLE_GRP;
import static com.hartwig.hmftools.linx.types.ResolvedType.UNBAL_TRANS;
import static com.hartwig.hmftools.linx.types.SvaConstants.MIN_DEL_LENGTH;
import static com.hartwig.hmftools.linx.types.SvaConstants.SHORT_TI_LENGTH;

import com.hartwig.hmftools.common.variant.structural.StructuralVariantType;
import com.hartwig.hmftools.linx.types.ResolvedType;
import com.hartwig.hmftools.linx.types.SvBreakend;
import com.hartwig.hmftools.linx.chaining.SvChain;
import com.hartwig.hmftools.linx.types.SvCluster;
import com.hartwig.hmftools.linx.types.SvLinkedPair;
import com.hartwig.hmftools.linx.types.SvVarData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SvClassification
{
    // super category for an SV or cluster
    public static final String SUPER_TYPE_SIMPLE = "SIMPLE";
    public static final String SUPER_TYPE_INSERTION = "INSERTION";
    public static final String SUPER_TYPE_BREAK_PAIR = "BREAK_PAIR";
    public static final String SUPER_TYPE_COMPLEX = "COMPLEX";
    public static final String SUPER_TYPE_INCOMPLETE = "INCOMPLETE";
    public static final String SUPER_TYPE_ARTIFACT = "ARTIFACT";

    private static final Logger LOGGER = LogManager.getLogger(SvClassification.class);


    public static String getSuperType(SvCluster cluster)
    {
        ResolvedType resolvedType = cluster.getResolvedType();

        if (resolvedType == LINE)
            return SUPER_TYPE_INSERTION;

        if (resolvedType.isSimple())
            return SUPER_TYPE_SIMPLE;

        if(isFilteredResolvedType(resolvedType))
            return SUPER_TYPE_ARTIFACT;

        if(isClusterPairType(resolvedType))
            return SUPER_TYPE_BREAK_PAIR;

        if(isIncompleteType(resolvedType) || resolvedType == PAIR_OTHER)
        {
            return SUPER_TYPE_INCOMPLETE;
        }

        return SUPER_TYPE_COMPLEX;
    }

    public static boolean isSimpleSingleSV(final SvCluster cluster)
    {
        return cluster.getSvCount() == 1 && cluster.getSV(0).isSimpleType();
    }

    public static boolean isIncompleteType(final ResolvedType resolvedType)
    {
        return (resolvedType == ResolvedType.INV || resolvedType == ResolvedType.SGL || resolvedType == ResolvedType.INF);
    }

    public static boolean isSyntheticType(SvCluster cluster)
    {
        ResolvedType resolvedType = cluster.getResolvedType();

        if(resolvedType.isSimple())
        {
            if(cluster.getSglBreakendCount() == 2)
                return false;
            else
                return cluster.getSvCount() > 1;
        }

        // will be more when look for synthetic INVs, SGLs and translocations
        if(isClusterPairType(resolvedType))
        {
            return cluster.getSvCount() > 2;
        }

        if(resolvedType == UNBAL_TRANS)
            return cluster.getSvCount() > 1;

        if(isIncompleteType(resolvedType))
            return cluster.getSvCount() > 1;

        return false;
    }

    public static boolean isFilteredResolvedType(final ResolvedType resolvedType)
    {
        return resolvedType == DUP_BE || resolvedType == LOW_VAF || resolvedType == PAIR_INF;
    }

    public static void setClusterResolvedState(
            SvCluster cluster, boolean isFinal, long longDelThreshold, long longDupThreshold, int proximityThreshold)
    {
        if(cluster.getResolvedType() != NONE)
            return;

        // isSpecificCluster(cluster);

        if(cluster.hasLinkingLineElements())
        {
            // skip further classification for now
            cluster.setResolved(true, LINE);
            return;
        }

        if (smallSimpleSVs(cluster))
        {
            if(cluster.getChains().size() == 1)
            {
                classifyPairClusters(cluster, longDelThreshold, longDupThreshold);

                if(cluster.getResolvedType() != NONE)
                    return;
            }

            boolean hasLongSVs = false;
            for(SvVarData var : cluster.getSVs())
            {
                if((var.type() == DEL && var.length() >= longDelThreshold) || (var.type() == DUP && var.length() >= longDupThreshold))
                {
                    hasLongSVs = true;
                    break;
                }
            }

            if(cluster.getSvCount() == 1)
            {
                StructuralVariantType type = cluster.getSV(0).type();

                if(type == DEL)
                    cluster.setResolved(!hasLongSVs, ResolvedType.DEL);
                else if(type == DUP)
                    cluster.setResolved(!hasLongSVs, ResolvedType.DUP);
                else if(type == INS)
                    cluster.setResolved(!hasLongSVs, ResolvedType.INS);
            }
            else
            {
                cluster.setResolved(!hasLongSVs, SIMPLE_GRP);
            }

            return;
        }

        // markSyntheticTypes(cluster, isFinal, longDelThreshold, longDupThreshold, proximityThreshold);
        if(cluster.getSglBreakendCount() == 0)
        {
            classifyPairClusters(cluster, longDelThreshold, longDupThreshold);
        }
        else
        {
            // to be reworked once new inferred changes are complete
            if (cluster.getSvCount() == 2 && cluster.isConsistent() && cluster.getSglBreakendCount() == 2)
            {
                final SvVarData sgl1 = cluster.getSV(0);
                final SvVarData sgl2 = cluster.getSV(1);
                ResolvedType resolvedType = markSinglePairResolvedType(sgl1, sgl2);

                if(resolvedType != NONE)
                {
                    cluster.setResolved(true, resolvedType);
                }
            }
        }

        if(cluster.getResolvedType() != NONE)
            return;

        if(isFinal)
        {
            if(cluster.getSvCount() == 1)
            {
                StructuralVariantType type = cluster.getSV(0).type();

                if(type == BND)
                    cluster.setResolved(false, UNBAL_TRANS);
                else if(type == INV)
                    cluster.setResolved(false, ResolvedType.INV);
                else if(type == INF)
                    cluster.setResolved(false, ResolvedType.INF);
                else if(type == SGL)
                    cluster.setResolved(false, ResolvedType.SGL);

                return;
            }

            markSyntheticIncompletes(cluster);

            if(cluster.getResolvedType() != NONE)
                return;

            if(cluster.getSvCount() == 2)
            {
                cluster.setResolved(false, PAIR_OTHER);
                return;
            }

            cluster.setResolved(false, COMPLEX);
        }
    }

    private static boolean smallSimpleSVs(final SvCluster cluster)
    {
        if(cluster.getSvCount() > SMALL_CLUSTER_SIZE)
            return false;

        for(final SvVarData var : cluster.getSVs())
        {
            if(!var.isSimpleType())
                return false;
        }

        return true;
    }

    public static long getSyntheticLength(final SvCluster cluster)
    {
        // assumes has been called on an appropriate cluster

        if(cluster.getResolvedType() == SGL_PAIR_DEL || cluster.getResolvedType() == SGL_PAIR_DUP)
        {
            return abs(cluster.getSV(0).position(true) - cluster.getSV(1).position(true));
        }
        else
        {
            if(cluster.getChains().size() != 1)
                return NO_LENGTH;

            final SvChain chain = cluster.getChains().get(0);
            return abs(chain.getOpenBreakend(true).position() - chain.getOpenBreakend(false).position());
        }
    }

    public static long getSyntheticTiLength(final SvCluster cluster)
    {
        if(cluster.getChains().size() != 1)
            return NO_LENGTH;

        final SvChain chain = cluster.getChains().get(0);

        return chain.getLinkedPairs().stream().mapToLong(x -> x.length()).max().getAsLong();
    }

    public static long getSyntheticGapLength(final SvCluster cluster)
    {
        // the min distance between the longest TI and the chain ends, if has a TI on the same end
        if(cluster.getChains().size() != 1)
            return NO_LENGTH;

        final SvChain chain = cluster.getChains().get(0);
        final SvBreakend chainStart = chain.getOpenBreakend(true);
        final SvBreakend chainEnd = chain.getOpenBreakend(false);

        if(!chainStart.getChrArm().equals(chainEnd.getChrArm()))
            return NO_LENGTH;

        long minDistance = NO_LENGTH;

        for(final SvLinkedPair pair : chain.getLinkedPairs())
        {
            if(pair.length() > SHORT_TI_LENGTH)
            {
                if(!pair.chromosome().equals(chainStart.chromosome()))
                    return NO_LENGTH;
            }
            else if(!pair.chromosome().equals(chainStart.chromosome()))
            {
                continue;
            }

            long distanceStart = min(
                    abs(chainStart.position() - pair.firstBreakend().position()),
                    abs(chainStart.position() - pair.secondBreakend().position()));

            long distanceEnd = min(
                    abs(chainEnd.position() - pair.firstBreakend().position()),
                    abs(chainEnd.position() - pair.secondBreakend().position()));

            long gapLength = min(distanceStart, distanceEnd);

            if(minDistance == NO_LENGTH || gapLength < minDistance)
                minDistance = gapLength;
        }

        return minDistance;
    }

    public static void markSyntheticIncompletes(SvCluster cluster)
    {
        // look for chains of short TIs which when reduced form a SGL, INF INV or unbalanced TRANS (ie a BND)
        if (cluster.getSvCount() > 5 || !cluster.isFullyChained(false) || cluster.getChains().size() != 1)
            return;

        SvChain chain = cluster.getChains().get(0);

        // test the chain for short TIs only
        int totalChainLength = 0;
        int longTICount = 0;
        long longestTILength = 0;
        for (SvLinkedPair pair : chain.getLinkedPairs())
        {
            if (pair.length() > SHORT_TI_LENGTH)
                return;

            longestTILength = max(pair.length(), longestTILength);
            totalChainLength += pair.length();
        }

        // first look for chains ending in SGLs or INFs
        ResolvedType resolvedType = NONE;

        if(chain.getLastSV().type() == INF || chain.getFirstSV().type() == INF)
        {
            resolvedType = ResolvedType.INF;
        }
        else if(chain.getLastSV().type() == SGL || chain.getFirstSV().type() == SGL)
        {
            resolvedType = ResolvedType.SGL;
        }
        else
        {
            final SvBreakend startBreakend = chain.getOpenBreakend(true);
            final SvBreakend endBreakend = chain.getOpenBreakend(false);

            if (!startBreakend.chromosome().equals(endBreakend.chromosome()) || startBreakend.arm() != endBreakend.arm())
            {
                resolvedType = UNBAL_TRANS;
            }
            else
            {
                resolvedType = ResolvedType.INV;
            }
        }

        if (resolvedType == NONE)
            return;

        LOGGER.debug("cluster({}) chain(links=({} len={} tiLen({}) marked as {}",
                cluster.id(), chain.getLinkCount(), totalChainLength, longestTILength, resolvedType);

        cluster.setResolved(false, resolvedType);
    }

    public static ResolvedType markSinglePairResolvedType(final SvVarData sgl1, final SvVarData sgl2)
    {
        if(sgl1.sglToCentromereOrTelomere() || sgl2.sglToCentromereOrTelomere())
            return NONE;

        if(sgl1.isInferredSgl() && sgl2.isInferredSgl())
            return NONE;

        final SvBreakend breakend1 = sgl1.getBreakend(true);
        final SvBreakend breakend2 = sgl2.getBreakend(true);

        // to form a simple del or dup, they need to have different orientations
        if(breakend1.orientation() == breakend2.orientation())
            return NONE;

        // check copy number consistency
        if(!ploidyMatch(sgl1, sgl2))
            return NONE;

        boolean breakendsFace = (breakend1.position() < breakend2.position() && breakend1.orientation() == -1)
                || (breakend2.position() < breakend1.position() && breakend2.orientation() == -1);

        long length = abs(breakend1.position() - breakend2.position());

        ResolvedType resolvedType = NONE;

        if(breakendsFace)
        {
            // a DUP if breakends are further than the anchor distance away, else an INS
            int minTiLength = getMinTemplatedInsertionLength(breakend1, breakend2);
            if(length >= minTiLength)
                resolvedType = SGL_PAIR_DUP;
            else
                resolvedType = SGL_PAIR_INS;
        }
        else
        {
            // a DEL if the breakends are further than the min DEL length, else an INS
            if(length >= MIN_DEL_LENGTH)
                resolvedType = SGL_PAIR_DEL;
            else
                resolvedType = SGL_PAIR_INS;
        }

        // mark these differently from those formed from normal SVs
        return resolvedType;
    }


}
