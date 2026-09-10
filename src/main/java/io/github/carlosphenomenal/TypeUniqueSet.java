package io.github.carlosphenomenal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A collection that holds at most one element per <em>runtime type</em>.
 *
 * <p>Unlike a normal {@link java.util.Set}, which decides uniqueness using
 * {@code equals()}/{@code hashCode()}, a {@code TypeUniqueSet} decides
 * uniqueness using {@link Object#getClass()}. In other words: you can never
 * have two elements of the exact same class stored at once, regardless of
 * whether those two elements are {@code equal()} to each other or not.
 *
 * <p><strong>Note on subtyping:</strong> uniqueness is based on the
 * <em>exact</em> runtime class, not on inheritance. So if {@code Dog} and
 * {@code Cat} both extend {@code Animal}, you can store one {@code Dog} and
 * one {@code Cat} at the same time — they occupy different "slots" — but you
 * cannot store two different {@code Dog} instances. Looking up by
 * {@code Animal.class} will not return a stored {@code Dog}; you must look
 * it up by its exact class.
 *
 * <p>This implementation is backed by a {@link HashMap} and is
 * <strong>not thread-safe</strong>. If concurrent access is required,
 * external synchronization is needed, or the backing map should be swapped
 * for a {@link java.util.concurrent.ConcurrentHashMap}.
 *
 * <p>{@code null} elements are not permitted.
 *
 * @param <T> the upper bound type for elements stored in this set
 */
public class TypeUniqueSet<T> implements Iterable<T> {

    private final Map<Class<? extends T>, T> map = new HashMap<>();
    private boolean immutable = false;

    /**
     * Creates an immutable {@code TypeUniqueSet} containing the given
     * elements.
     *
     * <p>Each element is keyed by its exact runtime type, exactly as with
     * {@link #add(Object)}. The returned set cannot be modified afterwards:
     * calls to {@link #add(Object)}, {@link #remove(Object)},
     * {@link #remove(Class)}, or {@link #clear()} will throw
     * {@link UnsupportedOperationException}.
     *
     * @param elements the elements to populate the set with; must not
     *                  contain {@code null}, and must not contain two
     *                  elements of the same exact runtime type
     * @param <T> the upper bound type for elements stored in the set
     * @return an immutable {@code TypeUniqueSet} containing the given
     *         elements
     * @throws IllegalArgumentException if {@code elements} contains
     *         {@code null}, or contains two elements of the same exact
     *         runtime type
     */
    @SafeVarargs
    public static <T> TypeUniqueSet<T> of(T... elements) {
        TypeUniqueSet<T> set = new TypeUniqueSet<>();
        for (T element : elements) {
            if (element == null) {
                throw new IllegalArgumentException("TypeUniqueSet.of(...) does not permit null elements");
            }
            if (!set.add(element)) {
                throw new IllegalArgumentException(
                        "TypeUniqueSet.of(...) received more than one element of type "
                                + element.getClass().getName());
            }
        }
        set.immutable = true;
        return set;
    }

    /**
     * Throws {@link UnsupportedOperationException} if this set was created
     * via {@link #of(Object[])} and is therefore immutable.
     */
    private void checkMutable() {
        if (immutable) {
            throw new UnsupportedOperationException("This TypeUniqueSet is immutable");
        }
    }

    /**
     * Adds the given element to this set, keyed by its exact runtime type.
     *
     * <p>If an element of the same runtime type is already present, this
     * set is left unchanged and the existing element is <em>not</em>
     * replaced.
     *
     * @param element the element to add; must not be {@code null}
     * @return {@code true} if no element of this type was already present
     *         (i.e. the element was added); {@code false} if an element of
     *         this exact type already existed, or if {@code element} is
     *         {@code null}
     * @throws UnsupportedOperationException if this set is immutable (see
     *         {@link #of(Object[])})
     */
    public boolean add(T element) {
        checkMutable();
        if (element == null) return false;

        @SuppressWarnings("unchecked")
        Class<? extends T> type = (Class<? extends T>) element.getClass();

        return map.putIfAbsent(type, element) == null;
    }

    /**
     * Removes the element of the given exact type, if present.
     *
     * @param type the exact type of the element to remove
     * @return the removed element, or {@code null} if no element of that
     *         type was present
     * @throws UnsupportedOperationException if this set is immutable (see
     *         {@link #of(Object[])})
     */
    public T remove(Class<? extends T> type) {
        checkMutable();
        return map.remove(type);
    }

    /**
     * Removes the given element from this set, if it is currently the
     * element stored for its runtime type.
     *
     * <p>This compares by runtime type, not by {@code equals()}. If a
     * different instance of the same type currently occupies that slot,
     * that instance is removed instead — mirroring the type-based identity
     * used by {@link #add(Object)}.
     *
     * @param element the element to remove; {@code null} is ignored
     * @return {@code true} if an element of this element's runtime type was
     *         present and was removed
     * @throws UnsupportedOperationException if this set is immutable (see
     *         {@link #of(Object[])})
     */
    public boolean remove(T element) {
        checkMutable();
        if (element == null) return false;

        @SuppressWarnings("unchecked")
        Class<? extends T> type = (Class<? extends T>) element.getClass();

        return map.remove(type) != null;
    }

    /**
     * Returns the element stored for the given exact type.
     *
     * @param type the exact type to look up
     * @return the element of that type, or {@code null} if none is present
     */
    public <S extends T> S get(Class<S> type) {
        @SuppressWarnings("unchecked")
        S result = (S) map.get(type);
        return result;
    }

    /**
     * Returns whether an element of the given exact type is present.
     *
     * @param type the exact type to check
     * @return {@code true} if an element of that type is present
     */
    public boolean containsType(Class<? extends T> type) {
        return map.containsKey(type);
    }

    /**
     * Returns whether this set contains an element of the same runtime
     * type as the given element.
     *
     * @param element the element whose type to check for; {@code null}
     *                always returns {@code false}
     * @return {@code true} if an element of that runtime type is present
     */
    public boolean contains(T element) {
        if (element == null) return false;
        return map.containsKey(element.getClass());
    }

    /**
     * Returns the number of elements currently stored (equivalently, the
     * number of distinct types represented).
     *
     * @return the size of this set
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns whether this set contains no elements.
     *
     * @return {@code true} if this set is empty
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Removes all elements from this set.
     *
     * @throws UnsupportedOperationException if this set is immutable (see
     *         {@link #of(Object[])})
     */
    public void clear() {
        checkMutable();
        map.clear();
    }

    /**
     * Returns an unmodifiable view of the elements in this set.
     *
     * @return an unmodifiable collection of the elements currently stored
     */
    public Collection<T> values() {
        return Collections.unmodifiableCollection(map.values());
    }

    /**
     * Returns a sequential {@link java.util.stream.Stream} over the
     * elements in this set.
     *
     * @return a stream of the elements currently stored
     */
    public java.util.stream.Stream<T> stream() {
        return values().stream();
    }

    /**
     * Returns an iterator over the elements in this set.
     *
     * <p>The returned iterator does not support {@link Iterator#remove()};
     * use {@link #remove(Class)} or {@link #remove(Object)} instead.
     *
     * @return an iterator over the elements in this set
     */
    @Override
    public Iterator<T> iterator() {
        return values().iterator();
    }

    /**
     * Returns a string representation of this set, listing the runtime
     * types currently present.
     *
     * @return a string representation of this set
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TypeUniqueSet[");
        Iterator<Class<? extends T>> it = map.keySet().iterator();
        while (it.hasNext()) {
            sb.append(it.next().getSimpleName());
            if (it.hasNext()) sb.append(", ");
        }
        return sb.append(']').toString();
    }
}