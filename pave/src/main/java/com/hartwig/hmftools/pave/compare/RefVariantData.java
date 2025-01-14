package com.hartwig.hmftools.pave.compare;

import static com.hartwig.hmftools.common.variant.SomaticVariantFactory.localPhaseSetsStringToList;
import static com.hartwig.hmftools.patientdb.database.hmfpatients.Tables.SOMATICVARIANT;
import static com.hartwig.hmftools.pave.VariantData.NO_LOCAL_PHASE_SET;

import java.util.List;
import java.util.StringJoiner;

import com.hartwig.hmftools.common.variant.CodingEffect;
import com.hartwig.hmftools.common.variant.Hotspot;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.common.variant.VariantType;
import com.hartwig.hmftools.patientdb.database.hmfpatients.Tables;

import org.jooq.Record;

public class RefVariantData
{
    public final String Chromosome;
    public final int Position;
    public final String Ref;
    public final String Alt;

    public final VariantType Type;
    public final String Gene;

    public final String CanonicalEffect;
    public final CodingEffect CanonicalCodingEffect;
    public final String HgvsCodingImpact;
    public final String HgvsProteinImpact;

    public final CodingEffect WorstCodingEffect;

    public final int LocalPhaseSet;
    public final String Microhomology;
    public final String RepeatSequence;
    public final int RepeatCount;
    public final boolean Reported;
    public final boolean IsHotspot;

    // repeatCount, localPhaseSet, localRealignmentSet, reported

    public RefVariantData(
            final String chromosome, final int position, final String ref, final String alt, final VariantType type,
            final String gene, final String canonicalEffect, final CodingEffect canonicalCodingEffect,
            final CodingEffect worstCodingEffect, final String hgvsCodingImpact, final String hgvsProteinImpact,
            final String microhomology, final String repeatSequence, int repeatCount, int localPhaseSet,
            boolean reported, boolean isHotspot)
    {
        Chromosome = chromosome;
        Position = position;
        Ref = ref;
        Alt = alt;
        Type = type;
        Gene = gene;

        CanonicalEffect = canonicalEffect;
        CanonicalCodingEffect = canonicalCodingEffect;
        WorstCodingEffect = worstCodingEffect;

        LocalPhaseSet = localPhaseSet;
        HgvsCodingImpact = hgvsCodingImpact;
        HgvsProteinImpact = hgvsProteinImpact;
        Microhomology = microhomology;
        RepeatSequence = repeatSequence;
        RepeatCount = repeatCount;
        Reported = reported;
        IsHotspot = isHotspot;
    }

    public static RefVariantData fromSomatic(final SomaticVariant variant)
    {
        int localPhaseSet = variant.topLocalPhaseSet() != null ? variant.topLocalPhaseSet() : NO_LOCAL_PHASE_SET;

        return new RefVariantData(
                variant.chromosome(), (int)variant.position(), variant.ref(), variant.alt(), variant.type(), variant.gene(),
                variant.canonicalEffect(), variant.canonicalCodingEffect(), variant.worstCodingEffect(),
                variant.canonicalHgvsCodingImpact(), variant.canonicalHgvsProteinImpact(),
                variant.microhomology(), variant.repeatSequence(), variant.repeatCount(),
                localPhaseSet, variant.reported(), variant.isHotspot());
    }

    public static RefVariantData fromRecord(final Record record)
    {
        CodingEffect canonicalCodingEffect = record.getValue(SOMATICVARIANT.CANONICALCODINGEFFECT).isEmpty()
                ? CodingEffect.UNDEFINED
                : CodingEffect.valueOf(record.getValue(SOMATICVARIANT.CANONICALCODINGEFFECT));

        CodingEffect worstCodingEffect = record.getValue(SOMATICVARIANT.WORSTCODINGEFFECT).isEmpty()
                ? CodingEffect.UNDEFINED
                : CodingEffect.valueOf(record.getValue(SOMATICVARIANT.WORSTCODINGEFFECT));

        String localPhaseSetStr = record.get(Tables.SOMATICVARIANT.LOCALPHASESET);
        List<Integer> localPhaseSets = localPhaseSetsStringToList(localPhaseSetStr);
        int localPhaseSet = localPhaseSets != null ? localPhaseSets.get(0) : NO_LOCAL_PHASE_SET;

        boolean isHotspot = Hotspot.valueOf(record.getValue(SOMATICVARIANT.HOTSPOT)) == Hotspot.HOTSPOT;

        return new RefVariantData(
                record.getValue(Tables.SOMATICVARIANT.CHROMOSOME),
                record.getValue(Tables.SOMATICVARIANT.POSITION),
                record.getValue(Tables.SOMATICVARIANT.REF),
                record.getValue(Tables.SOMATICVARIANT.ALT),
                VariantType.valueOf(record.getValue(SOMATICVARIANT.TYPE)),
                record.getValue(Tables.SOMATICVARIANT.GENE),
                record.getValue(SOMATICVARIANT.CANONICALEFFECT),
                canonicalCodingEffect,
                worstCodingEffect,
                record.getValue(SOMATICVARIANT.CANONICALHGVSCODINGIMPACT),
                record.getValue(SOMATICVARIANT.CANONICALHGVSPROTEINIMPACT),
                record.getValue(SOMATICVARIANT.MICROHOMOLOGY),
                record.getValue(SOMATICVARIANT.REPEATSEQUENCE),
                record.getValue(SOMATICVARIANT.REPEATCOUNT),
                localPhaseSet,
                record.getValue(SOMATICVARIANT.REPORTED) == 1,
                isHotspot);
    }

    public String toString()
    {
        return String.format("pos(%s:%d) variant(%s: %s>%s) canon(%s: %s) worst(%s) hgvs(coding=%s protein=%s)",
                Chromosome, Position, Type, Ref, Alt, CanonicalCodingEffect, CanonicalEffect,
                WorstCodingEffect, HgvsCodingImpact, HgvsProteinImpact);
    }

    public static String tsvHeader()
    {
        StringJoiner sj = new StringJoiner("\t");
        sj.add("sampleId");
        sj.add("chromosome");
        sj.add("position");
        sj.add("type");
        sj.add("ref");
        sj.add("alt");
        sj.add("gene");
        sj.add("worstCodingEffect");
        sj.add("canonicalEffect");
        sj.add("canonicalCodingEffect");
        sj.add("canonicalHgvsCodingImpact");
        sj.add("canonicalHgvsProteinImpact");
        sj.add("microhomology");
        sj.add("repeatSequence");
        sj.add("repeatCount");
        sj.add("localPhaseSet");
        sj.add("reported");
        sj.add("hotspot");
        return sj.toString();
    }

    public String tsvData(final String sampleId)
    {
        StringJoiner sj = new StringJoiner("\t");
        sj.add(sampleId);
        sj.add(Chromosome);
        sj.add(String.valueOf(Position));
        sj.add(Type.toString());
        sj.add(Ref);
        sj.add(Alt);
        sj.add(Gene);
        sj.add(String.valueOf(WorstCodingEffect));
        sj.add(CanonicalEffect);
        sj.add(String.valueOf(CanonicalCodingEffect));
        sj.add(HgvsCodingImpact);
        sj.add(HgvsProteinImpact);
        sj.add(Microhomology);
        sj.add(RepeatSequence);
        sj.add(String.valueOf(RepeatCount));
        sj.add(LocalPhaseSet == NO_LOCAL_PHASE_SET ? "NULL" : String.valueOf(LocalPhaseSet));
        sj.add(Reported ? "1" : "0");
        sj.add(IsHotspot ? Hotspot.HOTSPOT.toString() : "NONE");
        return sj.toString();
    }
}
