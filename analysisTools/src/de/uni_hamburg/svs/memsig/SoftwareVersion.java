package de.uni_hamburg.svs.memsig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * This class represents a specific version of a {@link Software}.
 * 
 * @author Jens Lindemann
 */
public class SoftwareVersion implements Comparable<SoftwareVersion> {
	private Software _software;
	private String _versionString;
	private ComparableVersion _versionNo;
	private File _path;
	private TreeSet<CodePart> _parts;
	private int _pageSize;
	
	/**
	 * Creates a new SoftwareVersion object.
	 * 
	 * @param software the {@link Software} the version belongs to
	 * @param versionString String representation of the version number
	 * @param path where the version is stored on the file system
	 */
	public SoftwareVersion(Software software, String versionString, File path, int pageSize) {
		_software = software;
		_versionString = versionString;
		_versionNo = new ComparableVersion(versionString);
		_path = path;
		_pageSize = pageSize;
		
		initializeParts();
	}
	
	/**
	 * Reads the parts of the binary (i.e. loadable segments for ELF binaries)
	 * from the file system.
	 */
	private void initializeParts() {
		_parts = new TreeSet<CodePart>();
		
		String pdirname = "parts-" + _pageSize;
		File partsDir = new File(_path, pdirname);
		
		// If the loadable segments have not previously
		// been extracted into separate files, do it now.
		if(!partsDir.exists()) {
			initializePartsDir(partsDir);
		}
		
		// Read the segments from files
		File[] partFiles = partsDir.listFiles();
		for(File partFile : partFiles) {
			String partName = partFile.getName();
			CodePart sec = new CodePart(this, partName, partFile);
			this.addPart(sec);
		}
	}
	
	/**
	 * Extracts the parts of the binary (i.e. loadable segments for ELF
	 * binaries) into individual files within a subdirectory of the version's
	 * directory.
	 * 
	 * @param partsDir directory to store code part files
	 */
	private void initializePartsDir(File partsDir) {
		File bin = new File(_path, _software.getBinaryName());
		if(!bin.exists()) {
			System.err.println("Error: Could not find binary " + bin.getAbsolutePath());
			System.exit(1);
		}
		
		partsDir.mkdir();
		ELFSegmentExtractor se = new ELFSegmentExtractor(bin, _pageSize);
		se.split(partsDir, false, true);
	}
	
	/**
	 * Adds a {@link CodePart}.
	 * 
	 * @param sec {@link CodePart} to be added
	 */
	private void addPart(CodePart sec) {
		_parts.add(sec);
	}
	
	/**
	 * Returns the parts of the version.
	 * 
	 * @return parts of the version
	 */
	public SortedSet<CodePart> getParts() {
		return Collections.unmodifiableSortedSet(_parts);
	}
	
	/**
	 * Checks whether a {@link Page} identical in contents to a specified
	 * Page is also present in this version.
	 * 
	 * @param o {@link Page} to search for
	 * @return true if an identical Page is contained in the version, false otherwise
	 */
	public boolean containsPageContent(Page o) {
		for(CodePart s : _parts) {
			if(s.pageContentIsInPart(o)) {
				return true;
			}
		}
		// ... else
		return false;
	}
	
	/**
	 * Compares the SoftwareVersion to another one. The method will
	 * generate statistics about the number of {@link Page}s that are 
	 * (1) internal duplicates within the version,
	 * (2) matching Pages in cmpVersion or
	 * (3) unique to this version.
	 * The method will not handle pages containing only 0- or 1-bits separately.
	 * 
	 * Note that this method will count *all* copies of internal duplicates as
	 * internal duplicate, despite the fact that one might want to keep one
	 * copy when actually generating signatures.
	 * 
	 * @param cmpVersion SoftwareVersion to compare to
	 * @param pageSize page size
	 * @return the {@link VersionComparisonResult} for the two versions
	 */
	public VersionComparisonResult compareToVersion(SoftwareVersion cmpVersion, int pageSize) {		
		// iterate over all pages, for each page:
			// iterate over all pages in cmpVersion
			// if page contents are equal, increase eq counter
			// if page contents are not equal, increase neq counter
				
		int matches = 0;
		int unique = 0;
		ArrayList<Page> internalDups = new ArrayList<Page>();
		
		// look for internal duplicates
		for(CodePart s : _parts) {
			for(int i = 0; i < s.numberOfPages(pageSize); i++) {
				if(pageHasInternalDuplicate(s.getPage(i, pageSize))) {
					internalDups.add(s.getPage(i, pageSize));
				}
			}
		}
		
		// Check whether a page is also contained in cmpVersion or not
		for(CodePart s : _parts) {
			for(int i = 0; i < s.numberOfPages(pageSize); i++) {
				Page p = s.getPage(i, pageSize);
				// if(!internalDups.contains(p)) { -- not necessary here as this only makes describing the presented data more complicated...
					boolean pageFound = cmpVersion.containsPageContent(p);
					if(pageFound) {
						matches++;
					} else {
						unique++;
					}
				// }
			}
		}
		
		VersionComparisonResult vcs = new VersionComparisonResult(this, cmpVersion, matches, unique, internalDups.size());
		return vcs;
	}
	
	/**
	 * Checks whether there is an internal duplicate of a Page.
	 * 
	 * @param p Page to check
	 * @return true if an internal duplicate exists, false otherwise
	 */
	private boolean pageHasInternalDuplicate(Page p) {
		for(CodePart s : _parts) {
			for(int i = 0; i < s.numberOfPages(p.getPageSize()); i++) {
				Page op = s.getPage(i, p.getPageSize());
				if(!(p.getPart().equals(s) && (p.getPos() == op.getPos()))) {
					if(p.contentsEqualTo(op)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Returns the {@link Software} that this version belongs to.
	 * 
	 * @return the {@link Software} that this version belongs to
	 */
	public Software getSoftware() {
		return _software;
	}
	
	/**
	 * Returns the number of pages in the version's binary for a specified page size.
	 * 
	 * @param pageSize page size
	 * @return number of pages in the binary
	 */
	public int numberOfPages(int pageSize) {
		int p = 0;
		for(CodePart s : getParts()) {
			p += s.numberOfPages(pageSize);
		}
		return p;
	}
	
	/**
	 * @return the number of parts (Linux: segments) the version consists of
	 */
	public int numberOfParts() {
		return _parts.size();
	}
	
	/**
	 * Returns all pages useable in signatures, i.\,e. all pages of the version
	 * excluding internal duplicates pages containing only zero or one bits.
	 * 
	 * @param pageSize page size
	 * @return pages useable in signatures
	 */
	public Page[] getUseablePages(int pageSize) {
		// TODO implement
		ArrayList<Page> pgs = new ArrayList<Page>();
		
		for(CodePart s : _parts) {
			outerloop:
			for(int i = 0; i < s.numberOfPages(pageSize); i++) {
				Page p = s.getPage(i, pageSize);
				
				if(p.isAllZeroes() || p.isAllOnes()) {
					continue;
				}
				
				for(Page pcmp : pgs) {
					if(p.contentsEqualTo(pcmp)) {
						continue outerloop;
					}
				}
				
				pgs.add(p);
			}
		}
		
		return pgs.toArray(new Page[0]);
	}

	/**
	 * This method will consider SoftwareVersions associated to a {@link Software} with an
	 *  identical version string to be identical, even if the path differs. 
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof SoftwareVersion)) {
			return false;
		} else {
			SoftwareVersion o = (SoftwareVersion)obj;
			if(!(_software.equals(o._software))) {
				return false;
			} else {
				//return _versionString.equals(o._versionString);
				return _versionNo.equals(o._versionNo);
			}
		}
	}

	@Override
	public int compareTo(SoftwareVersion o) {
		if(!(_software.equals(o._software))) {
			return _software.compareTo(o._software);
		} else {
			// TODO This compares character for character, i.e. 2.4.10<2.4.4.
			// Could be changed to take canonical ordering into account.
			return _versionNo.compareTo(o._versionNo);
		}
	}

	@Override
	public String toString() {
		return _versionString;
	}
}
