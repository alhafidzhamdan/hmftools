package com.hartwig.hmftools.sage.read;

import com.hartwig.hmftools.common.samtools.CigarHandler;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;

import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SAMRecord;

public class RawContextCigarHandler implements CigarHandler
{
    private final VariantHotspot mVariant;
    private final int mMaxSkippedReferenceRegions;
    private final boolean mIsInsert;
    private final boolean mIsDelete;
    private final boolean mIsSNV;

    private RawContext mResult;

    RawContextCigarHandler(final int maxSkippedReferenceRegions, final VariantHotspot variant)
    {
        mVariant = variant;
        mIsInsert = variant.ref().length() < variant.alt().length();
        mIsDelete = variant.ref().length() > variant.alt().length();
        mIsSNV = variant.ref().length() == variant.alt().length();
        mMaxSkippedReferenceRegions = maxSkippedReferenceRegions;
    }

    public RawContext result()
    {
        return mResult;
    }

    @Override
    public void handleLeftSoftClip(@NotNull final SAMRecord record, @NotNull final CigarElement element)
    {
        if(mVariant.position() < record.getAlignmentStart())
        {
            int readIndex = record.getReadPositionAtReferencePosition(record.getAlignmentStart()) - 1 - record.getAlignmentStart()
                    + (int) mVariant.position() - mVariant.alt().length() + mVariant.ref().length();
            mResult = RawContext.inSoftClip(readIndex);
        }
    }

    @Override
    public void handleRightSoftClip(@NotNull final SAMRecord record, @NotNull final CigarElement element, final int readIndex,
            final int refPosition)
    {
        if(mResult != null)
        {
            return;
        }

        long refPositionEnd = refPosition + element.getLength() - 1;
        if(refPositionEnd < mVariant.position())
        {
            throw new IllegalStateException("Variant is after record");
        }

        if(mVariant.position() >= refPosition && mVariant.position() <= refPositionEnd)
        {
            int alignmentEnd = record.getAlignmentEnd();
            int actualIndex = record.getReadPositionAtReferencePosition(alignmentEnd) - 1 - alignmentEnd + (int) mVariant.position();
            mResult = RawContext.inSoftClip(actualIndex);
        }
    }

    @Override
    public void handleAlignment(@NotNull final SAMRecord record, @NotNull final CigarElement e, final int readIndex, final int refPosition)
    {
        if(mResult != null)
        {
            return;
        }

        long refPositionEnd = refPosition + e.getLength() - 1;
        if(refPosition <= mVariant.position() && mVariant.position() <= refPositionEnd)
        {
            int readIndexOffset = (int) (mVariant.position() - refPosition);
            int variantReadIndex = readIndex + readIndexOffset;

            int baseQuality = record.getBaseQualities()[variantReadIndex];
            boolean altSupport = mIsSNV && refPositionEnd >= mVariant.end() && matchesString(record, variantReadIndex, mVariant.alt());
            boolean refSupport = !altSupport && matchesFirstBase(record, variantReadIndex, mVariant.ref());
            mResult = RawContext.alignment(variantReadIndex, altSupport, refSupport, baseQuality);
        }
    }

    @Override
    public void handleInsert(@NotNull final SAMRecord record, @NotNull final CigarElement e, final int readIndex, final int refPosition)
    {
        if(mResult != null)
        {
            return;
        }

        if(refPosition == mVariant.position())
        {
            boolean altSupport = mIsInsert && e.getLength() == mVariant.alt().length() - 1 && matchesString(record, readIndex, mVariant.alt());
            int baseQuality = altSupport ? baseQuality(readIndex, record, mVariant.alt().length()) : 0;
            mResult = RawContext.indel(readIndex, altSupport, baseQuality);
        }

    }

    @Override
    public void handleDelete(@NotNull final SAMRecord record, @NotNull final CigarElement e, final int readIndex, final int refPosition)
    {
        if(mResult != null)
        {
            return;
        }

        int refPositionEnd = refPosition + e.getLength();
        if(refPosition == mVariant.position())
        {
            boolean altSupport = mIsDelete && e.getLength() == mVariant.ref().length() - 1 && matchesFirstBase(record,
                    readIndex,
                    mVariant.ref());
            int baseQuality = altSupport ? baseQuality(readIndex, record, 2) : 0;
            mResult = RawContext.indel(readIndex, altSupport, baseQuality);
        }
        else if(refPositionEnd >= mVariant.position())
        {
            mResult = RawContext.inDelete(readIndex);
        }
    }

    @Override
    public void handleSkippedReference(@NotNull final SAMRecord record, @NotNull final CigarElement e, final int readIndex,
            final int refPosition)
    {
        if(mResult != null)
        {
            return;
        }

        if(e.getLength() > mMaxSkippedReferenceRegions)
        {
            int refPositionEnd = refPosition + e.getLength();
            if(refPositionEnd >= mVariant.position())
            {
                mResult = RawContext.inSkipped(readIndex);
            }
        }

        handleDelete(record, e, readIndex, refPosition);
    }

    private static boolean matchesFirstBase(@NotNull final SAMRecord record, int index, @NotNull final String expected)
    {
        return expected.charAt(0) == record.getReadBases()[index];
    }

    private static boolean matchesString(@NotNull final SAMRecord record, int index, @NotNull final String expected)
    {
        for(int i = 0; i < expected.length(); i++)
        {
            if((byte) expected.charAt(i) != record.getReadBases()[index + i])
            {
                return false;
            }
        }
        return true;
    }

    private int baseQuality(int readIndex, SAMRecord record, int length)
    {
        int maxIndex = Math.min(readIndex + length, record.getBaseQualities().length) - 1;
        int quality = Integer.MAX_VALUE;
        for(int i = readIndex; i <= maxIndex; i++)
        {
            quality = Math.min(quality, record.getBaseQualities()[i]);
        }
        return quality;
    }
}
