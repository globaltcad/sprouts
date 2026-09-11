package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Tuple to Association - Turning Sequences into Lookup Tables")
@Narrative('''

    A `Tuple` is an ordered sequence of items, whereas an `Association`
    is a mapping from keys to values. Turning the former into the latter
    is one of the most common things you do when modelling data:
    you have a list of value objects, and you want to look them up
    by one of their fields.

    The `Tuple` type offers three flavours of this conversion,
    one for every kind of `Association` that exists in Sprouts:

    - `toAssociation(Class, Function, Class, Function)` creates an
      unordered association, the fastest of the three.
    - `toLinkedAssociation(Class, Function, Class, Function)` creates
      an association which preserves the order of the tuple.
    - `toSortedAssociation(Class, Function, Class, Function, Comparator)`
      creates an association whose entries are ordered by their keys,
      with a natural order variant for keys which are `Comparable`.

    Note how every mapper function is preceded by the type of the
    objects it produces. This is because an `Association` always tracks
    the types of its keys and values, and a lambda, unlike a tuple,
    cannot tell us what it produces.

    All of these conversions are exact shorthands for filling an
    initially empty association with the derived key-value pairs,
    one after another. So if two items produce the same key,
    then the last one wins.

    When you do not want to lose the other ones, then reach for the
    grouping conversions instead, which exist in the same three
    flavours and collect all the values of a key into a `Tuple`:

    - `toGroupedAssociation(Class, Function, Class, Function)`
    - `toGroupedLinkedAssociation(Class, Function, Class, Function)`
    - `toGroupedSortedAssociation(Class, Function, Class, Function, Comparator)`

    These are lossless: every single item of the tuple ends up in
    exactly one of the groups of the resulting association.

''')
@Subject([Tuple, Association])
class Tuple_To_Association_Spec extends Specification
{
    /**
     *  A tiny immutable value object, used throughout this specification
     *  to demonstrate how a sequence of domain objects is turned into
     *  a lookup table keyed by one of their fields.
     */
    static class Documentary {
        final String title
        final int minutes
        final String topic
        Documentary( String title, int minutes, String topic ) {
            this.title = title
            this.minutes = minutes
            this.topic = topic
        }
        String title() { return this.title }
        int minutes() { return this.minutes }
        String topic() { return this.topic }
        @Override String toString() { return this.title + "(" + this.minutes + "min)" }
    }

    private static Tuple<Documentary> documentaries() {
        return Tuple.of(Documentary, [
                    new Documentary("Dominion", 125, "animals"),
                    new Documentary("Earthlings", 95, "animals"),
                    new Documentary("Cowspiracy", 91, "environment"),
                    new Documentary("Forks over Knives", 96, "health"),
                ])
    }

    def 'A tuple of value objects can be turned into a lookup table.'()
    {
        reportInfo """
            This is the bread and butter use case of the `toAssociation` method:
            you have a tuple of value objects and you want to look them up
            by one of their fields.
            
            The two mapper functions define what becomes the key and what
            becomes the value of every entry, and each of them is preceded
            by the type of the objects it produces.
        """
        given : 'A tuple of documentaries, each with a title and a runtime.'
            var docs = documentaries()

        when : 'We index the runtimes of the documentaries by their titles.'
            var runtimes = docs.toAssociation(
                                String.class,  { it.title() },
                                Integer.class, { it.minutes() }
                            )
        then : 'We can now look up the runtime of a documentary by its title.'
            runtimes.get("Dominion").get() == 125
            runtimes.get("Cowspiracy").get() == 91
        and : 'The association knows the types we declared for its keys and values.'
            runtimes.keyType() == String
            runtimes.valueType() == Integer
        and : 'It holds exactly one entry per item of the tuple.'
            runtimes.size() == 4
        and : 'Being a plain association, it is neither linked nor sorted.'
            !runtimes.isLinked()
            !runtimes.isSorted()
    }

    def 'The three kinds of conversion produce the three kinds of association.'()
    {
        reportInfo """
            Sprouts knows three kinds of associations: plain (unordered),
            linked (ordered by insertion) and sorted (ordered by key).
            There is one conversion method for each of them, and they all
            report their kind through `isLinked()` and `isSorted()`.
        """
        given : 'A tuple of words we want to index by their length.'
            var words = Tuple.of("seed", "sprout", "tree")

        when : 'We convert the tuple in all three ways.'
            var plain  = words.toAssociation(       Integer.class, { it.length() }, String.class, { it })
            var linked = words.toLinkedAssociation( Integer.class, { it.length() }, String.class, { it })
            var sorted = words.toSortedAssociation( Integer.class, { it.length() }, String.class, { it }, Comparator.naturalOrder())

        then : 'Each of them reports the kind of association it is.'
            !plain.isLinked()  && !plain.isSorted()
            linked.isLinked()  && !linked.isSorted()
            !sorted.isLinked() && sorted.isSorted()
        and : 'They all hold the same entries, they merely differ in their order.'
            plain.toMap()  == [4:"tree", 6:"sprout"]
            linked.toMap() == [4:"tree", 6:"sprout"]
            sorted.toMap() == [4:"tree", 6:"sprout"]
    }

    def 'A linked association preserves the order of the tuple it came from.'()
    {
        reportInfo """
            The `toLinkedAssociation` method is the one to reach for when
            the order of your tuple carries meaning. Iterating over the
            resulting association yields the entries in exactly the order
            in which the items they came from appear in the tuple.
        """
        given : 'A tuple of documentaries in no particular order.'
            var docs = documentaries()

        when : 'We index them by their titles, using a linked association.'
            var runtimes = docs.toLinkedAssociation(
                                String.class,  { it.title() },
                                Integer.class, { it.minutes() }
                            )
        then : 'The keys appear in the very order of the tuple.'
            runtimes.keySet().toList() == ["Dominion", "Earthlings", "Cowspiracy", "Forks over Knives"]
        and : 'And so do the values.'
            runtimes.values().toList() == [125, 95, 91, 96]

        when : 'We reverse the tuple and convert it again.'
            var reversed = docs.reversed().toLinkedAssociation(
                                String.class,  { it.title() },
                                Integer.class, { it.minutes() }
                            )
        then : 'The order of the entries is reversed as well.'
            reversed.keySet().toList() == ["Forks over Knives", "Cowspiracy", "Earthlings", "Dominion"]
        and : 'Both associations still hold the same entries, which is why they are equal.'
            reversed == runtimes
    }

    def 'A sorted association orders its entries by their keys, not by the tuple.'()
    {
        reportInfo """
            The `toSortedAssociation` method takes a `Comparator` as its
            last parameter, which defines the order of the keys in the
            resulting association. The order of the tuple is irrelevant
            for the resulting order, it only decides which value wins
            if two items produce the same key.
        """
        given : 'A tuple of documentaries in no particular order.'
            var docs = documentaries()

        when : 'We index them by their titles, sorted alphabetically.'
            var alphabetical = docs.toSortedAssociation(
                                    String.class,  { it.title() },
                                    Integer.class, { it.minutes() },
                                    Comparator.naturalOrder()
                                )
        then : 'The keys are sorted, irrespective of the order of the tuple.'
            alphabetical.keySet().toList() == ["Cowspiracy", "Dominion", "Earthlings", "Forks over Knives"]
        and : 'The association reports itself as sorted.'
            alphabetical.isSorted()
            !alphabetical.isLinked()

        when : 'We do the same, but with a reversed comparator.'
            var reversed = docs.toSortedAssociation(
                                String.class,  { it.title() },
                                Integer.class, { it.minutes() },
                                Comparator.reverseOrder()
                            )
        then : 'The keys are sorted the other way around.'
            reversed.keySet().toList() == ["Forks over Knives", "Earthlings", "Dominion", "Cowspiracy"]
    }

    def 'The natural order variant of `toSortedAssociation` needs no comparator.'()
    {
        reportInfo """
            Sorting by the natural order of the keys is so common that
            there is a variant of `toSortedAssociation` without a comparator.
            It requires the key type to implement `Comparable` and is
            exactly equivalent to passing `Comparator.naturalOrder()`.
        """
        given : 'A tuple of documentaries.'
            var docs = documentaries()

        when : 'We convert it without supplying a comparator...'
            var natural = docs.toSortedAssociation(
                                String.class,  { it.title() },
                                Integer.class, { it.minutes() }
                            )
        and : '...and once more, with the natural order comparator.'
            var explicit = docs.toSortedAssociation(
                                String.class,  { it.title() },
                                Integer.class, { it.minutes() },
                                Comparator.naturalOrder()
                            )
        then : 'Both produce the very same sorted association.'
            natural == explicit
            natural.hashCode() == explicit.hashCode()
            natural.keySet().toList() == ["Cowspiracy", "Dominion", "Earthlings", "Forks over Knives"]
    }

    def 'Converting an empty tuple yields an empty association of the declared types.'()
    {
        reportInfo """
            An empty tuple is a perfectly valid input for all of these
            conversions. Because the key and value types are declared
            explicitly, the resulting association knows its types even
            though there was not a single item to derive them from.
        """
        given : 'An empty tuple of documentaries.'
            var empty = Tuple.of(Documentary)

        when : 'We convert it in all four ways.'
            var plain   = empty.toAssociation(       String.class, { it.title() }, Integer.class, { it.minutes() })
            var linked  = empty.toLinkedAssociation( String.class, { it.title() }, Integer.class, { it.minutes() })
            var sorted  = empty.toSortedAssociation( String.class, { it.title() }, Integer.class, { it.minutes() }, Comparator.naturalOrder())
            var natural = empty.toSortedAssociation( String.class, { it.title() }, Integer.class, { it.minutes() })

        then : 'All of them are empty.'
            plain.isEmpty() && linked.isEmpty() && sorted.isEmpty() && natural.isEmpty()
        and : 'All of them know the types we declared.'
            [plain, linked, sorted, natural].every { it.keyType() == String && it.valueType() == Integer }
        and : 'And all of them are of the kind we asked for.'
            !plain.isLinked()  && !plain.isSorted()
            linked.isLinked()  && !linked.isSorted()
            !sorted.isLinked() && sorted.isSorted()
            !natural.isLinked() && natural.isSorted()
        and : 'They are equal to the empty associations of the corresponding factory methods.'
            plain   == Association.between(String, Integer)
            linked  == Association.betweenLinked(String, Integer)
            sorted  == Association.betweenSorted(String, Integer, Comparator.naturalOrder())
            natural == Association.betweenSorted(String, Integer)
    }

    def 'When two items produce the same key, then the last one wins.'()
    {
        reportInfo """
            An association cannot hold duplicate keys, so something has to
            give when two items of a tuple produce the same key.
            These conversion methods are exact shorthands for putting all
            derived pairs into an empty association one after another,
            which means the value of the **last** of the colliding items wins.
        """
        given : 'A tuple of words, where several of them start with the same letter.'
            var words = Tuple.of("apple", "avocado", "banana", "blueberry", "cherry")

        when : 'We index the words by their first character.'
            var byFirstChar = words.toAssociation(
                                    Character.class, { it.charAt(0) },
                                    String.class,    { it }
                                )
        then : 'Only one entry per distinct first character survives, the last one of each.'
            byFirstChar.size() == 3
            byFirstChar.get('a' as Character).get() == "avocado"
            byFirstChar.get('b' as Character).get() == "blueberry"
            byFirstChar.get('c' as Character).get() == "cherry"
    }

    def 'A colliding key keeps the position of the first item, but the value of the last.'()
    {
        reportInfo """
            In a linked association the position of an entry is decided when
            the key is first inserted, and putting a new value for an existing
            key does not move it. This is the behaviour of a `LinkedHashMap`,
            and since these conversions are shorthands for repeated `put` calls,
            it is the behaviour you get here as well.
        """
        given : 'A tuple whose first and second item share their first character.'
            var words = Tuple.of("apple", "avocado", "banana")

        when : 'We index the words by their first character, preserving the order.'
            var byFirstChar = words.toLinkedAssociation(
                                    Character.class, { it.charAt(0) },
                                    String.class,    { it }
                                )
        then : "The 'a' entry sits at the position of \"apple\", the first of the two..."
            byFirstChar.keySet().toList() == ['a' as Character, 'b' as Character]
        and : '...but it holds the value derived from "avocado", the last of the two.'
            byFirstChar.values().toList() == ["avocado", "banana"]
    }

    def 'A comparator which is inconsistent with equals is handed straight to the association.'()
    {
        reportInfo """
            Just like a `SortedMap`, a sorted `Association` wants a comparator
            which is consistent with `equals`, because it tells its keys apart
            through their equality, while it positions them through the comparator.

            This conversion does not invent a policy of its own for such a
            comparator. Whatever the sorted association makes of it is exactly
            what you get, because the conversion is nothing but a shorthand for
            putting the derived pairs into that very association.
        """
        given : 'A tuple of words which differ only in their casing, and a comparator ignoring casing.'
            var words = Tuple.of("Seed", "seed", "Tree")
            var caseInsensitive = String.CASE_INSENSITIVE_ORDER

        when : 'We convert the tuple using that comparator...'
            var converted = words.toSortedAssociation(
                                String.class,  { it },
                                Integer.class, { it.length() },
                                caseInsensitive
                            )
        and : '...and then assemble the very same association by hand.'
            var manual = Association.betweenSorted(String, Integer, caseInsensitive)
            words.each { manual = manual.put(it, it.length()) }

        then : 'The two are indistinguishable, down to their size and iteration order.'
            converted == manual
            converted.size() == manual.size()
            converted.keySet().toList() == manual.keySet().toList()

        when : 'We convert the very same tuple using the natural order, which is consistent with equals.'
            var caseSensitive = words.toSortedAssociation(
                                    String.class,  { it },
                                    Integer.class, { it.length() }
                                )
        then : 'Every distinct word became an entry of its own, sorted naturally.'
            caseSensitive.size() == 3
            caseSensitive.keySet().toList() == ["Seed", "Tree", "seed"]
    }

    def 'The conversions are exact shorthands for filling an empty association.'()
    {
        reportInfo """
            The contract of these conversion methods is defined in terms of
            the association factory methods you already know: create an empty
            association of the desired kind, and then put all derived
            key-value pairs into it, in the order of the tuple.
            This specification verifies that promise directly.
        """
        given : 'A tuple with a couple of duplicate keys hidden in it.'
            var words = Tuple.of("bee", "cow", "ant", "cat", "owl", "ape")
            var keyOf = { String word -> word.charAt(0) }
            var valueOf = { String word -> word.toUpperCase() }

        when : 'We build the three associations by hand, using plain `put` calls.'
            var manualPlain  = Association.between(Character, String)
            var manualLinked = Association.betweenLinked(Character, String)
            var manualSorted = Association.betweenSorted(Character, String, Comparator.naturalOrder())
            words.each {
                manualPlain  = manualPlain.put(keyOf(it), valueOf(it))
                manualLinked = manualLinked.put(keyOf(it), valueOf(it))
                manualSorted = manualSorted.put(keyOf(it), valueOf(it))
            }
        and : 'And then again, using the conversion methods.'
            var plain  = words.toAssociation(       Character.class, keyOf, String.class, valueOf)
            var linked = words.toLinkedAssociation( Character.class, keyOf, String.class, valueOf)
            var sorted = words.toSortedAssociation( Character.class, keyOf, String.class, valueOf, Comparator.naturalOrder())

        then : 'The results are indistinguishable, down to their iteration order.'
            plain  == manualPlain
            linked == manualLinked
            sorted == manualSorted
            linked.keySet().toList() == manualLinked.keySet().toList()
            sorted.keySet().toList() == manualSorted.keySet().toList()
    }

    def 'Both mappers are applied exactly once per item, in the order of the tuple.'()
    {
        reportInfo """
            The items of a tuple are processed from the first to the last,
            and each of the two mapper functions sees every item exactly once.
            This holds even for items whose keys end up colliding, because
            the value of a later item can only win if it was computed.
        """
        given : 'A tuple with a duplicate key hidden in it, and two recording mappers.'
            var words = Tuple.of("ant", "ape", "bee")
            var keysSeen = []
            var valuesSeen = []

        when : 'We convert the tuple into an association.'
            var byFirstChar = words.toAssociation(
                                    Character.class, { keysSeen << it; it.charAt(0) },
                                    String.class,    { valuesSeen << it; it }
                                )
        then : 'Every item was passed to both mappers exactly once, in the order of the tuple.'
            keysSeen == ["ant", "ape", "bee"]
            valuesSeen == ["ant", "ape", "bee"]
        and : 'The resulting association has the expected entries.'
            byFirstChar.toMap() == [('a' as Character) : "ape", ('b' as Character) : "bee"]
    }

    def 'A mapper producing null is an error, because associations cannot hold nulls.'(
        String description, Closure<Association> conversion, String expectedFragment
    ) {
        reportInfo """
            A `Tuple` may hold null items, but an `Association` may hold
            neither null keys nor null values. So when one of the two mappers
            produces a null, the conversion fails with a `NullPointerException`
            which tells you exactly which item is to blame.
        """
        when : 'We perform a conversion whose mapper produces a null.'
            conversion()
        then : 'A null pointer exception is thrown...'
            var exception = thrown(NullPointerException)
        and : '...naming the offending item, its index and what went wrong.'
            exception.message.contains("at index 1")
            exception.message.contains("'bb'")
            exception.message.contains(expectedFragment)

        where : 'We check this for the key mapper and the value mapper of every conversion.'
            description                   | conversion                                                                                                                          || expectedFragment
            'toAssociation, key'          | { Tuple.of("a","bb").toAssociation(String, { it == "bb" ? null : it }, Integer, { it.length() }) }                                   || "null keys"
            'toAssociation, value'        | { Tuple.of("a","bb").toAssociation(String, { it }, Integer, { it == "bb" ? null : it.length() }) }                                  || "null values"
            'toLinkedAssociation, key'    | { Tuple.of("a","bb").toLinkedAssociation(String, { it == "bb" ? null : it }, Integer, { it.length() }) }                             || "null keys"
            'toLinkedAssociation, value'  | { Tuple.of("a","bb").toLinkedAssociation(String, { it }, Integer, { it == "bb" ? null : it.length() }) }                            || "null values"
            'toSortedAssociation, key'    | { Tuple.of("a","bb").toSortedAssociation(String, { it == "bb" ? null : it }, Integer, { it.length() }, Comparator.naturalOrder()) }  || "null keys"
            'toSortedAssociation, value'  | { Tuple.of("a","bb").toSortedAssociation(String, { it }, Integer, { it == "bb" ? null : it.length() }, Comparator.naturalOrder()) } || "null values"
            'natural sorted, key'         | { Tuple.of("a","bb").toSortedAssociation(String, { it == "bb" ? null : it }, Integer, { it.length() }) }                             || "null keys"
            'natural sorted, value'       | { Tuple.of("a","bb").toSortedAssociation(String, { it }, Integer, { it == "bb" ? null : it.length() }) }                            || "null values"
    }

    def 'A tuple of nullable items can be converted, as long as the mappers cope with the nulls.'()
    {
        reportInfo """
            A tuple which allows null items hands those nulls to the mapper
            functions like any other item. It is up to the mappers to turn
            them into something an association can hold.
        """
        given : 'A nullable tuple with a hole in the middle of it.'
            var words = Tuple.ofNullable(String, "seed", null, "tree")

        when : 'We convert it with mappers which handle the null item.'
            var lengths = words.toLinkedAssociation(
                                String.class,  { it == null ? "<unknown>" : it },
                                Integer.class, { it == null ? 0 : it.length() }
                            )
        then : 'The null item became a regular entry of the association.'
            lengths.keySet().toList() == ["seed", "<unknown>", "tree"]
            lengths.values().toList() == [4, 0, 4]
    }

    def 'Passing null instead of a type, a mapper or a comparator throws an exception.'(
        String description, Closure conversion
    ) {
        reportInfo """
            None of the parameters of these conversion methods is optional,
            so passing null for any of them fails immediately.
        """
        when : 'We call a conversion method with a null argument.'
            conversion()
        then : 'A null pointer exception is thrown.'
            thrown(NullPointerException)

        where : 'We try this for every parameter of every conversion method.'
            description                        | conversion
            'toAssociation, key type'          | { Tuple.of("a").toAssociation(null, { it }, Integer, { it.length() }) }
            'toAssociation, key mapper'        | { Tuple.of("a").toAssociation(String, null, Integer, { it.length() }) }
            'toAssociation, value type'        | { Tuple.of("a").toAssociation(String, { it }, null, { it.length() }) }
            'toAssociation, value mapper'      | { Tuple.of("a").toAssociation(String, { it }, Integer, null) }
            'toLinkedAssociation, key type'    | { Tuple.of("a").toLinkedAssociation(null, { it }, Integer, { it.length() }) }
            'toLinkedAssociation, key mapper'  | { Tuple.of("a").toLinkedAssociation(String, null, Integer, { it.length() }) }
            'toLinkedAssociation, value type'  | { Tuple.of("a").toLinkedAssociation(String, { it }, null, { it.length() }) }
            'toLinkedAssociation, value mapper'| { Tuple.of("a").toLinkedAssociation(String, { it }, Integer, null) }
            'toSortedAssociation, key type'    | { Tuple.of("a").toSortedAssociation(null, { it }, Integer, { it.length() }, Comparator.naturalOrder()) }
            'toSortedAssociation, key mapper'  | { Tuple.of("a").toSortedAssociation(String, null, Integer, { it.length() }, Comparator.naturalOrder()) }
            'toSortedAssociation, value type'  | { Tuple.of("a").toSortedAssociation(String, { it }, null, { it.length() }, Comparator.naturalOrder()) }
            'toSortedAssociation, value mapper'| { Tuple.of("a").toSortedAssociation(String, { it }, Integer, null, Comparator.naturalOrder()) }
            'toSortedAssociation, comparator'  | { Tuple.of("a").toSortedAssociation(String, { it }, Integer, { it.length() }, null) }
            'natural sorted, key type'         | { Tuple.of("a").toSortedAssociation(null, { it }, Integer, { it.length() }) }
            'natural sorted, key mapper'       | { Tuple.of("a").toSortedAssociation(String, null, Integer, { it.length() }) }
            'natural sorted, value type'       | { Tuple.of("a").toSortedAssociation(String, { it }, null, { it.length() }) }
            'natural sorted, value mapper'     | { Tuple.of("a").toSortedAssociation(String, { it }, Integer, null) }
    }

    def 'The declared types are enforced by the resulting association.'()
    {
        reportInfo """
            Types are tracked in this library, which is why you have to
            declare the key and value types of the association you want.
            If a mapper produces something which does not fit the type
            it was paired with, then the association rejects it.
            (In statically typed Java code the compiler catches this for you,
            here we have to sneak past it using dynamic Groovy.)
        """
        when : 'We claim to produce integer keys, but produce strings.'
            Tuple.of("seed", "tree").toAssociation(
                Integer.class, { it },
                String.class,  { it }
            )
        then : 'The association rejects the key.'
            var keyException = thrown(IllegalArgumentException)
            keyException.message.contains("Integer")

        when : 'We claim to produce integer values, but produce strings.'
            Tuple.of("seed", "tree").toAssociation(
                String.class,  { it },
                Integer.class, { it }
            )
        then : 'The association rejects the value.'
            var valueException = thrown(IllegalArgumentException)
            valueException.message.contains("Integer")
    }

    def 'Converting a tuple never modifies the tuple itself.'()
    {
        reportInfo """
            A `Tuple` is an immutable value object, so turning it into an
            association is a pure read of its items. This is a fundamental
            contract we verify here by comparing the tuple with a snapshot
            taken before the conversions.
        """
        given : 'A tuple and a snapshot of its contents.'
            var docs = documentaries()
            var snapshot = docs.toList().collect { it }

        when : 'We convert the tuple in all four ways.'
            docs.toAssociation(       String.class, { it.title() }, Integer.class, { it.minutes() })
            docs.toLinkedAssociation( String.class, { it.title() }, Integer.class, { it.minutes() })
            docs.toSortedAssociation( String.class, { it.title() }, Integer.class, { it.minutes() }, Comparator.naturalOrder())
            docs.toSortedAssociation( String.class, { it.title() }, Integer.class, { it.minutes() })

        then : 'The tuple is completely unaffected.'
            docs.toList() == snapshot
            docs.size() == 4
            docs.type() == Documentary
    }

    def 'A large tuple, grown through many insertions, converts to a complete association.'()
    {
        reportInfo """
            A `Tuple` is backed by a tree of arrays whose shape depends on how
            the tuple was built. This specification grows a large tuple through
            many insertions at random positions, so that the resulting tree is
            deep and irregular, and then verifies that every single item of it
            makes it into the resulting associations.
        """
        given : 'A large tuple, grown through insertions at random positions.'
            var random = new Random(1997)
            var tuple = Tuple.of(Integer)
            var reference = new ArrayList<Integer>()
            1000.times { number ->
                var index = random.nextInt(tuple.size() + 1)
                tuple = tuple.addAt(index, number)
                reference.add(index, number)
            }

        expect : 'The tuple matches the reference list before we start.'
            tuple.toList() == reference

        when : 'We index every number by itself.'
            var plain  = tuple.toAssociation(       Integer.class, { it }, String.class, { "#" + it })
            var linked = tuple.toLinkedAssociation( Integer.class, { it }, String.class, { "#" + it })
            var sorted = tuple.toSortedAssociation( Integer.class, { it }, String.class, { "#" + it })

        then : 'All of them hold every single number of the tuple.'
            plain.size() == 1000
            linked.size() == 1000
            sorted.size() == 1000
        and : 'Every number can be looked up in every one of them.'
            (0..999).every { number ->
                plain.get(number).get() == "#" + number &&
                linked.get(number).get() == "#" + number &&
                sorted.get(number).get() == "#" + number
            }
        and : 'The linked association preserved the order of the tuple...'
            linked.keySet().toList() == reference
        and : '...while the sorted association ordered the numbers naturally.'
            sorted.keySet().toList() == (0..999).toList()
    }

    def 'The conversions work for all kinds of tuples, including primitive backed ones.'(
        Tuple<Object> tuple, List<Object> expectedKeys
    ) {
        reportInfo """
            Tuples of primitives are stored in dense arrays of primitives
            instead of arrays of boxed objects. The conversion reads the items
            through the same abstraction as every other tuple operation,
            so these tuples convert just like any other.
        """
        when : 'We index the items of the tuple by themselves.'
            var association = tuple.toLinkedAssociation(
                                    tuple.type(), { it },
                                    String.class, { String.valueOf(it) }
                                )
        then : 'Every item of the tuple became a key of the association.'
            association.keySet().toList() == expectedKeys
        and : 'And every value is the string representation of its key.'
            association.values().toList() == expectedKeys.collect { String.valueOf(it) }

        where : 'We use tuples backed by the various kinds of arrays.'
            tuple                                        || expectedKeys
            Tuple.of(1, 2, 3)                            || [1, 2, 3]
            Tuple.of([1, 2, 3] as int[])                 || [1, 2, 3]
            Tuple.of([1L, 2L, 3L] as long[])             || [1L, 2L, 3L]
            Tuple.of([1d, 2d, 3d] as double[])           || [1d, 2d, 3d]
            Tuple.of([1f, 2f, 3f] as float[])            || [1f, 2f, 3f]
            Tuple.of([1, 2, 3] as byte[])                || [1 as byte, 2 as byte, 3 as byte]
            Tuple.of("seed", "sprout")                   || ["seed", "sprout"]
            Tuple.of(java.time.Month.MAY, java.time.Month.JUNE) || [java.time.Month.MAY, java.time.Month.JUNE]
    }

    def 'An exception thrown by a mapper is relayed to the caller.'()
    {
        reportInfo """
            The mapper functions are your code, so if one of them fails,
            then the conversion fails with it. Nothing is swallowed,
            and no half-built association is handed back to you.
        """
        given : 'A tuple of words and a mapper which trips over the second one.'
            var words = Tuple.of("seed", "boom", "tree")

        when : 'We convert the tuple with that failing mapper.'
            words.toAssociation(
                String.class,  { it == "boom" ? { throw new IllegalStateException("Boom!") }() : it },
                Integer.class, { it.length() }
            )
        then : 'The exception of the mapper reaches us unchanged.'
            var exception = thrown(IllegalStateException)
            exception.message == "Boom!"
    }

    def 'A tuple of value objects can be grouped into an association of tuples.'()
    {
        reportInfo """
            Where `toAssociation` builds a lookup table with a single value per key,
            `toGroupedAssociation` builds one with a whole `Tuple` of values per key.
            Every item of the tuple ends up in exactly one of those groups.
            
            The two mapper functions play the same roles as before, except that the
            second one no longer produces the value of an entry, but a single member
            of a group.
        """
        given : 'A tuple of documentaries, some of which share their topic.'
            var docs = documentaries()

        when : 'We group the titles of the documentaries by their topic.'
            var titlesByTopic = docs.toGroupedAssociation(
                                    String.class, { it.topic() },
                                    String.class, { it.title() }
                                )
        then : 'Every topic is associated with a tuple of all the titles which share it.'
            titlesByTopic.get("animals").get()     == Tuple.of("Dominion", "Earthlings")
            titlesByTopic.get("environment").get() == Tuple.of("Cowspiracy")
            titlesByTopic.get("health").get()      == Tuple.of("Forks over Knives")
        and : 'There is one entry per distinct topic, not one per documentary.'
            titlesByTopic.size() == 3
        and : 'The association knows the type of its keys, and that its values are tuples.'
            titlesByTopic.keyType() == String
            titlesByTopic.valueType() == Tuple
        and : 'Being a plain association, it is neither linked nor sorted.'
            !titlesByTopic.isLinked()
            !titlesByTopic.isSorted()
    }

    def 'Grouping is lossless, whereas the plain conversion keeps only the last value.'()
    {
        reportInfo """
            This is the whole point of the grouping conversions. An `Association` cannot
            hold two values under one key, so `toAssociation` has to drop all but the last
            of them. `toGroupedAssociation` instead widens the values into tuples, which
            means that nothing of the original tuple is lost.
        """
        given : 'A tuple of words, several of which start with the same letter.'
            var words = Tuple.of("apple", "avocado", "banana", "blueberry", "cherry")

        when : 'We convert it in both ways, using the very same two mappers.'
            var collapsed = words.toAssociation(
                                Character.class, { it.charAt(0) },
                                String.class,    { it }
                            )
            var grouped = words.toGroupedAssociation(
                                Character.class, { it.charAt(0) },
                                String.class,    { it }
                            )
        then : 'Both associations have one entry per distinct first character.'
            collapsed.size() == 3
            grouped.size() == 3
        and : 'The collapsed one silently forgot "apple" and "banana".'
            collapsed.get('a' as Character).get() == "avocado"
            collapsed.get('b' as Character).get() == "blueberry"
        and : 'The grouped one kept every single word.'
            grouped.get('a' as Character).get() == Tuple.of("apple", "avocado")
            grouped.get('b' as Character).get() == Tuple.of("banana", "blueberry")
            grouped.get('c' as Character).get() == Tuple.of("cherry")
        and : 'Which we can also confirm by counting all the grouped words.'
            grouped.values().toList().sum({ it.size() }) == words.size()
    }

    def 'The three kinds of grouped conversion produce the three kinds of association.'()
    {
        reportInfo """
            Just like the plain conversions, the grouping conversions come in three
            flavours, one for every kind of `Association` in Sprouts.
        """
        given : 'A tuple of words we want to group by their length.'
            var words = Tuple.of("seed", "tree", "sprout", "branch", "leaf")

        when : 'We group the tuple in all three ways.'
            var plain  = words.toGroupedAssociation(       Integer.class, { it.length() }, String.class, { it })
            var linked = words.toGroupedLinkedAssociation( Integer.class, { it.length() }, String.class, { it })
            var sorted = words.toGroupedSortedAssociation( Integer.class, { it.length() }, String.class, { it }, Comparator.naturalOrder())

        then : 'Each of them reports the kind of association it is.'
            !plain.isLinked()  && !plain.isSorted()
            linked.isLinked()  && !linked.isSorted()
            !sorted.isLinked() && sorted.isSorted()
        and : 'They all hold the same groups, they merely differ in their order.'
            [plain, linked, sorted].every {
                it.get(4).get() == Tuple.of("seed", "tree", "leaf") &&
                it.get(6).get() == Tuple.of("sprout", "branch")
            }
    }

    def 'A grouped linked association keeps the order in which the keys first occur.'()
    {
        reportInfo """
            A group takes its position from the first item which produced its key,
            because that is when the key entered the association. The items which
            join the group later do not move it.
        """
        given : 'A tuple of documentaries whose topics are interleaved.'
            var docs = Tuple.of(Documentary, [
                            new Documentary("Cowspiracy", 91, "environment"),
                            new Documentary("Dominion", 125, "animals"),
                            new Documentary("Seaspiracy", 89, "environment"),
                            new Documentary("Earthlings", 95, "animals"),
                        ])

        when : 'We group the titles by their topic, preserving the order.'
            var titlesByTopic = docs.toGroupedLinkedAssociation(
                                    String.class, { it.topic() },
                                    String.class, { it.title() }
                                )
        then : 'The groups appear in the order in which their topics first occur.'
            titlesByTopic.keySet().toList() == ["environment", "animals"]
        and : 'And within every group, the titles appear in the order of the tuple.'
            titlesByTopic.get("environment").get() == Tuple.of("Cowspiracy", "Seaspiracy")
            titlesByTopic.get("animals").get()     == Tuple.of("Dominion", "Earthlings")
    }

    def 'A grouped sorted association orders its groups by their keys.'()
    {
        reportInfo """
            The `toGroupedSortedAssociation` method takes a `Comparator` as its last
            parameter, which orders the groups by their keys. The order of the tuple
            still decides the order of the values inside every group.
        """
        given : 'A tuple of documentaries whose topics are interleaved.'
            var docs = Tuple.of(Documentary, [
                            new Documentary("Cowspiracy", 91, "environment"),
                            new Documentary("Dominion", 125, "animals"),
                            new Documentary("Seaspiracy", 89, "environment"),
                            new Documentary("Earthlings", 95, "animals"),
                        ])

        when : 'We group the titles by topic, sorted alphabetically.'
            var alphabetical = docs.toGroupedSortedAssociation(
                                    String.class, { it.topic() },
                                    String.class, { it.title() },
                                    Comparator.naturalOrder()
                                )
        then : 'The groups are ordered by their topics, irrespective of the tuple.'
            alphabetical.keySet().toList() == ["animals", "environment"]
        and : 'But the titles inside the groups still follow the order of the tuple.'
            alphabetical.get("environment").get() == Tuple.of("Cowspiracy", "Seaspiracy")

        when : 'We do the same, but with a reversed comparator.'
            var reversed = docs.toGroupedSortedAssociation(
                                String.class, { it.topic() },
                                String.class, { it.title() },
                                Comparator.reverseOrder()
                            )
        then : 'The groups are ordered the other way around.'
            reversed.keySet().toList() == ["environment", "animals"]
        and : 'While the contents of the groups are untouched.'
            reversed.get("environment").get() == Tuple.of("Cowspiracy", "Seaspiracy")
    }

    def 'The natural order variant of `toGroupedSortedAssociation` needs no comparator.'()
    {
        reportInfo """
            Just like for the plain conversions, sorting the groups by the natural
            order of their keys does not require you to pass a comparator, as long
            as the key type implements `Comparable`.
        """
        given : 'A tuple of documentaries.'
            var docs = documentaries()

        when : 'We group them without supplying a comparator...'
            var natural = docs.toGroupedSortedAssociation(
                                String.class, { it.topic() },
                                String.class, { it.title() }
                            )
        and : '...and once more, with the natural order comparator.'
            var explicit = docs.toGroupedSortedAssociation(
                                String.class, { it.topic() },
                                String.class, { it.title() },
                                Comparator.naturalOrder()
                            )
        then : 'Both produce the very same sorted association of groups.'
            natural == explicit
            natural.hashCode() == explicit.hashCode()
            natural.keySet().toList() == ["animals", "environment", "health"]
    }

    def 'The values of a grouped association are tuples, which is what its value type says.'()
    {
        reportInfo """
            This is the one thing to keep in mind about the grouping conversions:
            the type you pair with the value mapper is the type of the items
            **inside** the value tuples, not the value type of the association.
            The association itself holds tuples, and says so.
        """
        given : 'A tuple of words.'
            var words = Tuple.of("seed", "tree", "leaf")

        when : 'We group the words by their length, declaring `String` as the value item type.'
            var grouped = words.toGroupedAssociation(
                                Integer.class, { it.length() },
                                String.class,  { it }
                            )
        then : 'The value type of the association is `Tuple`, not `String`.'
            grouped.valueType() == Tuple
            grouped.valueType() != String
        and : 'And the tuples it holds are tuples of strings.'
            grouped.get(4).get().type() == String
            grouped.get(4).get() == Tuple.of("seed", "tree", "leaf")
        and : 'Those tuples do not allow null items.'
            !grouped.get(4).get().allowsNull()
    }

    def 'A key produced by a single item is associated with a group of size one.'()
    {
        reportInfo """
            There is no special case for keys which only a single item produced.
            They are associated with a tuple like every other key, one holding
            exactly one value. This keeps the shape of the result predictable.
        """
        given : 'A tuple of words which all have a different first character.'
            var words = Tuple.of("seed", "tree", "leaf")

        when : 'We group them by their first character.'
            var grouped = words.toGroupedAssociation(
                                Character.class, { it.charAt(0) },
                                String.class,    { it }
                            )
        then : 'Every group is a tuple of exactly one word.'
            grouped.size() == 3
            grouped.values().toList().every { it.size() == 1 }
            grouped.get('s' as Character).get() == Tuple.of("seed")
    }

    def 'Equal values within a group are kept rather than merged.'()
    {
        reportInfo """
            A group is a `Tuple`, not a `ValueSet`, which means that it holds
            duplicates. So if two items of the tuple produce both the same key
            and the same value, then that value appears twice in the group.
        """
        given : 'A tuple in which one word occurs twice.'
            var words = Tuple.of("seed", "seed", "sprout")

        when : 'We group the words by their first character.'
            var grouped = words.toGroupedAssociation(
                                Character.class, { it.charAt(0) },
                                String.class,    { it }
                            )
        then : 'Both occurrences of the repeated word are in the group.'
            grouped.get('s' as Character).get() == Tuple.of("seed", "seed", "sprout")
            grouped.get('s' as Character).get().size() == 3
    }

    def 'Grouping an empty tuple yields an empty association of the declared types.'()
    {
        reportInfo """
            An empty tuple has no items to group, so the result is an empty
            association. It still knows its key type, and that its values
            would be tuples.
        """
        given : 'An empty tuple of documentaries.'
            var empty = Tuple.of(Documentary)

        when : 'We group it in all four ways.'
            var plain   = empty.toGroupedAssociation(       String.class, { it.topic() }, String.class, { it.title() })
            var linked  = empty.toGroupedLinkedAssociation( String.class, { it.topic() }, String.class, { it.title() })
            var sorted  = empty.toGroupedSortedAssociation( String.class, { it.topic() }, String.class, { it.title() }, Comparator.naturalOrder())
            var natural = empty.toGroupedSortedAssociation( String.class, { it.topic() }, String.class, { it.title() })

        then : 'All of them are empty.'
            plain.isEmpty() && linked.isEmpty() && sorted.isEmpty() && natural.isEmpty()
        and : 'All of them know their types.'
            [plain, linked, sorted, natural].every { it.keyType() == String && it.valueType() == Tuple }
        and : 'And all of them are of the kind we asked for.'
            !plain.isLinked()   && !plain.isSorted()
            linked.isLinked()   && !linked.isSorted()
            !sorted.isLinked()  && sorted.isSorted()
            !natural.isLinked() && natural.isSorted()
    }

    def 'Both mappers are applied exactly once per item when grouping.'()
    {
        reportInfo """
            Grouping visits every item of the tuple exactly once, and hands each
            of them to both mappers, in the order of the tuple. Items which join
            an existing group are no exception.
        """
        given : 'A tuple whose items mostly share a key, and two recording mappers.'
            var words = Tuple.of("ant", "ape", "bee")
            var keysSeen = []
            var valuesSeen = []

        when : 'We group the tuple by the first character of its items.'
            var grouped = words.toGroupedAssociation(
                                Character.class, { keysSeen << it; it.charAt(0) },
                                String.class,    { valuesSeen << it; it }
                            )
        then : 'Every item was passed to both mappers exactly once, in the order of the tuple.'
            keysSeen == ["ant", "ape", "bee"]
            valuesSeen == ["ant", "ape", "bee"]
        and : 'And the groups came out as expected.'
            grouped.get('a' as Character).get() == Tuple.of("ant", "ape")
            grouped.get('b' as Character).get() == Tuple.of("bee")
    }

    def 'A grouping conversion fails when one of its mappers produces null.'(
        String description, Closure<Association> conversion, String expectedFragment
    ) {
        reportInfo """
            Neither the keys of an association nor the items of a tuple of values
            may be null, so a mapper producing one fails the conversion, naming
            the item which is to blame.
        """
        when : 'We perform a grouping whose mapper produces a null.'
            conversion()
        then : 'A null pointer exception is thrown...'
            var exception = thrown(NullPointerException)
        and : '...naming the offending item, its index and what went wrong.'
            exception.message.contains("at index 1")
            exception.message.contains("'bb'")
            exception.message.contains(expectedFragment)

        where : 'We check this for the key mapper and the value mapper of every grouping.'
            description                          | conversion                                                                                                                                 || expectedFragment
            'grouped, key'                       | { Tuple.of("a","bb").toGroupedAssociation(String, { it == "bb" ? null : it }, Integer, { it.length() }) }                                   || "null keys"
            'grouped, value'                     | { Tuple.of("a","bb").toGroupedAssociation(String, { it }, Integer, { it == "bb" ? null : it.length() }) }                                  || "null items"
            'grouped linked, key'                | { Tuple.of("a","bb").toGroupedLinkedAssociation(String, { it == "bb" ? null : it }, Integer, { it.length() }) }                             || "null keys"
            'grouped linked, value'              | { Tuple.of("a","bb").toGroupedLinkedAssociation(String, { it }, Integer, { it == "bb" ? null : it.length() }) }                            || "null items"
            'grouped sorted, key'                | { Tuple.of("a","bb").toGroupedSortedAssociation(String, { it == "bb" ? null : it }, Integer, { it.length() }, Comparator.naturalOrder()) }  || "null keys"
            'grouped sorted, value'              | { Tuple.of("a","bb").toGroupedSortedAssociation(String, { it }, Integer, { it == "bb" ? null : it.length() }, Comparator.naturalOrder()) } || "null items"
            'grouped natural sorted, key'        | { Tuple.of("a","bb").toGroupedSortedAssociation(String, { it == "bb" ? null : it }, Integer, { it.length() }) }                             || "null keys"
            'grouped natural sorted, value'      | { Tuple.of("a","bb").toGroupedSortedAssociation(String, { it }, Integer, { it == "bb" ? null : it.length() }) }                            || "null items"
    }

    def 'A grouping conversion throws when passed null instead of a type, a mapper or a comparator.'(
        String description, Closure conversion
    ) {
        reportInfo """
            None of the parameters of the grouping conversions is optional,
            so passing null for any of them fails immediately.
        """
        when : 'We call a grouping method with a null argument.'
            conversion()
        then : 'A null pointer exception is thrown.'
            thrown(NullPointerException)

        where : 'We try this for every parameter of every grouping method.'
            description                            | conversion
            'grouped, key type'                    | { Tuple.of("a").toGroupedAssociation(null, { it }, Integer, { it.length() }) }
            'grouped, key mapper'                  | { Tuple.of("a").toGroupedAssociation(String, null, Integer, { it.length() }) }
            'grouped, value item type'             | { Tuple.of("a").toGroupedAssociation(String, { it }, null, { it.length() }) }
            'grouped, value mapper'                | { Tuple.of("a").toGroupedAssociation(String, { it }, Integer, null) }
            'grouped linked, key type'             | { Tuple.of("a").toGroupedLinkedAssociation(null, { it }, Integer, { it.length() }) }
            'grouped linked, key mapper'           | { Tuple.of("a").toGroupedLinkedAssociation(String, null, Integer, { it.length() }) }
            'grouped linked, value item type'      | { Tuple.of("a").toGroupedLinkedAssociation(String, { it }, null, { it.length() }) }
            'grouped linked, value mapper'         | { Tuple.of("a").toGroupedLinkedAssociation(String, { it }, Integer, null) }
            'grouped sorted, key type'             | { Tuple.of("a").toGroupedSortedAssociation(null, { it }, Integer, { it.length() }, Comparator.naturalOrder()) }
            'grouped sorted, key mapper'           | { Tuple.of("a").toGroupedSortedAssociation(String, null, Integer, { it.length() }, Comparator.naturalOrder()) }
            'grouped sorted, value item type'      | { Tuple.of("a").toGroupedSortedAssociation(String, { it }, null, { it.length() }, Comparator.naturalOrder()) }
            'grouped sorted, value mapper'         | { Tuple.of("a").toGroupedSortedAssociation(String, { it }, Integer, null, Comparator.naturalOrder()) }
            'grouped sorted, comparator'           | { Tuple.of("a").toGroupedSortedAssociation(String, { it }, Integer, { it.length() }, null) }
            'grouped natural sorted, key type'     | { Tuple.of("a").toGroupedSortedAssociation(null, { it }, Integer, { it.length() }) }
            'grouped natural sorted, key mapper'   | { Tuple.of("a").toGroupedSortedAssociation(String, null, Integer, { it.length() }) }
            'grouped natural sorted, value type'   | { Tuple.of("a").toGroupedSortedAssociation(String, { it }, null, { it.length() }) }
            'grouped natural sorted, value mapper' | { Tuple.of("a").toGroupedSortedAssociation(String, { it }, Integer, null) }
    }

    def 'A tuple of nullable items can be grouped, as long as the mappers cope with the nulls.'()
    {
        reportInfo """
            A tuple which allows null items hands those nulls to the mappers when
            grouping, just like it does for the plain conversions. The groups
            themselves never allow nulls.
        """
        given : 'A nullable tuple with a hole in the middle of it.'
            var words = Tuple.ofNullable(String, "seed", null, "sprout")

        when : 'We group it with mappers which handle the null item.'
            var grouped = words.toGroupedLinkedAssociation(
                                Character.class, { it == null ? ('?' as Character) : it.charAt(0) },
                                String.class,    { it == null ? "<unknown>" : it }
                            )
        then : 'The null item became a group of its own.'
            grouped.keySet().toList() == ['s' as Character, '?' as Character]
            grouped.get('s' as Character).get() == Tuple.of("seed", "sprout")
            grouped.get('?' as Character).get() == Tuple.of("<unknown>")
        and : 'And none of the groups allows null items.'
            grouped.values().toList().every { !it.allowsNull() }
    }

    def 'Grouping a tuple never modifies the tuple itself.'()
    {
        reportInfo """
            Just like every other operation on a `Tuple`, grouping is a pure
            read of its items.
        """
        given : 'A tuple and a snapshot of its contents.'
            var docs = documentaries()
            var snapshot = docs.toList().collect { it }

        when : 'We group the tuple in all four ways.'
            docs.toGroupedAssociation(       String.class, { it.topic() }, String.class, { it.title() })
            docs.toGroupedLinkedAssociation( String.class, { it.topic() }, String.class, { it.title() })
            docs.toGroupedSortedAssociation( String.class, { it.topic() }, String.class, { it.title() }, Comparator.naturalOrder())
            docs.toGroupedSortedAssociation( String.class, { it.topic() }, String.class, { it.title() })

        then : 'The tuple is completely unaffected.'
            docs.toList() == snapshot
            docs.size() == 4
            docs.type() == Documentary
    }

    def 'A large tuple is grouped into buckets which hold every single item.'()
    {
        reportInfo """
            This specification grows a large tuple through many insertions at random
            positions, so that the tree behind it is deep and irregular, and then
            groups it into buckets. Not a single item may go missing on the way.
        """
        given : 'A large tuple, grown through insertions at random positions.'
            var random = new Random(1997)
            var tuple = Tuple.of(Integer)
            var reference = new ArrayList<Integer>()
            1000.times { number ->
                var index = random.nextInt(tuple.size() + 1)
                tuple = tuple.addAt(index, number)
                reference.add(index, number)
            }

        when : 'We group the numbers into ten buckets, by their last digit.'
            var buckets = tuple.toGroupedSortedAssociation(
                                Integer.class, { it % 10 },
                                Integer.class, { it }
                            )
        then : 'There are ten buckets, sorted by their digit.'
            buckets.size() == 10
            buckets.keySet().toList() == (0..9).toList()
        and : 'Every bucket holds a hundred numbers, all of them ending in its digit.'
            buckets.values().toList().every { it.size() == 100 }
            buckets.entrySet().stream().allMatch { pair -> pair.second().all { it % 10 == pair.first() } }
        and : 'Taken together, the buckets hold every single number of the tuple.'
            buckets.values().toList().sum({ it.size() }) == 1000
            buckets.values().toList().collectMany { it.toList() }.toSorted() == (0..999).toList()
        and : 'And within every bucket, the numbers follow the order of the tuple.'
            buckets.get(7).get().toList() == reference.findAll { it % 10 == 7 }
    }

    def 'A grouping conversion rejects a key or a group member of the wrong type.'()
    {
        reportInfo """
            A grouping conversion tracks a key type and the item type of the groups, and
            it rejects a mapper which does not honour them. The key is checked by the
            association, whereas the members of a group are checked against the item
            type of the tuples the groups are made of.

            Because the conversion knows which item it is busy with when a group member
            turns out to be of the wrong type, it can tell you exactly where to look.
            (In statically typed Java code the compiler catches all of this for you,
            here we have to sneak past it using dynamic Groovy.)
        """
        when : 'We claim to produce integer keys, but produce strings.'
            Tuple.of("seed", "tree").toGroupedAssociation(
                Integer.class, { it },
                String.class,  { it }
            )
        then : 'The association rejects the key.'
            var keyException = thrown(IllegalArgumentException)
            keyException.message.contains("Integer")

        when : 'We claim to produce integer group members, but produce a string for the second item.'
            Tuple.of("seed", "tree").toGroupedAssociation(
                String.class,  { it },
                Integer.class, { it == "tree" ? it : it.length() }
            )
        then : 'The group rejects the value...'
            var valueException = thrown(IllegalArgumentException)
            valueException.message.contains("Integer")
        and : '...naming the offending item, its index and the type it actually produced.'
            valueException.message.contains("'tree'")
            valueException.message.contains("at index 1")
            valueException.message.contains("String")

        when : 'We do the very same thing, but declare the group members as a primitive type.'
            Tuple.of("seed", "tree").toGroupedAssociation(
                String.class, { it },
                Integer.TYPE, { it == "tree" ? it : it.length() }
            )
        then : 'The group rejects the value just as decisively, naming the primitive type.'
            var primitiveException = thrown(IllegalArgumentException)
            primitiveException.message.contains("expected type 'int'")
        and : 'And it points at the offending item in exactly the same way.'
            primitiveException.message.contains("'tree'")
            primitiveException.message.contains("at index 1")
            primitiveException.message.contains("String")
    }

    def 'A group declared on a supertype accepts the subtypes of that type.'()
    {
        reportInfo """
            The item type of a group is a bound, not an exact match: a group of `Number`
            holds an `Integer` like any other number, exactly like a `Tuple<Number>` does.

            This is worth pinning, because the check which rejects a group member of the
            wrong type has to tell a subtype apart from an unrelated type. A check which
            merely compared the two types for equality would pass every specification
            above and still turn a perfectly good `Integer` away here.
        """
        given : 'A tuple of words we want to group by themselves.'
            var words = Tuple.of("seed", "tree")

        when : 'We collect their lengths into groups declared to hold any kind of number.'
            var grouped = words.toGroupedAssociation(
                                String.class, { it },
                                Number.class, { it.length() }
                            )
        then : 'The integers were accepted into the groups of numbers.'
            grouped.get("seed").get() == Tuple.of(Number, 4)
            grouped.get("tree").get() == Tuple.of(Number, 4)
        and : 'And the groups report the supertype we declared as their item type.'
            grouped.values().toList().every { it.type() == Number }

        when : 'We hand the very same groups a value which is no number at all.'
            words.toGroupedAssociation(
                String.class, { it },
                Number.class, { it }
            )
        then : 'That one is turned away, naming the type it expected.'
            var exception = thrown(IllegalArgumentException)
            exception.message.contains("expected type 'Number'")
    }

    def 'A group can be a tuple of primitive values.'()
    {
        reportInfo """
            The type paired with the value mapper is the item type of the value tuples,
            and a `Tuple` may be declared on a primitive item type, storing its items
            in a dense array. The conversion hands that type to the groups exactly as
            you declare it.

            Do keep in mind that a tuple tracks its item type, which means that a group
            of primitives is *not* equal to a group of the very same values boxed.
            Pick one of the two and stick with it.

            (Below we write `Integer.TYPE`, which is the very same class object as the
            `int.class` you would write in Java.)
        """
        given : 'A tuple of words we want to group by their first character.'
            var words = Tuple.of("seed", "sprout", "tree")

        when : 'We group their lengths, declaring the item type of the groups as primitive.'
            var lengths = words.toGroupedLinkedAssociation(
                                Character.class, { it.charAt(0) },
                                Integer.TYPE,    { it.length() }
                            )
        then : 'The groups came out as expected.'
            lengths.keySet().toList() == ['s' as Character, 't' as Character]
            lengths.get('s' as Character).get().toList() == [4, 6]
            lengths.get('t' as Character).get().toList() == [4]
        and : 'And they really are backed by primitives...'
            lengths.get('s' as Character).get().type() == Integer.TYPE
            lengths.get('s' as Character).get() == Tuple.of(Integer.TYPE, 4, 6)
        and : '...which is why they differ from the groups of boxed values.'
            lengths.get('s' as Character).get() != Tuple.of(4, 6)
    }

    def 'An exception thrown by a mapper of a grouping conversion reaches the caller unchanged.'()
    {
        reportInfo """
            The mapper functions of a grouping conversion are your code, so if one of
            them fails, then the grouping fails with it. Nothing is swallowed, and no
            half-built association is handed back to you.
        """
        given : 'A tuple of words and a mapper which trips over the second one.'
            var words = Tuple.of("seed", "boom", "tree")

        when : 'We group the tuple with that failing mapper.'
            words.toGroupedAssociation(
                Character.class, { it == "boom" ? { throw new IllegalStateException("Boom!") }() : it.charAt(0) },
                String.class,    { it }
            )
        then : 'The exception of the mapper reaches us unchanged.'
            var exception = thrown(IllegalStateException)
            exception.message == "Boom!"
    }
}
