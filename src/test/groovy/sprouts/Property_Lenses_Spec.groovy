package sprouts

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.impl.PropertyLens

import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.Month

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
        
        static record TrainStation(String name, String description, LocalDate foundingDate, List<String> trains) {
            public TrainStation withName(String name) {
                return new TrainStation(name, this.description, this.foundingDate, this.trains);
            }
    
            public TrainStation withDescription(String description) {
                return new TrainStation(this.name, description, this.foundingDate, this.trains);
            }
    
            public TrainStation withBirthDate(LocalDate birthDate) {
                return new TrainStation(this.name, this.description, birthDate, this.trains);
            }
    
            public TrainStation withBooks(List<String> books) {
                return new TrainStation(this.name, this.description, this.foundingDate, books);
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

    static record TrainStation(String name, String description, LocalDate foundingDate, List<String> trains) {
        public TrainStation withName(String name) {
            return new TrainStation(name, this.description, this.foundingDate, this.trains);
        }

        public TrainStation withDescription(String description) {
            return new TrainStation(this.name, description, this.foundingDate, this.trains);
        }

        public TrainStation withBirthDate(LocalDate birthDate) {
            return new TrainStation(this.name, this.description, birthDate, this.trains);
        }

        public TrainStation withBooks(List<String> books) {
            return new TrainStation(this.name, this.description, this.foundingDate, books);
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
            Viewable.cast(firstNameLens).onChange(From.ALL,it -> firstNameTrace << it.currentValue().orElseNull() )
            Viewable.cast(lastNameLens).onChange(From.ALL,it -> lastNameTrace << it.currentValue().orElseNull() )
            Viewable.cast(birthDateLens).onChange(From.ALL,it -> birthDateTrace << it.currentValue().orElseNull() )
            Viewable.cast(booksLens).onChange(From.ALL,it -> booksTrace << it.currentValue().orElseNull() )
            Viewable.cast(authorProperty).onChange(From.ALL,it -> authorTrace << it.currentValue().orElseNull() )

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

    def 'Many lens properties can be created from a regular property and instances of the `Lens` interface.'()
    {
        reportInfo """
            Note that a lens only focuses on a single field of the targeted data structure.
            So if the original property changes its state to a completely new instance of the data structure,
            then the lens will only receive an update event if the focused field of the new instance is different
            from the focused field of the old instance.
        """
        given: """
            For this example we will again use the Author record
            together with anonymous instances of the `Lens` interface
            to demonstrate how each property of the record can be
            accessed and updated using a lens property.
            
            We also assert that the lenses receive updates when the
            the record property they focus on is updated.
        """
            var trainStation = new TrainStation("Shin-Osaka", "Cool", LocalDate.of(1964, 10, 1), ["Train1", "Train2"])
            var stationProperty = Var.of(trainStation)
        and :
            var nameLens        = Lens.of(TrainStation::name,         TrainStation::withName)
            var descriptionLens = Lens.of(TrainStation::description,  TrainStation::withDescription)
            var foundingLens = Lens.of(TrainStation::foundingDate, TrainStation::withBirthDate)
            var trainsLens    = Lens.of(TrainStation::trains,       TrainStation::withBooks)
        and : 'We create lenses for each property of the TrainStation record:'
            var nameLensProp = stationProperty.zoomTo(nameLens)
            var descriptionLensProp = stationProperty.zoomTo(descriptionLens)
            var foundingLensProp = stationProperty.zoomTo(foundingLens)
            var trainsLensProp    = stationProperty.zoomTo(trainsLens)
        and : 'We also create lists and listeners for recording all change events:'
            var nameTrace = []
            var descriptionTrace = []
            var foundingTrace = []
            var trainsTrace = []
            var stationTrace = []
            Viewable.cast(nameLensProp).onChange(From.ALL,it -> nameTrace << it.currentValue().orElseNull() )
            Viewable.cast(descriptionLensProp).onChange(From.ALL,it -> descriptionTrace << it.currentValue().orElseNull() )
            Viewable.cast(foundingLensProp).onChange(From.ALL,it -> foundingTrace << it.currentValue().orElseNull() )
            Viewable.cast(trainsLensProp).onChange(From.ALL,it -> trainsTrace << it.currentValue().orElseNull() )
            Viewable.cast(stationProperty).onChange(From.ALL,it -> stationTrace << it.currentValue().orElseNull() )

        expect: 'Initial values are correct:'
            nameLensProp.get() == trainStation.name()
            descriptionLensProp.get() == trainStation.description()
            foundingLensProp.get() == trainStation.foundingDate()
            trainsLensProp.get() == trainStation.trains()
            stationProperty.get() == trainStation

        when : 'We create a completely new and completely different TrainStation record:'
            var newStation = new TrainStation("Amsterdam-Central", "Nice", LocalDate.of(1889, 10, 15), ["Train3", "Train4"])
            stationProperty.set(newStation)

        then : 'The lenses have updated values:'
            nameLensProp.get() == newStation.name()
            descriptionLensProp.get() == newStation.description()
            foundingLensProp.get() == newStation.foundingDate()
            trainsLensProp.get() == newStation.trains()
            stationProperty.get() == newStation
        and : 'All change events were recorded:'
            nameTrace == ["Amsterdam-Central"]
            descriptionTrace == ["Nice"]
            foundingTrace == [LocalDate.of(1889, 10, 15)]
            trainsTrace == [["Train3", "Train4"]]
            stationTrace == [newStation]

        when : 'We update the TrainStation in a way where effectively nothing changes:'
            stationProperty.set(newStation)

        then : 'The lenses have the same values as before:'
            nameLensProp.get() == newStation.name()
            descriptionLensProp.get() == newStation.description()
            foundingLensProp.get() == newStation.foundingDate()
            trainsLensProp.get() == newStation.trains()
            stationProperty.get() == newStation
        and : 'No additional change events were recorded:'
            nameTrace == ["Amsterdam-Central"]
            descriptionTrace == ["Nice"]
            foundingTrace == [LocalDate.of(1889, 10, 15)]
            trainsTrace == [["Train3", "Train4"]]
            stationTrace == [newStation]

        when : 'We update the TrainStation in a way where only 2 properties change:'
            var newStation2 = newStation.withName("Vienna-Central").withBirthDate(LocalDate.of(2014, 10, 10))
            stationProperty.set(newStation2)

        then : 'The lenses have updated values:'
            nameLensProp.get() == newStation2.name()
            descriptionLensProp.get() == newStation2.description()
            foundingLensProp.get() == newStation2.foundingDate()
            trainsLensProp.get() == newStation2.trains()
            stationProperty.get() == newStation2
        and : 'Only the relevant change events were recorded:'
            nameTrace == ["Amsterdam-Central", "Vienna-Central"]
            descriptionTrace == ["Nice"]
            foundingTrace == [LocalDate.of(1889, 10, 15), LocalDate.of(2014, 10, 10)]
            trainsTrace == [["Train3", "Train4"]]
            stationTrace == [newStation, newStation2]

        when : """
            We modify one of the lenses, then this will automatically
            update the full value of the original property.
            The other lenses on the other hand will not receive any updates
            because the focused field of the original property have not changed:
        """
            nameLensProp.set("Kyoto-eki")

        then : 'The original property has updated values:'
            stationProperty.get() == newStation2.withName("Kyoto-eki")
        and : 'Only the relevant change events were recorded:'
            nameTrace == ["Amsterdam-Central", "Vienna-Central", "Kyoto-eki"]
            descriptionTrace == ["Nice"]
            foundingTrace == [LocalDate.of(1889, 10, 15), LocalDate.of(2014, 10, 10)]
            trainsTrace == [["Train3", "Train4"]]
            stationTrace == [newStation, newStation2, newStation2.withName("Kyoto-eki")]

        when : 'We pass a value to a lens which is equal to the current value of the lens:'
            descriptionLensProp.set("Nice")

        then : 'No change events are recorded:'
            nameTrace == ["Amsterdam-Central", "Vienna-Central", "Kyoto-eki"]
            descriptionTrace == ["Nice"]
            foundingTrace == [LocalDate.of(1889, 10, 15), LocalDate.of(2014, 10, 10)]
            trainsTrace == [["Train3", "Train4"]]
            stationTrace == [newStation, newStation2, newStation2.withName("Kyoto-eki")]
    }

    def 'You can create lens properties from other lens properties to dive deeper into nested data structures.'()
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
            Create property lenses for nested records and also trace lists for recording
            state changes of the original property and all of its lens properties.
        """
            var bookLens = loanProperty.zoomTo(Loan::book, Loan::withBook)
            var memberLens = loanProperty.zoomTo(Loan::member, Loan::withMember)
            var authorLens = bookLens.zoomTo(Book::author, Book::withAuthor)

            var loanTrace = []
            var bookTrace = []
            var memberTrace = []
            var authorTrace = []

            Viewable.cast(loanProperty).onChange(From.ALL, it -> loanTrace << it.currentValue().orElseNull())
            Viewable.cast(bookLens).onChange(From.ALL, it -> bookTrace << it.currentValue().orElseNull())
            Viewable.cast(memberLens).onChange(From.ALL, it -> memberTrace << it.currentValue().orElseNull())
            Viewable.cast(authorLens).onChange(From.ALL, it -> authorTrace << it.currentValue().orElseNull())

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
            memberProperty.onChange(From.ALL, it -> memberTrace << it.currentValue().orElseNull())
            emailLens.onChange(From.ALL, it -> emailTrace << it.currentValue().orElseNull())

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

        when: "Creating a lens property with a null default value"
            authorProperty.zoomTo(null, Author::firstName, Author::withFirstName)

        then: "An exception is thrown"
            thrown(NullPointerException)

        when: "Creating a lens property from a `Lens` with a null default value"
            authorProperty.zoomTo(null, Lens.of(Author::firstName, Author::withFirstName))

        then: "An exception is thrown again."
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
        when : 'We update the lens to have a custom id String.'
            lens = lens.withId("patient_birth")
        then : 'The string representation of the view is as expected.'
            lens.toString() == "Lens<LocalDate>[patient_birth=2008-08-12]"
    }

    def 'You can subscribe and unsubscribe observer lambdas on property lenses.()'()
    {
        reportInfo """
            A property lens may act like a publisher that can have multiple observers.
            In this test we create a property lens and a change observer that listens to changes to the lens
            and is then unsubscribed, which should prevent further notifications.
        """
        given : 'A property based on the Author record.'
            var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)
        and : 'A lens of the surname of the author.'
            Val<String> lens = authorProperty.zoomTo(Author::lastName, Author::withLastName)
        and : 'A trace list and a change listener that listens to changes on the lens.'
            var trace = []
            Observer observer = { trace << lens.get() }
        expect : 'The trace list is empty and there are no change listeners.'
            trace.isEmpty()
            lens.numberOfChangeListeners() == 0

        when : 'We subscribe the listener to the lens.'
            Viewable.cast(lens).subscribe(observer)
        then : 'The listener is not immediately notified, but the lens has one change listener.'
            trace.isEmpty()
            lens.numberOfChangeListeners() == 1

        when : 'We change the value of the source property as well as the lens directly.'
            authorProperty.set(author.withLastName("Smith"))
            authorProperty.set(authorProperty.get().withFirstName("Jane"))
            lens.set("Mayer")
        then : 'The listener is notified of the new value of the lens.'
            trace == ["Smith", "Mayer"]

        when : 'We unsubscribe the listener from the lens.'
            Viewable.cast(lens).unsubscribe(observer)
        and : 'We change the value of the source property as well as the lens directly.'
            authorProperty.set(author.withLastName("Johnson"))
            authorProperty.set(authorProperty.get().withFirstName("Raffaela"))
            lens.set("Brown")
        then : 'The listener is not notified of the new value of the lens.'
            trace == ["Smith", "Mayer"]
        and : 'The lens has no change listeners.'
            lens.numberOfChangeListeners() == 0
    }

    def 'A chain of lenses may not be garbage collected.'()
    {
        reportInfo """
            Every lens holds a reference to the source property whose field it focuses on.
            So if a chain of lenses is created, the source property will ultimately be referenced,
            either directly or indirectly, by all lenses in the chain.
            These references may be weak references if the parent property is not a lens itself.
            This is also true for views, which are also always strongly referenced by a lens.
            A plain property on the other hand is weakly referenced by the lenses.
            So the chain of lenses may not be garbage collected if the source property 
            is not garbage collected.
        """
        given : 'A property based on the `Loan` record.'
            var loanProperty = Var.of(new Loan(
                                                    "l0an-1d",
                                                    new Book("Very Interesting Book",
                                                        new Author(
                                                            "Margarete", "Wick",
                                                            LocalDate.of(1932, 2, 15),
                                                            ["Very Interesting Book", "Not so..."]
                                                        ),
                                                        Genre.SCIENCE,
                                                        LocalDate.of(1963, 9, 23), 932
                                                    ),
                                                    new Member(
                                                        "m1d", "Lia", "Lua",
                                                        MembershipLevel.PLATINUM,
                                                        LocalDate.of(2015, 6, 13),
                                                        null
                                                    ),
                                                    LocalDate.of(2023, 8, 12),
                                                    LocalDate.of(2023, 9, 12),
                                                    false
                                                ))
        and : 'A chain of lenses going deeper into the nested records.'
            Val<Book>   view1 = loanProperty.zoomTo(Loan::book, Loan::withBook)
            Val<Author> view2 = view1.zoomTo(Book::author, Book::withAuthor)
            Val<String> view3 = view2.zoomTo(Author::firstName, Author::withFirstName)
        and : """
            Now we store all of these references in a list of WeakReference objects.
            This way we can check which of the references are still present
            after awaiting garbage collection.
        """
            var refs = [
                    new WeakReference(loanProperty), new WeakReference(view1),
                    new WeakReference(view2), new WeakReference(view3)
                ]
        expect : 'All references are still present after awaiting garbage collection.'
            refs.every( it -> it.get() != null )
        and : 'Each property has the expected number of change listeners for their respective child.'
            loanProperty.numberOfChangeListeners() == 1
            view1.numberOfChangeListeners() == 1
            view2.numberOfChangeListeners() == 1
            view3.numberOfChangeListeners() == 0

        when : 'We now remove the intermediate lenses from the chain.'
            view1 = null
            view2 = null
        and : 'We await garbage collection.'
            waitForGarbageCollection()
            Thread.sleep(500)
        then : 'Every reference is still present.'
            refs.every( it -> it.get() != null )

        when : 'We remove the last lens from the chain.'
            view3 = null
        and : 'We await garbage collection.'
            waitForGarbageCollection()
            Thread.sleep(500)
        then : 'All lenses were garbage collected.'
            refs[0].get() != null
            refs[1].get() == null
            refs[2].get() == null
            refs[3].get() == null
    }

    def 'The channel of a property change event will propagate to its lenses.'()
    {
        reportInfo """
            Every mutation to a property can have a channel associated with it.
            You can call the `Var.set(Channel,T)` method to mutate the property with a custom channel,
            and then in your change listeners you can check the channel on the property delegate!
            
            This exact same principle is also true for the lenses of a property
            whose change event listeners will also receive the channel of the origin property.
        """
        given : 'We have a book property and 3 lenses focusing on different fields of the book.'
            var book = new Book("The Book", new Author("John", "Doe", LocalDate.of(1980, 2, 15), ["New Book"]), Genre.HISTORY, LocalDate.of(2010, 7, 24), 374)
            var bookProperty = Var.of(book)
            var titleLens = bookProperty.zoomTo(Book::title, Book::withTitle)
            var authorLens = bookProperty.zoomTo(Book::author, Book::withAuthor)
            var genreLens = bookProperty.zoomTo(Book::genre, Book::withGenre)
        and : 'We create trace lists for each lens to observe different channels of the change events.'
            var titleTrace = []
            var authorTrace = []
            var genreTrace = []
            Viewable.cast(titleLens).onChange(From.ALL, it -> titleTrace << it.channel() )
            Viewable.cast(authorLens).onChange(From.VIEW, it -> authorTrace << it.channel() )
            Viewable.cast(genreLens).onChange(From.VIEW_MODEL, it -> genreTrace << it.channel() )
        expect : 'Initially, the traces are all empty.'
            titleTrace.isEmpty()
            authorTrace.isEmpty()
            genreTrace.isEmpty()

        when : """
            We first change something unrelated to the lenses by 
            updating the page count field on different channels.
        """
            bookProperty.set(From.ALL, book.withPageCount(1))
            bookProperty.set(From.VIEW, book.withPageCount(2))
            bookProperty.set(From.VIEW_MODEL, book.withPageCount(3))
            book = bookProperty.get()
        then : 'The traces should all still be empty, because the do not focus on page counts.'
            titleTrace.isEmpty() && authorTrace.isEmpty() && genreTrace.isEmpty()

        when : 'We now update the title of the book on 3 different channels...'
            bookProperty.set(From.ALL, book.withTitle("New Title 1"))
            bookProperty.set(From.VIEW, book.withTitle("New Title 2"))
            bookProperty.set(From.VIEW_MODEL, book.withTitle("New Title 3"))
            book = bookProperty.get()
        then : """
            ...only the title trace should have recorded the channel of the change events.
            Note that it received all change events. This is because the "ALL" channel
            is unique in that is always receives all change events, 
            regardless of the channel they were sent with.
        """
            titleTrace == [From.ALL, From.VIEW, From.VIEW_MODEL]
            authorTrace.isEmpty()
            genreTrace.isEmpty()

        when : 'We update the author of the book on 3 different channels...'
            bookProperty.set(From.ALL, book.withAuthor(book.author().withFirstName("Megan")))
            bookProperty.set(From.VIEW, book.withAuthor(book.author().withFirstName("Maggie")))
            bookProperty.set(From.VIEW_MODEL, book.withAuthor(book.author().withFirstName("Kyle")))
            book = bookProperty.get()
        then : """
            ...only the author trace should have recorded the channel of the change events.
            But contrary to the title trace, the author trace only recorded the VIEW 
            and the VIEW_MODEL channel, because the author lens is not listening to the ALL channel.
        """
            titleTrace == [From.ALL, From.VIEW, From.VIEW_MODEL]
            authorTrace == [From.ALL, From.VIEW]
            genreTrace.isEmpty()

        when : 'We lastly update the genre of the book on 3 different channels...'
            bookProperty.set(From.ALL, book.withGenre(Genre.FANTASY))
            bookProperty.set(From.VIEW, book.withGenre(Genre.SCIENCE))
            bookProperty.set(From.VIEW_MODEL, book.withGenre(Genre.HISTORY))
            book = bookProperty.get()
        then : """
            ...only the genre trace should have recorded the channel of the change events.
            But contrary to the title trace, the genre
            trace only recorded the VIEW_MODEL channel, because the genre lens is not 
            listening to the ALL and VIEW channels.
        """
            titleTrace == [From.ALL, From.VIEW, From.VIEW_MODEL]
            authorTrace == [From.ALL, From.VIEW]
            genreTrace == [From.ALL, From.VIEW_MODEL]
    }

    def 'Events processed by an `Observer` registered through the `subscribe` method will be invoked on all channels.'()
    {
        reportInfo """
            An `Observer` registered through the `subscribe` method will be invoked on all channels.
            This is because the `Observer` is not channel-specific and will be notified of all kinds
            of changes happening to a property lens.
        """
        given : 'We have an `Author` property and an observer that listens to changes on the property.'
            var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)
            var trace = []
        and : 'An observer which records if the change event was triggered.'
            Observer observer = { trace << "!" }
        and : 'We subscribe the observer.'
            Viewable.cast(authorProperty).subscribe(observer)

        when : 'We change three different fields on three different channels, with one no-change.'
            authorProperty.set(From.ALL, author.withFirstName("Lilo"))
            authorProperty.set(From.VIEW, author.withLastName("Schmidt"))
            authorProperty.set(From.VIEW, author.withLastName("Schmidt")) // No change
            authorProperty.set(From.VIEW_MODEL, author.withBooks(["Good Book", "Nice Book"]))
        then : 'The observer was notified of all changes.'
            trace == ["!", "!", "!"]

        when : 'We unsubscribe the observer.'
            Viewable.cast(authorProperty).unsubscribe(observer)
        and : 'Then again change three different fields on three different channels.'
            authorProperty.set(From.ALL, author.withFirstName("Jane"))
            authorProperty.set(From.ALL, author.withFirstName("Jane")) // No change
            authorProperty.set(From.VIEW, author.withLastName("Mayer"))
            authorProperty.set(From.VIEW_MODEL, author.withBooks(["Great Book", "Awesome Book"]))
        then : 'The observer was not notified of any changes.'
            trace == ["!", "!", "!"]
    }

    def 'A lens on a property an effect on a view on this same property, even if it is de-referenced.'()
    {
        reportInfo """
            A lens is essentially just a narrow view on a property.
            So when the original property is no longer referenced, the lens
            will still maintain the state of the original property.
            So this means that another view on the same property 
            will continue to be updated correctly through the 
            lens even if the link between the two is no
            longer accessible.
        """
        given : 'We create a simple property for the  `Author` record.'
            var author = new Author("Lisa", "Su", LocalDate.of(1969, 11, 7), ["Ryzen", "Epyc"])
            var authorProperty = Var.of(author)
            var authorRef = new WeakReference(authorProperty)
        and : 'We create a lens as well as a view, both focusing on the first name of the author.'
            var firstNameLens = authorProperty.zoomTo(Author::firstName, Author::withFirstName)
            var firstNameView = authorProperty.viewAsString(Author::firstName)
        and : 'We create a trace list for the view, to observe the changes from mutating the lens.'
            var trace = []
            firstNameView.onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        expect : 'The initial values are correct.'
            firstNameLens.get() == "Lisa"
            firstNameView.get() == "Lisa"
            trace.isEmpty()

        when : 'We change the first name of the author through the lens....'
            firstNameLens.set("Lisa-Marie")
        then : 'The lens and the view should have the new first name.'
            firstNameLens.get() == "Lisa-Marie"
            firstNameView.get() == "Lisa-Marie"
            trace == ["Lisa-Marie"]

        when : 'We de-reference the original property.'
            authorProperty = null
        and : 'We wait for the garbage collector to run.'
            waitForGarbageCollection()
        then : 'The author property did not get garbage collected.'
            authorRef.get() != null

        when : 'We change the first name of the author through the lens again...'
            firstNameLens.set("Lisa-Marie-Anne")
        then : 'The lens and the view should have the new first name.'
            firstNameLens.get() == "Lisa-Marie-Anne"
            firstNameView.get() == "Lisa-Marie-Anne"
            trace == ["Lisa-Marie", "Lisa-Marie-Anne"]
    }

    def 'A lens stays in memory when there is a view on it.'()
    {
        reportInfo """
            A lens is a narrowed down mutable view on a property.
            So when a view is then created on this lens and the lens
            is de-referenced, then it must be kept in memory as long
            as the source property is still in memory.
            
            This is because modifications to the source property
            must be propagated through the lens to the view.
        """
        given : 'We create a simple property for the  `Memebr` record, a lens, and a view.'
            var member = new Member("42", "Lilly", "Lemon", MembershipLevel.GOLD, LocalDate.of(2019, 7, 24), null)
            var memberProperty = Var.of(member)
            var nameLens = memberProperty.zoomTo(Member::firstName, Member::withFirstName)
            var nameView = nameLens.viewAsString(it -> it.toUpperCase())
            var weakRef = new WeakReference(nameLens)
        and : 'We create a trace list for the view, to observe the changes from mutating the lens.'
            var trace = []
            nameView.onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        expect : 'The initial values are correct.'
            nameLens.get() == "Lilly"
            nameView.get() == "LILLY"
            trace.isEmpty()

        when : 'We now de-reference the lens and wait for the garbage collector to run.'
            nameLens = null
            waitForGarbageCollection()
        then : 'The lens did not get garbage collected.'
            weakRef.get() != null

        when : 'We change the first name of the member through the view.'
            memberProperty.set(member.withFirstName("Sally"))
        then : 'The view should have the new first name, despite the lens being de-referenced.'
            nameView.get() == "SALLY"
            trace == ["SALLY"]
    }

    def 'A chain of lenses may be garbage collected, even when the source property stays in memory.'()
    {
        reportInfo """
            A chain of lenses is similar to a chain of views with the difference that
            a lens is mutable and can be used to modify the source property.
            But just like views, lenses can be garbage collected if they are no longer
            referenced anywhere except by the source property, which
            only holds a weak reference to the lenses.
        """
        given : 'A property based on the `Loan` record.'
            var loanProperty = Var.of(new Loan(
                                                    "abcd",
                                                    new Book("Average Book",
                                                        new Author(
                                                            "Margarete", "Wick",
                                                            LocalDate.of(1912, 3, 15),
                                                            ["Very Interesting Book", "Not so..."]
                                                        ),
                                                        Genre.SCIENCE,
                                                        LocalDate.of(1963, 8, 13), 932
                                                    ),
                                                    new Member(
                                                        "m1d", "Haruka", "Lee",
                                                        MembershipLevel.PLATINUM,
                                                        LocalDate.of(2005, 6, 13),
                                                        null
                                                    ),
                                                    LocalDate.of(2023, 8, 12),
                                                    LocalDate.of(2023, 9, 12),
                                                    false
                                                ))
        and : 'A chain of lenses going deeper into the nested records.'
            var lens1 = loanProperty.zoomTo(Loan::book, Loan::withBook)
            var lens2 = lens1.zoomTo(Book::author, Book::withAuthor)
            var lens3 = lens2.zoomTo(Author::firstName, Author::withFirstName)
            var weakRefLens1 = new WeakReference(lens1)
            var weakRefLens2 = new WeakReference(lens2)
            var weakRefLens3 = new WeakReference(lens3)

        when : 'We de-reference all lenses and wait for the garbage collector to run.'
            lens2 = null
            lens1 = null
            lens3 = null
            waitForGarbageCollection()
        then : 'All lenses were garbage collected.'
            weakRefLens1.get() == null
            weakRefLens2.get() == null
            weakRefLens3.get() == null
    }

    def 'A `WeakAction` is removed and garbage collected together with its owner.'()
    {
        reportInfo """
            You are not supposed to register an action directly onto a lens property.
            Instead you should use the `.view()` of the lens to register an action.
            But if you really need to register an action directly onto a property
            you may want to consider using a `WeakAction` to avoid memory leaks.
            
            A weak action is a special kind of action that has a weakly referenced "owner".
            This owner determines if the action is still alive or not and should be removed
            after the owner.
            
            Warning! Never reference the owner in the action itself, not even indirectly!
            This will effectively turn your owner and action into memory leaks.
        """
        given : 'We first create a base property for the lens:'
            var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)
        and : 'A lens and an owner:'
            var nameLens = authorProperty.zoomTo(Author::firstName, Author::withFirstName)
            var owner = new Object()
        and : 'A trace list to record the side effect.'
            var trace = []
        and : 'Finally we register a weak action on the property.'
            Viewable.cast(nameLens).onChange(From.ALL, new sprouts.impl.WeakAction(owner, (o, it) -> trace << it.currentValue().orElseThrow()))

        when : 'We change the lens...'
            nameLens.set("William")
        then : 'The side effect is executed.'
            trace == ["William"]

        when : 'We remove the owner and then wait for the garbage collector to remove the weak action.'
            owner = null
            waitForGarbageCollection()
        and : 'We change the lens again...'
            nameLens.set("Hannah")
        then : 'The side effect is not executed anymore.'
            trace == ["William"]
    }

    def 'A weak observable is removed and garbage collected together with its event listeners.'()
    {
        reportInfo """
            You are not supposed to register an observer directly onto a lens property.
            Instead you should get a `.view()` (Viewable) to register an observer.
            Here we show you how to use this method to create a weak observer.
            
            A weak observer is a special kind of observer that has a weakly referenced.
            This determines if the observer is still alive or not and if its listeners should be removed
            after it is no longer reachable (i.e. garbage collected).
        """
        given : 'We first create a base property for the lens:'
            var date = LocalDate.of(2021, 8, 12)
            var dateProperty = Var.of(date)
        and : 'A lens and an owner:'
            var monthLens = dateProperty.zoomTo(LocalDate::getMonth, (d, m) -> d.withMonth(m.getValue()))
            var weakObservable = monthLens.view()
        and : 'A trace list to record the side effect.'
            var trace = []
        and : 'Finally we register a weak observer on the property.'
            weakObservable.subscribe({trace<<"!"})

        when : 'We change the lens...'
            monthLens.set(Month.JANUARY)
        then : 'The side effect is executed.'
            trace == ["!"]

        when : 'We remove the owner and then wait for the garbage collector to remove the weak observer.'
            weakObservable = null
            waitForGarbageCollection()
        and : 'We change the lens again...'
            monthLens.set(Month.FEBRUARY)
        then : 'The side effect is not executed anymore.'
            trace == ["!"]
    }

    def 'Exceptions in the `toString()` of an item, will not cripple the `toString()` of a lens property.'()
    {
        reportInfo """
            When you call the `toString()` method on a lens property, it will
            indirectly call the `toString()` method on the item of the lens property.
            Now, if the item throws an exception in its `toString()` method,
            let's say, because of a bug in the code, then it should not affect
            the reliability of the `toString()` method of the lens property itself!
            This is because the `toString()` method of a property is meant to
            provide a human-readable representation, ans so if the control
            flow is interrupted by an exception, then the property would not
            be able to provide any information at all.
            
            If an error occurs in the `toString()` method of an item,
            then an error message will be logged to the console, and
            the string representation will tell you about the error.
        """
        given : 'We first create a new `PrintStream` that will capture the `System.err`.'
            var originalErr = System.err
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
            System.err = printStream
        and : 'A data structure that allows us to zoom into the item.'
            var data = [
                    go: "Go",
                    watch: "Watch",
                    dominion: "Dominion",
                    now: new Object() {
                        @Override public String toString() {
                            throw new RuntimeException("Heart explodes!")
                        }
                    }
            ]
        and : 'We create a property that wraps the data structure as well as a lens that focuses on the "now" field.'
            var property = Var.of(data)
            var lens = property.zoomTo( it -> it.now, (it, now) -> { it.now = now; return it } )

        when : 'We call the `toString()` method on the lens.'
            var result = lens.toString()
        then : 'The `toString()` method of the lens property does not throw an exception.'
            noExceptionThrown()
        and : 'The string representation of the lens property contains the exception message.'
            result == "Lens<>[java.lang.RuntimeException: Heart explodes!]"
        and : 'The output stream contains the exception message.'
            outputStream.toString().contains("java.lang.RuntimeException: Heart explodes!")
            outputStream.toString().contains("at ") // This is the stack trace of the exception.

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'You can remove all change listeners from a lens property using `Viewable::unsubscribeAll()`!'()
    {
        reportInfo """
            The `Viewable::unsubscribeAll()` method is used to unsubscribe all listeners
            from a property. Note that internally, every property, also implements the `Viewable` interface,
            which is why you can call this method on any property if you cast it to `Viewable`.
            Keep in mind though, that in most cases you should not cast a property to `Viewable`,
            and instead use `Var::view()` or `Val::view()` to create a view of the property.
            
            But here we are just testing the `unsubscribeAll()` method.
        """
        given : 'A mutable property with an initial value and a lens on it.'
            var property = Var.of(new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"]))
            var lens = property.zoomTo(Author::firstName, Author::withFirstName)
        and : 'A trace list to record the side effects.'
            var trace = []
        and : 'We add two change listener that will be called when the lens property changes.'
            Viewable.cast(lens).onChange(From.ALL, it -> {
                trace << "Listened to: " + it.currentValue().orElseThrow()
            })
            Viewable.cast(lens).onChange(From.ALL, it -> {
                trace << "Also listened to: " + it.currentValue().orElseThrow()
            })
        expect : 'The lens property has two change listener.'
            Viewable.cast(lens).numberOfChangeListeners() == 2

        when : 'We change the value of the lens property.'
            lens.set("Smith")
        then : 'The change listeners are notified.'
            trace == ["Listened to: Smith", "Also listened to: Smith"]

        when : 'We unsubscribe all listeners from the lens.'
            Viewable.cast(lens).unsubscribeAll()
        then : 'The lens property no longer has any change listeners.'
            Viewable.cast(lens).numberOfChangeListeners() == 0

        when : 'We change the value of the lens property again.'
            lens.set("Mayer")
        then : 'The change listeners are not notified anymore, so the trace is unchanged.'
            trace == ["Listened to: Smith", "Also listened to: Smith"]
    }

    def 'You can remove all change listeners from a lens property view using `Viewable::unsubscribeAll()`!'()
    {
        reportInfo """
            When we create a view of a lens property, we can also unsubscribe all listeners
            from the view using the `Viewable::unsubscribeAll()` method.
            In this test we create a lens property and a view of it,
            then we add two change listeners to the view, and finally we unsubscribe all listeners
            from the view using the `unsubscribeAll()` method.
        """
        given : 'A mutable property with an initial value and a lens on it, and then a view of the lens.'
            var property = Var.of(new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"]))
            var lens = property.zoomTo(Author::firstName, Author::withFirstName)
            var view = lens.view()
        and : 'A trace list to record the side effects.'
            var trace = []
        and : 'We add two change listener that will be called when the lens property changes.'
            view.onChange(From.ALL, it -> {
                trace << "Listened to: " + it.currentValue().orElseThrow()
            })
            view.onChange(From.ALL, it -> {
                trace << "Also listened to: " + it.currentValue().orElseThrow()
            })
        expect : 'The lens property view has two change listener.'
            view.numberOfChangeListeners() == 2

        when : 'We change the value of the lens property.'
            lens.set("Smith")
        then : 'The change listeners are notified.'
            trace == ["Listened to: Smith", "Also listened to: Smith"]

        when : 'We unsubscribe all listeners from the lens view.'
            view.unsubscribeAll()
        then : 'The view no longer has any change listeners.'
            view.numberOfChangeListeners() == 0

        when : 'We change the value of the lens property again.'
            lens.set("Mayer")
        then : 'The change listeners are not notified anymore, so the trace is unchanged.'
            trace == ["Listened to: Smith", "Also listened to: Smith"]
    }

    def 'You can force a change event from a source property all of its lens properties and sub-lens properties.'()
    {
        reportInfo """
            When changing the item of a regular property, then this only triggers change events
            in its views and lens properties if the value actually changed.
            However, sometimes you may want to force a change event to be fired,
            even if the value did not change, in which case the event will propagate
            to all lens properties and sub-lens properties.
            
            This can be done using the `Var::fireChange(Channel)` method.
            This method will trigger a change event on the property,
            which will then propagate to all its lens properties and sub-lens properties
            even if the value did not change.
        """
        given : 'A mutable property with an initial value and a lens on it.'
            var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), ["Book1", "Book2"])
            var authorProperty = Var.of(author)
            var firstNameLens = authorProperty.zoomTo(Author::firstName, Author::withFirstName)
            var dateLens = authorProperty.zoomTo(Author::birthDate, Author::withBirthDate)
            var yearLens = dateLens.zoomTo(LocalDate::getYear, (d, y) -> LocalDate.of(y, d.getMonth(), d.getDayOfMonth()))
        and : 'We create views for each lens to observe the change events.'
            var firstNameView = firstNameLens.view()
            var dateView = dateLens.view()
            var yearView = yearLens.view()
        and : 'A trace list to record the side effects.'
            var trace = []
            firstNameView.onChange(From.ALL, it -> trace << "First name changed to: " + it.currentValue().orElseThrow())
            dateView.onChange(From.ALL, it -> trace << "Birth date changed to: " + it.currentValue().orElseThrow())
            yearView.onChange(From.ALL, it -> trace << "Birth year changed to: " + it.currentValue().orElseThrow())

        when : 'We change a meaningless property in the author property.'
            authorProperty.set(authorProperty.get().withBooks(["Book1", "Book2"])) // No actual change
        then : 'No change events are fired.'
            trace.isEmpty()

        when : 'We now force a change event from the source property.'
            authorProperty.fireChange(From.ALL)
        then : 'Change events are fired for all lens properties and sub-lens properties.'
            trace == [
                    "First name changed to: John",
                    "Birth year changed to: 1829",
                    "Birth date changed to: 1829-08-12"
                ]
    }

    def 'The nullable lens of a nullable property handles `null` values gracefully.'() {
        given :
            var member = new Member("24", "Eddie", "England", MembershipLevel.BASIC, LocalDate.of(2013, 2, 19), null)
            var memberProperty = Var.ofNullable(Member.class, member)
            var level = memberProperty.zoomTo(Member::membershipLevel, Member::withMembershipLevel)
            var date = memberProperty.zoomToNullable(LocalDate.class, Member::joinDate, Member::withJoinDate)
            var name = memberProperty.zoomTo("", Member::firstName, Member::withFirstName)
        expect :
            level.get() == MembershipLevel.BASIC
            date.get() == LocalDate.of(2013, 2, 19)
            name.get() == "Eddie"

        when :
            memberProperty.set(null)
        then :
            level.orElseNull() == null
            date.orElseNull() == null
            name.get() == ""
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
