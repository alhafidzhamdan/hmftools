package com.hartwig.hmftools.purple.config;

import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.REF_GENOME;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeSource.REF_GENOME_CFG_DESC;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion.REF_GENOME_VERSION;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion.V37;
import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion.V38;
import static com.hartwig.hmftools.purple.PurpleCommon.PPL_LOGGER;
import static com.hartwig.hmftools.purple.config.SampleDataFiles.GERMLINE_VARIANTS;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanelConfig;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGene;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanel;
import com.hartwig.hmftools.common.drivercatalog.panel.DriverGenePanelFactory;
import com.hartwig.hmftools.common.genome.chromosome.Chromosome;
import com.hartwig.hmftools.common.genome.chromosome.ChromosomeLength;
import com.hartwig.hmftools.common.genome.chromosome.ChromosomeLengthFactory;
import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.genome.genepanel.HmfGenePanelSupplier;
import com.hartwig.hmftools.common.genome.position.GenomePosition;
import com.hartwig.hmftools.common.genome.position.GenomePositions;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeCoordinates;
import com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion;
import com.hartwig.hmftools.common.genome.region.HmfTranscriptRegion;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspotFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.utils.Lists;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;

public class ReferenceData
{
    public final RefGenomeVersion RefGenVersion;

    public final IndexedFastaSequenceFile RefGenome;

    public final Map<Chromosome, GenomePosition> ChromosomeLengths;

    public final Map<Chromosome, GenomePosition> Centromeres;

    public final List<HmfTranscriptRegion> TranscriptRegions;

    public final DriverGenePanel GenePanel;

    public final ListMultimap<Chromosome, VariantHotspot> SomaticHotspots;

    public final ListMultimap<Chromosome, VariantHotspot> GermlineHotspots;

    public final String GcProfileFilename;

    private boolean mIsValid;

    private static final String SOMATIC_HOTSPOT = "somatic_hotspots";
    private static final String GERMLINE_HOTSPOT = "germline_hotspots";
    private static final String GC_PROFILE = "gc_profile";

    // rename to driver enabled or always true anyway?
    public static String DRIVER_ENABLED = "driver_catalog";

    public static void addOptions(final Options options)
    {
        options.addOption(REF_GENOME, true, REF_GENOME_CFG_DESC);
        options.addOption(REF_GENOME_VERSION, true, "Ref genome version: V37 (default) or V38");

        options.addOption(SOMATIC_HOTSPOT, true, "Path to somatic hotspot VCF");
        options.addOption(GERMLINE_HOTSPOT, true, "Path to germline hotspot VCF");
        options.addOption(GC_PROFILE, true, "Path to GC profile.");

        DriverGenePanelConfig.addGenePanelOption(false, options);
    }

    public ReferenceData(final CommandLine cmd)
    {
        mIsValid = true;

        if(!cmd.hasOption(REF_GENOME))
        {
            mIsValid = false;
            PPL_LOGGER.error(REF_GENOME + " is a mandatory argument");
        }

        final String refGenomePath = cmd.getOptionValue(REF_GENOME);
        GcProfileFilename = cmd.getOptionValue(GC_PROFILE);

        final Map<Chromosome, GenomePosition> lengthPositions = Maps.newHashMap();

        IndexedFastaSequenceFile refGenome = null;

        try
        {
            refGenome = new IndexedFastaSequenceFile(new File(refGenomePath));

            SAMSequenceDictionary sequenceDictionary = refGenome.getSequenceDictionary();
            if(sequenceDictionary == null)
            {
                throw new ParseException("Supplied ref genome must have associated sequence dictionary");
            }

            lengthPositions.putAll(fromLengths(ChromosomeLengthFactory.create(refGenome.getSequenceDictionary())));
        }
        catch (Exception e)
        {
            mIsValid = false;
            PPL_LOGGER.error("failed to load ref genome: {}", e.toString());
        }

        RefGenome = refGenome;

        RefGenomeVersion version;

        if(cmd.hasOption(REF_GENOME_VERSION))
        {
            version = RefGenomeVersion.from(cmd.getOptionValue(REF_GENOME_VERSION));
        }
        else
        {
            // determine automatically from chromosome length
            final GenomePosition chr1Length = lengthPositions.get(HumanChromosome._1);
            if(chr1Length != null && chr1Length.position() == RefGenomeCoordinates.COORDS_38.lengths().get(HumanChromosome._1))
            {
                version = V38;
            }
            else
            {
                version = V37;
            }
        }

        RefGenVersion = version;

        PPL_LOGGER.info("Using ref genome: {}", RefGenVersion);

        final RefGenomeCoordinates refGenomeCoords = RefGenVersion == V37 ? RefGenomeCoordinates.COORDS_37 : RefGenomeCoordinates.COORDS_38;

        final Map<Chromosome, String> chromosomeNames =
                lengthPositions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().chromosome()));

        TranscriptRegions = refGenomeCoords == RefGenomeCoordinates.COORDS_38 ?
                HmfGenePanelSupplier.allGeneList38() : HmfGenePanelSupplier.allGeneList37();

        ChromosomeLengths = toPosition(refGenomeCoords.lengths(), chromosomeNames);
        Centromeres = toPosition(refGenomeCoords.centromeres(), chromosomeNames);

        boolean enabled = cmd.hasOption(DRIVER_ENABLED);

        String somaticHotspotVcf = cmd.getOptionValue(SOMATIC_HOTSPOT, Strings.EMPTY);
        String germlineHotspotVcf = cmd.getOptionValue(GERMLINE_HOTSPOT, Strings.EMPTY);

        if(enabled)
        {
            if(!DriverGenePanelConfig.isConfigured(cmd))
            {
                mIsValid = false;
                PPL_LOGGER.error(DriverGenePanelConfig.DRIVER_GENE_PANEL_OPTION + " is a mandatory argument when " + DRIVER_ENABLED + " enabled");
            }

            if(somaticHotspotVcf.isEmpty())
            {
                mIsValid = false;
                PPL_LOGGER.error(SOMATIC_HOTSPOT + " is a mandatory argument when " + DRIVER_ENABLED + " enabled");
            }

            if(!new File(somaticHotspotVcf).exists())
            {
                mIsValid = false;
                PPL_LOGGER.error("Unable to open " + SOMATIC_HOTSPOT + " file " + somaticHotspotVcf);
            }

            final List<DriverGene> driverGenes = Lists.newArrayList();

            try
            {
                driverGenes.addAll(DriverGenePanelConfig.driverGenes(cmd));
            }
            catch(IOException e)
            {
                mIsValid = false;
                PPL_LOGGER.error("Unable to load driver genes: {}", e.toString());
            }

            final RefGenomeVersion driverGeneRefGenomeVersion = RefGenVersion.is37() ? RefGenomeVersion.V37 : RefGenomeVersion.V38;

            GenePanel = DriverGenePanelFactory.create(driverGeneRefGenomeVersion, driverGenes);

            if(cmd.hasOption(GERMLINE_VARIANTS))
            {
                if(germlineHotspotVcf.isEmpty())
                {
                    mIsValid = false;
                    PPL_LOGGER.error(GERMLINE_HOTSPOT + " is a mandatory argument when " + DRIVER_ENABLED + " enabled");
                }

                if(!new File(germlineHotspotVcf).exists())
                {
                    mIsValid = false;
                    PPL_LOGGER.error("Unable to open " + GERMLINE_HOTSPOT + " file " + germlineHotspotVcf);
                }
            }
        }
        else
        {
            GenePanel = DriverGenePanelFactory.empty();
        }

        SomaticHotspots = ArrayListMultimap.create();
        GermlineHotspots = ArrayListMultimap.create();

        try
        {
            SomaticHotspots.putAll(somaticHotspotVcf.equals(Strings.EMPTY) ?
                    ArrayListMultimap.create() : VariantHotspotFile.readFromVCF(somaticHotspotVcf));

            GermlineHotspots.putAll(germlineHotspotVcf.equals(Strings.EMPTY) ?
                    ArrayListMultimap.create() : VariantHotspotFile.readFromVCF(germlineHotspotVcf));
        }
        catch (IOException e)
        {
            mIsValid = false;
            PPL_LOGGER.error("failed to load hotspots: {}", e.toString());

        }
    }

    public boolean isValid() { return mIsValid; }

    private static Map<Chromosome, GenomePosition> toPosition(final Map<Chromosome, Long> longs, final Map<Chromosome, String> contigMap)
    {
        final Map<Chromosome, GenomePosition> result = Maps.newHashMap();

        for(Map.Entry<Chromosome, String> entry : contigMap.entrySet())
        {
            final Chromosome chromosome = entry.getKey();
            final String contig = entry.getValue();
            if(longs.containsKey(chromosome))
            {
                result.put(chromosome, GenomePositions.create(contig, longs.get(chromosome)));
            }

        }

        return result;
    }

    @NotNull
    static Map<Chromosome, GenomePosition> fromLengths(@NotNull final Collection<ChromosomeLength> lengths)
    {
        return lengths.stream()
                .filter(x -> HumanChromosome.contains(x.chromosome()))
                .collect(Collectors.toMap(x -> HumanChromosome.fromString(x.chromosome()),
                        item -> GenomePositions.create(item.chromosome(), item.length())));
    }

}
