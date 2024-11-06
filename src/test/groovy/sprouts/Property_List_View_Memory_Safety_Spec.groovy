package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.lang.ref.WeakReference
import java.time.chrono.JapaneseEra

@Title('Property List View Memory Safety')
@Narrative('''

    Both the read only `Vals` and the mutable `Vars` are designed to be
    used as part of a view model in a Model-View-ViewModel (MVVM) architecture
    or Model-View-Intent (MVI) architecture.
    As a consequence, they need to be observable in some way.
    This is done by registering change listeners on 
    their so called "views", which are weakly referenced
    live views of the original property lists which get updated
    automatically when the original property changes.
    You can then subscribe to these views to get notified
    of changes to the original property.
    
    This is especially useful when you want to observe a property list
    of one type as a property list of another type, or when you want to
    observe a property lists with some transformation applied to them.
    
    This specification shows how to create views from both nullable and non-nullable property lists,
    and it demonstrates how they may be garbage collected when they are no longer referenced.

''')
@Subject([Vals, Vars, Viewables, Viewable])
class Property_List_View_Memory_Safety_Spec extends Specification
{
    def 'The view of a property list will be garbage collected if no longer referenced.'()
    {
        reportInfo """
            The property list view is a live view on the source property list,
            which gets updated whenever the source list changes.
            But the source list does not hold a reference to the view,
            so if the view is no longer referenced, it will be garbage collected.
            This also includes all of the change listeners that were attached to it.
            So they will not be updated anymore.
        """
        given : 'A property list of 3 japanese eras.'
            Vars<JapaneseEra> eras = Vars.of(JapaneseEra.HEISEI,JapaneseEra.SHOWA,JapaneseEra.TAISHO)
        and : 'A view on the names of the japanese eras and a weak reference to the view.'
            Viewables<String> names = eras.view("null", "error", JapaneseEra::toString)
            WeakReference<Viewables<String>> namesRef = new WeakReference<>(names)
        and : 'A list to record the changes and a listener to record the changes.'
            var viewTrace = []
            names.onChange({ viewTrace << it.vals().toList() })
        expect : 'The trace is empty initially.'
            viewTrace.isEmpty()

        when : 'We remove an era from the source property list.'
            eras.removeAt(1)
        then : 'The view is updated and the trace records the change.'
            viewTrace == [["Heisei","Taisho"]]

        when : 'We remove the reference to the view, clear the trace and wait for garbage collection.'
            names = null
            viewTrace.clear()
            waitForGarbageCollection()
        then : 'The view is garbage collected!'
            namesRef.get() == null

        when : 'We try to trigger a change in the source property list.'
            eras.add(JapaneseEra.REIWA)
        then : 'The view is not updated and the trace is empty.'
            viewTrace == []
    }

    def 'The views of individual properties of a property list view can be garbage collected.'()
    {
        reportInfo """
            You can create individual property views from the properties of a property list view.
            These individual property views are also weakly referenced, so they can be garbage collected
            if they are no longer referenced.
        """
        given : 'A property list of 4 important names.'
            Vars<String> people = Vars.of("Gary Yourofsky", "Joey Carbstrong", "Earthling Ed")
        and : 'Three property views, one for each name.'
            Viewable<Integer> gary = people.at(0).viewAsInt( name -> name.length() )
            Viewable<Integer> joey = people.at(1).viewAsInt( name -> name.length() )
            Viewable<Integer> ed = people.at(2).viewAsInt( name -> name.length() )
        and : 'Weak references to the property views.'
            WeakReference<Viewable<Integer>> garyRef = new WeakReference<>(gary)
            WeakReference<Viewable<Integer>> joeyRef = new WeakReference<>(joey)
            WeakReference<Viewable<Integer>> edRef = new WeakReference<>(ed)
        and : 'A list to record the changes and a listener to record the changes.'
            var viewTrace = []
            gary.onChange(From.ALL, { viewTrace << it.get() })
            joey.onChange(From.ALL, { viewTrace << it.get() })
            ed.onChange(From.ALL, { viewTrace << it.get() })
        expect : 'The trace is empty initially.'
            viewTrace.isEmpty()

        when : 'We mutate all the names.'
            people.at(0).set("Richard Burgess")
            people.at(1).set("Joe Armstrong")
            people.at(2).set("Edward Wren")
        then : 'The views are updated and the trace records the changes.'
            viewTrace == [15, 13, 11]

        when : 'We remove the references to the views, clear the trace and wait for garbage collection.'
            gary = null
            joey = null
            ed = null
            viewTrace.clear()
            waitForGarbageCollection()
        then : 'The views are garbage collected!'
            garyRef.get() == null
            joeyRef.get() == null
            edRef.get() == null

        when : 'We try to trigger a change in the source property list.'
            people.at(0).set("x")
            people.at(1).set("y")
            people.at(2).set("z")
        then : 'The views are not updated and the trace is still empty.'
            viewTrace == []
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
