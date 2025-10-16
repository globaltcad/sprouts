package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.Month
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

@Title("Linked Association - a Data Oriented Map")
@Narrative('''

    Sprouts is a property library with a heavy focus on bridging the
    gap between classical place oriented programming and modern
    data oriented programming. 
    This is why sprouts has both the `Tuple` and `Association` classes.
    The `Association` class is a value object that represents a set of
    key-value pairs. You can think of it as an immutable map, but with
    an API that is designed around transforming the data in the map
    rather than mutating it.
    
    One type of association is the "linked Association", which is
    similar to a `LinkedHashMap` in Java, but with an API that is
    designed around data transformation rather than mutation.
    
''')
@Subject([Association])
class Linked_Association_Spec extends Specification
{
    enum Operation {
        ADD, REMOVE,
        REPLACE,  // Replace existing key's value
        PUT_IF_ABSENT, // Add only if absent
        CLEAR     // Clear all entries
    }

    def 'An empty association is created by supplying the type of the key and value'() {
        given:
            var association = Association.betweenLinked(String, Integer)

        expect:
            !association.isSorted()
            !association.isNotEmpty()
            association.isEmpty()
            association.keyType() == String
            association.valueType() == Integer
    }

    def 'A linked association is invariant to a linked hash map.'(
        List<Tuple2<Operation, String>> operations
    ) {
        given:
            var association = Association.betweenLinked(String, String)
            var map = [:] as LinkedHashMap<String,String>
            var operationsApplier = { currentAssociations ->
                operations.each { operation, key ->
                    switch (operation) {
                        case Operation.ADD:
                            currentAssociations = currentAssociations.put(key, "value of " + key)
                            map[key] = "value of " + key
                            break
                        case Operation.REMOVE:
                            currentAssociations = currentAssociations.remove(key)
                            map.remove(key)
                            break
                    }
                    if ( map.size() != currentAssociations.size() ) {
                        throw new IllegalStateException(
                            "The size of the linked map and the linked association are not equal after element '$key' was introduced "+
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

        when : 'We apply the operations to the associations and the map a few more times.'
            10.times {
                association = operationsApplier(association) as Association<String, String>
            }
        then: 'The associations and the map are still equal in terms of size, keys, and values.'
            association.size() == map.size()
            association.keySet().toSet() == map.keySet()
            association.values().sort().toList() == map.values().sort()
        and : 'We can lookup any value from its corresponding key.'
            map.values() as Set == map.keySet().collect { key -> association.get(key).orElseThrow(MissingItemException::new) } as Set

        when : 'We use the `toMap()` method to convert the association to a map.'
            var convertedMap = association.toMap()
        then: 'The converted map is equal to the reference map.'
            convertedMap == map

        when : 'We use the stream API to map both the association and the reference map.'
            var mappedAssociation = association.entrySet().stream().map({ it.withFirst(it.first().toUpperCase() + "!") }).filter({ it.hashCode() % 2 == 0 }).collect(Association.collectorOfLinked(String, String))
            var mappedMap = map.entrySet().stream().map({ Pair.of(it.key.toUpperCase() + "!", it.value) }).filter({ it.hashCode() % 2 == 0 }).collect(Collectors.toMap({it.first()}, {it.second()}))
        then : 'The mapped association and map are equal.'
            mappedAssociation.toMap() == mappedMap

        when : 'We use the parallel stream API to map both the association and the reference map.'
            var mappedAssociationParallel = association.entrySet().parallelStream().map({ it.withFirst(it.first().toUpperCase() + "!") }).filter({ it.hashCode() % 2 == 0 }).collect(Association.collectorOfLinked(String, String))
            var mappedMapParallel = map.entrySet().parallelStream().map({ Pair.of(it.key.toUpperCase() + "!", it.value) }).filter({ it.hashCode() % 2 == 0 }).collect(Collectors.toMap({it.first()}, {it.second()}))
        then : 'The mapped association and map are equal in terms of their contents.'
            mappedAssociationParallel.toMap() == mappedMapParallel
        and : 'Finally, we also check the `isSorted()` flag:'
            !association.isSorted()

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
                    })
            ]
    }

    def 'Association maintains full invariance with Map across all operations.'(
            List<Tuple2<Operation, String>> operations
    ) {
        given: 'Fresh association and reference map'
            var assoc = Association.betweenLinked(String, String)
            var map = new LinkedHashMap<String,String>()
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
            3.times { assoc = operationsApplier(assoc) }
        then: 'Immediate invariance'
            assoc.size() == map.size()
            assoc.keySet().toSet() == map.keySet()
            assoc.values().sort().toList() == map.values().sort()
            assoc.toMap() == map
        and :
            map.every { k, v ->
                assoc.get(k).orElse(null) == v
            }
            assoc.every { pair ->
                pair.second() == map[pair.first()]
            }
        and :
            !assoc.isSorted()

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

    def 'Two linked associations with the same operations applied to them are always equal.'(
        List<Tuple2<Operation, String>> operations
    ) {
        given: 'We create an association and a map as well as a closure to apply the operations to them.'
            var association = Association.betweenLinked(String, String)
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
            var association1 = Association.betweenLinked(String, Integer)
            var association2 = Association.betweenLinked(String, Integer)
        when : 'We add some values to the first association.'
            association1 = association1.put("a", 1).put("b", 2).put("c", 3)
        and : 'We add some values to the second association.'
            association2 = association2.put("d", 4).put("e", 5).put("f", 6)
        and : 'We merge the two associations.'
            var mergedAssociations = association1.putAll(association2)
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
            var association1 = Association.betweenLinked(String, Integer)
            var association2 = Association.betweenLinked(String, Integer)
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

    def 'You can remove the entries of one linked association from another, using the `removeAll(Set)` method.'() {
        given : 'Two empty associations between strings and integers.'
            var association1 = Association.betweenLinked(String, Integer)
            var association2 = Association.betweenLinked(String, Integer)
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

    def 'You remove the entries of one linked association from another, using the `removeAll(Stream)` method.'() {
        given : 'Two empty associations between strings and integers.'
            var association1 = Association.betweenLinked(String, Integer)
            var association2 = Association.betweenLinked(String, Integer)
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

    def 'You can remove entries from a linked association selectively, using `removeIf(Predicate)`.'(
            Class<?> type, List<Object> elements, Predicate<Pair<Object, String>> predicate
    ) {
        reportInfo """
            You can remove key/value pairs from an association which satisfy
            a given `Predicate` lambda. Or in other words,
            if the `Predicate.test(Object)` method yields `true` for a particular
            pair, then it will be removed, otherwise, it will remain in the
            returned association.
        """
        given : 'We create a linked association and a regular linked JDK map containing some test elements.'
            var assoc = Association.betweenLinked(type, String).putAll(elements.stream().map(it->Pair.of(it, it.toString())))
            Map<?,String> map = new LinkedHashMap<Object, String>().putAll(elements.stream().map(it->new AbstractMap.SimpleEntry<>(it, it.toString())).collect(Collectors.toList()))

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
            var association = Association.betweenLinked(String, Integer)
            association = association.put("a", 1).put("b", 2).put("c", 3)
        expect : 'The association is not empty.'
            association.isNotEmpty()
            !association.isEmpty()
        and : 'It tells us that it is a linked association.'
            !association.isSorted()
            association.isLinked()

        when : 'We clear the linked association.'
            association = association.clear()
        then : 'The association is empty, linked but not sorted.'
            association.isEmpty()
            association.isLinked()
            !association.isSorted()
        and : 'The key and value types are the same.'
            association.keyType() == String
            association.valueType() == Integer
    }

    def 'You can merge two large linked associations into one using the `putAll` method.'(int size) {
        given : 'We create two associations and some random key and value generators.'
            var association1 = Association.betweenLinked(String, Character)
            var association2 = Association.betweenLinked(String, Character)
            var indexToRandomKey = { index -> Integer.toHexString(Math.abs((929 * (42 + index as int)) % size)) }
            var indexToRandomValue = { index -> Math.abs(65+(index as int)) as char }
        and : 'We create a regular mutable map to check the results are correct.'
            var map = [:] as LinkedHashMap<String, Character>
            var map1 = [:] as LinkedHashMap<String, Character>
            var map2 = [:] as LinkedHashMap<String, Character>
        when : 'We add some values to the first association.'
            size.times { index ->
                var key = indexToRandomKey(index%(size/2))
                var value = indexToRandomValue(index)
                association1 = association1.put(key, value)
                map[key] = value
                map1[key] = value
            }
        and : 'We add some values to the second association.'
            size.times { index ->
                var key = indexToRandomKey(-index%(size/2))
                var value = indexToRandomValue(index)
                association2 = association2.put(key, value)
                map[key] = value
                map2[key] = value
            }
        then :
            association1.size() == map1.size()
            association2.size() == map2.size()
            association1.toMap() == map1
            association2.toMap() == map2
        when : 'We merge the two associations.'
            var mergedAssociations = association1.putAll(association2)
        then : 'The merged association contains all the values from the two associations.'
            mergedAssociations.size() == map.size()
            mergedAssociations.keySet() as Set == map.keySet()
            mergedAssociations.values().sort().toList() == map.values().sort()

        when : 'We use the `toMap()` method to convert the linked association to a linked map.'
            var convertedMap = mergedAssociations.toMap()
        then: 'The converted map is equal to the reference map.'
            convertedMap == map

        where :
            size << [10, 100, 1_000, 10_000]
    }

    def 'You can remove the entries of a large association from another, using the `removeAll` method.'(int size) {
        given : 'Two empty associations between strings and integers.'
            var associations1 = Association.betweenLinked(String, Integer)
            var associations2 = Association.betweenLinked(String, Integer)
            var indexToRandomKey = { index -> Integer.toHexString(Math.abs((929 * (42 + index as int)) % size)) }
            var indexToRandomValue = { index -> Math.abs(65+(index as int)) }
        and : 'We create two regular mutable maps to check the results are correct based on their `remove` method.'
            var map1 = [:] as LinkedHashMap<String, Integer>
            var map2 = [:] as LinkedHashMap<String, Integer>
        when : 'We add some values to the first association and map.'
            size.times { index ->
                var key = indexToRandomKey(index%(size/2))
                var value = indexToRandomValue(index)
                associations1 = associations1.put(key, value)
                map1[key] = value
            }
        and : 'We add some values to the second association and map.'
            size.times { index ->
                var key = indexToRandomKey(-index%(size/2))
                var value = indexToRandomValue(index)
                associations2 = associations2.put(key, value)
                map2[key] = value
            }
        and : 'We remove the entries of the second association from the first.'
            var result = associations1.removeAll(associations2.keySet())
        and : 'We remove the entries of the second map from the first.'
            map2.forEach { key, value -> map1.remove(key) }
        then : 'The result contains only the entries that were not in the second association.'
            result.size() == map1.size()
            result.keySet() as Set == map1.keySet()
            result.values().sort().toList() == map1.values().sort()

        where :
            size << [10, 100, 1_000, 10_000]
    }

    def 'You can merge a large linked association and a map into a single association using the `putAll` method.'(int size) {
        given : 'We create an association and a map and some random key and value generators.'
            var associations = Association.betweenLinked(String, Character)
            var otherMap = [:] as LinkedHashMap<String, Character>
            var indexToRandomKey = { index -> Integer.toHexString(Math.abs((929 * (42 + index as int)) % size)) }
            var indexToRandomValue = { index -> Math.abs(65+(index as int)) as char }
        and : 'We create a regular mutable map to check the results are correct.'
            var map = [:] as LinkedHashMap<String, Character>
        when : 'We add some values to the first association.'
            size.times { index ->
                var key = indexToRandomKey(index%(size/2))
                var value = indexToRandomValue(index)
                associations = associations.put(key, value)
                map[key] = value
            }
        and : 'We add some values to the second association.'
            size.times { index ->
                var key = indexToRandomKey(-index%(size/2))
                var value = indexToRandomValue(index)
                otherMap[key] = value
                map[key] = value
            }
        and : 'We merge the two linked associations.'
            var mergedAssociations = associations.putAll(otherMap)
        then : 'The merged association contains all the values from the two associations.'
            mergedAssociations.size() == map.size()
            mergedAssociations.keySet() as Set == map.keySet()
            mergedAssociations.values().sort().toList() == map.values().sort()

        when : 'We loop over both the merged association as well as the reference map and collect the pairs.'
            var listFromAssociation = []
            var listFromMap = []
            for ( pair in mergedAssociations ) {
                listFromAssociation.add(pair)
            }
            for ( entry in map ) {
                listFromMap.add(Pair.of(entry.key, entry.value))
            }
        then : 'We verify, the two lists are completely equal.'
            listFromAssociation == listFromMap

        where :
            size << [10, 100, 1_000, 10_000]
    }

    def 'A linked `Association` has an intuitive string representation.'() {
        given :
            var associations = Association.betweenLinked(String, Integer)
        when : 'We add some values to the association.'
            associations = associations.put("a", 1).put("b", 2).put("c", 3)
        then : 'The string representation of the association is as expected.'
            associations.toString() == 'LinkedAssociation<String,Integer>["a" ↦ 1, "b" ↦ 2, "c" ↦ 3]'
    }

    def 'A larger linked `Association` will have a trimmed string representation.'() {
        given :
            var associations = Association.betweenLinked(Character, Byte)
        when : 'We add some values to the association.'
            30.times { index ->
                var key = Math.abs(65+(index)) as char
                var value = Math.abs(1997*index) as byte
                associations = associations.put(key, value)
            }
        then : 'The string representation of the association is as expected.'
            associations.toString() == "LinkedAssociation<Character,Byte>['A' ↦ 0, 'B' ↦ -51, 'C' ↦ -102, 'D' ↦ 103, 'E' ↦ 52, 'F' ↦ 1, 'G' ↦ -50, 'H' ↦ -101, , ...22 more entries]"
    }

    def 'The `replace` method replaces the value of a key with a new value, if and only if the key is present.'() {
        given :
            var associations = Association.betweenLinked(String, Integer)

        when : 'We add some values to the association.'
            associations = associations.put("a", 1).put("b", 2).put("c", 3)
        and : 'We replace the value of a key that is present.'
            associations = associations.replace("b", 4)
        then : 'The value of the key is replaced.'
            associations.get("b").orElseThrow(MissingItemException::new) == 4
        and : 'The size of the association remains the same.'
            associations.size() == 3

        when : 'We call the `replace` method with a key that is not present.'
            associations = associations.replace("d", 5)
        then : 'The size of the association remains the same.'
            associations.size() == 3
        and : 'The value of the key is not replaced.'
            !associations.get("d").isPresent()
    }

    def 'Use `putAll(Pair...)` to populate an association at once from multiple pairs.'() {
        given :
            var associations = Association.betweenLinked(String, Integer)
        when : 'We add some values to the association.'
            associations = associations.putAll(
                Pair.of("I", 1),
                Pair.of("was", 2),
                Pair.of("added", 3),
                Pair.of("to", 4),
                Pair.of("the", 5),
                Pair.of("association", 6)
            )
        then : 'The association contains the values.'
            associations.size() == 6
            associations.get("I").orElseThrow(MissingItemException::new) == 1
            associations.get("was").orElseThrow(MissingItemException::new) == 2
            associations.get("added").orElseThrow(MissingItemException::new) == 3
            associations.get("to").orElseThrow(MissingItemException::new) == 4
            associations.get("the").orElseThrow(MissingItemException::new) == 5
            associations.get("association").orElseThrow(MissingItemException::new) == 6
    }

    def 'Use `putAll(Tuple<Pair>)` to populate an association at once from multiple pairs.'() {
        given :
            var associations = Association.betweenLinked(Integer, String)
        when : 'We add some values to the association.'
            associations = associations.putAll(
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
            associations.get(2).orElseThrow(MissingItemException::new) == "was"
            associations.get(3).orElseThrow(MissingItemException::new) == "added"
            associations.get(4).orElseThrow(MissingItemException::new) == "to"
            associations.get(5).orElseThrow(MissingItemException::new) == "the"
            associations.get(6).orElseThrow(MissingItemException::new) == "association"
    }

    def 'Use `putAll(Collection<Pair>)` to populate an association at once from multiple pairs.'() {
        reportInfo """
            This method ensures compatibility with the `Collection` interface, which
            is especially useful when you have a list of pairs that you want to
            populate the association with.
        """
        given :
            var associations = Association.betweenLinked(Integer, String)
        when : 'We add some values to the association.'
            associations = associations.putAll([
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
            associations.get(2).orElseThrow(MissingItemException::new) == "was"
            associations.get(3).orElseThrow(MissingItemException::new) == "added"
            associations.get(4).orElseThrow(MissingItemException::new) == "to"
            associations.get(5).orElseThrow(MissingItemException::new) == "the"
            associations.get(6).orElseThrow(MissingItemException::new) == "association"
    }

    def 'Use `putAll(Set<Pair>)` to populate an association at once from multiple pairs.'() {
        reportInfo """
            This method ensures compatibility with the `Set` interface, which
            is especially useful when you have a set of pairs that you want to
            populate the association with.
        """
        given :
            var associations = Association.betweenLinked(Character, String)
        when : 'We add some values to the association.'
            associations = associations.putAll([
                Pair.of('I' as char, "I"),
                Pair.of('w' as char, "was"),
                Pair.of('a' as char, "added"),
                Pair.of('t' as char, "to"),
                Pair.of('t' as char, "the"), // overwrites the previous value
                Pair.of('a' as char, "association") // overwrites the previous value
            ] as Set)
        then : 'The association contains the values.'
            associations.size() == 4
            associations.get('I' as char).orElseThrow(MissingItemException::new) == "I"
            associations.get('w' as char).orElseThrow(MissingItemException::new) == "was"
            associations.get('a' as char).orElseThrow(MissingItemException::new) == "association"
            associations.get('t' as char).orElseThrow(MissingItemException::new) == "the"
    }

    def 'The `putIfAbsent(K, V)` method adds a key-value pair to the association if, and only if, the key is not present.'() {
        given : 'We create an association between shorts and chars and add a bunch of entries to the association.'
            var associations = Association.betweenLinked(Short, Character).putAll([
                Pair.of(11 as short, 'a' as char),
                Pair.of(22 as short, 'b' as char),
                Pair.of(33 as short, 'c' as char),
                Pair.of(44 as short, 'x' as char),
                Pair.of(55 as short, 'y' as char),
                Pair.of(66 as short, 'z' as char)
            ])
        when : 'We add a new entry to the association.'
            associations = associations.putIfAbsent(77 as short, 'w' as char)
        then : 'The new entry is added to the association.'
            associations.size() == 7
            associations.get(77 as short).orElseThrow(MissingItemException::new) == 'w' as char

        when : 'We try to add an entry with a key that is already present.'
            associations = associations.putIfAbsent(44 as short, 'v' as char)
        then : 'The association remains the same.'
            associations.size() == 7
            associations.get(44 as short).orElseThrow(MissingItemException::new) == 'x' as char

        when : 'We now ry this for every key in the association.'
            associations.keySet().forEach { key ->
                associations = associations.putIfAbsent(key, '!' as char)
            }
        then : 'The association still remains the same.'
            associations.size() == 7
            associations.get(44 as short).orElseThrow(MissingItemException::new) == 'x' as char
    }

    def 'Use `Association.betweenLinked(Number.class, Number.class)` to create an association between all kinds of numbers.'() {
        given :
            var associations = Association.betweenLinked(Number.class, Number.class)
        expect :
            associations.isEmpty()
            associations.keyType() == Number.class
            associations.valueType() == Number.class
        when : 'We add some values to the association.'
            associations = associations.put(1, 1).put(2L, 2L).put(3.0f, 3.0f).put(4.0, 4.0)
        then : 'The association contains the values.'
            associations.size() == 4
            associations.get(1).orElseThrow(MissingItemException::new) == 1
            associations.get(2L).orElseThrow(MissingItemException::new) == 2L
            associations.get(3.0f).orElseThrow(MissingItemException::new) == 3.0f
            associations.get(4.0).orElseThrow(MissingItemException::new) == 4.0
    }

    def 'The of factory method throws NPE for null parameters'() {
        when:
            Association.ofLinked(null, "value")
        then:
            thrown(NullPointerException)

        when:
            Association.ofLinked("key", null)
        then:
            thrown(NullPointerException)

        when:
            Association.ofLinked("key", "value")
        then:
            noExceptionThrown()
    }

    def 'The `entrySet` is immutable and contains correct pairs'() {
        given:
            var assoc = Association.ofLinked("a", 1).put("b", 2)
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
            var assoc = Association.betweenLinked(Integer, String).putAll(
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

    def 'Using `replaceAll(Map)` only updates existing keys in a linked `Association`.'() {
        given:
            var original = Association.ofLinked("a", 1).put("b", 2).put("c", 3)
            var replacementMap = [a:10, d:40, c:30] as LinkedHashMap
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
            var original = Association.ofLinked("a", 1).put("b", 2).put("c", 3)
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
            var original = Association.ofLinked("a", 1).put("b", 2).put("c", 3)
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
            var original = Association.ofLinked("a", 1).put("b", 2).put("c", 3)
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

    def 'The `retainAll(Set)` method keeps only specified keys.'() {
        given:
            var assoc = Association.ofLinked("a", 1).put("b", 2).put("c", 3)
        when:
            var retained = assoc.retainAll(["a", "c"] as Set)
        then:
            retained.size() == 2
            retained.containsKey("a")
            retained.containsKey("c")
            !retained.containsKey("b")
    }

    def 'Use `putAllIfAbsent(Pair...)` to populate a linked association at once from multiple pairs.'() {
        reportInfo """
            The `putAllIfAbsent(Pair...)` method allows you to populate a linked association from
            an array of entry pairs. However, it only adds new entries to the new association,
            if they are not already present.
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create a linked association with some initial entries:'
            var associations = Association.betweenLinked(String, Integer).putAll(
                Pair.of("was", -1),
                Pair.of("the", -2),
                Pair.of("association", -3)
            )
        when : 'We add some values to the linked association.'
            associations = associations.putAllIfAbsent(
                Pair.of("I", 1),
                Pair.of("was", 2),
                Pair.of("added", 3),
                Pair.of("to", 4),
                Pair.of("the", 5),
                Pair.of("association", 6)
            )
        then : 'The linked association contains the values.'
            associations.size() == 6
            associations.get("I").orElseThrow(MissingItemException::new) == 1
            associations.get("was").orElseThrow(MissingItemException::new) == -1
            associations.get("added").orElseThrow(MissingItemException::new) == 3
            associations.get("to").orElseThrow(MissingItemException::new) == 4
            associations.get("the").orElseThrow(MissingItemException::new) == -2
            associations.get("association").orElseThrow(MissingItemException::new) == -3
    }

    def 'Use `putAllIfAbsent(Tuple<Pair>)` to populate a linked association at once from multiple pairs.'() {
        reportInfo """
            The `putAllIfAbsent(Tuple<Pair>)` method allows you to populate a linked association from
            a tuple of entry pairs. However, it only adds new entries to the new association,
            if they are not already present.
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create a linked association with some initial entries:'
            var associations = Association.betweenLinked(Integer, String).putAll(
                Pair.of(2, ":o"),
                Pair.of(4, ":3"),
                Pair.of(6, ":I")
            )
        when : 'We add some values to the linked association using the `putAllIfAbsent` method:'
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
        then : 'The linked association contains the values.'
            associations.size() == 6
            associations.get(1).orElseThrow(MissingItemException::new) == "I"
            associations.get(2).orElseThrow(MissingItemException::new) == ":o"
            associations.get(3).orElseThrow(MissingItemException::new) == "added"
            associations.get(4).orElseThrow(MissingItemException::new) == ":3"
            associations.get(5).orElseThrow(MissingItemException::new) == "the"
            associations.get(6).orElseThrow(MissingItemException::new) == ":I"
    }

    def 'Use `putAllIfAbsent(Collection<Pair>)` to populate a linked association at once from multiple pairs.'() {
        reportInfo """
            This method ensures compatibility with the `Collection` interface, which
            is especially useful when you have a list of pairs that you want to
            populate the association with if they are not already present.
            
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create a linked association with some initial entries:'
            var associations = Association.betweenLinked(Integer, String).putAll(
                Pair.of(2, ":o"),
                Pair.of(4, ":3"),
                Pair.of(6, ":I")
            )
        when : 'We add some values to the linked association using the `putAllIfAbsent` method:'
            associations = associations.putAllIfAbsent([
                Pair.of(1, "I"),
                Pair.of(2, "was"),
                Pair.of(3, "added"),
                Pair.of(4, "to"),
                Pair.of(5, "the"),
                Pair.of(6, "association")
            ])
        then : 'The linked association contains the values.'
            associations.size() == 6
            associations.get(1).orElseThrow(MissingItemException::new) == "I"
            associations.get(2).orElseThrow(MissingItemException::new) == ":o"
            associations.get(3).orElseThrow(MissingItemException::new) == "added"
            associations.get(4).orElseThrow(MissingItemException::new) == ":3"
            associations.get(5).orElseThrow(MissingItemException::new) == "the"
            associations.get(6).orElseThrow(MissingItemException::new) == ":I"
    }

    def 'Use `putAllIfAbsent(Set<Pair>)` to populate a linked association at once from multiple pairs.'() {
        reportInfo """
            This method ensures compatibility with the `Set` interface, which
            is especially useful when you have a collection of key-value pairs (represented as `Pair` objects) that you want to
            use to populate the association with if they are not already present.
            
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create a linked association with some initial entries:'
            var associations = Association.betweenLinked(Character, String).putAll(
                Pair.of('w' as char, ":)"),
                Pair.of('t' as char, ":P")
            )
        when : 'We add some values to the linked association using the `putAllIfAbsent` method:'
            associations = associations.putAllIfAbsent([
                Pair.of('I' as char, "I"),
                Pair.of('w' as char, "was"),
                Pair.of('a' as char, "added"),
                Pair.of('t' as char, "to"),
                Pair.of('t' as char, "the"), // does not overwrite the previous value
                Pair.of('a' as char, "association") // does not overwrite the previous value
            ] as Set)
        then : 'The linked association contains the values.'
            associations.size() == 4
            associations.get('I' as char).orElseThrow(MissingItemException::new) == "I"
            associations.get('w' as char).orElseThrow(MissingItemException::new) == ":)"
            associations.get('a' as char).orElseThrow(MissingItemException::new) == "added"
            associations.get('t' as char).orElseThrow(MissingItemException::new) == ":P"
    }

    def 'Use `putAllIfAbsent(Map<K,V>)` to populate a linked association at once from a regular Java map.'() {
        reportInfo """
            This method ensures compatibility with the `Map` interface, which
            is especially useful when you have a set of pairs that you want to
            populate the association with if they are not already present.
            
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create an association with some initial entries:'
            var associations = Association.betweenLinked(Character, String).putAll(
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

    def 'Use `putAllIfAbsent(Association<K,V>)` to populate a linked association from another one.'() {
        reportInfo """
            This method merges a linked association with another one
            in a way where only those key-value pairs are added from the supplied 
            association, whose keys are not already present in the targeted one.
            
            So contrary to `putAll`, the `putAllIfAbsent` does not overwrite existing entries!
        """
        given : 'We create a linked association with some initial entries:'
            var associations = Association.betweenLinked(Character, String).putAll(
                Pair.of('w' as char, ":)"),
                Pair.of('t' as char, ":P")
            )
        when : 'We add some values to the association.'
            associations = associations.putAllIfAbsent(Association.betweenLinked(Character, String).putAll(
                Pair.of('I' as char, "I"),
                Pair.of('w' as char, "was"),
                Pair.of('a' as char, "added"),
                Pair.of('t' as char, "to"),
                Pair.of('t' as char, "the"),
                Pair.of('a' as char, "association")
            ))
        then : 'The association contains the expected values.'
            associations.size() == 4
            associations.get('I' as char).orElseThrow(MissingItemException::new) == "I"
            associations.get('w' as char).orElseThrow(MissingItemException::new) == ":)"
            associations.get('a' as char).orElseThrow(MissingItemException::new) == "association"
            associations.get('t' as char).orElseThrow(MissingItemException::new) == ":P"
    }

    def 'The `containsKey` method of a linked `Association` throws an exception when passing arguments of the wrong type.'()
    {
        given :
            var association = Association.betweenLinked(Integer, Number)

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

    def 'The `remove` method of a linked `Association` throws an exception when passing arguments of the wrong type.'()
    {
        given :
            var association = Association.betweenLinked(Integer, Number)

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

    def 'The `get` method of a linked `Association` throws an exception when passing arguments of the wrong type.'()
    {
        given :
            var association = Association.betweenLinked(Integer, Number)

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

    def 'The `putIfAbsent` does not overwrite an existing value already stored in a linked association.'() {
        given:
            var assoc = Association.ofLinked("a", 1).putIfAbsent("a", 2)
        expect:
            assoc.get("a").get() == 1
    }

    def 'The `putIfAbsent` method of a linked `Association` throws an exception when passing arguments of the wrong type.'()
    {
        given :
            var association = Association.betweenLinked(Integer, Number)

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

    def 'The `put` method of a linked `Association` throws an exception when passing arguments of the wrong type.'()
    {
        given :
            var association = Association.betweenLinked(Integer, Number)

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

    def 'Linked associations with same entries, but in different order are still equal.'() {
        given:
            var assoc1 = Association.ofLinked("a", 1).put("b", 2).put("c", 3)
            var assoc2 = Association.ofLinked("c", 3).put("b", 2).put("a", 1)
        expect:
            assoc1 == assoc2
            assoc1.hashCode() == assoc2.hashCode()
    }

    def 'The `values()` returns a tuple containing all values, including duplicates.'() {
        given:
            var assoc = Association.ofLinked("a", 10)
                .put("b", 20)
                .put("c", 10) // Duplicate value
        when:
            var values = assoc.values()
        then:
            values.size() == 3
            values.sort().toList() == [10, 10, 20]
    }

    def 'The `replaceAll(Stream)` method ignores non-existing keys in replacement pairs.'() {
        given:
            var original = Association.ofLinked("a", 1).put("b", 2)
            var replacements = [Pair.of("k", -40), Pair.of("a", 10), Pair.of("v", 30)]
        when:
            var updated = original.replaceAll(replacements.stream())
        then:
            updated.size() == 2
            updated.get("a").get() == 10
            updated.get("b").get() == 2 // Unchanged
            !updated.containsKey("k")
            !updated.containsKey("v")
    }

    def 'clear on empty association returns an empty instance'() {
        given:
            var emptyAssoc = Association.betweenLinked(String, Integer).clear()
        expect:
            emptyAssoc.isEmpty()
            emptyAssoc.keyType() == String
            emptyAssoc.valueType() == Integer
    }

    def 'The linked `Association` is an `Iterable` which allows you to iterate over its entries.'() {
        given:
            var associations = Association.ofLinked("x", 1).put("y", 2).put("z", 3)
        when:
            var entries = []
            for (var entry : associations) {
                entries << entry
            }
        then:
            entries.size() == 3
            entries.contains(Pair.of("x", 1))
            entries.contains(Pair.of("y", 2))
            entries.contains(Pair.of("z", 3))
    }

    def 'Use `Association.collectorOfLinked(Class,Class)` to collect a stream of `Pair`s into a new linked `Association`.'() {
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
            var associations = sentence.collect(Association.collectorOfLinked(Integer, String))
        then:
            associations.size() == 6
            associations.get(0).get() == "I"
            associations.get(1).get() == "watch"
            associations.get(2).get() == "dominion"
            associations.get(3).get() == "documentary"
            associations.get(4).get() == "on"
            associations.get(5).get() == "www.dominionmovement.com"
    }

    def 'Use the `sort(Comparator)` method to create a sorted association from a linked association.'() {
        reportInfo """
            The `sort` takes a `Comparator` that defines the order of the entries in the association
            and then returns a new `Association` with the entries sorted according to that comparator.
            This new association is based on a binary tree structure, so it is still efficient
            for lookups. (Although not as efficient as the original `Association`)
        """
        given:
            var documentaryTimes = Association.betweenLinked(String, String).putAll(
                Pair.of("Dominion", "125min"),
                Pair.of("Forks over Knives", "96min"),
                Pair.of("Land of Hope and Glory", "48min"),
                Pair.of("Earthlings", "95min"),
                Pair.of("Cowspiracy", "91min"),
            )
        expect :
            !documentaryTimes.isSorted()
        when:
            var sortedDocumentaries = documentaryTimes.sort( (Comparator<String>) { a, b -> a.compareTo(b) } )
        then:
            sortedDocumentaries.isSorted()
            sortedDocumentaries.size() == 5
            sortedDocumentaries.keySet().toList() == [
                "Cowspiracy",
                "Dominion",
                "Earthlings",
                "Forks over Knives",
                "Land of Hope and Glory"
            ]

        when:
            var inverseDocumentaries = documentaryTimes.sort( (Comparator<String>) { a, b -> b.compareTo(a) } )
        then:
            inverseDocumentaries.isSorted()
            inverseDocumentaries.size() == 5
            inverseDocumentaries.keySet().toList() == [
                "Land of Hope and Glory",
                "Forks over Knives",
                "Earthlings",
                "Dominion",
                "Cowspiracy"
            ]
    }

    def 'The `equals` and `hashCode` implementations of an Association works reliably for a large number of entries.'(
        Class<Object> keyType, Class<Object> valueType, List<Pair<Object, Object>> entries
    ) {
        reportInfo """
            Here we test the implementation of the `equals` and `hashCode` methods exhaustively.
            This may look like an exaggerated amount of test data and equality checks, but you
            have to know that under the hood specifically the `equals` implementations are
            highly optimized to specific cases which need to be covered.
            
            More specifically, if there are only small differences between associations,
            we can avoid a lot of work due to two associations sharing most of their branches.
        """
        given : 'We create randomly sorted variants of the test data:'
            var keyHasher1 = { (it.hashCode() ^ (it.hashCode() ** 2)) as int }
            var keyHasher2 = { (it.hashCode() + (it.hashCode() ** 2)) as int }
            Comparator<Object> randomSort1 = (a, b) -> (keyHasher1(a)<=>keyHasher1(b))
            Comparator<Object> randomSort2 = (a, b) -> (keyHasher2(a)<=>keyHasher2(b))
            var scrambledEntries1 = entries.toSorted(randomSort1)
            var scrambledEntries2 = entries.toSorted(randomSort2)

        when : 'We create two different `Association` instances from the two scrambled list...'
            var association1 = Association.betweenLinked(keyType, valueType).putAll(scrambledEntries1)
            var association2 = Association.betweenLinked(keyType, valueType).putAll(scrambledEntries2)
        then : 'The two associations are equal!'
            association1.equals(association2)
        and : 'They also have the same hash codes:'
            association1.hashCode() == association2.hashCode()

        when : 'We create modified version of the two associations...'
            var randomEntry = entries[Math.abs(entries.hashCode()*1997)%entries.size()]
            var randomKey1 = randomEntry.first()
            var randomValue = entries.find({it.first() != randomKey1 && it.second() != association1.get(randomKey1).get()}).second()
            var modifiedAssociation1 = association1.put(randomKey1, randomValue)
            var modifiedAssociation2 = association2.put(randomKey1, randomValue)
        then : 'We have the expected relationships between all of the objects:'
            !modifiedAssociation1.equals(association1)
            !modifiedAssociation2.equals(association2)
            !modifiedAssociation1.equals(association2)
            !modifiedAssociation2.equals(association1)
            modifiedAssociation1.equals(modifiedAssociation2)

        when : 'We put back in the old value to restore the previous states...'
            var restoredAssociation1 = modifiedAssociation1.put(randomKey1, randomEntry.second())
            var restoredAssociation2 = modifiedAssociation2.put(randomKey1, randomEntry.second())
        then : 'We have the expected relationships between all of the objects:'
            !restoredAssociation1.equals(modifiedAssociation1)
            !restoredAssociation2.equals(modifiedAssociation2)
            !restoredAssociation1.equals(modifiedAssociation2)
            !restoredAssociation2.equals(modifiedAssociation1)
            restoredAssociation1.equals(restoredAssociation2)
            restoredAssociation1.equals(association1)
            restoredAssociation2.equals(association2)
            restoredAssociation1.equals(association2)
            restoredAssociation2.equals(association1)

        when : 'We create smaller version of the two associations by removing something...'
            var randomKey2 = entries[Math.abs(entries.hashCode()*31)%entries.size()].first()
            var smallerAssociation1 = association1.remove(randomKey2)
            var smallerAssociation2 = association2.remove(randomKey2)
        then : 'We have the expected relationships between all of the objects:'
            !smallerAssociation1.equals(association1)
            !smallerAssociation2.equals(association2)
            !smallerAssociation1.equals(association2)
            !smallerAssociation2.equals(association1)
            smallerAssociation1.equals(smallerAssociation2)
        and :
            smallerAssociation1.hashCode() != association1.hashCode()
            smallerAssociation2.hashCode() != association2.hashCode()
            smallerAssociation1.hashCode() != association2.hashCode()
            smallerAssociation2.hashCode() != association1.hashCode()
            smallerAssociation1.hashCode() == smallerAssociation2.hashCode()

        where : 'We use the following entry data source:'
            keyType  |  valueType  |   entries
            Byte     | Boolean     |  (0..100).collect(it -> Pair.of(it as Byte, (it as Byte)%2==0)).toList()
            Short    | Byte        |  (0..100).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..100).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..100).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()

            Byte     | Boolean     |  (0..1_000).collect(it -> Pair.of(it as Byte, (it as Byte)%2==0)).toList()
            Short    | Byte        |  (0..1_000).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..1_000).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..1_000).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()

            Byte     | Boolean     |  (0..10_000).collect(it -> Pair.of(it as Byte, (it as Byte)%2==0)).toList()
            Short    | Byte        |  (0..10_000).collect(it -> Pair.of(it as Short, (it*73) as Byte)).toList()
            Integer  | Integer     |  (0..10_000).collect(it -> Pair.of(it, -it)).toList()
            String   | Double      |  (0..10_000).collect(it -> Pair.of(String.valueOf(it), -it * 1234e-5 as double)).toList()
    }
}
