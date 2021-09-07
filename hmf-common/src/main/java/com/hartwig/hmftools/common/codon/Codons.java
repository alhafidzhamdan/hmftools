package com.hartwig.hmftools.common.codon;

import static com.hartwig.hmftools.common.codon.Nucleotides.DNA_BASES;

import org.jetbrains.annotations.NotNull;

public final class Codons
{
    public static final String START_CODON = "ATG";

    public static final String STOP_CODON_1 = "TAA";
    public static final String STOP_CODON_2 = "TAG";
    public static final String STOP_CODON_3 = "TGA";

    public static final char START_AMINO_ACID = 'M';
    public static final char STOP_AMINO_ACID = 'X';

    public static final char UNKNOWN = '.';

    public static boolean isStopCodon(final String codon)
    {
        return codon.equals(STOP_CODON_1) || codon.equals(STOP_CODON_2) || codon.equals(STOP_CODON_3);
    }

    public static boolean isStartCodon(final String codon)
    {
        return codon.equals(START_CODON);
    }

    public static char codonToAminoAcid(final String codon)
    {
        if(isStopCodon(codon))
            return STOP_AMINO_ACID;

        if(isStartCodon(codon))
            return START_AMINO_ACID;

        switch(codon)
        {
            // SECOND BASE T
            case "TTT":
            case "TTC":
                return 'F';
            case "TTA":
            case "TTG":
            case "CTT":
            case "CTC":
            case "CTA":
            case "CTG":
                return 'L';
            case "ATT":
            case "ATC":
            case "ATA":
                return 'I';
            case "GTT":
            case "GTC":
            case "GTA":
            case "GTG":
                return 'V';

            // SECOND BASE C
            case "TCT":
            case "TCC":
            case "TCA":
            case "TCG":
                return 'S';
            case "CCT":
            case "CCC":
            case "CCA":
            case "CCG":
                return 'P';
            case "ACT":
            case "ACC":
            case "ACA":
            case "ACG":
                return 'T';
            case "GCT":
            case "GCC":
            case "GCA":
            case "GCG":
                return 'A';

            // SECOND BASE A
            case "TAT":
            case "TAC":
                return 'Y';
            case "CAT":
            case "CAC":
                return 'H';
            case "CAA":
            case "CAG":
                return 'Q';
            case "AAT":
            case "AAC":
                return 'N';
            case "AAA":
            case "AAG":
                return 'K';
            case "GAT":
            case "GAC":
                return 'D';
            case "GAA":
            case "GAG":
                return 'E';

            // SECOND BASE G
            case "TGT":
            case "TGC":
                return 'C';
            case "TGG":
                return 'W';
            case "CGT":
            case "CGC":
            case "CGA":
            case "CGG":
                return 'R';
            case "AGT":
            case "AGC":
                return 'S';
            case "AGA":
            case "AGG":
                return 'R';
            case "GGT":
            case "GGC":
            case "GGA":
            case "GGG":
                return 'G';
        }

        return UNKNOWN;
    }

    public static String aminoAcidsToCodons(@NotNull String aminoAcids)
    {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < aminoAcids.length(); i++)
        {
            builder.append(aminoAcidToCodon(aminoAcids.charAt(i)));
        }
        return builder.toString();
    }

    @NotNull
    public static String aminoAcidToCodon(char aminoAcid)
    {
        for(final char firstBase : DNA_BASES)
        {
            for(final char secondBase : DNA_BASES)
            {
                for(final char thirdBase : DNA_BASES)
                {
                    final String codon = String.valueOf(firstBase) + secondBase + thirdBase;
                    if(codonToAminoAcid(codon) == aminoAcid)
                    {
                        return codon;
                    }
                }
            }
        }

        throw new IllegalArgumentException("Unknown amino acid " + aminoAcid);
    }

    @NotNull
    public static String aminoAcidFromBases(@NotNull String dna)
    {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < dna.length() - 2; i += 3)
        {
            builder.append(codonToAminoAcid(dna.substring(i, i + 3)));
        }

        return builder.toString();
    }

}
