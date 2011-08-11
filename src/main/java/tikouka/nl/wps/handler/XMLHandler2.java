/*
 * Copyright 2011 by EDINA, University of Edinburgh, Landcare Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tikouka.nl.wps.handler;



import org.xml.sax.Attributes;
import tikouka.nl.wps.algorithm.util.RasterTable;
import tikouka.nl.wps.algorithm.util.RasterTable2;
import tikouka.nl.wps.algorithm.util.Table2;
/**
 *
 * @author niels
 * derived from a SAXParser example
 */
public class XMLHandler2 extends XMLContentHandler {

     private RasterTable2 rastertable;

	public RasterTable2 getRasterRable() {
		return rastertable;
	}

	protected Object createElement(Object parent, String name, Attributes attributes) throws Exception
	{
		Object element = null;

		if( name.compareToIgnoreCase("rastertable") == 0 )
			element = createRasterTable(attributes);
		else if( name.compareToIgnoreCase("table") == 0 )
			element = createTable((RasterTable2)parent, attributes);

		return element;
	}

	private RasterTable2 createRasterTable(Attributes attributes)
	{
                String name = attributes.getValue("name");
		rastertable = new RasterTable2(name);
		return rastertable;
	}

	private Table2 createTable(RasterTable2 rastertable, Attributes attributes)
	{
		String id = attributes.getValue("id");
		String key = attributes.getValue("key");
                String value = attributes.getValue("value");

		Table2 table = new Table2(id, key, value);
		rastertable.addTable(table);

		return table;
	}

	protected void processText(Object element,  String str) throws Exception
	{
	}

	protected void processCDATA(Object element, String str) throws Exception {
	}
}