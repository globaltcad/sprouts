package sprouts

import spock.lang.*

import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Title("Property Projection Lenses")
@Narrative('''

    The Sprouts Property Projection Lens is a specialized form of the Lens design pattern
    that establishes a bi-directional relationship between two representations of the same data.
    While regular lenses focus on accessing and updating fields within a nested structure,
    projection lenses focus on converting between different representations of the same logical data.
    
    A projection lens consists of a pair of functions:
    - A "getter" function that converts from the source type T to target type B
    - A "setter" function that converts from target type B back to source type T
    
    This creates a two-way street where changes in either property automatically propagate
    to the other, maintaining synchronization between different representations.
    
    This is particularly useful for:
    1. Unit conversions (meters ↔ feet, Celsius ↔ Fahrenheit)
    2. Format conversions (raw data ↔ formatted strings)
    3. Encryption/decryption (plain text ↔ encrypted text)
    4. Validation boundaries (validated objects ↔ raw input)
    5. View model adaptations (domain objects ↔ UI-friendly representations)
    
    The projection methods come in three flavors:
    1. `projectTo(getter, setter)` - Basic bi-directional projection
    2. `projectTo(nullObject, getter, setter)` - Projection with guaranteed non-null fallback
    3. `projectToNullable(type, getter, setter)` - Projection allowing null values
    
    This specification explores all three methods through practical, real-world examples
    and thoroughly tests edge cases, memory safety, and mathematical properties.
    
''')
@Subject([Var, Val, Viewable])
class Property_Projection_Lenses_Spec extends Specification {

    // ==================== Example Domain Models ====================

    /** Simple record for temperature conversion examples */
    static record Celsius(double value) {
        Fahrenheit toFahrenheit() { new Fahrenheit(value * 9/5 + 32) }
    }

    static record Fahrenheit(double value) {
        Celsius toCelsius() { new Celsius((value - 32) * 5/9) }
    }

    /** Record for encryption/decryption examples */
    static record SecretMessage(String plainText, String encrypted) {
        SecretMessage withPlainText(String plainText) { new SecretMessage(plainText, encrypt(plainText)) }
        SecretMessage withEncrypted(String encrypted) { new SecretMessage(decrypt(encrypted), encrypted) }

        private static String encrypt(String text) {
            if (text == null) return null
            // Simple Caesar cipher for demonstration
            return text.chars()
                      .map { c -> c + 3 }
                      .collect { (char) it }
                      .join("")
        }

        private static String decrypt(String text) {
            if (text == null) return null
            return text.chars()
                      .map { c -> c - 3 }
                      .collect { (char) it }
                      .join("")
        }
    }

    /** Record for formatting examples */
    static record Product(String name, double priceInCents) {
        Product withName(String name) { new Product(name, priceInCents) }
        Product withPriceInCents(double priceInCents) { new Product(name, priceInCents) }

        String formattedPrice() { String.format("\$%.2f", priceInCents / 100) }

        static double parseFormattedPrice(String formatted) {
            if (formatted == null || !formatted.startsWith("\$")) return 0
            return Double.parseDouble(formatted.substring(1)) * 100
        }
    }

    /** Record for validation examples */
    static record UserInput(String rawInput) {
        ValidatedEmail toValidatedEmail() {
            if ("" == rawInput) {
                return null
            }
            if (rawInput == null || !rawInput.contains("@")) {
                return new ValidatedEmail(null, "Invalid email format")
            }
            return new ValidatedEmail(rawInput, null)
        }
    }

    static record ValidatedEmail(String email, String error) {
        boolean isValid() { error == null }

        UserInput toUserInput() {
            new UserInput(email)
        }
    }

    /** Sealed interface for unit conversion examples (from documentation) */
    sealed interface Length permits Length.Millimeter, Length.Meter, Length.Kilometer {
        record Millimeter(double value) implements Length {
            Meter toMeter() { new Meter(value / 1000) }
            Kilometer toKilometer() { new Kilometer(value / 1_000_000) }
        }
        record Meter(double value) implements Length {
            Millimeter toMillimeter() { new Millimeter(value * 1000) }
            Kilometer toKilometer() { new Kilometer(value / 1000) }
        }
        record Kilometer(double value) implements Length {
            Millimeter toMillimeter() { new Millimeter(value * 1_000_000) }
            Meter toMeter() { new Meter(value * 1000) }
        }
    }

    /** Record for date formatting examples */
    static record Event(LocalDate date, String description) {
        Event withDate(LocalDate date) { new Event(date, description) }
        Event withDescription(String description) { new Event(date, description) }

        String formattedDate() {
            date == null ? "" : date.format(DateTimeFormatter.ISO_DATE)
        }

        static LocalDate parseFormattedDate(String formatted) {
            try {
                return formatted == null || formatted.isEmpty() ?
                    null : LocalDate.parse(formatted, DateTimeFormatter.ISO_DATE)
            } catch (DateTimeParseException e) {
                return null
            }
        }
    }

    // ==================== Basic Projection Tests ====================

    def 'A basic projection creates a bi-directional mapping between two representations.'() {
        reportInfo """
            The simplest form of projection establishes a perfect isomorphism
            between two representations of the same data. Changes in either
            representation automatically propagate to the other.
            
            This example demonstrates temperature conversion between Celsius and Fahrenheit.
            The conversion functions form a mathematical isomorphism:
            - f(c) = c × 9/5 + 32
            - c(f) = (f - 32) × 5/9
            
            For all temperatures, f(c(f)) = f and c(f(c)) = c.
        """
        given: 'A Celsius temperature property'
            var celsius = Var.of(new Celsius(100.0))

        and: 'A Fahrenheit projection using isomorphic conversion functions'
            var fahrenheit = celsius.projectTo(
                Celsius::toFahrenheit,
                Fahrenheit::toCelsius
            )

        expect: 'Initial conversion is correct'
            celsius.get().value() == 100.0
            fahrenheit.get().value() == 212.0  // 100°C = 212°F

        when: 'Celsius is updated through the source property'
            celsius.set(new Celsius(0.0))

        then: 'Fahrenheit updates automatically'
            celsius.get().value() == 0.0
            fahrenheit.get().value() == 32.0  // 0°C = 32°F

        when: 'Fahrenheit is updated through the projection'
            fahrenheit.set(new Fahrenheit(77.0))  // 25°C

        then: 'Celsius updates automatically'
            celsius.get().value() == 25.0
            fahrenheit.get().value() == 77.0

        when: 'We create a chain of projections'
            var fahrenheitAsString = fahrenheit.projectTo(
                { f -> String.format("%.1f°F", f.value()) },
                { s -> new Fahrenheit(Double.parseDouble(s.replace("°F", ""))) }
            )

        then: 'Changes propagate through the chain'
            fahrenheitAsString.get() == "77.0°F"

            celsius.set(new Celsius(100.0))
            fahrenheitAsString.get() == "212.0°F"

            fahrenheitAsString.set("32.0°F")
            celsius.get().value() == 0.0
    }

    def 'Projection maintains referential transparency for isomorphic functions.'() {
        reportInfo """
            For a projection to behave predictably, the conversion functions
            should ideally form an isomorphism. This means:
            
            1. For all t: setter.apply(getter.apply(t)).equals(t)
            2. For all b: getter.apply(setter.apply(b)).equals(b)
            
            This test verifies that updates round-trip correctly through
            the projection, maintaining data integrity.
        """
        given: 'Various isomorphic transformation examples'
            // String reversal (perfect isomorphism)
            var original = Var.of("Hello World")
            var reversed = original.projectTo(
                { s -> new StringBuilder(s).reverse().toString() },
                { r -> new StringBuilder(r).reverse().toString() }
            )

            // Unit conversion within Length sealed interface
            var meters = Var.of(new Length.Meter(2.5))
            var millimeters = meters.projectTo(
                Length.Meter::toMillimeter,
                Length.Millimeter::toMeter
            )

            // Boolean inversion (perfect isomorphism)
            var flag = Var.of(true)
            var inverted = flag.projectTo(
                { b -> !b },
                { b -> !b }
            )

        expect: 'Initial projections are correct'
            reversed.get() == "dlroW olleH"
            millimeters.get().value() == 2500.0
            inverted.get() == false

        when: 'We perform round-trip updates'
            // Test string reversal
            original.set("Sprouts")
        then:
            reversed.get() == "stuorpS"

        when: reversed.set("desreveR")
        then: original.get() == "Reversed"

        // Test unit conversion
        when: meters.set(new Length.Meter(1.0))
        then: millimeters.get().value() == 1000.0

        when: millimeters.set(new Length.Millimeter(500.0))
        then: meters.get().value() == 0.5

        // Test boolean inversion
        when: flag.set(false)
        then: inverted.get() == true

        when: inverted.set(false)  // false inverted is true
        then: flag.get() == true

        and: 'Multiple updates maintain consistency'
            // Verify no information loss after multiple transformations
            for (i in 1..10) {
                meters.update { m -> new Length.Meter(m.value() + 0.1) }
                assert Math.abs(millimeters.get().value() - meters.get().value() * 1000) < 0.0001
            }
    }

    def 'Projection with non-isomorphic functions may cause data loss or unexpected behavior.'() {
        reportInfo """
            When projection functions are not perfectly isomorphic,
            updates may not round-trip correctly, leading to:
            1. Data loss (information discarded during conversion)
            2. State drift (values diverge from expected)
            3. Infinite update loops (if not properly guarded)
            
            This is not necessarily wrong - sometimes we want lossy conversions
            (e.g., rounding decimals). But we must be aware of the consequences.
        """
        given: 'A lossy projection (rounding to nearest integer)'
            var precise = Var.of(3.14159d)
            var rounded = precise.projectTo(
                { d -> Math.round(d) },
                { l -> l.toDouble() }
            )

        and: 'A many-to-one projection (absolute value)'
            var signed = Var.of(-5)
            var absolute = signed.projectTo(
                Math::abs,
                { abs -> abs }  // Lossy: can't recover original sign
            )

        expect: 'Initial projections work'
            rounded.get() == 3L
            absolute.get() == 5

        when: 'We update through lossy projection'
            rounded.set(4L)

        then: 'Information is lost (fractional part)'
            precise.get() == 4.0  // Not 3.14159 anymore

        when: 'We try to recover original sign through absolute projection'
            absolute.set(10)

        then: 'Original could be either 10 or -10'
            signed.get() == 10  // Lost information about original sign

        when: 'We create a projection that could cause infinite loops'
            var counter = 0
            var source = Var.of(0)
            var dangerous = source.projectTo(
                { n -> n + 1 },  // Always different
                { n -> n - 1 }   // Always different
            )

            // Add a guard to prevent infinite loops
            Viewable.cast(source).onChange(From.ALL) { counter++ }

        then: 'Single update causes expected propagation'
            dangerous.set(5)
            source.get() == 4
            counter == 1  // Only one change event
    }

    // ==================== Projection with Null Object Tests ====================

    def 'Projection with null object provides guaranteed non-null values.'() {
        reportInfo """
            The `projectTo(nullObject, getter, setter)` method ensures the
            projected property never contains null. When the source is null,
            the projection uses the provided nullObject as a fallback.
            
            This is useful for:
            1. Providing safe defaults for null source values
            2. Ensuring downstream code doesn't need null checks
            3. Creating user-friendly representations of missing data
        """
        given: 'A nullable product property'
            var product = Var.ofNullable(Product.class, null)
            var defaultProduct = new Product("Unnamed", 0.0)

        and: 'A projection with null object fallback'
            var safeProduct = product.projectTo(
                defaultProduct,
                { p -> p == null ? defaultProduct : p },
                { p -> p == defaultProduct ? null : p }
            )

        expect: 'Null source uses null object'
            product.orElseNull() == null
            safeProduct.get() == defaultProduct

        when: 'Source is set to a real product'
            var realProduct = new Product("Laptop", 99999.0)
            product.set(realProduct)

        then: 'Projection shows real product'
            safeProduct.get() == realProduct

        when: 'Projection is modified'
            safeProduct.set(new Product("Tablet", 49999.0))

        then: 'Source is updated'
            product.get() == new Product("Tablet", 49999.0)

        when: 'Projection is set to null object'
            safeProduct.set(defaultProduct)

        then: 'Source becomes null'
            product.orElseNull() == null
            safeProduct.get() == defaultProduct

        and: 'Multiple operations maintain consistency'
            // Cycle through various states
            var testProducts = [
                null,
                new Product("A", 100),
                new Product("B", 200),
                null,
                new Product("C", 300)
            ]

            for (testProd in testProducts) {
                product.set(testProd)
                if (testProd == null) {
                    assert safeProduct.get() == defaultProduct
                } else {
                    assert safeProduct.get() == testProd
                }
            }
    }

    def 'Null object projection handles edge cases gracefully.'() {
        reportInfo """
            The null object in a projection should be semantically meaningful
            and handle various edge cases properly:
            
            1. The null object itself should not be null
            2. The null object should be distinguishable from "real" values
            3. Conversion functions should handle the null object explicitly
            4. Memory references to the null object should be stable
        """
        given: 'Various null object scenarios'
            // String with empty string as null object
            var nullableString = Var.ofNullable(String.class, null)
            var safeString = nullableString.projectTo(
                "",
                { s -> s == null ? "" : s },
                { s -> s.isEmpty() ? null : s }
            )
        and :
            // List with empty list as null object
            var emptyList = []
            var nullableList = Var.ofNullable(List.class, null)
            var safeList = nullableList.projectTo(
                emptyList,
                { list -> list == null ? [] : list },
                { list -> list.isEmpty() ? null : list }
            )

            // Custom object with sentinel as null object
            var sentinel = new Product("SENTINEL", -1.0)
            var nullableProduct = Var.ofNullable(Product.class, null)
            var safeProduct = nullableProduct.projectTo(
                sentinel,
                { p -> p == null ? sentinel : p },
                { p -> p == sentinel ? null : p }
            )

        expect: 'Initial states use null objects'
            safeString.get() == ""
            safeList.get() == []
            safeProduct.get() == sentinel

        when: 'We modify through projections'
            safeString.set("Hello")
            safeList.set(["a", "b", "c"])
            safeProduct.set(new Product("Real", 100d))

        then: 'Sources are updated'
            nullableString.get() == "Hello"
            nullableList.get() == ["a", "b", "c"]
            nullableProduct.get() == new Product("Real", 100d)

        when: 'We set projections back to null objects'
            safeString.set("")
            safeList.set([])
            safeProduct.set(sentinel)

        then: 'Sources become null'
            nullableString.orElseNull() == null
            nullableList.orElseNull() == null
            nullableProduct.orElseNull() == null
        and:
            safeList.get() == safeList.get()
            safeProduct.get() == sentinel
    }

    // ==================== Nullable Projection Tests ====================

    def 'Nullable projection allows null values in both directions.'() {
        reportInfo """
            The `projectToNullable(type, getter, setter)` method creates a projection
            where null is a valid value in both the source and target domains.
            
            This is useful for:
            1. Partial functions that may fail to convert
            2. Optional or missing data representations
            3. Error states or invalid conversions
            4. Graceful degradation when conversion isn't possible
        """
        given: 'A string property for date parsing'
            var dateString = Var.ofNullable(String.class, "2023-12-25")

        and: 'A nullable projection to LocalDate'
            var date = dateString.projectToNullable(
                LocalDate.class,
                { s ->
                    try {
                        return s == null ? null : LocalDate.parse(s)
                    } catch (Exception e) {
                        return null  // Parsing failed
                    }
                },
                { d -> d == null ? null : d.toString() }
            )

        expect: 'Initial parsing works'
            date.get() == LocalDate.of(2023, 12, 25)

        when: 'String is set to invalid date'
            dateString.set("invalid-date")

        then: 'Projection becomes null'
            date.orElseNull() == null

        when: 'Projection is set to null'
            date.set(null)

        then: 'String is still the invalid string because nothing changed!'
            dateString.orElseNull() == "invalid-date"

        when: 'Projection is set to valid date'
            date.set(LocalDate.of(2024, 1, 1))

        then: 'The String updates accordingly.'
            dateString.get() == "2024-01-01"

        when: 'Projection is set back to null...'
            date.set(null)

        then: 'The String is also set to null!'
            dateString.orElseNull() == null

        and: 'Null propagation works in cycles'
            def testCases = [
                [input: "2023-01-01", expected: LocalDate.of(2023, 1, 1)],
                [input: "not-a-date", expected: null],
                [input: null, expected: null],
                [input: "2023-12-31", expected: LocalDate.of(2023, 12, 31)]
            ]

            for (testCase in testCases) {
                dateString.set(testCase.input)
                assert date.orElseNull() == testCase.expected
            }
    }

    def 'Nullable projection handles validation scenarios elegantly.'() {
        reportInfo """
            Nullable projections are perfect for validation boundaries,
            where invalid input maps to null in the validated domain.
            
            This creates a clean separation:
            - Raw domain: Accepts any input (including invalid)
            - Validated domain: Only contains valid values (or null)
            
            Changes propagate in both directions, but validation failures
            result in null in the validated domain.
        """
        given: 'User input for email validation'
            var userInput = Var.of(new UserInput("user@example.com"))
            var emptyInput = new UserInput("")

        and: 'Nullable projection to validated email'
            var validatedEmail = userInput.projectToNullable(
                ValidatedEmail.class,
                UserInput::toValidatedEmail,
                { it -> it == null ? emptyInput : it.toUserInput() }
            )

        expect: 'Valid email passes through'
            validatedEmail.get().email() == "user@example.com"
            validatedEmail.get().isValid()

        when: 'User enters invalid email'
            userInput.set(new UserInput("not-an-email"))

        then: 'Validated email becomes null'
            validatedEmail.orElseNull() == new ValidatedEmail(null, "Invalid email format")

        when: 'User enters an empty email'
            userInput.set(new UserInput(""))

        then: 'Validated email becomes null'
            validatedEmail.orElseNull() == null

        when: 'Validated email is set programmatically'
            validatedEmail.set(new ValidatedEmail("admin@example.com", null))

        then: 'User input updates'
            userInput.get().rawInput() == "admin@example.com"

        when: 'Validated email is set to null'
            validatedEmail.set(null)

        then: 'The user input clears:'
            userInput.get().rawInput() == ""

        when:
            userInput.set(new UserInput(""))
        then :
            validatedEmail.orElseNull() == null

        when :
            userInput.set(new UserInput("valid@email.com"))
        then :
            validatedEmail.get().isValid()
    }

    // ==================== Memory Safety Tests ====================

    def 'Projection properties are garbage collected when no longer referenced.'() {
        reportInfo """
            Like all Sprouts properties, projection properties use weak references
            to avoid memory leaks. When a projection is no longer strongly referenced,
            it should be garbage collected, and its listeners should be removed
            from the source property.
            
            This test verifies proper memory management for projection properties.
        """
        given: 'A source property and strong projection reference'
            var source = Var.of("Original")
            var strongProjection = source.projectTo(
                { s -> s.toUpperCase() },
                { u -> u.toLowerCase() }
            )
        and:
            waitForGarbageCollection()
            waitForGarbageCollection()

        expect: 'Source has change listeners for the projection'
            source.numberOfChangeListeners() == 1

        when: 'We create a weak reference to another projection'
            var weakProjection = source.projectTo(
                { s -> s + "!" },
                { s -> s.substring(0, s.length() - 1) }
            )
            var weakRef = new WeakReference(weakProjection)

        and: 'We release the strong reference and force GC'
            weakProjection = null
            waitForGarbageCollection()
            waitForGarbageCollection()

        then: 'Weak projection is collected'
            weakRef.get() == null

        when :
            waitForGarbageCollection()
            waitForGarbageCollection()
        then: 'Source listener count decreases'
            source.numberOfChangeListeners() == 1  // Only strong projection remains

        when: 'We also release the strong projection'
            strongProjection = null
            waitForGarbageCollection()
            waitForGarbageCollection()

        then: 'All projections are collected'
            source.numberOfChangeListeners() == 0
    }

    def 'Projection chain maintains proper reference relationships.'() {
        reportInfo """
            When creating chains of projections (A → B → C), the memory references
            should form a directed graph where:
            
            1. Each projection weakly references its source
            2. The source strongly references nothing (except its own state)
            3. Garbage collection can collect any unreferenced projection
            
            This prevents memory leaks in complex projection chains.
        """
        given:
            var trace = []
        and : 'A chain of projections: string → upper → reversed'
            var source = Var.of("hello")
            var upper = source.projectTo(
                String::toUpperCase,
                String::toLowerCase
            )
            var reversed = upper.projectTo(
                { s -> new StringBuilder(s).reverse().toString() },
                { r -> new StringBuilder(r).reverse().toString() }
            )
        and :
            Viewable.cast(reversed).onChange(From.ALL, {
                trace << it.currentValue().orElseNull()
            })

        and: 'Weak references to track garbage collection'
            var sourceRef = new WeakReference(source)
            var upperRef = new WeakReference(upper)
            var reversedRef = new WeakReference(reversed)

        when: 'We release intermediate projection'
            upper = null
            waitForGarbageCollection()

        then:
            trace == []

        when: 'We update source'
            sourceRef.get().set("world")

        then: 'Chain still propagates (through weak reference)'
            trace == ["DLROW"]

        when: 'We release all projections'
            reversed = null
            waitForGarbageCollection()
            waitForGarbageCollection()

        then: 'All projections collected, source remains'
            reversedRef.get() == null
            sourceRef.get() != null
            sourceRef.get().numberOfChangeListeners() == 0
        and :
            trace == ["DLROW"]
    }

    // ==================== Error Handling Tests ====================

    def 'Projection handles exceptions in conversion functions gracefully.'() {
        reportInfo """
            Conversion functions in projections may throw exceptions
            (e.g., parsing errors, validation failures). The projection
            should handle these gracefully without crashing the application.
            
            For nullable projections, exceptions typically result in null.
            For non-nullable projections, exceptions may need explicit handling.
        """
        given: 'A projection with potentially throwing functions'
            var source = Var.of("123")
            var parser = source.projectToNullable(
                Integer.class,
                { s ->
                    try {
                        return Integer.parseInt(s)
                    } catch (NumberFormatException e) {
                        return null  // Graceful failure
                    }
                },
                { i -> i == null ? "0" : i.toString() }
            )

        and: 'A projection that throws unconditionally in one direction'
            var dangerousSource = Var.of("test")
            var dangerous = dangerousSource.projectToNullable(
                String.class,
                { s ->
                    if ( s == "test" )
                        return "initial"
                    else
                        throw new RuntimeException("Always fails") },
                { s -> s }  // This direction works
            )

        expect: 'Valid parsing works'
            parser.get() == 123

        when: 'Source becomes unparsable'
            source.set("not-a-number")

        then: 'Projection becomes null (graceful failure)'
            parser.orElseNull() == null

        when: 'We access dangerous projection'
            var result = dangerous.orElseNull()

        then: 'Exception in getter results in null (or last known value)'
            result == "initial"

        and: 'Setter direction still works'
            dangerous.set("new value")
            dangerousSource.get() == "new value"
    }

    def 'Projection maintains state when getter fails but setter works.'() {
        reportInfo """
            In some scenarios, the getter may fail (returning null or throwing)
            while the setter continues to work. The projection should:
            
            1. Preserve the last known good value when getter fails
            2. Allow updates through the setter
            3. Attempt to recover when getter starts working again
            
            This provides robustness in the face of transient failures.
        """
        given: 'A projection with toggleable getter failure'
            var source = Var.of("initial")
            var getterFails = false

            var projection = source.projectToNullable(
                String.class,
                { s ->
                    if (getterFails) {
                        throw new RuntimeException("Getter failed!")
                    }
                    return s == null ? null : s.toUpperCase()
                },
                { u -> u == null ? null : u.toLowerCase() }
            )

        expect: 'Initial state works'
            projection.get() == "INITIAL"

        when: 'Getter starts failing'
            getterFails = true

        then: 'Projection may show last known value or null'
            // Implementation dependent - should not crash
            noExceptionThrown()

        when: 'We update through projection (setter still works)'
            projection.set("NEWVALUE")

        then: 'Source is updated despite getter failure'
            source.get() == "newvalue"

        when: 'Getter recovers'
            getterFails = false

        then: 'Projection shows current state'
            projection.get() == "NEWVALUE"
    }

    // ==================== Channel Propagation Tests ====================

    def 'Projection propagates change channels correctly.'() {
        reportInfo """
            When a source property is updated with a specific channel
            (VIEW_MODEL, VIEW, or ALL), the projection should propagate
            that channel to its listeners.
            
            This allows differentiated handling of changes based on their
            origin (user input vs programmatic update).
        """
        given: 'A temperature projection with channel-aware listeners'
            var celsius = Var.of(new Celsius(20.0))
            var fahrenheit = celsius.projectTo(
                Celsius::toFahrenheit,
                Fahrenheit::toCelsius
            )

            var celsiusTrace = []
            var fahrenheitTrace = []

            Viewable.cast(celsius).onChange(From.VIEW_MODEL) { celsiusTrace << "VIEW_MODEL:${it.currentValue().orElseThrow().value()}" }
            Viewable.cast(celsius).onChange(From.VIEW) { celsiusTrace << "VIEW:${it.currentValue().orElseThrow().value()}" }
            Viewable.cast(fahrenheit).onChange(From.VIEW_MODEL) { fahrenheitTrace << "VIEW_MODEL:${it.currentValue().orElseThrow().value()}" }
            Viewable.cast(fahrenheit).onChange(From.VIEW) { fahrenheitTrace << "VIEW:${it.currentValue().orElseThrow().value()}" }

        when: 'Celsius is updated from VIEW_MODEL'
            celsius.set(From.VIEW_MODEL, new Celsius(25.0))

        then: 'Both properties notify VIEW_MODEL listeners'
            celsiusTrace == ["VIEW_MODEL:25.0"]
            fahrenheitTrace == ["VIEW_MODEL:77.0"]  // 25°C = 77°F

        when: 'Fahrenheit is updated from VIEW (simulating user input)'
            fahrenheit.set(From.VIEW, new Fahrenheit(32.0))

        then: 'Both properties notify VIEW listeners'
            celsiusTrace == ["VIEW_MODEL:25.0", "VIEW:0.0"]  // 32°F = 0°C
            fahrenheitTrace == ["VIEW_MODEL:77.0", "VIEW:32.0"]
    }

    // ==================== Real-World Use Case Tests ====================

    def 'Projection enables clean MVVM pattern with formatted display values.'() {
        reportInfo """
            A common MVVM pattern uses projections to separate:
            - Model: Raw data (cents, unformatted dates)
            - ViewModel: Formatted display values
            
            The projection automatically keeps them in sync:
            - User edits formatted value → model updates
            - Model changes programmatically → display updates
            
            No manual synchronization code needed!
        """
        given: 'Product model with price in cents'
            var product = Var.of(new Product("Widget", 1999.0))  // $19.99

        and: 'Formatted price projection for UI display'
            var formattedPrice = product.projectTo(
                { p -> p.formattedPrice() },
                { formatted -> new Product(
                        product.get().name(),
                        Product.parseFormattedPrice(formatted)
                    )
                }
            )

        and: 'UI simulation with trace'
            var uiUpdates = []
            Viewable.cast(formattedPrice).onChange(From.VIEW_MODEL) {
                uiUpdates << "UI shows: ${it.currentValue().orElseThrow()}"
            }

        expect: 'Initial formatting is correct'
            formattedPrice.get() == "\$19.99"

        when: 'Model updates programmatically (sale price)'
            product.set(new Product("Widget", 1499.0))  // $14.99

        then: 'UI updates automatically'
            formattedPrice.get() == "\$14.99"
            uiUpdates == ["UI shows: \$14.99"]

        when: 'User edits price in UI (with dollar sign)'
            formattedPrice.set("\$24.99")

        then: 'Model updates automatically'
            product.get().priceInCents() == 2499.0
            uiUpdates == ["UI shows: \$14.99", "UI shows: \$24.99"]

        and: 'Format validation works naturally'
            // Invalid input would need nullable projection
            var nullableFormatted = product.projectToNullable(
                String.class,
                { p -> p.formattedPrice() },
                { formatted ->
                    try {
                        new Product(
                            product.get().name(),
                            Product.parseFormattedPrice(formatted)
                        )
                    } catch (Exception e) {
                        null  // Invalid input
                    }
                }
            )
        when :
            nullableFormatted.set("invalid")
        then :
            product.get().priceInCents() == 0.0
    }

    def 'Projection enables two-way data binding for encryption scenarios.'() {
        reportInfo """
            Projections are perfect for encryption/decryption boundaries:
            - Plain text property (for business logic)
            - Encrypted text property (for storage/transmission)
            
            Changes to either automatically encrypt/decrypt to the other.
            The conversion functions should be isomorphic for perfect
            round-trip conversion.
        """
        given: 'Plain text message'
            var plain = Var.of("Secret Message")

        and: 'Encrypted projection using simple Caesar cipher'
            var encrypted = plain.projectTo(
                { text ->
                    text.chars()
                        .map { c -> c + 3 }
                        .collect { (char) it }
                        .join()
                },
                { cipher ->
                    cipher.chars()
                        .map { c -> c - 3 }
                        .collect { (char) it }
                        .join()
                }
            )

        expect: 'Initial encryption works'
            plain.get() == "Secret Message"
            encrypted.get() == "Vhfuhw#Phvvdjh"  // Each char + 3

        when: 'Plain text is updated'
            plain.set("New Secret")

        then: 'Encrypted text updates automatically'
            encrypted.get() == "Qhz#Vhfuhw"

        when: 'Encrypted text is updated (simulating receiving encrypted data)'
            encrypted.set("Uhdg#Vdpsoh")

        then: 'Plain text is decrypted automatically'
            plain.get() == "Read Sample"  // Each char - 3

        and: 'Round-trip conversion is lossless'
            var testMessages = ["Hello", "World", "123!@#", "", "Multi\nLine"]
            for (testMsg in testMessages) {
                plain.set(testMsg)
                var encryptedMsg = encrypted.get()
                encrypted.set("different")  // Break symmetry
                encrypted.set(encryptedMsg)  // Restore
                assert plain.get() == testMsg  // Perfect recovery
            }
    }

    def 'Projection creates type-safe unit conversion systems.'() {
        reportInfo """
            Using sealed interfaces and projections, we can create
            type-safe unit conversion systems where:
            - Each unit is a distinct type
            - Conversions are compile-time checked
            - Projections handle the conversion automatically
            
            This eliminates unit confusion bugs and provides
            automatic conversion between representations.
        """
        given: 'Length in millimeters'
            var mm = Var.of(new Length.Millimeter(1500.0))

        and: 'Projections to other units'
            var meters = mm.projectTo(
                Length.Millimeter::toMeter,
                Length.Meter::toMillimeter
            )

            var km = meters.projectTo(
                Length.Meter::toKilometer,
                Length.Kilometer::toMeter
            )

        expect: 'Initial conversions are correct'
            mm.get().value() == 1500.0
            meters.get().value() == 1.5
            km.get().value() == 0.0015

        when: 'Millimeters are updated'
            mm.set(new Length.Millimeter(3000.0))

        then: 'All projections update'
            meters.get().value() == 3.0
            km.get().value() == 0.003

        when: 'Kilometers are updated (e.g., from map scale)'
            km.set(new Length.Kilometer(2.0))

        then: 'All units synchronize'
            meters.get().value() == 2000.0
            mm.get().value() == 2_000_000.0

        and: 'Mathematical consistency is maintained'
            // Test associative property: mm → m → km should equal mm → km
            var directKm = mm.projectTo(
                Length.Millimeter::toKilometer,
                Length.Kilometer::toMillimeter
            )

            for (testValue in [0.0, 1.0, 1000.0, 1_000_000.0]) {
                mm.set(new Length.Millimeter(testValue))
                assert Math.abs(km.get().value() - directKm.get().value()) < 0.0000001
            }
    }

    // ==================== Edge Case Tests ====================

    @Unroll
    def 'Projection handles edge cases for type #typeName'(String typeName, Object nullObject, Object testValue) {
        reportInfo """
            Projections should handle various edge cases gracefully:
            - Empty strings and collections
            - Boundary values
            - Special floating point values
            - Identity projections
        """
        given: 'Source property with edge case value'
            var source = Var.ofNullable(Object.class, testValue)

        and: 'Identity projection (value unchanged)'
            var identity = source.projectTo(
                { it -> it },
                { it -> it }
            )

        and: 'Nullable identity projection'
            var nullableIdentity = source.projectToNullable(
                Object.class,
                { it -> it },
                { it -> it }
            )

        expect: 'Identity projection preserves value'
            identity.orElseNull() == testValue

        and: 'Nullable identity projection preserves value'
            nullableIdentity.orElseNull() == testValue

        when: 'Source is set to null'
            source.set(null)

        then: 'Projections reflect null'
            identity.orElseNull() == null
            nullableIdentity.orElseNull() == null

        where:
            typeName      | nullObject | testValue
            'String'      | ""         | ""
            'String'      | ""         | " "
            'String'      | ""         | "null"
            'Integer'     | 0          | 0
            'Integer'     | 0          | Integer.MAX_VALUE
            'Integer'     | 0          | Integer.MIN_VALUE
            'Double'      | 0.0        | 0.0
            'Double'      | 0.0        | Double.NaN
            'Double'      | 0.0        | Double.POSITIVE_INFINITY
            'List'        | []         | []
            'List'        | []         | [null]
            'Boolean'     | false      | true
            'Boolean'     | false      | false
    }

    def 'Projection with null object maintains stability under concurrent access patterns.'() {
        reportInfo """
            The null object in a projection should provide stable behavior
            even under patterns that might cause issues:
            
            1. Repeated setting to null object
            2. Rapid toggling between null and non-null
            3. Concurrent-like access patterns
            4. Memory reference stability
        """
        given: 'Source with null object projection'
            var source = Var.ofNullable(String.class, "initial")
            var nullObject = "[EMPTY]"
            var projection = source.projectTo(
                nullObject,
                { s -> s == null ? nullObject : s },
                { p -> p == nullObject ? null : p }
            )

            var changeCount = 0
            Viewable.cast(projection).onChange(From.ALL) { changeCount++ }

        when: 'We rapidly toggle between states'
            for (i in 1..100) {
                source.set("value"+i)
                source.set(null)
            }

        then: 'Projection remains stable'
            projection.get() == nullObject
            changeCount > 0  // Some changes occurred

        when: 'We set projection repeatedly to null object'
            for (i in 1..50) {
                projection.set(nullObject)
            }

        then: 'Source remains null, no unnecessary changes'
            source.orElseNull() == null

        and: 'Null object reference is stable'
            var firstRef = projection.get()
            for (i in 1..10) {
                source.set("test"+i)
                source.set(null)
                assert projection.get().is(firstRef)  // Same instance
            }
    }

    // ==================== Mathematical Property Tests ====================

    def 'Ideal projection functions form a mathematical isomorphism.'() {
        reportInfo """
            For perfect round-trip conversion, projection functions should be:
            
            1. Injective (one-to-one): f(a) = f(b) ⇒ a = b
            2. Surjective (onto): ∀b∈B, ∃a∈A: f(a) = b
            3. Total: Defined for all inputs
            
            This test verifies mathematical properties of ideal projections.
        """
        given: 'Perfect isomorphism: string reversal'
            var isReversible = { String a, String b ->
                a.reverse() == b && b.reverse() == a
            }

            var source = Var.of("abcdef")
            var projection = source.projectTo(
                String::reverse,
                String::reverse
            )

        expect: 'Functions are inverses'
            isReversible(source.get(), projection.get())
        and: 'We test injectivity (one-to-one)'
            // For reversal, different strings give different reversed strings
            var testPairs = [["ab", "ba"], ["abc", "cba"], ["a", "a"]]
            for (pair in testPairs) {
                source.set(pair[0])
                assert projection.get() == pair[1]
                assert source.get() == pair[0]  // Can recover original
            }
        and: 'We test surjectivity (onto)'
            // For reversal, every string has a reverse
            var testValues = ["", "x", "123", "hello world"]
            for (value in testValues) {
                projection.set(value)
                assert source.get() == value.reverse()
                assert projection.get() == value  // Can reach any value
            }
        and: 'Round-trip property holds'
            var testStrings = ["", "a", "ab", "abc", "test", "123!@#"]
            for (testStr in testStrings) {
                source.set(testStr)
                var throughProjection = projection.get()
                projection.set(throughProjection)
                assert source.get() == testStr  // Perfect round-trip
            }
    }

    def 'Projection composition follows mathematical rules.'() {
        reportInfo """
            When composing projections (A → B → C), the composition
            should behave predictably according to function composition rules.
            
            For perfect isomorphisms: (g∘f)⁻¹ = f⁻¹∘g⁻¹
        """
        given: 'Chain of isomorphic projections: lower → upper → reversed'
            var lower = Var.of("hello")
            var upper = lower.projectTo(
                String::toUpperCase,
                String::toLowerCase
            )
            var reversed = upper.projectTo(
                { s -> new StringBuilder(s).reverse().toString() },
                { r -> new StringBuilder(r).reverse().toString() }
            )

        and: 'Direct projection: lower → reversed'
            var direct = lower.projectTo(
                { s -> new StringBuilder(s.toUpperCase()).reverse().toString() },
                { r -> new StringBuilder(r).reverse().toString().toLowerCase() }
            )

        expect: 'Composition equals direct projection'
            reversed.get() == direct.get()

        when: 'We update through chain'
            lower.set("world")

        then: 'Both paths give same result'
            reversed.get() == direct.get()

        when: 'We update through reversed in chain'
            reversed.set("!DLROW")

        then: 'Both paths update source correctly'
            lower.get() == "world!"  // Reversed twice, lowercased

        and: 'Mathematical composition property holds'
            // Test that (g∘f)⁻¹ = f⁻¹∘g⁻¹
            var testValues = ["a", "ab", "abc", "test", "123"]
            for (value in testValues) {
                lower.set(value)
                var viaChain = reversed.get()
                reversed.set(viaChain)
                var recoveredViaChain = lower.get()

                lower.set(value)
                var viaDirect = direct.get()
                direct.set(viaDirect)
                var recoveredViaDirect = lower.get()

                assert recoveredViaChain == recoveredViaDirect
            }
    }

    /**
     * This method guarantees that garbage collection is
     * done unlike <code>{@link System#gc()}</code>
     */
    static void waitForGarbageCollection() {
        Object obj = new Object();
        WeakReference ref = new WeakReference<>(obj);
        obj = null;
        while(ref.get() != null) {
            System.gc();
        }
    }
}