package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

@Title('Composite Property Views')
@Narrative('''

    A composite property view is an immutable property that is a live view of two other properties.
    It is especially useful when you want to observe 2 properties merged into one maybe with some
    sort of transformation applied to them.
    
''')
class Composite_Properties_Spec extends Specification
{
    def 'A composite property view is a live view of 2 other properties.'()
    {
        reportInfo """
            A composite property view is an immutable property that is a live view of 2 other properties.
            whose items are merged into one using a simple lambda function that takes the 2 items as arguments
            and returns the merged item. The function is called every time one of the 2 properties changes, 
            and the view is updated with the result of the function.
            
            It is especially useful when you want to observe 2 properties merged into one maybe with some
            sort of transformation applied to them.
            
            A classic example is to merge a forename and a surname into a full name.
        """
        given : 'We have 2properties modelling a forename and a surname of a person.'
            Var<String> forename = Var.of("John")
            Var<String> surname = Var.of("Doe")
        and : 'Now we create a view of the 2 properties that represents the full name.'
            Val<String> fullName = Val.viewOf(forename, surname, (f, s) -> f + " " + s)
        expect : 'Initially, the view has the expected value.'
            fullName.get() == "John Doe"
        when : 'We change the value of the properties.'
            forename.set("Jane")
            surname.set("Smith")
        then : 'The full name view has the new item which is the first name and the last name merged.'
            fullName.get() == "Jane Smith"
    }

    def 'A composite property view created using the `Val.viewOf(..)` does not update when receiving `null` items'()
    {
        reportInfo """
            A composite property view created using the `Val.viewOf(..)` does not allow `null` items.
            This does not mean that the properties it is composed of cannot be nullable, it just means
            that the items produced by the combiner function cannot be `null`.
        """
        given : 'We have 2 properties modelling the address of a person.'
            Var<String> street = Var.of("Kingsway")
            Var<Integer> number = Var.of(123)
        and : 'Now we create a view of the 2 properties that represents the full address.'
            Val<String> fullAddress = Val.viewOf(street, number, (s, n) -> s.isEmpty() ? null : s + " " + n)
        expect : 'The view does not allow null items.'
            !fullAddress.allowsNull()
        when : 'We turn the street into an empty string to cause the view to produce a `null` item.'
            street.set("")
        then: 'The view will retain the old value.'
            fullAddress.get() == "Kingsway 123"
    }

    def 'A non nullable composite property view may be created from two nullable properties.'()
    {
        reportInfo """
            Although a composite property view created using the `Val.viewOf(..)` does permit `null` items
            itself, it may be created from two nullable properties, as long as the combiner function
            does not produce `null` items.
        """
        given : """
            We have 2 properties modelling the weight and height of a person.
            These properties are nullable for this example.
        """
            Var<Double> weight = Var.ofNullable(Double, 80d)
            Var<Double> height = Var.ofNullable(Double, 1.8d)
        and : 'A view of the 2 properties that represents the BMI.'
            Val<Double> bmi = Val.viewOf(weight, height, (w, h) -> w / (h * h))
        expect : 'The view does not allow `null` items.'
            !bmi.allowsNull()
        and : 'The view has the expected value.'
            bmi.get() == 24.691358024691358d
        when : 'We set the weight to `null`.'
            weight.set(null)
        then: 'The view will retain the old value.'
            bmi.get() == 24.691358024691358d
        when : 'We create a new view with the 2 properties and the same combiner.'
            Val.viewOf(weight, height, (w, h) -> w / (h * h))
        then : """
            Note that one of these properties is empty, which means that the combiner function will throw an exception
            when invoked with their items.
            An exception in the combiner function on the first call will throw a `NullPointerException`.
        """
            thrown(NullPointerException)
    }

    def 'A composite property view created using the `Val.viewOfNullable(..)` allows `null`.'()
    {
        reportInfo """
            A composite property view created using the `Val.viewOfNullable(..)` allows `null` items.
            This means that the items produced by the combiner function can be `null`.
        """
        given : 'We have 2 properties modelling the address of a person.'
            Var<String> street = Var.of("Kingsway")
            Var<Integer> number = Var.of(123)
        and : 'Now we create a view of the 2 properties that represents the full address.'
            Val<String> fullAddress = Val.viewOfNullable(street, number.viewAsString(), (s, n) -> s.isEmpty() ? null : s + " " + n)
        expect : 'The view allows `null` items.'
            fullAddress.allowsNull()
        when : 'We turn the street into an empty string to cause the view to produce a `null` item.'
            street.set("")
        then : 'No exception is thrown.'
            noExceptionThrown()
        and : 'The view produces a `null` item.'
            fullAddress.orElseNull() == null
    }

    def 'Some non-nullable composite properties created using the `Val.viewOf(..)` method cannot deal with `null`.'()
    {
        reportInfo """
            Some non-nullable composite properties created using the `Lal.viewOf(..)` method cannot deal with potential
            `null` items produced by the combiner function, due to the type of the view
            not having a default value (which is the case for primitive types or strings).
        """
        given : 'We are using `Date` based properties.'
            Var<Date> date1 = Var.ofNullable(Date.class, new Date())
            Var<Date> date2 = Var.ofNullable(Date.class, new Date())
        and : 'A view of the 2 properties that represents the earliest date, or `null` if any of the dates is `null`.'
            Val<Date> earliestDate = Val.viewOf(date1, date2, (d1, d2) -> (d1==null||d2==null) ? null : d1.before(d2) ? d1 : d2)
        expect : 'The view tells us that it does not allow `null` items.'
            !earliestDate.allowsNull()
        and : 'It contains an initial value.'
            earliestDate.get() != null
        when : 'We set the first date to `null`.'
            date1.set(null)
        then : """
            You might expect the view to either produce a `null` item, or to throw an exception.
            But, you have to remember that a view tries to be resilient to exceptions, which 
            includes null-pointer exceptions. 
            
            So what this means in practice is that in this case, the view will
            log a warning and keep the last value it had.
        """
            noExceptionThrown()
        and : 'The view still contains the initial value.'
            earliestDate.get() != null
        when : """
            We take a look at a more unfortunate scenario where at least one of the dates is `null` initially,
            then this whole situation is not going to work out without an exception being thrown
            at you.
        """
            date1 = Var.ofNull(Date)
            date2 = Var.of(new Date())
            earliestDate = Val.viewOf(date1, date2, (d1, d2) -> (d1==null) ? null : d1.before(d2) ? d1 : d2)
        then : 'An exception is thrown.'
            thrown(NullPointerException)
    }
}
