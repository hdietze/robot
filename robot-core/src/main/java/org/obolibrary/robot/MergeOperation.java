package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Merge multiple ontologies into a single ontology.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class MergeOperation {
    /**
     * Logger.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(MergeOperation.class);

    /**
     * Given a single ontology with zero or more imports,
     * add all the imported axioms into the ontology itself,
     * return the modified ontology.
     *
     * @param ontology the ontology to merge
     * @return the new ontology
     */
    public static OWLOntology merge(OWLOntology ontology) {
        List<OWLOntology> ontologies = new ArrayList<OWLOntology>();
        ontologies.add(ontology);
        return merge(ontologies);
    }

    /**
     * Given one or more ontologies,
     * add all their axioms into the first ontology,
     * and return the first ontology.
     * We use a list instead of a set because OWLAPI judges identity
     * simply by the ontology IRI, even if two ontologies have different axioms.
     *
     * @param ontologies the list of ontologies to merge
     * @return the first ontology
     */
    public static OWLOntology merge(List<OWLOntology> ontologies) {
        OWLOntology merged = ontologies.get(0);
        OWLOntologyManager manager = merged.getOWLOntologyManager();
        for (OWLOntology ontology: ontologies) {
            manager.addAxioms(merged, ontology.getAxioms());
        }
        return merged;
    }
}
