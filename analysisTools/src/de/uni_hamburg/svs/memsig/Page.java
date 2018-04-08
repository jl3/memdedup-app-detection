package de.uni_hamburg.svs.memsig;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class models a memory page of a {@link CodeSection}.
 * 
 * @author Jens Lindemann
 */
public class Page {
	private byte[] _bytes;
	private CodeSection _section;
	private long _pos;
	
	/**
	 * Creates a new Page object.
	 * 
	 * @param bytes contents of the Page
	 * @param section {@link CodeSection} the Page belongs to
	 * @param pos offset of the Page within the {@link CodeSection}
	 */
	public Page(byte[] bytes, CodeSection section, long pos) {
		_bytes = bytes; // TODO could be retrieved from CodeSection to save memory
		_section = section;
		_pos = pos;
	}
	
	/**
	 * Returns the contents of the Page.
	 * 
	 * @return contents of the Page
	 */
	public byte[] getBytes() {
		return _bytes;
	}
	
	/**
	 * Checks whether the contents of the Page are equal to those of o.
	 * 
	 * @param o Page to compare to
	 * @return true if contents are equal, false otherwise
	 */
	public boolean contentsEqualTo(Page o) {
		return Arrays.equals(_bytes, o._bytes);
	}

	/**
	 * Returns the size of the Page.
	 * 
	 * @return size of the Page
	 */
	public int getPageSize() {
		return _bytes.length;
	}
	
	/**
	 * Returns the {@link CodeSection} the Page belongs to.
	 * 
	 * @return the {@link CodeSection} the Page belongs to
	 */
	public CodeSection getSection() {
		return _section;
	}
	
	/**
	 * Returns the index of the Page within its {@link CodeSection}
	 * 
	 * @return index of the Page
	 */
	public long getPageNumber() {
		long pageno = _pos / _bytes.length;
		return pageno;
	}
	
	/**
	 * Returns the position of the Page within its {@link CodeSection}.
	 * 
	 * @return position of the Page within its {@link CodeSection}
	 */
	public long getPos() {
		return _pos;
	}
	
	/**
	 * Checks whether the Page contains only 0-bits.
	 * 
	 * @return true if Page contains only 0-bits, false otherwise
	 */
	public boolean isAllZeroes() {
		for(int i = 0; i < _bytes.length; i++) {
			if(_bytes[i] != 0) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks whether the Page contains only 1-bits.
	 * 
	 * @return true if Page contains only 1-bits, false otherwise
	 */
	public boolean isAllOnes() {
		for(int i = 0; i < _bytes.length; i++) {
			if(_bytes[i] != 1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This method will consider a Page of equal size, code section and position therein to be equal.
	 * Contents will NOT be compared. If you wish to check for content equality, please use
	 * {@link #contentsEqualTo(Page)} instead or in addition.
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Page)) {
			return false;
		}
		
		Page op = (Page)obj;
		if((this._bytes.length == op._bytes.length) && (this._section.equals(op._section)) && (this._pos == op._pos)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(_section, _pos, _bytes.length);
	}
	
	
}
