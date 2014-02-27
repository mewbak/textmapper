/**
 * Copyright 2002-2013 Evgeny Gryaznov
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
package org.textmapper.tool.gen;

import org.textmapper.lapg.LapgCore;
import org.textmapper.lapg.api.LexerData;
import org.textmapper.lapg.api.ParserData;
import org.textmapper.lapg.api.ProcessingStatus;
import org.textmapper.lapg.api.TextSourceElement;
import org.textmapper.lapg.api.ast.AstModel;
import org.textmapper.templates.api.EvaluationContext;
import org.textmapper.templates.api.TemplatesStatus;
import org.textmapper.templates.api.types.IClass;
import org.textmapper.templates.bundle.DefaultTemplateLoader;
import org.textmapper.templates.bundle.IBundleLoader;
import org.textmapper.templates.bundle.StringTemplateLoader;
import org.textmapper.templates.bundle.TemplatesRegistry;
import org.textmapper.templates.eval.TemplatesFacade;
import org.textmapper.templates.objects.IxFactory;
import org.textmapper.templates.storage.ClassResourceLoader;
import org.textmapper.templates.storage.IResourceLoader;
import org.textmapper.templates.storage.Resource;
import org.textmapper.templates.storage.ResourceRegistry;
import org.textmapper.templates.types.TiInstance;
import org.textmapper.templates.types.TypesRegistry;
import org.textmapper.tool.compiler.TMGrammar;
import org.textmapper.tool.compiler.TMMapper;
import org.textmapper.tool.parser.TMTree.TextSource;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class TMGenerator {

	private final TMOptions options;
	private final ProcessingStatus status;
	private final ProcessingStrategy strategy;

	public TMGenerator(TMOptions options, ProcessingStatus status, ProcessingStrategy strategy) {
		this.options = options;
		this.status = status;
		this.strategy = strategy;
	}

	public boolean compileGrammar(TextSource input, boolean checkOnly) {
		try {
			TemplatesStatusAdapter templatesStatus = new TemplatesStatusAdapter(status);
			ResourceRegistry resources = createResourceRegistry();
			TypesRegistry types = new TypesRegistry(resources, templatesStatus);

			TMGrammar s = SyntaxUtil.parseSyntax(input, status, types);
			if (s == null || s.hasErrors()) {
				return false;
			}

			// TODO only when options.genast = true
			AstModel astModel = new TMMapper(s.getGrammar(), status).deriveAST();

			TemplatesRegistry registry = createTemplateRegistry(s.getTemplates(), resources, types, templatesStatus);
			if (!checkOptions(s, registry)) {
				return false;
			}

			// prepare options
			Map<String, Object> genOptions = new HashMap<String, Object>(s.getOptions());
			for (Entry<String, String> entry : options.getAdditionalOptions().entrySet()) {

				// TODO parse value, check type
				genOptions.put(entry.getKey(), entry.getValue());
			}

			// Generate tables
			long start = System.currentTimeMillis();
			ParserData r = null;
			if (s.getGrammar().getRules() != null) {
				r = LapgCore.generateParser(s.getGrammar(), status);
				if (r == null) {
					return false;
				}
			}
			LexerData l = LapgCore.generateLexer(s.getGrammar(), status);
			if (l == null) {
				return false;
			}

			if (checkOnly) return true;

			long generationTime = System.currentTimeMillis() - start;

			// Generate text
			start = System.currentTimeMillis();
			EvaluationContext context = createEvaluationContext(types, s, astModel, genOptions, l, r);
			TemplatesFacade env = new TemplatesFacadeExt(new GrammarIxFactory(s, getTemplatePackage(s), context), registry);
			env.executeTemplate(getTemplatePackage(s) + ".main", context, null, null);
			long textTime = System.currentTimeMillis() - start;
			status.report(ProcessingStatus.KIND_INFO, "lalr: " + generationTime / 1000. + "s, text: " + textTime
					/ 1000. + "s");
			return true;
		} catch (Exception t) {
			String message = "lapg: internal error: " + t.getClass().getName();
			status.report(message, t);
			return false;
		}
	}

	private String getTemplatePackage(TMGrammar g) {
		String result = options.getTemplateName();
		if (result != null) {
			return result;
		}

		result = g.getTargetLanguage();
		if (result != null) {
			return result;
		}

		return "common";
	}

	private boolean checkOptions(TMGrammar s, TemplatesRegistry registry) {
		String templPackage = getTemplatePackage(s);
		IClass cl = registry.getTypesRegistry().getClass(templPackage + ".Options", null);

		// TODO

		return true;
	}

	private ResourceRegistry createResourceRegistry() {
		List<IResourceLoader> loaders = new ArrayList<IResourceLoader>();
		for (String path : options.getIncludeFolders()) {
			IResourceLoader resourceLoader = strategy.createResourceLoader(path);
			if (resourceLoader != null) {
				loaders.add(resourceLoader);
			}
		}
		if (options.isUseDefaultTemplates()) {
			loaders.add(new ClassResourceLoader(getClass().getClassLoader(), "org/textmapper/tool/templates", "utf8"));
		}
		return new ResourceRegistry(loaders.toArray(new IResourceLoader[loaders.size()]));
	}

	private TemplatesRegistry createTemplateRegistry(TextSourceElement grammarTemplates, ResourceRegistry resources, TypesRegistry types, TemplatesStatus templatesStatus) {
		List<IBundleLoader> loaders = new ArrayList<IBundleLoader>();
		if (grammarTemplates != null) {
			loaders.add(new StringTemplateLoader(new Resource(URI.create(grammarTemplates.getResourceName()), grammarTemplates.getText(), grammarTemplates.getLine(), grammarTemplates.getOffset())));
		}
		loaders.add(new DefaultTemplateLoader(resources));
		return new TemplatesRegistry(templatesStatus, types, loaders.toArray(new IBundleLoader[loaders.size()]));
	}

	private EvaluationContext createEvaluationContext(TypesRegistry types, TMGrammar s, AstModel astModel, Map<String, Object> genOptions, LexerData l, ParserData r) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("syntax", s);
		map.put("lex", l); // new JavaIxObjectWithType(l, types.getClass("common.Lexer", null))
		map.put("parser", r);
		map.put("ast", astModel);

		String templPackage = getTemplatePackage(s);
		IClass optsClass = types.getClass(templPackage + ".Options", null);
		if (optsClass != null) {
			map.put("opts", new TiInstance(optsClass, genOptions));
		} else {
			map.put("opts", genOptions);
		}


		TiInstance context = new TiInstance(types.getClass("common.Context", null), map);
		EvaluationContext evaluationContext = new EvaluationContext(context);
		evaluationContext.setVariable("util", new TemplateStaticMethods());
		evaluationContext.setVariable("context", context);
		evaluationContext.setVariable("$", "lapg_gg.value");
		return evaluationContext;
	}

	private final class TemplatesFacadeExt extends TemplatesFacade {

		private TemplatesFacadeExt(IxFactory factory, TemplatesRegistry registry) {
			super(factory, registry);
		}

		@Override
		public void createStream(String name, String contents) {
			strategy.createFile(name, contents, status);
		}
	}
}