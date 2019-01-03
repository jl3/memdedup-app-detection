package de.uni_hamburg.svs.memsig;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class models a memory page of a {@link CodePart}.
 * 
 * @author Jens Lindemann
 */
public class Page {
	private byte[] _bytes;
	private CodePart _part;
	private long _pos;
	
	private boolean _all0Checked;
	private boolean _isAll0;
	private boolean _all1Checked;
	private boolean _isAll1;
	
	/**
	 * Creates a new Page object.
	 * 
	 * @param bytes contents of the Page
	 * @param part {@link CodePart} the Page belongs to
	 * @param pos offset of the Page within the {@link CodePart}
	 */
	public Page(byte[] bytes, CodePart part, long pos) {
		_bytes = bytes; // TODO This could be retrieved from CodePart to save memory.
		_part = part;
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
	 * Returns the {@link CodePart} the Page belongs to.
	 * 
	 * @return the {@link CodePart} the Page belongs to
	 */
	public CodePart getPart() {
		return _part;
	}
	
	/**
	 * Returns the index of the Page within its {@link CodePart}
	 * 
	 * @return index of the Page
	 */
	public long getPageNumber() {
		long pageno = _pos / _bytes.length;
		return pageno;
	}
	
	/**
	 * Returns the position of the Page within its {@link CodePart}.
	 * 
	 * @return position of the Page within its {@link CodePart}
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
		if(!_all0Checked) {
			_isAll0 = true;
			for(int i = 0; i < _bytes.length; i++) {
				if(_bytes[i] != 0) {
					_isAll0 = false;
					break;
				}
			}
			_all0Checked = true;
		}
		
		return _isAll0;
	}
	
	/**
	 * Checks whether the Page contains only 1-bits.
	 * 
	 * @return true if Page contains only 1-bits, false otherwise
	 */
	public boolean isAllOnes() {
		if(!_all1Checked) {
			_isAll1 = true;
			for(int i = 0; i < _bytes.length; i++) {
				if(_bytes[i] != 1) {
					_isAll1 = false;
					break;
				}
			}
			_all1Checked = true;
		}
		
		return _isAll1;
	}

	/**
	 * This method will consider a Page of equal size, code part and position therein to be equal.
	 * Contents will NOT be compared. If you wish to check for content equality, please use
	 * {@link #contentsEqualTo(Page)} instead or in addition.
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Page)) {
			return false;
		}
		
		Page op = (Page)obj;
		if((this._bytes.length == op._bytes.length) && (this._part.equals(op._part)) && (this._pos == op._pos)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(_part, _pos, _bytes.length);
	}
	
	
}
