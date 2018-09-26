package com.hartwig.hmftools.svanalysis.analysis;

import static java.lang.Math.round;

import static com.hartwig.hmftools.svanalysis.analysis.ClusterAnalyser.reduceInferredToShortestLinks;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.svanalysis.types.SvChain;
import com.hartwig.hmftools.svanalysis.types.SvClusterData;
import com.hartwig.hmftools.svanalysis.types.SvLinkedPair;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChainFinder
{
    private static double MIN_CHAIN_PERCENT = 0.6;

    private static final Logger LOGGER = LogManager.getLogger(ChainFinder.class);

    public static boolean assessClusterChaining(final String sampleId, SvCluster cluster, List<SvLinkedPair> assemblyLinkedPairs, List<SvLinkedPair> inferredLinkedPairs)
    {
        // take the assembly links as a given and then try out the inferred links to see if a single chain can be formed from all the breakends

        // check whether there are enough potential links to form full or near-to-full chains
        if(!checkLinksPotential(cluster, assemblyLinkedPairs, inferredLinkedPairs))
            return false;

        int reqInferredLinks = cluster.getCount() - assemblyLinkedPairs.size();

        // for now if there are too many potential inferred links, cull the the list first
        if(reqInferredLinks > 10 && inferredLinkedPairs.size() > 10)
        {
            reduceInferredToShortestLinks(inferredLinkedPairs);

            if(!checkLinksPotential(cluster, assemblyLinkedPairs, inferredLinkedPairs))
                return false;
        }

        boolean hasChains = formChains(sampleId, cluster, assemblyLinkedPairs, inferredLinkedPairs);

        return hasChains;
    }

    private static boolean checkLinksPotential(SvCluster cluster, List<SvLinkedPair> assemblyLinkedPairs, List<SvLinkedPair> inferredLinkedPairs)
    {
        int minRequiredLinks = (int)round((cluster.getCount() - 1) * MIN_CHAIN_PERCENT);

        if(assemblyLinkedPairs.size() + inferredLinkedPairs.size() < minRequiredLinks)
            return false;

        // count up unique linked-pair breakends to see whether there is the potential to form sufficiently long chains
        int matchedVarCount = 0;
        List<SvLinkedPair> allLinks = Lists.newArrayList();
        allLinks.addAll(assemblyLinkedPairs);
        allLinks.addAll(inferredLinkedPairs);

        for(final SvClusterData var : cluster.getSVs())
        {
            boolean startMatched = false;
            boolean endMatched = false;

            for (final SvLinkedPair linkedPair : allLinks)
            {
                if(linkedPair.hasVariantBE(var, true))
                    startMatched = true;
                else if(linkedPair.hasVariantBE(var, false))
                    endMatched = true;

                if(startMatched && endMatched)
                {
                    ++matchedVarCount;
                    break;
                }
            }
        }

        return matchedVarCount >= minRequiredLinks;
    }

    private static boolean formChains(final String sampleId, SvCluster cluster, List<SvLinkedPair> assemblyLinkedPairs, List<SvLinkedPair> inferredLinkedPairs)
    {
        // find all the combinations of linked pairs where every breakend end except at most 2 are covered by a linked pair
        List<SvLinkedPair> trialLinkedPairs = Lists.newArrayList();
        trialLinkedPairs.addAll(assemblyLinkedPairs);

        List<SvChain> completeChains = Lists.newArrayList();
        List<SvChain> partialChains = Lists.newArrayList();

        // first form any partial chains out of the assembly linked pairs
        for(SvLinkedPair linkedPair : assemblyLinkedPairs)
        {
            addLinkToChains(linkedPair, partialChains);
        }

        int clusterSvCount = cluster.getCount();

        if(!assemblyLinkedPairs.isEmpty() && cluster.getCount() > 4)
        {
            LOGGER.debug("sample({}) cluster({}) assemblyLinks({}) inferredLinks({}) svCount({}))",
                    sampleId, cluster.getId(), assemblyLinkedPairs.size(), inferredLinkedPairs.size(), clusterSvCount);
        }

        findCompletedLinks(clusterSvCount, assemblyLinkedPairs, assemblyLinkedPairs, partialChains, inferredLinkedPairs, 0, completeChains);

        if(completeChains.isEmpty())
            return false;

        List<SvClusterData> chainedSVs = Lists.newArrayList();

        // first check for any complete chains, and if found, add the shortest one
        int shortestLength = -1;
        SvChain shortestFullChain = null;
        for(SvChain chain : completeChains)
        {
            if(chain.getLinkCount() < 2 || chain.getSvCount() != clusterSvCount)
                continue; // only consider reciprocal chains if this short

            chain.recalcLength();

            if(shortestFullChain == null || chain.getLength() < shortestLength)
            {
                shortestFullChain = chain;
                shortestLength = chain.getLength();
            }
        }

        if(shortestFullChain != null)
        {
            shortestFullChain.setId(cluster.getChains().size());

            LOGGER.info("sample({}) cluster({}) adding complete chain({}) length({}) with {} linked pairs:",
                    sampleId, cluster.getId(), shortestFullChain.getId(), shortestFullChain.getLength(), shortestFullChain.getLinkCount());

            shortestFullChain.logLinks();

            cluster.addChain(shortestFullChain);
            chainedSVs.addAll(shortestFullChain.getSvList());
            return true;
        }

        // otherwise add the longest mutually exclusive chains
        while(!completeChains.isEmpty())
        {
            int maxSvLength = -1;
            SvChain maxLengthChain = null;

            for (SvChain chain : completeChains)
            {
                // cannot contain existing chained SV
                boolean hasExistingSVs = false;

                if(!chainedSVs.isEmpty())
                {
                    for (final SvClusterData var : chain.getSvList())
                    {
                        if (chainedSVs.contains(var))
                        {
                            hasExistingSVs = true;
                            break;
                        }
                    }
                }

                if(hasExistingSVs)
                    continue;

                if(chain.getSvCount() > maxSvLength)
                {
                    maxSvLength = chain.getSvCount();
                    maxLengthChain = chain;
                }
            }

            if(maxLengthChain == null)
                break;

            maxLengthChain.setId(cluster.getChains().size());

            LOGGER.info("sample({}) cluster({}) adding incomplete chain({}) length({}) with {} linked pairs",
                    sampleId, cluster.getId(), maxLengthChain.getId(), maxLengthChain.getLength(), maxLengthChain.getLinkCount());

            maxLengthChain.logLinks();

            cluster.addChain(maxLengthChain);
            chainedSVs.addAll(maxLengthChain.getSvList());
            completeChains.remove(maxLengthChain);
        }

        return false;
    }

    private static boolean findCompletedLinks(
            int maxSvCount, final List<SvLinkedPair> requiredLinks, final List<SvLinkedPair> existingLinks,
            final List<SvChain> partialChains, List<SvLinkedPair> testLinks, int currentIndex, List<SvChain> completeChains)
    {
        if(currentIndex >= testLinks.size())
            return false;

        LOGGER.debug("testLinkIndex({}) existingLinks({}) completedChains({}) partialChains({}) maxSVCount({})",
                currentIndex, existingLinks.size(), completeChains.size(), partialChains.size(), maxSvCount);

        boolean linksAdded = false;

        // try adding each of the remaining links recursively to get a set of completely-links breakends
        // and if so, add this collection of potential chains
        for(int i = currentIndex; i < testLinks.size(); ++i)
        {
            final SvLinkedPair testLink = testLinks.get(i);

            // check this ne test link against both the existing links and the existing chains
            if(!canAddLinkedPair(existingLinks, partialChains, testLink, maxSvCount))
                continue;

            linksAdded = true;

            // must be a deep copy to avoid changing the set of partial chains at this level of the search
            List<SvChain> workingChains = Lists.newArrayList();
            for(final SvChain chain : partialChains)
            {
                workingChains.add(new SvChain(chain));
            }

            // add this link to any chains if possible
            addLinkToChains(testLink, workingChains);

            // every new SV is now part of a partial chain (even if only a single link)
            List<SvLinkedPair> workingLinks = Lists.newArrayList();
            workingLinks.addAll(existingLinks);
            workingLinks.add(testLink);

            // reconcile chains together if possible
            if(workingChains.size() > 1)
            {
                for (int firstIndex = 0; firstIndex < workingChains.size(); ++firstIndex)
                {
                    final SvChain firstChain = workingChains.get(firstIndex);

                    if(firstChain.getSvCount() == maxSvCount || firstChain.isClosedLoop())
                        continue;

                    int nextIndex = firstIndex + 1;
                    while (nextIndex < workingChains.size())
                    {
                        final SvChain nextChain = workingChains.get(nextIndex);

                        if(nextChain.getSvCount() == maxSvCount || nextChain.isClosedLoop())
                        {
                            ++nextIndex;
                            continue;
                        }

                        if (reconcileChains(firstChain, nextChain))
                        {
                            workingChains.remove(nextIndex);
                        }
                        else
                        {
                            ++nextIndex;
                        }
                    }
                }
            }

            // and check whether any chains are complete
            int chainIndex = 0;
            while(chainIndex < workingChains.size())
            {
                final SvChain chain = workingChains.get(chainIndex);

                if(chain.getSvCount() < maxSvCount)
                {
                    ++chainIndex;
                    continue;
                }

                boolean isDuplicate = false;
                for(final SvChain completeChain : completeChains)
                {
                    if(completeChain.isIdentical(chain))
                    {
                        isDuplicate = true;

                        LOGGER.debug("skipping duplicate complete chain:");
                        chain.logLinks();
                        break;
                    }
                }

                boolean hasRequiredLinks = chain.hasLinks(requiredLinks);

                if(!isDuplicate && hasRequiredLinks)
                {
                    chain.setId(completeChains.size());
                    completeChains.add(chain);

                    LOGGER.debug("iters({} of {}) testLinkIndex({}) existingLinks({}) completedChains({}) workingChains({}) - adding complete chain({} len={}):",
                            i, testLinks.size(), currentIndex, existingLinks.size(), completeChains.size(), workingChains.size(), chain.getId(), chain.getSvCount());

                    // LOGGER.debug("added complete potential chain:");
                    chain.logLinks();
                }

                workingChains.remove(chainIndex);
            }

            if(workingChains.isEmpty())
            {
                // this search path is done so no need to keep searching for more links to add
                break;
            }

            LOGGER.debug("iters({} of {}) testLinkIndex({}) existingLinks({}) completedChains({}) workingChains({}) - continuing search",
                    i, testLinks.size(), currentIndex, existingLinks.size(), completeChains.size(), workingChains.size());

            // continue the search, moving on to try adding the next test link
            boolean hasMoreTestLinks = findCompletedLinks(maxSvCount, requiredLinks, workingLinks, workingChains, testLinks, i + 1, completeChains);

            if(!hasMoreTestLinks)
            {
                // add any sufficiently long chains since this search path has been exhausted
                for(final SvChain chain : workingChains)
                {
                    double lengthPerc = chain.getSvCount() / (double)maxSvCount;

                    if(lengthPerc >= MIN_CHAIN_PERCENT)
                    {
                        chain.setId(completeChains.size());
                        completeChains.add(chain);

                        LOGGER.debug("iters({} of {}) testLinkIndex({}) existingLinks({}) completedChains({}) workingChains({}) - adding incomplete chain({} len={})",
                                i, testLinks.size(), currentIndex, existingLinks.size(), completeChains.size(), workingChains.size(), chain.getId(), chain.getSvCount());
                    }
                }
            }
        }

        return linksAdded;
    }

    private static void addLinkToChains(SvLinkedPair linkedPair, List<SvChain> chains)
    {
        // add to an existing chain or create a new one
        for(final SvChain chain : chains)
        {
            if(chain.canAddLinkedPairToStart(linkedPair))
            {
                chain.addLink(linkedPair, true);
                return;
            }

            if(chain.canAddLinkedPairToEnd(linkedPair))
            {
                chain.addLink(linkedPair, false);
                return;
            }
        }

        SvChain newChain = new SvChain(0);
        newChain.addLink(linkedPair, true);
        chains.add(newChain);
    }

    private static boolean reconcileChains(SvChain firstChain, SvChain nextChain)
    {
        boolean canAddToStart = firstChain.canAddLinkedPairToStart(nextChain.getFirstLinkedPair());
        boolean canAddToEnd = firstChain.canAddLinkedPairToEnd(nextChain.getFirstLinkedPair());

        if(canAddToStart || canAddToEnd)
        {
            for(SvLinkedPair linkedPair : nextChain.getLinkedPairs())
            {
                firstChain.addLink(linkedPair, canAddToStart);
            }

            return true;
        }

        canAddToStart = firstChain.canAddLinkedPairToStart(nextChain.getLastLinkedPair());
        canAddToEnd = firstChain.canAddLinkedPairToEnd(nextChain.getLastLinkedPair());

        if(canAddToStart || canAddToEnd)
        {
            // add in reverse
            for(int index = nextChain.getLinkedPairs().size() - 1; index >= 0; --index)
            {
                SvLinkedPair linkedPair = nextChain.getLinkedPairs().get(index);
                firstChain.addLink(linkedPair, canAddToStart);
            }

            return true;
        }

        return false;
    }

    private static boolean canAddLinkedPair(final List<SvLinkedPair> existingLinks, final List<SvChain> partialChains, final SvLinkedPair testLink, int requiredSvCount)
    {
        // first check that the breakends aren't already used
        for(SvLinkedPair linkedPair : existingLinks)
        {
            if (linkedPair.hasLinkClash(testLink))
                return false;
        }

        // then check that this linked pair doesn't close a chain of a smaller size than one involving all required SVs
        for(final SvChain chain : partialChains)
        {
            boolean linkCouldCloseChain = chain.linkWouldCloseChain(testLink);

            if(!linkCouldCloseChain)
                continue;

            // work out whether all SVs would be accounted for
            int uniqueSVCount = chain.getSvCount();

            if(!chain.getSvList().contains(testLink.first()))
                ++uniqueSVCount;

            if(!chain.getSvList().contains(testLink.second()))
                ++uniqueSVCount;

            if(uniqueSVCount < requiredSvCount)
            {
                // closing this chain would make it too short
                return false;
            }
            else
            {
                // this would be a looped chain involving all required SVs
                return true;
            }
        }

        return true;
    }

    // old methods which don't use assembly info
    public void findSvChains(final String sampleId, SvCluster cluster)
    {
        if(!cluster.getChains().isEmpty())
            return;

        // findSvChains(sampleId, cluster, cluster.getLinkedPairs(), cluster.getChains());
    }

    public void findSvChains(final String sampleId, SvCluster cluster, List<SvLinkedPair> linkedPairs, List<SvChain> chainsList)
    {
        if(linkedPairs.isEmpty() || linkedPairs.size() < 2)
            return;

        List<SvLinkedPair> workingLinkedPairs = Lists.newArrayList();
        workingLinkedPairs.addAll(linkedPairs);

        LOGGER.debug("cluster({}) attempting to find chained SVs from {} linked pairs", cluster.getId(), workingLinkedPairs.size());

        while(workingLinkedPairs.size() >= 2)
        {
            // start with a single linked pair
            // for each of its ends (where the first BE is labelled 'first', and the second labelled 'last'),
            // search for the closest possible linking BE from another linked pair
            // for BEs to link they must be facing (like a TI)

            SvLinkedPair linkedPair = workingLinkedPairs.get(0);
            workingLinkedPairs.remove(0);

            int chainId = chainsList.size()+1;
            SvChain currentChain = new SvChain(chainId);

            LOGGER.debug("sample({}) cluster({}) starting chain({}) with linked pair({})",
                    sampleId, cluster.getId(), chainId, linkedPair.toString());

            currentChain.addLink(linkedPair, true);

            while(!workingLinkedPairs.isEmpty())
            {
                // now search the remaining SVs for links at either end of the current chain
                SvClusterData beFirst = currentChain.getFirstSV();
                boolean chainFirstUnlinkedOnStart = currentChain.firstLinkOpenOnStart();
                SvClusterData beLast = currentChain.getLastSV();
                boolean chainLastUnlinkedOnStart = currentChain.lastLinkOpenOnStart();

                SvLinkedPair closestStartPair = null;
                int closestStartLen = -1;

                SvLinkedPair closestLastPair = null;
                int closestLastLen = -1;

                for(SvLinkedPair pair : workingLinkedPairs) {

                    // first check for a linked pair which has the same variant (but the other BE) to the unlinked on
                    if((beFirst.equals(pair.first()) && chainFirstUnlinkedOnStart == pair.firstLinkOnStart())
                            || (beFirst.equals(pair.second()) && chainFirstUnlinkedOnStart == pair.secondLinkOnStart()))
                    {
                        closestStartPair = pair;
                        closestStartLen = 0; // to prevent another match
                    }

                    if((beLast.equals(pair.first()) && chainLastUnlinkedOnStart == pair.firstLinkOnStart())
                            || (beLast.equals(pair.second()) && chainLastUnlinkedOnStart == pair.secondLinkOnStart()))
                    {
                        closestLastPair = pair;
                        closestLastLen = 0; // to prevent another match
                    }
                }

                if(closestStartPair == null && closestLastPair == null)
                {
                    break;
                }

                if(closestStartPair != null)
                {
                    LOGGER.debug("adding linked pair({}) on chain start({}) with length({})",
                            closestStartPair.toString(), beFirst.posId(chainFirstUnlinkedOnStart), closestStartLen);

                    // add this to the chain at the start
                    currentChain.addLink(closestStartPair, true);
                    workingLinkedPairs.remove(closestStartPair);
                }

                if(closestLastPair != null & closestLastPair != closestStartPair)
                {
                    LOGGER.debug("adding linked pair({}) on chain end({}) with length({})",
                            closestLastPair.toString(), beLast.posId(chainLastUnlinkedOnStart), closestLastLen);

                    // add this to the chain at the start
                    currentChain.addLink(closestLastPair, false);
                    workingLinkedPairs.remove(closestLastPair);
                }
            }

            if(currentChain.getLinkCount() > 1)
            {
                LOGGER.debug("sample({}) cluster({}) adding chain({}) with {} linked pairs:",
                        sampleId, cluster.getId(), currentChain.getId(), currentChain.getLinkCount());

                chainsList.add(currentChain);

                for(int i = 0; i < currentChain.getLinkCount(); ++i)
                {
                    final SvLinkedPair pair = currentChain.getLinkedPairs().get(i);
                    LOGGER.debug("sample({}) cluster({}) chain({}) {}: pair({}) {} len={}",
                            sampleId, cluster.getId(), currentChain.getId(), i, pair.toString(), pair.linkType(), pair.length());
                }
            }
        }
    }


}
