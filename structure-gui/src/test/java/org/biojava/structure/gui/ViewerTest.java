package org.biojava.structure.gui;

import java.awt.Color;


import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureImpl;
import org.biojava.bio.structure.io.PDBFileReader;
import org.biojava3.structure.gui.JmolViewerImpl;
import org.biojava3.structure.gui.RenderStyle;
import org.biojava3.structure.gui.Selection;
import org.biojava3.structure.gui.SelectionImpl;
import org.biojava3.structure.gui.StructureViewer;


import junit.framework.TestCase;

/**
 *
 * @author Jules
 */
public class ViewerTest extends TestCase {
	StructureViewer viewer;
	Structure structure;

	@Override
	protected void setUp(){
		//viewer = new OpenAstexViewer();
		viewer = new JmolViewerImpl();
		//viewer = new RCSBViewer();
		structure = new StructureImpl();
	}

	/**
	 * First we want to get a viewer object
	 */


	/**
	 * then load a PDB file.
	 */
	public void testStructureLoad(){
		PDBFileReader parser = new PDBFileReader();
		parser.setAutoFetch(true);
		try {
			structure = parser.getStructureById("4hhb");

			viewer.setStructure(structure);

			// manipulate the coodriantes
			// 
			//Calc.rotate(structure,Matrix m);

			viewer.repaint();

			Selection selection = new SelectionImpl();

			//selection can be a whole structure, mol_id, chain, residue, atom or SCOP, Pfam, UniProt features

			viewer.setSelection(selection);

			viewer.setColor(Color.RED);

			viewer.setStyle(RenderStyle.WIREFRAME);

			viewer.clear();

			viewer.setZoom(50);
		} catch (Exception e){
			fail(e.getMessage());
		}


	}

}
