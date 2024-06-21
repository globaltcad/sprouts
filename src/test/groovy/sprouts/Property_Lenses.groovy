package sprouts

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.impl.PropertyLens

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
class Property_Lenses extends Specification
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


}
