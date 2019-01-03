/**
 * A {@link GroupFinder} implementation that sorts candidates for groups
 * according to their similarity to the first version in the group. After
 * choosing the first version in the dataset that is not assigned to a
 * group yet, the algorithm tries to add additional versions to the group,
 * starting with the most similar version still unassigned. Candidates for
 * addition to a group are also restricted by their distance from each other
 * (i.e. two members of a group may not be more than maxDist versions apart)
 * and the signature size that a signature for the individual version on its
 * own would have. While this class does not consider all possible group
 * configurations, we found that it is able to identify group configurations
 * leading to significantly improved signature sizes over the individual-
 * version case and is a good compromise between the achieved signature sizes
 * and computational complexity.
 * 
 * @author Jens Lindemann
 */
package de.uni_hamburg.svs.memsig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;

public class IterativeSimilarityGroupFinder extends GroupFinder {
	ArrayList<SoftwareVersionGroup> _groups;
	VersionSignature[] _sigs;
	double _sigsizeAvg;
	
	/**
	 * Creates a new Object. Parameters are identical to the parent class:
	 * @see{de.uni_hamburg.svs.memsig.GroupFinder#GroupFinder(Software,int,double,int)}
	 */
	public IterativeSimilarityGroupFinder(Software sw, int pagesize, double sigsizeThresh, int maxDist) {
		super(sw, pagesize, sigsizeThresh, maxDist);
		// TODO Auto-generated constructor stub
		_sigsizeAvg = -1;
	}

	/* (non-Javadoc)
	 * @see de.uni_hamburg.svs.memsig.GroupFinder#findSignatureGroups()
	 */
	@Override
	public SoftwareVersionGroup[] findSignatureGroups() {
		// TODO Auto-generated method stub
		//ArrayList<SoftwareVersion[]> groups = new ArrayList<SoftwareVersion[]>();
		TreeSet<VersionSignature> sigset = new TreeSet<VersionSignature>();
		// TODO make VersionSignature Comparable...
		//ArrayList<VersionSignature> sigset = new ArrayList<VersionSignature>();
		
		// Add all versions that are not candidates as 'groups' containing
		// a single version.
		for(int i = 0; i < _nonCands.length; i++) {
			SoftwareVersion[] idvGroup = new SoftwareVersion[1];
			idvGroup[0] = _idvSigs[_nonCands[i]].getSoftwareVersions()[0];
			//groups.add(idvGroup);
			sigset.add(_idvSigs[_nonCands[i]]);
		}
		
		while(_candList.size() > 0) {
			ArrayList<SoftwareVersion> grp = new ArrayList<SoftwareVersion>();
			int g0 = _candList.get(0);
			VersionSignature bestSig = _idvSigs[g0];
			SoftwareVersion sv = bestSig.getSoftwareVersions()[0];
			grp.add(sv);
			
			ArrayList<Integer> grpIdx = new ArrayList<Integer>();
			grpIdx.add(g0);
			
			int bestSigsize = bestSig.numberOfPages();
			ArrayList<SoftwareVersion> bestGrp = (ArrayList<SoftwareVersion>)grp.clone();
			ArrayList<Integer> bestGrpIdx = (ArrayList<Integer>)grpIdx.clone();
			
			// find most similar other cands
			// Create a HashMap containing the number of matching Pages in other candidates.
			HashMap<Integer, Integer> candSimilarity = new HashMap<Integer, Integer>();
			for(int i = 1; i < _candList.size(); i++) {
				int candIdx = _candList.get(i);
				SoftwareVersion candSv = _idvSigs[candIdx].getSoftwareVersions()[0];
				VersionComparisonResult vcr = sv.compareToVersion(candSv, _pagesize);
				int numMatches = vcr.numberOfMatches();
				candSimilarity.put(candIdx, numMatches);
			}
			
			List hmValues = new LinkedList(candSimilarity.entrySet());
			
			Comparator candSimComp = new Comparator() {
	            public int compare(Object o1, Object o2) {
	                return ((Comparable) ((Map.Entry) (o2)).getValue())
	                   .compareTo(((Map.Entry) (o1)).getValue());
	             }
	        };
	        
			Collections.sort(hmValues, candSimComp);
			
			candSimilarity = new LinkedHashMap<Integer, Integer>();
			Iterator<Map.Entry<Integer, Integer>> candSimIt = hmValues.iterator();
			while(candSimIt.hasNext()) {
				Map.Entry<Integer, Integer> entry = candSimIt.next();
				candSimilarity.put(entry.getKey(), entry.getValue());
			}
			
			Iterator<Integer> sortedCandIt = candSimilarity.keySet().iterator();
			while(sortedCandIt.hasNext()) {
				int gaddidx = sortedCandIt.next();
				int gaddsimilarity = candSimilarity.get(gaddidx);
				
				if((gaddidx - g0) > _maxDist) {
					// We do not need to consider this version if it is further
					// away from the first element of the group than specified
					// by the maximum distance.
					continue;
				}
				
				if(gaddsimilarity < bestSigsize) {
					break;
					// If there are fewer identical pages in the additional version
					// than are contained in the best signature found so far, we can
					// stop here, as adding further versions can only decrease signature
					// size.
				}
				
				SoftwareVersion gaddVer = _idvSigs[gaddidx].getSoftwareVersions()[0];
				grp.add(gaddVer);
				grpIdx.add(gaddidx);
				
				SoftwareVersion[] grpArray = grp.toArray(new SoftwareVersion[0]);
				
				VersionSignature newSig = _sw.generateVersionsSignature(grpArray, _pagesize);
				int newSigsize = newSig.numberOfPages();
				
				if(newSigsize >= bestSigsize) {
					bestSigsize = newSigsize;
					bestSig = newSig;
					bestGrp = (ArrayList<SoftwareVersion>)grp.clone();
					bestGrpIdx = (ArrayList<Integer>)grpIdx.clone();
				} else {
					// TODO do nothing?
				}
			}
			
			for(int i = bestGrpIdx.size() - 1; i >= 0; i--) {
				// remove versions in bestGrp from _candList
				_candList.remove(bestGrpIdx.get(i));
			}
			
			SoftwareVersion[] group = bestGrp.toArray(new SoftwareVersion[0]);
			//groups.add(group);
			sigset.add(bestSig);
		}
		
		_sigs = new VersionSignature[sigset.size()];
		long sigsizeSum = 0;
		
		_groups = new ArrayList<SoftwareVersionGroup>();
		
		Iterator<VersionSignature> sigIter = sigset.iterator();
		for(int i = 0; i < _sigs.length; i++) {
			_sigs[i] = sigIter.next();
			sigsizeSum += _sigs[i].numberOfPages();
			SoftwareVersionGroup grp = new SoftwareVersionGroup(_sigs[i].getSoftwareVersions());
			_groups.add(grp);
		}
		_sigsizeAvg = (double)sigsizeSum / _sigs.length;
		
		return getBestGroupConfig();
	}

	/* (non-Javadoc)
	 * @see de.uni_hamburg.svs.memsig.GroupFinder#getBestGroupConfig()
	 */
	@Override
	public SoftwareVersionGroup[] getBestGroupConfig() {
		if(_groups == null) {
			findSignatureGroups();
		}
		
		int numGroups = _groups.size();
		SoftwareVersionGroup[] cfg = new SoftwareVersionGroup[numGroups];
		
		for(int i = 0; i < numGroups; i++) {
			SoftwareVersionGroup grp = _groups.get(i);
			cfg[i] = grp;
		}
		
		return cfg;
	}

	/* (non-Javadoc)
	 * @see de.uni_hamburg.svs.memsig.GroupFinder#getBestGroupConfigSignatures()
	 */
	@Override
	public VersionSignature[] getBestGroupConfigSignatures() {
		// TODO check whether groups have been generated
		return _sigs;
	}

	/* (non-Javadoc)
	 * @see de.uni_hamburg.svs.memsig.GroupFinder#getBestGroupConfigAvgSigsize()
	 */
	@Override
	public double getBestGroupConfigAvgSigsize() {
		// TODO check whether groups have been generated
		return _sigsizeAvg;
	}

}
