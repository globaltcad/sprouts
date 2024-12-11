package sprouts.impl;

import java.util.Optional;

public interface TupleDiffOwner
{
    Optional<TupleDiff> differenceFromPrevious();
}
