package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T058_ComplexesWhereCompartmentDoesNotMatchWithAnyOfTheParticipants;

public class T058_ComplexesWhereCompartmentDoesNotMatchWithAnyOfTheParticipantsTest
extends QACheckReportComparisonTester {

    public T058_ComplexesWhereCompartmentDoesNotMatchWithAnyOfTheParticipantsTest() {
        super(new T058_ComplexesWhereCompartmentDoesNotMatchWithAnyOfTheParticipants(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();

        // A valid empty complex.
         SchemaClass complexCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.Complex);
         GKInstance complex = new GKInstance(complexCls);
         complex.setDbAdaptor(dba);
         fixture.add(complex);

         // A valid complex whose sole compartment is shared by a component.
         complex = new GKInstance(complexCls);
         complex.setDbAdaptor(dba);
         fixture.add(complex);
         SchemaClass compartmentCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.Compartment);
         GKInstance compartment = new GKInstance(compartmentCls);
         compartment.setDbAdaptor(dba);
         fixture.add(compartment);
         complex.addAttributeValue(ReactomeJavaConstants.compartment, compartment);
         SchemaClass entityCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
         GKInstance component = new GKInstance(entityCls);
         component.setDbAdaptor(dba);
         fixture.add(component);
         complex.addAttributeValue(ReactomeJavaConstants.hasComponent, component);
         component.addAttributeValue(ReactomeJavaConstants.compartment, compartment);

         // An invalid complex whose sole compartment is not shared by a component.
         complex = new GKInstance(complexCls);
         complex.setDbAdaptor(dba);
         fixture.add(complex);
         compartment = new GKInstance(compartmentCls);
         compartment.setDbAdaptor(dba);
         fixture.add(compartment);
         complex.addAttributeValue(ReactomeJavaConstants.compartment, compartment);
         component = new GKInstance(entityCls);
         component.setDbAdaptor(dba);
         fixture.add(component);
         complex.addAttributeValue(ReactomeJavaConstants.hasComponent, component);
         GKInstance otherComponent = new GKInstance(entityCls);
         otherComponent.setDbAdaptor(dba);
         fixture.add(otherComponent);
         GKInstance otherCompartment = new GKInstance(compartmentCls);
         otherCompartment.setDbAdaptor(dba);
         fixture.add(otherCompartment);
         component.addAttributeValue(ReactomeJavaConstants.compartment, otherCompartment);

         // A valid complex with compartment shared by a component member.
         complex = new GKInstance(complexCls);
         complex.setDbAdaptor(dba);
         fixture.add(complex);
         compartment = new GKInstance(compartmentCls);
         compartment.setDbAdaptor(dba);
         fixture.add(compartment);
         complex.addAttributeValue(ReactomeJavaConstants.compartment, compartment);
         SchemaClass definedSetCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.DefinedSet);
         component = new GKInstance(definedSetCls);
         component.setDbAdaptor(dba);
         fixture.add(component);
         complex.addAttributeValue(ReactomeJavaConstants.hasComponent, component);
         GKInstance member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         component.addAttributeValue(ReactomeJavaConstants.hasMember, member);
         member.addAttributeValue(ReactomeJavaConstants.compartment, compartment);

         // A valid complex with compartment shared by a component candidate.
         complex = new GKInstance(complexCls);
         complex.setDbAdaptor(dba);
         fixture.add(complex);
         compartment = new GKInstance(compartmentCls);
         compartment.setDbAdaptor(dba);
         fixture.add(compartment);
         complex.addAttributeValue(ReactomeJavaConstants.compartment, compartment);
         SchemaClass candidateSetCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.CandidateSet);
         component = new GKInstance(candidateSetCls);
         component.setDbAdaptor(dba);
         fixture.add(component);
         complex.addAttributeValue(ReactomeJavaConstants.hasComponent, component);
         member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         component.addAttributeValue(ReactomeJavaConstants.hasCandidate, member);
         member.addAttributeValue(ReactomeJavaConstants.compartment, compartment);

         // A valid complex with compartment shared by a component repeated unit.
         complex = new GKInstance(complexCls);
         complex.setDbAdaptor(dba);
         fixture.add(complex);
         compartment = new GKInstance(compartmentCls);
         compartment.setDbAdaptor(dba);
         fixture.add(compartment);
         complex.addAttributeValue(ReactomeJavaConstants.compartment, compartment);
         SchemaClass polymerCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.Polymer);
         component = new GKInstance(polymerCls);
         component.setDbAdaptor(dba);
         fixture.add(component);
         complex.addAttributeValue(ReactomeJavaConstants.hasComponent, component);
         member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         component.addAttributeValue(ReactomeJavaConstants.repeatedUnit, member);
         member.addAttributeValue(ReactomeJavaConstants.compartment, compartment);

         // A valid complex with compartment shared very indirectly.
         complex = new GKInstance(complexCls);
         complex.setDbAdaptor(dba);
         fixture.add(complex);
         compartment = new GKInstance(compartmentCls);
         compartment.setDbAdaptor(dba);
         fixture.add(compartment);
         complex.addAttributeValue(ReactomeJavaConstants.compartment, compartment);
         component = new GKInstance(candidateSetCls);
         component.setDbAdaptor(dba);
         fixture.add(component);
         complex.addAttributeValue(ReactomeJavaConstants.hasComponent, component);
         otherComponent = new GKInstance(entityCls);
         otherComponent.setDbAdaptor(dba);
         fixture.add(otherComponent);
         complex.addAttributeValue(ReactomeJavaConstants.hasComponent, otherComponent);
         member = new GKInstance(definedSetCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         component.addAttributeValue(ReactomeJavaConstants.hasMember, member);
         otherCompartment = new GKInstance(compartmentCls);
         otherCompartment.setDbAdaptor(dba);
         fixture.add(otherCompartment);
         member.addAttributeValue(ReactomeJavaConstants.compartment, otherCompartment);
         GKInstance candidateSet = new GKInstance(candidateSetCls);
         candidateSet.setDbAdaptor(dba);
         fixture.add(candidateSet);
         member.addAttributeValue(ReactomeJavaConstants.hasMember, candidateSet);
         member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         component.addAttributeValue(ReactomeJavaConstants.hasCandidate, member);
         member.addAttributeValue(ReactomeJavaConstants.compartment, compartment);

         return fixture;
      }

}
