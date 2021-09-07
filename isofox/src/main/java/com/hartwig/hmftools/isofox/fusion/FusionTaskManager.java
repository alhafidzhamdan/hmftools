package com.hartwig.hmftools.isofox.fusion;

import static java.lang.Math.max;

import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_END;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_START;
import static com.hartwig.hmftools.isofox.IsofoxConfig.ISF_LOGGER;
import static com.hartwig.hmftools.isofox.fusion.FusionConfig.LOG_READ_ID;
import static com.hartwig.hmftools.isofox.fusion.FusionUtils.formChromosomePair;
import static com.hartwig.hmftools.isofox.fusion.ReadGroup.mergeChimericReadMaps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache;
import com.hartwig.hmftools.common.utils.PerformanceCounter;
import com.hartwig.hmftools.isofox.IsofoxConfig;
import com.hartwig.hmftools.isofox.common.ReadRecord;
import com.hartwig.hmftools.common.utils.TaskExecutor;

public class FusionTaskManager
{
    private final IsofoxConfig mConfig;
    private final EnsemblDataCache mGeneTransCache;

    private final List<FusionFinder> mFusionTasks;
    private final FusionWriter mFusionWriter;
    private final FusionGeneFilters mGeneFilters;

    private final Map<String,List<FusionFragment>> mRealignCandidateMap;
    private final Map<String,Map<String,ReadGroup>> mIncompleteReadGroups; // keyed by chromosome then readId

    private final PerformanceCounter mPerfCounter;

    public FusionTaskManager(final IsofoxConfig config, final EnsemblDataCache geneTransCache)
    {
        mConfig = config;
        mGeneTransCache = geneTransCache;

        mFusionTasks = Lists.newArrayList();

        mGeneFilters = new FusionGeneFilters(config, geneTransCache);

        mRealignCandidateMap = Maps.newHashMap();
        mIncompleteReadGroups = Maps.newHashMap();

        mPerfCounter = new PerformanceCounter("Fusions");
        mFusionWriter = new FusionWriter(mConfig);
    }

    public FusionFinder createFusionFinder(final String id)
    {
        return new FusionFinder(id, mConfig, mGeneTransCache, mGeneFilters, mFusionWriter);
    }

    public synchronized List<ReadGroup> addIncompleteReadGroup(
            final String chromosome, final Map<String,Map<String,ReadGroup>> chrIncompleteGroups,
            final Map<String,List<FusionFragment>> racFragments)
    {
        int prevIncomplete = mIncompleteReadGroups.values().stream().mapToInt(x -> x.size()).sum();

        List<ReadGroup> completeGroups = Lists.newArrayList();

        for(Map.Entry<String,Map<String,ReadGroup>> entry : chrIncompleteGroups.entrySet())
        {
            String otherChromosome = entry.getKey();
            Map<String,ReadGroup> newGroups = entry.getValue();

            Map<String,ReadGroup> existingGroups = mIncompleteReadGroups.get(otherChromosome);

            if(existingGroups == null)
            {
                Map<String,ReadGroup> chromosomeGroups = mIncompleteReadGroups.get(chromosome);
                if(chromosomeGroups == null)
                {
                    chromosomeGroups = Maps.newHashMap();
                    mIncompleteReadGroups.put(chromosome, chromosomeGroups);
                }

                chromosomeGroups.putAll(newGroups);

                ISF_LOGGER.debug("added chromosomes({} & {}) newGroups({})",
                        chromosome, otherChromosome, newGroups.size());
            }
            else
            {
                mergeChimericReadMaps(existingGroups, completeGroups, newGroups);

                ISF_LOGGER.debug("combined chromosomes({} & {}) existing({}) new({}) complete({})",
                        chromosome, otherChromosome, existingGroups.size(), newGroups.size(), completeGroups.size());
            }
        }

        mRealignCandidateMap.putAll(racFragments);
        int totalRacFrags = mRealignCandidateMap.values().stream().mapToInt(x -> x.size()).sum();
        int newRacFrags = racFragments.values().stream().mapToInt(x -> x.size()).sum();
        int newIncomplete = mIncompleteReadGroups.values().stream().mapToInt(x -> x.size()).sum();

        // exclude duplicate reads now that group is known (since not all reads are marked as duplicates)
        completeGroups = completeGroups.stream().filter(x -> !x.isDuplicate()).collect(Collectors.toList());

        ISF_LOGGER.info("chr({}) chimeric groups(partial={} complete={}) total incomplete({} -> {}) racFrags({} new={})",
                chromosome, chrIncompleteGroups.size(), completeGroups.size(), prevIncomplete, newIncomplete,
                totalRacFrags, newRacFrags);

        return completeGroups;
    }

    public synchronized final Map<String,List<FusionFragment>> getRealignCandidateMap() { return mRealignCandidateMap; }

    public void close()
    {
        long unfusedRacFrags = mRealignCandidateMap.values().stream()
                .mapToLong(x -> x.stream().filter(y -> y.assignedFusions().isEmpty()).count()).sum();

        int incompleteGroups = mIncompleteReadGroups.values().stream().mapToInt(x -> x.size()).sum();

        ISF_LOGGER.info("all fusion tasks complete - unfused RAC frags({}) incompleteGroups({})", unfusedRacFrags, incompleteGroups);

        // write any unassigned RAC fragments
        mFusionWriter.writeUnfusedFragments(mRealignCandidateMap);
        mFusionWriter.close();
    }

    private static final int LOG_COUNT = 100000;

    public void processCachedFragments(final List<ReadGroup> readGroups)
    {
        // convert any set of valid reads into a fragment, and then process these in groups by chromosomal pair
        ISF_LOGGER.info("processing {} chimeric read groups", readGroups.size());

        mPerfCounter.start();

        int invalidFragments = 0;
        int skipped = 0;
        int fragments = 0;
        int missingSuppReads = 0;

        int readGroupCount = 0;
        int nextLog = LOG_COUNT;

        final Map<String,List<FusionFragment>> chrPairFragments = Maps.newHashMap();

        for(ReadGroup readGroup : readGroups)
        {
            ++readGroupCount;

            if(readGroupCount >= nextLog)
            {
                nextLog += LOG_COUNT;
                ISF_LOGGER.info("processed {} chimeric read groups", readGroupCount);
            }

            boolean isComplete = readGroup.isComplete();
            final List<ReadRecord> reads = readGroup.Reads;

            if(reads.get(0).Id.equals(LOG_READ_ID))
            {
                ISF_LOGGER.debug("specific read: {}", reads.get(0));
            }

            if(reads.stream().anyMatch(x -> mGeneFilters.skipRead(x.mateChromosome(), x.mateStartPosition())))
            {
                ++skipped;
                continue;
            }

            if(!isComplete)
            {
                if(readGroup.hasSuppAlignment())
                {
                    if(skipMissingReads(reads))
                    {
                        ++skipped;
                        continue;
                    }

                    ++missingSuppReads;
                }

                ++invalidFragments;
            }
            else
            {
                FusionFragment fragment = new FusionFragment(readGroup);

                if(fragment.type() == FusionFragmentType.UNKNOWN)
                {
                    ++invalidFragments;
                    continue;
                }

                ++fragments;

                final String chrPair = formChromosomePair(fragment.chromosomes()[SE_START], fragment.chromosomes()[SE_END]);
                List<FusionFragment> fragmentList = chrPairFragments.get(chrPair);

                if(fragmentList == null)
                {
                    chrPairFragments.put(chrPair, Lists.newArrayList(fragment));
                }
                else
                {
                    fragmentList.add(fragment);
                }
            }
        }

        // allocate fusion pairs evenly amongst threads (if multi-thread)
        mFusionTasks.clear();

        for(int taskId = 0; taskId < max(mConfig.Threads, 1); ++taskId)
        {
            mFusionTasks.add(createFusionFinder(String.valueOf(taskId)));
        }

        for(List<FusionFragment> chrPairFrags : chrPairFragments.values())
        {
            // allocate the next chr-pair fragment batch to the task with the least
            FusionFinder leastAllocated = null;
            int minAllocated = 0;

            for(FusionFinder fusionTask : mFusionTasks)
            {
                if(minAllocated == 0 || fusionTask.getFragments().size() < minAllocated)
                {
                    leastAllocated = fusionTask;
                    minAllocated = fusionTask.getFragments().size();
                    if(minAllocated == 0)
                        break;
                }
            }

            leastAllocated.getFragments().addAll(chrPairFrags);
        }

        ISF_LOGGER.info("chimeric groups({} skipped={} invalid={} miss={} candidates={}) chrPairs({}) tasks({})",
                readGroups.size(), skipped, invalidFragments, missingSuppReads, fragments,
                chrPairFragments.size(), mFusionTasks.size());

        if(mFusionTasks.isEmpty())
        {
            ISF_LOGGER.warn("no fusion tasks created");
            return;
        }
        else
        {
            final List<Callable> callableList = mFusionTasks.stream().collect(Collectors.toList());
            TaskExecutor.executeTasks(callableList, mConfig.Threads);
            logPerformanceStats();
        }

        mFusionWriter.close();

        mPerfCounter.stop();

        if(mConfig.Fusions.PerformanceStats)
            mPerfCounter.logStats();

        ISF_LOGGER.info("fusion calling complete");
    }

    private boolean skipMissingReads(final List<ReadRecord> reads)
    {
        for(final ReadRecord read : reads)
        {
            if(read.hasSuppAlignment())
            {
                SupplementaryReadData suppData = SupplementaryReadData.from(read.getSuppAlignment());

                if(mGeneFilters.skipRead(suppData.Chromosome, suppData.Position))
                    return true;

                ISF_LOGGER.debug("read({}) missing supp({})", read, suppData);
            }
        }

        return false;
    }

    private void logPerformanceStats()
    {
        if(!mConfig.Fusions.PerformanceStats)
            return;

        if(!ISF_LOGGER.isDebugEnabled() && mConfig.Functions.size() > 1)
            return;

        final PerformanceCounter perfCounter = mFusionTasks.get(0).getPerfCounter();

        for (int i = 1; i < mFusionTasks.size(); ++i)
        {
            perfCounter.merge(mFusionTasks.get(i).getPerfCounter());
        }

        perfCounter.logStats();
    }
}
