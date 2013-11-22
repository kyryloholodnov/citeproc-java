// Copyright 2013 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.undercouch.citeproc;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import de.undercouch.citeproc.csl.CSLDate;
import de.undercouch.citeproc.csl.CSLDateBuilder;
import de.undercouch.citeproc.script.ScriptRunner;
import de.undercouch.citeproc.script.ScriptRunnerException;
import de.undercouch.citeproc.script.ScriptRunnerFactory;

/**
 * An intelligent parser for date strings. This class is able to handle
 * a wide range of date formats (e.g. YYYY-mm-dd, YYYY/mm/dd, Month YYYY)
 * @author Michel Kraemer
 */
public class CSLDateParser {
	/**
	 * A JavaScript runner used to execute citeproc-js
	 */
	private final ScriptRunner runner;
	
	/**
	 * Creates a new date parser
	 * @throws IOException if the underlying JavaScript files could not be loaded
	 */
	public CSLDateParser() throws IOException {
		//create JavaScript runner
		runner = ScriptRunnerFactory.createRunner();
		
		//load bundles scripts
		try {
			runner.eval("var CSL = new function() {};");
			runner.eval("CSL.DATE_PARTS_ALL = [\"year\", \"month\", \"day\", \"season\"];");
			runner.eval("CSL.debug = function(msg) {};");
			runner.loadScript(getClass().getResource("dateparser.js"));
		} catch (ScriptRunnerException e) {
			//should never happen because bundled JavaScript files should be OK indeed
			throw new RuntimeException("Invalid bundled javascript file", e);
		}
		
		//initialize parser
		try {
			runner.eval("var __parser__ = new CSL.DateParser();");
			runner.eval("__parser__.returnAsArray();");
		} catch (ScriptRunnerException e) {
			throw new IllegalArgumentException("Could not initialize date parser", e);
		}
	}
	
	/**
	 * Parses a string to a date
	 * @param str the string to parse
	 * @return the parsed date
	 */
	public CSLDate parse(String str) {
		Map<String, Object> res;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = (Map<String, Object>)runner.callMethod(
					"__parser__", "parse", str);
			res = m;
		} catch (ScriptRunnerException e) {
			throw new IllegalArgumentException("Could not update items", e);
		}
		
		CSLDate r = CSLDate.fromJson(res);
		if (r.getDateParts().length == 2 && Arrays.equals(r.getDateParts()[0], r.getDateParts()[1])) {
			r = new CSLDateBuilder(r).dateParts(r.getDateParts()[0]).build();
		}
		return r;
	}
}
