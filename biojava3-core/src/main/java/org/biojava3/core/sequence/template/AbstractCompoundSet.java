package org.biojava3.core.sequence.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.biojava3.core.exceptions.CompoundNotFoundError;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

/**
 *
 * @author Andy Yates
 *
 * @param <C> The compound this set will contain
 */
public abstract class AbstractCompoundSet<C extends Compound> implements CompoundSet<C> {

  private Map<CharSequence, C> charSeqToCompound = new HashMap<CharSequence, C>();
  private int maxCompoundCharSequenceLength = -1;
  private SetMultimap<C, C> equivalentsMap = HashMultimap.create();

  protected void addCompound(C compound, C lowerCasedCompound, Iterable<C> equivalents) {
    addCompound(compound);
    addCompound(lowerCasedCompound);

    addEquivalent(compound, lowerCasedCompound);
    addEquivalent(lowerCasedCompound, compound);

    for(C equivalent: equivalents) {
      addEquivalent(compound, equivalent);
      addEquivalent(equivalent, compound);
      addEquivalent(lowerCasedCompound, equivalent);
      addEquivalent(equivalent, lowerCasedCompound);
    }
  }

  protected void addCompound(C compound, C lowerCasedCompound, C... equivalents) {
    List<C> equiv = new ArrayList<C>(equivalents.length);
    for(C c: equivalents) {
      equiv.add(c);
    }
    addCompound(compound, lowerCasedCompound, equiv);
  }

  protected void addEquivalent(C compound, C equivalent) {
    equivalentsMap.put(compound, equivalent);
  }

  protected void addCompound(C compound) {
    charSeqToCompound.put(compound.toString(), compound);
    maxCompoundCharSequenceLength = -1;
  }

  public String getStringForCompound(C compound) {
    return compound.toString();
  }

  public C getCompoundForString(String string) {
    if(string == null) {
      throw new IllegalArgumentException("Given a null CharSequence to process");
    }

    if (string.length()==0) {
      return null;
    }

    if (string.length() > getMaxSingleCompoundStringLength()) {
      throw new IllegalArgumentException("CharSequence supplied is too long.");
    }

    return charSeqToCompound.get(string);
  }

  public int getMaxSingleCompoundStringLength() {
    if(maxCompoundCharSequenceLength == -1) {
      for(C compound: charSeqToCompound.values()) {
        int size = getStringForCompound(compound).length();
        if(size > maxCompoundCharSequenceLength) {
          maxCompoundCharSequenceLength = size;
        }
      }
    }
    return maxCompoundCharSequenceLength;
  }

  public boolean hasCompound(C compound) {
    C retrievedCompound = getCompoundForString(compound.toString());
    return (retrievedCompound == null) ? false : true;
  }

  public boolean compoundsEquivalent(C compoundOne, C compoundTwo) {
    assertCompound(compoundOne);
    assertCompound(compoundTwo);
    return equivalentsMap.get(compoundOne).contains(compoundTwo);
  }

  public Set<C> getEquivalentCompounds(C compound) {
    return equivalentsMap.get(compound);
  }

  public boolean compoundsEqual(C compoundOne, C compoundTwo) {
    assertCompound(compoundOne);
    assertCompound(compoundTwo);
    return compoundOne.equalsIgnoreCase(compoundTwo);
  }

  public void verifySequence(Sequence<C> sequence) throws CompoundNotFoundError {
    for(C compound: sequence) {
      assertCompound(compound);
    }
  }

  public List<C> getAllCompounds() {
    return new ArrayList<C>(charSeqToCompound.values());
  }

  private void assertCompound(C compound) {
    boolean okay = hasCompound(compound);
    if(! okay) {
      throw new CompoundNotFoundError("The CompoundSet "+
          getClass().getSimpleName()+" knows nothing about the compound "+
          compound);
    }
  }
}
