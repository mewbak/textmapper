/**
 * Copyright 2002-2014 Evgeny Gryaznov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.textmapper.lapg.builder;

import org.textmapper.lapg.api.Nonterminal;
import org.textmapper.lapg.api.Symbol;

public class LiUtil {
	public static String getSymbolName(Symbol s) {
		String name = s.getName();
		if (name != null) {
			return name;
		}
		// for anonymous nonterminals we can get an approximate name from nameHint user data.
		return (String) s.getUserData(Nonterminal.UD_NAME_HINT);
	}

}