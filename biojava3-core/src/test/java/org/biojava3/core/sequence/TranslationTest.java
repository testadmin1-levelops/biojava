package org.biojava3.core.sequence;

import static org.biojava3.core.sequence.io.util.IOUtils.close;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.biojava3.core.sequence.compound.AminoAcidCompound;
import org.biojava3.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava3.core.sequence.compound.DNACompoundSet;
import org.biojava3.core.sequence.compound.NucleotideCompound;
import org.biojava3.core.sequence.io.DNASequenceCreator;
import org.biojava3.core.sequence.io.FastaReader;
import org.biojava3.core.sequence.io.GenericFastaHeaderParser;
import org.biojava3.core.sequence.io.IUPACParser;
import org.biojava3.core.sequence.io.ProteinSequenceCreator;
import org.biojava3.core.sequence.io.util.ClasspathResource;
import org.biojava3.core.sequence.template.Sequence;
import org.biojava3.core.sequence.transcription.Frame;
import org.biojava3.core.sequence.transcription.RNAToAminoAcidTranslator;
import org.biojava3.core.sequence.transcription.TranscriptionEngine;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TranslationTest {

  private static DNACompoundSet dnaCs = DNACompoundSet.getDNACompoundSet();
  private static AminoAcidCompoundSet aaCs = AminoAcidCompoundSet.getAminoAcidCompoundSet();

  private static DNASequence brca2Dna;
  private static Sequence<AminoAcidCompound> brca2Pep;

  @BeforeClass
  public static void parseSequences() {
    InputStream cdsIs = new ClasspathResource(
        "org/biojava3/core/sequence/BRCA2-cds.fasta").getInputStream();
    InputStream pepIs = new ClasspathResource(
        "org/biojava3/core/sequence/BRCA2-peptide.fasta").getInputStream();

    try {
      FastaReader<DNASequence, NucleotideCompound> dnaReader = new FastaReader<DNASequence, NucleotideCompound>(cdsIs,
          new GenericFastaHeaderParser<DNASequence, NucleotideCompound>(), new DNASequenceCreator(dnaCs));
      brca2Dna = dnaReader.process().values().iterator().next();
      FastaReader<ProteinSequence, AminoAcidCompound> pReader = new FastaReader<ProteinSequence, AminoAcidCompound>(
          pepIs, new GenericFastaHeaderParser<ProteinSequence, AminoAcidCompound>(), new ProteinSequenceCreator(
              aaCs));
      brca2Pep = pReader.process().values().iterator().next();
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Encountered exception");
    }
    finally {
      close(cdsIs);
      close(pepIs);
    }
  }

  @Test
  public void getUniversal() {
    IUPACParser.getInstance().getTable(1);
    IUPACParser.getInstance().getTable("UNIVERSAL");
  }

  @Test
  public void basicTranslation() {
    TranscriptionEngine e = TranscriptionEngine.getDefault();
    DNASequence dna = new DNASequence("ATG");
    RNASequence rna = dna.getRNASequence(e);
    ProteinSequence protein = rna.getProteinSequence(e);
    AminoAcidCompound initMet = protein.getCompoundAt(1);
    assertThat("Initator methionine wrong", initMet.toString(), is("M"));
  }

  @SuppressWarnings("serial")
  @Test
  public void multiFrameTranslation() {
    TranscriptionEngine e = TranscriptionEngine.getDefault();
    DNASequence dna = new DNASequence("ATGGCGTGA");

    Map<Frame, String> expectedTranslations = new EnumMap<Frame,String>(Frame.class) {{
      put(Frame.ONE, "MA");
      put(Frame.TWO, "WR");
      put(Frame.THREE, "GV");
      put(Frame.REVERSED_ONE, "SRH");
      put(Frame.REVERSED_TWO, "HA");
      put(Frame.REVERSED_THREE, "TP");
    }};

    Map<Frame, Sequence<AminoAcidCompound>> translations =
      e.multipleFrameTranslation(dna, Frame.getAllFrames());

    for(Entry<Frame, Sequence<AminoAcidCompound>> entry: translations.entrySet()) {
      String expected = expectedTranslations.get(entry.getKey());
      Sequence<AminoAcidCompound> protein = entry.getValue();
      assertThat("Checking 6 frame translation", protein.toString(), is(expected));
    }
  }

  @Test
  public void translateBrca2ExonOne() {
    TranscriptionEngine e = TranscriptionEngine.getDefault();
    DNASequence dna = new DNASequence(
        "ATGCCTATTGGATCCAAAGAGAGGCCAACATTTTTTGAAATTTTTAAGACACGCTGCAACAAAGCA");
    RNASequence rna = dna.getRNASequence(e);
    Sequence<AminoAcidCompound> peptide = rna.getProteinSequence(e);
    assertThat("Initator methionine wrong", peptide.getSequenceAsString(),
        is("MPIGSKERPTFFEIFKTRCNKA"));
  }

  @Test(timeout=2000)
  public void translateBrca2() {
    TranscriptionEngine e = TranscriptionEngine.getDefault();
    RNAToAminoAcidTranslator t = e.getRnaAminoAcidTranslator();
    for(int i =0; i < 100; i++) {
    RNASequence rna = brca2Dna.getRNASequence();
    Sequence<AminoAcidCompound> peptide = t.createSequence(rna);
    assertThat("BRCA2 does not translate", peptide.getSequenceAsString(),
        is(brca2Pep.getSequenceAsString()));
    }
  }

}
