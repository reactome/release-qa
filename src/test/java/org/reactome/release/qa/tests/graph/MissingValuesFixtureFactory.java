package org.reactome.release.qa.tests.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.SchemaClass;

public class MissingValuesFixtureFactory {

    private String[] attNames;
    private SchemaClass schemaCls;
    private MySQLAdaptor dba;

    public MissingValuesFixtureFactory(MySQLAdaptor dba, String clsName, String... attNames) {
        this.dba = dba;
        this.schemaCls = dba.getSchema().getClassByName(clsName);
        this.attNames = attNames;
    }

    /**
     * Creates the test fixture with an instance for each combination
     * of value settings.
     * 
     * @param values the values for each attribute
     * @return the test fixture instances
     * @throws InvalidAttributeValueException 
     * @throws InvalidAttributeException 
     */
    protected List<Instance> createTestFixture(Object... values)
            throws InvalidAttributeException, InvalidAttributeValueException {
        List<Instance> fixture = new ArrayList<Instance>();
        // Add value instances to the fixture.
        for (Object value: values) {
            if (value instanceof Instance && !fixture.contains(value)) {
                fixture.add((Instance)value);
            }
        }
        for (BitSet indexes: permutations(values.length)) {
            GKInstance instance = createTestInstance(values, indexes);
            fixture.add(instance);
        }
        return fixture;
    }

    private Collection<BitSet> permutations(int n) {
        if (n == 0) {
            return new ArrayList<BitSet>();
        }
        BitSet bs = new BitSet(n);
        bs.set(0, n);
        return permutations(bs);
    }

    private static Collection<BitSet> permutations(BitSet base) {
        // Collect the other permutations.
        Set<BitSet> bitsets = base.stream()
                .mapToObj(i -> clear(base, i))
                .map(MissingValuesFixtureFactory::permutations)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        // Add the bitset with all indexes set to the others.
        bitsets.add(base);
        return bitsets;
    }

    private static BitSet clear(BitSet bs, int i) {
        BitSet copy = (BitSet) bs.clone();
        copy.clear(i);
        return copy;
    }

    private GKInstance createTestInstance(Object[] values, BitSet indexes)
            throws InvalidAttributeException, InvalidAttributeValueException {
        // Make the instance.
        GKInstance instance = new GKInstance(schemaCls);
        instance.setDbAdaptor(dba);
        
        // Fill the instance.
        for (int i: indexes.stream().toArray()) {
            instance.setAttributeValue(attNames[i], values[i]);
        }
        
        return instance;
    }

}
