package benchmark;
import sprouts.Association;
import sprouts.Pair;
import sprouts.Tuple;
import sprouts.ValueSet;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ValueCollectionsPerformanceTest {

    private static final long PRIME_1 = 12055296811267L;
    private static final long PRIME_2 = 53982894593057L;


    enum Operation {
        ADD, REMOVE, GET, ITERATE, STREAM;

        static Operation of(double numberBetweenZeroAndOne) {
            if (numberBetweenZeroAndOne < 0.3) {
                return ADD; // 30% chance to add an item
            } else if (numberBetweenZeroAndOne < 0.6) {
                return REMOVE; // 30% chance to remove an item
            } else if (numberBetweenZeroAndOne < 0.99) {
                return GET; // 39% chance to get an item
            } else if (numberBetweenZeroAndOne < 0.999) {
                return ITERATE; // 0.9% chance to iterate over items
            } else {
                return STREAM; // 0.1% chance to stream over items
            }
        }
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
        testAssociationAgainstHashMap(70_000);
        testSortedAssociationAgainstTreeMap(70_000);
        testLinkedAssociationAgainstLinkedHashMap(70_000);
        testValueSetAgainstHashSet(70_000);
        testSortedValueSetAgainstTreeSet(70_000);
        testLinkedValueSetAgainstLinkedHashMap(70_000);
        testTupleAgainstArrayList(60_000, 70_000);
        testTupleAgainstArrayList(30_000, 70_000);
        testTupleAgainstArrayList(10_000, 70_000);
        testTupleAgainstArrayList(5_000, 70_000);
        testTupleAgainstArrayList(100, 70_000);
    }

    private static void testAssociationAgainstHashMap(int numberOfOperations) {
        List<OperationKeyPair> operations = generateOperations(numberOfOperations);
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
                            case ITERATE:
                                int sum = 0;
                                for (Pair<String, String> entry : result) {
                                    sum += entry.first().hashCode() + entry.second().hashCode();
                                }
                                break;
                            case STREAM:
                                int sum2 = result.entrySet().stream().mapToInt(entry -> entry.first().hashCode() * entry.second().hashCode()).filter(it -> it > 0).sum();
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
                            case ITERATE:
                                int sum = 0;
                                for (Map.Entry<String, String> entry : map.entrySet()) {
                                    sum += entry.getKey().hashCode() + entry.getValue().hashCode();
                                }
                                break;
                            case STREAM:
                                int sum2 = map.entrySet().stream().mapToInt(entry -> entry.getKey().hashCode() * entry.getValue().hashCode()).filter(it -> it > 0).sum();
                                break;
                        }
                    }
                    return map;
                },
                (a, b)-> a.toMap().equals(b)
        );
    }

    private static void testSortedAssociationAgainstTreeMap(int numberOfOperations) {
        List<OperationKeyPair> operations = generateOperations(numberOfOperations);
        test(
                "SortedAssociation", ()->Association.betweenSorted(String.class, String.class), assoc-> {
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
                            case ITERATE:
                                int sum = 0;
                                for (Pair<String, String> entry : result) {
                                    sum += entry.first().hashCode() + entry.second().hashCode();
                                }
                                break;
                            case STREAM:
                                int sum2 = result.entrySet().stream().mapToInt(entry -> entry.first().hashCode() * entry.second().hashCode()).filter(it -> it > 0).sum();
                                break;
                        }
                    }
                    return result;
                },
                "TreeMap", ()->new TreeMap<String,String>(String::compareTo),
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
                            case ITERATE:
                                int sum = 0;
                                for (Map.Entry<String, String> entry : map.entrySet()) {
                                    sum += entry.getKey().hashCode() + entry.getValue().hashCode();
                                }
                                break;
                            case STREAM:
                                int sum2 = map.entrySet().stream().mapToInt(entry -> entry.getKey().hashCode() * entry.getValue().hashCode()).filter(it -> it > 0).sum();
                                break;
                        }
                    }
                    return map;
                },
                (a, b)-> a.toMap().equals(b)
        );
    }

    private static void testLinkedAssociationAgainstLinkedHashMap(int numberOfOperations) {
        List<OperationKeyPair> operations = generateOperations(numberOfOperations);
        test(
                "LinkedAssociation", ()->Association.betweenLinked(String.class, String.class), assoc-> {
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
                            case ITERATE:
                                int sum = 0;
                                for (Pair<String, String> entry : result) {
                                    sum += entry.first().hashCode() + entry.second().hashCode();
                                }
                                break;
                            case STREAM:
                                int sum2 = result.entrySet().stream().mapToInt(entry -> entry.first().hashCode() * entry.second().hashCode()).filter(it -> it > 0).sum();
                                break;
                        }
                    }
                    return result;
                },
                "LinkedHashMap", ()->new LinkedHashMap<String, String>(),
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
                            case ITERATE:
                                int sum = 0;
                                for (Map.Entry<String, String> entry : map.entrySet()) {
                                    sum += entry.getKey().hashCode() + entry.getValue().hashCode();
                                }
                                break;
                            case STREAM:
                                int sum2 = map.entrySet().stream().mapToInt(entry -> entry.getKey().hashCode() * entry.getValue().hashCode()).filter(it -> it > 0).sum();
                                break;
                        }
                    }
                    return map;
                },
                (a, b)-> a.toMap().equals(b)
        );
    }

    private static void testValueSetAgainstHashSet(int numberOfOperations) {
        List<OperationKeyPair> operations = generateOperations(numberOfOperations);
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
                            case ITERATE:
                                int sum = 0;
                                for (String value : result) {
                                    sum += value.hashCode();
                                }
                                break;
                            case STREAM:
                                int prod = result.stream().map(String::hashCode).filter(it->it>0).mapToInt(i->i).sum();
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
                            case ITERATE:
                                int sum = 0;
                                for (String value : map) {
                                    sum += value.hashCode();
                                }
                                break;
                            case STREAM:
                                int sum2 = map.stream().map(String::hashCode).filter(it->it>0).mapToInt(i->i).sum();
                                break;
                        }
                    }
                    return map;
                },
                (a, b)->a.toSet().equals(b)
        );
    }

    private static void testSortedValueSetAgainstTreeSet(int numberOfOperations) {
        List<OperationKeyPair> operations = generateOperations(numberOfOperations);
        test(
                "SortedValueSet", ()-> ValueSet.ofSorted(String.class), assoc-> {
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
                            case ITERATE:
                                int sum = 0;
                                for (String value : result) {
                                    sum += value.hashCode();
                                }
                                break;
                            case STREAM:
                                int prod = result.stream().map(String::hashCode).filter(it->it>0).mapToInt(i->i).sum();
                                break;
                        }
                    }
                    return result;
                },
                "TreeSet", ()->new TreeSet<String>(String::compareTo),
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
                            case ITERATE:
                                int sum = 0;
                                for (String value : map) {
                                    sum += value.hashCode();
                                }
                                break;
                            case STREAM:
                                int sum2 = map.stream().map(String::hashCode).filter(it->it>0).mapToInt(i->i).sum();
                                break;
                        }
                    }
                    return map;
                },
                (a, b)->a.toSet().equals(b)
        );
    }

    private static void testLinkedValueSetAgainstLinkedHashMap(int numberOfOperations) {
        List<OperationKeyPair> operations = generateOperations(numberOfOperations);
        test(
                "LinkedValueSet", ()-> ValueSet.ofLinked(String.class), assoc-> {
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
                            case ITERATE:
                                int sum = 0;
                                for (String value : result) {
                                    sum += value.hashCode();
                                }
                                break;
                            case STREAM:
                                int prod = result.stream().map(String::hashCode).filter(it->it>0).mapToInt(i->i).sum();
                                break;
                        }
                    }
                    return result;
                },
                "TreeSet", ()->new TreeSet<String>(String::compareTo),
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
                            case ITERATE:
                                int sum = 0;
                                for (String value : map) {
                                    sum += value.hashCode();
                                }
                                break;
                            case STREAM:
                                int sum2 = map.stream().map(String::hashCode).filter(it->it>0).mapToInt(i->i).sum();
                                break;
                        }
                    }
                    return map;
                },
                (a, b)->a.toSet().equals(b)
        );
    }

    private static void testTupleAgainstArrayList(int initialSize, int numberOfOperations) {
        List<OperationKeyPair> operations = generateOperations(numberOfOperations);
        IntFunction<Integer> randomIndexSource = size-> {
            return size == 0 ? 0 : new Random(size).nextInt(size);
        };
        List<String> initialList = IntStream.range(0, initialSize).mapToObj(i->String.valueOf(i)).collect(Collectors.toList());
        test(
                "Tuple - init "+initialSize, ()-> Tuple.of(String.class, initialList), tuple-> {
                    Tuple<String> result = tuple;
                    for (OperationKeyPair pair : operations) {
                        switch (pair.operation) {
                            case ADD:
                                result = result.addAt(randomIndexSource.apply(result.size()), pair.key);
                                break;
                            case REMOVE:
                                if ( result.size() > 0 ) {
                                    result = result.removeAt(randomIndexSource.apply(result.size()));
                                }
                                break;
                            case GET:
                                if ( result.size() > 0 ) {
                                    String value = result.get(randomIndexSource.apply(result.size()));
                                }
                                break;
                            case ITERATE:
                                int sum = 0;
                                for (String value : result) {
                                    sum += value.hashCode();
                                }
                                break;
                            case STREAM:
                                int prod = result.parallelStream().map(String::hashCode).filter(it->it>0).mapToInt(i->i).sum();
                                break;
                        }
                    }
                    return result;
                },
                "ArrayList - init "+initialSize, ()->new ArrayList<String>(initialList),
                list->{
                    for (OperationKeyPair pair : operations) {
                        switch (pair.operation) {
                            case ADD:
                                list.add(randomIndexSource.apply(list.size()), pair.key);
                                break;
                            case REMOVE:
                                if ( list.size() > 0 ) {
                                    list.remove(randomIndexSource.apply(list.size()).intValue());
                                }
                                break;
                            case GET:
                                if ( list.size() > 0 ) {
                                    String value = list.get(randomIndexSource.apply(list.size()));
                                }
                                break;
                            case ITERATE:
                                int sum = 0;
                                for (String value : list) {
                                    sum += value.hashCode();
                                }
                                break;
                            case STREAM:
                                int prod = list.parallelStream().map(String::hashCode).filter(it->it>0).mapToInt(i->i).sum();
                                break;
                        }
                    }
                    return list;
                },
                (a, b) -> a.toList().equals(b)
        );
    }

    public static <A,B> void test(
        String titleA, Supplier<A> initA, Function<A,A> runA,
        String titleB, Supplier<B> initB, Function<B,B> runB,
        BiPredicate<A,B> invarianceChecker
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
            Pair<A, Long> currentAAndTime = run(11, initA, runA);
            associationTime += currentAAndTime.second();

            // Garbage collect before next measurement
            System.gc();
            // Measure Map performance
            Pair<B, Long> currentBAndTime = run(11, initB, runB);
            mapTime += currentBAndTime.second();

            if ( !invarianceChecker.test(currentAAndTime.first(), currentBAndTime.first())) {
                throw new IllegalStateException("Invariance check failed");
            }
        }
        associationTime /= samples;
        mapTime /= samples;
        double associationTimeSeconds = associationTime / 1_000_000_000.0;
        double mapTimeSeconds = mapTime / 1_000_000_000.0;

        System.out.println(titleA+" total time: " + associationTimeSeconds + " s");
        System.out.println(titleB+" total time: " + mapTimeSeconds + " s");
        System.out.println("Factor: " + associationTimeSeconds / mapTimeSeconds + "x\n");
    }


    private static List<OperationKeyPair> generateOperations(int numberOfOperations) {
        List<OperationKeyPair> operations = new ArrayList<>();
        Random random = new Random(PRIME_1);
        for (int i = 0; i < numberOfOperations; i++) {
            int localHash = Math.abs(Long.hashCode(PRIME_1 * (i - PRIME_2 * (i))));
            Operation op = Operation.of(random.nextDouble());
            operations.add(new OperationKeyPair(op, String.valueOf(localHash)));
        }
        return operations;
    }

    private static <T> Pair<T,Long> run(int count, Supplier<T> init, Function<T,T> ops) {
        T current = init.get();

        long startTime = System.nanoTime();
        for (int i = 0; i < count; i++) {
            current = ops.apply(current);
        }
        return Pair.of(current, System.nanoTime() - startTime);
    }


}
