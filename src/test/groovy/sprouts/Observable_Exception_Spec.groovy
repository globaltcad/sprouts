package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.lang.reflect.UndeclaredThrowableException
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiFunction
import java.util.function.Function

@Title("Observable Exception Handling and Interrupt Behavior")
@Narrative('''
    The Sprouts observable types (Viewable, Var, Observable, Event) are designed to handle
    exceptions gracefully in user-supplied lambdas while maintaining proper control flow.
    
    Key behaviors:
    1. Regular exceptions in observers/actions are caught and logged without breaking the control flow
    2. InterruptedException is re-thrown to preserve Java's concurrency model  
    3. Serious Error types (OutOfMemoryError, StackOverflowError) are not caught
    4. Thread interrupt status is properly managed
    
    These tests verify that all observable types maintain consistent exception handling
    behavior across different scenarios.
''')
@Subject([Viewable, Var, Observable, Event])
class Observable_Exception_Spec extends Specification
{
    def 'InterruptedException in Viewable onChange action is re-thrown and preserves thread interrupt status'() {
        reportInfo """
            When a Viewable's onChange action throws InterruptedException, it should be re-thrown
            to comply with Java's concurrency model. The thread's interrupt status should be preserved.
            This ensures that Viewable observers don't silently swallow thread interrupts.
        """
        given: 'A Var property and its Viewable'
            def var = Var.of("initial")
            def viewable = var.view()

        and: 'An action that throws InterruptedException'
            def interruptingAction = { ValDelegate<String> delegate ->
                throw new InterruptedException("Viewable action interrupted")
            } as Action<ValDelegate<String>>

        and: 'Clear initial interrupt status'
            Thread.interrupted()

        when: 'We register the interrupting action'
            viewable.onChange(From.VIEW_MODEL, interruptingAction)

        and: 'Trigger a change that would invoke the action'
            var.set("new value")

        then: 'InterruptedException is re-thrown'
            thrown(InterruptedException)

        and: 'Thread interrupt status is set'
            Thread.currentThread().interrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'Regular exceptions in Viewable onChange action are caught and logged without breaking flow'() {
        reportInfo """
            Regular exceptions in Viewable onChange actions should be caught and logged
            without breaking the control flow. This prevents one faulty observer from
            disrupting the entire property change notification system.
        """
        given: 'A Var property and its Viewable'
            def var = Var.of("initial")
            def viewable = var.view()

        and: 'A counter to track successful executions'
            def successCounter = new AtomicInteger(0)
            def exceptionCounter = new AtomicInteger(0)

        and: 'Actions - one that throws exception, one that succeeds'
            def exceptionAction = { ValDelegate<String> delegate ->
                exceptionCounter.incrementAndGet()
                throw new IllegalStateException("Regular exception in action")
            } as Action<ValDelegate<String>>

            def successAction = { ValDelegate<String> delegate ->
                successCounter.incrementAndGet()
            } as Action<ValDelegate<String>>

        when: 'We register both actions'
            viewable.onChange(From.VIEW_MODEL, exceptionAction)
            viewable.onChange(From.VIEW_MODEL, successAction)

        and: 'Trigger a change'
            var.set("new value")

        then: 'No exception is thrown to the caller'
            noExceptionThrown()

        and: 'Both actions were executed despite one throwing exception'
            exceptionCounter.get() == 1
            successCounter.get() == 1

        and: 'Property value was updated successfully'
            var.get() == "new value"
    }

    def 'InterruptedException in Observable subscribe is re-thrown'() {
        reportInfo """
            When an Observer registered through Observable.subscribe throws InterruptedException,
            it should be re-thrown to maintain consistent behavior across all observable types.
        """
        given: 'An Event and its Observable'
            def event = Event.create()
            def observable = event.observable()

        and: 'An observer that throws InterruptedException'
            def interruptingObserver = {
                throw new InterruptedException("Observable observer interrupted")
            } as Observer

        and: 'Clear interrupt status'
            Thread.interrupted()

        when: 'We subscribe the interrupting observer'
            observable.subscribe(interruptingObserver)

        and: 'Fire the event'
            event.fire()

        then: 'InterruptedException is re-thrown'
            thrown(InterruptedException)

        and: 'Thread interrupt status is set'
            Thread.currentThread().interrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'Regular exceptions in Observable observers are caught and logged'() {
        reportInfo """
            Regular exceptions in Observable observers should be caught and logged
            without preventing other observers from being notified.
            This ensures robust event handling in the face of faulty observer code.
        """
        given: 'An Event and its Observable'
            def event = Event.create()
            def observable = event.observable()

        and: 'Counters to track observer executions'
            def exceptionObserverCount = new AtomicInteger(0)
            def successObserverCount = new AtomicInteger(0)

        and: 'Observers - one that throws, one that succeeds'
            def exceptionObserver = {
                exceptionObserverCount.incrementAndGet()
                throw new IllegalArgumentException("Observer exception")
            } as Observer

            def successObserver = {
                successObserverCount.incrementAndGet()
            } as Observer

        when: 'We subscribe both observers'
            observable.subscribe(exceptionObserver)
            observable.subscribe(successObserver)

        and: 'Fire the event'
            event.fire()

        then: 'No exception is thrown to the caller'
            noExceptionThrown()

        and: 'Both observers were executed'
            exceptionObserverCount.get() == 1
            successObserverCount.get() == 1
    }

    def 'InterruptedException in Event fire execution bubbles up!'() {
        reportInfo """
            When using a custom Event executor, if the fire execution encounters
            an InterruptedException, it should be re-thrown to preserve the
            Java concurrency model.
        """
        given: 'An Event with a custom executor that throws InterruptedException'
            def interruptingExecutor = { Runnable runnable ->
                throw new InterruptedException("Event executor interrupted")
            } as Event.Executor

            def event = Event.using(interruptingExecutor)

        and: 'Clear interrupt status'
            Thread.interrupted()

        when: 'We fire the event'
            event.fire()

        then: 'InterruptedException is re-thrown'
            var ex = thrown(UndeclaredThrowableException)
            ex.cause instanceof InterruptedException
    }

    def 'Serious Error types propagate normally from observable actions'() {
        reportInfo """
            Serious Error subtypes like OutOfMemoryError and StackOverflowError
            should NOT be caught by observable exception handling. These represent
            severe platform-level problems that applications should not typically
            attempt to handle.
        """
        given: 'A Var and its Viewable'
            def var = Var.of("initial")
            def viewable = var.view()

        and: 'An action that throws OutOfMemoryError'
            def errorAction = { ValDelegate<String> delegate ->
                throw new OutOfMemoryError("Simulated memory error")
            } as Action<ValDelegate<String>>

        when: 'We register the error-throwing action'
            viewable.onChange(From.VIEW_MODEL, errorAction)

        and: 'Trigger a change'
            var.set("new value")

        then: 'OutOfMemoryError propagates unchanged'
            thrown(OutOfMemoryError)
    }

    def 'StackOverflowError propagates normally from observable actions'() {
        given: 'An Event and its Observable'
            def event = Event.create()
            def observable = event.observable()

        and: 'An observer that causes stack overflow'
            def stackOverflowObserver = {
                throw new StackOverflowError("Simulated stack overflow")
            } as Observer

        when: 'We subscribe the stack-overflowing observer'
            observable.subscribe(stackOverflowObserver)

        and: 'Fire the event'
            event.fire()

        then: 'StackOverflowError propagates unchanged'
            thrown(StackOverflowError)
    }

    def 'Mixed interrupt and regular exception scenarios in complex observable chains'() {
        reportInfo """
            This test verifies that interrupt handling takes precedence over
            regular exception handling in complex scenarios with multiple
            observers and different exception types.
        """
        given: 'A Var property with multiple Viewables'
            def var = Var.of("start")
            def mainView = var.view()
            def secondaryView = var.view()

        and: 'Various actions with different behaviors'
            def regularExceptionAction = { ValDelegate<String> delegate ->
                throw new IllegalStateException("Regular problem")
            } as Action<ValDelegate<String>>

            def successAction = { ValDelegate<String> delegate ->
                // This should still execute despite other exceptions
            } as Action<ValDelegate<String>>

            def interruptingAction = { ValDelegate<String> delegate ->
                throw new InterruptedException("Thread interruption")
            } as Action<ValDelegate<String>>

        and: 'Clear interrupt status'
            Thread.interrupted()

        when: 'We register actions on different viewables'
            mainView.onChange(From.VIEW_MODEL, regularExceptionAction)
            mainView.onChange(From.VIEW_MODEL, successAction)
            secondaryView.onChange(From.VIEW_MODEL, interruptingAction)

        and: 'Trigger a change'
            var.set("updated")

        then: 'InterruptedException takes precedence and is re-thrown'
            thrown(InterruptedException)

        and: 'Thread interrupt status is set'
            Thread.currentThread().interrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'Thread interrupt status is preserved across multiple observable operations'() {
        reportInfo """
            The thread's interrupt status should be properly managed and preserved
            across multiple observable operations, even when regular exceptions occur.
        """
        given: 'We start with clear interrupt status'
            Thread.interrupted()

        and: 'Multiple observable components'
            def event = Event.create()
            def observable = event.observable()
            def var = Var.of("test")
            def viewable = var.view()

        when: 'We perform normal operations with regular exceptions'
            observable.subscribe({ throw new RuntimeException("Regular exception") } as Observer)
            viewable.onChange(From.VIEW_MODEL, { throw new IllegalArgumentException("Another regular exception") } as Action)

            event.fire()
            var.set("new value")

        then: 'Thread interrupt status remains clear despite regular exceptions'
            !Thread.currentThread().isInterrupted()

        when: 'We manually interrupt the thread'
            Thread.currentThread().interrupt()

        and: 'Perform more operations with regular exceptions'
            def var2 = Var.of("another")
            var2.view().onChange(From.VIEW_MODEL, { throw new IllegalStateException("Yet another exception") } as Action)
            var2.set("changed")

        then: 'Thread interrupt status is preserved despite regular exceptions'
            Thread.currentThread().isInterrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'Exception handling in Viewable composite properties'() {
        reportInfo """
            Viewable composite properties (created with Viewable.of/Viewable.ofNullable)
            should maintain the same exception handling behavior when their
            combiner functions throw exceptions.
        """
        given: 'Two source properties'
            def first = Var.of("hello")
            def second = Var.of("world")

        and: 'A combiner that throws regular exception'
            def exceptionCombiner = { String a, String b ->
                throw new IllegalStateException("Combiner failed")
            } as BiFunction<String, String, String>

        when: 'We create a composite Viewable with exception-throwing combiner'
            def composite = Viewable.ofNullable(first, second, exceptionCombiner)

        then: 'The composite is created successfully, but empty initially'
            composite.isEmpty()

        when: 'We change one source property'
            first.set("changed")

        then: 'Regular exception in combiner is caught and handled appropriately'
            noExceptionThrown()
            // The specific handling depends on implementation (might keep old value, set to null, etc.)
    }

    def 'Unsubscribe prevents further exception handling'() {
        reportInfo """
            Unsubscribing observers should prevent them from being invoked
            in future notifications, which also means their exceptions won't occur.
        """
        given: 'An Event and its Observable'
            def event = Event.create()
            def observable = event.observable()

        and: 'An observer that throws exceptions and a counter'
            def exceptionCount = new AtomicInteger(0)
            def exceptionObserver = {
                exceptionCount.incrementAndGet()
                throw new RuntimeException("Observer exception")
            } as Observer

        when: 'We subscribe the exception-throwing observer'
            observable.subscribe(exceptionObserver)

        and: 'Fire the event multiple times'
            event.fire()
            event.fire()

        then: 'Exception count reflects both firings'
            exceptionCount.get() == 2

        when: 'We unsubscribe the observer'
            observable.unsubscribe(exceptionObserver)

        and: 'Fire the event again'
            event.fire()

        then: 'Exception count remains the same (observer was not called)'
            exceptionCount.get() == 2
    }

    def 'Multiple channels maintain independent exception handling'() {
        reportInfo """
            When using different channels (From.VIEW_MODEL, From.VIEW, etc.),
            exception handling should work independently per channel.
            An exception in one channel's observers shouldn't affect other channels.
        """
        given: 'A Var property and its Viewable'
            def var = Var.of("initial")
            def viewable = var.view()

        and: 'Counters for different channels'
            def viewModelExceptions = new AtomicInteger(0)
            def viewExceptions = new AtomicInteger(0)

        and: 'Exception-throwing actions for different channels'
            def viewModelExceptionAction = { ValDelegate<String> delegate ->
                viewModelExceptions.incrementAndGet()
                throw new IllegalStateException("VIEW_MODEL channel exception")
            } as Action<ValDelegate<String>>

            def viewExceptionAction = { ValDelegate<String> delegate ->
                viewExceptions.incrementAndGet()
                throw new IllegalArgumentException("VIEW channel exception")
            } as Action<ValDelegate<String>>

        when: 'We register channel-specific exception actions'
            viewable.onChange(From.VIEW_MODEL, viewModelExceptionAction)
            viewable.onChange(From.VIEW, viewExceptionAction)

        and: 'Trigger changes on different channels'
            var.set(From.VIEW_MODEL, "view model change")
            var.set(From.VIEW, "view change")

        then: 'No exceptions are thrown to caller'
            noExceptionThrown()

        and: 'Both channel actions were executed'
            viewModelExceptions.get() == 1
            viewExceptions.get() == 1

        and: 'Property was updated successfully for both channels'
            var.get() == "view change"
    }

    def 'Exception handling in Var update methods'() {
        reportInfo """
            Var update methods (update and updateNullable) should propagate exceptions
            normally since they are not observer callbacks but direct mutations.
            These methods don't catch exceptions because they are part of the
            direct property modification API.
        """
        given: 'A Var property'
            def var = Var.of("initial")

        and: 'An update function that throws exception'
            def exceptionUpdate = { String current ->
                throw new IllegalStateException("Update failed")
            } as Function<String, String>

        when: 'We call update with exception-throwing function'
            var.update(exceptionUpdate)

        then: 'Exception propagates normally (not caught)'
            thrown(IllegalStateException)

        and: 'Property value remains unchanged'
            var.get() == "initial"
    }

    def 'Complex scenario: Nested observables with mixed exceptions'() {
        reportInfo """
            This test verifies exception handling behavior in complex scenarios
            with nested observables and multiple levels of observation.
        """
        given: 'Multiple properties and events'
            def sourceVar = Var.of("source")
            def derivedView = sourceVar.view()
            def event = Event.create()
            def eventObservable = event.observable()

        and: 'Nested observation structure'
            def outerExceptionCount = new AtomicInteger(0)
            def innerExceptionCount = new AtomicInteger(0)
            def successCount = new AtomicInteger(0)

        and: 'Complex observers that trigger other observables'
            def outerObserver = {
                outerExceptionCount.incrementAndGet()
                // This observer triggers the event when called
                event.fire()
                throw new RuntimeException("Outer observer exception")
            } as Observer

            def innerObserver = {
                innerExceptionCount.incrementAndGet()
                throw new IllegalStateException("Inner observer exception")
            } as Observer

            def successObserver = {
                successCount.incrementAndGet()
            } as Observer

        when: 'We set up the nested observation'
            eventObservable.subscribe(innerObserver)
            eventObservable.subscribe(successObserver)
            derivedView.onChange(From.VIEW_MODEL, { outerObserver.invoke() } as Action)

        and: 'Trigger the initial change'
            sourceVar.set("trigger")

        then: 'No exception reaches the caller (all caught and logged)'
            noExceptionThrown()

        and: 'All observers were executed despite exceptions'
            outerExceptionCount.get() == 1
            innerExceptionCount.get() == 1
            successCount.get() == 1
    }

}