package com.hartwig.hmftools.common.doid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import org.junit.Test;

public class DoidParentsTest {

    @Test
    public void testBehaviour() {
        List<DoidEdge> edges = Lists.newArrayList();
        edges.add(createParent("299", "305"));
        edges.add(createParent("305", "162"));
        edges.add(createEdge("305", "has_a", "162"));

        DoidParents victim = new DoidParents(edges);
        assertEquals(2, victim.size());

        Set<String> parents299 = victim.parents("299");
        assertEquals(2, parents299.size());
        assertTrue(parents299.contains("305"));
        assertTrue(parents299.contains("162"));

        Set<String> parents305 = victim.parents("305");
        assertEquals(1, parents305.size());
        assertTrue(parents305.contains("162"));

    }


    private static DoidEdge createParent(String child, String parent) {
        final String prefix = "http://purl.obolibrary.org/obo/DOID_";
        return createEdge(prefix+ child, "is_a", prefix + parent);
    }

    private static DoidEdge createEdge(String subject, String pred, String object) {
        return ImmutableDoidEdge.builder().subject(subject).predicate(pred).object(object).build();
    }
}
