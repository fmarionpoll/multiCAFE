package plugins.fmp.multicafe2.tools.nherve.toolbox.concurrent;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * The Class SingleDataTask.
 * 
 * @param <Input>
 *            the generic type
 * @param <Output>
 *            the generic type
 * @author Nicolas HERVE - nherve@ina.fr
 */
public abstract class SingleDataTask<Input, Output> implements Callable<Output> {
	
	/** The data. */
	private Input data;
	
	/** The idx. */
	private int idx;
	
	/**
	 * Instantiates a new single data task.
	 * 
	 * @param allData
	 *            the all data
	 * @param idx
	 *            the idx
	 */
	public SingleDataTask(List<Input> allData, int idx) {
		super();
		
		this.data = allData.get(idx);
		this.idx = idx;
	}

	/**
	 * Gets the data.
	 * 
	 * @return the data
	 */
	public Input getData() {
		return data;
	}

	/**
	 * Gets the idx.
	 * 
	 * @return the idx
	 */
	public int getIdx() {
		return idx;
	}

}
