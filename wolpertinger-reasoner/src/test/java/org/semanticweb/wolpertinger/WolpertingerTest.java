package org.semanticweb.wolpertinger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 * Unit test for simple App.
 */
public class WolpertingerTest 
    extends TestCase
{
	private static String PREFIX = "http://www.semanticweb.org/wolpertinger";
	
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public WolpertingerTest( String testName )
    {
        super( testName );
    }
    
    @Override
    protected void setUp() throws Exception {
    	super.setUp();
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( WolpertingerTest.class );
    }
    
    /**
     * Atomic clash
     */
    public void testUnsatifiabilityDueToClashInABoxAssertions() {
    	OWLDataFactory factory = OWLManager.getOWLDataFactory();
    	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    	
    	OWLClassExpression expr1 = factory.getOWLClass(IRI.create(String.format("%s#%s", PREFIX, "A")));
    	OWLClassExpression expr2 = factory.getOWLObjectComplementOf(expr1);
    	OWLNamedIndividual indiv = factory.getOWLNamedIndividual(IRI.create(String.format("%s#%s", PREFIX, "a")));
    	
    	OWLIndividualAxiom fact1 = factory.getOWLClassAssertionAxiom(expr1, indiv);
    	OWLIndividualAxiom fact2 = factory.getOWLClassAssertionAxiom(expr2, indiv);
    	
    	try {
			OWLOntology ontology = manager.createOntology();
			manager.addAxiom(ontology, fact1);
			manager.addAxiom(ontology, fact2);
			
			Wolpertinger wolpertinger = new Wolpertinger(ontology);
			
			assertFalse(wolpertinger.isConsistent());
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			fail();
		}
    }
    
    public void testUnsatisfiabilityDuetoSimpleSubsumptionViolation() {
    	OWLDataFactory factory = OWLManager.getOWLDataFactory();
    	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    	
    	OWLClassExpression classA = factory.getOWLClass(IRI.create(String.format("%s#%s", PREFIX, "A")));
    	OWLClassExpression classB = factory.getOWLClass(IRI.create(String.format("%s#%s", PREFIX, "B")));
    	OWLClassExpression complClassB = factory.getOWLObjectComplementOf(classB);
    	
    	OWLNamedIndividual indiv = factory.getOWLNamedIndividual(IRI.create(String.format("%s#%s", PREFIX, "a")));
    	
    	OWLIndividualAxiom fact1 = factory.getOWLClassAssertionAxiom(classA, indiv);
    	OWLIndividualAxiom fact2 = factory.getOWLClassAssertionAxiom(complClassB, indiv);
    	
    	OWLSubClassOfAxiom subClOf = factory.getOWLSubClassOfAxiom(classA, classB);
    	
    	try {
			OWLOntology ontology = manager.createOntology();
			manager.addAxiom(ontology, fact1);
			manager.addAxiom(ontology, fact2);
			manager.addAxiom(ontology, subClOf);
			
			Wolpertinger wolpertinger = new Wolpertinger(ontology);
			
			assertFalse(wolpertinger.isConsistent());
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			fail();
		}
    }
    
    /**
     * Smth like:
     *    A subClassOf B
     *    A subClassOf C
     *    C disjoint with B
     *    ...
     */
    public void testUnsatisfiabilityDueToConflictingAxioms1() {
    	OWLDataFactory factory = OWLManager.getOWLDataFactory();
    	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    	
    	OWLClassExpression classA = factory.getOWLClass(IRI.create(String.format("%s#%s", PREFIX, "A")));
    	OWLClassExpression classB = factory.getOWLClass(IRI.create(String.format("%s#%s", PREFIX, "B")));
    	OWLClassExpression classC = factory.getOWLClass(IRI.create(String.format("%s#%s", PREFIX, "C")));
    	OWLNamedIndividual indiv = factory.getOWLNamedIndividual(IRI.create(String.format("%s#%s", PREFIX, "a")));
    	
    	OWLIndividualAxiom fact1 = factory.getOWLClassAssertionAxiom(classA, indiv);
    	OWLSubClassOfAxiom axmAsubB = factory.getOWLSubClassOfAxiom(classA, classB);
    	OWLSubClassOfAxiom axmAsubC = factory.getOWLSubClassOfAxiom(classA, classC);
    	OWLDisjointClassesAxiom axmBdisC = factory.getOWLDisjointClassesAxiom(classB, classC);
    	
    	try {
			OWLOntology ontology = manager.createOntology();
			manager.addAxiom(ontology, fact1);
			manager.addAxiom(ontology, axmAsubB);
			manager.addAxiom(ontology, axmAsubC);
			manager.addAxiom(ontology, axmBdisC);
			
			Wolpertinger wolpertinger = new Wolpertinger(ontology);
			
			assertFalse(wolpertinger.isConsistent());
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			fail();
		}
    }
    
    /**
     * Smth like
     *    A subClassOf r min 5 B
     *  But we have only a domain with 4 elements ...
     */
    public void testUnsatisfiabilityDoToFixedDomain1() {
    	assertTrue(false);
    }
    
    public void testUnsatisfiabilityDoToFixedDomain2() {
    	assertTrue(false);
    }
    
    public void testSimple3ColoringHas4AnswerSets() {
    	assertTrue(false);
    }
    
    

}
