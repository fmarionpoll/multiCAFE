package plugins.fmp.multicafe2.tools.nherve.toolbox.image;

import icy.sequence.Sequence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import plugins.adufour.connectedcomponents.ConnectedComponent;
import plugins.adufour.connectedcomponents.ConnectedComponents;

/**
 * The Class My2DConnectedComponentFinder.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class My2DConnectedComponentFinder implements Iterable<My2DConnectedComponent> {
	
	/** The found objects. */
	private ArrayList<My2DConnectedComponent> foundObjects;
	
	/**
	 * Instantiates a new my2 d connected component finder.
	 * 
	 * @param binaryData
	 *            the binary data
	 * @param minVolume
	 *            the min volume
	 * @param maxVolume
	 *            the max volume
	 */
	public My2DConnectedComponentFinder(BinaryIcyBufferedImage binaryData, int minVolume, int maxVolume) {
		super();
		
		foundObjects = new ArrayList<My2DConnectedComponent>();
		List<ConnectedComponent> ccs = ConnectedComponents.extractConnectedComponents(new Sequence(binaryData), minVolume, maxVolume, null).get(0);
		
		int id = 0;
		for (ConnectedComponent cc : ccs) {
			foundObjects.add(new My2DConnectedComponent(id, cc));
			id++;
		}
	}

	/**
	 * Gets the component.
	 * 
	 * @param index
	 *            the index
	 * @return the component
	 * @throws ArrayIndexOutOfBoundsException
	 *             the array index out of bounds exception
	 */
	public My2DConnectedComponent getComponent(int index) throws ArrayIndexOutOfBoundsException {
		return foundObjects.get(index);
	}

	/**
	 * Gets the nb objects.
	 * 
	 * @return the nb objects
	 */
	public int getNbObjects() {
		return foundObjects.size();
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<My2DConnectedComponent> iterator() {
		return foundObjects.iterator();
	}
}
