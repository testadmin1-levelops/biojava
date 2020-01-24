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
 * Created on Mar 11, 2010
 * Author: Andreas Prlic 
 *
 */

package demo;

import java.util.List;

import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.Group;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureTools;
import org.biojava.bio.structure.align.StructureAlignment;
import org.biojava.bio.structure.align.StructureAlignmentFactory;
import org.biojava.bio.structure.align.ce.CeMain;
import org.biojava.bio.structure.align.fatcat.FatCatFlexible;
import org.biojava.bio.structure.align.gui.DisplayAFP;
import org.biojava.bio.structure.align.gui.StructureAlignmentDisplay;
import org.biojava.bio.structure.align.gui.jmol.StructureAlignmentJmol;
import org.biojava.bio.structure.align.model.AFPChain;
import org.biojava.bio.structure.align.model.AfpChainWriter;
import org.biojava.bio.structure.align.util.AtomCache;


public class CookBook
{


   public static void main(String[] args){

      String name1="1HNG.B";
      String name2="1A64.A";
      

      try {

         // for this example we are going to use the jFatCat-rigid algorithm
         StructureAlignment algorithm = StructureAlignmentFactory.getAlgorithm(FatCatFlexible.algorithmName);

         // where can the PDB files be found?
         // if they are not there, they will be automatically downloaded and installed there.
         String pdbFilePath = "/tmp/";      

         // is the PDB installation split into subdirectories (as on the PDB ftp servers)
         boolean isSplitInstallation = true;

         // the cache takes care of loading the structures
         AtomCache cache = new AtomCache(pdbFilePath,isSplitInstallation);


         //////////////////////////////
         // no need to change anything below this line
         // ////////////////////////////

         // load the structures 
         Structure structure1 = cache.getStructure(name1);
         Structure structure2 = cache.getStructure(name2);

         // we are only using the CA atoms in the structure for the alignment
         Atom[] ca1 = StructureTools.getAtomCAArray(structure1);
         Atom[] ca2 = StructureTools.getAtomCAArray(structure2);

         // do the actual alignment
         AFPChain afpChain = algorithm.align(ca1,ca2);

         // just name the two molecules 
         afpChain.setName1(name1);
         afpChain.setName2(name2);

         // print and display results:


         // flexible original results:
         System.out.println(afpChain.toFatcat(ca1,ca2));


         // If there is only one alignment block then the hetatoms and nucleotides 
         // can get rotated as well, which makes it nicer to compare active sites.

         List<Group> hetatms = structure1.getChain(0).getAtomGroups("hetatm");
         List<Group> nucs    = structure1.getChain(0).getAtomGroups("nucleotide");

         List<Group> hetatms2 = structure2.getChain(0).getAtomGroups("hetatm");
         List<Group> nucs2    = structure2.getChain(0).getAtomGroups("nucleotide");


         // they are not rotated at this point.. the display will do it for us...

         // show the alignment in 3D in jmol
         StructureAlignmentJmol jmol= StructureAlignmentDisplay.display(afpChain, ca1, ca2,  hetatms, nucs, hetatms2, nucs2);
                 
         // set the display title for the frame
         jmol.setTitle(algorithm.getAlgorithmName() + " : " + name1 + " vs. " + name2);

         // here we open up the alignment - text panel that can interact with the 3D jmol display.
         DisplayAFP.showAlignmentImage(afpChain, ca1,ca2,jmol);
         
         // we can print an XML version 
         //System.out.println(AFPChainXMLConverter.toXML(afpChain, ca1, ca2));

         // or print the same output as original FATCAT 
         System.out.println(AfpChainWriter.toFatCat(afpChain, ca1, ca2));
         
   

       

      } catch (Exception e){
         e.printStackTrace();
      }
   }
}
