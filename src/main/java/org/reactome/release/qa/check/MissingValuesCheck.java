package org.reactome.release.qa.check;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.Instance;

abstract public class MissingValuesCheck extends MissingValueCheck {

    private String[] attNames;

    public MissingValuesCheck(String clsName, String ...attNames) {
        super(clsName, attNames[0]);
        this.attNames = attNames;
    }

    protected String getDescription(String conjunction) {
        String[] restAttNames = Arrays.copyOfRange(attNames, 1, attNames.length);
        String delim = " " + conjunction + " ";
        String suffix = Stream.of(restAttNames).collect(Collectors.joining(delim));
        return super.getDisplayName() + " " + suffix;
    }

    public String[] getAttributeNames() {
        return attNames;
    }

    /**
     * Creates the test fixture with an instance for each
     * combination of value settings. If a value is an
     * {@link Instance}, then the caller is responsible
     * for adding that instance to the fixture if it
     * should be deleted after the test is run.
     * 
     * @param values the values for each attribute
     * @return the test fixture instances
     */
    protected List<Instance> createTestFixture(Object ...values) {
        return permutations(values.length)
                .stream()
                .map(bs -> createTestInstance(values, bs))
                .collect(Collectors.toList());
    }

    private Collection<BitSet> permutations(int n) {
        BitSet bs = new BitSet(n);
        bs.set(0, n);
        return permutations(bs);
    }

    private static Collection<BitSet> permutations(BitSet bs) {
        // Collect the other permutations.
        Set<BitSet> bitsets = bs.stream()
                .mapToObj(i -> clear(bs, i))
                .map(MissingValuesCheck::permutations)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        // Add this bitset to the others.
        bitsets.add(bs);
        return bitsets;
    }

    private static BitSet clear(BitSet bs, int i) {
        BitSet copy = (BitSet) bs.clone();
        copy.clear(i);
        return copy;
    }

    private Instance createTestInstance(Object[] values, BitSet indexes) {
        Instance inst = createInstance(getSchemaClassName());
        indexes.stream()
                .forEach(i -> setAttributeValue(inst, attNames[i], values[i]));
        return inst;
    }

}
