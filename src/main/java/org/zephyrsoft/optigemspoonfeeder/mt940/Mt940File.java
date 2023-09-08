/*
 * Copyright (C) 2008 Arnout Engelen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Origin: https://github.com/ccavanaugh/jgnash
 */
package org.zephyrsoft.optigemspoonfeeder.mt940;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Mt940File {
	private List<Mt940Record> records = new ArrayList<>();

	public List<Mt940Entry> getEntries() {
		List<Mt940Entry> retval = new ArrayList<>();
		for (Mt940Record mt940Record : getRecords()) {
			retval.addAll(mt940Record.getEntries());
		}
		return retval;
	}
}
