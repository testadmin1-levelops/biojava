/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 * Created on 01-21-2010
 *
 * @author Richard Holland
 * @auther Scooter Willis
 *
 */

package org.biojava3.core.sequence.compound;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.biojava3.core.exceptions.CompoundNotFoundError;
import org.biojava3.core.sequence.template.CompoundSet;
import org.biojava3.core.sequence.template.Sequence;

public class AminoAcidCompoundSet implements CompoundSet<AminoAcidCompound> {

    private final Map<String, AminoAcidCompound> aminoAcidCompoundCache = new HashMap<String, AminoAcidCompound>();

    public AminoAcidCompoundSet() {
        aminoAcidCompoundCache.put("A", new AminoAcidCompound(this, "A", "Ala", "Alanine", 0.0f));
        aminoAcidCompoundCache.put("R", new AminoAcidCompound(this, "R", "Arg", "Arginine", 0.0f));
        aminoAcidCompoundCache.put("N", new AminoAcidCompound(this, "N", "Asn", "Asparagine", 0.0f));
        aminoAcidCompoundCache.put("D", new AminoAcidCompound(this, "D", "Asp", "Aspartic acid", 0.0f));
        aminoAcidCompoundCache.put("C", new AminoAcidCompound(this, "C", "Cys", "Cysteine", 0.0f));
        aminoAcidCompoundCache.put("E", new AminoAcidCompound(this, "E", "Glu", "Glutamic acid", 0.0f));
        aminoAcidCompoundCache.put("Q", new AminoAcidCompound(this, "Q", "Gln", "Glutamine", 0.0f));
        aminoAcidCompoundCache.put("G", new AminoAcidCompound(this, "G", "Gly", "Glycine", 0.0f));
        aminoAcidCompoundCache.put("H", new AminoAcidCompound(this, "H", "His", "Histidine", 0.0f));
        aminoAcidCompoundCache.put("I", new AminoAcidCompound(this, "I", "Ile", "Isoleucine", 0.0f));
        aminoAcidCompoundCache.put("L", new AminoAcidCompound(this, "L", "Leu", "Leucine", 0.0f));
        aminoAcidCompoundCache.put("K", new AminoAcidCompound(this, "K", "Lys", "Lysine", 0.0f));
        aminoAcidCompoundCache.put("M", new AminoAcidCompound(this, "M", "Met", "Methionine", 0.0f));
        aminoAcidCompoundCache.put("F", new AminoAcidCompound(this, "F", "Phe", "Phenylalanine", 0.0f));
        aminoAcidCompoundCache.put("P", new AminoAcidCompound(this, "P", "Pro", "Proline", 0.0f));
        aminoAcidCompoundCache.put("S", new AminoAcidCompound(this, "S", "Ser", "Serine", 0.0f));
        aminoAcidCompoundCache.put("T", new AminoAcidCompound(this, "T", "Thr", "Threonine", 0.0f));
        aminoAcidCompoundCache.put("W", new AminoAcidCompound(this, "W", "Trp", "Tryptophan", 0.0f));
        aminoAcidCompoundCache.put("Y", new AminoAcidCompound(this, "Y", "Tyr", "Tyrosine", 0.0f));
        aminoAcidCompoundCache.put("V", new AminoAcidCompound(this, "V", "Val", "Valine", 0.0f));
        aminoAcidCompoundCache.put("B", new AminoAcidCompound(this, "B", "Asx", "Asparagine or Aspartic acid", null));
        aminoAcidCompoundCache.put("Z", new AminoAcidCompound(this, "Z", "Glx", "Glutamine or glutamic acid", null));
        aminoAcidCompoundCache.put("J", new AminoAcidCompound(this, "J", "Xle", "Leucine or Isoleucine", null));
        aminoAcidCompoundCache.put("X", new AminoAcidCompound(this, "Z", "Xaa", "Unspecified", null));
        aminoAcidCompoundCache.put("-", new AminoAcidCompound(this, "-", "---", "Unspecified", null));
        aminoAcidCompoundCache.put(".", new AminoAcidCompound(this, ".", "...", "Unspecified", null));
        aminoAcidCompoundCache.put("_", new AminoAcidCompound(this, "_", "___", "Unspecified", null));
        aminoAcidCompoundCache.put("*", new AminoAcidCompound(this, "*", "***", "Stop", null));

        //Selenocystine - this is encoded by UGA with the presence
        //of a SECIS element (SElenoCysteine Insertion Sequence) in the mRNA
        //and is a post-translation modification
        aminoAcidCompoundCache.put("U", new AminoAcidCompound(this, "U", "Sec", "Selenocystine", 168.053f));

        //Pyrrolysine is encoded by UAG in mRNA (normally Amber stop codon) which is translated to
        //this amino acid under the presence of pylT which creates an anti-codon CUA & pylS
        //which then does the actual conversion to Pyl.
        aminoAcidCompoundCache.put("O", new AminoAcidCompound(this, "O", "Pyl", "Pyrrolysine", 255.31f));
    }

    public String getStringForCompound(AminoAcidCompound compound) {
        return compound.toString();
    }

    public AminoAcidCompound getCompoundForString(String string) {
        if (string.length() == 0) {
            return null;
        }
        if (string.length() > this.getMaxSingleCompoundStringLength()) {
            throw new IllegalArgumentException("String supplied ("+string+") is too long. Max is "+getMaxSingleCompoundStringLength());
        }
        return this.aminoAcidCompoundCache.get(string);
    }

    public int getMaxSingleCompoundStringLength() {
        return 1;
    }

    private final static AminoAcidCompoundSet aminoAcidCompoundSet = new AminoAcidCompoundSet();

    static public AminoAcidCompoundSet getAminoAcidCompoundSet() {
        return aminoAcidCompoundSet;
    }

    public boolean compoundsEquivalent(AminoAcidCompound compoundOne,
        AminoAcidCompound compoundTwo) {
      throw new UnsupportedOperationException("Unimplemented");
    }

    public Set<AminoAcidCompound> getEquivalentCompounds(
        AminoAcidCompound compound) {
      throw new UnsupportedOperationException("Unimplemented");
    }

    public boolean hasCompound(AminoAcidCompound compound) {
      throw new UnsupportedOperationException("Unimplemented");
    }

    public void verifySequence(Sequence<AminoAcidCompound> sequence)
        throws CompoundNotFoundError {
      throw new UnsupportedOperationException("Unimplemented");
    }

    public List<AminoAcidCompound> getAllCompounds() {
      return new ArrayList<AminoAcidCompound>(aminoAcidCompoundCache.values());
    }
}
