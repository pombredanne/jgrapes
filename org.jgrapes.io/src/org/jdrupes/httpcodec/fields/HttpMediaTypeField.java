/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016  Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package org.jdrupes.httpcodec.fields;

import java.io.IOException;
import java.io.ObjectInput;
import java.text.ParseException;

import javax.activation.MimeType;
import javax.activation.MimeTypeParameterList;
import javax.activation.MimeTypeParseException;

/**
 * Represents a header field's value and provides methods for interpreting
 * that value.
 * 
 * @author Michael N. Lipp
 */
public class HttpMediaTypeField extends HttpField<MimeType> {

	private MimeType value;
	
	/**
	 * Creates a new representation of a media type field value.
	 * 
	 * @param name the field name
	 * @param value the field value
	 * @throws ParseException 
	 */
	public HttpMediaTypeField(String name, String value) throws ParseException {
		super(name, value);
		try {
			this.value = new MimeType(value) {

				@Override
				public void readExternal(ObjectInput in)
				        throws IOException, ClassNotFoundException {
					throw new UnsupportedOperationException();
				}

				@Override
				public void setPrimaryType(String primary)
				        throws MimeTypeParseException {
					throw new UnsupportedOperationException();
				}

				@Override
				public void setSubType(String sub)
				        throws MimeTypeParseException {
					throw new UnsupportedOperationException();
				}
				
			};
		} catch (MimeTypeParseException e) {
			throw new ParseException(e.getMessage(), 0);
		}
	}

	/**
	 * Creates new object with the given type and subtype and no parameters.
	 * 
	 * @param name the field name
	 * @param type the type 
	 * @param subtype the subtype
	 * @throws ParseException 
	 */
	public HttpMediaTypeField(String name, String type, String subtype) 
			throws ParseException {
		this(name, type + "/" + subtype);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.fields.HttpField#getValue()
	 */
	@Override
	public MimeType getValue() {
		return value;
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.util.HttpFieldValue#asString()
	 */
	@Override
	public String valueToString() {
		return value.toString();
	}

	/**
	 * @see javax.activation.MimeType#getBaseType()
	 */
	public String getBaseType() {
		return value.getBaseType();
	}

	/**
	 * @see javax.activation.MimeType#getParameter(java.lang.String)
	 */
	public String getParameter(String name) {
		return value.getParameter(name);
	}

	/**
	 * @see javax.activation.MimeType#getParameters()
	 */
	public MimeTypeParameterList getParameters() {
		return value.getParameters();
	}

	/**
	 * @see javax.activation.MimeType#getPrimaryType()
	 */
	public String getPrimaryType() {
		return value.getPrimaryType();
	}

	/**
	 * @see javax.activation.MimeType#getSubType()
	 */
	public String getSubType() {
		return value.getSubType();
	}

	/**
	 * @see javax.activation.MimeType#match(javax.activation.MimeType)
	 */
	public boolean match(MimeType type) {
		return value.match(type);
	}

	/**
	 * @see javax.activation.MimeType#match(java.lang.String)
	 */
	public boolean match(String rawdata) throws MimeTypeParseException {
		return value.match(rawdata);
	}

	/**
	 * @see javax.activation.MimeType#removeParameter(java.lang.String)
	 */
	public void removeParameter(String name) {
		value.removeParameter(name);
	}

	/**
	 * @see javax.activation.MimeType#setParameter(java.lang.String, java.lang.String)
	 */
	public void setParameter(String name, String value) {
		this.value.setParameter(name, value);
	}

	
}
