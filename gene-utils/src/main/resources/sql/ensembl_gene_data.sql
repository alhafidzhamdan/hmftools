# Extract gene data for Ensembl data cache

select gene.stable_id as GeneId, display_xref.display_label as GeneName, entrez_xref.display_label as EntrezId,
case when entrez_xref.external_db_id = 1100 then 1100 else 0 end as ExtDbId, seq_region.name as Chromosome,
gene.seq_region_strand as Strand, gene.seq_region_start as GeneStart, gene.seq_region_end as GeneEnd,
GROUP_CONCAT(DISTINCT entrez_xref.dbprimary_acc ORDER BY entrez_xref.dbprimary_acc SEPARATOR ';') as EntrezIds,
GROUP_CONCAT(DISTINCT karyotype.band ORDER BY karyotype.band SEPARATOR '-') as KaryotypeBand,
GROUP_CONCAT(DISTINCT syn_xref.dbprimary_acc ORDER BY syn_xref.dbprimary_acc SEPARATOR ';') as Synonyms
from gene
inner join object_xref as ox on gene.gene_id = ox.ensembl_id and ox.ensembl_object_type = 'GENE'
inner join xref as display_xref on display_xref.xref_id = gene.display_xref_id
inner join karyotype on gene.seq_region_id = karyotype.seq_region_id
inner join seq_region on gene.seq_region_id = seq_region.seq_region_id
left join xref as entrez_xref on (entrez_xref.xref_id = ox.xref_id and entrez_xref.external_db_id = 1100)
inner join xref as syn_xref on syn_xref.xref_id = ox.xref_id
where seq_region.name in ('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', 'X', 'Y')
and ((gene.seq_region_start >= karyotype.seq_region_start and gene.seq_region_start <= karyotype.seq_region_end)
or (gene.seq_region_end >= karyotype.seq_region_start and gene.seq_region_end <= karyotype.seq_region_end))
and seq_region.coord_system_id = COORD_SYSTEM
group by GeneId, GeneName, EntrezId, ExtDbId, Chromosome, Strand, GeneStart, GeneEnd
order by GeneName, ExtDbId desc;
