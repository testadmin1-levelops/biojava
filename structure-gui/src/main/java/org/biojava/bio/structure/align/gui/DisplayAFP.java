/*
 *                    PDB web development code
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
 *
 * Created on Jun 13, 2009
 * Created by Andreas Prlic
 *
 */

package org.biojava.bio.structure.align.gui;


import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;


import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;

import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.ChainImpl;
import org.biojava.bio.structure.Group;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.StructureImpl;
import org.biojava.bio.structure.align.gui.aligpanel.AligPanel;
import org.biojava.bio.structure.align.gui.aligpanel.StatusDisplay;
import org.biojava.bio.structure.align.gui.jmol.JmolTools;
import org.biojava.bio.structure.align.gui.jmol.StructureAlignmentJmol;
import org.biojava.bio.structure.align.model.AFPChain;


public class DisplayAFP
{

   public static final boolean debug =  false;


   //TODO: same as getEqrPos??? !!!
   public static final List<Integer> getEQRAlignmentPos(AFPChain afpChain){
      List<Integer> lst = new ArrayList<Integer>();

      char[] s1 = afpChain.getAlnseq1();
      char[] s2 = afpChain.getAlnseq2();
      char[] symb = afpChain.getAlnsymb();
      boolean isFatCat = afpChain.getAlgorithmName().startsWith("jFatCat");

      for ( int i =0 ; i< s1.length; i++){
         char c1 = s1[i];
         char c2 = s2[i];

         if ( isAlignedPosition(i,c1,c2,isFatCat, symb)) {
            lst.add(i);			  
         }

      }
      return lst;

   }





   private static boolean isAlignedPosition(int i, char c1, char c2, boolean isFatCat,char[]symb)
   {
      if ( isFatCat){
         char s = symb[i];
         if ( c1 != '-' && c2 != '-' && s != ' '){
            return true;
         }          
      } else {

         if ( c1 != '-' && c2 != '-')
            return true;
      }

      return false;


   }





   public static final List<String> getPDBresnum(int aligPos, AFPChain afpChain, Atom[] ca){
      List<String> lst = new ArrayList<String>();
      if ( aligPos > 1) {
         System.err.println("multiple alignments not supported yet!");
         return lst;
      }	

      int blockNum = afpChain.getBlockNum();      
      int[] optLen = afpChain.getOptLen();
      int[][][] optAln = afpChain.getOptAln();

      if ( optLen == null)
         return lst;

      for(int bk = 0; bk < blockNum; bk ++)       {

         for ( int i=0;i< optLen[bk];i++){

            int pos = optAln[bk][aligPos][i];
            String pdbInfo = JmolTools.getPdbInfo(ca[pos]);
            //lst.add(ca1[pos].getParent().getPDBCode());
            lst.add(pdbInfo);
         }

      }
      return lst;
   }
   
   public static int getBlockNrForAlignPos(AFPChain afpChain, int aligPos){
      int ungappedPos = -1;
      int blockNum = afpChain.getBlockNum();

      int[] optLen = afpChain.getOptLen();
      int[][][] optAln = afpChain.getOptAln();

      int len = 0;
      int p1b=0;
      int p2b=0;
      
      for(int i = 0; i < blockNum; i ++)  {   

         for(int j = 0; j < optLen[i]; j ++) {

            int p1 = optAln[i][0][j];
            int p2 = optAln[i][1][j];

            //                 System.out.println(p1 + " " + p2 + " " +  footer2.toString());

            if ( len == 0){
               //
            } else {
               // check for gapped region
               int lmax = (p1 - p1b - 1)>(p2 - p2b - 1)?(p1 - p1b - 1):(p2 - p2b - 1);
               for(int k = 0; k < lmax; k ++)      {
                  len++;
               }
            }

            len++;
            ungappedPos++;
            p1b = p1;
            p2b = p2;
            if ( len >= aligPos) {

               return i;
            }
         }
      }

      return blockNum;

   }



   /** return the atom at alignment position aligPos. at the present only works with block 0
    * @param chainNr the number of the aligned pair. 0... first chain, 1... second chain.
    * @param afpChain
    * @param aligPos position on the alignment
    * @param getPrevious gives the previous position if false, gives the next posible atom
    * @return
    */
   public static final Atom getAtomForAligPos(AFPChain afpChain,int chainNr, int aligPos, Atom[] ca , boolean getPrevious ) throws StructureException{
      int[] optLen = afpChain.getOptLen();
      // int[][][] optAln = afpChain.getOptAln();

      if ( optLen == null)
         return null;

      if (chainNr < 0 || chainNr > 1){
         throw new StructureException("So far only pairwise alignments are supported, but you requested results for alinged chain nr " + chainNr);
      }

      //if (  afpChain.getAlgorithmName().startsWith("jFatCat")){

      /// for FatCat algorithms...
      int capos = getUngappedFatCatPos(afpChain, chainNr, aligPos);
      if ( capos < 0) {

         capos = getNextFatCatPos(afpChain, chainNr, aligPos,getPrevious);

         //System.out.println(" got next" + capos + " for " + chainNr + " alignedPos: " + aligPos);
      } else {
         //System.out.println("got aligned fatcat position: " + capos + " " + chainNr + " for alig pos: " + aligPos);	
      }

      if ( capos < 0) {
         System.err.println("could not match position " + aligPos + " in chain " + chainNr +". Returing null...");
         return null;
      }
      if ( capos > ca.length){
         System.err.println("Atom array "+ chainNr + " does not have " + capos +" atoms. Returning null.");
         return null;
      }
      return ca[capos];
      //}

      //    
      //      
      //      int ungappedPos = getUngappedPos(afpChain, aligPos);
      //      System.out.println("getAtomForAligPOs " + aligPos  + " " + ungappedPos );
      //      return ca[ungappedPos];
      //      
      //      if ( ungappedPos >= optAln[bk][chainNr].length)
      //         return null;
      //      int pos = optAln[bk][chainNr][ungappedPos];
      //      if ( pos > ca.length)
      //         return null;
      //      return ca[pos];
   }


   private static int getNextFatCatPos(AFPChain afpChain, int chainNr,
         int aligPos, boolean getPrevious) {

      char[] aseq;
      if ( chainNr == 0 )
         aseq = afpChain.getAlnseq1();
      else
         aseq = afpChain.getAlnseq2();

      if ( aligPos > aseq.length)
         return -1;
      if ( aligPos < 0)
         return -1;

      int blockNum = afpChain.getBlockNum();
      int[] optLen = afpChain.getOptLen();
      int[][][] optAln = afpChain.getOptAln();

      int p1, p2;
      int p1b = 0;
      int p2b = 0;
      int len = 0;



      boolean terminateNextMatch = false;
      for(int i = 0; i < blockNum; i ++)  {        	
         for(int j = 0; j < optLen[i]; j ++) {

            p1 = optAln[i][0][j];
            p2 = optAln[i][1][j];


            if(len > 0)     {

               int lmax = (p1 - p1b - 1)>(p2 - p2b - 1)?(p1 - p1b - 1):(p2 - p2b - 1);

               // lmax gives the length of an alignment gap

               //System.out.println("  pos "+ len+" p1-p2: " + p1 + " - " + p2 + " lmax: " + lmax + " p1b-p2b:"+p1b + " " + p2b + " terminate? "+ terminateNextMatch);	
               for(int k = 0; k < lmax; k ++)      {

                  if(k >= (p1 - p1b - 1)) {
                     // a gap position in chain 0
                     if ( aligPos == len && chainNr == 0 ){
                        if ( getPrevious)
                           return p1b;
                        else
                           terminateNextMatch = true;
                     }
                  }
                  else {
                     if ( aligPos == len && chainNr == 0)
                        return p1b+1+k;


                  }
                  if(k >= (p2 - p2b - 1)) {
                     // a gap position in chain 1
                     if ( aligPos == len && chainNr == 1){
                        if ( getPrevious)
                           return p2b;
                        else 
                           terminateNextMatch = true;
                     }
                  }
                  else  {
                     if ( aligPos == len && chainNr == 1) {
                        return p2b+1+k;
                     }


                  }
                  len++;

               }
            }

            if ( aligPos == len && chainNr == 0)
               return p1;
            if ( aligPos == len && chainNr == 1)
               return p2;



            if ( terminateNextMatch)
               if ( chainNr == 0)
                  return p1;
               else 
                  return p2;
            if ( len > aligPos) {
               if ( getPrevious) {
                  if ( chainNr == 0)
                     return p1b;
                  else
                     return p2b;
               } else {
                  terminateNextMatch = true;
               }
            }

            len++;
            p1b = p1;
            p2b = p2;




         }
      }


      // we did not find an aligned position
      return -1;

   }

   private static final int getUngappedFatCatPos(AFPChain afpChain, int chainNr, int aligPos){
      char[] aseq;
      if ( chainNr == 0 )
         aseq = afpChain.getAlnseq1();
      else
         aseq = afpChain.getAlnseq2();

      if ( aligPos > aseq.length)
         return -1;
      if ( aligPos < 0)
         return -1;

      int blockNum = afpChain.getBlockNum();
      int[] optLen = afpChain.getOptLen();
      int[][][] optAln = afpChain.getOptAln();

      int p1, p2;
      int p1b = 0;
      int p2b = 0;
      int len = 0;


      for(int i = 0; i < blockNum; i ++)  {        	
         for(int j = 0; j < optLen[i]; j ++) {

            p1 = optAln[i][0][j];
            p2 = optAln[i][1][j];


            if(len > 0)     {

               int lmax = (p1 - p1b - 1)>(p2 - p2b - 1)?(p1 - p1b - 1):(p2 - p2b - 1);

               // lmax gives the length of an alignment gap

               //System.out.println("   p1-p2: " + p1 + " - " + p2 + " lmax: " + lmax + " p1b-p2b:"+p1b + " " + p2b);	
               for(int k = 0; k < lmax; k ++)      {

                  if(k >= (p1 - p1b - 1)) {
                     // a gap position in chain 0
                     if ( aligPos == len && chainNr == 0){
                        return -1;
                     }
                  }
                  else {
                     if ( aligPos == len && chainNr == 0)
                        return p1b+1+k;


                  }
                  if(k >= (p2 - p2b - 1)) {
                     // a gap position in chain 1
                     if ( aligPos == len && chainNr == 1){
                        return -1;
                     }
                  }
                  else  {
                     if ( aligPos == len && chainNr == 1) {
                        return p2b+1+k;
                     }


                  }
                  len++;

               }
            }

            if ( aligPos == len && chainNr == 0)
               return p1;
            if ( aligPos == len && chainNr == 1)
               return p2;

            len++;
            p1b = p1;
            p2b = p2;


         }
      }


      // we did not find an aligned position
      return -1;
   }


   /** get an artifical List of chains containing the Atoms and groups.
    * Does NOT rotate anything.
    * @param ca1
    * @param ca2
    * @return
    * @throws StructureException
    */
   private static final List<Chain> getAlignedModel(Atom[] ca){

      List<Chain> model = new ArrayList<Chain>();
      for ( Atom a: ca){

         Group g = a.getParent();
         Chain parentC = g.getParent();

         Chain newChain = null;
         for ( Chain c :  model) {
            if ( c.getName().equals(parentC.getName())){
               newChain = c;
               break;
            }
         }
         if ( newChain == null){

            newChain = new ChainImpl();

            newChain.setName(parentC.getName());

            model.add(newChain);
         }

         newChain.addGroup(g);

      }

      return model;
   }
  

   /** get an artifical Structure containing both chains.
    * Does NOT rotate anything
    * @param ca1
    * @param ca2
    * @return
    * @throws StructureException
    */
   public static final Structure getAlignedStructure(Atom[] ca1, Atom[] ca2) throws StructureException{

      Structure s = new StructureImpl();

      s.setNmr(true);


      List<Chain>model1 = getAlignedModel(ca1);
      List<Chain>model2 = getAlignedModel(ca2);
      s.addModel(model1);
      s.addModel(model2);

      return s;
   }

   
   
   public static final Atom[] getAtomArray(Atom[] ca,List<Group> hetatms, List<Group> nucleotides ) throws StructureException{
      List<Atom> atoms = new ArrayList<Atom>();

      for (Atom a: ca){			
         atoms.add(a);         
      }

      if ( debug)
         System.out.println("got " + hetatms.size() + " hetatoms");
      // we only add atom nr 1, since the getAlignedStructure method actually adds the parent group, and not the atoms...
      for (Group g : hetatms){
         //if (debug)
         //   System.out.println("adding group " + g);
         Atom a = g.getAtom(0);
         //if (debug)
         //  System.out.println(a);
         a.setParent(g);
         atoms.add(a);
      }
      for (Group g : nucleotides ){
         //if (debug)
         //   System.out.println("adding group " + g);
         Atom a = g.getAtom(0);
         //if (debug)
         //   System.out.println(a);
         a.setParent(g);
         atoms.add(a);
      }

      Atom[] arr = (Atom[]) atoms.toArray(new Atom[atoms.size()]);

      return arr;
   }


   /** Note: ca2, hetatoms2 and nucleotides2 should not be rotated. This will be done here...
    * */

   public static final StructureAlignmentJmol display(AFPChain afpChain,Group[] twistedGroups, Atom[] ca1, Atom[] ca2,List<Group> hetatms, List<Group> nucleotides, List<Group> hetatms2, List<Group> nucleotides2 ) throws StructureException{

      List<Atom> twistedAs = new ArrayList<Atom>();

      for ( Group g: twistedGroups){
         if ( g == null )
            continue;
         if ( g.size() < 1)
            continue;
         Atom a = g.getAtom(0);
         twistedAs.add(a);
      }
      Atom[] twistedAtoms = (Atom[])twistedAs.toArray(new Atom[twistedAs.size()]);

      Atom[] arr1 = getAtomArray(ca1, hetatms, nucleotides);
      Atom[] arr2 = getAtomArray(twistedAtoms, hetatms2, nucleotides2);

      // 

      //if ( hetatms2.size() > 0)
      //	System.out.println("atom after:" + hetatms2.get(0).getAtom(0));
      
      //if ( hetatms2.size() > 0)
      //	System.out.println("atom after:" + hetatms2.get(0).getAtom(0));

      String title =  afpChain.getAlgorithmName() + " V." +afpChain.getVersion() + " : " + afpChain.getName1() + " vs. " + afpChain.getName2();

      //System.out.println(artificial.toPDB());



      StructureAlignmentJmol jmol = new StructureAlignmentJmol(afpChain,arr1,arr2);      
      //jmol.setStructure(artificial);


      //jmol.setTitle("Structure Alignment: " + afpChain.getName1() + " vs. " + afpChain.getName2());
      jmol.setTitle(title);
      return jmol;
   }

   public static void showAlignmentImage(AFPChain afpChain, Atom[] ca1, Atom[] ca2, StructureAlignmentJmol jmol) {
      String result = afpChain.toFatcat(ca1, ca2);

      //String rot = afpChain.toRotMat();
      //DisplayAFP.showAlignmentImage(afpChain, result + AFPChain.newline + rot);

      System.out.println(result);

      AligPanel me = new AligPanel();
      me.setStructureAlignmentJmol(jmol);		
      me.setAFPChain(afpChain);

      JFrame frame = new JFrame();

      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);		
      frame.setTitle(afpChain.getName1() + " vs. " + afpChain.getName2() + " | " + afpChain.getAlgorithmName() + " V. " + afpChain.getVersion());
      me.setPreferredSize(new Dimension(me.getCoordManager().getPreferredWidth() , me.getCoordManager().getPreferredHeight()));

      JMenuBar menu = MenuCreator.getAlignmentTextMenu(frame,me,afpChain);
      frame.setJMenuBar(menu);

      JScrollPane scroll = new JScrollPane(me);
      scroll.setAutoscrolls(true);

      StatusDisplay status = new StatusDisplay();
      status.setAfpChain(afpChain);

      status.setCa1(ca1);
      status.setCa2(ca2);
      me.setCa1(ca1);
      me.setCa2(ca2);
      me.addAlignmentPositionListener(status);


      Box vBox = Box.createVerticalBox();
      vBox.add(scroll);
      vBox.add(status);


      frame.getContentPane().add(vBox);

      frame.pack();
      frame.setVisible(true);
      // make sure they get cleaned up correctly:
      frame.addWindowListener(me);
      frame.addWindowListener(status);
   }

   public static void showAlignmentImage(AFPChain afpChain, String result) {

      JFrame frame = new JFrame();

      String title = afpChain.getAlgorithmName() + " V."+afpChain.getVersion() + " : " + afpChain.getName1()  + " vs. " + afpChain.getName2() ;
      frame.setTitle(title);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

      AlignmentTextPanel txtPanel = new AlignmentTextPanel();
      txtPanel.setText(result);

      JMenuBar menu = MenuCreator.getAlignmentTextMenu(frame,txtPanel,afpChain);

      frame.setJMenuBar(menu);
      JScrollPane js = new JScrollPane();
      js.getViewport().add(txtPanel);
      js.getViewport().setBorder(null);
      //js.setViewportBorder(null);
      //js.setBorder(null);
      //js.setBackground(Color.white);

      frame.getContentPane().add(js);
      frame.pack();      
      frame.setVisible(true);

   }


}
