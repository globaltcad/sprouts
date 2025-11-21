package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.Month
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

@Title("Sorted Association - a Data Oriented Mapping")
@Narrative('''

    Sprouts is a property library with a heavy focus on bridging the
    gap between classical place oriented programming and modern
    data oriented programming. 
    This is why sprouts has both the `Tuple` and `Association` classes.
    The `Association` class is a value object that represents a set of
    key-value pairs. You can think of it as an immutable map, but with
    an API that is designed around transforming the data in the map
    rather than mutating it.
    
    A regular `Association` is unordered, meaning that the order of the
    key-value pairs is not guaranteed. But if you use special
    factory methods, you can create a `SortedAssociation` that
    guarantees the order of the key-value pairs.
    Under the hood, this key-value store is based on a binary tree
    with a node size growing with the depth of the tree.

''')
@Subject([Association])
class Sorted_Association_Spec extends Specification
{
    enum Operation {
        ADD, REMOVE,
        REPLACE,  // Replace existing key's value
        PUT_IF_ABSENT, // Add only if absent
        CLEAR     // Clear all entries
    }

    def 'An empty association is created by supplying the type of the key and value'() {
        given:
            var association = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )

        expect:
            association.isSorted()
            association.isEmpty()
            !association.isNotEmpty()
            association.keyType() == String
            association.valueType() == Integer
    }

    def 'An association is invariant to a map.'(
        List<Tuple2<Operation, String>> operations
    ) {
        given:
            var association = Association.betweenSorted(String, String, (k1, k2) -> k1 <=> k2 )
            var map = [:] as TreeMap
            var operationsApplier = { currentAssociations ->
                operations.each { operation, key ->
                    switch (operation) {
                        case Operation.ADD:
                            currentAssociations = currentAssociations.put(key, "value of " + key)
                            map[key] = "value of " + key
                            if ( !currentAssociations.containsKey(key) ) {
                                throw new IllegalStateException(
                                    "Entry with key '$key' not found in the association " +
                                    "right after it was added with operation.."
                                )
                            }
                            break
                        case Operation.REMOVE:
                            currentAssociations = currentAssociations.remove(key)
                            map.remove(key)
                            break
                    }
                    if ( map.size() != currentAssociations.size() ) {
                        throw new IllegalStateException(
                            "The size of the map and the association are not equal after key '$key' was introduced "+
                            "with operation '$operation'. Map size: ${map.size()}, Association size: ${currentAssociations.size()}"
                        )
                    }
                }
                return currentAssociations
            }
        when : 'We apply the operations to the associations and the map for the first time.'
            association = operationsApplier(association) as Association<String, String>
        then: 'The associations and the map are equal in terms of size, keys, and values.'
            association.size() == map.size()
            association.keySet().toSet() == map.keySet()
            association.values().sort().toList() == map.values().sort()
            association.collect({it.first()}) == map.collect({it.key})
            association.collect({it.second()}) == map.collect({it.value})

        when : 'We apply the operations to the associations and the map a few more times.'
            10.times {
                association = operationsApplier(association) as Association<String, String>
            }
        then: 'The associations and the map are still equal in terms of size, keys, and values.'
            association.size() == map.size()
            association.keySet().toSet() == map.keySet()
            association.values().sort().toList() == map.values().sort()
            association.collect({it.first()}) == map.collect({it.key})
            association.collect({it.second()}) == map.collect({it.value})
        and : 'We can lookup any value from its corresponding key.'
            map.values() as Set == map.keySet().collect { key -> association.get(key).orElseThrow(MissingItemException::new) } as Set

        when : 'We use the `toMap()` method to convert the association to a map.'
            var convertedMap = association.toMap()
        then: 'The converted map is equal to the reference map.'
            convertedMap == map

        when : 'We use the stream API to map both the association and the reference map.'
            var mappedAssociation = association.entrySet().stream().map({ it.withFirst(it.first().toUpperCase() + "!") }).filter({ it.hashCode() % 2 == 0 }).collect(Association.collectorOf(String, String))
            var mappedMap = map.entrySet().stream().map({ Pair.of(it.key.toUpperCase() + "!", it.value) }).filter({ it.hashCode() % 2 == 0 }).collect(Collectors.toMap({it.first()}, {it.second()}))
        then : 'The mapped association and map are equal.'
            mappedAssociation.toMap() == mappedMap

        when : 'We use the parallel stream API to map both the association and the reference map.'
            var mappedAssociationParallel = association.entrySet().parallelStream().map({ it.withFirst(it.first().toUpperCase() + "!") }).filter({ it.hashCode() % 2 == 0 }).collect(Association.collectorOf(String, String))
            var mappedMapParallel = map.entrySet().parallelStream().map({ Pair.of(it.key.toUpperCase() + "!", it.value) }).filter({ it.hashCode() % 2 == 0 }).collect(Collectors.toMap({it.first()}, {it.second()}))
        then : 'The mapped association and map are equal in terms of their contents.'
            mappedAssociationParallel.toMap() == mappedMapParallel
        and : 'Finally, we also check the `isSorted()` and `isLinked()` flag:'
            association.isSorted()
            !association.isLinked()

        and : 'We also check if the key set is distinct!'
            association.size() == new HashSet<>(association.toMap().keySet()).size()

        where :
            operations << [[
                        new Tuple2(Operation.ADD, 'a'),
                        new Tuple2(Operation.ADD, 'b'),
                        new Tuple2(Operation.ADD, 'c')
                    ], [
                        new Tuple2(Operation.REMOVE, 'a'),
                        new Tuple2(Operation.REMOVE, 'b'),
                        new Tuple2(Operation.REMOVE, 'c')
                    ], [
                        new Tuple2(Operation.ADD, 'a'),
                        new Tuple2(Operation.ADD, 'b'),
                        new Tuple2(Operation.ADD, 'c'),
                        new Tuple2(Operation.REMOVE, 'a'),
                        new Tuple2(Operation.REMOVE, 'b'),
                        new Tuple2(Operation.REMOVE, 'c')
                    ], [
                        new Tuple2(Operation.ADD, 'a'),
                        new Tuple2(Operation.ADD, 'b'),
                        new Tuple2(Operation.REMOVE, 'a'),
                        new Tuple2(Operation.ADD, 'c'),
                        new Tuple2(Operation.REMOVE, 'b'),
                        new Tuple2(Operation.REMOVE, 'c')
                    ], [
                        new Tuple2(Operation.ADD, 'a'),
                        new Tuple2(Operation.REMOVE, 'a'),
                        new Tuple2(Operation.ADD, 'b'),
                        new Tuple2(Operation.REMOVE, 'b'),
                        new Tuple2(Operation.ADD, 'c'),
                        new Tuple2(Operation.REMOVE, 'c')
                    ], [
                        new Tuple2(Operation.ADD, 'a'),
                        new Tuple2(Operation.ADD, 'a'),
                        new Tuple2(Operation.REMOVE, 'a'),
                        new Tuple2(Operation.ADD, 'b'),
                        new Tuple2(Operation.ADD, 'b'),
                        new Tuple2(Operation.REMOVE, 'b'),
                        new Tuple2(Operation.ADD, 'c'),
                        new Tuple2(Operation.ADD, 'c'),
                        new Tuple2(Operation.REMOVE, 'c')
                    ], (0..100).collect({
                        var hash = Math.abs((139 * (90 + it.hashCode())) % 100)
                        var operation = hash % 3 == 0 ? Operation.REMOVE : Operation.ADD
                        return new Tuple2(operation, hash.toString())
                    }), (0..1_000).collect({
                        var hash = Math.abs((31 * 139 * (500 + it.hashCode())) % 1_000)
                        var operation = hash % 3 == 0 ? Operation.REMOVE : Operation.ADD
                        return new Tuple2(operation, hash.toString())
                    }), (0..5_000).collect({
                        var hash = Math.abs(it % 2_000)
                        var operation = it > 2_000 ? Operation.REMOVE : Operation.ADD
                        return new Tuple2(operation, hash.toString())
                    }), (0..10_000).collect({
                        var hash1 = Math.abs((1997 * 139 * (500 + it.hashCode())) % 7_000)
                        var operation = hash1 > 3_000 ? Operation.ADD : Operation.REMOVE
                        var hash2 = Math.abs((1997 * 139 * hash1) % 9_000)
                        return new Tuple2(operation, hash2.toString())
                    }), (0..10_000).collect({
                    /*
                        Here the operations come in sequences of 50, which
                        means 50 add operations, then 50 remove operations then 50 add... etc.
                        There is a total of 160 possible values!
                     */
                        var hash = Math.abs((it*1997) % 160)
                        var operation = ((int)(it/50)) % 2 == 0 ? Operation.REMOVE : Operation.ADD
                        return new Tuple2(operation, (hash*1997).toString())
                    }), (0..10_000).collect({
                        /*
                            Here the operations come in sequences of 100, which
                            means 100 add operations, then 100 remove operations then 100 add... etc.
                            There is a total of 190 possible values!
                         */
                        var hash = Math.abs((it*1997) % 190)
                        var operation = ((int)(it/100)) % 2 == 0 ? Operation.REMOVE : Operation.ADD
                        return new Tuple2(operation, (hash*1997).toString())
                    }), (0..10_000).collect({
                        /*
                            Here the operations come in REVERSED and ORDERED sequences of 50, which
                            means 50 add operations, then 50 remove operations then 50 add... etc.
                            There is a total of 160 possible values!
                         */
                        var item = 160-Math.abs(it % 160)
                        var operation = ((int)(it/50)) % 2 == 0 ? Operation.REMOVE : Operation.ADD
                        return new Tuple2(operation, item.toString())
                    }), (0..10_000).collect({
                        /*
                            Here the operations come in REVERSED and ORDERED sequences of 100, which
                            means 100 add operations, then 100 remove operations then 100 add... etc.
                            There is a total of 190 possible values!
                         */
                        var item = 190-Math.abs(it % 190)
                        var operation = ((int)(it/100)) % 2 == 0 ? Operation.REMOVE : Operation.ADD
                        return new Tuple2(operation, (item*1997).toString())
                    })
            ]
    }

    def 'Association maintains full invariance with Map across all operations.'(
            List<Tuple2<Operation, String>> operations
    ) {
        given: 'Fresh association and reference map'
            var assoc = Association.betweenSorted(String, String, (k1, k2) -> k1 <=> k2 )
            var map = new TreeMap<String,String>((k1, k2) -> k1 <=> k2)
            var valueGenerator = { key -> "[value-of:$key]".toString() }
            var replacementValueGenerator = { key -> "[replaced-value-of:$key]".toString() }

            var operationsApplier = { currentAssoc ->
                operations.each { op, key ->
                    switch (op) {
                        case Operation.ADD:
                            currentAssoc = currentAssoc.put(key, valueGenerator(key))
                            map.put(key, valueGenerator(key))
                            break
                        case Operation.REMOVE:
                            currentAssoc = currentAssoc.remove(key)
                            map.remove(key)
                            break
                        case Operation.REPLACE:
                            currentAssoc = currentAssoc.replace(key, replacementValueGenerator(key))
                            if (map.containsKey(key)) {
                                map.put(key, replacementValueGenerator(key))
                            }
                            break
                        case Operation.PUT_IF_ABSENT:
                            currentAssoc = currentAssoc.putIfAbsent(key, valueGenerator(key))
                            map.putIfAbsent(key, valueGenerator(key))
                            break
                        case Operation.CLEAR:
                            currentAssoc = currentAssoc.clear()
                            map.clear()
                            break
                    }
                }
                return currentAssoc
            }

        when: 'Apply operations 3 times'
            3.times {
                assoc = operationsApplier(assoc) as Association<String, String>
            }
        then: 'Immediate invariance'
            assoc.size() == map.size()
            assoc.keySet().toSet() == map.keySet()
            assoc.values().sort().toList() == map.values().sort()
            assoc.toMap() == map
            assoc.collect({it.first()}) == map.collect({it.key})
            assoc.collect({it.second()}) == map.collect({it.value})
        and :
            map.every { k, v ->
                assoc.get(k).orElse(null) == v
            }
            assoc.every { pair ->
                pair.second() == map[pair.first()]
            }

        when : 'We verify the `Iterable` implementation of the association by iterating over it.'
            var pairSet = new HashSet()
            for ( pair in assoc ) {
                pairSet.add(pair)
            }
        then : 'The pair set is equal to the map and all sizes match.'
            pairSet.size() == assoc.size()
            pairSet.size() == map.size()
            pairSet == map.collect({ k, v -> Pair.of(k, v) }).toSet()

        where:
            operations << [
                (0..2000).collect {
                    var random = new Random(it*1997).nextInt()
                    var randKey = "key-"+Integer.toHexString(random)
                    var op = Operation.values()[Math.abs(random % 5)]
                    return new Tuple2(op, randKey)
                },
            ]
    }

    def 'Two associations the the same operations applied to them are always equal.'(
        List<Tuple2<Operation, String>> operations
    ) {
        given: 'We create an association and a map as well as a closure to apply the operations to them.'
            var association = Association.betweenSorted(String, String, (k1, k2) -> k1 <=> k2 )
            var operationsApplier = { currentAssociations ->
                operations.each { operation, key ->
                    switch (operation) {
                        case Operation.ADD:
                            currentAssociations = currentAssociations.put(key, "value of " + key)
                            break
                        case Operation.REMOVE:
                            currentAssociations = currentAssociations.remove(key)
                            break
                    }
                }
                return currentAssociations
            }
        when : 'We apply the operations to two associations and the map for the first time.'
            var association1 = operationsApplier(association) as Association<String, String>
            var association2 = operationsApplier(association) as Association<String, String>
        and : 'We apply some more custom operations on top:'
            association1 = association1.put("Temp1", "_").put("Temp2", "_")
            association1 = association1.remove("Temp1").remove("Temp2")
        then: 'The two associations are equal.'
            association1 == association2
        and : 'Their hash codes are equal.'
            association1.hashCode() == association2.hashCode()

        when : 'We update the second association to be a bit different.'
            association2 = association2.put("A bit", "different")
        then: 'The two associations are not equal.'
            association1 != association2
        and : 'Their hash codes are not equal.'
            association1.hashCode() != association2.hashCode()

        where :
            operations << [[
                                   new Tuple2(Operation.ADD, 'a'),
                                   new Tuple2(Operation.ADD, 'b'),
                                   new Tuple2(Operation.ADD, 'c')
                    ], [
                                   new Tuple2(Operation.REMOVE, 'a'),
                                   new Tuple2(Operation.REMOVE, 'b'),
                                   new Tuple2(Operation.REMOVE, 'c')
                    ], [
                                   new Tuple2(Operation.ADD, 'a'),
                                   new Tuple2(Operation.ADD, 'b'),
                                   new Tuple2(Operation.ADD, 'c'),
                                   new Tuple2(Operation.REMOVE, 'a'),
                                   new Tuple2(Operation.REMOVE, 'b'),
                                   new Tuple2(Operation.REMOVE, 'c')
                    ], [
                                   new Tuple2(Operation.ADD, 'a'),
                                   new Tuple2(Operation.ADD, 'b'),
                                   new Tuple2(Operation.REMOVE, 'a'),
                                   new Tuple2(Operation.ADD, 'c'),
                                   new Tuple2(Operation.REMOVE, 'b'),
                                   new Tuple2(Operation.REMOVE, 'c')
                    ], [
                                   new Tuple2(Operation.ADD, 'a'),
                                   new Tuple2(Operation.REMOVE, 'a'),
                                   new Tuple2(Operation.ADD, 'b'),
                                   new Tuple2(Operation.REMOVE, 'b'),
                                   new Tuple2(Operation.ADD, 'c'),
                                   new Tuple2(Operation.REMOVE, 'c')
                    ], [
                                   new Tuple2(Operation.ADD, 'a'),
                                   new Tuple2(Operation.ADD, 'a'),
                                   new Tuple2(Operation.REMOVE, 'a'),
                                   new Tuple2(Operation.ADD, 'b'),
                                   new Tuple2(Operation.ADD, 'b'),
                                   new Tuple2(Operation.REMOVE, 'b'),
                                   new Tuple2(Operation.ADD, 'c'),
                                   new Tuple2(Operation.ADD, 'c'),
                                   new Tuple2(Operation.REMOVE, 'c')
                    ], (0..100).collect({
                        var hash = Math.abs((139 * (90 + it.hashCode())) % 100)
                        var operation = hash % 3 == 0 ? Operation.REMOVE : Operation.ADD
                        return new Tuple2(operation, hash.toString())
                    }), (0..1_000).collect({
                        var hash = Math.abs((31 * 139 * (500 + it.hashCode())) % 1_000)
                        var operation = hash % 3 == 0 ? Operation.REMOVE : Operation.ADD
                        return new Tuple2(operation, hash.toString())
                    }), (0..5_000).collect({
                        var hash = Math.abs(it % 2_000)
                        var operation = it > 2_000 ? Operation.REMOVE : Operation.ADD
                        return new Tuple2(operation, hash.toString())
                    })
            ]
    }

    def 'You can merge two associations into one using the `putAll` method.'() {
        given :
            var associations1 = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
            var associations2 = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
        when : 'We add some values to the first association.'
            associations1 = associations1.put("a", 1).put("b", 2).put("c", 3)
        and : 'We add some values to the second association.'
            associations2 = associations2.put("d", 4).put("e", 5).put("f", 6)
        and : 'We merge the two associations.'
            var mergedAssociations = associations1.putAll(associations2)
        then : 'The merged association contains all the values from the two associations.'
            mergedAssociations.size() == 6
            mergedAssociations.get("a").orElseThrow(MissingItemException::new) == 1
            mergedAssociations.get("b").orElseThrow(MissingItemException::new) == 2
            mergedAssociations.get("c").orElseThrow(MissingItemException::new) == 3
            mergedAssociations.get("d").orElseThrow(MissingItemException::new) == 4
            mergedAssociations.get("e").orElseThrow(MissingItemException::new) == 5
            mergedAssociations.get("f").orElseThrow(MissingItemException::new) == 6
    }

    def 'You can remove the entries of one association from another, using the `removeAll(ValueSet)` method.'() {
        given : 'Two empty associations between strings and integers.'
            var association1 = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
            var association2 = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
        when : 'We add some values to the first association.'
            association1 = association1.put("x", 1).put("y", 2).put("z", 3)
        and : 'We add some values to the second association.'
            association2 = association2.put("y", 2).put("z", 3).put("o", 4)
        and : 'We remove the entries of the second association from the first.'
            var result = association1.removeAll(association2.keySet())
        then : 'The result contains only the entries that were not in the second association.'
            result.size() == 1
            result.get("x").orElseThrow(MissingItemException::new) == 1
    }

    def 'You can remove the entries of one association from another, using the `removeAll(Set)` method.'() {
        given : 'Two empty associations between strings and integers.'
            var association1 = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
            var association2 = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
        when : 'We add some values to the first association.'
            association1 = association1.put("a", 1).put("b", 2).put("c", 3)
        and : 'We add some values to the second association.'
            association2 = association2.put("b", 2).put("c", 3).put("d", 4)
        and : 'We remove the entries of the second association from the first.'
            var result = association1.removeAll(association2.keySet().toSet())
        then : 'The result contains only the entries that were not in the second association.'
            result.size() == 1
            result.get("a").orElseThrow(MissingItemException::new) == 1
    }

    def 'You remove the entries of one sorted association from another, using the `removeAll(Stream)` method.'() {
        given : 'Two empty sorted associations between strings and integers.'
            var association1 = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
            var association2 = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
        when : 'We add some values to the first association.'
            association1 = association1.put("x", 1).put("y", 2).put("z", 3)
        and : 'We add some values to the second association.'
            association2 = association2.put("y", 2).put("z", 3).put("i", 4)
        and : 'We remove the entries of the second association from the first using a stream of keys.'
            var result = association1.removeAll(association2.keySet().stream())
        then : 'The result contains only the entries that were not in the second association.'
            result.size() == 1
            result.get("x").orElseThrow(MissingItemException::new) == 1
    }

    def 'You can remove entries from a sorted association selectively, using `removeIf(Predicate)`.'(
        Class<?> type, List<Object> elements, Predicate<Pair<Object, String>> predicate
    ) {
        reportInfo """
            You can remove key/value pairs from a sorted association which satisfy
            a given `Predicate` lambda. Or in other words,
            if the `Predicate.test(Object)` method yields `true` for a particular
            pair, then it will be removed, otherwise, it will remain in the
            returned association.
        """
        given : 'We create a sorted association and a regular JDK map containing some test elements.'
            var assoc = Association.betweenSorted(type, String).putAll(elements.stream().map(it->Pair.of(it, it.toString())))
            Map<?,String> map = new TreeMap<Object, String>().putAll(elements.stream().map(it->new AbstractMap.SimpleEntry<>(it, it.toString())).collect(Collectors.toList()))

        when : 'We apply a predicate to both types of mappings...'
            var updatedAssoc = assoc.removeIf(predicate)
            map.removeAll(entry -> predicate.test(Pair.of(entry.key, entry.value)))
        then : 'They contain the exact same entries!'
            updatedAssoc.toMap() == map

        where :
            type      |  elements                                               |  predicate
            Float     | [4.3f, 7f, 0.1f, 26.34f, 23f, 86.3f, 218f, 2f, 1.2f, 9f]|  { (it.first() - it.first() % 1) == it.first() }
            Integer   | (-50..50).toList()                                      |  { it.first() % 3 == 1 }
            String    | (-50..50).collect({Integer it -> it + "!"}).toList()    |  { it.first().hashCode() % 5 == 1 }
            Short     | (0..1000).collect({Integer it -> it as Short}).toList() |  { it.hashCode() * 1997 % 8 == 2 }
            Character | ['a' as char, 'x' as char, '4' as char, '#' as char]    |  { it.first() == 'x' as char }
            Boolean   | (0..100).collect({Integer it -> ( it * 1997 ) % 2 == 0})|  { it.first() }
            Month     | (0..100).collect({ Integer it -> Month.values()[it%12]}) | { it.first() == Month.DECEMBER }
    }

    def 'The `clear` method creates an empty association with the same key and value types.'() {
        given :
            var association = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
            association = association.put("a", 1).put("b", 2).put("c", 3)
        expect : 'The association is not empty.'
            association.isNotEmpty()
            !association.isEmpty()

        when : 'We clear the association.'
            association = association.clear()
        then : 'The association is empty, sorted but not linked.'
            association.isEmpty()
            association.isSorted()
            !association.isLinked()
        and : 'The key and value types are the same.'
            association.keyType() == String
            association.valueType() == Integer
    }

    def 'You can merge two large associations into one using the `putAll` method.'(int size) {
        given : 'We create two associations and some random key and value generators.'
            var association1 = Association.betweenSorted(String, Character, (k1, k2) -> k1 <=> k2 )
            var association2 = Association.betweenSorted(String, Character, (k1, k2) -> k1 <=> k2 )
            var indexToRandomKey = { index -> Integer.toHexString(Math.abs((929 * (42 + index as int)) % size)) }
            var indexToRandomValue = { index -> Math.abs(65+(index as int)) as char }
        and : 'We create a regular mutable map to check the results are correct.'
            var map = [:]
        when : 'We add some values to the first association.'
            size.times { index ->
                var key = indexToRandomKey(index%(size/2))
                var value = indexToRandomValue(index)
                association1 = association1.put(key, value)
                map[key] = value
            }
        and : 'We add some values to the second association.'
            size.times { index ->
                var key = indexToRandomKey(-index%(size/2))
                var value = indexToRandomValue(index)
                association2 = association2.put(key, value)
                map[key] = value
            }
        and : 'We merge the two associations.'
            var mergedAssociations = association1.putAll(association2)
        then : 'The merged association contains all the values from the two associations.'
            mergedAssociations.size() == map.size()
            mergedAssociations.keySet() as Set == map.keySet()
            mergedAssociations.values().sort().toList() == map.values().sort()

        when : 'We use the `toMap()` method to convert the association to a map.'
            var convertedMap = mergedAssociations.toMap()
        then: 'The converted map is equal to the reference map.'
            convertedMap == map


        where :
            size << [10, 100, 1_000, 10_000]
    }

    def 'You can remove the entries of a large association from another, using the `removeAll` method.'(int size) {
        given : 'Two empty associations between strings and integers.'
            var association1 = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
            var association2 = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
            var indexToRandomKey = { index -> Integer.toHexString(Math.abs((929 * (42 + index as int)) % size)) }
            var indexToRandomValue = { index -> Math.abs(65+(index as int)) }
        and : 'We create two regular mutable maps to check the results are correct based on their `remove` method.'
            var map1 = [:]
            var map2 = [:]
        when : 'We add some values to the first association and map.'
            size.times { index ->
                var key = indexToRandomKey(index%(size/2))
                var value = indexToRandomValue(index)
                association1 = association1.put(key, value)
                map1[key] = value
            }
        and : 'We add some values to the second association and map.'
            size.times { index ->
                var key = indexToRandomKey(-index%(size/2))
                var value = indexToRandomValue(index)
                association2 = association2.put(key, value)
                map2[key] = value
            }
        and : 'We remove the entries of the second association from the first.'
            var result = association1.removeAll(association2.keySet())
        and : 'We remove the entries of the second map from the first.'
            map2.forEach { key, value -> map1.remove(key) }
        then : 'The result contains only the entries that were not in the second association.'
            result.size() == map1.size()
            result.keySet() as Set == map1.keySet()
            result.values().sort().toList() == map1.values().sort()

        where :
            size << [10, 100, 1_000, 10_000]
    }

    def 'You can merge a large association and a map into a single association using the `putAll` method.'(int size) {
        given : 'We create an association and a map and some random key and value generators.'
            var association = Association.betweenSorted(String, Character, (k1, k2) -> k1 <=> k2 )
            var otherMap = [:]
            var indexToRandomKey = { index -> Integer.toHexString(Math.abs((929 * (42 + index as int)) % size)) }
            var indexToRandomValue = { index -> Math.abs(65+(index as int)) as char }
        and : 'We create a regular mutable map to check the results are correct.'
            var map = [:]
        when : 'We add some values to the first association.'
            size.times { index ->
                var key = indexToRandomKey(index%(size/2))
                var value = indexToRandomValue(index)
                association = association.put(key, value)
                map[key] = value
            }
        and : 'We add some values to the second association.'
            size.times { index ->
                var key = indexToRandomKey(-index%(size/2))
                var value = indexToRandomValue(index)
                otherMap[key] = value
                map[key] = value
            }
        and : 'We merge the two associations.'
            var mergedAssociations = association.putAll(otherMap)
        then : 'The merged association contains all the values from the two associations.'
            mergedAssociations.size() == map.size()
            mergedAssociations.keySet() as Set == map.keySet()
            mergedAssociations.values().sort().toList() == map.values().sort()
        where :
            size << [10, 100, 1_000, 10_000]
    }

    def 'An `Association` has an intuitive string representation.'() {
        given :
            var association = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
        when : 'We add some values to the association.'
            association = association.put("a", 1).put("b", 2).put("c", 3)
        then : 'The string representation of the association is as expected.'
            association.toString() == 'SortedAssociation<String,Integer>["a" ↦ 1, "b" ↦ 2, "c" ↦ 3]'
    }

    def 'A larger `Association` will have a trimmed string representation.'() {
        given :
            var association = Association.betweenSorted(Character, Byte, (k1, k2) -> k1 <=> k2 )
        when : 'We add some values to the association.'
            30.times { index ->
                var key = Math.abs(65+(index)) as char
                var value = Math.abs(1997*index) as byte
                association = association.put(key, value)
            }
        then : 'The string representation of the association is as expected.'
            association.toString() == "SortedAssociation<Character,Byte>['A' ↦ 0, 'B' ↦ -51, 'C' ↦ -102, 'D' ↦ 103, 'E' ↦ 52, 'F' ↦ 1, 'G' ↦ -50, 'H' ↦ -101, ... 22 items left]"
    }

    def 'The `replace` method replaces the value of a key with a new value, if and only if the key is present.'() {
        given :
            var association = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )

        when : 'We add some values to the association.'
            association = association.put("a", 1).put("b", 2).put("c", 3)
        and : 'We replace the value of a key that is present.'
            association = association.replace("b", 4)
        then : 'The value of the key is replaced.'
            association.get("b").orElseThrow(MissingItemException::new) == 4
        and : 'The size of the association remains the same.'
            association.size() == 3

        when : 'We call the `replace` method with a key that is not present.'
            association = association.replace("d", 5)
        then : 'The size of the association remains the same.'
            association.size() == 3
        and : 'The value of the key is not replaced.'
            !association.get("d").isPresent()
    }

    def 'Use `putAll(Pair...)` to populate an association at once from multiple pairs.'() {
        given :
            var association = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
        when : 'We add some values to the association.'
            association = association.putAll(
                Pair.of("I", 1),
                Pair.of("was", 2),
                Pair.of("added", 3),
                Pair.of("to", 4),
                Pair.of("the", 5),
                Pair.of("association", 6)
            )
        then : 'The association contains the values.'
            association.size() == 6
            association.get("I").orElseThrow(MissingItemException::new) == 1
            association.get("was").orElseThrow(MissingItemException::new) == 2
            association.get("added").orElseThrow(MissingItemException::new) == 3
            association.get("to").orElseThrow(MissingItemException::new) == 4
            association.get("the").orElseThrow(MissingItemException::new) == 5
            association.get("association").orElseThrow(MissingItemException::new) == 6
    }

    def 'Use `putAll(Tuple<Pair>)` to populate an association at once from multiple pairs.'() {
        given :
            var association = Association.betweenSorted(Integer, String, (k1, k2) -> k1 <=> k2 )
        when : 'We add some values to the association.'
            association = association.putAll(
                Tuple.of(
                    Pair.of(1, "I"),
                    Pair.of(2, "was"),
                    Pair.of(3, "added"),
                    Pair.of(4, "to"),
                    Pair.of(5, "the"),
                    Pair.of(6, "association")
                )
            )
        then : 'The association contains the values.'
            association.size() == 6
            association.get(1).orElseThrow(MissingItemException::new) == "I"
            association.get(2).orElseThrow(MissingItemException::new) == "was"
            association.get(3).orElseThrow(MissingItemException::new) == "added"
            association.get(4).orElseThrow(MissingItemException::new) == "to"
            association.get(5).orElseThrow(MissingItemException::new) == "the"
            association.get(6).orElseThrow(MissingItemException::new) == "association"
    }

    def 'Use `putAll(Collection<Pair>)` to populate an association at once from multiple pairs.'() {
        reportInfo """
            This method ensures compatibility with the `Collection` interface, which
            is especially useful when you have a list of pairs that you want to
            populate the association with.
        """
        given :
            var association = Association.betweenSorted(Integer, String, (k1, k2) -> k1 <=> k2 )
        when : 'We add some values to the association.'
            association = association.putAll([
                Pair.of(1, "I"),
                Pair.of(2, "was"),
                Pair.of(3, "added"),
                Pair.of(4, "to"),
                Pair.of(5, "the"),
                Pair.of(6, "association")
            ])
        then : 'The association contains the values.'
            association.size() == 6
            association.get(1).orElseThrow(MissingItemException::new) == "I"
            association.get(2).orElseThrow(MissingItemException::new) == "was"
            association.get(3).orElseThrow(MissingItemException::new) == "added"
            association.get(4).orElseThrow(MissingItemException::new) == "to"
            association.get(5).orElseThrow(MissingItemException::new) == "the"
            association.get(6).orElseThrow(MissingItemException::new) == "association"
    }

    def 'Use `putAll(Set<Pair>)` to populate an association at once from multiple pairs.'() {
        reportInfo """
            This method ensures compatibility with the `Set` interface, which
            is especially useful when you have a set of pairs that you want to
            populate the association with.
        """
        given :
            var association = Association.betweenSorted(Character, String, (k1, k2) -> k1 <=> k2 )
        when : 'We add some values to the association.'
            association = association.putAll([
                Pair.of('I' as char, "I"),
                Pair.of('w' as char, "was"),
                Pair.of('a' as char, "added"),
                Pair.of('t' as char, "to"),
                Pair.of('t' as char, "the"), // overwrites the previous value
                Pair.of('a' as char, "association") // overwrites the previous value
            ] as Set)
        then : 'The association contains the values.'
            association.size() == 4
            association.get('I' as char).orElseThrow(MissingItemException::new) == "I"
            association.get('w' as char).orElseThrow(MissingItemException::new) == "was"
            association.get('a' as char).orElseThrow(MissingItemException::new) == "association"
            association.get('t' as char).orElseThrow(MissingItemException::new) == "the"
    }

    def 'Use `putAllIfAbsent(Pair...)` to populate a sorted association at once from multiple pairs.'() {
        reportInfo """
            The `putAllIfAbsent(Pair...)` method allows you to populate a sorted association from
            an array of entry pairs. However, it only adds new entries to the new association,
            if they are not already present.
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create a sorted association with some initial entries:'
            var associations = Association.betweenSorted(String, Integer).putAll(
                Pair.of("was", -1),
                Pair.of("the", -2),
                Pair.of("association", -3)
            )
        when : 'We add some values to the association.'
            associations = associations.putAllIfAbsent(
                Pair.of("I", 1),
                Pair.of("was", 2),
                Pair.of("added", 3),
                Pair.of("to", 4),
                Pair.of("the", 5),
                Pair.of("association", 6)
            )
        then : 'The sorted association contains the values.'
            associations.size() == 6
            associations.get("I").orElseThrow(MissingItemException::new) == 1
            associations.get("was").orElseThrow(MissingItemException::new) == -1
            associations.get("added").orElseThrow(MissingItemException::new) == 3
            associations.get("to").orElseThrow(MissingItemException::new) == 4
            associations.get("the").orElseThrow(MissingItemException::new) == -2
            associations.get("association").orElseThrow(MissingItemException::new) == -3
    }

    def 'Use `putAllIfAbsent(Tuple<Pair>)` to populate a sorted association at once from multiple pairs.'() {
        reportInfo """
            The `putAllIfAbsent(Tuple<Pair>)` method allows you to populate a sorted association from
            a tuple of entry pairs. However, it only adds new entries to the new association,
            if they are not already present.
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create a sorted association with some initial entries:'
            var associations = Association.betweenSorted(Integer, String).putAll(
                Pair.of(2, ":o"),
                Pair.of(4, ":3"),
                Pair.of(6, ":I")
            )
        when : 'We add some values to the association.'
            associations = associations.putAllIfAbsent(
                Tuple.of(
                    Pair.of(1, "I"),
                    Pair.of(2, "was"),
                    Pair.of(3, "added"),
                    Pair.of(4, "to"),
                    Pair.of(5, "the"),
                    Pair.of(6, "association")
                )
            )
        then : 'The association contains the values.'
            associations.size() == 6
            associations.get(1).orElseThrow(MissingItemException::new) == "I"
            associations.get(2).orElseThrow(MissingItemException::new) == ":o"
            associations.get(3).orElseThrow(MissingItemException::new) == "added"
            associations.get(4).orElseThrow(MissingItemException::new) == ":3"
            associations.get(5).orElseThrow(MissingItemException::new) == "the"
            associations.get(6).orElseThrow(MissingItemException::new) == ":I"
    }

    def 'Use `putAllIfAbsent(Collection<Pair>)` to populate a sorted association at once from multiple pairs.'() {
        reportInfo """
            This method ensures compatibility with the `Collection` interface, which
            is especially useful when you have a list of pairs that you want to
            populate the sorted association with if they are not already present.
            
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create a sorted association with some initial entries:'
            var associations = Association.betweenSorted(Integer, String).putAll(
                Pair.of(2, ":o"),
                Pair.of(4, ":3"),
                Pair.of(6, ":I")
            )
        when : 'We add some values to the association.'
            associations = associations.putAllIfAbsent([
                Pair.of(1, "I"),
                Pair.of(2, "was"),
                Pair.of(3, "added"),
                Pair.of(4, "to"),
                Pair.of(5, "the"),
                Pair.of(6, "association")
            ])
        then : 'The association contains the values.'
            associations.size() == 6
            associations.get(1).orElseThrow(MissingItemException::new) == "I"
            associations.get(2).orElseThrow(MissingItemException::new) == ":o"
            associations.get(3).orElseThrow(MissingItemException::new) == "added"
            associations.get(4).orElseThrow(MissingItemException::new) == ":3"
            associations.get(5).orElseThrow(MissingItemException::new) == "the"
            associations.get(6).orElseThrow(MissingItemException::new) == ":I"
    }

    def 'Use `putAllIfAbsent(Set<Pair>)` to populate a sorted association at once from multiple pairs.'() {
        reportInfo """
            This method ensures compatibility with the `Set` interface, which
            is especially useful when you have a set of pairs that you want to
            populate the sorted association with if they are not already present.
            
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create a sorted association with some initial entries:'
            var associations = Association.betweenSorted(Character, String).putAll(
                Pair.of('w' as char, ":)"),
                Pair.of('t' as char, ":P")
            )
        when : 'We add some values to the association.'
            associations = associations.putAllIfAbsent([
                Pair.of('I' as char, "I"),
                Pair.of('w' as char, "was"),
                Pair.of('a' as char, "added"),
                Pair.of('t' as char, "to"),
                Pair.of('t' as char, "the"), // does not overwrite the previous value
                Pair.of('a' as char, "association") // does not overwrite the previous value
            ] as Set)
        then : 'The association contains the values.'
            associations.size() == 4
            associations.get('I' as char).orElseThrow(MissingItemException::new) == "I"
            associations.get('w' as char).orElseThrow(MissingItemException::new) == ":)"
            associations.get('a' as char).orElseThrow(MissingItemException::new) == "added"
            associations.get('t' as char).orElseThrow(MissingItemException::new) == ":P"
    }

    def 'Use `putAllIfAbsent(Map<K,V>)` to populate a sorted association at once from a regular Java map.'() {
        reportInfo """
            This method ensures compatibility with the `Map` interface, which
            is especially useful when you have a plain old key-value mapping that you want to
            use to populate the association with if they are not already present.
            
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create an association with some initial entries:'
            var associations = Association.betweenSorted(Character, String).putAll(
                Pair.of('w' as char, ":)"),
                Pair.of('t' as char, ":P")
            )
        when : 'We add some values to the association.'
            associations = associations.putAllIfAbsent([
                ('I' as char): "I",
                ('w' as char): "was",
                ('a' as char): "added",
                ('t' as char): "to",
                ('t' as char): "the",
                ('a' as char): "association"
            ] as LinkedHashMap)
        then : 'The association contains the values.'
            associations.size() == 4
            associations.get('I' as char).orElseThrow(MissingItemException::new) == "I"
            associations.get('w' as char).orElseThrow(MissingItemException::new) == ":)"
            associations.get('a' as char).orElseThrow(MissingItemException::new) == "association"
            associations.get('t' as char).orElseThrow(MissingItemException::new) == ":P"
    }

    def 'Use `putAllIfAbsent(Association<K,V>)` to populate a sorted association from another one.'() {
        reportInfo """
            This method merges a sorted association with another one
            in a way where only those key-value pairs are added from the supplied 
            association, whose keys are not already present in the targeted one.
            
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create a sorted association with some initial entries:'
            var associations = Association.betweenSorted(Character, String).putAll(
                Pair.of('w' as char, ":)"),
                Pair.of('t' as char, ":P")
            )
        when : 'We add some values to the association.'
            associations = associations.putAllIfAbsent(Association.betweenSorted(Character, String).putAll(
                Pair.of('I' as char, "I"),
                Pair.of('w' as char, "was"),
                Pair.of('a' as char, "added"),
                Pair.of('t' as char, "to"),
                Pair.of('t' as char, "the"),
                Pair.of('a' as char, "association")
            ))
        then : 'The sorted association contains the expected values.'
            associations.size() == 4
            associations.get('I' as char).orElseThrow(MissingItemException::new) == "I"
            associations.get('w' as char).orElseThrow(MissingItemException::new) == ":)"
            associations.get('a' as char).orElseThrow(MissingItemException::new) == "association"
            associations.get('t' as char).orElseThrow(MissingItemException::new) == ":P"
    }

    def 'The `putIfAbsent(K, V)` method adds a key-value pair to the association if, and only if, the key is not present.'() {
        given : 'We create an association between shorts and chars and add a bunch of entries to the association.'
            var association = Association.betweenSorted(Short, Character, (k1, k2) -> k1 <=> k2 ).putAll([
                Pair.of(11 as short, 'a' as char),
                Pair.of(22 as short, 'b' as char),
                Pair.of(33 as short, 'c' as char),
                Pair.of(44 as short, 'x' as char),
                Pair.of(55 as short, 'y' as char),
                Pair.of(66 as short, 'z' as char)
            ])
        when : 'We add a new entry to the association.'
            association = association.putIfAbsent(77 as short, 'w' as char)
        then : 'The new entry is added to the association.'
            association.size() == 7
            association.get(77 as short).orElseThrow(MissingItemException::new) == 'w' as char

        when : 'We try to add an entry with a key that is already present.'
            association = association.putIfAbsent(44 as short, 'v' as char)
        then : 'The association remains the same.'
            association.size() == 7
            association.get(44 as short).orElseThrow(MissingItemException::new) == 'x' as char

        when : 'We now ry this for every key in the association.'
            association.keySet().forEach { key ->
                association = association.putIfAbsent(key, '!' as char)
            }
        then : 'The association still remains the same.'
            association.size() == 7
            association.get(44 as short).orElseThrow(MissingItemException::new) == 'x' as char
    }

    def 'Use `Association.betweenSorted(Number.class, Number.class)` to create an association between all kinds of numbers.'() {
        given :
            var association = Association.betweenSorted(Number.class, Number.class, (k1, k2) -> k1 <=> k2 )
        expect :
            association.isEmpty()
            association.keyType() == Number.class
            association.valueType() == Number.class
        when : 'We add some values to the association.'
            association = association.put(1, 1).put(2L, 2L).put(3.0f, 3.0f).put(4.0, 4.0)
        then : 'The association contains the values.'
            association.size() == 4
            association.get(1).orElseThrow(MissingItemException::new) == 1
            association.get(2L).orElseThrow(MissingItemException::new) == 2L
            association.get(3.0f).orElseThrow(MissingItemException::new) == 3.0f
            association.get(4.0).orElseThrow(MissingItemException::new) == 4.0
    }

    def 'The classTyped method returns the correct class and handles null parameters'() {
        when:
            var associationClass = Association.classTyped(String, Integer)
        then:
            associationClass == Association.class

        when:
            Association.classTyped(null, Integer)
        then:
            thrown(NullPointerException)

        when:
            Association.classTyped(String, null)
        then:
            thrown(NullPointerException)
    }

    def 'The of factory method throws NPE for null parameters'() {
        when:
            Association.ofSorted(null, "value", (k1, k2) -> k1 <=> k2)
        then:
            thrown(NullPointerException)

        when:
            Association.ofSorted("key", null, (k1, k2) -> k1 <=> k2)
        then:
            thrown(NullPointerException)
    }

    def 'The `entrySet` is immutable and contains correct pairs'() {
        given:
            var assoc = Association.ofSorted("a", 1, (k1, k2) -> k1 <=> k2).put("b", 2)
        when:
            var entryValueSet = assoc.entrySet()
            var entrySet = entryValueSet.toSet()
        then:
            entrySet.size() == 2
            entrySet.contains(Pair.of("a", 1))
            entrySet.contains(Pair.of("b", 2))
        and :
            entryValueSet.size() == 2
            entryValueSet.contains(Pair.of("a", 1))
            entryValueSet.contains(Pair.of("b", 2))

        when:
            entrySet.add(Pair.of("c", 3))
        then:
            thrown(UnsupportedOperationException)
    }

    def 'You can iterate over the `entrySet` of all pairs in an `Association`.'() {
        given:
            var assoc = Association.betweenSorted(Integer, String, (k1, k2)-> k1 <=> k2 ).putAll(
                Pair.of(1, "I"),
                Pair.of(2, "was"),
                Pair.of(3, "added"),
                Pair.of(4, "to"),
                Pair.of(5, "the"),
                Pair.of(6, "association")
            )
        when:
            var entries = []
            for (var entry : assoc.entrySet().toSet()) {
                entries << entry
            }
        then:
            entries.size() == 6
            entries.contains(Pair.of(1, "I"))
            entries.contains(Pair.of(2, "was"))
            entries.contains(Pair.of(3, "added"))
            entries.contains(Pair.of(4, "to"))
            entries.contains(Pair.of(5, "the"))
            entries.contains(Pair.of(6, "association"))
    }

    def 'Use the `replaceAll(Map<K,V>)` to only updates existing keys.'() {
        given:
            var original = Association.ofSorted("a", 1, (k1, k2) -> k1 <=> k2).put("b", 2).put("c", 3)
            var replacementMap = [a:10, d:40, c:30] as Map
        when:
            var updated = original.replaceAll(replacementMap)
        then:
            updated.size() == 3
            updated.get("a").get() == 10
            updated.get("b").get() == 2  // Should remain unchanged
            updated.get("c").get() == 30
            !updated.containsKey("d")
    }

    def 'The `replaceAll(Pair<K,V>...)` method only updates existing key-value pairs.'() {
        given:
            var original = Association.ofSorted("a", 1, (k1, k2) -> k1 <=> k2).put("b", 2).put("c", 3)
        when:
            var updated = original.replaceAll(
                    Pair.of("a", 10),
                    Pair.of("d", 40),
                    Pair.of("c", 30)
                )
        then:
            updated.size() == 3
            updated.get("a").get() == 10
            updated.get("b").get() == 2  // Should remain unchanged
            updated.get("c").get() == 30
            !updated.containsKey("d")
    }

    def 'The `replaceAll(Tuple<Pair<K,V>>)` method only updates existing key-value pairs.'() {
        given:
            var original = Association.ofSorted("a", 1, (k1, k2) -> k1 <=> k2).put("b", 2).put("c", 3)
        when:
            var updated = original.replaceAll(Tuple.of(
                    Pair.of("a", 10),
                    Pair.of("d", 40),
                    Pair.of("c", 30)
                ))
        then:
            updated.size() == 3
            updated.get("a").get() == 10
            updated.get("b").get() == 2  // Should remain unchanged
            updated.get("c").get() == 30
            !updated.containsKey("d")
    }

    def 'The `replaceAll(Set<Pair<K,V>>)` method only updates existing key-value pairs.'() {
        given:
            var original = Association.ofSorted("a", 1, (k1, k2) -> k1 <=> k2).put("b", 2).put("c", 3)
        when:
            var updated = original.replaceAll([
                    Pair.of("a", 10),
                    Pair.of("d", 40),
                    Pair.of("c", 30)
                ] as Set)
        then:
            updated.size() == 3
            updated.get("a").get() == 10
            updated.get("b").get() == 2  // Should remain unchanged
            updated.get("c").get() == 30
            !updated.containsKey("d")
    }

    def 'The `retainAll` method keeps only specified keys'() {
        given:
            var assoc = Association.ofSorted("a", 1, (k1, k2) -> k1 <=> k2).put("b", 2).put("c", 3)
        when:
            var retained = assoc.retainAll(["a", "c"] as Set)
        then:
            retained.size() == 2
            retained.containsKey("a")
            retained.containsKey("c")
            !retained.containsKey("b")
    }

    def 'The `putIfAbsent` does not overwrite an existing value already stored in an association.'() {
        given:
            var assoc = Association.ofSorted("a", 1, (k1, k2) -> k1 <=> k2).putIfAbsent("a", 2)
        expect:
            assoc.get("a").get() == 1
    }

    def 'The `containsKey` method of a sorted `Association` throws an exception when passing arguments of the wrong type.'()
    {
        given :
            var association = Association.betweenSorted(Integer, Number)

        when :
            association.containsKey("Boom!")
        then :
            thrown(IllegalArgumentException)

        when :
            association.containsKey(null)
        then :
            thrown(NullPointerException)

        when :
            association.containsKey(42)
        then :
            noExceptionThrown()
    }

    def 'The `remove` method of a sorted `Association` throws an exception when passing arguments of the wrong type.'()
    {
        given :
            var association = Association.betweenSorted(Integer, Number)

        when :
            association.remove("Boom!")
        then :
            thrown(IllegalArgumentException)

        when :
            association.remove(null)
        then :
            thrown(NullPointerException)

        when :
            association.remove(42)
        then :
            noExceptionThrown()
    }

    def 'The `get` method of a sorted `Association` throws an exception when passing arguments of the wrong type.'()
    {
        given :
            var association = Association.betweenSorted(Integer, Number)

        when :
            association.get("Boom!")
        then :
            thrown(IllegalArgumentException)

        when :
            association.get(null)
        then :
            thrown(NullPointerException)

        when :
            association.remove(42)
        then :
            noExceptionThrown()
    }

    def 'The `putIfAbsent` method of a sorted `Association` throws an exception when passing arguments of the wrong type.'()
    {
        given :
            var association = Association.betweenSorted(Integer, Number)

        when :
            association.putIfAbsent("Boom!", 42)
        then :
            thrown(IllegalArgumentException)

        when :
            association.putIfAbsent(42, "Boom!")
        then :
            thrown(IllegalArgumentException)

        when :
            association.putIfAbsent(42.666f, 42)
        then :
            thrown(IllegalArgumentException)

        when :
            association.putIfAbsent(42, null)
        then :
            thrown(NullPointerException)

        when :
            association.putIfAbsent(null, 42)
        then :
            thrown(NullPointerException)

        when :
            association.putIfAbsent(42, 42)
        then :
            noExceptionThrown()
    }

    def 'The `put` method of a sorted `Association` throws an exception when passing arguments of the wrong type.'()
    {
        given :
            var association = Association.betweenSorted(Integer, Number)

        when :
            association.put("Boom!", 42)
        then :
            thrown(IllegalArgumentException)

        when :
            association.put(42, "Boom!")
        then :
            thrown(IllegalArgumentException)

        when :
            association.put(42.666f, 42)
        then :
            thrown(IllegalArgumentException)

        when :
            association.put(42, null)
        then :
            thrown(NullPointerException)

        when :
            association.put(null, 42)
        then :
            thrown(NullPointerException)

        when :
            association.put(42, 42)
        then :
            noExceptionThrown()
    }

    def 'Associations with same entries in different order are equal'() {
        given:
            var comparator = { k1, k2 -> k1 <=> k2 }
            var assoc1 = Association.ofSorted("a", 1, comparator).put("b", 2).put("c", 3)
            var assoc2 = Association.ofSorted("c", 3, comparator).put("b", 2).put("a", 1)
        expect:
            assoc1 == assoc2
            assoc1.hashCode() == assoc2.hashCode()
        and :
            assoc1.toList() == assoc2.toList()
            assoc1.toList() == [
                Pair.of("a", 1),
                Pair.of("b", 2),
                Pair.of("c", 3)
            ]
    }

    def 'values() contains all values including duplicates'() {
        given:
            var assoc = Association.ofSorted("c", 10, (k1, k2) -> k1 <=> k2)
                .put("b", 20)
                .put("a", 10) // Duplicate value
        when:
            var values = assoc.values()
        then:
            values.size() == 3
            values.sort().toList() == [10, 10, 20]
        and : 'The values are in the expected order.'
            assoc.toList() == [
                Pair.of("a", 10),
                Pair.of("b", 20),
                Pair.of("c", 10)
            ]
    }

    def 'replaceAll ignores non-existing keys in replacement stream'() {
        given:
            var original = Association.ofSorted("a", 1, (k1, k2) -> k1 <=> k2).put("b", 2)
            var replacements = [Pair.of("k", -40), Pair.of("a", 10), Pair.of("v", 30)]
        when:
            var updated = original.replaceAll(replacements.stream())
        then:
            updated.size() == 2
            updated.get("a").get() == 10
            updated.get("b").get() == 2 // Unchanged
            !updated.containsKey("k")
            !updated.containsKey("v")
        and : 'The updated association has the expected order.'
            updated.toList() == [
                Pair.of("a", 10),
                Pair.of("b", 2)
            ]
    }

    def 'clear on empty association returns an empty instance'() {
        given:
            var emptyAssoc = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 ).clear()
        expect:
            emptyAssoc.isEmpty()
            emptyAssoc.keyType() == String
            emptyAssoc.valueType() == Integer
    }

    def 'The `Association` class is an `Iterable` which allows you to iterate over its entries.'() {
        given:
            var associations = Association.ofSorted("x", 1, (k1, k2) -> k1 <=> k2).put("y", 2).put("z", 3)
        when:
            var entries = []
            for (var entry : associations) {
                entries << entry
            }
        then:
            entries == [Pair.of("x", 1), Pair.of("y", 2), Pair.of("z", 3)]
    }

    def 'Use `Association.collectorOf(Class,Class)` to collect a stream of `Pair`s into a new `Association`.'() {
        given:
            var sentence = Stream.of(
                Pair.of(0, "I"),
                Pair.of(1, "watch"),
                Pair.of(2, "dominion"),
                Pair.of(3, "documentary"),
                Pair.of(4, "on"),
                Pair.of(5, "www.dominionmovement.com")
            )
        when:
            var association = sentence.collect(Association.collectorOfSorted(Integer, String, (k1, k2) -> k1 <=> k2))
        then:
            association.size() == 6
            association.get(0).get() == "I"
            association.get(1).get() == "watch"
            association.get(2).get() == "dominion"
            association.get(3).get() == "documentary"
            association.get(4).get() == "on"
            association.get(5).get() == "www.dominionmovement.com"
        and : 'The entry has the correct order.'
            association.toList() == [
                Pair.of(0, "I"),
                Pair.of(1, "watch"),
                Pair.of(2, "dominion"),
                Pair.of(3, "documentary"),
                Pair.of(4, "on"),
                Pair.of(5, "www.dominionmovement.com")
            ]
    }

    def 'A no-op operations returns the same instance!'() {
        reportInfo """
            This test ensures that no-op operations like `putAll`, `retainAll`, `removeAll`... 
            do not create a new instance of the `Association` class, but rather return the same instance
            when passed an empty collection or set.
        """
        given:
            var association = Association.betweenSorted(String, Integer, (k1, k2) -> k1 <=> k2 )
        and : 'We add some initial values to the association.'
            association = association.put("a", 1).put("b", 2).put("c", 3)

        when: 'We call the `putAll` method with an empty collection.'
            var result = association.putAll([])
        then: 'The result is the same instance as the original association.'
            result.is(association)

        when: 'We call the `retainAll` method with an empty set.'
            result = association.retainAll([] as Set)
        then: 'The result is the same instance as the original association.'
            result.is(association)

        when: 'We call the `removeAll` method with an empty set.'
            result = association.removeAll([] as Set)
        then: 'The result is the same instance as the original association.'
            result.is(association)

        when : 'We now do the same thing with cases where the entries already exist in the association.'
            result = association.putAll([
                Pair.of("a", 1),
                Pair.of("b", 2),
                Pair.of("c", 3)
            ])
        then : 'The result is still the same instance as the original association.'
            result.is(association)

        when : 'We call the `retainAll` method with a set that contains all keys of the association.'
            result = association.retainAll(association.keySet())
        then : 'The result is still the same instance as the original association.'
            result.is(association)

        when : 'We call the `removeAll` method with a set that contains irrelevant keys.'
            result = association.removeAll(["x", "y", "z"] as Set)
        then : 'The result is still the same instance as the original association.'
            result.is(association)
    }

    def 'Use the `toMap()` method to convert a sorted `Association` to an unmodifiable `Map`.'() {
        given:
            var association = Association.betweenSorted(Byte, String)
                .put((byte) 1, "one")
                .put((byte) 2, "two")
                .put((byte) 3, "three")
        when:
            var map = association.toMap()
        then:
            map instanceof Map
            map.size() == 3
            map.get((byte) 1) == "one"
            map.get((byte) 2) == "two"
            map.get((byte) 3) == "three"

        when : 'We try to modify the map.'
            map.put((byte) 4, "four")
        then : 'An UnsupportedOperationException is thrown.'
            thrown(UnsupportedOperationException)

        when : 'We try to remove an entry from the map.'
            map.remove((byte) 1)
        then : 'An UnsupportedOperationException is thrown.'
            thrown(UnsupportedOperationException)
    }

    def 'The spliterator of a sorted association preserves order during traversal.'(int size) {
        reportInfo """
            The spliterator of a sorted `Association` must report the correct characteristics:
                - DISTINCT: Entries are unique
                - IMMUTABLE: Cannot be modified
                - SIZED: Knows exact size
                - NONNULL: Entries aren't null
                - ORDERED: The order according to the comparator
            
            Elements must be traversed in sorted order regardless of splitting.
            All entries should be traversed exactly once regardless of splitting strategy.
            It must support both sequential and parallel traversal, including splitting, 
            where parallel splitting should maintain global ordering.
        """
        given: "A sorted association"
            var sortedAssociation = Association.betweenSorted(Integer, Integer)
            (0..<size).each { i -> sortedAssociation = sortedAssociation.put(i, i*i) }
            var expectedOrder = (0..<size).collect { Pair.of(it, it*it) }

        when: "We create a spliterator..."
            var spliterator = sortedAssociation.spliterator()

        then: "The characteristics include ORDERED, among other things:"
            spliterator.characteristics() & Spliterator.ORDERED
            spliterator.characteristics() & Spliterator.DISTINCT
            spliterator.characteristics() & Spliterator.IMMUTABLE
            spliterator.characteristics() & Spliterator.SIZED
            spliterator.estimateSize() == size

        when: "We do a sequential traversal..."
            var collected = []
            spliterator.forEachRemaining { collected.add(it) }

        then: "The elements are in ascending order!"
            collected == expectedOrder

        when: "We do a parallel-ready traversal with splitting"
            var spliterator2 = sortedAssociation.spliterator()
            var collectedBySplitting = []
            var consumer = { Pair<Integer, Integer> pair -> collectedBySplitting.add(pair) } as Consumer<Pair<Integer, Integer>>
            while (spliterator2.tryAdvance(consumer)) {
                var split = spliterator2.trySplit()
                if (split != null) split.forEachRemaining(consumer)
            }

        then: "Global order maintained despite splits"
            collectedBySplitting == expectedOrder

        where:
            size << [0, 1, 2, 10, 100, 1000]
    }


    def 'The `equals` and `hashCode` implementations of a sorted Association works reliably after removing a large part of entries.'(
        Class<Object> keyType, Class<Object> valueType, List<Pair<Object, Object>> entries
    ) {
        reportInfo """
            Here we test the implementation of the `equals` and `hashCode` methods exhaustively.
            This may look like an exaggerated amount of test data and equality checks, but you
            have to know that under the hood specifically the `equals` implementations are
            highly optimized to specific cases which need to be covered.
            
            More specifically, if there are only small differences between sorted associations,
            we can avoid a lot of work due to two sorted associations sharing most of their branches.
        """
        given : 'We create randomly sorted variants of the test data:'
            var keyHasher1 = { (31 * it.hashCode() * (1997 * it.hashCode() ** 3)%1024) as int }
            var keyHasher2 = { (256 * it.hashCode() * (1997 * it.hashCode() ** 2)%256) as int }
            Comparator<Object> randomSort1 = (a, b) -> (keyHasher1(a)<=>keyHasher1(b))
            Comparator<Object> randomSort2 = (a, b) -> (keyHasher2(a)<=>keyHasher2(b))
            var scrambledEntries1 = entries.toSorted(randomSort1)
            var scrambledEntries2 = entries.toSorted(randomSort2)

        when : 'We create two different sorted `Association` instances from the two scrambled list...'
            var association1 = Association.betweenSorted(keyType, valueType).putAll(scrambledEntries1)
            var association2 = Association.betweenSorted(keyType, valueType).putAll(scrambledEntries2)
        then : 'The two associations are equal!'
            association1.equals(association2)
        and : 'They also have the same hash codes:'
            association1.hashCode() == association2.hashCode()

        when : 'We create versions of the associations where parts are removed...'
            var subList = entries.subList(0, (entries.size() * 0.5) as int)
            var toRemove = subList.collect({it.first()}).toSet()
            var smallerAssociation1 = association1.removeAll(toRemove)
            var smallerAssociation2 = association2.removeAll(toRemove)
        then : 'They are equal...'
            smallerAssociation1 == smallerAssociation2
            smallerAssociation1.hashCode() == smallerAssociation2.hashCode()

        when : 'We make them different by adding to them...'
            var toAdd1 = subList.subList(0, (subList.size() * 0.5) as int).toSet()
            var toAdd2 = subList.subList((subList.size() * 0.5) as int, subList.size()).toSet()
            var lessSmallAssociation1 = smallerAssociation1.putAll(toAdd1)
            var lessSmallAssociation2 = smallerAssociation2.putAll(toAdd2)
        then :
            lessSmallAssociation1.size() == lessSmallAssociation2.size()
            lessSmallAssociation1 != lessSmallAssociation2
            lessSmallAssociation1.hashCode() != lessSmallAssociation2.hashCode()

        where : 'We use the following entry data source:'
            keyType  |  valueType  |   entries
            Long     | Boolean     |  (0..24).collect(it -> Pair.of(it as Long, it%2==0)).toList()
            Short    | Byte        |  (0..24).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..24).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..24).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()

            Long     | Boolean     |  (0..128).collect(it -> Pair.of(it as Long, it%2==0)).toList()
            Short    | Byte        |  (0..128).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..128).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..128).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()

            Long     | Boolean     |  (0..1_000).collect(it -> Pair.of(it as Long, it%2==0)).toList()
            Short    | Byte        |  (0..1_000).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..1_000).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..1_000).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()

            Long     | Boolean     |  (0..10_000).collect(it -> Pair.of(it as Long, it%2==0)).toList()
            Short    | Byte        |  (0..10_000).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..10_000).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..10_000).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()
    }

    def 'The `equals` and `hashCode` implementations of a sorted Association works reliably after a series of modifications.'(
        Class<Object> keyType, Class<Object> valueType, List<Pair<Object, Object>> entries
    ) {
        reportInfo """
            Here we test the implementation of the `equals` and `hashCode` methods exhaustively.
            This may look like an exaggerated amount of test data and equality checks, but you
            have to know that under the hood specifically the `equals` implementations are
            highly optimized to specific cases which need to be covered.
            
            More specifically, if there are only small differences between sorted associations,
            we can avoid a lot of work due to two  sorted associations sharing most of their branches.
        """
        given : 'We create randomly sorted variants of the test data:'
            var keyHasher = {42 * it.hashCode() * (1997 * it.hashCode() ** 3)%256}
            Comparator<Object> randomSort = (a, b) -> (keyHasher(a)<=>keyHasher(b))
            entries.sort(randomSort)
            var firstHalf = entries.subList(0, (entries.size() * 0.5) as int)
            var secondHalf = entries.subList(1 + (entries.size() * 0.5) as int, entries.size())
        and :
            var originalAssociation = Association.betweenSorted(keyType, valueType).putAll(firstHalf)

        when :
            var quarter = firstHalf.subList(0, (firstHalf.size() * 0.5) as int)
            var toRemove = quarter.collect({it.first()}).toSet()
            var associationModified = originalAssociation.removeAll(toRemove)
        then :
            associationModified != originalAssociation
            associationModified.hashCode() != originalAssociation.hashCode()

        when :
            var subSubList1 = secondHalf.subList(0, (secondHalf.size() * 0.5) as int)
            var subSubList2 = secondHalf.subList((secondHalf.size() * 0.5) as int, secondHalf.size())
            var associationModified1 = associationModified.putAll(subSubList1)
            var associationModified2 = associationModified.putAll(subSubList2)
        then :
            associationModified1.size() == associationModified2.size()
            associationModified1 != associationModified2

        when :
            associationModified = associationModified.putAll(firstHalf)
        then :
            originalAssociation == associationModified
            originalAssociation.hashCode() == associationModified.hashCode()

        when :
            associationModified1 = associationModified.putAll(subSubList1)
            associationModified2 = associationModified.putAll(subSubList2)
        then :
            associationModified1.size() == associationModified2.size()
            associationModified1 != associationModified2

        where : 'We use the following entry data source:'
            keyType  |  valueType  |   entries
            Long     | Boolean     |  (0..24).collect(it -> Pair.of(it as Long, it%2==0)).toList()
            Short    | Byte        |  (0..24).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..24).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..24).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()

            Long     | Boolean     |  (0..128).collect(it -> Pair.of(it as Long, it%2==0)).toList()
            Short    | Byte        |  (0..128).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..128).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..128).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()

            Long     | Boolean     |  (0..1_000).collect(it -> Pair.of(it as Long, it%2==0)).toList()
            Short    | Byte        |  (0..1_000).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..1_000).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..1_000).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()

            Long     | Boolean     |  (0..10_000).collect(it -> Pair.of(it as Long, it%2==0)).toList()
            Short    | Byte        |  (0..10_000).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..10_000).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..10_000).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()
    }


    def 'The `update` method transforms existing values using a function while preserving the sorted association structure.'() {
        reportInfo """
            The `update` method provides a safe way to modify existing values in an association
            without having to manually check for key existence. It only applies the transformation
            to keys that actually exist in the association, making it ideal for selective updates.
            
            This method is particularly useful when you want to apply business logic transformations
            to specific entries without affecting the overall structure of the association.
        """

        given: 'A sorted association representing product inventory with initial quantities'
            var inventory = Association.betweenSorted(String, Integer).putAll(
                Pair.of("apples", 10),
                Pair.of("oranges", 15),
                Pair.of("bananas", 20),
                Pair.of("grapes", 8)
            )

        when: 'We update quantities for specific products that exist'
            var updatedInventory = inventory
                .update("apples") { quantity -> quantity + 5 }      // Restock apples
                .update("grapes") { quantity -> quantity - 2 }      // Sell some grapes
                .update("mangoes") { quantity -> quantity * 2 }     // Non-existent key - no change

        then: 'Only existing entries are updated as expected'
            updatedInventory.size() == 4
            updatedInventory.get("apples").get() == 15      // 10 + 5
            updatedInventory.get("oranges").get() == 15     // unchanged
            updatedInventory.get("bananas").get() == 20     // unchanged
            updatedInventory.get("grapes").get() == 6       // 8 - 2
            !updatedInventory.containsKey("mangoes")        // still doesn't exist

        and: 'The original association remains unchanged (immutability)'
            inventory.get("apples").get() == 10
            inventory.get("grapes").get() == 8
    }

    def 'The `update` method handles edge cases and exceptions appropriately for sorted associations.'() {
        given: 'A sorted association with various value types'
            var assoc = Association.betweenSorted(String, Object).putAll(
                Pair.of("number", 42),
                Pair.of("text", "hello"),
                Pair.of("list", [1, 2, 3])
            )

        when: 'Updating with a function that returns null'
            var result = assoc.update("number") { null }

        then: 'Null values are rejected with NullPointerException'
            thrown(NullPointerException)

        when: 'Updating with null key'
            assoc.update(null) { it }

        then: 'Null keys are rejected'
            thrown(NullPointerException)

        when: 'Updating with null function'
            assoc.update("number", null)

        then: 'Null functions are rejected'
            thrown(NullPointerException)
    }

    def 'Use `updateAll` with a Tuple of keys to batch-update multiple values efficiently on sorted associations.'() {
        reportInfo """
            The `updateAll(Tuple, Function)` method allows you to apply the same transformation
            to multiple keys in a single operation. This is more efficient than calling `update`
            multiple times and provides cleaner code when you need to apply uniform changes
            to a predefined set of entries.
            
            This method silently ignores keys that don't exist in the association, making it
            safe to use with dynamic key lists.
        """

        given: 'A sorted association representing student grades'
            var grades = Association.betweenSorted(String, Integer).putAll(
                Pair.of("Alice", 85),
                Pair.of("Bob", 72),
                Pair.of("Charlie", 90),
                Pair.of("Diana", 68),
                Pair.of("Eve", 95)
            )

        when: 'We apply a curve to specific students using a Tuple of keys'
            var curvedGrades = grades.updateAll(
                Tuple.of("Bob", "Diana", "Frank"),  // Frank doesn't exist - ignored
                { grade -> Math.min(grade + 5, 100) }  // Add 5 points, cap at 100
            )

        then: 'Only the specified existing students receive the curve'
            curvedGrades.size() == 5
            curvedGrades.get("Alice").get() == 85     // unchanged
            curvedGrades.get("Bob").get() == 77       // 72 + 5
            curvedGrades.get("Charlie").get() == 90   // unchanged
            curvedGrades.get("Diana").get() == 73     // 68 + 5
            curvedGrades.get("Eve").get() == 95       // unchanged
    }

    def 'Use `updateAll` with a ValueSet of keys for type-safe batch updates on sorted associations.'() {
        reportInfo """
            When you already have a ValueSet of keys (perhaps from filtering or other operations),
            the `updateAll(ValueSet, Function)` method provides a type-safe way to apply
            transformations to all matching entries.
            
            ValueSets maintain their own ordering and uniqueness guarantees, making this method
            particularly useful in data processing pipelines.
        """

        given: 'A sorted association of product prices and a ValueSet of products on sale'
            var prices = Association.betweenSorted(String, Double).putAll(
                Pair.of("laptop", 999.99d),
                Pair.of("mouse", 25.50d),
                Pair.of("keyboard", 75.00d),
                Pair.of("monitor", 299.99d),
                Pair.of("headphones", 149.99d)
            )

            var saleProducts = prices.keySet()
                .stream()
                .filter { key -> key in ["mouse", "keyboard", "headphones"] }
                .collect(ValueSet.collectorOf(String))

        when: 'We apply a discount to all sale products'
            var salePrices = prices.updateAll(saleProducts) { price -> price * 0.85 }  // 15% discount

        then: 'Only sale products receive the discount'
            salePrices.size() == 5
            salePrices.get("laptop").get() == 999.99      // unchanged
            salePrices.get("monitor").get() == 299.99     // unchanged
            salePrices.get("mouse").get() == 21.675       // 25.50 * 0.85
            salePrices.get("keyboard").get() == 63.75     // 75.00 * 0.85
            salePrices.get("headphones").get() == 127.4915 // 149.99 * 0.85
    }

    def 'Use `updateAll` with a Collection of keys for flexible batch updates on sorted associations.'() {
        reportInfo """
            The `updateAll(Collection, Function)` method accepts any Collection implementation,
            making it the most flexible option for batch updates. This is ideal when integrating
            with existing Java code that uses Lists, Sets, or other collection types.
            
            Like the other updateAll variants, it safely ignores keys that don't exist in
            the association.
        """

        given: 'An association of website traffic statistics'
            var traffic = Association.betweenSorted(String, Integer).putAll(
                Pair.of("/home", 1500),
                Pair.of("/about", 800),
                Pair.of("/products", 1200),
                Pair.of("/contact", 300),
                Pair.of("/blog", 950)
            )

            var highTrafficPages = ["/home", "/products", "/blog", "/nonexistent"] as List

        when: 'We add bonus traffic to high-performing pages'
            var updatedTraffic = traffic.updateAll(highTrafficPages) { views -> views + 100 }

        then: 'Only existing high-traffic pages receive the bonus'
            updatedTraffic.size() == 5
            updatedTraffic.get("/home").get() == 1600     // 1500 + 100
            updatedTraffic.get("/about").get() == 800     // unchanged
            updatedTraffic.get("/products").get() == 1300 // 1200 + 100
            updatedTraffic.get("/contact").get() == 300   // unchanged
            updatedTraffic.get("/blog").get() == 1050     // 950 + 100
    }

    def 'Use `updateAll` with a Stream of keys for efficient pipeline processing for sorted associations.'() {
        reportInfo """
            The `updateAll(Stream, Function)` method is designed for stream processing scenarios.
            When you have a stream of keys (perhaps from filtering, mapping, or other stream
            operations), this method allows you to efficiently apply transformations without
            intermediate collection steps.
            
            This is particularly useful in data processing pipelines where you want to maintain
            the lazy evaluation benefits of streams.
        """

        given: 'An association of employee data'
            var employees = Association.betweenSorted(String, Map).putAll(
                Pair.of("alice", [department: "Engineering", salary: 80000, level: "Senior"]),
                Pair.of("bob", [department: "Marketing", salary: 65000, level: "Mid"]),
                Pair.of("charlie", [department: "Engineering", salary: 75000, level: "Mid"]),
                Pair.of("diana", [department: "HR", salary: 60000, level: "Junior"])
            )

        when: 'We give raises to engineering employees using a filtered stream'
            var updatedEmployees = employees.updateAll(
                employees.entrySet().stream()
                    .filter { entry -> entry.second().department == "Engineering" }
                    .map { entry -> entry.first() },
                { employeeData ->
                    def updated = new HashMap(employeeData)
                    updated.salary = employeeData.salary * 1.10  // 10% raise
                    updated
                }
            )

        then: 'Only engineering employees receive raises'
            updatedEmployees.size() == 4
            updatedEmployees.get("alice").get().salary == 88000      // 80000 * 1.10
            updatedEmployees.get("bob").get().salary == 65000        // unchanged
            updatedEmployees.get("charlie").get().salary == 82500    // 75000 * 1.10
            updatedEmployees.get("diana").get().salary == 60000      // unchanged
    }

    def 'The update methods work correctly with large datasets and maintain performance.'(int size) {
        given: 'A large sorted association and a subset of keys to update'
            var largeAssoc = Association.betweenSorted(Integer, Integer)
            (0..<size).each { i -> largeAssoc = largeAssoc.put(i, i * 10) }

            var keysToUpdate = (0..<size).findAll { it % 3 == 0 }  // Update every 3rd key

        when: 'We update values for the selected keys'
            var updatedAssoc = largeAssoc.updateAll(keysToUpdate) { value -> value + 1000 }

        then: 'All specified keys are updated correctly'
            keysToUpdate.every { key ->
                updatedAssoc.get(key).get() == (key * 10) + 1000
            }

            (0..<size).findAll { it % 3 != 0 }.every { key ->
                updatedAssoc.get(key).get() == key * 10  // Unchanged keys
            }

        and: 'The association size remains the same'
            updatedAssoc.size() == size

        where:
            size << [100, 1000, 5000]
    }

    def 'Update methods can be chained for complex transformation pipelines on sorted associations.'() {
        reportInfo """
            Since all update methods return new Association instances, they can be chained
            together to create complex transformation pipelines. This functional style
            makes it easy to express multi-step data transformations in a readable way.
        """

        given: 'An association representing a shopping cart'
            var cart = Association.betweenSorted(String, Map).putAll(
                Pair.of("item1", [name: "Book", price: 29.99, quantity: 2, category: "education"]),
                Pair.of("item2", [name: "Pen", price: 2.99, quantity: 5, category: "office"]),
                Pair.of("item3", [name: "Notebook", price: 4.99, quantity: 3, category: "office"])
            )

        when: 'We apply a series of transformations in a pipeline'
            var processedCart = cart
                // Apply 10% discount to education items
                .update("item1") { item ->
                    def updated = new HashMap(item)
                    updated.price = item.price * 0.90
                    updated
                }
                // Apply bulk discount for office supplies with quantity > 3
                .updateAll(["item2", "item3"] as Set) { item ->
                    if (item.quantity > 3) {
                        def updated = new HashMap(item)
                        updated.price = item.price * 0.95  // 5% bulk discount
                        updated
                    } else {
                        item
                    }
                }
                // Calculate total for each item
                .updateAll(cart.keySet().stream()) { item ->
                    def updated = new HashMap(item)
                    updated.total = item.price * item.quantity
                    updated
                }

        then: 'All transformations are applied correctly'
            processedCart.get("item1").get().price == 26.991  // 29.99 * 0.90
            processedCart.get("item1").get().total == 53.982  // 26.991 * 2

            processedCart.get("item2").get().price == 2.8405  // 2.99 * 0.95 (bulk discount)
            processedCart.get("item2").get().total == 14.2025 // 2.8405 * 5

            processedCart.get("item3").get().price == 4.99    // no bulk discount (quantity <= 3)
            processedCart.get("item3").get().total == 14.97   // 4.99 * 3
    }

    def 'Update methods preserve sorted association characteristics.'() {
        given: 'A sorted association'
            var sortedAssoc = Association.betweenSorted(String, Integer)
                .put("Charlie", 30)
                .put("Alice", 25)
                .put("Bob", 28)

        when: 'We update values in the sorted association'
            var updatedSorted = sortedAssoc.update("Alice") { age -> age + 1 }

        then: 'The sorted characteristic is preserved'
            updatedSorted.isSorted()
            updatedSorted.keySet().toList() == ["Alice", "Bob", "Charlie"]  // Still sorted
    }

    def 'Update methods handle complex value types and nested structures.'() {
        given: 'A sorted association with complex nested values'
            var complexData = Association.betweenSorted(String, Map).putAll(
                Pair.of("user1", [
                    profile: [name: "Alice", age: 30],
                    preferences: [theme: "dark", notifications: true],
                    stats: [logins: 150, lastActive: "2024-01-15"]
                ]),
                Pair.of("user2", [
                    profile: [name: "Bob", age: 25],
                    preferences: [theme: "light", notifications: false],
                    stats: [logins: 75, lastActive: "2024-01-10"]
                ])
            )

        when: 'We update nested structures within the values'
            var updatedData = complexData.updateAll(["user1", "user2"] as Set) { userData ->
                def updated = new HashMap(userData)
                // Increment login count and update last active
                updated.stats = new HashMap(userData.stats)
                updated.stats.logins = userData.stats.logins + 1
                updated.stats.lastActive = "2024-01-20"
                updated
            }

        then: 'Nested structures are updated correctly'
            updatedData.get("user1").get().stats.logins == 151
            updatedData.get("user1").get().stats.lastActive == "2024-01-20"
            updatedData.get("user2").get().stats.logins == 76
            updatedData.get("user2").get().stats.lastActive == "2024-01-20"

            // Other fields remain unchanged
            updatedData.get("user1").get().profile.name == "Alice"
            updatedData.get("user2").get().preferences.theme == "light"
    }
}
