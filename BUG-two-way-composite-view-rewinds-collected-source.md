# Bug: a two way composite view never looks at the source property which changed

*Two symptoms, one root cause: the view can rewind a garbage collected source to an older
item, and it can end up permanently disagreeing with its sources.*

**Component:** `sprouts.impl.PropertyView` — the composite views created by
`Viewable.of(Val, Val, BiFunction)` and its three sibling factory methods.

**Status:** open, with the specifications already written and committed as `@PendingFeature`,
so the build stays green until the fix lands and turns red the moment it starts working:

| specification | features | what they cover |
|---|---|---|
| `Property_View_Memory_Safety_Spec` | 4 pending | the garbage collection symptom |
| `Property_View_Re_Entrancy_Spec` | 1 pending, 3 passing | the disagreement symptom, plus the behaviour that must be preserved |

The same defect was found and fixed for the builder based composite views and for all three
lens cores, see *Background* below. `PropertyView` was deliberately left out of that change,
because curing it touches eight listener sites in core code.

---

## The bug in one sentence

When a plain source property of a composite view is garbage collected, the view keeps
contributing the item that property held **when the view was created**, instead of the item
it held **when the view last saw it** — so every change made in between is silently undone.

## Why this matters

This is not a view going stale, and it is not a view breaking. It is a view *travelling
backwards in time*. A value that was already computed, propagated to listeners and rendered
on screen reverts to an older value, and the only thing that triggers it is a garbage
collection — something that happens at an unpredictable moment and is invisible to the
application. A bug reported against this would be very hard to reproduce.

## Reproduction

```java
Var<String> a = Var.of("A");
Var<String> b = Var.of("B");
Val<String> c = Viewable.of(a, b, (x, y) -> x + y);

c.get();                  // "AB"  ✅

a.set("a");
c.get();                  // "aB"  ✅  the change was propagated and observed

a = null;                 // drop the last strong reference to the first source
System.gc();              // `a` is collected — this is intended, a view must not keep state alive

b.set("b");               // any change to the surviving source recomputes the view
c.get();                  // "Ab"  ❌  expected "ab"
```

| | value of `c` |
|---|---|
| actual, today | `"Ab"` — `a` reverted from `"a"` back to `"A"` |
| expected | `"ab"` — `a` contributes `"a"`, the newest item the view saw it holding |

## Root cause

A plain source property is held through a `TransientParentRef`, which keeps a weak reference
to it plus the item to fall back on once it is collected. Since commit `8c30234` that
fallback item is refreshed on every look at a live parent:

```java
// sprouts/impl/TransientParentRef.java
@Override
public V get() {
    @Nullable V current = _ref.get();
    if ( current == null )
        return (V) Property.ofNullable(false, (Class) _lastType, _lastItem);
    _lastItem = current.orElseNull();   // memorize while the parent is still alive
    return current;
}
```

`get()` is the only door: every consumer of a weakly referenced parent has to go through it
in order to derive its own item, which is what makes the refresh impossible to forget.

**`PropertyView` does not go through that door for the source that changed.** Its listeners
re-read the *other* source, but take the changed one straight from the change event:

```java
// sprouts/impl/PropertyView.java
BiConsumer<PropertyView<T>,ValDelegate<T>> firstListener = (innerResult, v) -> {
    Val<U> innerSecond = innerResult._getSource(1);                 // source 1 is read → memorized ✅
    T newItem = fullCombiner.apply(v.currentValue(), innerSecond);  // source 0 comes from the event → never memorized ❌
    ...
};
BiConsumer<PropertyView<T>,ValDelegate<U>> secondListener = (innerResult, v) -> {
    Val<T> innerFirst = innerResult._getSource(0);                  // source 0 is read → memorized ✅
    T newItem = fullCombiner.apply(innerFirst, v.currentValue());   // source 1 comes from the event → never memorized ❌
    ...
};
```

So a source's fallback item is only ever refreshed when *the other source* changes.

`PropertyView` is also **eager**: `orElseNull()` returns the stored `_currentItem` and never
consults its parents, so there is no read path that could memorize either. This is the
difference to `PropertyLens`, which is **lazy** and re-reads all of its sources on every
single read — which is exactly why the builder based composite views were fully cured by the
`TransientParentRef` change alone.

## Second symptom: the view can permanently disagree with its sources

The same root cause — *the view never looks at the source that changed* — has a second
consequence which has nothing to do with garbage collection, and which is arguably worse
because it needs no GC to trigger and leaves the view wrong forever.

Listeners on a property are called in the order they were registered. So a listener which was
registered on a source **before** the view was created runs first, and if it changes that
source again, the view is told about the two changes in the wrong order:

```groovy
Var<String> a = Var.of("A")
Var<String> b = Var.of("B")

// registered FIRST, so it runs before the view's own listener:
Viewable.cast(a).onChange(From.ALL, { if ( a.get() == "x" ) a.set("y") })

Val<String> c = Viewable.of(a, b, (x, y) -> x + y)

a.set("x")
```

What happens, step by step:

1. `a.set("x")` notifies the listeners of `a`, in order.
2. The first listener sets `a` to `"y"`, which re-enters the notification of `a`.
3. Inside that, the view's listener is called with the event for `"y"` and computes `"yB"`.
4. The re-entrant notification unwinds, and the view's listener is now called with the *outer*,
   older event for `"x"` — and overwrites `"yB"` with `"xB"`.

Measured:

| | trace of what the view reported | `a` | `b` | final `c` |
|---|---|---|---|---|
| `Viewable.of(a, b, combiner)` | `[yB, xB]` | `"y"` | `"B"` | `"xB"` ❌ |
| builder based `Viewable.of(seed, cfg)` | `[yB]` | `"y"` | `"B"` | `Merged[a=y, b=B]` ✅ |

The two way view ends up holding `"xB"` while its sources hold `"y"` and `"B"`. It also
reported two events, the second one going *backwards*. The builder based composite view gets
this right for free, because it re-reads the current item of every source on every
recomputation instead of trusting what an event tells it.

This is pinned by `'A composite view agrees with its properties even when one of them changes twice in a row.'`
in `Property_View_Re_Entrancy_Spec`, currently `@PendingFeature`.

### Affected sites

Both listeners in all four private factory methods of `PropertyView`, **eight in total**:

| factory | listeners |
|---|---|
| `of(Val, Val, BiFunction)` | `firstListener`, `secondListener` |
| `ofNullable(Val, Val, BiFunction)` | `firstListener`, `secondListener` |
| `of(Class, Val, Val, BiFunction)` | two inline lambdas |
| `ofNullable(Class, Val, Val, BiFunction)` | two inline lambdas |

---

## Background: how this was fixed for the builder based composite views

Identical defect, same root cause, cured in `8c30234`. The specification that pins the fixed
behaviour is:

> `src/test/groovy/sprouts/Composite_View_Memory_Safety_Spec.groovy`

with these five features, each of which fails when the one line in `TransientParentRef.get()`
is removed:

1. `'A garbage collected property contributes its newest item, and not the one it was joined with.'`
2. `'A change is remembered even if nobody reads the composite view before the garbage collection.'`
3. `'Every combiner of a property joined several times gets its newest item after garbage collection.'`
4. `'A nullable property emptied before the garbage collection contributes `null` afterwards.'`
5. `'A composite view keeps its item after all of its joined properties were garbage collected.'`

The features below are the `PropertyView` counterparts of 1, 2, 4 and 5.

---

## Suggested fixes

> **The two symptoms are not equally easy to fix.** Option A below cures the garbage
> collection symptom only; the disagreement symptom survives it, because that one is caused by
> the view *using* the item from the event rather than by it *failing to memorize* the item.
> Only Option B cures both.

### Option A — memorize explicitly (cures one of the two symptoms)

Add a second method to `ParentRef`:

```java
/**
 *  Records the supplied item as the newest known item of the parent property, so that it is
 *  what the stand-in carries once the parent is garbage collected. A parent which is held
 *  strongly has no such stand-in, so this does nothing for it.
 */
default void memorize( @Nullable Object item ) {}
```

implement it in `TransientParentRef` as `_lastItem = item;`, expose a small
`_memorizeSource(int index, @Nullable Object item)` helper on `PropertyView`, and call it at
the eight sites with `v.currentValue().orElseNull()`.

*Pro:* nothing about which items get combined changes, so no existing behaviour can shift.
*Con:* eight call sites which a future contributor has to remember, so the bug can come back.
And it leaves the second symptom in place: the view still combines the item from the event, so
an older event still overwrites a newer one.

### Option B — read both sources in both listeners (recommended)

Replace the item taken from the event with a read of the source:

```java
T newItem = fullCombiner.apply(innerResult._getSource(0), innerResult._getSource(1));
```

*Pro:* both fallback items are refreshed as a side effect of doing the ordinary thing, the two
listeners collapse into one shared body, and `PropertyView` gains the same "always recompute
from the current items of all sources" property that makes the builder based composite views
free of intermediate items.
*Con:* under re-entrancy — an observer which changes a source from inside a change
notification — this combines the *newest* item rather than the item the event was about.
That is the very change which cures the second symptom, so it is wanted here, but it is a real
semantic change. The two re-entrancy shapes which must survive it are already pinned and green
in `Property_View_Re_Entrancy_Spec`:

* `'An observer of a composite view may change the property which triggered it.'`
* `'An observer of a composite view may change the other property of that view.'`

so the risk is measurable rather than hypothetical: if either of those flips, the fix changed
something it should not have. Keep the existing `if (innerSecond == null) return;` guard.

---

## The specifications, already written

They are committed and runnable, marked `@PendingFeature` so the build stays green until the
fix lands. Spock fails the build as soon as a pending feature starts passing, so none of them
can be forgotten:

**`src/test/groovy/sprouts/Property_View_Memory_Safety_Spec.groovy`** — the garbage collection
symptom, four pending features:

1. `'A composite view of two properties uses the newest item of a garbage collected source.'`
2. `'A change is remembered even if nobody reads the composite view before the garbage collection.'`
3. `` 'A nullable source emptied before the garbage collection contributes `null` afterwards.' ``
4. `'A composite view built with an explicit type has the same guarantee.'`

**`src/test/groovy/sprouts/Property_View_Re_Entrancy_Spec.groovy`** — the disagreement symptom
and the behaviour around it:

| feature | state | role |
|---|---|---|
| `'An observer of a composite view may change the property which triggered it.'` | green | must keep working |
| `'An observer of a composite view may change the other property of that view.'` | green | must keep working |
| `'A composite view agrees with its properties even when one of them changes twice in a row.'` | pending | the bug |
| `'A composite view built with the composite builder agrees with its properties in the same situation.'` | green | shows the target behaviour is reachable |

The last one is worth keeping even after the fix: it is the same scenario built with the
composite builder, and it passes today. It documents what correct looks like, and it is the
reason we know the desired behaviour is not wishful thinking.

### Two existing features which should be strengthened

`Property_View_Memory_Safety_Spec` already claims this guarantee twice:

- `'A composite view of 2 properties will not break if the first observed property is garbage collected.'`
- `'A composite view of 2 properties will not break if the second observed property is garbage collected.'`

> *"the composite property `c` is still updated based on the last known value of `a`"*

Both **pass vacuously**: the collected property is never changed before it is collected, so
the item from creation time happens to equal the newest item, and the assertion cannot tell
the two apart. They should be extended with a `set(..)` before the property is dropped.

---

## A trap to watch out for while writing these specs

Spock's power assertion value recorder holds on to the values of the **last evaluated
condition**. `waitForGarbageCollection()` runs in a `when:` block, which is *before* the next
condition resets that recorder — so a condition like

```groovy
then : 'The view still works.'
    c.get() == "aB"          // records `c` AND the item
when : 'We drop the property and collect it.'
    a = null
    waitForGarbageCollection()
```

can pin the very object the next block expects to be collected, and the spec then fails for a
reason that has nothing to do with the code under test.

The specs in this repository work around it in two ways, both of which are worth copying:

* wrap single references in a list and assert through a closure, so the referent itself is
  never recorded: `[aRef].every( it -> it.get() != null )`
* hoist reads into a local inside the `when:` block, so the last condition before the garbage
  collection only records an inert value: `var merged = c.get()` and then `merged == "ab"`

Three of the ten features in `Composite_View_Memory_Safety_Spec` failed for exactly this
reason before the workaround was applied.
