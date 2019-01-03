package de.uni_hamburg.svs.memsig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;

/**
 * This class describes a software. It encapsulates information about all
 * versions of the software and contains information on where the corresponding
 * binaries are stored.
 * 
 * Within the directory for a software version, subdirectories must exist for
 * all versions of this software, each containing the corresponding binary.
 * 
 * @author Jens Lindemann
 */
public class Software implements Comparable<Software> {
	private String _name;
	private TreeSet<SoftwareVersion> _versions;
	private File _swDir; // Directory containing subdirs for all versions of the software
	private String _binaryName;
	private int _pageSize;
	
	/**
	 * Creates a new Software object.
	 * 
	 * @param name name of the software
	 * @param swDir directory where software versions are stored
	 * @param binaryName file name of the binary
	 */
	public Software(String name, File swDir, String binaryName, int pageSize) {
		this._name = name;
		this._swDir = swDir;
		this._binaryName = binaryName;
		this._pageSize = pageSize;
		
		initializeVersions();
	}
	
	/**
	 * Returns the name of the software.
	 * 
	 * @return name of the software
	 */
	public String getName() {
		return this._name;
	}
	
	/**
	 * Initializes the versions of the software by reading them
	 * from the file system.
	 */
	private void initializeVersions() {
		_versions = new TreeSet<SoftwareVersion>();
		
		File[] vdirs = _swDir.listFiles();
		for(File vdir : vdirs) {
			if(!vdir.isDirectory()) continue;
			
			String vstring = vdir.getName();
			SoftwareVersion sv = new SoftwareVersion(this, vstring, vdir, _pageSize);
			this.addSoftwareVersion(sv);
		}
	}
	
	/**
	 * Returns the binary file name.
	 * 
	 * @return binary file name
	 */
	public String getBinaryName() {
		return _binaryName;
	}
	
	/**
	 * Returns all {@link SoftwareVersion}s of the Software
	 * 
	 * @return all {@link SoftwareVersion}s of the Software
	 */
	public SortedSet<SoftwareVersion> getVersions() {
		return Collections.unmodifiableSortedSet(_versions);
	}
	
	/**
	 * Adds a {@link SoftwareVersion}
	 * 
	 * @param sv {@link SoftwareVersion} to add
	 */
	public void addSoftwareVersion(SoftwareVersion sv) {
		_versions.add(sv);
	}
	
	/**
	 * Generate a {@link VersionSignature} for the version and page size specified.
	 * 
	 * @param sv {@link SoftwareVersion} to generate signature for
	 * @param pageSize page size
	 * @return the signature
	 */
	public VersionSignature generateVersionSignature(SoftwareVersion sv, int pageSize) {
		SoftwareVersion[] svarray = { sv };
		return generateVersionsSignature(svarray, pageSize);
	}
	
	/**
	 * This will generate a VersionSignature for the versions specified. 
	 * The signature will contain all memory pages that appear in all of 
	 * these SoftwareVersions, but no other SoftwareVersion.
	 * 
	 * @param sigVersions {@link SoftwareVersion}s to generate signature for
	 * @param pageSize page size 
	 * @return the signature
	 */
	public VersionSignature generateVersionsSignature(SoftwareVersion[] sigVersions, int pageSize) {
		ArrayList<Page> vPages = new ArrayList<Page>();
		if(sigVersions.length == 0) {
			System.err.println("Invalid arguments: sigVersions must not be empty");
			return null;
		}
		
		SortedSet<CodePart> parts0 = sigVersions[0].getParts();
		
		int all01count = 0;
		int intDupCount = 0;
		int notMatchingInGroupCount = 0;
		
		for(CodePart part : parts0) {
			Page[] pages = part.getPages(pageSize);
			for(Page p : pages) {
				// Remove all-0 and all-1 pages as they are almost certain to trigger a false positive.
				if(p.isAllOnes() || p.isAllZeroes()) {
					all01count++;
					continue;
				}
				
				// Internal duplicates need to be removed, as a duplicate page within the
				// signature would be sufficient to trigger deduplication even without the
				// binary itself being present in memory. We will keep one copy, however,
				// as we still assume that there is only another copy of the page on the host
				// if the version is actually being executed in another VM.
				boolean addPage = true;
				for(Page vp : vPages) {
					if(vp.contentsEqualTo(p) && !(vp.equals(p))) {
						addPage = false;
						intDupCount++;
						break;
					}
				}
				
				if(addPage) {
					vPages.add(p);
				}
			}
		}
		
		// Remove all pages that are not contained in all
		// of the versions specified from the signature.
		if(sigVersions.length > 1) {
			for(int j = 0; j < vPages.size(); j++) {
				Page p = vPages.get(j);
				for(int i = 1; i < sigVersions.length; i++) {
					System.out.println(j);
					if(!sigVersions[i].containsPageContent(p)) {
						vPages.remove(j);
						notMatchingInGroupCount++;
						j--; // By removing, all remaining elements have been
						// shifted to the left, so we have to correct j.
						break;
					}
				}
			}
		}
		
		return generateSignature(vPages.toArray(new Page[0]), sigVersions, pageSize, all01count, intDupCount, notMatchingInGroupCount);
	}
	
	/**
	 * Generates signatures for all versions of the Software.
	 * 
	 * @param pageSize page size to generate signatures for
	 * @return the signatures
	 */
	public VersionSignature[] generateVersionSignatures(int pageSize) {
		VersionSignature[] sigs = new VersionSignature[_versions.size()];
		Iterator<SoftwareVersion> svs = _versions.iterator();
		for(int i = 0; i < _versions.size(); i++) {
			sigs[i] = generateVersionSignature(svs.next(), pageSize);
		}
		return sigs;
	}
	
	/**
	 * Generates a signature for the specified version and pages of the version.
	 * 
	 * @param vPages Pages appearing in all SoftwareVersions in sigVersions
	 * @param sigVersions versions for which signature is to be generated
	 * @param pageSize page size
	 * @param all01count number of pages containing only 0 or 1 bits
	 * @param intDupCount number of internal duplicates
	 * @return the signature
	 */
	private VersionSignature generateSignature(Page[] vPages, SoftwareVersion[] sigVersions, int pageSize, int all01count, int intDupCount, int notMatchingInGroupCount) {
		VersionSignature sig = new VersionSignature(sigVersions, pageSize, all01count, intDupCount, notMatchingInGroupCount);
		int othVerDups = 0;
		for(Page p : vPages) {
			boolean pageFound = false;
			for(SoftwareVersion sv : _versions) {
				boolean svIsSigVersion = false;
				for(int i = 0; i < sigVersions.length && !svIsSigVersion; i++) {
					svIsSigVersion = sv.equals(sigVersions[i]);
				}
				if(svIsSigVersion) continue;
				
				// check whether an identical page is also in sv
				Iterator<CodePart> sectit = sv.getParts().iterator();
				
				while(sectit.hasNext()) {
					CodePart sect = sectit.next();
					for(int i = 0; i < sect.numberOfPages(pageSize); i++) {
						pageFound = sect.getPage(i, pageSize).contentsEqualTo(p);
						if(pageFound) {
							othVerDups++;
						}
						if(pageFound) break;
					}
					if(pageFound) break;
				}
				if(pageFound) break;
			}
			
			if(!pageFound) {
				sig.addPage(p);
			}
		}
		sig.setOtherVersionDups(othVerDups);
		
		return sig;
	}
	
	/**
	 * Compares all versions of the software to all other versions and returns the results as
	 * a TreeMap. For comparisons of a SoftwareVersion with itself, the matrix will contain null.
	 * 
	 * @return comparison results
	 */
	public TreeMap<SoftwareVersion,HashMap<SoftwareVersion, VersionComparisonResult>> compareAllVersions(int pageSize) {
		TreeMap<SoftwareVersion,HashMap<SoftwareVersion, VersionComparisonResult>> matrix = new TreeMap<SoftwareVersion,HashMap<SoftwareVersion, VersionComparisonResult>>();
		
		for(SoftwareVersion v : getVersions()) {
			HashMap<SoftwareVersion,VersionComparisonResult> vMap = new HashMap<SoftwareVersion,VersionComparisonResult>();
			for(SoftwareVersion u : getVersions()) {
				if(!v.equals(u)) {
					VersionComparisonResult compRes = v.compareToVersion(u, pageSize);
					vMap.put(u, compRes);
				} else {
					vMap.put(u, null);
				}
			}
			matrix.put(v, vMap);
		}
		
		return matrix;
	}
	
	@Override
	public int compareTo(Software o) {
		return _name.compareTo(o._name);
	}

	/**
	 * This method will consider objects that are associated with an identical software name as equal,
	 *  even if the directories etc. are not equal.
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Software)) {
			return false;
		} else {
			Software o = (Software)obj;
			return _name.equals(o._name);
		}
	}
}
