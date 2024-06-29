package sprouts

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.impl.PropertyLens

import java.lang.ref.WeakReference
import java.time.LocalDate

@Title("Property Lenses")
@Narrative('''

    The Sprouts Property Lens is based on the Lens design pattern.
    The Lens design pattern is a functional programming 
    technique used to simplify the process of accessing and updating parts of 
    a nested (immutable) data structures into a new instance of the data structure.
    
    A Lens is essentially a pair of functions: 
    one to get a value from a specific part of a data structure (like a record), 
    and another to set or update that value while producing a new 
    instance of the data structure. This pattern is particularly useful with Java records, 
    which are immutable by design, as it allows for clean and concise manipulation 
    of deeply nested fields without breaking immutability.
    
    Now what does this have to do with Sprouts properties?
    After all, the MVVM properties of this library are mutable 
    wrapper types with regular getter and setter methods.
    Although properties are mutable, their items are expected to
    be immutable data carriers, such as ints, doubles, strings or records.
    In case of records (or other custom value oriented data types),
    there is really no limit to how deeply nested the data structure can be.
    You may even want to model your entire application state as a single record
    composed of other records, lists, maps and primitives.
    This is where the Property Lens comes in:
    
    You can create a lens property from any regular property
    holding an immutable data structure, and then use the lens property
    like a regular property.
    
    Under the hood the lens property will use the lens pattern to access
    and update the nested data structure of the original property.
    
    In this specification we will demonstrate how to create and use
    lens properties and also explore edge cases and limitations.
    
    In this Specification we are using the following 
    data structure classes:
    ````java
        import java.time.LocalDate;
        import java.util.List;
        
        // Enums
        enum Genre {
            FICTION, NON_FICTION, SCIENCE, FANTASY, BIOGRAPHY, HISTORY
        }
        
        enum MembershipLevel {
            BASIC, SILVER, GOLD, PLATINUM
        }
        
        // Records
        public record Author(String firstName, String lastName, LocalDate birthDate, List<String> books) {
            public Author withFirstName(String firstName) {
                return new Author(firstName, this.lastName, this.birthDate, this.books);
            }
        
            public Author withLastName(String lastName) {
                return new Author(this.firstName, lastName, this.birthDate, this.books);
            }
        
            public Author withBirthDate(LocalDate birthDate) {
                return new Author(this.firstName, this.lastName, birthDate, this.books);
            }
        
            public Author withBooks(List<String> books) {
                return new Author(this.firstName, this.lastName, this.birthDate, books);
            }
        }
        
        public record Book(String title, Author author, Genre genre, LocalDate publicationDate, int pageCount) {
            public Book withTitle(String title) {
                return new Book(title, this.author, this.genre, this.publicationDate, this.pageCount);
            }
        
            public Book withAuthor(Author author) {
                return new Book(this.title, author, this.genre, this.publicationDate, this.pageCount);
            }
        
            public Book withGenre(Genre genre) {
                return new Book(this.title, this.author, genre, this.publicationDate, this.pageCount);
            }
        
            public Book withPublicationDate(LocalDate publicationDate) {
                return new Book(this.title, this.author, this.genre, publicationDate, this.pageCount);
            }
        
            public Book withPageCount(int pageCount) {
                return new Book(this.title, this.author, this.genre, this.publicationDate, pageCount);
            }
        }
        
        public record Member(String memberId, String firstName, String lastName, MembershipLevel membershipLevel, LocalDate joinDate, String email) {
            public Member withMemberId(String memberId) {
                return new Member(memberId, this.firstName, this.lastName, this.membershipLevel, this.joinDate, this.email);
            }
        
            public Member withFirstName(String firstName) {
                return new Member(this.memberId, firstName, this.lastName, this.membershipLevel, this.joinDate, this.email);
            }
        
            public Member withLastName(String lastName) {
                return new Member(this.memberId, this.firstName, lastName, this.membershipLevel, this.joinDate, this.email);
            }
        
            public Member withMembershipLevel(MembershipLevel membershipLevel) {
                return new Member(this.memberId, this.firstName, this.lastName, membershipLevel, this.joinDate, this.email);
            }
        
            public Member withJoinDate(LocalDate joinDate) {
                return new Member(this.memberId, this.firstName, this.lastName, this.membershipLevel, joinDate, this.email);
            }
        
            public Member withEmail(String email) {
                return new Member(this.memberId, this.firstName, this.lastName, this.membershipLevel, this.joinDate, email);
            }
        }
        
        public record Loan(String loanId, Book book, Member member, LocalDate loanDate, LocalDate returnDate, boolean returned) {
            public Loan withLoanId(String loanId) {
                return new Loan(loanId, this.book, this.member, this.loanDate, this.returnDate, this.returned);
            }
        
            public Loan withBook(Book book) {
                return new Loan(this.loanId, book, this.member, this.loanDate, this.returnDate, this.returned);
            }
        
            public Loan withMember(Member member) {
                return new Loan(this.loanId, this.book, member, this.loanDate, this.returnDate, this.returned);
            }
        
            public Loan withLoanDate(LocalDate loanDate) {
                return new Loan(this.loanId, this.book, this.member, loanDate, this.returnDate, this.returned);
            }
        
            public Loan withReturnDate(LocalDate returnDate) {
                return new Loan(this.loanId, this.book, this.member, this.loanDate, returnDate, this.returned);
            }
        
            public Loan withReturned(boolean returned) {
                return new Loan(this.loanId, this.book, this.member, this.loanDate, this.returnDate, returned);
            }
        }
        ```
''')
@Subject([PropertyLens, Var])
@CompileDynamic
class Property_Lenses_Spec extends Specification
{
    enum Genre { FICTION, NON_FICTION, SCIENCE, FANTASY, BIOGRAPHY, HISTORY }

    enum MembershipLevel { BASIC, SILVER, GOLD, PLATINUM }

    static record Author(String firstName, String lastName, LocalDate birthDate, List<String> books) {
        public Author withFirstName(String firstName) {
            return new Author(firstName, this.lastName, this.birthDate, this.books);
        }

        public Author withLastName(String lastName) {
            return new Author(this.firstName, lastName, this.birthDate, this.books);
        }

        public Author withBirthDate(LocalDate birthDate) {
            return new Author(this.firstName, this.lastName, birthDate, this.books);
        }

        public Author withBooks(List<String> books) {
            return new Author(this.firstName, this.lastName, this.birthDate, books);
        }
    }

    public static record Book(String title, Author author, Genre genre, LocalDate publicationDate, int pageCount) {
        public Book withTitle(String title) {
            return new Book(title, this.author, this.genre, this.publicationDate, this.pageCount);
        }

        public Book withAuthor(Author author) {
            return new Book(this.title, author, this.genre, this.publicationDate, this.pageCount);
        }

        public Book withGenre(Genre genre) {
            return new Book(this.title, this.author, genre, this.publicationDate, this.pageCount);
        }

        public Book withPublicationDate(LocalDate publicationDate) {
            return new Book(this.title, this.author, this.genre, publicationDate, this.pageCount);
        }

        public Book withPageCount(int pageCount) {
            return new Book(this.title, this.author, this.genre, this.publicationDate, pageCount);
        }
    }

    public static record Member(String memberId, String firstName, String lastName, MembershipLevel membershipLevel, LocalDate joinDate, String email) {
        public Member withMemberId(String memberId) {
            return new Member(memberId, this.firstName, this.lastName, this.membershipLevel, this.joinDate, this.email);
        }

        public Member withFirstName(String firstName) {
            return new Member(this.memberId, firstName, this.lastName, this.membershipLevel, this.joinDate, this.email);
        }

        public Member withLastName(String lastName) {
            return new Member(this.memberId, this.firstName, lastName, this.membershipLevel, this.joinDate, this.email);
        }

        public Member withMembershipLevel(MembershipLevel membershipLevel) {
            return new Member(this.memberId, this.firstName, this.lastName, membershipLevel, this.joinDate, this.email);
        }

        public Member withJoinDate(LocalDate joinDate) {
            return new Member(this.memberId, this.firstName, this.lastName, this.membershipLevel, joinDate, this.email);
        }

        public Member withEmail(String email) {
            return new Member(this.memberId, this.firstName, this.lastName, this.membershipLevel, this.joinDate, email);
        }
    }

    public static record Loan(String loanId, Book book, Member member, LocalDate loanDate, LocalDate returnDate, boolean returned) {
        public Loan withLoanId(String loanId) {
            return new Loan(loanId, this.book, this.member, this.loanDate, this.returnDate, this.returned);
        }

        public Loan withBook(Book book) {
            return new Loan(this.loanId, book, this.member, this.loanDate, this.returnDate, this.returned);
        }

        public Loan withMember(Member member) {
            return new Loan(this.loanId, this.book, member, this.loanDate, this.returnDate, this.returned);
        }

        public Loan withLoanDate(LocalDate loanDate) {
            return new Loan(this.loanId, this.book, this.member, loanDate, this.returnDate, this.returned);
        }

        public Loan withReturnDate(LocalDate returnDate) {
            return new Loan(this.loanId, this.book, this.member, this.loanDate, returnDate, this.returned);
        }

        public Loan withReturned(boolean returned) {
            return new Loan(this.loanId, this.book, this.member, this.loanDate, this.returnDate, returned);
        }
    }

    def 'Many lens properties can be created from a regular property.'()
    {
        reportInfo """
            Note that a lens only focuses on a single field of the targeted data structure.
            So if the original property changes its state to a completely new instance of the data structure,
            then the lens will only receive an update event if the focused field of the new instance is different
            from the focused field of the old instance.
        """
        given: """
            For this example we will merely use the Author record
            to demonstrate how each property of the record can be
            accessed and updated using a lens property.
            
            We also assert that the lenses receive updates when the
            the record property they focus on is updated.
        """
            var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)
        and : 'We create lenses for each property of the Author record:'
            var firstNameLens    = authorProperty.zoomTo(Author::firstName, Author::withFirstName)
            var lastNameLens     = authorProperty.zoomTo(Author::lastName,  Author::withLastName)
            var birthDateLens = authorProperty.zoomTo(Author::birthDate, Author::withBirthDate)
            var booksLens      = authorProperty.zoomTo(Author::books,     Author::withBooks)
        and : 'We also create lists and listeners for recording all change events:'
            var firstNameTrace = []
            var lastNameTrace = []
            var birthDateTrace = []
            var booksTrace = []
            var authorTrace = []
            firstNameLens.onChange(From.ALL,it -> firstNameTrace << it.get() )
            lastNameLens.onChange(From.ALL,it -> lastNameTrace << it.get() )
            birthDateLens.onChange(From.ALL,it -> birthDateTrace << it.get() )
            booksLens.onChange(From.ALL,it -> booksTrace << it.get() )
            authorProperty.onChange(From.ALL,it -> authorTrace << it.get() )

        expect: 'Initial values are correct:'
            firstNameLens.get() == author.firstName()
            lastNameLens.get() == author.lastName()
            birthDateLens.get() == author.birthDate()
            booksLens.get() == author.books()
            authorProperty.get() == author

        when : 'We create a completely new and completely different Author record:'
            var newAuthor = new Author("Jane", "Smith", LocalDate.of(1997, 9, 3), ["Book3", "Book4"])
            authorProperty.set(newAuthor)

        then : 'The lenses have updated values:'
            firstNameLens.get() == newAuthor.firstName()
            lastNameLens.get() == newAuthor.lastName()
            birthDateLens.get() == newAuthor.birthDate()
            booksLens.get() == newAuthor.books()
            authorProperty.get() == newAuthor
        and : 'All change events were recorded:'
            firstNameTrace == ["Jane"]
            lastNameTrace == ["Smith"]
            birthDateTrace == [LocalDate.of(1997, 9, 3)]
            booksTrace == [["Book3", "Book4"]]
            authorTrace == [newAuthor]

        when : 'We update the Author in a way where effectively nothing changes:'
            authorProperty.set(newAuthor)

        then : 'The lenses have the same values as before:'
            firstNameLens.get() == newAuthor.firstName()
            lastNameLens.get() == newAuthor.lastName()
            birthDateLens.get() == newAuthor.birthDate()
            booksLens.get() == newAuthor.books()
            authorProperty.get() == newAuthor
        and : 'No additional change events were recorded:'
            firstNameTrace == ["Jane"]
            lastNameTrace == ["Smith"]
            birthDateTrace == [LocalDate.of(1997, 9, 3)]
            booksTrace == [["Book3", "Book4"]]
            authorTrace == [newAuthor]

        when : 'We update the Author in a way where only 2 properties change:'
            var newAuthor2 = newAuthor.withFirstName("Martha").withBirthDate(LocalDate.of(2002, 5, 17))
            authorProperty.set(newAuthor2)

        then : 'The lenses have updated values:'
            firstNameLens.get() == newAuthor2.firstName()
            lastNameLens.get() == newAuthor2.lastName()
            birthDateLens.get() == newAuthor2.birthDate()
            booksLens.get() == newAuthor2.books()
            authorProperty.get() == newAuthor2
        and : 'Only the relevant change events were recorded:'
            firstNameTrace == ["Jane", "Martha"]
            lastNameTrace == ["Smith"]
            birthDateTrace == [LocalDate.of(1997, 9, 3), LocalDate.of(2002, 5, 17)]
            booksTrace == [["Book3", "Book4"]]
            authorTrace == [newAuthor, newAuthor2]

        when : """
            We modify one of the lenses, then this will automatically
            update the full value of the original property.
            The other lenses on the other hand will not receive any updates
            because the focused field of the original property have not changed:
        """
            firstNameLens.set("Raffaela")

        then : 'The original property has updated values:'
            authorProperty.get() == newAuthor2.withFirstName("Raffaela")
        and : 'Only the relevant change events were recorded:'
            firstNameTrace == ["Jane", "Martha", "Raffaela"]
            lastNameTrace == ["Smith"]
            birthDateTrace == [LocalDate.of(1997, 9, 3), LocalDate.of(2002, 5, 17)]
            booksTrace == [["Book3", "Book4"]]
            authorTrace == [newAuthor, newAuthor2, newAuthor2.withFirstName("Raffaela")]

        when : 'We pass a value to a lens which is equal to the current value of the lens:'
            lastNameLens.set("Smith")

        then : 'No change events are recorded:'
            firstNameTrace == ["Jane", "Martha", "Raffaela"]
            lastNameTrace == ["Smith"]
            birthDateTrace == [LocalDate.of(1997, 9, 3), LocalDate.of(2002, 5, 17)]
            booksTrace == [["Book3", "Book4"]]
            authorTrace == [newAuthor, newAuthor2, newAuthor2.withFirstName("Raffaela")]
    }

    def 'You can create lenses from other lenses to dive deeper into nested data structures.'()
    {
        reportInfo """
            As previously mentioned, a property lens effectively behaves like a regular property,
            which includes the ability to create yet another lens from an existing lens
            for which the same rules apply.
            
            This allows you to create a chain of lenses to access deeply nested fields
            of a data structure.
            
            In this example we will create a property for the `Loan` record
            and then create lenses its nested `Book` and `Member` records
            and then create lenses for the nested `Author` record of the `Book` record.
            Changing the original property will trigger updates for all lenses in the chain.
        """
        given: """
            We first create full data structure composed of many nested records.
        """
            var author = new Author("Raffaela", "Raab", LocalDate.of(1996, 3, 21), ["Book1", "Book2"])
            var book   = new Book("The Book", author, Genre.HISTORY, LocalDate.of(2019, 5, 12), 304)
            var member = new Member("1234", "Marc", "Mayer", MembershipLevel.GOLD, LocalDate.of(2015, 2, 3), null)
            var loan   = new Loan("5678", book, member, LocalDate.of(2021, 8, 12), LocalDate.of(2021, 9, 12), false)
            var loanProperty = Var.of(loan)

        and: """
            Create lenses for nested records and also trace lists for recording
            state changes of the original property and all of its lenses.
        """
            var bookLens = loanProperty.zoomTo(Loan::book, Loan::withBook)
            var memberLens = loanProperty.zoomTo(Loan::member, Loan::withMember)
            var authorLens = bookLens.zoomTo(Book::author, Book::withAuthor)

            var loanTrace = []
            var bookTrace = []
            var memberTrace = []
            var authorTrace = []

            loanProperty.onChange(From.ALL, it -> loanTrace << it.get())
            bookLens.onChange(From.ALL, it -> bookTrace << it.get())
            memberLens.onChange(From.ALL, it -> memberTrace << it.get())
            authorLens.onChange(From.ALL, it -> authorTrace << it.get())

        expect: """
            The initial values are correct.
            This is a bit of a sanity check to ensure that the lenses
            are correctly set up and that they can access the nested records.
        """
            loanProperty.get() == loan
            bookLens.get() == loan.book()
            memberLens.get() == loan.member()
            authorLens.get() == loan.book().author()

        when: """
            We update various levels of nested structures.
        """
            var newAuthor = new Author("John", "Doe", LocalDate.of(1980, 2, 15), ["New Book"])
            var newBook = loan.book().withAuthor(newAuthor)
            var newLoan = loan.withBook(newBook)
            loanProperty.set(newLoan)

        then: """
            Values are updated correctly and change events are recorded.
            Note that the member lens trace is empty because the member record
            was not updated from the original loan record.
        """
            loanProperty.get() == newLoan
            bookLens.get() == newLoan.book()
            memberLens.get() == newLoan.member()
            authorLens.get() == newLoan.book().author()

            loanTrace == [newLoan]
            bookTrace == [newLoan.book()]
            memberTrace == []
            authorTrace == [newLoan.book().author()]
    }

    def 'You can create nullable lens properties from a regular property.'()
    {
        reportInfo """
            This test ensures that the `zoomToNullable` method works correctly
            with a property that contains a nullable field.
            
            We will use the Member record which has an optional email field
            to demonstrate how the lens property behaves with nullable fields.
        """
        given: """
            Initialize a Member record instance with an optional email field.
        """
            var member = new Member("1234", "John", "Doe", MembershipLevel.GOLD, LocalDate.of(2010, 1, 1), null)
            var memberProperty = Var.of(member)

        and: """
            Create a lens for the nullable email field of the Member record.
            Also set up a trace list to record state changes of the lens and the original property.
        """
            var emailLens = memberProperty.zoomToNullable(String.class, Member::email, Member::withEmail)
            var memberTrace = []
            var emailTrace = []
            memberProperty.onChange(From.ALL, it -> memberTrace << it.get())
            emailLens.onChange(From.ALL, it -> emailTrace << it.get())

        expect: """
            Initial values are correct:
            - The lens should correctly retrieve the initial nullable email field value.
            - The original property should contain the initial Member record.
        """
            emailLens.orElseNull() == null
            memberProperty.get() == member

        when: """
            Update the nullable email field using the lens.
        """
            emailLens.set("john.doe@example.com")

        then: """
            The lens should update the nullable email field value.
            Both the lens and the original property should reflect the changes.
        """
            emailLens.get() == "john.doe@example.com"
            memberProperty.get().email() == "john.doe@example.com"

        and: """
            Change the nullable email field back to null using the lens.
        """
            emailLens.set(null)

        then: """
            The lens should update the nullable email field value to null.
            Both the lens and the original property should reflect the changes.
        """
            emailLens.orElseNull() == null
            memberProperty.get().email() == null

        when: """
            Update the original property with a new Member instance where the email field is null.
        """
            var newMember = member.withEmail(null)
            memberProperty.set(newMember)

        then: """
            The lens should correctly reflect the updated nullable email field value as null.
            Both the lens and the original property should reflect the changes.
        """
            emailLens.orElseNull() == null
            memberProperty.get() == newMember

        and: """
            Update the original property with a new Member instance where the email field is set.
        """
            var updatedMember = member.withEmail("jane.doe@example.com")
            memberProperty.set(updatedMember)

        then: """
            The lens should correctly reflect the updated nullable email field value.
            Both the lens and the original property should reflect the changes.
        """
            emailLens.get() == "jane.doe@example.com"
            memberProperty.get() == updatedMember
    }

    def 'A lens created from the `zoomTo` method throws an exception when receiving null values.'() {
        given: "An Author record and its property"
            var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)

        and: "A lens focusing on the first name of the Author"
            var firstNameLens = authorProperty.zoomTo(Author::firstName, Author::withFirstName)

        when: "Setting a null value via the lens"
            firstNameLens.set(null)

        then: "An exception is thrown"
            thrown(NullPointerException)
    }

    def 'You cannot create a lens from the `zoomTo` method if the target value is null.'() {
        given: "An Author record with a null first name and its property"
            var author = new Author(null, "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)

        when: "Creating a lens focusing on the first name of the Author"
            authorProperty.zoomTo(Author::firstName, Author::withFirstName)

        then: "An exception is thrown"
            thrown(NullPointerException)
    }

    def 'A lens on a nested null value models null without throwing an exception.'() {
        given: "A Member record with a null book"
            var member = new Member("1234", "John", "Doe", MembershipLevel.GOLD, LocalDate.of(2010, 1, 1), null)
            var memberProperty = Var.of(member)

        when: "Creating a lens focusing on a nested nullable field"
            var emailLens = memberProperty.zoomToNullable(String.class, Member::email, Member::withEmail)

        then: "The lens handles the null value correctly"
            emailLens.orElseNull() == null
    }

    def 'We can create nullable lenses from nullable property even if there is no initial value.'()
    {
        reportInfo """
            It should be possible to create a nullable lens from a nullable property
            even if the initial value of the first lens is null.
            In that case the second lens should also have a null value.
        """
        given: """
            We create a nullable `Book` based property which is 
            initially set to null.
            And then we create a nullable lens for the `Author` record
            of the `Book` record.
        """
            var bookProperty = Var.ofNull(Book.class)
            var authorLens = bookProperty.zoomToNullable(Author.class, Book::author, Book::withAuthor)
        expect : """
            Both the lens and the original property should have a null value.
        """
            authorLens.orElseNull() == null
            bookProperty.orElseNull() == null

        when : 'We set a new Book instance without an author.'
            var book = new Book("The Book", null, Genre.HISTORY, LocalDate.of(2019, 5, 12), 304)
            bookProperty.set(book)
        then : 'The book property has the new value and the lens has a null value.'
            bookProperty.get() == book
            authorLens.orElseNull() == null

        when : 'We set a new Book instance with an author.'
            var author = new Author("John", "Doe", LocalDate.of(1980, 2, 15), ["New Book"])
            var newBook = book.withAuthor(author)
            bookProperty.set(newBook)
        then : 'The book property has the new value and the lens has the author value.'
            bookProperty.get() == newBook
            authorLens.get() == author
    }

    def 'A property lens with a default value focuses on specific field with a non-null parent value.'() {
        given: "An Author record and its property"
            var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)
        and: "A lens focusing on the first name of the Author with a default value"
            var defaultFirstName = "Unknown"
            var firstNameLens = authorProperty.zoomTo(defaultFirstName, Author::firstName, Author::withFirstName)

        expect: "The lens retrieves the correct initial value"
            firstNameLens.get() == author.firstName()

        when: "The first name is updated through the lens"
            firstNameLens.set("Jane")

        then: "The original property is updated correctly"
            authorProperty.get().firstName() == "Jane"
            firstNameLens.get() == "Jane"
    }

    def 'A property lens with a default value uses its default value when the parent property value is null initially.'() {
        given: "A nullable Author property initialized to null"
            var authorProperty = Var.ofNull(Author.class)
        and: "A lens focusing on the first name of the Author with a default value"
            var defaultFirstName = "Unknown"
            var firstNameLens = authorProperty.zoomTo(defaultFirstName, Author::firstName, Author::withFirstName)

        expect: "The lens retrieves the default value when the parent is null"
            firstNameLens.get() == defaultFirstName

        when: "A new Author record is set with a specific first name"
            var newAuthor = new Author("Jane", "Smith", LocalDate.of(1997, 9, 3), ["Book3", "Book4"])
            authorProperty.set(newAuthor)

        then: "The lens retrieves the new value from the parent"
            firstNameLens.get() == newAuthor.firstName()
    }

    def 'A property lens with a default value updates the parent value correctly when setting it through the lens.'() {
        given: "An Author record and its property"
            var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)
        and: "A lens focusing on the first name of the Author with a default value"
            var defaultFirstName = "Unknown"
            var firstNameLens = authorProperty.zoomTo(defaultFirstName, Author::firstName, Author::withFirstName)

        when: "The first name is updated through the lens"
            firstNameLens.set("Jane")

        then: "The original property is updated correctly"
            authorProperty.get().firstName() == "Jane"
    }

    def 'A lens with a default value throws an exception when the null object is null.'() {
        given: "An Author record and its property"
            var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)

        when: "Creating a lens with a null default value"
            authorProperty.zoomTo(null, Author::firstName, Author::withFirstName)

        then: "An exception is thrown"
            thrown(NullPointerException)
    }

    def 'A lens with a default value handles updates gracefully when switching from null parent to non-null parent.'() {
        given: "A nullable Author property initialized to null"
            var authorProperty = Var.ofNull(Author.class)
        and: "A lens focusing on the first name of the Author with a default value"
            var defaultFirstName = "Unknown"
            var firstNameLens = authorProperty.zoomTo(defaultFirstName, Author::firstName, Author::withFirstName)

        expect: "The lens retrieves the default value when the parent is null"
            firstNameLens.get() == defaultFirstName

        when: "A new Author record is set with a specific first name"
            var newAuthor = new Author("Jane", "Smith", LocalDate.of(1997, 9, 3), ["Book3", "Book4"])
            authorProperty.set(newAuthor)

        then: "The lens retrieves the new value from the parent"
            firstNameLens.get() == newAuthor.firstName()

        when: "The parent is set back to null"
            authorProperty.set(null)

        then: "The lens retrieves the default value again"
            firstNameLens.get() == defaultFirstName
    }

    def 'The lenses of a property are garbage collected when no longer referenced strongly.'()
    {
        reportInfo """
            A property lens registers a weak action listener on the original property.
            These weak listeners are stored in a weak hash map where the property lens
            itself is the weakly referenced key.
            So when the property lens is no longer referenced strongly, it should be
            garbage collected and the weak listener should be removed
            from the original property.
            
            We can verify this by checking the reported number of change listeners.
        """
        given : 'We have an `Author` record and a property holding it.'
            var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)
        expect : 'Initially there are no change listeners registered:'
            authorProperty.numberOfChangeListeners() == 0

        when : 'We create two lenses which we reference strongly.'
            var firstName = authorProperty.zoomTo(Author::firstName, Author::withFirstName)
            var lastName = authorProperty.zoomTo(Author::lastName,  Author::withLastName)

        then : 'The author property has 2 change listeners registered.'
            authorProperty.numberOfChangeListeners() == 2

        when : 'We create two lenses which we do not reference strongly.'
            authorProperty.zoomTo(Author::birthDate, Author::withBirthDate)
            authorProperty.zoomTo(Author::books,     Author::withBooks)
        and : 'We wait for the garbage collector to run.'
            waitForGarbageCollection()
            Thread.sleep(500)

        then : 'The author property should still have 2 change listeners registered.'
            authorProperty.numberOfChangeListeners() == 2
    }

    def 'You can access the item of a lens, even if an exception occurs in its getter.'()
    {
        reportInfo """
            The item of a property lens is always retrieved from a getter function.
            If an exception occurs in the getter function, then the lens will
            return the last known item.
        """
        given : 'We create an `Author` and store it in a regular property.'
            var author = new Author("Joe", "Average", LocalDate.of(1990, 1, 1), [])
            var authorProperty = Var.of(author)
        and : 'We create a lens for the last name of the author with a faulty getter function.'
            var getterFails = false
            var lastName = authorProperty.zoomTo(
                                                {
                                                    if (getterFails) throw new RuntimeException()
                                                    else return it.lastName()
                                                },
                                                Author::withLastName
                                            )
        expect : 'The lens should have the correct initial value.'
            lastName.get() == "Average"
        when : 'We set the getter to fail and then try to access the item of the lens.'
            getterFails = true
        then : 'No exception is thrown...'
            noExceptionThrown()
        and : '...and the lens returns the last known item.'
            lastName.get() == "Average"

        when : 'We completely change the original property.'
            authorProperty.set(new Author("Jane", "Doe", LocalDate.of(1995, 5, 5), ["Book1", "Book2"]))
        then : """
            The lens should still return the last known item due to the exception
            preventing the lens from updating its item correctly
            through the getter function.
        """
            lastName.get() == "Average"

        when : 'We set the getter to work again and then try to access the item of the lens.'
            getterFails = false
        then : 'The lens should return the correct item.'
            lastName.get() == "Doe"
    }


    def 'You can try to change the item of a lens, even if an exception occurs in its wither.'()
    {
        reportInfo """
            When modifying the item of a lens, a wither function is used.
            This wither function takes the item of the parent property
            and the new item of the lens as arguments and then returns
            an updated item for the parent property.
            
            If an exception occurs in this wither function, then the lens
            will fail to update the parent property.
        """
        given : 'We create an `Author` and store it in a regular property.'
            var author = new Author("Filia", "Fischer", LocalDate.of(2010, 3, 6), [])
            var authorProperty = Var.of(author)
        and : 'We create a lens for the last name of the author with a faulty wither function.'
            var witherFails = false
            var lastName = authorProperty.zoomTo(
                                            Author::lastName,
                                            (oldAuthor, newName)->{
                                                if (witherFails) throw new RuntimeException()
                                                else return oldAuthor.withLastName(newName)
                                            }
                                        )
        expect : 'The lens should have the correct initial value.'
            lastName.get() == "Fischer"
        when : 'We modify the property lens to "Mayer"...'
            lastName.set("Mayer")
        then : 'The parent property is updated correctly.'
            authorProperty.get().lastName() == "Mayer"

        when : """
            We set the wither to fail and then try to modify the item of the lens again.
            Let's say we want to set the last name to "Smith".
        """
            witherFails = true
            lastName.set("Smith")
        then : 'No exception is thrown...'
            noExceptionThrown()
        and : """
            Due to the exception in the wither function, the parent property
            is not updated correctly.
            And because the lens represents the last known item of the parent property,
            it still returns the last name "Mayer", the name "Smith" is never applied.
        """
            authorProperty.get().lastName() == "Mayer"
            lastName.get() == "Mayer"
    }

    def 'A property lens knows that it is a property lens.'()
    {
        given : 'We create a regular property, a property lens and a view of a property.'
            var regularProperty = Var.of(new Author("Forename", "Surname", LocalDate.of(2008, 8, 12), []))
            var lens = regularProperty.zoomTo(Author::birthDate, Author::withBirthDate)
            var view = lens.viewAsString( date -> "Year: " + date.getDayOfYear() )
        expect :
            !regularProperty.isLens() && !regularProperty.isView()
            lens.isLens() && !lens.isView()
            !view.isLens() && view.isView()
    }

    def 'A property knows if it is mutable or not or nullable or not.'()
    {
        given : 'We create a regular property, a property lens and a view of a property.'
            var regularProperty = Var.of(new Author("Forename", "Surname", LocalDate.of(2008, 8, 12), []))
            var lens = regularProperty.zoomTo(Author::birthDate, Author::withBirthDate)
            var view = lens.viewAsString( date -> "Year: " + date.getDayOfYear() )
            var regularNullProperty = Var.ofNullable(Author.class, new Author("Forename", "Surname", LocalDate.of(2008, 8, 12), []))
            var nullLens = regularProperty.zoomToNullable(Author.class, Author::birthDate, Author::withBirthDate)
            var nullView = lens.viewAsNullable(String.class, date -> "Year: " + date.getDayOfYear() )
            var immutableProperty = Val.of("I never change!")
            var immutableNullProperty = Val.ofNullable(String, "I also never change!")
        expect :
            regularProperty.isMutable() && !regularProperty.allowsNull()
            lens.isMutable() && !lens.allowsNull()
            view.isMutable() && !view.allowsNull()
            regularNullProperty.isMutable() && regularNullProperty.allowsNull()
            nullLens.isMutable() && nullLens.allowsNull()
            nullView.isMutable() && nullView.allowsNull()
            !immutableProperty.isMutable() && !immutableProperty.allowsNull()
            !immutableNullProperty.isMutable() && immutableNullProperty.allowsNull()
    }

    def 'You can recognize a property lens from its String representation.'()
    {
        reportInfo """
            A property lens has a specific string representation that can be used to recognize it.
            The string representation of a property lens starts with "Lens" followed by the item
            type and square brackets containing the current item of the lens.
        """
        given : 'A source property and a lens focusing on a specific field.'
            var author = new Author("Forename", "Surname", LocalDate.of(2008, 8, 12), [])
            var authorProperty = Var.of(author)
            var lens = authorProperty.zoomTo(Author::birthDate, Author::withBirthDate)
        expect : 'The string representation of the lens should be recognizable.'
            lens.toString() == "Lens<LocalDate>[2008-08-12]"
    }

    /**
     * This method guarantees that garbage collection is
     * done unlike <code>{@link System#gc()}</code>
     */
    static void waitForGarbageCollection() {
        Object obj = new Object();
        WeakReference ref = new WeakReference<Object>(obj);
        obj = null;
        while(ref.get() != null) {
            System.gc();
        }
    }

}
