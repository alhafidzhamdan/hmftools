package com.hartwig.hmftools.purple.somatic;

import static com.hartwig.hmftools.common.variant.VariantHeader.REPORTED_FLAG;
import static com.hartwig.hmftools.purple.PurpleCommon.PPL_LOGGER;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalog;
import com.hartwig.hmftools.common.drivercatalog.SomaticVariantDrivers;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanel;
import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.purple.PurityAdjuster;
import com.hartwig.hmftools.common.purple.copynumber.PurpleCopyNumber;
import com.hartwig.hmftools.common.purple.gene.GeneCopyNumber;
import com.hartwig.hmftools.common.purple.region.FittedRegion;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.common.variant.SomaticVariantFactory;
import com.hartwig.hmftools.common.variant.VariantContextDecorator;
import com.hartwig.hmftools.common.variant.VariantType;
import com.hartwig.hmftools.purple.config.ReferenceData;
import com.hartwig.hmftools.purple.fitting.PeakModel;
import com.hartwig.hmftools.common.variant.msi.MicrosatelliteStatus;
import com.hartwig.hmftools.common.variant.tml.TumorMutationalStatus;
import com.hartwig.hmftools.purple.config.PurpleConfig;
import com.hartwig.hmftools.purple.plot.RChartData;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;

public class SomaticStream implements Consumer<VariantContext>
{
    private final ReferenceData mReferenceData;
    private final PurpleConfig mConfig;

    private final String mInputVCF;
    private boolean mEnabled;
    private final String mOutputVCF;
    private final TumorMutationalLoad mTumorMutationalLoad;
    private final MicrosatelliteIndels mMicrosatelliteIndels;
    private final SomaticVariantDrivers mDrivers;
    private final SomaticVariantFactory mSomaticVariantFactory;
    private final RChartData mRChartData;
    private final DriverGenePanel mGenePanel;
    private final List<PeakModel> mPeakModel;
    private final Set<String> mReportedGenes;

    private final List<VariantContextDecorator> mDownsampledVariants; // cached for charting
    private final int mSnpMod;
    private final int mIndelMod;
    private int mSnpCount;
    private int mIndelCount;

    private VariantContextWriter mVcfWriter;

    private static final int CHART_DOWNSAMPLE_FACTOR = 25000; // eg for 50K variants, only every second will be kept for plotting

    public SomaticStream(
            final PurpleConfig config, final ReferenceData referenceData,
            int snpCount, int indelCount, final List<PeakModel> peakModel, final String somaticVcfFilename)
    {
        mReferenceData = referenceData;
        mConfig = config;

        mGenePanel = referenceData.GenePanel;
        mPeakModel = peakModel;
        mOutputVCF = config.OutputDir + config.TumorId + ".purple.somatic.vcf.gz";
        mEnabled = !somaticVcfFilename.isEmpty();
        mInputVCF = somaticVcfFilename;
        mTumorMutationalLoad = new TumorMutationalLoad();
        mMicrosatelliteIndels = new MicrosatelliteIndels();
        mDrivers = new SomaticVariantDrivers(mGenePanel);
        mSomaticVariantFactory = SomaticVariantFactory.passOnlyInstance();
        mRChartData = new RChartData(config.OutputDir, config.TumorId);

        mReportedGenes = Sets.newHashSet();
        mDownsampledVariants = Lists.newArrayList();
        mSnpMod = snpCount <= CHART_DOWNSAMPLE_FACTOR ? 1 : snpCount / CHART_DOWNSAMPLE_FACTOR;
        mIndelMod = indelCount <= CHART_DOWNSAMPLE_FACTOR ? 1 : indelCount / CHART_DOWNSAMPLE_FACTOR;

        mVcfWriter = null;
    }

    public double microsatelliteIndelsPerMb()
    {
        return mMicrosatelliteIndels.microsatelliteIndelsPerMb();
    }

    public MicrosatelliteStatus microsatelliteStatus()
    {
        return mEnabled ? MicrosatelliteStatus.fromIndelsPerMb(microsatelliteIndelsPerMb()) : MicrosatelliteStatus.UNKNOWN;
    }

    public double tumorMutationalBurdenPerMb()
    {
        return mTumorMutationalLoad.burdenPerMb();
    }

    public int tumorMutationalLoad()
    {
        return mTumorMutationalLoad.load();
    }

    public TumorMutationalStatus tumorMutationalBurdenPerMbStatus()
    {
        return mEnabled ? TumorMutationalStatus.fromBurdenPerMb(tumorMutationalBurdenPerMb()) : TumorMutationalStatus.UNKNOWN;
    }

    public TumorMutationalStatus tumorMutationalLoadStatus()
    {
        return mEnabled ? TumorMutationalStatus.fromLoad(tumorMutationalLoad()) : TumorMutationalStatus.UNKNOWN;
    }

    public List<DriverCatalog> drivers(final List<GeneCopyNumber> geneCopyNumbers)
    {
        return mDrivers.build(geneCopyNumbers);
    }

    public Set<String> reportedGenes()
    {
        return mReportedGenes;
    }

    public List<VariantContextDecorator> downsampledVariants() { return mDownsampledVariants; }

    public void processAndWrite(
            final PurityAdjuster purityAdjuster, final List<PurpleCopyNumber> copyNumbers, final List<FittedRegion> fittedRegions)
    {
        if(!mEnabled)
            return;

        try
        {
            VCFFileReader vcfReader = new VCFFileReader(new File(mInputVCF), false);

            mVcfWriter = new VariantContextWriterBuilder().setOutputFile(mOutputVCF)
                    .setOption(htsjdk.variant.variantcontext.writer.Options.ALLOW_MISSING_FIELDS_IN_HEADER)
                    .build();

            final SomaticVariantEnrichment enricher = new SomaticVariantEnrichment(
                    mConfig.DriverEnabled, mConfig.SomaticFitting.clonalityBinWidth(), mConfig.Version,
                    mConfig.ReferenceId, mConfig.TumorId, mReferenceData.RefGenome,
                    purityAdjuster, mGenePanel, copyNumbers, fittedRegions,
                    mReferenceData.SomaticHotspots, mReferenceData.TranscriptRegions, mPeakModel, this::accept);

            final VCFHeader header = enricher.enrichHeader(vcfReader.getFileHeader());
            mVcfWriter.writeHeader(header);

            int flushCount = 10000;
            int varCount = 0;

            for(VariantContext context : vcfReader)
            {
                enricher.accept(context);
                ++varCount;

                if(varCount > 0 && (varCount % flushCount) == 0)
                {
                    PPL_LOGGER.debug("enriched {} somatic variants", varCount);
                    enricher.flush();
                }
            }

            mVcfWriter.close();
            mRChartData.write();
        }
        catch(IOException e)
        {
            PPL_LOGGER.error("failed to enrich somatic variants: {}", e.toString());
        }
    }

    public void accept(final VariantContext context)
    {
        // post-enrichment processing
        VariantContextDecorator variant = new VariantContextDecorator(context);

        boolean isValidChromosome = HumanChromosome.contains(variant.chromosome());

        if(isValidChromosome)
        {
            mTumorMutationalLoad.processVariant(variant);
            mMicrosatelliteIndels.processVariant(context);
            checkDrivers(context, variant);
        }

        mVcfWriter.add(context);

        if(isValidChromosome)
        {
            mRChartData.processVariant(context);
            checkChartDownsampling(variant);
        }
    }

    private void checkDrivers(final VariantContext context, final VariantContextDecorator variant)
    {
        Optional<SomaticVariant> somaticVariant = mSomaticVariantFactory.createVariant(mConfig.TumorId, context);

        if(somaticVariant.isPresent())
        {
            boolean reported = mDrivers.add(somaticVariant.get());

            if(reported)
            {
                context.getCommonInfo().putAttribute(REPORTED_FLAG, true);
                mReportedGenes.add(variant.gene());
            }
        }
    }

    private void checkChartDownsampling(final VariantContextDecorator variant)
    {
        if(!variant.isPass())
            return;

        if(mConfig.Charting.disabled())
            return;

        if(variant.type() == VariantType.INDEL)
        {
            mIndelCount++;

            if(mIndelCount % mIndelMod == 0)
                mDownsampledVariants.add(variant);
        }
        else
        {
            mSnpCount++;

            if(mSnpCount % mSnpMod == 0)
                mDownsampledVariants.add(variant);
        }
    }
}
