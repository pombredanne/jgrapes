/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2016-2018 Michael N. Lipp
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

package org.jgrapes.io.events;

import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 *
 */
public class FileOpened extends Opened {

	private Path path;
	private OpenOption[] options;
	
	public FileOpened(Path path, OpenOption[] options) {
		this.path = path;
		this.options = options;
	}

	/**
	 * @return the path
	 */
	public Path path() {
		return path;
	}

	/**
	 * @return the options
	 */
	public OpenOption[] options() {
		return options;
	}

}
