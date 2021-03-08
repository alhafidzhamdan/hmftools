package com.hartwig.hmftools.compar.linx;

import static com.hartwig.hmftools.compar.Category.LINX_DATA;
import static com.hartwig.hmftools.compar.CommonUtils.ITEM_DELIM;
import static com.hartwig.hmftools.compar.CommonUtils.diffValue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.hmftools.common.drivercatalog.DriverCatalog;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantData;
import com.hartwig.hmftools.common.variant.structural.linx.LinxCluster;
import com.hartwig.hmftools.common.variant.structural.linx.LinxSvAnnotation;
import com.hartwig.hmftools.compar.Category;
import com.hartwig.hmftools.compar.ComparableItem;

import org.apache.commons.compress.utils.Lists;

public class LinxSvData implements ComparableItem
{
    public final StructuralVariantData SvData;
    public final LinxSvAnnotation Annotation;
    public final LinxCluster Cluster;

    public LinxSvData(final StructuralVariantData svData, final LinxSvAnnotation annotation, final LinxCluster cluster)
    {
        SvData = svData;
        Cluster = cluster;
        Annotation = annotation;
    }

    public Category category() { return LINX_DATA; }

    public boolean matches(final ComparableItem other)
    {
        final LinxSvData otherSv = (LinxSvData)other;

        if(otherSv.SvData.type() != SvData.type())
            return false;

        if(!otherSv.SvData.startChromosome().equals(SvData.startChromosome()) || !otherSv.SvData.endChromosome().equals(SvData.endChromosome()))
            return false;

        if(otherSv.SvData.startPosition() != SvData.startPosition() || otherSv.SvData.endPosition() != SvData.endPosition())
            return false;

        if(otherSv.SvData.startOrientation() != SvData.startOrientation() || otherSv.SvData.endOrientation() != SvData.endOrientation())
            return false;

        return true;
    }

    public List<String> findDifferences(final ComparableItem other)
    {
        final LinxSvData otherSv = (LinxSvData)other;

        final List<String> diffs = Lists.newArrayList();

        if(!Cluster.resolvedType().equals(otherSv.Cluster.resolvedType()))
        {
            diffs.add(String.format("resolvedType(%s/%s)", Cluster.resolvedType(), otherSv.Cluster.resolvedType()));
        }

        if(Cluster.clusterCount() != otherSv.Cluster.clusterCount())
        {
            diffs.add(String.format("clusterCount(%d/%d)", Cluster.clusterCount(), otherSv.Cluster.clusterCount()));
        }

        double jcn = Annotation.junctionCopyNumberMin() + Annotation.junctionCopyNumberMax();
        double jcnOther = otherSv.Annotation.junctionCopyNumberMin() + otherSv.Annotation.junctionCopyNumberMax();

        if(diffValue(jcn, jcnOther))
        {
            diffs.add(String.format("jcn(%.1f/%.1f)", jcn, jcnOther));
        }

        if(!genesEqual(Annotation.geneStart(), otherSv.Annotation.geneStart()) || !genesEqual(Annotation.geneEnd(), otherSv.Annotation.geneEnd()))
        {
            diffs.add(String.format("genes(%s-%s/%s-%s)",
                    Annotation.geneStart(), Annotation.geneEnd(), otherSv.Annotation.geneStart(), otherSv.Annotation.geneEnd()));
        }

        if(Annotation.isFoldback() != otherSv.Annotation.isFoldback())
        {
            diffs.add(String.format("foldback(%s/%s)", Annotation.isFoldback(), otherSv.Annotation.isFoldback()));
        }

        return diffs;
    }

    private boolean genesEqual(final String genes1, final String genes2)
    {
        final List<String> geneNames1 = Arrays.stream(genes1.split(ITEM_DELIM, -1)).collect(Collectors.toList());
        final List<String> geneNames2 = Arrays.stream(genes2.split(ITEM_DELIM, -1)).collect(Collectors.toList());

        if(geneNames1.size() != geneNames2.size())
            return false;

        return !geneNames1.stream().noneMatch(x -> geneNames2.contains(x));
    }

    public String description()
    {
        return String.format("%d_%s %s_%d - %s_%d",
                SvData.id(), SvData.type(), SvData.startChromosome(), SvData.startPosition(), SvData.endChromosome(), SvData.endPosition());
    }
}
