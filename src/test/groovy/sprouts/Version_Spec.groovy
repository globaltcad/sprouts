package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

@Title("Value based Identity")
@Narrative('''

    When doing functional or data oriented programming then you are
    working with value objects. These are objects that are immutable
    and define their identity in terms of their values.
    Which means that two objects are considered equal if they have
    the same values, even if they are different instances.
    
    To model identity in value objects Sprouts provides the `Version` class.
    It is a value object representing a unique ID. This ID consisting
    of two numbers, a lineage and succession number allowing you to identify something
    and also determine the order in which something was created
    and updated.
    This is intended to be used to emulate identity in
    your value objects, which is useful for tracking changes
    and synchronizing state across different parts of your application.
    You may also use version changes to do reactive programming
    for your value based view models, similar as you would do with
    `Event`s in traditional place oriented view models.

''')
class Version_Spec extends Specification
{
    def 'Two newly created versions have different lineage numbers but the same succession.'() {
        given:
            var version1 = Version.create()
            var version2 = Version.create()
        expect:
            version1.lineage() != version2.lineage()
            version1.succession() == version2.succession()

        when : 'We create two successions for each version...'
            var nextVersion1 = version1.next()
            var nextVersion2 = version2.next()
        then : '...then the same principle applies.'
            nextVersion1.lineage() != nextVersion2.lineage()
            nextVersion1.succession() == nextVersion2.succession()
    }

    def 'Two versions created from the same lineage have different succession numbers.'() {
        given:
            var version1 = Version.create()
            var version2 = version1.next()
            var version3 = version2.next()
            var version4 = version3.next()
        expect : 'They all share the same lineage number:'
            version1.lineage() == version2.lineage()
            version1.lineage() == version3.lineage()
            version1.lineage() == version4.lineage()
        and : 'Their are neither equal nor have the same succession numbers:'
            version1 != version2
            version1 != version3
            version1 != version4
            version2 != version3
            version2 != version4
            version3 != version4
            version1.succession() != version2.succession()
            version1.succession() != version3.succession()
            version1.succession() != version4.succession()
    }

    def 'A lineage of successions can be ordered.'() {
        given:
            var version1 = Version.create()
            var version2 = version1.next()
            var version3 = version2.next()
            var version4 = version3.next()
            var unOrderedVersions = [version2, version1, version4, version3]
        when :
            var orderedVersions1 = unOrderedVersions.toSorted( (a, b) -> a.isSuccessorOf(b) ? 1 : -1 )
        then:
            orderedVersions1 == [version1, version2, version3, version4]

        when :
            var orderedVersions2 = unOrderedVersions.toSorted( (a, b) -> a.isPredecessorOf(b) ? 1 : -1 )
        then:
            orderedVersions2 == [version4, version3, version2, version1]
    }

    def 'Use `isDirectSuccessorOf` to check if a version is the direct successor of another.'() {
        given:
            var version1 = Version.create()
            var version2 = version1.next()
            var version3 = version2.next()
            var version4 = version3.next()
        expect:
            !version1.isDirectSuccessorOf(version2)
            version2.isDirectSuccessorOf(version1)
            !version2.isDirectSuccessorOf(version3)
            version3.isDirectSuccessorOf(version2)
            !version3.isDirectSuccessorOf(version4)
            version4.isDirectSuccessorOf(version3)
    }

    def 'Use `isDirectPredecessorOf` to check if a version is the direct predecessor of another.'() {
        given:
            var version1 = Version.create()
            var version2 = version1.next()
            var version3 = version2.next()
            var version4 = version3.next()
        expect:
            version1.isDirectPredecessorOf(version2)
            !version2.isDirectPredecessorOf(version1)
            version2.isDirectPredecessorOf(version3)
            !version3.isDirectPredecessorOf(version2)
            version3.isDirectPredecessorOf(version4)
            !version4.isDirectPredecessorOf(version3)
    }
}
