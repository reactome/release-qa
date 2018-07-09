package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T062_DuplicatedReferenceEntities;

public class T062_DuplicatedReferenceEntitiesTest extends QACheckReportComparisonTester {

    public T062_DuplicatedReferenceEntitiesTest() {
        super(new T062_DuplicatedReferenceEntities(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();

        // A valid reference entity.
        SchemaClass refEntityCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.ReferenceIsoform);
        GKInstance refEntity = new GKInstance(refEntityCls);
        refEntity.setDbAdaptor(dba);
        fixture.add(refEntity);
        SchemaClass refDbCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDatabase);
        GKInstance refDb = new GKInstance(refDbCls);
        refDb.setDbAdaptor(dba);
        fixture.add(refDb);
        refEntity.addAttributeValue(ReactomeJavaConstants.referenceDatabase, refDb);
        refEntity.addAttributeValue(ReactomeJavaConstants.identifier, "Valid");

        // A redundant reference entity.
        refEntity = new GKInstance(refEntityCls);
        refEntity.setDbAdaptor(dba);
        fixture.add(refEntity);
        refEntity.addAttributeValue(ReactomeJavaConstants.referenceDatabase, refDb);
        refEntity.addAttributeValue(ReactomeJavaConstants.identifier, "Invalid");
        refEntity = new GKInstance(refEntityCls);
        refEntity.setDbAdaptor(dba);
        fixture.add(refEntity);
        refEntity.addAttributeValue(ReactomeJavaConstants.referenceDatabase, refDb);
        refEntity.addAttributeValue(ReactomeJavaConstants.identifier, "Invalid");

        // Valid reference entities distinguished by the ref database.
        refEntity = new GKInstance(refEntityCls);
        refEntity.setDbAdaptor(dba);
        fixture.add(refEntity);
        refEntity.addAttributeValue(ReactomeJavaConstants.referenceDatabase, refDb);
        refEntity.addAttributeValue(ReactomeJavaConstants.identifier, "RefDB");
        refEntity = new GKInstance(refEntityCls);
        refEntity.setDbAdaptor(dba);
        fixture.add(refEntity);
        refDb = new GKInstance(refDbCls);
        refDb.setDbAdaptor(dba);
        fixture.add(refDb);
        refEntity.addAttributeValue(ReactomeJavaConstants.referenceDatabase, refDb);
        refEntity.addAttributeValue(ReactomeJavaConstants.identifier, "RefDB");

        // Valid reference entities distinguished by a variant identifier.
        refEntity = new GKInstance(refEntityCls);
        refEntity.setDbAdaptor(dba);
        fixture.add(refEntity);
        refEntity.addAttributeValue(ReactomeJavaConstants.referenceDatabase, refDb);
        refEntity.addAttributeValue(ReactomeJavaConstants.identifier, "Variant");
        refEntity.addAttributeValue(ReactomeJavaConstants.variantIdentifier, "This");
        refEntity = new GKInstance(refEntityCls);
        refEntity.setDbAdaptor(dba);
        fixture.add(refEntity);
        refEntity.addAttributeValue(ReactomeJavaConstants.referenceDatabase, refDb);
        refEntity.addAttributeValue(ReactomeJavaConstants.identifier, "Variant");
        refEntity.addAttributeValue(ReactomeJavaConstants.variantIdentifier, "That");

        return fixture;
      }

}
