/**
 *  This is the main package for the Sprouts property collections library
 *  designed to provide all necessary tools to implement common architectural
 *  design patterns like MVVM, MVI, MVL, based on data oriented programming
 *  principles (immutability, value objects, lenses, circular data flow, etc.). <br>
 *  <br>
 *  It was designed in conjunction with the
 *  <a href="https://github.com/globaltcad/swing-tree">SwingTree GUI Framework</a>,
 *  which has native support for binding to the Sprouts properties. <br>
 *  <p>
 *  We recommended using Sprouts as a tool for bridging the gap between
 *  place oriented programming and data oriented programming,
 *  which is especially useful in GUI programming, where data oriented / functional
 *  programming principles are hard to apply due to the inherently place oriented
 *  nature of GUI components. <br>
 *  Sprouts allows you to create lens based properties that make value object based
 *  view models interoperate with your GUI components or other kinds of views. <br>
 *  This is best achieved using the MVI (Model-View-Intent) and MVL (Model-View-Lens) patterns. <br>
 *  <p>
 *  Here an example demonstrating how to create
 *  a set of bindable properties from a basic record based
 *  modelling scenario: <br>
 *  <b>View Models:</b>
 *  <pre>{@code
 *    public record Address(
 *        String street, String city, int postalCode
 *    ) {
 *        public Address withStreet(String street) {
 *            return new Address(street, city, postalCode);
 *        }
 *        public Address withCity(String city) {
 *            return new Address(street, city, postalCode);
 *        }
 *        public Address withPostalCode(int postalCode) {
 *            return new Address(street, city, postalCode);
 *        }
 *    }
 *  }</pre>
 *  <pre>{@code
 *    public record Person(
 *        String forename, String surname, Address address
 *    ) {
 *        public Person withForename(String forename) {
 *            return new Person(forename, surname, address);
 *        }
 *        public Person withSurname(String surname) {
 *            return new Person(forename, surname, address);
 *        }
 *        public Person withAddress(Address address) {
 *            return new Person(forename, surname, address);
 *        }
 *    }
 *  }</pre>
 *  <b>View:</b>
 *  <pre>{@code
 *    public final class PersonView {
 *
 *        public static void main(String[] args) {
 *            Person person = new Person("John", "Doe", new Address("Main Street", "Springfield", 12345));
 *            Var<Person> applicationState = Var.of(person);
 *            var view = new PersonView(applicationState);
 *            // show view
 *        }
 *
 *        public PersonView(Var<Person> vm) {
 *            Var<String> forename = vm.zoomTo(Person::forename, Person::withForename);
 *            Var<String> surname = vm.zoomTo(Person::surname, Person::withSurname);
 *            Val<String> fullName = vm.viewAsString( person -> person.forename() + " " + person.surname() );
 *            Var<Address> address = vm.zoomTo(Person::address, Person::withAddress);
 *            Var<String> street = address.zoomTo(Address::street, Address::withStreet);
 *            Var<String> city = address.zoomTo(Address::city, Address::withCity);
 *            Var<Integer> postalCode = address.zoomTo(Address::postalCode, Address::withPostalCode);
 *            // Use mutable properties in the views GUI...
 *        }
 *    }
 *  }</pre>
 *  <p>
 *  For more usage information <a href="https://globaltcad.github.io/sprouts/">check out this page</a>,
 *  where you can browse the living documentation of the Sprouts API. <br>
 *  Also check out <a href="https://github.com/globaltcad/swing-tree">the SwingTree GUI Framework</a> for more example
 *  code using the Sprouts properties natively.
 */
@NullMarked package sprouts;

import org.jspecify.annotations.NullMarked;