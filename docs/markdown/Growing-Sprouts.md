# Sprouts: Immutable State Management

> **🌱 Sprouts** is a Java library designed for modern application development, 
> combining **immutable data structures** with **reactive properties** to create robust, 
> maintainable, and memory leak-free state management.

## Table of Contents
- [The Philosophy](#the-philosophy)
- [Core Concepts](#core-concepts)
- [Immutable Domain Modeling](#immutable-domain-modeling)
- [Property Lenses](#property-lenses-and-views)
- [Avoiding Memory Leaks](#memory-leak-safe-reactive-listeners)
- [Working with Collections](#working-with-collections)
- [Practical Examples](#practical-example)

## The Philosophy

Sprouts embraces **Data-Oriented Programming** - treating data as immutable 
values rather than mutable objects with identity to share them freely in your program
without side effects. 
This approach brings several benefits:

- **Predictability**: Immutable data leads to more predictable code
- **Thread Safety**: No synchronization needed for immutable values
- **Testability**: Pure functions are easier to test
- **Debugging**: State changes are explicit and traceable

> 💡 **Trivia**: The "Sprouts" library was developed together with the "SwingTree" UI framework.
> The names of these projects are inspired by the way plants form similar branching data structures as in
> the world of computer science. More specifically, the idea that state "grows" through successive immutable versions, 
> much like a plant growing new leaves while keeping its core structure (structural sharing).

## Core Concepts

The biggest problem we as developers face when trying to switch to using
immutable data structures is interoperability with existing mutable systems.
The real world is full of stateful systems like UI frameworks, databases, and network APIs
where state changes are expected to happen for individual properties.
So in a sense, they impose a **place-oriented** data models onto us.
Sprouts solves this problem elegantly through **reactive properties** with
built-in support for **lenses** to "zoom into" nested data structures.

But more on that later - first, let's get an overview of 
the most important building blocks of the library.

### Var - Mutable Properties

The core member of this library is the `Var<T>` type, which is a mutable wrapper
designed to hold immutable values. It extends `Val<T>` with mutation capabilities 
through `set(T)` and `update(Function<T,T>)` methods:

```java
// Creating mutable properties
Var<String> message = Var.of("Initial message");

// Updating values
message.set("Updated message"); // Creates new immutable instance

// Functional updates
message.update(current -> current + "!");
```

### Val - Readonly Properties

The `Val<T>` interface is the read-only supertype of `Var<T>`.
It does not expose any mutating methods, making it suitable for
sharing a read-only view of a property:

```java
Var<Integer> number = Var.of(42);
Val<Integer> readOnlyNumber = number; // Upcast to Val
```
However, you can also create standalone immutable properties using `Val.of(T)`
and use it like you would use an `Optional<T>`:
```java
// Creating immutable properties
Val<String> username = Val.of("Mr Bean");
Val<Integer> age = Val.of(42);

// Safe operations
if (username.isPresent()) {
    String name = username.get(); // "Mr Bean"
}

// Mapping to new values
Val<String> greeting = username.map(name -> "Hello, " + name);
```

## Immutable Domain Modeling

Sprouts encourages using **records** and **with-methods** for 
creating and updating domain models:

```java
// Traditional mutable approach (not recommended)
class MutablePerson {
    private String name;
    private int age;
    
    // getters, setters, equals, hashCode...
}

// Sprouts approach with records
record Person(String name, int age) {
    // "Wither" methods for updates
    public Person withName(String newName) {
        return new Person(newName, this.age);
    }
    
    public Person withAge(int newAge) {
        return new Person(this.name, newAge);
    }
}
```

The new approach is almost the same thing, except that
instead of writing setters or using the assignment operator,
you use `with` methods that create new instances with the desired changes.

### Complex Domain Models

This principle extends naturally to nested records,
to form larger data structures:

```java
record Address(String street, String city, String zipCode) {
    public Address withStreet(String street) {/*...*/}
    public Address withCity(String city) {/*...*/}
}

record Person(String forename, String surname, int age, Address address) {
    public Person withForename(String forename) {/*...*/}
    public Person withSurname(String surname) {/*...*/}
    public Person withAge(int age) {/*...*/}
    public Person withAddress(Address address) {/*...*/}
}
```

## Property Lenses and Views

**Lenses** and **views** are the most powerful feature of Sprouts - they allow you to "zoom into" 
nested data structures while maintaining immutability.

### Basic Lens Usage

```java
// Create a person property
Var<Person> person = Var.of(
            new Person(
                "Victor", "Frankenstein", 
                40,
                new Address("123 Main St", "Oslo", "12345")
            )
        );

// Create lenses for nested properties
Val<String> fullName = person.viewAsString( p -> p.forename() + " " + p.surname() );
Var<Integer> ageLens = person.zoomTo(Person::age, Person::withAge);
Var<Address> addressLens = person.zoomTo(Person::address, Person::withAddress);

// Create lenses for deeply nested properties
Var<String> streetLens = addressLens.zoomTo(Address::street, Address::withStreet);
Var<String> cityLens = addressLens.zoomTo(Address::city, Address::withCity);

// Update through lenses
streetLens.set("456 Oakwood Ave");
// person now contains: Person("Alice", "Frankenstein", 30, Address("456 Oakwood Ave", "Oslo", "12345"))
```

## Memory Leak Safe Reactive Listeners

One of Sprouts' key innovations is its **automatic cleanup** of unused listeners!
It achieves this through a separate type called `Viewable<T>`, which is derived from `Var<T>` properties
using the `view()` method which is the actual type onto which you can register listeners safely. You cannot
register listeners directly on `Var<T>` or `Val<T>` properties. The trick is that `Viewable<T>` instance
is only weakly referenced by the property, so when there are no strong references to the view in your code,
it can be garbage collected along with all its listeners.

### Sprouts' Solution: Viewable Properties

```java
Var<String> message = Var.of("Hello");

// Create a weakly referenced view
Viewable<String> messageView = message.view();

// Register listeners safely
messageView.onChange(From.VIEW_MODEL, val -> {
    System.out.println("Message changed to: " + val.currentValue());
});

// When the view goes out of scope, listeners are automatically cleaned up
// Or when you set it to null
messageView = null; // Now eligible for GC
```

> [!TIP]
> Note that the `Viewable<T>` interface extends `Val<T>`, so you can
> upcast it to a read-only property if you do not need to register listeners.

### Best Practices for Memory Safety

```java
public class SafeComponent {
    private final Viewable<String> safeView;
    
    public SafeComponent(Var<String> property) {
        // Create view in constructor, store as field
        this.safeView = property.view().onChange(From.ALL, this::handleChange);
    }
    
    private void handleChange(ValDelegate<String> value) {
        // Safe - won't cause memory leaks
        updateDisplay(value.currentValue());
    }
    
    // Component can be garbage collected normally
}

// Antipattern: Don't do this!
public class UnsafeComponent {
    public UnsafeComponent(Var<String> property) {
        // DON'T: Casting to Viewable creates strong references
        Viewable.cast(property).onChange(From.ALL, val -> {
            // This captures 'this' strongly!
            this.handleChange(val);
        });
    }
}
```

> [!WARNING]
> 🛡️ **Safety Note**: Always use `var.view()` instead 
>  of `Viewable.cast(Var)` unless you're absolutely sure about the lifecycle 
>  and manually manage subscription cleanup.

## Working with Collections

Sprouts provides you with various custom collections optimized for data oriented programming
and which work seamlessly with properties.
They are **immutable and persistent collections**, meaning that operations like adding or removing elements
create new instances that share unchanged parts of the data structure to save memory and CPU time.

### Tuples

Think of `Tuple<T>` as an immutable, type-safe alternative to `List<T>` 
with a fixed element type and rich set of functional operations:

```java
// Creating a tuple of quest items for our hero
Tuple<String> questItems = Tuple.of("Ancient Map", "Healing Potion", "Glowing Rune");

// Functional operations to prepare for the journey
Tuple<String> capitalizedItems = questItems.map(String::toUpperCase);
Tuple<String> importantItems = questItems.retainIf(item -> item.contains("Ancient") || item.contains("Rune"));

// Adding/removing elements creates new instances (structural sharing)
Tuple<String> betterSupplies = questItems.add("Magic Compass");
Tuple<String> lighterPack = questItems.removeAt(1); // No more healing potion? Risky!
```

Tuples are perfect for using them in your record based domain models
to reference an arbitrary number of elements of the same type.
This can even be yet another domain model record if needed.
Like for example a `Party` record containing a `Tuple<Guest>` listing 
all invited guests in the order they were added.

### Associations

An `Association<K, V>` is Sprouts' immutable, type-safe equivalent of a `Map<K, V>`.
So it maintains key-value pairs in which keys are unique.
Every modification returns a new instance with the desired changes.

```java
// Let's map magical creatures to their favorite snacks!
Association<String, String> creatureSnacks = Association.between(
                                                String.class, String.class
                                            )
                                            .put("Dragon", "Roasted Knight")
                                            .put("Unicorn", "Sparkling Berries")
                                            .put("Goblin", "Shiny Trinkets");

// Safely access values
Optional<String> dragonSnack = creatureSnacks.get("Dragon");

// Create new associations with updates
Association<String, String> healthierSnacks = creatureSnacks
    .put("Dragon", "Volcanic Rocks") // Dragons are trying to eat healthier!
    .remove("Goblin"); // Goblins on a diet

// Bulk operations
var moreSnacks = dragonSnack.putAll(
                    Pair.of("Elf", "Lembas Bread"),
                    Pair.of("Troll", "Bridge Rubble"),
                    Pair.of("Dwarf", "Mushroom Stew")
                );

// Filtering
Association<String, String> fruitySnacks = creatureSnacks.removeIf(
        pair -> pair.second().contains("Berries")
);
```

Associations are ideal for untyped and dynamic input data, fast lookup tables, or any scenario where 
you need flexible key-value mappings. If you want to maintain insertion order, consider
creating them using `Association.ofLinked(...)`.

### ValueSet

A `ValueSet<E>` is an immutable set implementation that guarantees 
no duplicates while maintaining value semantics. 
Like other Sprouts collections, all modifications return new instances.

```java
// A wizard's unique spell repertoire
ValueSet<String> spells = ValueSet.of(String.class).addAll("Fireball", "Teleport", "Invisibility", "Shield");

// Attempting to add duplicates changes nothing
ValueSet<String> sameSpells = spells.add("Fireball"); // Returns the same set

// Set operations feel natural
ValueSet<String> defenseSpells = ValueSet.of("Shield", "Barrier");
ValueSet<String> defensiveRepertoire = spells.retainAll(defenseSpells);

// Functional operations
boolean hasLongNames = spells.any(name -> name.length() > 5);
ValueSet<String> longNames = spells.retainIf(name -> name.length() > 5);
```
ValueSets are perfect for representing unique collections of items where order may 
or may not be important. If you want to maintain insertion order, consider
creating them using `ValueSet.ofLinked(...)`.

### Reactive Collections with Properties

By putting Sprouts collections inside `Var<T>` properties, you can create
"reactive collections" that automatically notify listeners of changes.

```java
// A reactive inventory for our game character
Var<Tuple<String>> inventory = Var.of(
                                    Tuple.of("Wooden Sword", "Leather Armor", "Health Potion")
                                );

// Create a view to observe changes
Viewable<Tuple<String>> inventoryView = inventory.view();

// Using associations:
inventoryView.onChange(From.ALL, newInventory -> {
        System.out.println("Inventory changed! Now carrying: " + newInventory.currentValue());
    });

// All modifications are reactive and immutable
inventory.update(items -> items.add("Magic Amulet"));
inventory.update(items -> items.sort(String::compareTo));

// Reactive association example
Var<Association<String, Integer>> reactiveAges = Var.of(
                                                    Association.between(String.class, Integer.class)
                                                            .addAll("Alice", 30, "Bob", 25)
                                                );

// Lens into specific values
Var<Integer> aliceAge = reactiveAges.zoomTo(
    map -> map.get("Alice").orElse(0),
    (map, newAge) -> map.put("Alice", newAge)
);

// Now updating through the lens updates the parent association
aliceAge.set(31); // Updates the entire association immutably
```

## Practical Example

The real power of Sprouts collections emerges when you combine your custom
domain models with `Var` properties to create reactive data structures that 
automatically propagate changes and which give you the ability to 
interact with your data as if it was mutable.

This is where it gives you full interoperability between value-oriented
code and place-oriented systems, like UI frameworks for example.

Let's build a fun, reactive application for planning a fantasy party!
We'll use all the collection types together.

```java
record Guest(
    String name, 
    String favoriteDrink, 
    ValueSet<String> allergies
) implements HasId<String> {
    @Override public String id() { return name; } // Name is our unique identifier
}

record PartyPlan(
    String theme,
    Tuple<Guest> guests,
    Association<String, Integer> drinkStock, // drink name -> quantity
    ValueSet<String> decorations
) {}

// Create our initial party plan
Var<PartyPlan> partyPlan = Var.of(
    new PartyPlan(
        "Enchanted Forest",
        Tuple.of(
            new Guest("Gandalf", "Elven Wine", ValueSet.of(String.class, "Gluten")),
            new Guest("Aragorn", "Ale", ValueSet.of(String.class)),
            new Guest("Legolas", "Spring Water", ValueSet.of(String.class, "Dairy"))
        ),
        Association.of(String.class, Integer.class)
            .put("Elven Wine", 5)
            .put("Ale", 10)
            .put("Spring Water", 3),
        ValueSet.of(String.class, "Fairy Lights", "Mushroom Seats", "Glowing Flowers")
    )
);

// Create lenses for different aspects of the party
Var<Tuple<Guest>> guestsLens = partyPlan.zoomTo(PartyPlan::guests, PartyPlan::withGuests);
Var<Association<String, Integer>> drinksLens = partyPlan.zoomTo(PartyPlan::drinkStock, PartyPlan::withDrinkStock);
Var<ValueSet<String>> decorationsLens = partyPlan.zoomTo(PartyPlan::decorations, PartyPlan::withDecorations);

// Add a new guest reactively
guestsLens.update(guests -> guests.add(
    new Guest("Gimli", "Dwarven Stout", ValueSet.of(String.class))
));

// Update drink stock when Gimli arrives (he's thirsty!)
drinksLens.update(stock -> stock.put("Dwarven Stout", 20));

// Remove problematic decorations
decorationsLens.update(decorations -> decorations.remove("Mushroom Seats"));
```

## Advanced Patterns

Now that you have the basics down, let's explore some advanced patterns
that leverage the full power of Sprouts.

### Custom Lens Logic

For more complex scenarios, you can implement custom lenses 
to handle derived or calculated state. Simply implement the `Lens<S, T>` interface
and add your logic in the `getter` and `wither` methods. Of course, you can also
add some custom state if needed.

Here's an example of a custom lens that manages a 
character's health system in a hypothetical game:
```java
/**
 * A custom lens that manages health with clamping and regeneration logic
 */
class HealthLens implements Lens<GameCharacter, Integer> {
    private final int maxHealth;
    private final int regenerationRate;

    public HealthLens(int maxHealth, int regenerationRate) {
        this.maxHealth = maxHealth;
        this.regenerationRate = regenerationRate;
    }

    @Override
    public Integer getter(GameCharacter character) throws Exception {
        // Return current health, applying regeneration if character is alive
        int currentHealth = character.health();
        if (currentHealth > 0 && currentHealth < maxHealth) {
            return Math.min(currentHealth + regenerationRate, maxHealth);
        }
        return currentHealth;
    }

    @Override
    public GameCharacter wither(GameCharacter character, Integer newHealth) throws Exception {
        // Clamp health between 0 and maxHealth, and handle death state
        int clampedHealth = Math.max(0, Math.min(newHealth, maxHealth));

        // If health reaches 0, mark character as dead
        GameCharacter updated = character.withHealth(clampedHealth);
        if (clampedHealth == 0) {
            updated = updated.withStatus(CharacterStatus.DEAD);
        }

        return updated;
    }
}

// Usage example:
record GameCharacter(String name, int health, CharacterStatus status) {
    public GameCharacter withHealth(int health) {
        return new GameCharacter(name, health, status);
    }
    public GameCharacter withStatus(CharacterStatus status) {
        return new GameCharacter(name, health, status);
    }
}

enum CharacterStatus { ALIVE, DEAD }

// Create a character with custom health management
Var<GameCharacter> hero = Var.of(new GameCharacter("Aria", 75, CharacterStatus.ALIVE));

// Apply the custom health lens
HealthLens healthLens = new HealthLens(100, 5); // Max 100 HP, regenerates 5 per get
Var<Integer> healthProperty = hero.zoomTo(healthLens);

// The lens automatically applies regeneration when accessed
System.out.println(healthProperty.get()); // Might output 80 instead of 75 due to regeneration

// Health is clamped and death is handled automatically
healthProperty.set(150); // Actually sets to 100 (max health)
healthProperty.set(-10); // Actually sets to 0 and marks character as DEAD
```
The lens automatically applies regeneration when health is accessed and 
ensures health values stay within valid bounds when updated, making 
the character's health system based on the custom logic defined in the lens.

### Combined Properties

Sometimes you need to derive state from multiple properties. 
Sprouts makes this easy with combined properties 
that automatically update when any of their dependencies change.

```java
// Create a view that combines multiple properties
Var<Double> price = Var.of(100.0);
Var<Double> taxRate = Var.of(0.19);

Viewable<Double> totalPrice = Viewable.of(price, taxRate, (p, tr) -> p * (1 + tr));

// totalPrice automatically updates when either price or taxRate changes
```

## Conclusion

Sprouts provides a robust foundation for building modern Java applications with:

- **Immutable data models** that are safe, fast and predictable
- **Property lenses** for fine-grained state management
- **Property views** for viewing and reacting to state changes
- **Automatic memory management** through weak-referenced views
- **Type-safe** operations throughout

The key to success with Sprouts is embracing immutability and letting the library 
handle the complexity of state propagation and memory management. Start with 
simple records and `Var` properties, then gradually incorporate lenses as 
your data structures become more complex.

> 🌟 **Pro Tip**: Combine Sprouts with Java's `record` types, 
> sealed interfaces together with pattern matching 
> for the most elegant and maintainable code architecture.

---

*Sprouts: Where your application state grows safely and predictably.* 🌱