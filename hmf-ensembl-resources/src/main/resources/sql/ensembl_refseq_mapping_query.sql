select
    gene.stable_id as geneId,
    transcript.stable_id as transcriptId,
    xref.display_label,
    xref.dbprimary_acc,
    transcript.seq_region_start,
    transcript.seq_region_end
from xref
inner join object_xref on xref.xref_id=object_xref.xref_id
inner join transcript on object_xref.ensembl_id=transcript.transcript_id
inner join gene on transcript.gene_id=gene.gene_id
left join xref as entrez_xref on (entrez_xref.xref_id=object_xref.xref_id and entrez_xref.external_db_id = 1801);
