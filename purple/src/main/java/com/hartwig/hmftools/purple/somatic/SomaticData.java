package com.hartwig.hmftools.purple.somatic;

import static com.hartwig.hmftools.common.variant.Hotspot.HOTSPOT_FLAG;

import com.hartwig.hmftools.common.genome.position.GenomePosition;
import com.hartwig.hmftools.common.variant.AllelicDepth;
import com.hartwig.hmftools.common.variant.VariantContextDecorator;
import com.hartwig.hmftools.common.variant.VariantType;
import com.hartwig.hmftools.common.variant.impact.VariantImpact;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;

public class SomaticData implements GenomePosition
{
    private final VariantContext mContext;
    private VariantContext mNewContext;

    private final String mChromosome;
    private final int mPosition;
    private final VariantContextDecorator mDecorator;
    private final AllelicDepth mTumorAllelicDepth;

    public SomaticData(final VariantContext context, final String sampleId)
    {
        mContext = context;
        mDecorator = new VariantContextDecorator(context);
        mChromosome = mContext.getContig();
        mPosition = mContext.getStart();
        mTumorAllelicDepth = sampleId != null ? mDecorator.allelicDepth(sampleId) :  null;
        mNewContext = null;
    }

    public VariantContext context() { return mContext; }

    public VariantContext newContext()
    {
        if(mNewContext == null)
            mNewContext = new VariantContextBuilder(mContext).make();

        return mNewContext;
    }

    @Override
    public String chromosome() { return mChromosome; }

    @Override
    public int position() { return mPosition; }

    // convenience methods
    public VariantContextDecorator decorator() { return mDecorator; }
    public VariantImpact variantImpact() { return mDecorator.variantImpact(); }

    public VariantType type() { return mDecorator.type(); }

    public boolean isPass() { return mDecorator.isPass(); }
    public boolean isFiltered() { return !isPass(); }

    public double variantCopyNumber() { return mDecorator.variantCopyNumber(); }

    public boolean isHotspot() { return mContext.hasAttribute(HOTSPOT_FLAG); }
    public boolean biallelic() { return mDecorator.biallelic(); }
    public String gene() { return mDecorator.variantImpact().CanonicalGeneName; }

    public boolean hasAlleleDepth() { return mTumorAllelicDepth != null; }
    public AllelicDepth tumorAlleleDepth() { return mTumorAllelicDepth; }
    public double alleleFrequency() { return mTumorAllelicDepth != null ? mTumorAllelicDepth.alleleFrequency() : 0; }
    public int totalReadCount() { return mTumorAllelicDepth != null ? mTumorAllelicDepth.totalReadCount() : 0; }
    public int alleleReadCount() { return mTumorAllelicDepth != null ? mTumorAllelicDepth.alleleReadCount() : 0; }
}