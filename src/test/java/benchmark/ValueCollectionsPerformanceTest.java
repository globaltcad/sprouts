package benchmark;
import sprouts.Association;
import sprouts.Pair;
import sprouts.ValueSet;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ValueCollectionsPerformanceTest {

    private static final long PRIME_1 = 12055296811267L;
    private static final long PRIME_2 = 53982894593057L;


    enum Operation {
        ADD, REMOVE, GET
    }

    static class OperationKeyPair {
        final Operation operation;
        final String key;

        OperationKeyPair(Operation operation, String key) {
            this.operation = operation;
            this.key = key;
        }
    }

    public static void main(String[] args) {
        //testAssociationAgainstHashMap();
        testValueSetAgainstHashSet();
    }

    private static void testAssociationAgainstHashMap() {
        List<OperationKeyPair> operations = generateOperations();
        test(
                "Association", ()->Association.between(String.class, String.class), assoc-> {
                    Association<String, String> result = assoc;
                    for (OperationKeyPair pair : operations) {
                        switch (pair.operation) {
                            case ADD:
                                result = result.put(pair.key, "value of " + pair.key);
                                break;
                            case REMOVE:
                                result = result.remove(pair.key);
                                break;
                            case GET:
                                result.get(pair.key);
                                break;
                        }
                    }
                    return result;
                },
                "HashMap", ()->new HashMap<String,String>(),
                map->{
                    for (OperationKeyPair pair : operations) {
                        switch (pair.operation) {
                            case ADD:
                                map.put(pair.key, "value of " + pair.key);
                                break;
                            case REMOVE:
                                map.remove(pair.key);
                                break;
                            case GET:
                                map.get(pair.key);
                                break;
                        }
                    }
                    return map;
                }
        );
    }

    private static void testValueSetAgainstHashSet() {
        List<OperationKeyPair> operations = generateOperations();
        test(
                "ValueSet", ()-> ValueSet.of(String.class), assoc-> {
                    ValueSet<String> result = assoc;
                    for (OperationKeyPair pair : operations) {
                        switch (pair.operation) {
                            case ADD:
                                result = result.add(pair.key);
                                break;
                            case REMOVE:
                                result = result.remove(pair.key);
                                break;
                            case GET:
                                boolean contains = result.contains(pair.key);
                                break;
                        }
                    }
                    return result;
                },
                "HashSet", ()->new HashSet<String>(),
                map->{
                    for (OperationKeyPair pair : operations) {
                        switch (pair.operation) {
                            case ADD:
                                map.add(pair.key);
                                break;
                            case REMOVE:
                                map.remove(pair.key);
                                break;
                            case GET:
                                boolean contains = map.contains(pair.key);
                                break;
                        }
                    }
                    return map;
                }
        );
    }

    public static <A,B> void test(
        String titleA, Supplier<A> initA, Function<A,A> runA,
        String titleB, Supplier<B> initB, Function<B,B> runB
    ) {

        // Warmup to trigger JIT compilation
        run(3, ()-> Pair.of(initA.get(), initB.get()), pair-> Pair.of(runA.apply(pair.first()), runB.apply(pair.second())));

        long samples = 10;
        long associationTime = 0;
        long mapTime = 0;

        for (int i = 0; i < samples; i++) {
            // Garbage collect before measurements
            System.gc();
            // Measure Association performance
            associationTime += run(11, initA, runA);

            // Garbage collect before next measurement
            System.gc();
            // Measure Map performance
            mapTime += run(11, initB, runB);
        }
        associationTime /= samples;
        mapTime /= samples;
        double associationTimeSeconds = associationTime / 1_000_000_000.0;
        double mapTimeSeconds = mapTime / 1_000_000_000.0;

        System.out.println(titleA+" total time: " + associationTimeSeconds + " s");
        System.out.println(titleB+" total time: " + mapTimeSeconds + " s");
        System.out.println("Factor: " + associationTimeSeconds / mapTimeSeconds + "x");
    }


    private static List<OperationKeyPair> generateOperations() {
        List<OperationKeyPair> operations = new ArrayList<>();
        for (int i = 0; i <= 500_000; i++) {
            int localHash = Math.abs(Long.hashCode(PRIME_1 * (i - PRIME_2 * (i))));
            Operation op = Operation.values()[localHash % 3];
            operations.add(new OperationKeyPair(op, String.valueOf(localHash)));
        }
        return operations;
    }

    private static <T> long run(int count, Supplier<T> init, Function<T,T> ops) {
        T current = init.get();

        long startTime = System.nanoTime();
        for (int i = 0; i < count; i++) {
            current = ops.apply(current);
        }
        return System.nanoTime() - startTime;
    }


}
