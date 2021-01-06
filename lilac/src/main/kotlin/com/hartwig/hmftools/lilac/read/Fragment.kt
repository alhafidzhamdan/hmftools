package com.hartwig.hmftools.lilac.read

data class Fragment(val reads: List<AminoAcidRead>) {

    fun containsAminoAcid(index: Int): Boolean {
        return reads.any { it.aminoAcidIndices().contains(index) }
    }

    fun aminoAcid(index: Int, minQual: Int = 0): Char {
        for (read in reads) {
            if (read.aminoAcidIndices().contains(index)) {
                return read.aminoAcid(index, minQual)
            }
        }

       throw IllegalArgumentException("Fragment does not contain amino acid at location $index")
    }

    fun aminoAcidIndices(): List<IntRange> = reads.map { it.aminoAcidIndices() }

}