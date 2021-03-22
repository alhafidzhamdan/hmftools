DROP TABLE IF EXISTS germlineVariant2;
CREATE TABLE germlineVariant2
(   id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    modified DATETIME NOT NULL,
    sampleId varchar(255) NOT NULL,
    chromosome varchar(255) NOT NULL,
    position int not null,
    filter varchar(255) NOT NULL,
    type varchar(255) NOT NULL,
    ref varchar(255) NOT NULL,
    alt varchar(255) NOT NULL,

    ## SAGE
    qual double precision not null,
    tier varchar(20) NOT NULL,
    germlineGenotype varchar(255) NOT NULL,
    germlineAlleleReadCount int,
    germlineTotalReadCount int,
    rnaAlleleReadCount int,
    rnaTotalReadCount int,
    tumorAlleleReadCount int NOT NULL,
    tumorTotalReadCount int NOT NULL,
    localPhaseSet int,

    ### PURPLE ENRICHMENT
    adjustedVaf DOUBLE PRECISION NOT NULL,
    variantCopyNumber DOUBLE PRECISION NOT NULL,
    copyNumber DOUBLE PRECISION NOT NULL,
    biallelic BOOLEAN NOT NULL,
    minorAlleleCopyNumber DOUBLE PRECISION NOT NULL,

    ### PATHOGENIC
    clinvarInfo varchar(255) NOT NULL,
    pathogenicity varchar(255) NOT NULL,
    pathogenic BOOLEAN NOT NULL,

    ## SNP EFF ENRICHMENT
    gene varchar(255) NOT NULL,
    genesEffected int not null,
    worstEffectTranscript varchar(255) NOT NULL,
    worstEffect varchar(255) NOT NULL,
    worstCodingEffect varchar(255) NOT NULL,
    canonicalEffect varchar(255) NOT NULL,
    canonicalCodingEffect varchar(255) NOT NULL,
    canonicalHgvsCodingImpact varchar(255) NOT NULL,
    canonicalHgvsProteinImpact varchar(255) NOT NULL,

    ### REF GENOME ENRICHMENT
    microhomology varchar(255) NOT NULL,
    repeatSequence varchar(255) NOT NULL,
    repeatCount int NOT NULL,
    trinucleotideContext varchar(3) NOT NULL,

    ### DRIVER CATALOG
    hotspot varchar(20) NOT NULL,
    mappability DOUBLE PRECISION NOT NULL,
    reported BOOLEAN NOT NULL,

    ### NOT USED
    #recovered BOOLEAN NOT NULL,
    #germlineStatus varchar(255) NOT NULL,
    #kataegis varchar(20) NOT NULL,
    #localRealignmentSet int,
    #phasedInframeIndel int,
    #subclonalLikelihood DOUBLE PRECISION NOT NULL,

    PRIMARY KEY (id),
    INDEX(sampleId),
    INDEX(filter),
    INDEX(type),
    INDEX(gene)
);