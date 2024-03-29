package org.biojava3.core.sequence.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.biojava3.core.sequence.AccessionID;
import org.biojava3.core.sequence.Strand;
import org.biojava3.core.sequence.template.Compound;
import org.biojava3.core.sequence.template.CompoundSet;
import org.biojava3.core.sequence.template.ProxySequenceReader;
import org.biojava3.core.sequence.template.Sequence;
import org.biojava3.core.sequence.template.SequenceMixin;
import org.biojava3.core.sequence.template.SequenceView;

/**
 * This reader actually proxies onto multiple types of sequence in order
 * to allow a number of sequence objects to act as if they are one sequence.
 * The code takes in any number of sequences, records the minimum and maximum
 * bounds each sequence covers with respect to 1 position indexing and then
 * binary searches these when a position is requested. Because of this
 * 0 length Sequences are excluded during construction.
 *
 * Performance is not as good as if you are using a flat sequence however the
 * speed of lookup is more than adaquate for most situations. Using the iterator
 * gives the best performance as this does not rely on the binary search
 * mechanism instead iterating through each sequence in turn.
 *
 * @author ayates
 * @param <C> Tyoe of compound to hold
 */
public class JoiningSequenceReader<C extends Compound> implements ProxySequenceReader<C> {

    /**
     * Internal implementation flag and controls how we look for the right
     * sub-sequence
     */
    private static final boolean BINARY_SEARCH = true;

    private final List<Sequence<C>> sequences;
    private int[] maxSequenceIndex;
    private int[] minSequenceIndex;

    public JoiningSequenceReader(Sequence<C>... sequences) {
        this(Arrays.asList(sequences));
    }

    public JoiningSequenceReader(List<Sequence<C>> sequences) {
        List<Sequence<C>> seqs = new ArrayList<Sequence<C>>();
        for(Sequence<C> s: sequences) {
            if(s.getLength() != 0) {
                seqs.add(s);
            }
        }
        this.sequences = seqs;
    }

    @Override
    public C getCompoundAt(int position) {
        int sequenceIndex = getSequenceIndex(position);
        Sequence<C> sequence = sequences.get(sequenceIndex);
        int indexInSequence = (position - getMinSequenceIndex()[sequenceIndex]) + 1;
        return sequence.getCompoundAt(indexInSequence);
    }

    @Override
    public CompoundSet<C> getCompoundSet() {
        return sequences.get(0).getCompoundSet();
    }

    @Override
    public int getLength() {
        int[] maxSeqIndex = getMaxSequenceIndex();
        return maxSeqIndex[maxSeqIndex.length - 1];
    }

    /**
     * Returns which Sequence holds the position queried for
     */
    private int getSequenceIndex(int position) {
        if (BINARY_SEARCH) {
            return binarySearch(position);
        } else {
            return linearSearch(position);
        }
    }

    private int[] getMinSequenceIndex() {
        if (minSequenceIndex == null) {
            initSeqIndexes();
        }
        return minSequenceIndex;
    }

    private int[] getMaxSequenceIndex() {
        if (maxSequenceIndex == null) {
            initSeqIndexes();
        }
        return maxSequenceIndex;
    }

    private void initSeqIndexes() {
        minSequenceIndex = new int[sequences.size()];
        maxSequenceIndex = new int[sequences.size()];
        int currentMaxIndex = 0;
        int currentMinIndex = 1;
        int lastLength = 0;
        for (int i = 0; i < sequences.size(); i++) {
            currentMinIndex += lastLength;
            currentMaxIndex += sequences.get(i).getLength();
            minSequenceIndex[i] = currentMinIndex;
            maxSequenceIndex[i] = currentMaxIndex;
            lastLength = sequences.get(i).getLength();
        }
    }

    /**
     * Scans through the sequence index arrays in linear time. Not the best
     * performance but easier to code
     */
    private int linearSearch(int position) {
        int[] minSeqIndex = getMinSequenceIndex();
        int[] maxSeqIndex = getMaxSequenceIndex();
        int length = minSeqIndex.length;
        for (int i = 0; i < length; i++) {
            if (position >= minSeqIndex[i] && position <= maxSeqIndex[i]) {
                return i;
            }
        }
        throw new IndexOutOfBoundsException("Given position " + position + " does not map into this Sequence");
    }

    /**
     * Scans through the sequence index arrays using binary search
     */
    private int binarySearch(int position) {
        int[] minSeqIndex = getMinSequenceIndex();
        int[] maxSeqIndex = getMaxSequenceIndex();

        int low = 0;
        int high = minSeqIndex.length - 1;
        while (low <= high) {
            //Go to the mid point in the array
            int mid = (low + high) >>> 1;

            //Get the max position represented by this Sequence
            int midMinPosition = minSeqIndex[mid];
            int midMaxPosition = maxSeqIndex[mid];

            //if current position is greater than the current bounds then
            //increase search space
            if (midMinPosition < position && midMaxPosition < position) {
                low = mid + 1;
            } //if current position is less than current bounds then decrease
            //search space
            else if (midMinPosition > position && midMaxPosition > position) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        throw new IndexOutOfBoundsException("Given position " + position + " does not map into this Sequence");
    }

    /**
     * Iterator implementation which attempts to move through the 2D structure
     * attempting to skip onto the next sequence as & when it is asked to
     */
    @Override
    public Iterator<C> iterator() {
        final List<Sequence<C>> localSequences = sequences;
        return new Iterator<C>() {

            private Iterator<C> currentSequenceIterator = null;
            private int currentPosition = 0;

            @Override
            public boolean hasNext() {
                //If the current iterator is null then see if the Sequences object has anything
                if (currentSequenceIterator == null) {
                    return ! localSequences.isEmpty();
                }

                //See if we had any compounds
                boolean hasNext = currentSequenceIterator.hasNext();
                if (!hasNext) {
                    hasNext = currentPosition < sequences.size();
                }
                return hasNext;
            }

            @Override
            public C next() {
                if(currentSequenceIterator == null) {
                    if(localSequences.isEmpty())
                        throw new NoSuchElementException("No sequences to iterate over; make sure you call hasNext() before next()");
                    currentSequenceIterator = localSequences.get(currentPosition).iterator();
                    currentPosition++;
                }
                if(!currentSequenceIterator.hasNext()) {
                    currentSequenceIterator = localSequences.get(currentPosition).iterator();
                    currentPosition++;
                }
                return currentSequenceIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove from this Sequence");
            }
        };
    }

    @Override
    public void setCompoundSet(CompoundSet<C> compoundSet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContents(String sequence) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int countCompounds(C... compounds) {
        return SequenceMixin.countCompounds(this, compounds);
    }

    @Override
    public AccessionID getAccession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<C> getAsList() {
        return SequenceMixin.toList(this);
    }

    @Override
    public int getIndexOf(C compound) {
        return SequenceMixin.indexOf(this, compound);
    }

    @Override
    public int getLastIndexOf(C compound) {
        return SequenceMixin.lastIndexOf(this, compound);
    }

    @Override
    public String getSequenceAsString() {
        return SequenceMixin.toStringBuilder(this).toString();
    }

    @Override
    public SequenceView<C> getSubSequence(Integer start, Integer end) {
        return SequenceMixin.createSubSequence(this, start, end);
    }

    @Override
    public String getSequenceAsString(Integer start, Integer end, Strand strand) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
