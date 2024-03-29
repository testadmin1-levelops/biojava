package org.biojava3.core.sequence.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.biojava3.core.exceptions.TranslationException;
import org.biojava3.core.sequence.io.template.SequenceCreatorInterface;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public abstract class AbstractCompoundTranslator<F extends Compound, T extends Compound>
    implements CompoundTranslator<F, T> {

  private final SequenceCreatorInterface<T> creator;
  private final ListMultimap<F, T>          mapper;
  private final CompoundSet<F>              fromCompoundSet;
  private final CompoundSet<T>              toCompoundSet;

  public AbstractCompoundTranslator(SequenceCreatorInterface<T> creator,
      CompoundSet<F> fromCompoundSet, CompoundSet<T> toCompoundSet) {
    this.creator = creator;
    this.mapper = ArrayListMultimap.create();
    this.fromCompoundSet = fromCompoundSet;
    this.toCompoundSet = toCompoundSet;
  }

  public SequenceCreatorInterface<T> getCreator() {
    return creator;
  }

  public CompoundSet<F> getFromCompoundSet() {
    return fromCompoundSet;
  }

  public CompoundSet<T> getToCompoundSet() {
    return toCompoundSet;
  }

  @SuppressWarnings("unchecked")
  protected void addStrings(String source, String... targets) {
    F f = getFromCompoundSet().getCompoundForString(source);
    for (String t : targets) {
      addCompounds(f, getToCompoundSet().getCompoundForString(t));
    }
  }

  protected void addCompounds(F source, T... targets) {
    for (T t : targets) {
      mapper.put(source, t);
    }
  }

  public List<T> translateMany(F fromCompound) {
    return mapper.get(fromCompound);
  }

  public T translate(F fromCompound) {
    List<T> compounds = translateMany(fromCompound);
    if (compounds.isEmpty()) {
      throw new TranslationException("No compounds found for " + fromCompound);
    }
    else if (compounds.size() > 1) {
      throw new TranslationException("Too many compounds found for "
          + fromCompound);
    }
    else {
      return compounds.get(0);
    }
  }

  public List<Sequence<T>> createSequences(Sequence<F> originalSequence) {
    List<List<T>> workingList = new ArrayList<List<T>>();
    for (F source : originalSequence) {
      List<T> compounds = translateMany(source);

      // Translate source to a list of possible compounds; if we have 1 then
      // just add onto the list. If we have n then start new paths in all
      // sequences i.e.
      //
      // MTAS (A & S have 2 routes) makes
      // AUG UGG GAU AGU
      // AUG UGG GAC AGU
      // AUG UGG GAU AGC
      // AUG UGG GAC AGC
      if (compounds.size() == 0) {
        throw new TranslationException("Compound " + source + " resulted in "
            + "no target compounds");
      }
      addCompoundsToList(compounds, workingList);
    }

    postProcessCompoundLists(workingList);

    return workingListToSequences(workingList);
  }

  protected abstract void postProcessCompoundLists(List<List<T>> compoundLists);

  protected void addCompoundsToList(List<T> compounds, List<List<T>> workingList) {
    int size = compounds.size();
    List<List<T>> currentWorkingList = new ArrayList<List<T>>();
    for (int i = 0; i < size; i++) {
      boolean last = (i == (size - 1));
      // If last run we add the compound to the top set of lists & then
      // add the remaining ones in
      if (last) {
        addCompoundToLists(workingList, compounds.get(i));
        if (!currentWorkingList.isEmpty()) {
          workingList.addAll(currentWorkingList);
        }
      }
      // Otherwise duplicate the current sequence set and add this compound
      else {
        List<List<T>> duplicate = duplicateList(workingList);
        addCompoundToLists(duplicate, compounds.get(i));
        currentWorkingList.addAll(duplicate);
      }
    }
  }

  protected List<Sequence<T>> workingListToSequences(List<List<T>> workingList) {
    List<Sequence<T>> sequences = new ArrayList<Sequence<T>>();
    for (List<T> seqList : workingList) {
      sequences.add(getCreator().getSequence(seqList));
    }
    return sequences;
  }

  private List<List<T>> duplicateList(List<List<T>> incoming) {
    List<List<T>> outgoing = new ArrayList<List<T>>();
    for (List<T> current : incoming) {
      outgoing.add(new ArrayList<T>(current));
    }
    return outgoing;
  }

  protected void addCompoundToLists(List<List<T>> list, T compound) {

    if (list.isEmpty()) {
      list.add(new ArrayList<T>());
    }

    for (List<T> current : list) {
      current.add(compound);
    }
  }

  public Sequence<T> createSequence(Sequence<F> originalSequence) {
    Collection<Sequence<T>> sequences = createSequences(originalSequence);
    if (sequences.size() > 1) {
      throw new TranslationException("Too many sequences created; "
          + "createSequence() assumes only one sequence can be created");
    }
    else if (sequences.size() == 0) {
      throw new TranslationException("No sequences created");
    }
    return sequences.iterator().next();
  }

}
