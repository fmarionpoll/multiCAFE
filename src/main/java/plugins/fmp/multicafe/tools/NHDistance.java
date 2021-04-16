package plugins.fmp.multicafe.tools;

/**
 * The Interface Distance.
 * author Nicolas HERVE 
 */

public interface NHDistance<T> 
{	
	double computeDistance(T s1, T s2) throws NHFeatureException;
}