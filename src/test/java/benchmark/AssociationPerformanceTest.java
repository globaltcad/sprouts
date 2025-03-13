package benchmark;
import sprouts.Association;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssociationPerformanceTest {

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
        List<OperationKeyPair> operations = generateOperations();

        // Warmup to trigger JIT compilation
        warmup(operations);

        // Garbage collect before measurements
        System.gc();

        // Measure Association performance
        long associationTime = measureAssociationPerformance(operations);

        // Garbage collect before next measurement
        System.gc();

        // Measure Map performance
        long mapTime = measureMapPerformance(operations);

        double associationTimeSeconds = associationTime / 1_000_000_000.0;
        double mapTimeSeconds = mapTime / 1_000_000_000.0;

        System.out.println("Association total time: " + associationTimeSeconds + " s");
        System.out.println("HashMap total time: " + mapTimeSeconds + " s");
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

    private static void warmup(List<OperationKeyPair> operations) {
        for (int i = 0; i < 3; i++) {
            Association<String, String> assoc = Association.between(String.class, String.class);
            applyAssociationOperations(assoc, operations);

            Map<String, String> map = new HashMap<>();
            applyMapOperations(map, operations);
        }
    }

    private static long measureAssociationPerformance(List<OperationKeyPair> operations) {
        Association<String, String> current = Association.between(String.class, String.class);

        long startTime = System.nanoTime();
        for (int i = 0; i < 11; i++) { // 1 initial + 10 iterations
            current = applyAssociationOperations(current, operations);
        }
        return System.nanoTime() - startTime;
    }

    private static long measureMapPerformance(List<OperationKeyPair> operations) {
        Map<String, String> map = new HashMap<>();

        long startTime = System.nanoTime();
        for (int i = 0; i < 11; i++) { // 1 initial + 10 iterations
            applyMapOperations(map, operations);
        }
        return System.nanoTime() - startTime;
    }

    private static Association<String, String> applyAssociationOperations(
            Association<String, String> current,
            List<OperationKeyPair> operations
    ) {
        Association<String, String> result = current;
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
    }

    private static void applyMapOperations(
            Map<String, String> map,
            List<OperationKeyPair> operations
    ) {
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
    }
}
