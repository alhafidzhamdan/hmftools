package com.hartwig.hmftools.purple.somatic;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.hartwig.hmftools.common.utils.Doubles;
import com.hartwig.hmftools.purple.fitting.PeakModel;
import com.hartwig.hmftools.purple.fitting.WeightedPloidyHistogram;

import org.jetbrains.annotations.NotNull;

public class SubclonalLikelihood
{
    private static final double MAX_PLOIDY = 2;

    private final double[] mSubclonalLikelihood;
    private final WeightedPloidyHistogram mHistogram;

    public SubclonalLikelihood(double binWidth, @NotNull final List<PeakModel> peakModel)
    {
        mHistogram = new WeightedPloidyHistogram(MAX_PLOIDY, binWidth);

        final List<PeakModel> validPeaks = peakModel.stream().filter(PeakModel::isValid).collect(toList());

        double[] clonalHistogram = mHistogram.modelHistogram(validPeaks.stream().filter(x -> !x.isSubclonal()).collect(toList()));
        double[] subclonalHistogram = mHistogram.modelHistogram(validPeaks.stream().filter(PeakModel::isSubclonal).collect(toList()));

        mSubclonalLikelihood = new double[clonalHistogram.length];

        for(int i = 0; i < clonalHistogram.length; i++)
        {
            double clonal = clonalHistogram[i];
            double subclonal = subclonalHistogram[i];
            double total = clonal + subclonal;

            if(Doubles.greaterThan(total, 0))
            {
                double likelihood = subclonal / (clonal + subclonal);
                mSubclonalLikelihood[i] = likelihood;
            }
        }
    }

    public double subclonalLikelihood(double ploidy)
    {
        int bucket = mHistogram.bucket(ploidy);

        if(bucket < mSubclonalLikelihood.length)
            return mSubclonalLikelihood[bucket];

        return 0;
    }
}
