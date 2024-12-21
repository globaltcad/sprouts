package sprouts;

/**
 *  This is a value object representing a unique ID consisting
 *  of two numbers, a lineage and succession, allowing you to identify something
 *  and also determine the order in which something was created
 *  and updated. <br>
 *  This is intended to be used to emulate identity in
 *  your value objects, which is useful for tracking changes
 *  and synchronizing state across different parts of your application.<br>
 *  You may also use version changes to do reactive programming
 *  for your value based view models, similar as you would do with
 *  {@link Event}s in traditional place oriented view models.<br>
 *  Consider the following example:<br>
 *  <b>View Model:</b>
 *  <pre>{@code
 *    public record SearchViewModel(
 *        String searchKey,
 *        Tuple<String> searchResult,
 *        String selection,
 *        Version closeSearch
 *    ) {
 *        public SearchViewModel doSearch(String searchKey) {
 *          var newSearchResults = Database.search(searchKey);
 *          return new SearchViewModel(searchKey, newSearchResults, selection, closeSearch);
 *        }
 *        public SearchViewModel withSelection(String selection) {
 *          var currentCloseSearch = closeSearch;
 *          if ( !selection.isBlank() && this.searchResult.contains(selection) ) {
 *              currentCloseSearch = currentCloseSearch.next();
 *          }
 *          return new SearchViewModel(searchKey, searchResult, selection, currentCloseSearch);
 *        }
 *    }
 *  }</pre>
 *  <b>View:</b>
 *  <pre>{@code
 *    public final class SearchView {
 *        Var<String> searchKey;
 *        Var<String> selection;
 *        Viewable<Tuple<String>> searchResult;
 *        Observable closeSearch;
 *
 *        public static void main(String[] args) {
 *            var search = new SearchViewModel("", Tuple.of(String.class), "", Version.create());
 *            var applicationState = Var.of(search);
 *            var view = new SearchView(applicationState);
 *            // show view...
 *        }
 *
 *        public SearchView(Var<SearchViewModel> vm) {
 *            this.searchKey    = vm.zoomTo(SearchViewModel::searchKey, SearchViewModel::doSearch);
 *            this.selection    = vm.zoomTo(SearchViewModel::selection, SearchViewModel::withSelection);
 *            this.searchResult = vm.view(SearchViewModel::searchResult);
 *            this.closeSearch  = vm.view(SearchViewModel::closeSearch);
 *
 *            // build GUI and bind to properties...
 *
 *            closeSearch.subscribe( () -> {
 *                // close search view...
 *            })
 *        }
 *    }
 *  }</pre>
 *  <p>
 *  In this example, the {@link Version} object is used to send a signal
 *  to the view when the selection is set to a valid value.
 *  We are using the state changes of the {@link Version} object
 *  to trigger an action in the view, just like you would with an {@link Event}.
 *  The difference here is that the {@link Version} object is a value object
 *  and so you are not forced to write place oriented code, but can instead
 *  write value/data oriented code, which is substantially easier to test and
 *  reason about.
 */
public final class Version
{
    private static long _instanceCount = 0;

    private final long _lineage;
    private final long _succession;

    private Version( long lineageId, long successionId ) {
        _lineage = lineageId;
        _succession = successionId;
    }

    /**
     *  Creates a unique version object with a new lineage and a succession
     *  number of zero. This is the first version of a new lineage.
     *  You can create successor versions through the {@link #next()} method,
     *  which will increment the succession number by one.
     *
     * @return A new {@link Version} object with a new lineage and a succession number of zero.
     */
    public static Version create() {
        return new Version( ++_instanceCount, 0 );
    }

    /**
     *  Creates a new version object with the same lineage as this one,
     *  but with a succession number incremented by one.
     *  <b>
     *      This means that the new version may not necessarily be
     *      unique, as it is possible to create multiple {@link Version}
     *      objects with the same lineage and succession number
     *      by calling this method multiple times on the same instance.
     *  </b>
     *  If you want to create a new unique version, then you should
     *  call the {@link #create()} method instead.
     *
     * @return A new {@link Version} object with the same lineage and a succession number incremented by one.
     */
    public Version next() {
        return new Version( _lineage, _succession + 1 );
    }

    /**
     *  Creates a new version object with the same lineage as this one,
     *  but with a succession number decremented by one.
     *  <b>
     *      This means that the new version may not necessarily be
     *      unique, as it is possible to create multiple {@link Version}
     *      objects with the same lineage and succession number
     *      by calling this method multiple times on the same instance.
     *  </b>
     *  If you want to create a new unique version, then you should
     *  call the {@link #create()} method instead.
     *
     * @return A new {@link Version} object with the same lineage and a succession number decremented by one.
     */
    public Version previous() {
        return new Version( _lineage, _succession - 1 );
    }

    /**
     *  Exposes the lineage number of this version object,
     *  which is a unique identifier that originates from the
     *  {@link #create()} method.
     *  Successions of this version object will have the same lineage number.
     *
     * @return The lineage of this version object.
     */
    public long lineage() {
        return _lineage;
    }

    /**
     *  Exposes the succession number of this version object,
     *  which is a non-unique that is incremented or decremented
     *  by the {@link #next()} or {@link #previous()} methods.
     *
     * @return The succession of this version object.
     */
    public long succession() {
        return _succession;
    }

    /**
     *  Determines if this version object is a direct successor of another version object.
     *  This is the case if the lineage of this version object is the same as the
     *  lineage of the other version object and the succession of this version object
     *  is exactly one greater than the succession of the other version object.
     *
     * @param other The other version object to compare this version object to.
     * @return True if this version object is a successor of the other version object, false otherwise.
     */
    public boolean isDirectSuccessorOf( Version other ) {
        long otherLineage = other.lineage();
        long otherSuccession = other.succession();
        return _lineage == otherLineage && _succession == otherSuccession + 1;
    }

    /**
     *  Determines if this version object is a successor of another version object.
     *  This is the case if the lineage of this version object is the same as the
     *  lineage of the other version object and the succession of this version object
     *  is greater than the succession of the other version object.
     *
     * @param other The other version object to compare this version object to.
     * @return True if this version object is a successor of the other version object, false otherwise.
     */
    public boolean isSuccessorOf( Version other ) {
        long otherLineage = other.lineage();
        long otherSuccession = other.succession();
        return _lineage == otherLineage && _succession > otherSuccession;
    }

    /**
     *  Determines if this version object is a direct predecessor of another version object.
     *  This is the case if the lineage of this version object is the same as the
     *  lineage of the other version object and the succession of this version object
     *  is exactly one less than the succession of the other version object.
     *
     * @param other The other version object to compare this version object to.
     * @return True if this version object is a predecessor of the other version object, false otherwise.
     */
    public boolean isDirectPredecessorOf( Version other ) {
        long otherLineage = other.lineage();
        long otherSuccession = other.succession();
        return _lineage == otherLineage && _succession == otherSuccession - 1;
    }

    /**
     *  Determines if this version object is a predecessor of another version object.
     *  This is the case if the lineage of this version object is the same as the
     *  lineage of the other version object and the succession of this version object
     *  is less than the succession of the other version object.
     *
     * @param other The other version object to compare this version object to.
     * @return True if this version object is a predecessor of the other version object, false otherwise.
     */
    public boolean isPredecessorOf( Version other ) {
        long otherLineage = other.lineage();
        long otherSuccession = other.succession();
        return _lineage == otherLineage && _succession < otherSuccession;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"[" +
                    "lineage="+ _lineage +", " +
                    "succession="+ _succession +
                "]";
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() ) {
            return false;
        }
        Version version = (Version) obj;
        return _lineage == version._lineage &&
               _succession == version._succession;
    }

    @Override
    public int hashCode() {
        return Long.hashCode( _lineage * 31 + _succession);
    }

}
