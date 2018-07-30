/**
 * Abstract class for group finders. This class can be extended to implement
 * different group finding algorithms for identifying suitable group 
 * configurations for use in memory deduplication side-channel attacks to
 * detect application versions in co-resident virtual machines.
 * 
 * @author Jens Lindemann
 */

package de.uni_hamburg.svs.memsig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;

public abstract class GroupFinder {
	Software _sw;
	int _pagesize;
	
	VersionSignature[] _idvSigs;
	double _sigsizeThresh;
	int _maxDist;
	
	ArrayList<Integer> _candList;
	int[] _nonCands;
	
	/**
	 * @param sw {@link Software} that groups are being formed for
	 * @param pagesize page size in bytes
	 * @param sigsizeThresh Threshold for individual version signature size:
	 * 						Versions for which the size of the individual
	 * 						version signature already exceeds the threshold
	 * 						will not be joined with other versions in a group.
	 * @param maxDist Maximum distance between first and last version in a group
	 */
	public GroupFinder(Software sw, int pagesize, double sigsizeThresh, int maxDist) {
		_sw = sw;
		_pagesize = pagesize;
		
		_sigsizeThresh = sigsizeThresh;
		_maxDist = maxDist;
		
		_idvSigs = _sw.generateVersionSignatures(_pagesize);
		findCandidates(sigsizeThresh);
	}
	
	/**
	 * Identifies suitable candidates for forming groups according to the 
	 * specified threshold for individual version signatures: If the proportion
	 * of pages that can be used for an individual version's signature is higher
	 * than the threshold, that version forms a group of its own and is not
	 * further considered for addition to larger groups.
	 * 
	 * @param sigsizeThreshold threshold for individual version signature size
	 */
	private void findCandidates(double sigsizeThreshold) {
		TreeSet<Integer> candTreeSet = new TreeSet<Integer>(); 
		TreeSet<Integer> nonCandSet = new TreeSet<Integer>();
		for(int i = 0; i < _idvSigs.length; i++) {
			VersionSignature is = _idvSigs[i];
			if(is.numberOfPages() < (is.getSoftwareVersions()[0].numberOfPages(_pagesize) * sigsizeThreshold)) {
				candTreeSet.add(i);
			} else {
				nonCandSet.add(i);
			}
		}
		
		_candList = new ArrayList<Integer>();
		Iterator<Integer> candIt = candTreeSet.iterator();
		while(candIt.hasNext()) {
			_candList.add(candIt.next());
		}
		
		_nonCands = ArrayUtils.toPrimitive(nonCandSet.toArray(new Integer[0]));
	}
	
	/**
	 * Identifies a suitable group configuration.
	 * 
	 * @return an array of SoftwareVersionGroup objects, each representing one group
	 */
	public abstract SoftwareVersionGroup[] findSignatureGroups();
	
	/**
	 * Returns the best identified group configuration (which may have already
	 * been calculated by findSignatureGroups().
	 * 
	 * @return an array of SoftwareVersionGroup objects, each representing one group
	 */
	public abstract SoftwareVersionGroup[] getBestGroupConfig();
	
	/**
	 * Returns the signatures corresponding the the groups in the group
	 * configuration identified by findSignatureGroups().
	 * 
	 * @return an array of VersionSignature objects, each representing a
	 * signature for one of the groups
	 */
	public abstract VersionSignature[] getBestGroupConfigSignatures();
	
	/**
	 * Returns the average signature size for the group configuration
	 * identified by findSignatureGroups().
	 * 
	 * @return average signature size for best group configuration
	 */
	public abstract double getBestGroupConfigAvgSigsize();
}
