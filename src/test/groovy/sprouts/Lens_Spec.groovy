package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.LocalDate

@Title("Functional Lenses")
@Narrative('''

    The `Lens` interface defines an access and update operation on an individual 
    part of a nested and immutable data structure. 
    A lens can also be composed with other lenses to focus 
    on more deeply nested parts.
    This design concept is part of the functional 
    programming paradigm, and it emulates
    mutable properties (getter and setter) in an immutable world 
    using a *getter* and more importantly a *wither* methods 
    (a function that returns a new structure with target field being updated).

    In this specification we demonstrate how to implement and use
    your own lenses through the `Lens` interface and then
    we show how these lenses can be composed to focus on deeply nested parts.
    
    We use the following data structures to demonstrate the concept:
    ```
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
    ```
''')
@Subject([Lens])
class Lens_Spec extends Specification
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


    def 'You can create lenses from other lenses to dive deeper into nested data structures.'()
    {
        given: """
            We first create full data structure composed of many nested records.
        """
            var author = new Author("Raffaela", "Raab", LocalDate.of(1996, 3, 21), ["Book1", "Book2"])
            var book   = new Book("The Book", author, Genre.HISTORY, LocalDate.of(2019, 5, 12), 304)
            var member = new Member("1234", "Marc", "Mayer", MembershipLevel.GOLD, LocalDate.of(2015, 2, 3), null)
            var loan   = new Loan("5678", book, member, LocalDate.of(2021, 8, 12), LocalDate.of(2021, 9, 12), false)

        and: """
            Create lenses for nested records and also trace lists for recording
            state changes of the original records and all of its lenses.
        """
            var bookLens = Lens.of(Loan::book, Loan::withBook)
            var memberLens = Lens.of(Loan::member, Loan::withMember)
            var authorLens = Lens.of(Book::author, Book::withAuthor)
        expect: """
            The lenses can fetch the nested records from the original records.
        """
            bookLens.getter(loan) == book
            memberLens.getter(loan) == member
            authorLens.getter(book) == author

        when: """
            We create updated versions of the original records using the lenses.
        """
            var newBook = authorLens.wither(book, author.withFirstName("Ed").withLastName("Winters"))
            var newLoan = memberLens.wither(loan, member.withFirstName("Peter").withLastName("Singer"))
        then: """
            The updated records are different from the original records.
        """
            newBook != book
            newLoan != loan
        and :
            newBook.author.firstName == "Ed"
            newBook.author.lastName == "Winters"
            newLoan.member.firstName == "Peter"
            newLoan.member.lastName == "Singer"
        and :
            newBook.author.firstName != author.firstName
            newBook.author.lastName != author.lastName
            newLoan.member.firstName != member.firstName
            newLoan.member.lastName != member.lastName
    }

    def 'You can compose lenses to update a deeply nested part of a data structure.'()
    {
        given: """
            We first create full data structure composed of many nested records.
        """
            var author = new Author("Ed", "Winters", LocalDate.of(1636, 5, 21), ["Book1", "Book2"])
            var book   = new Book("The Book", author, Genre.HISTORY, LocalDate.of(1849, 2, 6), 342)
            var member = new Member("5432", "Marc", "Mayer", MembershipLevel.PLATINUM, LocalDate.of(2015, 2, 3), "test@mail.net")
            var loan   = new Loan("7247", book, member, LocalDate.of(2002, 1, 4), LocalDate.of(1942, 5, 23), true)
        and : 'We create lenses for deeply nested records.'
            var bookLens = Lens.of(Loan::book, Loan::withBook)
            var authorLens = Lens.of(Book::author, Book::withAuthor)
            var nameLens = Lens.of(Author::firstName, Author::withFirstName)
        and : 'We compose the lenses to focus on deeply nested parts.'
            var bookToAuthorNameLens = bookLens.to(authorLens).to(nameLens)

        expect : 'The lens can fetch the correct deeply nested part from the original record.'
            bookToAuthorNameLens.getter(loan) == "Ed"
        when : 'We create an updated version of the original record using the composed lens.'
            var newLoan = bookToAuthorNameLens.wither(loan, "Raffaela")
        then : 'The updated record is different from the original record.'
            newLoan != loan
        and : 'The updated record has the correct deeply nested part updated.'
            newLoan.book.author.firstName == "Raffaela"
    }

    def 'Lens.across allows bulk transformation of tuples of homogeneous data'() {
        given: 'a tuple of authors with various first names'
            var alice = new Author("Alice", "Archer", LocalDate.of(1990, 1, 1), ["The Void"])
            var bob = new Author("Bob", "Builder", LocalDate.of(1985, 2, 2), ["Construction 101"])
            var authors = Tuple.of(alice, bob)

        and: 'a lens focusing on first names across the entire tuple'
            var firstNameLens = Lens.across(
                String.class,
                Author::firstName,
                Author::withFirstName
            )

        when: 'extracting first names using the lens'
            var extractedNames = firstNameLens.getter(authors)

        then: 'the names are captured in a type-correct tuple'
            extractedNames == Tuple.of("Alice", "Bob")
            extractedNames.type() == String.class

        when: 'performing a bulk update with matching tuple size'
            var updatedAuthors = firstNameLens.wither(authors, Tuple.of("Ada", "Blake"))

        then: 'new tuple contains authors with updated names'
            updatedAuthors.get(0).firstName == "Ada"
            updatedAuthors.get(1).firstName == "Blake"

        and: 'original tuple remains unchanged'
            authors.get(0).firstName == "Alice"
            authors.get(1).firstName == "Bob"

        when: 'attempting update with mismatched tuple size'
            firstNameLens.wither(authors, Tuple.of("Xavier"))

        then: 'operation fails with descriptive exception'
            thrown(IllegalArgumentException)
    }

    def 'Lens.across handles edge cases with empty and single-element tuples'() {
        given: 'an empty author tuple'
            var emptyAuthors = Tuple.of(Author.class)

        and: 'single-element author tuple'
            var soloAuthor = Tuple.of(new Author("Solo", "Artist", LocalDate.now(), []))

        and: 'lens configured for first name operations'
            var nameLens = Lens.across(
                String.class,
                Author::firstName,
                Author::withFirstName
            )

        when: 'operating on empty tuple'
            var emptyNames = nameLens.getter(emptyAuthors)
            var updatedEmpty = nameLens.wither(emptyAuthors, Tuple.of(String.class))

        then: 'getter returns empty typed tuple'
            emptyNames.isEmpty()
            emptyNames.type() == String.class

        and: 'wither preserves empty structure'
            updatedEmpty.isEmpty()
            updatedEmpty.type() == Author.class

        when: 'operating on single-element tuple'
            var singleName = nameLens.getter(soloAuthor)
            var updatedSolo = nameLens.wither(soloAuthor, Tuple.of("Updated"))

        then: 'bulk operations work atomically'
            singleName == Tuple.of("Solo")
            updatedSolo.get(0).firstName == "Updated"
    }

    def 'Lens.across maintains element order during bulk operations'() {
        given: 'tuple with ordered elements'
            var authors = Tuple.of(
                new Author("First", "1", LocalDate.now(), []),
                new Author("Second", "2", LocalDate.now(), []),
                new Author("Third", "3", LocalDate.now(), [])
            )

        and: 'positionally aware lens'
            var lens = Lens.across(String.class, Author::lastName, Author::withLastName)

        when: 'performing staggered updates'
            var updated = lens.wither(authors, Tuple.of("A", "B", "C"))

        then: 'new values appear in original positions'
            updated.get(0).lastName == "A"
            updated.get(1).lastName == "B"
            updated.get(2).lastName == "C"
    }
}
