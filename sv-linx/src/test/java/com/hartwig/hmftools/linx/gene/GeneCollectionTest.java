package com.hartwig.hmftools.linx.gene;

import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache.EXON_RANK_MAX;
import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache.EXON_RANK_MIN;
import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache.createBreakendTranscriptData;
import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache.getProteinDomainPositions;
import static com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache.setAlternativeTranscriptPhasings;
import static com.hartwig.hmftools.common.ensemblcache.GeneTestUtils.createGeneDataCache;
import static com.hartwig.hmftools.common.ensemblcache.GeneTestUtils.createTransExons;
import static com.hartwig.hmftools.common.ensemblcache.TranscriptProteinData.BIOTYPE_PROTEIN_CODING;
import static com.hartwig.hmftools.common.fusion.CodingBaseData.PHASE_0;
import static com.hartwig.hmftools.common.fusion.CodingBaseData.PHASE_1;
import static com.hartwig.hmftools.common.fusion.CodingBaseData.PHASE_2;
import static com.hartwig.hmftools.common.fusion.CodingBaseData.PHASE_NONE;
import static com.hartwig.hmftools.common.fusion.FusionCommon.NEG_STRAND;
import static com.hartwig.hmftools.common.fusion.FusionCommon.POS_STRAND;
import static com.hartwig.hmftools.common.fusion.BreakendTransData.POST_CODING_PHASE;
import static com.hartwig.hmftools.common.fusion.TranscriptCodingType.CODING;
import static com.hartwig.hmftools.common.fusion.TranscriptCodingType.UTR_3P;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_END;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_START;
import static com.hartwig.hmftools.common.utils.sv.SvCommonUtils.NEG_ORIENT;
import static com.hartwig.hmftools.common.utils.sv.SvCommonUtils.POS_ORIENT;
import static com.hartwig.hmftools.linx.utils.GeneTestUtils.CHR_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.ensemblcache.EnsemblDataCache;
import com.hartwig.hmftools.common.ensemblcache.EnsemblGeneData;
import com.hartwig.hmftools.common.ensemblcache.GeneTestUtils;
import com.hartwig.hmftools.common.ensemblcache.TranscriptData;
import com.hartwig.hmftools.common.ensemblcache.TranscriptProteinData;
import com.hartwig.hmftools.common.fusion.BreakendGeneData;
import com.hartwig.hmftools.common.fusion.BreakendTransData;

import org.junit.Test;

public class GeneCollectionTest
{
    @Test
    public void testExonDataExtraction()
    {
        EnsemblDataCache geneTransCache = createGeneDataCache();

        String geneName = "GENE1";
        String geneId = "ENSG0001";
        String chromosome = "1";

        List<EnsemblGeneData> geneList = Lists.newArrayList();
        geneList.add(GeneTestUtils.createEnsemblGeneData(geneId, geneName, chromosome, 1, 10000, 20000));
        GeneTestUtils.addGeneData(geneTransCache, chromosome, geneList);

        List<TranscriptData> transDataList = Lists.newArrayList();

        int transId = 1;
        byte strand = 1;

        int[] exonStarts = new int[]{10500, 11500, 12500, 13500};

        int codingStart = 11500;
        int codingEnd = 13598;
        TranscriptData transData = createTransExons(geneId, transId, strand, exonStarts, 99, codingStart, codingEnd,true, "");
        transDataList.add(transData);

        int transId2 = 2;

        exonStarts = new int[]{12500, 13500, 14500};

        transData = createTransExons(geneId, transId2, strand, exonStarts, 100, null, null, true, "");
        String transName2 = transData.TransName;
        transDataList.add(transData);

        GeneTestUtils.addTransExonData(geneTransCache, geneId, transDataList);

        // test exon retrieval
        transData = geneTransCache.getTranscriptData(geneId, "");
        assertEquals(transId, transData.TransId);
        assertEquals(4, transData.exons().size());

        transData = geneTransCache.getTranscriptData(geneId, transName2);
        assertEquals(transId2, transData.TransId);
        assertEquals(3, transData.exons().size());

        // test exon ranks given a position

        int[] transUpExonData = geneTransCache.getExonRankings(geneId, 11400);

        assertEquals(1, transUpExonData[EXON_RANK_MIN]);
        assertEquals(2, transUpExonData[EXON_RANK_MAX]);

        // before the first
        transUpExonData = geneTransCache.getExonRankings(geneId, 9000);

        assertEquals(0, transUpExonData[EXON_RANK_MIN]);
        assertEquals(1, transUpExonData[EXON_RANK_MAX]);

        // after the last
        transUpExonData = geneTransCache.getExonRankings(geneId, 16000);

        assertEquals(4, transUpExonData[EXON_RANK_MIN]);
        assertEquals(-1, transUpExonData[EXON_RANK_MAX]);

        // on an exon boundary
        transUpExonData = geneTransCache.getExonRankings(geneId, 12500);

        assertEquals(3, transUpExonData[EXON_RANK_MIN]);
        assertEquals(3, transUpExonData[EXON_RANK_MAX]);
    }

    @Test
    public void testTranscriptBreakends()
    {
        EnsemblDataCache geneTransCache = createGeneDataCache();

        // first a gene on the forward strand
        String geneName = "GENE1";
        String geneId = "ENSG0001";

        List<EnsemblGeneData> geneList = Lists.newArrayList();
        geneList.add(GeneTestUtils.createEnsemblGeneData(geneId, geneName, CHR_1, POS_STRAND, 100, 1000));
        GeneTestUtils.addGeneData(geneTransCache, CHR_1, geneList);

        BreakendGeneData genePosStrand = GeneTestUtils.createGeneAnnotation(0, true, geneName, geneId, POS_STRAND, CHR_1, 0, POS_ORIENT);

        List<TranscriptData> transDataList = Lists.newArrayList();

        int transId = 1;
        int[] exonStarts = new int[]{100, 300, 500, 700, 900};

        Integer codingStart = 349;
        Integer codingEnd = 950;
        TranscriptData transData = createTransExons(geneId, transId++, POS_STRAND, exonStarts, 100, codingStart, codingEnd, false, "");
        transDataList.add(transData);

        GeneTestUtils.addTransExonData(geneTransCache, geneId, transDataList);

        int position = 250;
        genePosStrand.setPositionalData(CHR_1, position, POS_ORIENT);
        BreakendTransData trans = createBreakendTranscriptData(transData, position, genePosStrand);

        assertEquals(5, trans.exonCount());
        assertEquals(1, trans.ExonUpstream);
        assertEquals(2, trans.ExonDownstream);
        assertEquals(PHASE_NONE, trans.Phase);

        // test caching of upstream phasings for exon-skipping fusion logic
        setAlternativeTranscriptPhasings(trans, transData.exons(), position, POS_ORIENT);
        assertEquals(0, trans.getAlternativePhasing().size());

        // and test as a downstream gene
        setAlternativeTranscriptPhasings(trans, transData.exons(), position, NEG_ORIENT);
        assertEquals(3, trans.getAlternativePhasing().size());
        Integer exonsSkipped = trans.getAlternativePhasing().get(PHASE_1);
        assertTrue(exonsSkipped != null && exonsSkipped == 1);
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_2);
        assertTrue(exonsSkipped != null && exonsSkipped == 3);
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_0);
        assertTrue(exonsSkipped != null && exonsSkipped == 2);

        position = 450;
        genePosStrand.setPositionalData(CHR_1, position, POS_ORIENT);
        trans = createBreakendTranscriptData(transData, position, genePosStrand);

        assertEquals(2, trans.ExonUpstream);
        assertEquals(3, trans.ExonDownstream);
        assertEquals(PHASE_1, trans.Phase);

        setAlternativeTranscriptPhasings(trans, transData.exons(), position, POS_ORIENT);
        assertEquals(1, trans.getAlternativePhasing().size());
        exonsSkipped = trans.getAlternativePhasing().get(-1);
        assertTrue(exonsSkipped != null && exonsSkipped == 1);

        setAlternativeTranscriptPhasings(trans, transData.exons(), position, NEG_ORIENT);
        assertEquals(2, trans.getAlternativePhasing().size());
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_2);
        assertTrue(exonsSkipped != null && exonsSkipped == 2);
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_0);
        assertTrue(exonsSkipped != null && exonsSkipped == 1);

        position = 650;
        genePosStrand.setPositionalData(CHR_1, position, POS_ORIENT);
        trans = createBreakendTranscriptData(transData, position, genePosStrand);

        assertEquals(3, trans.ExonUpstream);
        assertEquals(4, trans.ExonDownstream);
        assertEquals(PHASE_0, trans.Phase);

        setAlternativeTranscriptPhasings(trans, transData.exons(), position, POS_ORIENT);
        assertEquals(2, trans.getAlternativePhasing().size());
        exonsSkipped = trans.getAlternativePhasing().get(-1);
        assertTrue(exonsSkipped != null && exonsSkipped == 2);
        exonsSkipped = trans.getAlternativePhasing().get(1);
        assertTrue(exonsSkipped != null && exonsSkipped == 1);

        setAlternativeTranscriptPhasings(trans, transData.exons(), position, NEG_ORIENT);
        assertEquals(1, trans.getAlternativePhasing().size());
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_2);
        assertTrue(exonsSkipped != null && exonsSkipped == 1);

        // then a gene on the reverse strand
        geneName = "GENE2";
        geneId = "ENSG0002";

        geneList.add(GeneTestUtils.createEnsemblGeneData(geneId, geneName, CHR_1, POS_STRAND, 100, 1000));
        GeneTestUtils.addGeneData(geneTransCache, CHR_1, geneList);

        BreakendGeneData geneNegStrand = GeneTestUtils.createGeneAnnotation(0, true, geneName, geneId, NEG_STRAND, CHR_1, 0, POS_ORIENT);

        transDataList = Lists.newArrayList();

        transId = 2;

        exonStarts = new int[]{100, 300, 500, 700, 900};
        transData = createTransExons(geneId, transId++, NEG_STRAND, exonStarts, 100, codingStart, codingEnd, false, "");

        transDataList.add(transData);

        GeneTestUtils.addTransExonData(geneTransCache, geneId, transDataList);

        position = 850;
        geneNegStrand.setPositionalData(CHR_1, position, POS_ORIENT);
        trans = createBreakendTranscriptData(transData, position, geneNegStrand);

        assertEquals(5, trans.exonCount());
        assertEquals(1, trans.ExonUpstream);
        assertEquals(2, trans.ExonDownstream);
        assertEquals(PHASE_0, trans.Phase);

        setAlternativeTranscriptPhasings(trans, transData.exons(), position, NEG_ORIENT);
        assertEquals(0, trans.getAlternativePhasing().size());

        setAlternativeTranscriptPhasings(trans, transData.exons(), position, POS_ORIENT);
        assertEquals(3, trans.getAlternativePhasing().size());
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_1);
        assertTrue(exonsSkipped != null && exonsSkipped == 2);
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_NONE);
        assertTrue(exonsSkipped != null && exonsSkipped == 3);
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_2);
        assertTrue(exonsSkipped != null && exonsSkipped == 1);

        position = 250;
        geneNegStrand.setPositionalData(CHR_1, position, POS_ORIENT);
        trans = createBreakendTranscriptData(transData, position, geneNegStrand);

        assertEquals(4, trans.ExonUpstream);
        assertEquals(5, trans.ExonDownstream);
        assertEquals(POST_CODING_PHASE, trans.Phase);

        setAlternativeTranscriptPhasings(trans, transData.exons(), position, NEG_ORIENT);
        assertEquals(3, trans.getAlternativePhasing().size());
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_1);
        assertTrue(exonsSkipped != null && exonsSkipped == 1);
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_0);
        assertTrue(exonsSkipped != null && exonsSkipped == 3);
        exonsSkipped = trans.getAlternativePhasing().get(PHASE_2);
        assertTrue(exonsSkipped != null && exonsSkipped == 2);

        setAlternativeTranscriptPhasings(trans, transData.exons(), position, POS_ORIENT);
        assertEquals(0, trans.getAlternativePhasing().size());
    }

    @Test
    public void testBreakendTranscriptCoding()
    {
        EnsemblDataCache geneTransCache = createGeneDataCache();

        // first a gene on the forward strand
        String geneName = "GENE1";
        String geneId = "ENSG0001";
        String chromosome = "1";

        BreakendGeneData genePosStrand = GeneTestUtils.createGeneAnnotation(0, true, geneName, geneId, POS_STRAND, CHR_1, 0, POS_ORIENT);

        int transId = 1;

        int[] exonStarts = new int[]{100, 200, 300, 400, 500};

        // coding taking up exactly the first exon
        Integer codingStart = 100;
        Integer codingEnd = 110;

        TranscriptData transData = createTransExons(
                geneId, transId++, POS_STRAND, exonStarts, 10, codingStart, codingEnd, true, BIOTYPE_PROTEIN_CODING);

        int position = 150;
        genePosStrand.setPositionalData(CHR_1, position, POS_ORIENT);
        BreakendTransData trans = createBreakendTranscriptData(transData, position, genePosStrand);

        assertEquals(5, trans.exonCount());
        assertEquals(1, trans.ExonUpstream);
        assertEquals(2, trans.ExonDownstream);
        assertEquals(UTR_3P, trans.codingType());
        assertEquals(-2, trans.Phase);
        assertEquals(-2, trans.Phase);
        assertEquals(8, trans.CodingBases); // stop codon is taken out
        assertEquals(8, trans.TotalCodingBases);

        //
        codingStart = 105;
        codingEnd = 405;

        transData = createTransExons(
                geneId, transId++, POS_STRAND, exonStarts, 10, codingStart, codingEnd, true, BIOTYPE_PROTEIN_CODING);

        position = 350;
        genePosStrand.setPositionalData(CHR_1, position, POS_ORIENT);
        trans = createBreakendTranscriptData(transData, position, genePosStrand);

        assertEquals(3, trans.ExonUpstream);
        assertEquals(4, trans.ExonDownstream);
        assertEquals(CODING, trans.codingType());
        assertEquals(PHASE_1, trans.Phase);
        assertEquals(28, trans.CodingBases);
        assertEquals(31, trans.TotalCodingBases);

        // test the reverse strand
        geneName = "GENE2";
        geneId = "ENSG0002";
        chromosome = "1";

        BreakendGeneData geneNegStrand = GeneTestUtils.createGeneAnnotation(0, true, geneName, geneId, NEG_STRAND, CHR_1, 0, POS_ORIENT);

        // coding taking up exactly the first exon
        codingStart = 500;
        codingEnd = 510;

        transData = createTransExons(
                geneId, transId++, NEG_STRAND, exonStarts, 10, codingStart, codingEnd, true, BIOTYPE_PROTEIN_CODING);

        position = 450;
        geneNegStrand.setPositionalData(CHR_1, position, POS_ORIENT);
        trans = createBreakendTranscriptData(transData, position, geneNegStrand);

        assertEquals(5, trans.exonCount());
        assertEquals(1, trans.ExonUpstream);
        assertEquals(2, trans.ExonDownstream);
        assertEquals(UTR_3P, trans.codingType());
        assertEquals(-2, trans.Phase);
        assertEquals(8, trans.CodingBases);
        assertEquals(8, trans.TotalCodingBases);

        //
        codingStart = 205;
        codingEnd = 505;

        transData = createTransExons(
                geneId, transId++, NEG_STRAND, exonStarts, 10, codingStart, codingEnd, true, BIOTYPE_PROTEIN_CODING);

        position = 250;
        geneNegStrand.setPositionalData(CHR_1, position, POS_ORIENT);
        trans = createBreakendTranscriptData(transData, position, geneNegStrand);

        assertEquals(3, trans.ExonUpstream);
        assertEquals(4, trans.ExonDownstream);
        assertEquals(CODING, trans.codingType());
        assertEquals(PHASE_1, trans.Phase);
        assertEquals(28, trans.CodingBases);
        assertEquals(31, trans.TotalCodingBases);
    }

    @Test
    public void testProteinDomainPositions()
    {
        String geneId = "G0001";
        int transId = 1;

        int[] exonStarts = new int[]{100, 300, 500};

        int codingStart = 150;
        int codingEnd = 550;
        TranscriptData transData = createTransExons(geneId, transId++, POS_STRAND, exonStarts, 100, codingStart, codingEnd, true, "");

        TranscriptProteinData proteinData = new TranscriptProteinData(transId, 0, 0, 5, 55, "hd");

        Integer[] domainPositions = getProteinDomainPositions(proteinData, transData);
        assertEquals(165, (long)domainPositions[SE_START]);
        assertEquals(515, (long)domainPositions[SE_END]);

        // test again with a protein which starts after the first coding exon
        proteinData = new TranscriptProteinData(transId, 0, 0, 55, 65, "hd");

        domainPositions = getProteinDomainPositions(proteinData, transData);
        assertEquals(515, (long)domainPositions[SE_START]);
        assertEquals(545, (long)domainPositions[SE_END]);

        // now on the reverse strand
        proteinData = new TranscriptProteinData(transId, 0, 0, 5, 55, "hd");

        exonStarts = new int[]{100, 300, 500};

        codingStart = 350;
        codingEnd = 550;
        transData = createTransExons(geneId, transId++, NEG_STRAND, exonStarts, 100, codingStart, codingEnd, true, "");

        domainPositions = getProteinDomainPositions(proteinData, transData);
        assertEquals(185, (long)domainPositions[SE_START]);
        assertEquals(535, (long)domainPositions[SE_END]);

        proteinData = new TranscriptProteinData(transId, 0, 0, 55, 65, "hd");

        domainPositions = getProteinDomainPositions(proteinData, transData);
        assertEquals(155, (long)domainPositions[SE_START]);
        assertEquals(185, (long)domainPositions[SE_END]);

    }

}
