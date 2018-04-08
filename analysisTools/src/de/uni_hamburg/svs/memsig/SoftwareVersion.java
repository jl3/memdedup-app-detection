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
	private TreeSet<CodeSection> _sections;
	
	/**
	 * Creates a new SoftwareVersion object.
	 * 
	 * @param software the {@link Software} the version belongs to
	 * @param versionString String representation of the version number
	 * @param path where the version is stored on the file system
	 */
	public SoftwareVersion(Software software, String versionString, File path) {
		_software = software;
		_versionString = versionString;
		_versionNo = new ComparableVersion(versionString);
		_path = path;
		
		initializeSections();
	}
	
	/**
	 * Reads the sections of the binary from the file system.
	 */
	private void initializeSections() {
		_sections = new TreeSet<CodeSection>();
		
		File sectionsDir = new File(_path, "sections");
		
		// If the sections containing program bits have not previously
		// been extracted into separate files, do it now.
		if(!sectionsDir.exists()) {
			initializeSectionsDir(sectionsDir);
		}
		
		// Read the sections from the section files
		File[] secFiles = sectionsDir.listFiles();
		for(File secFile : secFiles) {
			String secName = secFile.getName();
			CodeSection sec = new CodeSection(this, secName, secFile);
			this.addSection(sec);
		}
	}
	
	/**
	 * Extracts the sections containing program bits into
	 * individual files within a subdirectory of the version's directory.
	 * 
	 * @param sectionsDir
	 */
	private void initializeSectionsDir(File sectionsDir) {
		File bin = new File(_path, _software.getBinaryName());
		if(!bin.exists()) {
			System.err.println("Error: Could not find binary " + bin.getAbsolutePath());
			System.exit(1);
		}
		
		sectionsDir.mkdir();
		SectionExtractor se = new SectionExtractor(bin);
		se.split(sectionsDir, false, true);
	}
	
	/**
	 * Adds a {@link CodeSection}.
	 * 
	 * @param sec {@link CodeSection} to be added
	 */
	private void addSection(CodeSection sec) {
		_sections.add(sec);
	}
	
	/**
	 * Returns the sections of the version.
	 * 
	 * @return sections of the version
	 */
	public SortedSet<CodeSection> getSections() {
		return Collections.unmodifiableSortedSet(_sections);
	}
	
	/**
	 * Checke whether a {@link Page} identical in contents to a specified
	 * Page is also present in this version.
	 * 
	 * @param o {@link Page} to search for
	 * @return true if an identical Page is contained in the version, false otherwise
	 */
	public boolean containsPageContent(Page o) {
		for(CodeSection s : _sections) {
			if(s.pageContentIsInSection(o)) {
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
		for(CodeSection s : _sections) {
			for(int i = 0; i < s.numberOfPages(pageSize); i++) {
				if(pageHasInternalDuplicate(s.getPage(i, pageSize))) {
					internalDups.add(s.getPage(i, pageSize));
				}
			}
		}
		
		// Check whether a page is also contained in cmpVersion or not
		for(CodeSection s : _sections) {
			for(int i = 0; i < s.numberOfPages(pageSize); i++) {
				Page p = s.getPage(i, pageSize);
				if(!internalDups.contains(p)) {
					boolean pageFound = cmpVersion.containsPageContent(p);
					if(pageFound) {
						matches++;
					} else {
						unique++;
					}
				}
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
		for(CodeSection s : _sections) {
			for(int i = 0; i < s.numberOfPages(p.getPageSize()); i++) {
				Page op = s.getPage(i, p.getPageSize());
				if(!(p.getSection().equals(s) && (p.getPos() == op.getPos()))) {
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
		for(CodeSection s : getSections()) {
			p += s.numberOfPages(pageSize);
		}
		return p;
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
			// TODO BUG: 2.4.10 should be > than 2.4.4...
			//return _versionString.compareTo(o._versionString);
			return _versionNo.compareTo(o._versionNo);
		}
	}

	@Override
	public String toString() {
		return _versionString;
	}
}
